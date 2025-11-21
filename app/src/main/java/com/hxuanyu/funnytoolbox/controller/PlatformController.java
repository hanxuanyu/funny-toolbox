package com.hxuanyu.funnytoolbox.controller;

import com.hxuanyu.funnytoolbox.common.Result;
import com.hxuanyu.funnytoolbox.plugin.core.PluginManager;
import com.hxuanyu.funnytoolbox.plugin.model.PluginDTO;
import com.hxuanyu.funnytoolbox.plugin.registry.MenuRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 平台管理接口
 */
@RestController
@RequestMapping("/api/platform")
@Slf4j
@Tag(name = "平台管理", description = "平台插件、菜单等管理接口")
public class PlatformController {

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private MenuRegistry menuRegistry;

    @Value("${platform.plugin.dir:./plugins}")
    private String pluginDir;

    /**
     * 获取所有插件列表
     */
    @Operation(summary = "获取插件列表", description = "返回当前平台已加载的所有插件的基础信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PluginDTO.class)))
    })
    @GetMapping("/plugins")
    public Result<List<PluginDTO>> listPlugins() {
        return Result.success(pluginManager.getAllPlugins());
    }

    /**
     * 获取菜单列表
     */
    @Operation(summary = "获取菜单列表", description = "返回当前平台聚合的菜单项")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/menus")
    public Result<List<MenuRegistry.MenuItem>> getMenus() {
        return Result.success(menuRegistry.getMenus());
    }

    /**
     * 上传并安装插件
     */
    @Operation(summary = "上传并安装插件",
            description = "上传插件JAR并保存到平台插件目录，随后加载并自动启用该插件。文件名建议使用 plugin-<插件ID>-<版本>.jar 格式")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "安装成功，返回插件ID"),
            @ApiResponse(responseCode = "400", description = "请求不合法"),
            @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping(value = "/plugins/install", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> installPlugin(
            @Parameter(description = "插件JAR文件", required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        try {
            // 1. 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String fileName = file.getOriginalFilename();
            // 规范化文件名，避免路径穿越及容器附带路径的情况
            if (fileName != null) {
                fileName = new File(fileName).getName();
            }
            if (fileName == null || !fileName.endsWith(".jar")) {
                return Result.error("只能上传 JAR 文件");
            }

            // 2. 目录与目标路径
            Path pluginDirectory = Paths.get(pluginDir);
            Files.createDirectories(pluginDirectory);
            Path targetPath = pluginDirectory.resolve(fileName).normalize();
            if (!targetPath.startsWith(pluginDirectory)) {
                return Result.error("非法文件名");
            }

            // 3. 先将上传内容保存到临时文件，再解析插件ID
            Path tempFile = Files.createTempFile("plugin-upload-", ".jar");
            try (java.io.InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String newPluginId = pluginManager.resolvePluginIdFromJar(tempFile.toFile());

            // 4. 冲突处理：若存在同名文件或同 ID 的插件，先卸载旧版本并删除旧 JAR
            // 4.1 同名文件
            if (Files.exists(targetPath)) {
                try {
                    // 尝试解析同名 JAR 的插件ID并卸载
                    String existedId = pluginManager.resolvePluginIdFromJar(targetPath.toFile());
                    try {
                        pluginManager.unloadPlugin(existedId);
                    } catch (Exception unloadEx) {
                        log.warn("Unload old plugin by file name failed or not loaded: {} -> {}", existedId, unloadEx.getMessage());
                    }
                } catch (Exception ignore) {
                    // 解析失败也不影响删除文件
                }

                // 卸载后删除（带重试，避免短暂占用）
                if (deleteWithRetry(targetPath.toFile(), 5, 300)) {
                    log.info("Deleted existing plugin file with same name: {}", targetPath);
                } else {
                    return Result.error("删除已存在的同名插件文件失败，请稍后重试");
                }
            }

            // 4.2 同插件ID（可能文件名不同）
            try {
                String existedJarPath = pluginManager.tryFindPluginJar(newPluginId);
                if (existedJarPath != null) {
                    try {
                        pluginManager.unloadPlugin(newPluginId);
                    } catch (Exception unloadEx) {
                        log.warn("Unload old plugin by ID failed or not loaded: {} -> {}", newPluginId, unloadEx.getMessage());
                    }
                    // 如果旧 JAR 与目标路径不同，也需要删除
                    File existedJar = new File(existedJarPath);
                    if (!existedJar.toPath().equals(targetPath)) {
                        if (deleteWithRetry(existedJar, 5, 300)) {
                            log.info("Deleted existing plugin JAR of same ID ({}): {}", newPluginId, existedJarPath);
                        } else {
                            return Result.error("删除已有相同插件ID的旧版本失败，请稍后重试");
                        }
                    }
                }
            } catch (Exception findEx) {
                // 忽略查找/删除中的异常，不阻断后续安装
                log.warn("Failed during old plugin cleanup for {}: {}", newPluginId, findEx.getMessage());
            }

            // 5. 将临时文件复制为目标文件
            Files.copy(tempFile, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Plugin file saved: {}", targetPath);

            // 6. 加载并启用插件
            pluginManager.loadPlugin(targetPath.toFile());
            pluginManager.enablePlugin(newPluginId);

            // 7. 清理临时文件（带重试）
            try {
                deleteWithRetry(tempFile.toFile(), 3, 200);
            } catch (Exception ignore) {
            }

            return Result.success(newPluginId, "插件安装成功");

        } catch (Exception e) {
            log.error("Failed to install plugin", e);
            return Result.error("安装失败: " + e.getMessage());
        }
    }

    /**
     * 启用插件
     */
    @Operation(summary = "启用插件", description = "根据插件ID启用插件")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "启用成功"),
            @ApiResponse(responseCode = "500", description = "启用失败")
    })
    @PostMapping("/plugins/{id}/enable")
    public Result<Void> enablePlugin(
            @Parameter(name = "id", description = "插件ID") @PathVariable("id") String id) {
        try {
            pluginManager.enablePlugin(id);
            return Result.success(null, "插件已启用");
        } catch (Exception e) {
            log.error("Failed to enable plugin: {}", id, e);
            return Result.error("启用失败: " + e.getMessage());
        }
    }

    /**
     * 禁用插件
     */
    @Operation(summary = "禁用插件", description = "根据插件ID禁用插件")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "禁用成功"),
            @ApiResponse(responseCode = "500", description = "禁用失败")
    })
    @PostMapping("/plugins/{id}/disable")
    public Result<Void> disablePlugin(
            @Parameter(name = "id", description = "插件ID") @PathVariable("id") String id) {
        try {
            pluginManager.disablePlugin(id);
            return Result.success(null, "插件已禁用");
        } catch (Exception e) {
            log.error("Failed to disable plugin: {}", id, e);
            return Result.error("禁用失败: " + e.getMessage());
        }
    }

    /**
     * 卸载插件
     */
    @Operation(summary = "卸载插件", description = "卸载并删除插件JAR文件")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "卸载成功"),
            @ApiResponse(responseCode = "500", description = "卸载失败")
    })
    @DeleteMapping("/plugins/{id}")
    public Result<Void> uninstallPlugin(
            @Parameter(name = "id", description = "插件ID") @PathVariable("id") String id) {
        try {
            // 1. 查找插件 JAR（通过读取每个 JAR 的 plugin.yml 比对 ID）
            String jarPath = pluginManager.tryFindPluginJar(id);

            // 2. 卸载插件
            pluginManager.unloadPlugin(id);

            // 3. 删除 JAR 文件
            if (jarPath != null) {
                File jarFile = new File(jarPath);
                if (deleteWithRetry(jarFile, 6, 300)) {
                    log.info("Plugin JAR deleted: {}", jarPath);
                } else {
                    return Result.error("删除插件文件失败，可能仍被占用，请稍后重试");
                }
            }

            return Result.success(null, "插件已卸载");

        } catch (Exception e) {
            log.error("Failed to uninstall plugin: {}", id, e);
            return Result.error("卸载失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件（带重试）。
     * 在 Windows 上，JAR 文件可能在关闭类加载器后短时间仍被占用。
     */
    private boolean deleteWithRetry(File file, int attempts, long sleepMs) {
        if (file == null) return true;
        IOException lastEx = null;
        for (int i = 0; i < attempts; i++) {
            try {
                if (!file.exists()) return true;
                Files.deleteIfExists(file.toPath());
                if (!file.exists()) return true;
            } catch (IOException e) {
                lastEx = e;
            }
            // 尝试强删
            try {
                if (file.exists()) {
                    FileUtils.forceDelete(file);
                }
                if (!file.exists()) return true;
            } catch (IOException e) {
                lastEx = e;
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (lastEx != null) {
            log.warn("Failed to delete file after retries: {} -> {}", file.getAbsolutePath(), lastEx.getMessage());
        }
        return !file.exists();
    }

    /**
     * 重新加载插件
     */
    @Operation(summary = "重新加载插件", description = "根据ID先卸载后加载，刷新插件状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "重载成功"),
            @ApiResponse(responseCode = "500", description = "重载失败")
    })
    @PostMapping("/plugins/{id}/reload")
    public Result<Void> reloadPlugin(
            @Parameter(name = "id", description = "插件ID") @PathVariable("id") String id) {
        try {
            pluginManager.reloadPlugin(id);
            return Result.success(null, "插件已重新加载");
        } catch (Exception e) {
            log.error("Failed to reload plugin: {}", id, e);
            return Result.error("重新加载失败: " + e.getMessage());
        }
    }

    // 说明：不再从文件名中提取插件ID，统一由 PluginManager 读取 JAR 内的 plugin.yml 确定插件ID
}
