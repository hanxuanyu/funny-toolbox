package com.hxuanyu.funnytoolbox.plugin.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 动态路由注册器
 */
@Component
@Slf4j
public class RouteRegistry {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    /**
     * 注册控制器的所有路由
     */
    public List<Object> registerController(String apiPrefix, Object controller) {
        List<Object> mappings = new ArrayList<>();

        Method[] methods = ReflectionUtils.getAllDeclaredMethods(controller.getClass());

        for (Method method : methods) {
            RequestMappingInfo mappingInfo = createMappingInfo(apiPrefix, method);

            if (mappingInfo != null) {
                handlerMapping.registerMapping(mappingInfo, controller, method);
                mappings.add(mappingInfo);

                log.info("Registered route: {} -> {}.{}",
                        mappingInfo.getPatternValues(),
                        controller.getClass().getSimpleName(),
                        method.getName());
            }
        }

        return mappings;
    }

    /**
     * 注销路由
     */
    public void unregisterMapping(Object mapping) {
        if (mapping instanceof RequestMappingInfo) {
            handlerMapping.unregisterMapping((RequestMappingInfo) mapping);
            log.info("Unregistered route: {}", ((RequestMappingInfo) mapping).getPatternValues());
        }
    }

    /**
     * 创建路由映射信息
     */
    private RequestMappingInfo createMappingInfo(String apiPrefix, Method method) {
        // 检查各种映射注解
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        GetMapping getMapping = AnnotationUtils.findAnnotation(method, GetMapping.class);
        PostMapping postMapping = AnnotationUtils.findAnnotation(method, PostMapping.class);
        PutMapping putMapping = AnnotationUtils.findAnnotation(method, PutMapping.class);
        DeleteMapping deleteMapping = AnnotationUtils.findAnnotation(method, DeleteMapping.class);
        PatchMapping patchMapping = AnnotationUtils.findAnnotation(method, PatchMapping.class);

        String[] paths = null;
        RequestMethod[] methods = null;

        if (requestMapping != null) {
            paths = requestMapping.value().length > 0 ? requestMapping.value() : requestMapping.path();
            methods = requestMapping.method();
        } else if (getMapping != null) {
            paths = getMapping.value().length > 0 ? getMapping.value() : getMapping.path();
            methods = new RequestMethod[]{RequestMethod.GET};
        } else if (postMapping != null) {
            paths = postMapping.value().length > 0 ? postMapping.value() : postMapping.path();
            methods = new RequestMethod[]{RequestMethod.POST};
        } else if (putMapping != null) {
            paths = putMapping.value().length > 0 ? putMapping.value() : putMapping.path();
            methods = new RequestMethod[]{RequestMethod.PUT};
        } else if (deleteMapping != null) {
            paths = deleteMapping.value().length > 0 ? deleteMapping.value() : deleteMapping.path();
            methods = new RequestMethod[]{RequestMethod.DELETE};
        } else if (patchMapping != null) {
            paths = patchMapping.value().length > 0 ? patchMapping.value() : patchMapping.path();
            methods = new RequestMethod[]{RequestMethod.PATCH};
        }

        if (paths == null || paths.length == 0) {
            return null;
        }

        // 添加 API 前缀
        String[] fullPaths = Arrays.stream(paths)
                .map(path -> {
                    path = path.startsWith("/") ? path : "/" + path;
                    return apiPrefix + path;
                })
                .toArray(String[]::new);

        // 构建 RequestMappingInfo
        return RequestMappingInfo
                .paths(fullPaths)
                .methods(methods)
                .build();
    }
}