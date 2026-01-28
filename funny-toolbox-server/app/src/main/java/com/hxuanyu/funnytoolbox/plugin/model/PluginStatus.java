package com.hxuanyu.funnytoolbox.plugin.model;

/**
 * 插件状态
 */
public enum PluginStatus {
    /**
     * 已加载（未启用）
     */
    LOADED,

    /**
     * 已启用（正在运行）
     */
    ENABLED,

    /**
     * 已禁用
     */
    DISABLED,

    /**
     * 错误状态
     */
    ERROR
}
