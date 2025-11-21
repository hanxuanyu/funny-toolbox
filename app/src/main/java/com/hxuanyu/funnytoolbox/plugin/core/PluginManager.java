package com.hxuanyu.funnytoolbox.plugin.core;


import com.hxuanyu.toolbox.plugin.api.IPlugin;
import com.hxuanyu.funnytoolbox.plugin.model.PluginDTO;
import com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor;
import com.hxuanyu.funnytoolbox.plugin.model.PluginException;
import com.hxuanyu.funnytoolbox.plugin.model.PluginStatus;
import com.hxuanyu.funnytoolbox.plugin.registry.MenuRegistry;
import com.hxuanyu.funnytoolbox.plugin.registry.RouteRegistry;
import com.hxuanyu.funnytoolbox.plugin.registry.StaticResourceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 插件管理器
 */
@Service
@Slf4j
public class PluginManager {

    private final Map<String, PluginContext> pluginContexts = new ConcurrentHashMap<>();

    @Autowired
    private RouteRegistry routeRegistry;

    @Autowired
    private StaticResourceRegistry staticResourceRegistry;

    @Autowired
    private MenuRegistry menuRegistry;

    @Autowired
    private ApplicationContext platformContext;

    @Value("${platform.plugin.dir:./plugins}")
    private String pluginDir;

    @Value("${platform.plugin.auto-load:true}")
    private boolean autoLoad;

    // 保留核心修复，移除不必要的诊断开关

    /**
     * 自动加载插件目录下的所有插件
     */
    public void autoLoadPlugins() {
        if (!autoLoad) {
            log.info("Auto-load is disabled");
            return;
        }

        File dir = new File(pluginDir);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            log.info("Created plugin directory: {}, result: {}", dir.getAbsolutePath(), result);
            return;
        }

        File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            log.info("No plugin found in {}", dir.getAbsolutePath());
            return;
        }

        log.info("Found {} plugin(s), loading...", jarFiles.length);

        for (File jarFile : jarFiles) {
            try {
                loadPlugin(jarFile);

                // 自动启用
                PluginDescriptor descriptor = readDescriptor(jarFile);
                enablePlugin(descriptor.getId());

            } catch (Exception e) {
                log.error("Failed to load plugin: {}", jarFile.getName(), e);
            }
        }
    }

    /**
     * 加载插件
     */
    public synchronized void loadPlugin(File jarFile) throws Exception {
        log.info("Loading plugin from: {}", jarFile.getAbsolutePath());

        // 1. 读取插件描述符
        PluginDescriptor descriptor = readDescriptor(jarFile);
        String pluginId = descriptor.getId();

        if (pluginContexts.containsKey(pluginId)) {
            throw new PluginException("Plugin already loaded: " + pluginId);
        }

        // 2. 创建类加载器
        URL jarUrl = jarFile.toURI().toURL();
        PluginClassLoader classLoader = new PluginClassLoader(
                pluginId,
                new URL[]{jarUrl},
                this.getClass().getClassLoader()
        );

        // 3. 创建插件上下文
        PluginContext context = new PluginContext();
        context.setPluginId(pluginId);
        context.setDescriptor(descriptor);
        context.setClassLoader(classLoader);
        context.setStatus(PluginStatus.LOADED);
        context.setDataDirectory(createPluginDirectory("data/plugins/" + pluginId));
        context.setConfigDirectory(createPluginDirectory("config/plugins/" + pluginId));
        context.setLoadTime(LocalDateTime.now());

        // 4. 创建插件的 Spring 上下文
        AnnotationConfigApplicationContext pluginAppContext =
                new AnnotationConfigApplicationContext();

        pluginAppContext.setClassLoader(classLoader);
        pluginAppContext.setParent(platformContext);

        // 5. 扫描插件包
        String basePackage = getBasePackage(descriptor.getMainClass());
        if (StringUtils.hasText(basePackage)) {
            pluginAppContext.scan(basePackage);
        }

        pluginAppContext.refresh();
        context.setApplicationContext(pluginAppContext);

        // 6. 实例化插件主类
        Class<?> mainClass = classLoader.loadClass(descriptor.getMainClass());
        IPlugin pluginInstance = (IPlugin) mainClass.getDeclaredConstructor().newInstance();
        context.setPluginInstance(pluginInstance);

        // 7. 调用插件 onLoad
        PlatformContextImpl platformCtx = new PlatformContextImpl(context);
        try {
            pluginInstance.onLoad(platformCtx);
        } catch (Exception e) {
            log.error("Plugin onLoad failed: {}", pluginId, e);
            throw e;
        }

        // 8. 保存上下文
        pluginContexts.put(pluginId, context);

        log.info("✅ Plugin loaded: {} v{}", descriptor.getName(), descriptor.getVersion());
    }

    /**
     * 启用插件
     */
    public synchronized void enablePlugin(String pluginId) throws Exception {
        PluginContext context = getContext(pluginId);

        if (context.getStatus() == PluginStatus.ENABLED) {
            log.warn("Plugin already enabled: {}", pluginId);
            return;
        }

        log.info("Enabling plugin: {}", pluginId);

        try {
            // 1. 调用插件 onEnable
            context.getPluginInstance().onEnable();

            // 2. 注册 API 路由
            registerApiRoutes(context);

            // 3. 注册静态资源
            registerStaticResources(context);

            // 4. 注册菜单
            registerMenu(context);

            // 5. 更新状态
            context.setStatus(PluginStatus.ENABLED);
            context.setStartTime(LocalDateTime.now());

            log.info("✅ Plugin enabled: {}", pluginId);

        } catch (Exception e) {
            context.setStatus(PluginStatus.ERROR);
            log.error("Failed to enable plugin: {}", pluginId, e);
            throw e;
        }
    }

    /**
     * 禁用插件
     */
    public synchronized void disablePlugin(String pluginId) throws Exception {
        PluginContext context = getContext(pluginId);

        if (context.getStatus() != PluginStatus.ENABLED) {
            log.warn("Plugin not enabled: {}", pluginId);
            return;
        }

        log.info("Disabling plugin: {}", pluginId);

        try {
            // 1. 调用插件 onDisable
            context.getPluginInstance().onDisable();

            // 2. 注销 API 路由
            unregisterApiRoutes(context);

            // 3. 注销静态资源
            unregisterStaticResources(context);

            // 4. 注销菜单
            unregisterMenu(context);

            // 5. 更新状态
            context.setStatus(PluginStatus.DISABLED);

            log.info("✅ Plugin disabled: {}", pluginId);

        } catch (Exception e) {
            log.error("Failed to disable plugin: {}", pluginId, e);
            throw e;
        }
    }

    /**
     * 卸载插件
     */
    public synchronized void unloadPlugin(String pluginId) throws Exception {
        // 先不要从全局上下文中移除，避免后续禁用步骤无法获取到上下文
        PluginContext context = getContext(pluginId);

        log.info("Unloading plugin: {}", pluginId);

        // 1. 先禁用
        if (context.getStatus() == PluginStatus.ENABLED) {
            disablePlugin(pluginId);
        }

        // 2. 调用插件 onUnload
        try {
            context.getPluginInstance().onUnload();
        } catch (Exception e) {
            log.error("Error in plugin onUnload: {}", pluginId, e);
        }

        // 3. 关闭 Spring 上下文
        try {
            context.getApplicationContext().close();
        } catch (Exception e) {
            log.error("Error closing ApplicationContext: {}", pluginId, e);
        }

        // 4. 关闭类加载器
        try {
            context.getClassLoader().close();
        } catch (IOException e) {
            log.error("Error closing ClassLoader: {}", pluginId, e);
        }

        // 5. 所有清理完成后，再从全局上下文中移除
        pluginContexts.remove(pluginId);

        log.info("✅ Plugin unloaded: {}", pluginId);
    }

    /**
     * 重新加载插件
     */
    public synchronized void reloadPlugin(String pluginId) throws Exception {
        PluginContext context = getContext(pluginId);
        String jarPath = findPluginJar(pluginId);

        // 卸载
        unloadPlugin(pluginId);

        // 等待资源释放
        Thread.sleep(500);

        // 重新加载
        loadPlugin(new File(jarPath));

        // 自动启用
        enablePlugin(pluginId);
    }

    /**
     * 获取所有插件
     */
    public List<PluginDTO> getAllPlugins() {
        return pluginContexts.values().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取插件上下文
     */
    private PluginContext getContext(String pluginId) {
        PluginContext context = pluginContexts.get(pluginId);
        if (context == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }
        return context;
    }

    /**
     * 注册 API 路由
     */
    private void registerApiRoutes(PluginContext context) {
        var appContext = context.getApplicationContext();
        PluginDescriptor descriptor = context.getDescriptor();

        // 获取所有 Controller
        Map<String, Object> controllers = new HashMap<>();
        controllers.putAll(appContext.getBeansWithAnnotation(RestController.class));
        controllers.putAll(appContext.getBeansWithAnnotation(Controller.class));

        if (controllers.isEmpty()) {
            log.info("No controller found in plugin: {}", descriptor.getId());
            return;
        }

        String apiPrefix = descriptor.getApi() != null ?
                descriptor.getApi().getPrefix() : "/api/" + descriptor.getId();

        for (Map.Entry<String, Object> entry : controllers.entrySet()) {
            Object controller = entry.getValue();
            List<Object> mappings = routeRegistry.registerController(apiPrefix, controller);

            // 保存映射信息以便卸载时清理
            mappings.forEach(mapping -> {
                PluginContext.MappingInfo info = new PluginContext.MappingInfo();
                info.setHandler(controller);
                info.setMapping(mapping);
                context.getRegisteredMappings().add(info);
            });

            log.info("Registered {} routes for controller: {}",
                    mappings.size(), controller.getClass().getSimpleName());
        }
    }

    /**
     * 注销 API 路由
     */
    private void unregisterApiRoutes(PluginContext context) {
        for (PluginContext.MappingInfo info : context.getRegisteredMappings()) {
            routeRegistry.unregisterMapping(info.getMapping());
        }
        context.getRegisteredMappings().clear();
    }

    /**
     * 注册静态资源
     */
    private void registerStaticResources(PluginContext context) {
        PluginDescriptor descriptor = context.getDescriptor();
        if (descriptor.getFrontend() == null) {
            log.info("No frontend config in plugin: {}", descriptor.getId());
            return;
        }

        String basePath = descriptor.getFrontend().getBasePath();
        if (basePath == null) {
            basePath = "/static";
        }

        String urlPath = "/plugins/" + descriptor.getId() + "/**";
        String resourceLocation = "jar:file:" + findPluginJar(descriptor.getId()) + "!" + basePath + "/";

        staticResourceRegistry.registerResources(descriptor.getId(), urlPath, resourceLocation);
        context.getRegisteredResourcePaths().add(urlPath);

        log.info("Registered static resources: {} -> {}", urlPath, resourceLocation);
    }

    /**
     * 注销静态资源
     */
    private void unregisterStaticResources(PluginContext context) {
        staticResourceRegistry.unregisterResources(context.getPluginId());
        context.getRegisteredResourcePaths().clear();
    }

    /**
     * 注册菜单
     */
    private void registerMenu(PluginContext context) {
        PluginDescriptor descriptor = context.getDescriptor();

        MenuRegistry.MenuItem item = new MenuRegistry.MenuItem();
        item.setPluginId(descriptor.getId());
        item.setLabel(descriptor.getName());
        String iconStr = descriptor.resolveIconString();
        item.setIcon((iconStr != null && !iconStr.isEmpty()) ? iconStr : "🔧");
        item.setRoute("/plugin/" + descriptor.getId());
        item.setOrder(0);

        menuRegistry.registerMenu(item);
    }

    /**
     * 注销菜单
     */
    private void unregisterMenu(PluginContext context) {
        menuRegistry.unregisterMenu(context.getPluginId());
    }

    /**
     * 读取插件描述符
     */
    private PluginDescriptor readDescriptor(File jarFile) throws Exception {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("META-INF/plugin.yml");
            if (entry == null) {
                throw new PluginException("plugin.yml not found in " + jarFile.getName());
            }

            // 必须在关闭 JarFile 之前关闭其返回的 InputStream，避免 Windows 下 JAR 被占用
            try (InputStream is = jar.getInputStream(entry)) {
                return PluginDescriptor.load(is);
            }
        }
    }

    /**
     * 公开方法：从 JAR 内部的描述文件解析插件ID
     */
    public String resolvePluginIdFromJar(File jarFile) throws Exception {
        PluginDescriptor descriptor = readDescriptor(jarFile);
        return descriptor.getId();
    }

    /**
     * 创建插件目录
     */
    private Path createPluginDirectory(String path) throws IOException {
        Path dir = Paths.get(path);
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * 获取基础包名
     */
    private String getBasePackage(String mainClass) {
        if (mainClass == null) {
            return "";
        }
        int lastDot = mainClass.lastIndexOf('.');
        return lastDot > 0 ? mainClass.substring(0, lastDot) : "";
    }

    /**
     * 查找插件 JAR 文件
     */
    private String findPluginJar(String pluginId) {
        try (Stream<Path> stream = Files.list(Paths.get(pluginDir))) {
            return stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> {
                        try {
                            File file = p.toFile();
                            PluginDescriptor desc = readDescriptor(file);
                            return desc.getId().equals(pluginId);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .map(Path::toString)
                    .orElseThrow(() -> new PluginException("JAR not found for plugin: " + pluginId));
        } catch (IOException e) {
            throw new PluginException("Failed to search plugin JAR", e);
        }
    }

    /**
     * 公开方法：尝试查找插件JAR，未找到时返回 null 而不是抛异常
     */
    public String tryFindPluginJar(String pluginId) {
        try {
            return findPluginJar(pluginId);
        } catch (PluginException ex) {
            return null;
        }
    }

    /**
     * 转换为 DTO
     */
    private PluginDTO toDTO(PluginContext context) {
        PluginDescriptor desc = context.getDescriptor();

        PluginDTO dto = new PluginDTO();
        dto.setId(desc.getId());
        dto.setName(desc.getName());
        dto.setVersion(desc.getVersion());
        dto.setDescription(desc.getDescription());
        dto.setAuthor(desc.getAuthor());
        String dtoIcon = desc.resolveIconString();
        dto.setIcon((dtoIcon != null && !dtoIcon.isEmpty()) ? dtoIcon : "🔧");
        dto.setStatus(context.getStatus().name());
        dto.setLoadTime(context.getLoadTime());
        dto.setStartTime(context.getStartTime());

        if (desc.getFrontend() != null && desc.getFrontend().getEntry() != null) {
            dto.setFrontendEntry("/plugins/" + desc.getId() +
                    desc.getFrontend().getEntry().replace("/static", ""));
        }

        if (desc.getApi() != null) {
            dto.setApiPrefix(desc.getApi().getPrefix());
        }

        return dto;
    }
}