package com.hxuanyu.funnytoolbox.plugin.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.core.io.UrlResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 静态资源注册器
 */
@Component
@Slf4j
public class StaticResourceRegistry implements WebMvcConfigurer {

    @Autowired
    private ResourceLoader resourceLoader;

    // pluginId -> base resource location (e.g. jar:file:/path/to/plugin.jar!/static/)
    private final Map<String, String> pluginResourceLocations = new ConcurrentHashMap<>();

    // 兼容对外查看：保存每个插件的资源位置列表（目前仅存放一个 resourceLocation）
    private final Map<String, List<String>> resourceMappings = new ConcurrentHashMap<>();

    /**
     * 注册静态资源映射
     *
     * @param pluginId 插件 ID
     * @param urlPath URL 路径模式，例如: /plugins/secret-capsule/**
     * @param resourceLocation 资源位置，例如: jar:file:plugins/plugin.jar!/static/
     */
    public void registerResources(String pluginId, String urlPath, String resourceLocation) {
        // 存储插件资源基础路径；后续通过自定义 ResourceResolver 动态解析
        pluginResourceLocations.put(pluginId, normalizeBase(resourceLocation));

        // 保存可视化信息（用于查询/调试）
        resourceMappings.put(pluginId, List.of(resourceLocation));

        log.info("Registered static resources mapping for plugin [{}]: {} -> {}", pluginId, urlPath, resourceLocation);
    }

    /**
     * 注销静态资源
     */
    public void unregisterResources(String pluginId) {
        resourceMappings.remove(pluginId);
        String removed = pluginResourceLocations.remove(pluginId);
        if (removed != null) {
            log.info("Unregistered static resources for plugin: {}", pluginId);
        }
    }

    /**
     * 获取所有资源映射
     */
    public Map<String, List<String>> getAllMappings() {
        return new ConcurrentHashMap<>(resourceMappings);
    }

    /**
     * 统一注册 /plugins/** 静态资源处理，但实际资源定位由自定义解析器根据 pluginId 动态决定。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/plugins/**")
                // 添加占位 location（必须提供至少一个），实际由 resolver 完成解析
                .addResourceLocations("classpath:/")
                .resourceChain(true)
                .addResolver(new PluginResourceResolver());
    }

    private String normalizeBase(String base) {
        if (base == null) return null;
        return base.endsWith("/") ? base : base + "/";
    }

    /**
     * 自定义资源解析器：根据 URL /plugins/{pluginId}/** 动态从对应插件 JAR 的 static 目录解析资源
     */
    class PluginResourceResolver implements ResourceResolver {

        @Override
        public Resource resolveResource(HttpServletRequest request,
                                        String requestPath,
                                        List<? extends Resource> locations,
                                        ResourceResolverChain chain) {
            // 期望 requestPath 形如：plugins/{pluginId}/path/inside 或 {pluginId}/path/inside
            if (requestPath == null) return null;

            String path = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            if (path.startsWith("plugins/")) {
                path = path.substring("plugins/".length());
            }

            int slash = path.indexOf('/');
            String pluginId;
            String innerPath;
            if (slash < 0) {
                pluginId = path;
                innerPath = "";
            } else {
                pluginId = path.substring(0, slash);
                innerPath = path.substring(slash + 1);
            }

            if (pluginId.isEmpty()) return null;

            String base = pluginResourceLocations.get(pluginId);
            if (base == null) {
                return null;
            }

            // 简单防护，避免路径穿越
            if (innerPath.contains("..")) {
                return null;
            }

            String full = base + innerPath;
            try {
                Resource resource = resourceLoader.getResource(full);
                if (resource != null && resource.exists() && resource.isReadable()) {
                    // 对于 jar:file: 协议，返回禁用缓存的包装，避免句柄被锁
                    try {
                        URL url = resource.getURL();
                        String protocol = url.getProtocol();
                        if (protocol != null && protocol.startsWith("jar")) {
                            log.debug("Resolving plugin static resource (non-cache) [{}] -> {}", requestPath, url);
                            return new NonCachingUrlResource(url);
                        }
                    } catch (Exception ignore) {
                        // 忽略并返回原资源
                    }
                    return resource;
                }
            } catch (Exception e) {
                log.warn("Failed to resolve plugin static resource: {} -> {}", requestPath, full, e);
            }

            return null;
        }

        @Override
        public String resolveUrlPath(String resourcePath,
                                     List<? extends Resource> locations,
                                     ResourceResolverChain chain) {
            // 保持原样
            return resourcePath;
        }
    }

    /**
     * 禁用 URLConnection 缓存的 UrlResource，避免 Windows 下 JarURLConnection 锁文件。
     */
    static class NonCachingUrlResource extends UrlResource {
        public NonCachingUrlResource(URL url) {
            super(url);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            URLConnection con = getURL().openConnection();
            con.setUseCaches(false);
            try {
                return con.getInputStream();
            } catch (IOException ex) {
                try {
                    con.getInputStream().close();
                } catch (IOException ignore) {
                }
                throw ex;
            }
        }
    }
}
