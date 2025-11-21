package com.hxuanyu.toolbox.plugin.api;


/**
 * 插件接口
 * 所有插件的主类必须实现此接口
 */
public interface IPlugin {

    /**
     * 插件加载时调用
     * 此时插件已被识别，但尚未启用
     *
     * @param context 平台上下文
     */
    void onLoad(PlatformContext context) throws Exception;

    /**
     * 插件启用时调用
     * 可以在此初始化资源、数据库等
     */
    void onEnable() throws Exception;

    /**
     * 插件禁用时调用
     * 应该在此清理资源
     */
    void onDisable() throws Exception;

    /**
     * 插件卸载前调用
     * 应该在此执行最终的清理工作
     */
    void onUnload() throws Exception;
}
