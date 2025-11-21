package com.hxuanyu.funnytoolbox.plugin.model;

import lombok.Data;

/**
 * 插件图标的结构化定义
 * 支持多种来源：emoji、URL、常见图标框架（如 Font Awesome、Material Icons）等
 */
@Data
public class PluginIcon {
    private IconType type;   // 图标类型
    private String value;    // 具体值：emoji字符、URL、框架类名/图标名等
    private String color;    // 可选：颜色
    private String style;    // 可选：风格，如 outlined/rounded 等

    public enum IconType {
        EMOJI,
        URL,
        SVG,
        FONT_AWESOME,
        MATERIAL,
        CUSTOM;

        public static IconType fromString(String s) {
            if (s == null) return null;
            String v = s.trim().toLowerCase();
            switch (v) {
                case "emoji":
                    return EMOJI;
                case "url":
                    return URL;
                case "svg":
                    return SVG;
                case "fa":
                case "fontawesome":
                case "font_awesome":
                case "font-awesome":
                    return FONT_AWESOME;
                case "material":
                case "material-icons":
                case "material_icons":
                    return MATERIAL;
                default:
                    return CUSTOM;
            }
        }
    }
}
