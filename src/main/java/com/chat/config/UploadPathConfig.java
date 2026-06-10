package com.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

@Component
public class UploadPathConfig {
    private File uploadDir;

    @Value("${upload.path:}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        if (uploadPath != null && !uploadPath.trim().isEmpty()) {
            uploadDir = new File(uploadPath.trim());
        } else {
            uploadDir = new File(System.getProperty("user.dir"), "upload");
        }
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IllegalStateException("无法创建上传目录: " + uploadDir.getAbsolutePath());
        }
    }

    public File getUploadDir() {
        return uploadDir;
    }

    public String getResourceLocation() {
        String path = uploadDir.getAbsolutePath().replace("\\", "/");
        if (!path.endsWith("/")) {
            path += "/";
        }
        return "file:" + path;
    }
}
