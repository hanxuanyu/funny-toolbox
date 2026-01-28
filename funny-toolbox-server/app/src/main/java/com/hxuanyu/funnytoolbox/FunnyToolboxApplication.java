package com.hxuanyu.funnytoolbox;

import com.hxuanyu.funnytoolbox.plugin.core.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class FunnyToolboxApplication {

    public static void main(String[] args) {
        var ctx = SpringApplication.run(FunnyToolboxApplication.class, args);
        var env = ctx.getEnvironment();

        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        if (contextPath == null) {
            contextPath = "";
        }
        // 规范化 context-path（确保以 / 开头且无尾部 /，根路径保持空串）
        if (!contextPath.isEmpty()) {
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
        }

        String baseUrl = "http://localhost:" + port + contextPath;

        log.info("===========================================");
        log.info("🎉 Toolbox Platform Started Successfully!");
        log.info("🌐 Access: {}", baseUrl);
        // 文档地址提示
        log.info("📘 OpenAPI JSON: {}/v3/api-docs", baseUrl);
        log.info("🧭 Swagger UI  : {}/swagger-ui/index.html", baseUrl);
        log.info("🔪 Knife4j UI  : {}/doc.html", baseUrl);
        log.info("===========================================");
    }

    /**
     * 启动时自动加载插件
     */
    @Bean
    public CommandLineRunner pluginAutoLoader(PluginManager pluginManager) {
        return args -> {
            log.info("🔌 Auto-loading plugins...");
            pluginManager.autoLoadPlugins();
        };
    }

}
