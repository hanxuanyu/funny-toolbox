package com.hxuanyu.funnytoolbox.plugin.model;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 插件描述符
 * 对应 plugin.yml 的内容
 */
@Data
public class PluginDescriptor {

    private String id;
    private String name;
    private String version;
    private String description;
    private String author;
    // 新增：更灵活的图标定义，支持 emoji、URL、常见图标框架等
    private PluginIcon iconMeta;

    private String mainClass;

    private FrontendConfig frontend;
    private ApiConfig api;

    private List<String> dependencies;
    private List<String> permissions;
    // 插件在描述符中声明的标签（用于默认分类，可被管理端覆盖）
    private List<String> tags;

    @Data
    public static class FrontendConfig {
        private String entry;       // /index.html
        private String basePath;    // /static
    }

    @Data
    public static class ApiConfig {
        private String prefix;      // /api/secret-capsule
    }

    /**
     * 从 YAML 加载
     */
    public static PluginDescriptor load(InputStream yamlStream) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlStream);

        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor.setId((String) data.get("id"));
        descriptor.setName((String) data.get("name"));
        descriptor.setVersion((String) data.get("version"));
        descriptor.setDescription((String) data.get("description"));
        descriptor.setAuthor((String) data.get("author"));
        // icon 仅支持对象形式（不再支持直接使用 String）
        Object iconObj = data.get("icon");
        if (iconObj instanceof Map) {
            // 新版：icon 支持对象形式
            PluginIcon iconMeta = parseIconMeta((Map<String, Object>) iconObj);
            descriptor.setIconMeta(iconMeta);
        }
        descriptor.setMainClass((String) data.get("mainClass"));

        // 解析 frontend
        Map<String, String> frontendData = (Map<String, String>) data.get("frontend");
        if (frontendData != null) {
            FrontendConfig frontend = new FrontendConfig();
            frontend.setEntry(frontendData.get("entry"));
            frontend.setBasePath(frontendData.get("basePath"));
            descriptor.setFrontend(frontend);
        }

        // 解析 api
        Map<String, String> apiData = (Map<String, String>) data.get("api");
        if (apiData != null) {
            ApiConfig api = new ApiConfig();
            api.setPrefix(apiData.get("prefix"));
            descriptor.setApi(api);
        }

        // 解析依赖和权限
        descriptor.setDependencies((List<String>) data.get("dependencies"));
        descriptor.setPermissions((List<String>) data.get("permissions"));

        // 解析标签（可选）
        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List) {
            // 兼容旧格式：YAML 列表
            descriptor.setTags(((List<?>) tagsObj).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        } else if (tagsObj instanceof String) {
            // 新推荐格式：逗号分隔的字符串，例如: tags: a,b,c
            String csv = ((String) tagsObj).trim();
            if (!csv.isEmpty()) {
                List<String> tags = java.util.Arrays.stream(csv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .toList();
                descriptor.setTags(tags);
            }
        }

        return descriptor;
    }

    /**
     * 将对象形式的 icon 字段解析为 PluginIcon
     * 支持示例：
     * icon:
     *   type: emoji
     *   value: "🔧"
     * 或
     * icon:
     *   type: url
     *   value: https://example.com/icon.png
     * 或
     * icon:
     *   type: font_awesome
     *   value: fa-solid fa-wrench
     * 或
     * icon:
     *   framework: material
     *   value: home
     */
    private static PluginIcon parseIconMeta(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        PluginIcon icon = new PluginIcon();
        // 读取 type 或 framework 兼容
        Object type = map.get("type");
        Object framework = map.get("framework");
        String typeStr = type != null ? String.valueOf(type) : (framework != null ? String.valueOf(framework) : null);
        if (typeStr != null) {
            icon.setType(PluginIcon.IconType.fromString(typeStr));
        }
        Object value = map.get("value");
        if (value == null) {
            // 兼容别名 key
            value = map.get("name");
            if (value == null) {
                value = map.get("url");
                if (value != null && icon.getType() == null) {
                    icon.setType(PluginIcon.IconType.URL);
                }
            }
            // 支持 svg 专用 key
            if (value == null) {
                value = map.get("svg");
                if (value != null && icon.getType() == null) {
                    icon.setType(PluginIcon.IconType.SVG);
                }
            }
        }
        icon.setValue(value != null ? String.valueOf(value) : null);
        Object color = map.get("color");
        if (color != null) {
            icon.setColor(String.valueOf(color));
        }
        Object style = map.get("style");
        if (style != null) {
            icon.setStyle(String.valueOf(style));
        }
        // 智能推断：当未显式指定类型时，根据内容推断 SVG/URL
        if (icon.getType() == null && icon.getValue() != null) {
            String v = icon.getValue().trim();
            String lower = v.toLowerCase();
            if (lower.startsWith("<svg") || lower.startsWith("data:image/svg+xml")) {
                icon.setType(PluginIcon.IconType.SVG);
            } else if (lower.endsWith(".svg") && (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/"))) {
                icon.setType(PluginIcon.IconType.URL);
            }
        }
        return icon;
    }

    /**
     * 解析为前端可直接使用的简易字符串
     * 规则：
     * - EMOJI: 直接返回 emoji
     * - URL: 返回 URL
     * - FONT_AWESOME: 返回前缀 fa: + value，例如 fa:fa-solid fa-wrench
     * - MATERIAL: 返回前缀 md: + value，例如 md:home
     * - CUSTOM/未知: 返回 value
     */
    public String resolveIconString() {
        if (iconMeta == null) {
            return null;
        }
        PluginIcon.IconType type = iconMeta.getType();
        String value = iconMeta.getValue();
        if (type == null) {
            // 没有明确类型时，尝试推断：URL/emoji 简单处理
            if (value != null && (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/"))) {
                return value;
            }
            return value;
        }
        switch (type) {
            case EMOJI:
                return value;
            case URL:
                return value;
            case SVG:
                if (value == null) return null;
                String v = value.trim();
                String lower = v.toLowerCase();
                // URL 或 data URI 直接返回；否则使用 svg: 前缀给前端处理 inline SVG
                if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/") || lower.startsWith("data:image/svg+xml")) {
                    return v;
                }
                return "svg:" + v;
            case FONT_AWESOME:
                return value != null ? "fa:" + value : null;
            case MATERIAL:
                return value != null ? "md:" + value : null;
            case CUSTOM:
            default:
                return value;
        }
    }
}
