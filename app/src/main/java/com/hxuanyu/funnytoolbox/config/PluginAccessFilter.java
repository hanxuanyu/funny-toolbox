package com.hxuanyu.funnytoolbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxuanyu.funnytoolbox.common.Result;
import com.hxuanyu.funnytoolbox.plugin.core.PluginManager;
import com.hxuanyu.funnytoolbox.plugin.model.PluginStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 在插件被禁用时，拦截访问其后端 API 的请求，并返回明确提示信息。
 *
 * 优化：不再拦截静态资源路径 /plugins/**。静态资源是否可访问由是否注册映射决定：
 *  - 启用时注册，禁用时注销；未注册将自然返回 404，无需过滤器额外拦截。
 */
@Component
public class PluginAccessFilter extends OncePerRequestFilter {

    @Autowired
    private PluginManager pluginManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = getRequestPath(request);

        // 优先放行静态资源：/plugins/** 不做启用状态拦截（未注册时会返回 404）
        if (path.startsWith("/plugins/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 仅对后端 API 前缀做拦截控制
        Optional<String> resolved = pluginManager.resolvePluginIdByApiPath(path);
        String pluginId = resolved.orElse(null);
        if (pluginId != null && !pluginId.isEmpty()) {
            Optional<PluginStatus> statusOpt = pluginManager.getPluginStatus(pluginId);
            if (statusOpt.isEmpty() || statusOpt.get() != PluginStatus.ENABLED) {
                writeDisabledResponse(response, pluginId);
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

    private void writeDisabledResponse(HttpServletResponse response, String pluginId) throws IOException {
        response.setStatus(423); // Locked，表示资源被锁定/不可用
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.error(423, "插件[" + pluginId + "]已禁用或未安装，无法访问其接口或资源");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
