package com.hxuanyu.funnytoolbox.controller;

import com.hxuanyu.funnytoolbox.common.Result;
import com.hxuanyu.funnytoolbox.plugin.core.PluginManager;
import com.hxuanyu.funnytoolbox.plugin.model.PluginDTO;
import com.hxuanyu.funnytoolbox.plugin.registry.MenuRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
    @GetMapping("/plugins")
    public Result<List<PluginDTO>> listPlugins() {
        return Result.success(pluginManager.getAllPlugins());
    }

    /**
     * 获取菜单列表
     */
    @GetMapping("/menus")
    public Result<List<MenuRegistry.MenuItem>> getMenus() {
        return Result.success(menuRegistry.getMenus());
    }

    /**
     * 上传并安装插件
     */
    @PostMapping("/plugins/install")
    public Result<String> installPlugin(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.endsWith(".jar")) {
                return Result.error("只能上传 JAR 文件");
            }

            // 2. 保存文件
            Path pluginDirectory = Paths.get(pluginDir);
            Files.createDirectories(pluginDirectory);

            Path targetPath = pluginDirectory.resolve(fileName);
            file.transferTo(targetPath.toFile());

            log.info("Plugin file saved: {}", targetPath);

            // 3. 加载插件
            pluginManager.loadPlugin(targetPath.toFile());

            // 4. 自动启用
            String pluginId = extractPluginId(fileName);
            pluginManager.enablePlugin(pluginId);

            return Result.success(pluginId, "插件安装成功");

        } catch (Exception e) {
            log.error("Failed to install plugin", e);
            return Result.error("安装失败: " + e.getMessage());
        }
    }

    /**
     * 启用插件
     */
    @PostMapping("/plugins/{id}/enable")
    public Result<Void> enablePlugin(@PathVariable String id) {
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
    @PostMapping("/plugins/{id}/disable")
    public Result<Void> disablePlugin(@PathVariable String id) {
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
    @DeleteMapping("/plugins/{id}")
    public Result<Void> uninstallPlugin(@PathVariable String id) {
        try {
            // 1. 查找插件 JAR
            String jarPath = findPluginJar(id);

            // 2. 卸载插件
            pluginManager.unloadPlugin(id);

            // 3. 删除 JAR 文件
            if (jarPath != null) {
                FileUtils.forceDelete(new File(jarPath));
                log.info("Plugin JAR deleted: {}", jarPath);
            }

            return Result.success(null, "插件已卸载");

        } catch (Exception e) {
            log.error("Failed to uninstall plugin: {}", id, e);
            return Result.error("卸载失败: " + e.getMessage());
        }
    }

    /**
     * 重新加载插件
     */
    @PostMapping("/plugins/{id}/reload")
    public Result<Void> reloadPlugin(@PathVariable String id) {
        try {
            pluginManager.reloadPlugin(id);
            return Result.success(null, "插件已重新加载");
        } catch (Exception e) {
            log.error("Failed to reload plugin: {}", id, e);
            return Result.error("重新加载失败: " + e.getMessage());
        }
    }

    /**
     * 从文件名提取插件 ID
     */
    private String extractPluginId(String fileName) {
        // 假设文件名格式: plugin-secret-capsule-1.0.0.jar
        return fileName
                .replaceFirst("^plugin-", "")
                .replaceFirst("-[\\d.]+\\.jar$", "");
    }

    /**
     * 查找插件 JAR 文件路径
     */
    private String findPluginJar(String pluginId) {
        try {
            return Files.list(Paths.get(pluginDir))
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> p.getFileName().toString().contains(pluginId))
                    .findFirst()
                    .map(Path::toString)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to find plugin JAR: {}", pluginId, e);
            return null;
        }
    }
}
