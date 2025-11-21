package com.hxuanyu.funnytoolbox.plugin.model;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
    private String icon;

    private String mainClass;

    private FrontendConfig frontend;
    private ApiConfig api;

    private List<String> dependencies;
    private List<String> permissions;

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
        descriptor.setIcon((String) data.get("icon"));
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

        return descriptor;
    }
}
