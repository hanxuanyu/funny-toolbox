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
        SpringApplication.run(FunnyToolboxApplication.class, args);
        log.info("===========================================");
        log.info("🎉 Toolbox Platform Started Successfully!");
        log.info("🌐 Access: http://localhost:8080");
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
