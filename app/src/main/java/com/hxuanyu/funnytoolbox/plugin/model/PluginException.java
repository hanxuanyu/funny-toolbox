package com.hxuanyu.funnytoolbox.plugin.model;

/**
 * 插件异常
 */
public class PluginException extends RuntimeException {

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}