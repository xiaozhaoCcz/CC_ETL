package com.cc.job.admin.task.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储工具类
 * 
 * <p>用于admin模块删除文件（与executor-compose模块的FileStorageService配合使用）
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
@Component
public class FileStorageUtil {
    
    private static final Logger log = LoggerFactory.getLogger(FileStorageUtil.class);
    
    @Value("${cc-job.result.storage.file.basePath:${java.io.tmpdir}/cc-job-results}")
    private String basePath;
    
    @PostConstruct
    public void init() {
        if (basePath == null || basePath.isEmpty()) {
            String tempDir = System.getProperty("java.io.tmpdir");
            basePath = tempDir + "/cc-job-results";
        }
        log.info("[FileStorageUtil] 文件存储基础路径: {}", basePath);
    }
    
    /**
     * 删除文件
     * 
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.warn("[FileStorageUtil] 文件路径为空，无法删除");
            return false;
        }
        
        try {
            Path fullPath;
            // 判断是相对路径还是绝对路径
            if (Paths.get(filePath).isAbsolute()) {
                fullPath = Paths.get(filePath);
            } else {
                // 相对路径，相对于basePath
                fullPath = Paths.get(basePath, filePath);
            }
            
            if (!Files.exists(fullPath)) {
                log.debug("[FileStorageUtil] 文件不存在，无需删除: {}", fullPath);
                return true; // 文件不存在视为删除成功
            }
            
            boolean deleted = Files.deleteIfExists(fullPath);
            if (deleted) {
                log.info("[FileStorageUtil] 删除文件成功 - path: {}", filePath);
            } else {
                log.warn("[FileStorageUtil] 删除文件失败 - path: {}", filePath);
            }
            
            return deleted;
            
        } catch (IOException e) {
            log.error("[FileStorageUtil] 删除文件异常 - path: {}", filePath, e);
            return false;
        }
    }
}

