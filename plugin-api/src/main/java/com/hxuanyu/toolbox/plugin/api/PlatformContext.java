package com.hxuanyu.toolbox.plugin.api;

import java.nio.file.Path;
import java.util.Properties;

/**
 * 平台上下文
 * 平台提供给插件的服务接口
 */
public interface PlatformContext {

    /**
     * 获取插件 ID
     */
    String getPluginId();

    /**
     * 获取插件名称
     */
    String getPluginName();

    /**
     * 获取插件数据目录
     * 插件可以在此目录下存储数据文件、数据库等
     */
    Path getDataDirectory();

    /**
     * 获取插件配置目录
     */
    Path getConfigDirectory();

    /**
     * 获取插件配置
     */
    Properties getConfig();

    /**
     * 设置配置项
     */
    void setConfig(String key, String value);

    /**
     * 保存配置
     */
    void saveConfig() throws Exception;

    /**
     * 记录日志
     */
    void log(String message);

    /**
     * 记录错误
     */
    void error(String message, Throwable throwable);
}
