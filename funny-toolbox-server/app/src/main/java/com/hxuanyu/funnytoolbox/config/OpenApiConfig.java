package com.hxuanyu.funnytoolbox.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 与 Knife4j 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI funnyToolboxOpenAPI() {
        Info info = new Info()
                .title("Funny Toolbox API")
                .description("Funny Toolbox 平台后端接口文档，包含插件管理、菜单等模块")
                .version("v1.0.0")
                .contact(new Contact().name("hxuanyu").url("https://github.com/hxuanyu").email(""))
                .license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html"));

        Server local = new Server().url("/").description("默认服务");

        return new OpenAPI()
                .info(info)
                .servers(List.of(local))
                .components(new Components());
    }
}
