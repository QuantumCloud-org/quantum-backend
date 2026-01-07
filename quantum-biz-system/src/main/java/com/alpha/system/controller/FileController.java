package com.alpha.system.controller;

import com.alpha.file.entity.FileInfo;
import com.alpha.file.service.FileStorageService;
import com.alpha.framework.entity.Result;
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
 */
@Slf4j
@Tag(name = "文件管理")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<FileInfo> upload(@RequestParam("file") MultipartFile file, @RequestParam(value = "path", required = false) String path) {
        FileInfo fileInfo = fileStorageService.upload(file, path);
        return Result.ok(fileInfo);
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件")
    public Result<List<FileInfo>> uploadBatch(@RequestParam("files") List<MultipartFile> files, @RequestParam(value = "path", required = false) String path) {
        List<FileInfo> fileInfos = fileStorageService.uploadBatch(files, path);
        return Result.ok(fileInfos);
    }

    @GetMapping("/download")
    @Operation(summary = "下载文件")
    public void download(@RequestParam("path") String path, @RequestParam(value = "name", required = false) String name, HttpServletResponse response) throws IOException {
        // 设置响应头
        String fileName = name != null ? name : path.substring(path.lastIndexOf("/") + 1);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        // 写入响应流
        try (InputStream is = fileStorageService.download(path);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    public Result<Boolean> delete(@RequestParam("path") String path) {
        boolean result = fileStorageService.delete(path);
        return Result.ok(result);
    }

    @PostMapping("/delete/batch")
    @Operation(summary = "批量删除文件")
    public Result<Integer> deleteBatch(@RequestBody List<String> paths) {
        int count = fileStorageService.deleteBatch(paths);
        return Result.ok(count);
    }

    @GetMapping("/exists")
    @Operation(summary = "检查文件是否存在")
    public Result<Boolean> exists(@RequestParam("path") String path) {
        boolean exists = fileStorageService.exists(path);
        return Result.ok(exists);
    }

    @GetMapping("/url")
    @Operation(summary = "获取文件访问URL")
    public Result<String> getUrl(@RequestParam("path") String path, @RequestParam(value = "expire", required = false, defaultValue = "3600") int expire) {
        String url = fileStorageService.getPresignedUrl(path, expire);
        return Result.ok(url);
    }
}