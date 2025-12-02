package com.hxuanyu.funnytoolbox.plugin.model.pack;

import com.hxuanyu.funnytoolbox.plugin.model.PluginIcon;
import lombok.Data;

/**
 * 前端-only 插件打包元数据（由前端提交）。
 */
@Data
public class FrontendPluginPackMeta {
    // 基本信息
    private String id;            // 插件ID（必填）
    private String name;          // 插件名称（必填）
    private String version;       // 版本（必填）
    private String description;   // 描述（可选）
    private String author;        // 作者（可选）

    // 图标（结构化配置，保持与 PluginDescriptor 一致）
    private PluginIcon iconMeta;  // 可选

    // 前端入口与静态目录
    private String frontendEntry;     // 例如 "/index.html"（必填）
    private String frontendBasePath;  // 例如 "/static"（可选，默认 /static）
}
