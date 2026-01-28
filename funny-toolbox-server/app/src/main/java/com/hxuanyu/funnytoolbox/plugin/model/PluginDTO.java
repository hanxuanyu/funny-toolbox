package com.hxuanyu.funnytoolbox.plugin.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 插件 DTO
 * 用于前端展示
 */
@Data
public class PluginDTO {
    private String id;
    private String name;
    private String version;
    private String description;
    private String author;
    private String icon;
    private String status;
    private LocalDateTime loadTime;
    private LocalDateTime startTime;
    private String frontendEntry;
    private String apiPrefix;
    private List<String> tags;
}