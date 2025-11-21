package com.hxuanyu.funnytoolbox.plugin.core;

import com.hxuanyu.toolbox.plugin.api.IPlugin;
import com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor;
import com.hxuanyu.funnytoolbox.plugin.model.PluginStatus;
import lombok.Data;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件运行时上下文
 */
@Data
public class PluginContext {

    private String pluginId;
    private PluginDescriptor descriptor;
    private PluginStatus status;

    // 类加载器
    private PluginClassLoader classLoader;

    // Spring 应用上下文
    private AnnotationConfigApplicationContext applicationContext;

    // 插件实例
    private IPlugin pluginInstance;

    // 数据目录
    private Path dataDirectory;

    // 配置目录
    private Path configDirectory;

    // 注册的路由映射信息（用于卸载时清理）
    private List<MappingInfo> registeredMappings = new ArrayList<>();

    // 注册的静态资源路径
    private List<String> registeredResourcePaths = new ArrayList<>();

    // 时间戳
    private LocalDateTime loadTime;
    private LocalDateTime startTime;

    @Data
    public static class MappingInfo {
        private Object handler;
        private Object mapping;
    }
}
