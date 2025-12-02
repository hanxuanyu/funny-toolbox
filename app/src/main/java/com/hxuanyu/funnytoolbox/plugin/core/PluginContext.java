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

    // 包类型与来源（用于前端-only 插件等场景）
    public enum PackageType { JAR, ZIP }

    // 插件包类型（JAR 或 ZIP），JAR 类型表示传统后端插件；ZIP 表示仅前端资源的轻量插件
    private PackageType packageType;

    // 插件包文件的绝对路径（用于静态资源提取等）
    private String packageFilePath;

    @Data
    public static class MappingInfo {
        private Object handler;
        private Object mapping;
    }
}
