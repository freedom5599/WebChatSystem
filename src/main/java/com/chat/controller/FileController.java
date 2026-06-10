package com.chat.controller;

import com.chat.common.Result;
import com.chat.config.UploadPathConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {
    @Autowired
    private UploadPathConfig uploadPathConfig;

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        String ext = resolveExtension(file);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        File destFile = new File(uploadPathConfig.getUploadDir(), fileName);
        try {
            if (!destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }
            file.transferTo(destFile.getAbsoluteFile());
            return Result.success("/upload/" + fileName);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("png")) {
                return ".png";
            }
            if (contentType.contains("gif")) {
                return ".gif";
            }
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                return ".jpg";
            }
            if (contentType.startsWith("image/")) {
                return ".jpg";
            }
            if (contentType.startsWith("audio/")) {
                return ".webm";
            }
        }
        return "";
    }
}
