package com.hxuanyu.funnytoolbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证配置，从 application.yaml 读取登录账号与密码。
 */
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 登录用户名 */
    private String username;

    /** 登录密码（明文存储于配置，仅用于简单内置认证） */
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
