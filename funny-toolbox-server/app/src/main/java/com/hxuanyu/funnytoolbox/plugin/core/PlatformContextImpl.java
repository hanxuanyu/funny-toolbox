package com.hxuanyu.funnytoolbox.plugin.core;

import com.hxuanyu.toolbox.plugin.api.PlatformContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 平台上下文实现
 */
@Slf4j
public class PlatformContextImpl implements PlatformContext {

    private final PluginContext pluginContext;
    private final Properties config;
    private final Path configFile;
    private final Logger logger;

    public PlatformContextImpl(PluginContext pluginContext) {
        this.pluginContext = pluginContext;
        this.config = new Properties();
        this.configFile = pluginContext.getConfigDirectory().resolve("config.properties");
        // 为当前插件创建独立的 Logger，便于插件内直接注入与使用
        this.logger = LoggerFactory.getLogger("plugin." + pluginContext.getPluginId());

        // 加载配置
        loadConfig();
    }

    @Override
    public String getPluginId() {
        return pluginContext.getPluginId();
    }

    @Override
    public String getPluginName() {
        return pluginContext.getDescriptor().getName();
    }

    @Override
    public Path getDataDirectory() {
        return pluginContext.getDataDirectory();
    }

    @Override
    public Path getConfigDirectory() {
        return pluginContext.getConfigDirectory();
    }

    @Override
    public Properties getConfig() {
        return config;
    }

    @Override
    public void setConfig(String key, String value) {
        config.setProperty(key, value);
    }

    @Override
    public void saveConfig() throws Exception {
        try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
            config.store(fos, "Plugin Configuration: " + getPluginName());
        }
    }

    @Override
    public void log(String message) {
        logger.info("[Plugin:{}] {}", getPluginId(), message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error("[Plugin:{}] {}", getPluginId(), message, throwable);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    private void loadConfig() {
        if (Files.exists(configFile)) {
            try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
                config.load(fis);
                log.debug("Loaded config for plugin: {}", getPluginId());
            } catch (IOException e) {
                log.warn("Failed to load config for plugin: {}", getPluginId(), e);
            }
        }
    }
}
