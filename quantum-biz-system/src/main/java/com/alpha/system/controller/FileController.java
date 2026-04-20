package com.alpha.system.controller;

import com.alpha.file.util.FileUtils;
import com.alpha.framework.entity.Result;
import com.alpha.logging.annotation.SystemLog;
import com.alpha.logging.enums.BusinessType;
import com.alpha.orm.entity.PageResult;
import com.alpha.security.annotation.RequiresRole;
import com.alpha.security.annotation.RequiresPermission;
import com.alpha.system.domain.SysFile;
import com.alpha.system.dto.request.SysFileQuery;
import com.alpha.system.service.ISysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件管理控制器
 * <p>
 * 对齐菜单权限 `system:file:*`, 通过 SysFileService 统一落库和生命周期管理。
 */
@Slf4j
@Tag(name = "文件管理")
@RestController
@RequestMapping("/system/file")
@RequiredArgsConstructor
public class FileController {

    private final ISysFileService sysFileService;

    @GetMapping("/list")
    @Operation(summary = "分页查询文件")
    @RequiresPermission("system:file:query")
    public Result<PageResult<SysFile>> list(SysFileQuery query) {
        return Result.ok(PageResult.of(sysFileService.selectFilePage(query)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询文件详情")
    @RequiresPermission("system:file:query")
    public Result<SysFile> getInfo(@PathVariable Long id) {
        return Result.ok(sysFileService.selectById(id));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    @SystemLog(title = "文件管理", businessType = BusinessType.INSERT, saveResult = false)
    @RequiresPermission("system:file:upload")
    public Result<SysFile> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(value = "bizType", required = false) String bizType,
                                  @RequestParam(value = "bizId", required = false) Long bizId,
                                  @RequestParam(value = "path", required = false) String path) {
        return Result.ok(sysFileService.upload(file, bizType, bizId, path));
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件")
    @SystemLog(title = "文件管理", businessType = BusinessType.INSERT, saveResult = false)
    @RequiresPermission("system:file:upload")
    public Result<List<SysFile>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                             @RequestParam(value = "bizType", required = false) String bizType,
                                             @RequestParam(value = "bizId", required = false) Long bizId,
                                             @RequestParam(value = "path", required = false) String path) {
        return Result.ok(sysFileService.uploadBatch(files, bizType, bizId, path));
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "按 ID 下载文件")
    @RequiresPermission("system:file:download")
    public void downloadById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysFile entity = sysFileService.selectById(id);
        streamFile(entity, response, true);
    }

    @GetMapping("/preview/{id}")
    @Operation(summary = "按 ID 预览文件（图片 / PDF 内联）")
    @RequiresPermission("system:file:query")
    public void previewById(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysFile entity = sysFileService.selectById(id);
        boolean inline = isInlinePreviewable(entity.getExtension());
        streamFile(entity, response, !inline);
    }

    @DeleteMapping("/{ids}")
    @Operation(summary = "批量删除文件")
    @SystemLog(title = "文件管理", businessType = BusinessType.DELETE)
    @RequiresPermission("system:file:remove")
    public Result<Integer> deleteByIds(@PathVariable List<Long> ids) {
        return Result.ok(sysFileService.deleteByIds(ids));
    }

    @DeleteMapping("/path")
    @Operation(summary = "按路径删除已登记文件（兼容接口，仅管理员）")
    @SystemLog(title = "文件管理", businessType = BusinessType.DELETE)
    @RequiresRole("admin")
    @RequiresPermission("system:file:remove")
    public Result<Boolean> deleteByPath(@RequestParam("path") String path) {
        return Result.ok(sysFileService.deleteByPath(path));
    }

    @GetMapping("/exists")
    @Operation(summary = "检查已登记文件是否存在（按路径，仅管理员）")
    @RequiresRole("admin")
    @RequiresPermission("system:file:query")
    public Result<Boolean> exists(@RequestParam("path") String path) {
        return Result.ok(sysFileService.existsByPath(path));
    }

    private void streamFile(SysFile entity, HttpServletResponse response, boolean asAttachment) throws IOException {
        String contentType = entity.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = FileUtils.getMimeType(entity.getExtension());
        }
        response.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        if (entity.getSize() != null && entity.getSize() > 0) {
            response.setContentLengthLong(entity.getSize());
        }

        String encodedName = URLEncoder.encode(entity.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = asAttachment ? "attachment" : "inline";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                disposition + "; filename*=UTF-8''" + encodedName);

        try (InputStream is = sysFileService.downloadById(entity.getId());
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        }
    }

    private boolean isInlinePreviewable(String extension) {
        return FileUtils.isImage(extension) || "pdf".equalsIgnoreCase(extension);
    }
}
