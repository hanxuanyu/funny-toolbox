package com.hxuanyu.funnytoolbox.plugin.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
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

    private final Map<String, List<String>> resourceMappings = new ConcurrentHashMap<>();

    /**
     * 注册静态资源映射
     *
     * @param pluginId 插件 ID
     * @param urlPath URL 路径模式，例如: /plugins/secret-capsule/**
     * @param resourceLocation 资源位置，例如: jar:file:plugins/plugin.jar!/static/
     */
    public void registerResources(String pluginId, String urlPath, String resourceLocation) {
        resourceMappings.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(urlPath);
        log.info("Registered static resources: {} -> {}", urlPath, resourceLocation);

        // 注意：Spring Boot 3.x 中，动态添加资源映射比较复杂
        // 这里我们采用另一种方案：在 WebMvcConfig 中统一配置
    }

    /**
     * 注销静态资源
     */
    public void unregisterResources(String pluginId) {
        List<String> paths = resourceMappings.remove(pluginId);
        if (paths != null) {
            log.info("Unregistered static resources for plugin: {}", pluginId);
        }
    }

    /**
     * 获取所有资源映射
     */
    public Map<String, List<String>> getAllMappings() {
        return new ConcurrentHashMap<>(resourceMappings);
    }
}
