package com.hxuanyu.funnytoolbox.controller;

import com.hxuanyu.funnytoolbox.common.Result;
import com.hxuanyu.funnytoolbox.config.AuthProperties;
import com.hxuanyu.funnytoolbox.config.AuthenticationFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 登录认证接口
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "登录/登出接口")
public class AuthController {

    @Autowired
    private AuthProperties authProperties;

    public record LoginRequest(String username, String password) {}

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "登录", description = "使用 application.yaml 配置的账号密码登录")
    public Result<Void> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (req == null || isEmpty(req.username()) || isEmpty(req.password())) {
            return Result.error(400, "用户名或密码不能为空");
        }
        String cfgUser = nullToEmpty(authProperties.getUsername());
        String cfgPass = nullToEmpty(authProperties.getPassword());
        if (req.username().equals(cfgUser) && req.password().equals(cfgPass)) {
            HttpSession session = request.getSession(true);
            session.setAttribute(AuthenticationFilter.SESSION_AUTH_KEY, Boolean.TRUE);
            return Result.success(null, "登录成功");
        }
        return Result.error(401, "用户名或密码错误");
    }

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "销毁登录会话")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.success(null, "已登出");
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
