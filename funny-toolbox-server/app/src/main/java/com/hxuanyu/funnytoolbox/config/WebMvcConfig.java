package com.hxuanyu.funnytoolbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置跨域
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }


//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // 平台自身的静态资源
//        registry.addResourceHandler("/static/**")
//                .addResourceLocations("classpath:/static/");
//    }

    /**
     * 配置静态资源
     * 注意：插件的静态资源在 PluginManager 中动态注册
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 1) Knife4j doc.html（位于 META-INF/resources/）
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/")
                .resourceChain(false);

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(false);


        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                // 注意：在可执行 JAR 中，开启资源链有时会导致自定义 PathResourceResolver 的解析出现异常，
                // 进而无法正确返回静态资源。这里关闭资源链以提升兼容性（IDE 与打包后行为一致）。
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    // 文档和静态资源白名单前缀
                    private final String[] DOC_PREFIXES = new String[]{
                            "swagger-ui/",     // swagger UI 静态资源
                            "v3/api-docs",     // openapi 文档接口
                            "swagger-resources", // swagger 资源
                            "webjars/",        // webjars 静态资源
                            "doc.html"         // knife4j 入口
                    };

                    private boolean isDocOrStaticWhitelisted(String resourcePath) {
                        for (String prefix : DOC_PREFIXES) {
                            if (resourcePath.startsWith(prefix)) {
                                return true;
                            }
                        }
                        // 常见静态文件（含favicon）
                        if (resourcePath.equals("favicon.ico")) {
                            return true;
                        }
                        // 看起来像静态文件（包含扩展名）
                        return resourcePath.contains(".");
                    }

                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 真实存在的静态文件直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // API 请求交给 Spring MVC（Controller）处理
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // 文档和静态资源前缀白名单：不要兜底到 index.html，交给 Spring 继续处理
                        if (isDocOrStaticWhitelisted(resourcePath)) {
                            return null;
                        }

                        // 其他路径兜底给 SPA 的 index.html
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
