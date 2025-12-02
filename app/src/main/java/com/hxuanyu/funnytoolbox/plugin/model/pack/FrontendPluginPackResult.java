package com.hxuanyu.funnytoolbox.plugin.model.pack;

import lombok.Data;

@Data
public class FrontendPluginPackResult {
    private String pluginId;
    private String fileName;
    private long fileSize;
    private String downloadUrl; // /api/platform/plugins/pack/download/{fileName}
    private boolean imported;   // 是否已导入并启用
}
