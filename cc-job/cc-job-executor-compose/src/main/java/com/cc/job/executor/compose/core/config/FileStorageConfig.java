package com.cc.job.executor.compose.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性
 * 
 * <p>配置节点执行结果的文件存储相关参数
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
@Component
@ConfigurationProperties(prefix = "cc-job.result.storage.file")
public class FileStorageConfig {

    /**
     * 是否启用文件存储（默认true）
     */
    private boolean enabled = true;

    /**
     * 文件存储基础路径（绝对路径）
     * 默认：系统临时目录下的 cc-job-results
     */
    private String basePath;

    /**
     * 子路径格式（如：{taskGroupId}/{executionBatchId}）
     * 默认：{taskGroupId}/{executionBatchId}
     */
    private String subPath = "{taskGroupId}/{executionBatchId}";

    /**
     * 文件编码（默认UTF-8）
     */
    private String encoding = "UTF-8";

    public FileStorageConfig() {
        // 设置默认基础路径为系统临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        this.basePath = tempDir + "/cc-job-results";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getSubPath() {
        return subPath;
    }

    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}

