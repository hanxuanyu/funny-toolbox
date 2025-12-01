package com.hxuanyu.funnytoolbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxuanyu.funnytoolbox.common.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 简易认证过滤器：
 * - 从 HttpSession 中检查是否已登录；
 * - 仅拦截平台的“管理相关接口”（/api/platform/**），
 *   但放行获取插件列表（GET /api/platform/plugins）以及 /api/auth/** 登录相关接口；
 * - 其他插件提供的公开接口/资源不受影响。
 */
@Component
@Order(10)
public class AuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_AUTH_KEY = "LOGINED";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = getRequestPath(request);
        String method = request.getMethod();

        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 放行登录接口
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 仅针对平台管理接口做认证
        if (path.startsWith("/api/platform")) {
            // 放行：获取插件列表（GET /api/platform/plugins）
            if ("GET".equalsIgnoreCase(method) && "/api/platform/plugins".equals(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 其他 /api/platform/** 需要登录
            HttpSession session = request.getSession(false);
            boolean logined = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_AUTH_KEY));
            if (!logined) {
                writeUnauthorized(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getRequestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            return uri.substring(ctx.length());
        }
        return uri;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.error(401, "未登录或登录已过期");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
