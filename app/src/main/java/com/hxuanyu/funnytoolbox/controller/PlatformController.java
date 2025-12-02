package com.hxuanyu.funnytoolbox.controller;

import com.hxuanyu.funnytoolbox.common.Result;
import com.hxuanyu.funnytoolbox.plugin.core.PluginManager;
import com.hxuanyu.funnytoolbox.plugin.model.PluginDTO;
import com.hxuanyu.funnytoolbox.plugin.registry.MenuRegistry;
import com.hxuanyu.funnytoolbox.plugin.model.pack.FrontendPluginPackMeta;
import com.hxuanyu.funnytoolbox.plugin.model.pack.FrontendPluginPackResult;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.hxuanyu.funnytoolbox.auth.AuthStatus;
import com.hxuanyu.funnytoolbox.config.AuthenticationFilter;

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
     * 获取当前会话的认证状态（与后台操作一致的访问控制）。
     * 注意：该接口位于 /api/platform/** 下，未登录时会被 AuthenticationFilter 拦截并返回 401。
     */
    @Operation(summary = "认证状态", description = "返回当前会话是否已认证及基本信息；未登录则在过滤器阶段返回 401")
    @GetMapping("/auth/status")
    public Result<AuthStatus> authStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean authenticated = session != null && Boolean.TRUE.equals(session.getAttribute(AuthenticationFilter.SESSION_AUTH_KEY));
        AuthStatus status = new AuthStatus();
        status.setAuthenticated(authenticated);
        status.setSessionId(session != null ? session.getId() : null);
        // 尝试读取用户名（若登录流程有设置）
        String user = session != null ? (String) session.getAttribute("USERNAME") : null;
        status.setUser(user);
        status.setServerTime(System.currentTimeMillis());
        return Result.success(status);
    }

    /**
     * 轻量探活：与后台操作一致的访问控制，用于前端快速判断是否需要跳转登录。
     * 未登录会在过滤器阶段返回 401。
     */
    @Operation(summary = "受保护探活", description = "HEAD 探活，未登录 401，已登录 200，无响应体")
    @RequestMapping(value = "/secure/ping", method = RequestMethod.HEAD)
    public ResponseEntity<Void> securePing() {
        return ResponseEntity.ok().build();
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
            description = "上传插件包并保存到平台插件目录，随后加载并自动启用该插件。支持 .jar（后端插件）与 .zip（前端-only 插件）。文件名建议使用 plugin-<插件ID>-<版本>.<jar|zip> 格式")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "安装成功，返回插件ID"),
            @ApiResponse(responseCode = "400", description = "请求不合法"),
            @ApiResponse(responseCode = "500", description = "服务端异常")
    })
    @PostMapping(value = "/plugins/install", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> installPlugin(
            @Parameter(description = "插件包文件（.jar 或 .zip）", required = true,
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
            if (fileName == null || !(fileName.toLowerCase().endsWith(".jar") || fileName.toLowerCase().endsWith(".zip"))) {
                return Result.error("仅支持上传 JAR 或 ZIP 插件包");
            }

            // 2. 目录与目标路径
            Path pluginDirectory = Paths.get(pluginDir);
            Files.createDirectories(pluginDirectory);
            Path targetPath = pluginDirectory.resolve(fileName).normalize();
            if (!targetPath.startsWith(pluginDirectory)) {
                return Result.error("非法文件名");
            }

            // 3. 先将上传内容保存到临时文件，再解析插件ID（兼容 JAR/ZIP）
            boolean isJar = fileName.toLowerCase().endsWith(".jar");
            boolean isZip = fileName.toLowerCase().endsWith(".zip");
            Path tempFile = Files.createTempFile("plugin-upload-", isJar ? ".jar" : ".zip");
            try (java.io.InputStream in = file.getInputStream()) {
                Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String newPluginId;
            if (isJar) {
                newPluginId = pluginManager.resolvePluginIdFromJar(tempFile.toFile());
            } else {
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(tempFile.toFile())) {
                    java.util.zip.ZipEntry entry = zip.getEntry("META-INF/plugin.yml");
                    if (entry == null) {
                        return Result.error("ZIP 中缺少 META-INF/plugin.yml");
                    }
                    try (InputStream is = zip.getInputStream(entry)) {
                        com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor desc = com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor.load(is);
                        newPluginId = desc.getId();
                    }
                }
            }
            if (isBlank(newPluginId)) {
                return Result.error("无法解析插件ID");
            }

            // 4. 冲突处理：若存在同名文件或同 ID 的插件，先卸载旧版本并删除旧 JAR
            // 4.1 同名文件
            if (Files.exists(targetPath)) {
                try {
                    // 尝试解析同名包的插件ID并卸载（兼容 JAR/ZIP）
                    String existedId = null;
                    if (targetPath.toString().toLowerCase().endsWith(".jar")) {
                        existedId = pluginManager.resolvePluginIdFromJar(targetPath.toFile());
                    } else if (targetPath.toString().toLowerCase().endsWith(".zip")) {
                        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(targetPath.toFile())) {
                            java.util.zip.ZipEntry entry = zip.getEntry("META-INF/plugin.yml");
                            if (entry != null) {
                                try (InputStream is = zip.getInputStream(entry)) {
                                    com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor desc = com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor.load(is);
                                    existedId = desc.getId();
                                }
                            }
                        }
                    }
                    try {
                        if (!isBlank(existedId)) {
                            pluginManager.unloadPlugin(existedId);
                        }
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

            // 4.2 同插件ID（可能文件名不同，且可能为 JAR 或 ZIP）
            try {
                String existedJarPath = pluginManager.tryFindPluginJar(newPluginId);
                String existedZipPath = null;
                try {
                    existedZipPath = pluginManager.tryFindPluginZip(newPluginId);
                } catch (Throwable t) {
                    // 低版本不支持公开 ZIP 查询时忽略
                }
                if (existedJarPath != null || existedZipPath != null) {
                    try {
                        pluginManager.unloadPlugin(newPluginId);
                    } catch (Exception unloadEx) {
                        log.warn("Unload old plugin by ID failed or not loaded: {} -> {}", newPluginId, unloadEx.getMessage());
                    }
                    if (existedJarPath != null) {
                        File existedJar = new File(existedJarPath);
                        if (!existedJar.toPath().equals(targetPath)) {
                            if (deleteWithRetry(existedJar, 5, 300)) {
                                log.info("Deleted existing plugin JAR of same ID ({}): {}", newPluginId, existedJarPath);
                            } else {
                                return Result.error("删除已有相同插件ID的旧版本失败，请稍后重试");
                            }
                        }
                    }
                    if (existedZipPath != null) {
                        File existedZip = new File(existedZipPath);
                        if (!existedZip.toPath().equals(targetPath)) {
                            if (deleteWithRetry(existedZip, 5, 300)) {
                                log.info("Deleted existing plugin ZIP of same ID ({}): {}", newPluginId, existedZipPath);
                            } else {
                                return Result.error("删除已有相同插件ID的旧版本失败，请稍后重试");
                            }
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

    // ===================== 前端-only 插件打包 =====================

    /**
     * 打包前端-only 插件（上传 dist 资源 + 元数据），产出 ZIP；可选直接导入。
     * 支持两种上传方式：
     * 1) 上传 dist.zip：使用参数 zip（单文件），其内容将被放入 meta.frontendBasePath 下；
     * 2) 批量文件上传：使用参数 files（多个）与 paths（与 files 一一对应的相对路径）。
     */
    @Operation(summary = "打包前端插件（ZIP）", description = "上传前端构建产物和元数据，后端生成符合平台规范的 ZIP 插件包，可选择立刻导入")
    @PostMapping(value = "/plugins/pack/frontend", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FrontendPluginPackResult> packFrontendPlugin(
            @Parameter(description = "插件元数据 JSON（作为 multipart 的 meta 字段，可为 application/json 或纯文本 JSON）", required = true)
            @RequestPart(value = "meta", required = false) String metaJson,
            @Parameter(description = "dist.zip（可选，和 files 二选一）")
            @RequestPart(value = "zip", required = false) MultipartFile distZip,
            @Parameter(description = "多个静态资源文件（可选，和 zip 二选一）")
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(description = "每个文件的相对路径，与 files 顺序一一对应，例如 js/app.js、index.html")
            @RequestParam(value = "paths", required = false) List<String> paths,
            @Parameter(description = "是否在打包完成后直接导入系统")
            @RequestParam(value = "import", defaultValue = "false") boolean importToSystem
    ) {
        try {
            // 解析元数据（兼容 meta 作为字符串 part 的情况；要求为 JSON 文本）
            if (isBlank(metaJson)) {
                return Result.error("缺少元数据 meta，且需为 JSON");
            }
            FrontendPluginPackMeta meta;
            try {
                meta = parseMeta(metaJson);
            } catch (Exception parseEx) {
                return Result.error("meta 解析失败：" + parseEx.getMessage());
            }
            // 校验元数据
            String pluginId = safe(meta.getId());
            if (isBlank(pluginId) || isBlank(meta.getName()) || isBlank(meta.getVersion())) {
                return Result.error("元数据缺少必填字段：id/name/version");
            }
            // 前端入口未提供时，默认使用 /index.html（无需前端显式传入）
            String entry = defaultIfBlank(meta.getFrontendEntry(), "/index.html");
            String basePath = defaultIfBlank(meta.getFrontendBasePath(), "/static");

            // 1) 创建临时工作目录
            Path workRoot = Files.createTempDirectory("frontend-pack-");
            Path metaInf = workRoot.resolve("META-INF");
            Files.createDirectories(metaInf);

            // 2) 准备静态资源根目录（移除前导斜杠）
            String normBase = basePath.startsWith("/") ? basePath.substring(1) : basePath;
            Path staticRoot = workRoot.resolve(normBase);
            Files.createDirectories(staticRoot);

            // 3) 写入 plugin.yml
            String yaml = buildPluginYaml(meta, basePath, entry);
            Files.writeString(metaInf.resolve("plugin.yml"), yaml);

            // 4) 注入静态资源
            if (distZip != null && !distZip.isEmpty()) {
                try (InputStream is = distZip.getInputStream(); ZipInputStream zis = new ZipInputStream(is)) {
                    unzipToDirectory(zis, staticRoot);
                }
            } else if (files != null && !files.isEmpty()) {
                if (paths == null || paths.size() != files.size()) {
                    return Result.error("paths 数量需要与 files 对应");
                }
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile f = files.get(i);
                    String rel = paths.get(i);
                    if (f == null || f.isEmpty() || isBlank(rel)) continue;
                    Path out = staticRoot.resolve(normalizeRelative(rel));
                    Files.createDirectories(out.getParent());
                    try (InputStream is = f.getInputStream()) {
                        Files.copy(is, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } else {
                return Result.error("必须提供 dist.zip 或 files");
            }

            // 5) 产出 ZIP 文件
            Path buildDir = Paths.get("data", "builds", "frontend-plugins");
            Files.createDirectories(buildDir);
            String fileBaseName = pluginId + "-" + meta.getVersion() + "-" + UUID.randomUUID().toString().substring(0, 8) + ".zip";
            Path outZip = buildDir.resolve(fileBaseName);
            zipDirectory(workRoot, outZip);

            // 6) 可选导入
            boolean imported = false;
            if (importToSystem) {
                Path pluginDirectory = Paths.get(pluginDir);
                Files.createDirectories(pluginDirectory);
                Path target = pluginDirectory.resolve(fileBaseName);
                Files.copy(outZip, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                // 加载并启用
                try {
                    pluginManager.loadPlugin(target.toFile());
                    pluginManager.enablePlugin(pluginId);
                    imported = true;
                } catch (Exception e) {
                    log.error("导入并启用 ZIP 插件失败: {}", pluginId, e);
                    return Result.error("导入并启用失败: " + e.getMessage());
                }
            }

            // 7) 返回结果
            FrontendPluginPackResult resp = new FrontendPluginPackResult();
            resp.setPluginId(pluginId);
            resp.setFileName(outZip.getFileName().toString());
            resp.setFileSize(Files.size(outZip));
            resp.setDownloadUrl("/api/platform/plugins/pack/download/" + urlEncode(outZip.getFileName().toString()));
            resp.setImported(imported);
            // 清理临时目录
            FileUtils.deleteDirectory(workRoot.toFile());
            return Result.success(resp);
        } catch (Exception ex) {
            log.error("打包前端-only 插件失败", ex);
            return Result.error("打包失败: " + ex.getMessage());
        }
    }

    /**
     * 下载打包生成的 ZIP。
     */
    @Operation(summary = "下载打包产物", description = "下载 /data/builds/frontend-plugins 目录下的打包 ZIP 文件")
    @GetMapping("/plugins/pack/download/{file}")
    public ResponseEntity<byte[]> downloadPackedZip(@PathVariable("file") String fileName) {
        try {
            String safeName = new File(fileName).getName();
            Path zipPath = Paths.get("data", "builds", "frontend-plugins", safeName).normalize();
            Path base = Paths.get("data", "builds", "frontend-plugins").toAbsolutePath().normalize();
            if (!zipPath.toAbsolutePath().normalize().startsWith(base) || !Files.exists(zipPath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(zipPath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(bytes.length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception ex) {
            log.error("下载打包产物失败: {}", fileName, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 将已打包生成的前端-only 插件 ZIP（位于 data/builds/frontend-plugins 下）导入到系统插件目录并加载/启用。
     */
    @Operation(summary = "导入已打包的前端插件ZIP", description = "根据打包结果文件名，将 ZIP 复制到插件目录并完成加载（可选启用）")
    @PostMapping("/plugins/pack/import")
    public Result<FrontendPluginPackResult> importPackedZip(
            @Parameter(description = "打包生成的 ZIP 文件名（位于 data/builds/frontend-plugins 下）", required = true)
            @RequestParam("fileName") String fileName,
            @Parameter(description = "是否在导入后立即启用该插件")
            @RequestParam(value = "enable", defaultValue = "true") boolean enable
    ) {
        try {
            if (isBlank(fileName)) {
                return Result.error("fileName 不能为空");
            }
            String safeName = new File(fileName).getName();
            Path buildBase = Paths.get("data", "builds", "frontend-plugins").toAbsolutePath().normalize();
            Path src = buildBase.resolve(safeName).normalize();
            if (!src.startsWith(buildBase) || !Files.exists(src)) {
                return Result.error("找不到指定的打包文件");
            }

            // 读取 plugin.yml 以获取插件ID
            String pluginId;
            long size = Files.size(src);
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(src.toFile())) {
                java.util.zip.ZipEntry entry = zip.getEntry("META-INF/plugin.yml");
                if (entry == null) {
                    return Result.error("ZIP 中缺少 META-INF/plugin.yml");
                }
                try (InputStream is = zip.getInputStream(entry)) {
                    com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor desc = com.hxuanyu.funnytoolbox.plugin.model.PluginDescriptor.load(is);
                    pluginId = desc.getId();
                }
            }
            if (isBlank(pluginId)) {
                return Result.error("无法从 plugin.yml 解析插件ID");
            }

            // 复制到插件目录
            Path pluginDirectory = Paths.get(pluginDir).toAbsolutePath().normalize();
            Files.createDirectories(pluginDirectory);
            Path target = pluginDirectory.resolve(safeName).normalize();
            if (!target.startsWith(pluginDirectory)) {
                return Result.error("非法文件名");
            }

            Files.copy(src, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // 若插件已加载，尝试先卸载
            try {
                var statusOpt = pluginManager.getPluginStatus(pluginId);
                if (statusOpt.isPresent()) {
                    try { pluginManager.unloadPlugin(pluginId); } catch (Exception ignore) { }
                }
            } catch (Exception ignore) {}

            // 加载并可选启用
            try {
                pluginManager.loadPlugin(target.toFile());
                if (enable) {
                    pluginManager.enablePlugin(pluginId);
                }
            } catch (Exception e) {
                log.error("导入 ZIP 插件失败: {}", pluginId, e);
                return Result.error("导入失败: " + e.getMessage());
            }

            FrontendPluginPackResult resp = new FrontendPluginPackResult();
            resp.setPluginId(pluginId);
            resp.setFileName(target.getFileName().toString());
            resp.setFileSize(size);
            resp.setDownloadUrl("/api/platform/plugins/pack/download/" + urlEncode(safeName));
            resp.setImported(true);
            return Result.success(resp);
        } catch (Exception ex) {
            log.error("导入打包 ZIP 失败: {}", fileName, ex);
            return Result.error("导入失败: " + ex.getMessage());
        }
    }

    // ===================== 辅助方法 =====================

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String defaultIfBlank(String s, String def) { return isBlank(s) ? def : s; }
    private static String safe(String s) { return s == null ? null : s.trim(); }

    private static String normalizeRelative(String rel) {
        String n = rel.replace("\\", "/");
        while (n.startsWith("/")) n = n.substring(1);
        if (n.contains("..")) throw new IllegalArgumentException("非法相对路径");
        return n;
    }

    private static String urlEncode(String s) {
        try { return URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception e) { return s; }
    }

    private static void unzipToDirectory(ZipInputStream zis, Path destRoot) throws IOException {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) { continue; }
            String name = entry.getName();
            if (name.contains("..")) { continue; }
            Path out = destRoot.resolve(normalizeRelative(name));
            Files.createDirectories(out.getParent());
            try (OutputStream os = Files.newOutputStream(out)) {
                zis.transferTo(os);
            }
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private static FrontendPluginPackMeta parseMeta(String metaJson) throws JsonProcessingException {
        return JSON.readValue(metaJson, FrontendPluginPackMeta.class);
    }

    private static void zipDirectory(Path sourceDir, Path outZip) throws IOException {
        try (OutputStream fos = Files.newOutputStream(outZip); ZipOutputStream zos = new ZipOutputStream(fos)) {
            Path root = sourceDir.toAbsolutePath().normalize();
            Files.walk(root).forEach(p -> {
                try {
                    if (Files.isDirectory(p)) return;
                    String rel = root.relativize(p.toAbsolutePath().normalize()).toString().replace("\\", "/");
                    ZipEntry e = new ZipEntry(rel);
                    zos.putNextEntry(e);
                    Files.copy(p, zos);
                    zos.closeEntry();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            });
        }
    }

    /**
     * 生成插件描述符 YAML（frontend-only，无 mainClass）。
     */
    private static String buildPluginYaml(FrontendPluginPackMeta meta, String basePath, String entry) {
        StringBuilder sb = new StringBuilder();
        // 顶层字段
        sb.append("id: ").append(escapeYaml(meta.getId())).append('\n');
        sb.append("name: ").append(escapeYaml(meta.getName())).append('\n');
        sb.append("version: ").append(escapeYaml(meta.getVersion())).append('\n');
        if (!isBlank(meta.getDescription())) sb.append("description: ").append(escapeYaml(meta.getDescription())).append('\n');
        if (!isBlank(meta.getAuthor())) sb.append("author: ").append(escapeYaml(meta.getAuthor())).append('\n');

        // icon（对象形式）
        if (meta.getIconMeta() != null) {
            var icon = meta.getIconMeta();
            sb.append("icon:\n");
            if (icon.getType() != null) {
                String typeStr;
                switch (icon.getType()) {
                    case EMOJI: typeStr = "emoji"; break;
                    case URL: typeStr = "url"; break;
                    case SVG: typeStr = "svg"; break;
                    case FONT_AWESOME: typeStr = "font_awesome"; break;
                    case MATERIAL: typeStr = "material"; break;
                    default: typeStr = "custom"; break;
                }
                sb.append("  type: ").append(typeStr).append('\n');
            }
            if (!isBlank(icon.getValue())) sb.append("  value: ").append(escapeYaml(icon.getValue())).append('\n');
            if (!isBlank(icon.getColor())) sb.append("  color: ").append(escapeYaml(icon.getColor())).append('\n');
            if (!isBlank(icon.getStyle())) sb.append("  style: ").append(escapeYaml(icon.getStyle())).append('\n');
        }

        // frontend
        sb.append("frontend:\n");
        sb.append("  entry: ").append(escapeYaml(entry)).append('\n');
        sb.append("  basePath: ").append(escapeYaml(basePath)).append('\n');

        return sb.toString();
    }

    private static String escapeYaml(String s) {
        if (s == null) return "";
        // 简单包一层双引号，并转义内部引号与换行
        String v = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return '"' + v + '"';
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
