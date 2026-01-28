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
import java.io.OutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * 插件管理器
 */
@Service
@Slf4j
public class PluginManager {

    private final Map<String, PluginContext> pluginContexts = new ConcurrentHashMap<>();
    // 处于重载过程中的插件ID集合，用于在卸载阶段跳过包文件删除（以便随后从同一包重新加载）
    private final Set<String> reloadingIds = ConcurrentHashMap.newKeySet();

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

        File[] pkgFiles = dir.listFiles((d, name) -> name.endsWith(".jar") || name.endsWith(".zip"));
        if (pkgFiles == null || pkgFiles.length == 0) {
            log.info("No plugin found in {}", dir.getAbsolutePath());
            return;
        }

        log.info("Found {} plugin package(s), loading...", pkgFiles.length);

        for (File pkgFile : pkgFiles) {
            try {
                loadPlugin(pkgFile);

                // 自动启用
                PluginDescriptor descriptor = readDescriptorFromArchive(pkgFile);
                String pluginId = descriptor.getId();
                boolean shouldEnable = readPersistedEnabledOrDefaultTrue(pluginId);
                if (shouldEnable) {
                    enablePlugin(pluginId);
                } else {
                    log.info("Plugin {} is marked as disabled (persisted). Skip auto enable.", pluginId);
                }

            } catch (Exception e) {
                log.error("Failed to load plugin: {}", pkgFile.getName(), e);
            }
        }
    }

    /**
     * 加载插件
     */
    public synchronized void loadPlugin(File packageFile) throws Exception {
        log.info("Loading plugin from: {}", packageFile.getAbsolutePath());

        // 1. 读取插件描述符（兼容 jar/zip 档）
        PluginDescriptor descriptor = readDescriptorFromArchive(packageFile);
        String pluginId = descriptor.getId();

        if (pluginContexts.containsKey(pluginId)) {
            throw new PluginException("Plugin already loaded: " + pluginId);
        }

        // 2. 创建插件上下文
        PluginContext context = new PluginContext();
        context.setPluginId(pluginId);
        context.setDescriptor(descriptor);
        context.setStatus(PluginStatus.LOADED);
        context.setDataDirectory(createPluginDirectory("data/plugins/" + pluginId));
        context.setConfigDirectory(createPluginDirectory("config/plugins/" + pluginId));
        context.setLoadTime(LocalDateTime.now());
        context.setPackageFilePath(packageFile.getAbsolutePath());
        context.setPackageType(packageFile.getName().endsWith(".jar") ? PluginContext.PackageType.JAR : PluginContext.PackageType.ZIP);
        // 3. 如果存在 mainClass（通常是 .jar 后端插件），则创建类加载器和 Spring 上下文
        if (StringUtils.hasText(descriptor.getMainClass())) {
            URL pkgUrl = packageFile.toURI().toURL();
            PluginClassLoader classLoader = new PluginClassLoader(
                    pluginId,
                    new URL[]{pkgUrl},
                    this.getClass().getClassLoader()
            );
            context.setClassLoader(classLoader);

            // 创建插件 Spring 上下文
            AnnotationConfigApplicationContext pluginAppContext = new AnnotationConfigApplicationContext();
            pluginAppContext.setClassLoader(classLoader);
            pluginAppContext.setParent(platformContext);

            // 平台上下文 Bean
            PlatformContextImpl platformCtx = new PlatformContextImpl(context);
            pluginAppContext.registerBean(com.hxuanyu.toolbox.plugin.api.PlatformContext.class, () -> platformCtx);
            pluginAppContext.registerBean(PlatformContextImpl.class, () -> platformCtx);

            // 扫描并刷新
            String basePackage = getBasePackage(descriptor.getMainClass());
            if (StringUtils.hasText(basePackage)) {
                pluginAppContext.scan(basePackage);
            }
            pluginAppContext.refresh();
            context.setApplicationContext(pluginAppContext);

            // 实例化主类
            Class<?> mainClass = classLoader.loadClass(descriptor.getMainClass());
            IPlugin pluginInstance = (IPlugin) mainClass.getDeclaredConstructor().newInstance();
            context.setPluginInstance(pluginInstance);

            // 调用 onLoad
            try {
                pluginInstance.onLoad(platformCtx);
            } catch (Exception e) {
                log.error("Plugin onLoad failed: {}", pluginId, e);
                throw e;
            }
        } else {
            log.info("Plugin {} has no mainClass, treated as frontend-only plugin.", pluginId);
        }

        // 7. 初始化标签到状态文件（若不存在则写入插件内置标签；若已存在，尊重已有值）
        try {
            initPersistedTagsIfAbsent(pluginId, descriptor.getTags());
        } catch (Exception ex) {
            log.warn("Init persisted tags failed for {}: {}", pluginId, ex.getMessage());
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
            // 1. 调用插件 onEnable（如存在后端主类）
            if (context.getPluginInstance() != null) {
                context.getPluginInstance().onEnable();
            }

            // 2. 注册 API 路由（仅后端插件）
            if (context.getApplicationContext() != null) {
                registerApiRoutes(context);
            }

            // 3. 注册静态资源
            registerStaticResources(context);

            // 4. 注册菜单
            registerMenu(context);

            // 5. 更新状态
            context.setStatus(PluginStatus.ENABLED);
            context.setStartTime(LocalDateTime.now());

            // 6. 持久化状态
            savePluginEnabled(pluginId, true);

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
            if (context.getPluginInstance() != null) {
                context.getPluginInstance().onDisable();
            }

            // 2. 注销 API 路由
            if (!context.getRegisteredMappings().isEmpty()) {
                unregisterApiRoutes(context);
            }

            // 3. 注销静态资源
            unregisterStaticResources(context);

            // 4. 注销菜单
            unregisterMenu(context);

            // 5. 更新状态
            context.setStatus(PluginStatus.DISABLED);

            // 6. 持久化状态
            savePluginEnabled(pluginId, false);

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
            if (context.getPluginInstance() != null) {
                context.getPluginInstance().onUnload();
            }
        } catch (Exception e) {
            log.error("Error in plugin onUnload: {}", pluginId, e);
        }

        // 3. 关闭 Spring 上下文
        try {
            if (context.getApplicationContext() != null) {
                context.getApplicationContext().close();
            }
        } catch (Exception e) {
            log.error("Error closing ApplicationContext: {}", pluginId, e);
        }

        // 4. 关闭类加载器
        try {
            if (context.getClassLoader() != null) {
                context.getClassLoader().close();
            }
        } catch (IOException e) {
            log.error("Error closing ClassLoader: {}", pluginId, e);
        }

        // 5. 所有清理完成后，再从全局上下文中移除
        try {
            // 优化：对前端-only（ZIP）插件，尝试删除插件目录下的 ZIP 包文件
            // 仅当记录了包路径且位于平台插件目录下时才执行删除，避免误删外部路径或非 ZIP 包
            // 若当前处于 reload 流程中，则跳过删除，以便随后从相同包重新加载
            if (context.getPackageType() == PluginContext.PackageType.ZIP && !reloadingIds.contains(pluginId)) {
                String pkgPath = context.getPackageFilePath();
                if (pkgPath != null && !pkgPath.isEmpty()) {
                    Path pkg = Paths.get(pkgPath).toAbsolutePath().normalize();
                    Path pluginsBase = Paths.get(pluginDir).toAbsolutePath().normalize();
                    if (Files.exists(pkg) && pkg.toString().toLowerCase().endsWith(".zip") && pkg.startsWith(pluginsBase)) {
                        boolean deleted = deleteWithRetry(pkg.toFile(), 5, 300);
                        if (deleted) {
                            log.info("Deleted ZIP plugin package file: {}", pkg);
                        } else {
                            log.warn("Failed to delete ZIP plugin package after retries: {}", pkg);
                        }
                    } else {
                        log.debug("Skip deleting plugin package. Exists? {} EndsWith .zip? {} In plugins dir? {}",
                                Files.exists(pkg), pkg.toString().toLowerCase().endsWith(".zip"), pkg.startsWith(pluginsBase));
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Exception while trying to delete plugin package for {}: {}", pluginId, ex.getMessage());
        } finally {
            pluginContexts.remove(pluginId);
        }

        log.info("✅ Plugin unloaded: {}", pluginId);
    }

    /**
     * 重新加载插件
     */
    public synchronized void reloadPlugin(String pluginId) throws Exception {
        PluginContext context = getContext(pluginId);

        // 优先使用已记录的原始包路径（兼容 JAR / ZIP）
        String packagePath = context.getPackageFilePath();
        if (packagePath == null || packagePath.isEmpty() || !Files.exists(Paths.get(packagePath))) {
            // 根据包类型或实际存在情况回退查找
            if (context.getPackageType() == PluginContext.PackageType.JAR) {
                packagePath = findPluginJar(pluginId);
            } else if (context.getPackageType() == PluginContext.PackageType.ZIP) {
                packagePath = findPluginZip(pluginId);
            } else {
                // 未知时，先尝试 JAR，再尝试 ZIP
                String tryJar = null;
                try {
                    tryJar = findPluginJar(pluginId);
                } catch (PluginException ignored) { }
                if (tryJar != null) {
                    packagePath = tryJar;
                } else {
                    packagePath = findPluginZip(pluginId);
                }
            }
        }

        // 卸载（标记重载过程，避免卸载阶段删除包文件）
        reloadingIds.add(pluginId);
        try {
            unloadPlugin(pluginId);
        } finally {
            // 确保标记被清理
            reloadingIds.remove(pluginId);
        }

        // 等待资源释放
        Thread.sleep(500);

        // 重新加载
        loadPlugin(new File(packagePath));

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
     * 查询插件状态（可选返回）。
     */
    public Optional<PluginStatus> getPluginStatus(String pluginId) {
        PluginContext ctx = pluginContexts.get(pluginId);
        return Optional.ofNullable(ctx).map(PluginContext::getStatus);
    }

    /**
     * 是否处于启用状态。
     */
    public boolean isPluginEnabled(String pluginId) {
        return getPluginStatus(pluginId).orElse(null) == PluginStatus.ENABLED;
    }

    /**
     * 根据请求路径解析隶属的插件ID（针对 API 路由）。
     * 会根据每个已加载插件的 apiPrefix 进行前缀匹配。
     * 仅在插件被加载（不论启用/禁用）情况下有效。
     */
    public Optional<String> resolvePluginIdByApiPath(String requestPath) {
        if (requestPath == null) return Optional.empty();
        String path = requestPath.trim();
        // 规范化，确保以 "/" 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        for (PluginContext ctx : pluginContexts.values()) {
            PluginDescriptor descriptor = ctx.getDescriptor();
            String apiPrefix = (descriptor.getApi() != null && descriptor.getApi().getPrefix() != null
                    && !descriptor.getApi().getPrefix().isEmpty())
                    ? descriptor.getApi().getPrefix()
                    : "/api/" + descriptor.getId();

            // 统一去掉尾部斜杠
            if (apiPrefix.endsWith("/")) {
                apiPrefix = apiPrefix.substring(0, apiPrefix.length() - 1);
            }

            // 匹配当前请求是否以该前缀开头（完整段匹配）
            if (path.equals(apiPrefix) || path.startsWith(apiPrefix + "/")) {
                return Optional.of(descriptor.getId());
            }
        }
        return Optional.empty();
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

        // 优化：将插件包内的静态资源解压到平台本地缓存目录（支持 JAR / ZIP）
        String resourceLocation;
        try {
            String extractedDir;
            if (context.getPackageType() == PluginContext.PackageType.ZIP) {
                extractedDir = extractZipStaticToCache(descriptor.getId(), context.getPackageFilePath(), basePath);
            } else {
                extractedDir = extractPluginStaticToCache(descriptor.getId(), basePath);
            }
            resourceLocation = "file:" + ensureEndsWithSlash(extractedDir);
        } catch (Exception ex) {
            if (context.getPackageType() == PluginContext.PackageType.JAR) {
                log.warn("Failed to extract static resources for plugin {}, fallback to jar access: {}",
                        descriptor.getId(), ex.getMessage());
                resourceLocation = "jar:file:" + findPluginJar(descriptor.getId()) + "!" + ensureStartsWithSlash(basePath) + "/";
            } else {
                throw new PluginException("Failed to extract static resources for ZIP plugin: " + descriptor.getId(), ex);
            }
        }

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

        // 同步清理本地缓存的静态资源目录（若存在）
        try {
            Path cacheDir = getStaticCacheDir(context.getPluginId());
            if (Files.exists(cacheDir)) {
                FileUtils.deleteDirectory(cacheDir.toFile());
                log.info("Deleted static cache directory for plugin {}: {}", context.getPluginId(), cacheDir);
            }
        } catch (Exception ex) {
            log.warn("Failed to delete static cache directory for plugin {}: {}", context.getPluginId(), ex.getMessage());
        }
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
     * 读取插件描述符（兼容 JAR/ZIP 包）
     */
    private PluginDescriptor readDescriptorFromArchive(File packageFile) throws Exception {
        String name = packageFile.getName().toLowerCase();
        if (name.endsWith(".jar")) {
            return readDescriptor(packageFile);
        } else if (name.endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(packageFile)) {
                ZipEntry entry = zip.getEntry("META-INF/plugin.yml");
                if (entry == null) {
                    throw new PluginException("plugin.yml not found in " + packageFile.getName());
                }
                try (InputStream is = zip.getInputStream(entry)) {
                    return PluginDescriptor.load(is);
                }
            }
        } else {
            throw new PluginException("Unsupported plugin package type: " + packageFile.getName());
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
     * 查找插件 ZIP 文件（用于前端-only 插件）。
     */
    private String findPluginZip(String pluginId) {
        try (Stream<Path> stream = Files.list(Paths.get(pluginDir))) {
            return stream
                    .filter(p -> p.toString().endsWith(".zip"))
                    .filter(p -> {
                        try {
                            File file = p.toFile();
                            PluginDescriptor desc = readDescriptorFromArchive(file);
                            return desc.getId().equals(pluginId);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .map(Path::toString)
                    .orElseThrow(() -> new PluginException("ZIP not found for plugin: " + pluginId));
        } catch (IOException e) {
            throw new PluginException("Failed to search plugin ZIP", e);
        }
    }

    // ===================== 静态资源提取与缓存 =====================

    private Path getStaticCacheDir(String pluginId) {
        return Paths.get("data/static-cache/" + pluginId);
    }

    private String ensureEndsWithSlash(String path) {
        if (path == null || path.isEmpty()) return path;
        return path.endsWith("/") || path.endsWith("\\") ? path : path + "/";
    }

    private String ensureStartsWithSlash(String path) {
        if (path == null || path.isEmpty()) return "/";
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * 将插件 JAR 中 basePath 下的静态资源解压至本地缓存目录，返回缓存目录绝对路径。
     */
    private String extractPluginStaticToCache(String pluginId, String basePath) throws IOException {
        String jarPath = findPluginJar(pluginId);
        String normBase = basePath == null ? "static" : basePath;
        if (normBase.startsWith("/")) normBase = normBase.substring(1);
        if (!normBase.endsWith("/")) normBase = normBase + "/";

        Path cacheDir = getStaticCacheDir(pluginId);
        Files.createDirectories(cacheDir);

        // 先清空旧缓存目录，避免遗留脏文件
        try {
            if (Files.exists(cacheDir)) {
                FileUtils.cleanDirectory(cacheDir.toFile());
            }
        } catch (IOException ignore) {
            // 清空失败不影响后续覆盖写入
        }

        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(normBase)) {
                    continue;
                }
                String relative = name.substring(normBase.length());
                if (relative.isEmpty()) {
                    continue;
                }
                // 基础的路径穿越防护
                if (relative.contains("..") || relative.startsWith("/")) {
                    continue;
                }

                Path outPath = cacheDir.resolve(relative).normalize();
                if (!outPath.startsWith(cacheDir)) {
                    // 防止逃逸
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, outPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }

        return cacheDir.toAbsolutePath().toString();
    }

    /**
     * 将 ZIP 插件包中 basePath 下的静态资源解压至本地缓存目录，返回缓存目录绝对路径。
     */
    private String extractZipStaticToCache(String pluginId, String zipPath, String basePath) throws IOException {
        String normBase = basePath == null ? "static" : basePath;
        if (normBase.startsWith("/")) normBase = normBase.substring(1);
        if (!normBase.endsWith("/")) normBase = normBase + "/";

        Path cacheDir = getStaticCacheDir(pluginId);
        Files.createDirectories(cacheDir);

        try {
            if (Files.exists(cacheDir)) {
                FileUtils.cleanDirectory(cacheDir.toFile());
            }
        } catch (IOException ignore) {
        }

        try (ZipFile zip = new ZipFile(zipPath)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(normBase)) {
                    continue;
                }
                String relative = name.substring(normBase.length());
                if (relative.isEmpty()) continue;
                if (relative.contains("..") || relative.startsWith("/")) continue;

                Path outPath = cacheDir.resolve(relative).normalize();
                if (!outPath.startsWith(cacheDir)) continue;

                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (InputStream is = zip.getInputStream(entry)) {
                        Files.copy(is, outPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        return cacheDir.toAbsolutePath().toString();
    }

    // ===================== 文件删除重试（Windows 友好） =====================
    private boolean deleteWithRetry(File file, int attempts, long sleepMillis) {
        if (file == null) return false;
        for (int i = 0; i < Math.max(1, attempts); i++) {
            try {
                if (!file.exists()) return true;
                Files.deleteIfExists(file.toPath());
                if (!file.exists()) return true;
            } catch (Exception ignore) {
            }
            try {
                Thread.sleep(Math.max(0, sleepMillis));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        return !file.exists();
    }

    // ===================== 插件状态持久化 =====================

    /**
     * 读取持久化的插件启用状态；当没有记录时，默认返回 true（即默认启用）。
     */
    private boolean readPersistedEnabledOrDefaultTrue(String pluginId) {
        try {
            Path stateFile = getPluginStateFile(pluginId);
            if (!Files.exists(stateFile)) {
                return true; // 无记录则默认启用
            }
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(stateFile)) {
                props.load(is);
            }
            String v = props.getProperty("enabled");
            if (v == null) return true;
            return Boolean.parseBoolean(v.trim());
        } catch (Exception ex) {
            log.warn("Failed to read persisted state for plugin {}: {}. Use default: enabled.", pluginId, ex.getMessage());
            return true;
        }
    }

    /**
     * 持久化保存插件启用状态。
     */
    private void savePluginEnabled(String pluginId, boolean enabled) {
        try {
            Path stateFile = getPluginStateFile(pluginId);
            Files.createDirectories(stateFile.getParent());
            Properties props = new Properties();
            // 读取旧内容，避免覆盖其它属性（如 tags）
            if (Files.exists(stateFile)) {
                try (InputStream is = Files.newInputStream(stateFile)) {
                    props.load(is);
                } catch (IOException ignore) {}
            }
            props.setProperty("enabled", Boolean.toString(enabled));
            try (OutputStream os = Files.newOutputStream(stateFile, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                props.store(os, "Plugin state for " + pluginId);
            }
            log.debug("Persisted plugin state: {} -> enabled={}", pluginId, enabled);
        } catch (Exception ex) {
            log.warn("Failed to persist state for plugin {}: {}", pluginId, ex.getMessage());
        }
    }

    private Path getPluginStateFile(String pluginId) {
        return Paths.get("config", "plugins", pluginId, "plugin-state.properties");
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
     * 公开方法：尝试查找插件ZIP（前端-only），未找到时返回 null 而不是抛异常
     */
    public String tryFindPluginZip(String pluginId) {
        try {
            return findPluginZip(pluginId);
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

        // 设置标签（来自状态文件的最终标签；若无则回退到描述符）
        try {
            dto.setTags(getPluginTags(desc.getId()));
        } catch (Exception ignore) {
            dto.setTags(desc.getTags());
        }

        return dto;
    }

    // ===================== 标签支持 =====================

    private void initPersistedTagsIfAbsent(String pluginId, List<String> defaultTags) {
        Path stateFile = getPluginStateFile(pluginId);
        try {
            Files.createDirectories(stateFile.getParent());
            Properties props = new Properties();
            if (Files.exists(stateFile)) {
                try (InputStream is = Files.newInputStream(stateFile)) {
                    props.load(is);
                }
            }
            if (!props.containsKey("tags")) {
                if (defaultTags != null && !defaultTags.isEmpty()) {
                    props.setProperty("tags", joinTags(defaultTags));
                    try (OutputStream os = Files.newOutputStream(stateFile, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                        props.store(os, "Plugin state for " + pluginId);
                    }
                } else if (!Files.exists(stateFile)) {
                    // 确保至少创建一个空的 state 文件
                    try (OutputStream os = Files.newOutputStream(stateFile, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                        props.store(os, "Plugin state for " + pluginId);
                    }
                }
            } else {
                // 已有 tags，不覆盖
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> parseTags(String csv) {
        if (csv == null || csv.trim().isEmpty()) return Collections.emptyList();
        return Stream.of(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String joinTags(List<String> tags) {
        if (tags == null) return "";
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
    }

    public List<String> getPluginTags(String pluginId) {
        Path stateFile = getPluginStateFile(pluginId);
        Properties props = new Properties();
        try {
            if (Files.exists(stateFile)) {
                try (InputStream is = Files.newInputStream(stateFile)) {
                    props.load(is);
                }
            }
        } catch (IOException e) {
            log.warn("Read tags failed for {}: {}", pluginId, e.getMessage());
        }
        String csv = props.getProperty("tags");
        if (csv == null || csv.isEmpty()) {
            // 回退到描述符
            PluginContext ctx = pluginContexts.get(pluginId);
            if (ctx != null && ctx.getDescriptor() != null) {
                List<String> tags = ctx.getDescriptor().getTags();
                return tags != null ? tags : Collections.emptyList();
            }
            return Collections.emptyList();
        }
        return parseTags(csv);
    }

    public void setPluginTags(String pluginId, List<String> tags) {
        Path stateFile = getPluginStateFile(pluginId);
        try {
            Files.createDirectories(stateFile.getParent());
            Properties props = new Properties();
            if (Files.exists(stateFile)) {
                try (InputStream is = Files.newInputStream(stateFile)) {
                    props.load(is);
                }
            }
            props.setProperty("tags", joinTags(tags));
            try (OutputStream os = Files.newOutputStream(stateFile, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                props.store(os, "Plugin state for " + pluginId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addPluginTag(String pluginId, String tag) {
        List<String> tags = new ArrayList<>(getPluginTags(pluginId));
        if (tag != null) {
            String t = tag.trim();
            if (!t.isEmpty() && !tags.contains(t)) {
                tags.add(t);
            }
        }
        setPluginTags(pluginId, tags);
    }

    public void removePluginTag(String pluginId, String tag) {
        List<String> tags = new ArrayList<>(getPluginTags(pluginId));
        if (tag != null) {
            tags.removeIf(s -> s.equalsIgnoreCase(tag.trim()));
        }
        setPluginTags(pluginId, tags);
    }

    public List<PluginDTO> getPluginsByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return Collections.emptyList();
        String target = tag.trim();
        return pluginContexts.values().stream()
                .map(this::toDTO)
                .filter(dto -> dto.getTags() != null && dto.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(target)))
                .collect(Collectors.toList());
    }

    public List<PluginDTO> getPluginsByTags(List<String> tags, boolean matchAll) {
        if (tags == null || tags.isEmpty()) return Collections.emptyList();
        List<String> normalized = tags.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (normalized.isEmpty()) return Collections.emptyList();
        return pluginContexts.values().stream()
                .map(this::toDTO)
                .filter(dto -> {
                    List<String> ts = dto.getTags();
                    if (ts == null || ts.isEmpty()) return false;
                    if (matchAll) {
                        return normalized.stream().allMatch(n -> ts.stream().anyMatch(t -> t.equalsIgnoreCase(n)));
                    } else {
                        return normalized.stream().anyMatch(n -> ts.stream().anyMatch(t -> t.equalsIgnoreCase(n)));
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取当前平台所有插件的“已存在标签”集合（去重、按字母顺序，大小写不敏感）。
     * 用于前端提供可选择的标签列表。
     */
    public List<String> getAllTags() {
        // 使用不区分大小写的排序与去重
        Set<String> set = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (PluginContext ctx : pluginContexts.values()) {
            String id = ctx.getPluginId();
            List<String> tags = getPluginTags(id);
            if (tags != null) {
                tags.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(set::add);
            }
        }
        return new ArrayList<>(set);
    }
}