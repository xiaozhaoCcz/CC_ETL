package com.cc.job.executor.compose.core.service;

import com.cc.job.executor.compose.core.config.FileStorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件存储服务
 * 
 * <p>负责将节点执行结果保存到本地文件系统，以及从文件系统读取数据
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
@Component
public class FileStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    private final FileStorageConfig config;
    
    public FileStorageService(FileStorageConfig config) {
        this.config = config;
    }
    
    @PostConstruct
    public void init() {
        if (!config.isEnabled()) {
            logger.info("[FileStorage] 文件存储功能已禁用");
            return;
        }
        
        // 确保基础目录存在
        try {
            Path basePath = Paths.get(config.getBasePath());
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                logger.info("[FileStorage] 创建文件存储基础目录: {}", config.getBasePath());
            }
        } catch (IOException e) {
            logger.error("[FileStorage] 创建文件存储基础目录失败: {}", config.getBasePath(), e);
        }
    }
    
    /**
     * 生成文件路径
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @param jobId 节点任务ID
     * @return 文件路径（相对路径，相对于basePath）
     */
    public String getFilePath(Long taskGroupId, String executionBatchId, Long jobId) {
        // 构建子路径：{taskGroupId}/{executionBatchId}
        String subPath = config.getSubPath()
                .replace("{taskGroupId}", String.valueOf(taskGroupId))
                .replace("{executionBatchId}", executionBatchId);
        
        // 文件命名：{executionBatchId}_{jobId}.json
        String fileName = executionBatchId + "_" + jobId + ".json";
        
        return subPath + "/" + fileName;
    }
    
    /**
     * 获取完整文件路径（绝对路径）
     */
    private Path getFullPath(Long taskGroupId, String executionBatchId, Long jobId) {
        String relativePath = getFilePath(taskGroupId, executionBatchId, jobId);
        return Paths.get(config.getBasePath(), relativePath);
    }
    
    /**
     * 将JSON数据保存到本地文件
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID
     * @param jobId 节点任务ID
     * @param jsonData JSON数据字符串
     * @return 文件路径（相对路径），如果保存失败则返回null
     */
    public String saveToFile(Long taskGroupId, String executionBatchId, Long jobId, String jsonData) {
        if (!config.isEnabled()) {
            logger.debug("[FileStorage] 文件存储功能已禁用，跳过保存");
            return null;
        }
        
        if (jsonData == null || jsonData.isEmpty()) {
            logger.warn("[FileStorage] JSON数据为空，跳过保存 - jobId: {}", jobId);
            return null;
        }
        
        try {
            Path fullPath = getFullPath(taskGroupId, executionBatchId, jobId);
            
            // 确保父目录存在
            Path parentDir = fullPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.debug("[FileStorage] 创建目录: {}", parentDir);
            }
            
            // 写入文件
            Charset charset = Charset.forName(config.getEncoding());
            Files.write(fullPath, jsonData.getBytes(charset), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING, 
                    StandardOpenOption.WRITE);
            
            String relativePath = getFilePath(taskGroupId, executionBatchId, jobId);
            logger.info("[FileStorage] 保存文件成功 - jobId: {}, path: {}, size: {} bytes", 
                    jobId, relativePath, jsonData.length());
            
            return relativePath;
            
        } catch (IOException e) {
            logger.error("[FileStorage] 保存文件失败 - taskGroupId: {}, batchId: {}, jobId: {}", 
                    taskGroupId, executionBatchId, jobId, e);
            return null;
        }
    }
    
    /**
     * 从本地文件读取JSON数据
     * 
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return JSON数据字符串，如果读取失败则返回null
     */
    public String readFromFile(String filePath) {
        if (!config.isEnabled()) {
            logger.debug("[FileStorage] 文件存储功能已禁用，无法读取");
            return null;
        }
        
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("[FileStorage] 文件路径为空，无法读取");
            return null;
        }
        
        try {
            Path fullPath;
            // 判断是相对路径还是绝对路径
            if (Paths.get(filePath).isAbsolute()) {
                fullPath = Paths.get(filePath);
            } else {
                // 相对路径，相对于basePath
                fullPath = Paths.get(config.getBasePath(), filePath);
            }
            
            if (!Files.exists(fullPath)) {
                logger.warn("[FileStorage] 文件不存在: {}", fullPath);
                return null;
            }
            
            Charset charset = Charset.forName(config.getEncoding());
            byte[] bytes = Files.readAllBytes(fullPath);
            String jsonData = new String(bytes, charset);
            
            logger.debug("[FileStorage] 读取文件成功 - path: {}, size: {} bytes", 
                    filePath, bytes.length);
            
            return jsonData;
            
        } catch (IOException e) {
            logger.error("[FileStorage] 读取文件失败 - path: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 删除本地文件
     * 
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return 是否删除成功
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("[FileStorage] 文件路径为空，无法删除");
            return false;
        }
        
        try {
            Path fullPath;
            // 判断是相对路径还是绝对路径
            if (Paths.get(filePath).isAbsolute()) {
                fullPath = Paths.get(filePath);
            } else {
                // 相对路径，相对于basePath
                fullPath = Paths.get(config.getBasePath(), filePath);
            }
            
            if (!Files.exists(fullPath)) {
                logger.debug("[FileStorage] 文件不存在，无需删除: {}", fullPath);
                return true; // 文件不存在视为删除成功
            }
            
            boolean deleted = Files.deleteIfExists(fullPath);
            if (deleted) {
                logger.info("[FileStorage] 删除文件成功 - path: {}", filePath);
            } else {
                logger.warn("[FileStorage] 删除文件失败 - path: {}", filePath);
            }
            
            return deleted;
            
        } catch (IOException e) {
            logger.error("[FileStorage] 删除文件异常 - path: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return 文件是否存在
     */
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        try {
            Path fullPath;
            // 判断是相对路径还是绝对路径
            if (Paths.get(filePath).isAbsolute()) {
                fullPath = Paths.get(filePath);
            } else {
                // 相对路径，相对于basePath
                fullPath = Paths.get(config.getBasePath(), filePath);
            }
            
            return Files.exists(fullPath);
            
        } catch (Exception e) {
            logger.error("[FileStorage] 检查文件是否存在异常 - path: {}", filePath, e);
            return false;
        }
    }
    
    /**
     * 获取文件大小（字节）
     * 
     * @param filePath 文件路径（相对路径或绝对路径）
     * @return 文件大小，如果文件不存在则返回-1
     */
    public long getFileSize(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return -1;
        }
        
        try {
            Path fullPath;
            // 判断是相对路径还是绝对路径
            if (Paths.get(filePath).isAbsolute()) {
                fullPath = Paths.get(filePath);
            } else {
                // 相对路径，相对于basePath
                fullPath = Paths.get(config.getBasePath(), filePath);
            }
            
            if (!Files.exists(fullPath)) {
                return -1;
            }
            
            return Files.size(fullPath);
            
        } catch (IOException e) {
            logger.error("[FileStorage] 获取文件大小异常 - path: {}", filePath, e);
            return -1;
        }
    }
}

