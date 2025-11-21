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
 * 在插件被禁用时，拦截访问其 API 与静态资源的请求，并返回明确提示信息。
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

        // 仅拦截插件相关路径：/plugins/** 或 匹配到某插件的 API 前缀
        String pluginId = null;

        if (path.startsWith("/plugins/")) {
            // 提取 /plugins/{id}/... 的 {id}
            String sub = path.substring("/plugins/".length());
            int idx = sub.indexOf('/') ;
            pluginId = (idx >= 0) ? sub.substring(0, idx) : sub; // 允许直接访问 /plugins/{id}
        } else {
            Optional<String> resolved = pluginManager.resolvePluginIdByApiPath(path);
            pluginId = resolved.orElse(null);
        }

        if (pluginId != null && !pluginId.isEmpty()) {
            // 若插件不存在或未启用，则直接给出明确提示
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
