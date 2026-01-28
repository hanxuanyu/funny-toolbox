package com.hxuanyu.funnytoolbox.plugin.core;

import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * 插件类加载器
 * 采用 Parent-Last 策略，优先从插件 JAR 加载
 */
@Slf4j
public class PluginClassLoader extends URLClassLoader {

    private final String pluginId;
    private final Set<String> platformClasses;

    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.pluginId = pluginId;

        // 定义必须从父加载器加载的类（平台 API 和核心库）
        this.platformClasses = Set.of(
                "com.hxuanyu.toolbox.plugin.api.",
                "org.springframework.",
                "org.apache.commons.",
                "javax.",
                "jakarta.",
                "java.",
                "sun.",
                "jdk."
        );
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // 1. 检查是否已加载
            Class<?> clazz = findLoadedClass(name);
            if (clazz != null) {
                return clazz;
            }

            // 2. 平台类必须从父加载器加载
            if (isPlatformClass(name)) {
                try {
                    return super.loadClass(name, resolve);
                } catch (ClassNotFoundException e) {
                    log.warn("Platform class not found: {}", name);
                    throw e;
                }
            }

            // 3. 先尝试从插件 JAR 加载
            try {
                clazz = findClass(name);
                if (resolve) {
                    resolveClass(clazz);
                }
                log.debug("Loaded class from plugin {}: {}", pluginId, name);
                return clazz;
            } catch (ClassNotFoundException e) {
                // 4. 插件找不到，再从父加载器加载
                log.debug("Class not found in plugin {}, trying parent: {}", pluginId, name);
                return super.loadClass(name, resolve);
            }
        }
    }

    private boolean isPlatformClass(String name) {
        return platformClasses.stream().anyMatch(name::startsWith);
    }

    public String getPluginId() {
        return pluginId;
    }
}