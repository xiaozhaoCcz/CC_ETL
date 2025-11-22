package com.cc.job.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置类
 */
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    
    private static final Properties properties = new Properties();
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    
    static {
        // 尝试加载配置文件
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
            }
        } catch (IOException e) {
            logger.error("⚠ 加载配置文件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取后端服务地址
     */
    public static String getBaseUrl() {
        return properties.getProperty("api.base.url", DEFAULT_BASE_URL);
    }
    
    /**
     * 获取连接超时时间（秒）
     */
    public static int getConnectTimeout() {
        return Integer.parseInt(properties.getProperty("api.connect.timeout", "10"));
    }
    
    /**
     * 获取读取超时时间（秒）
     */
    public static int getReadTimeout() {
        return Integer.parseInt(properties.getProperty("api.read.timeout", "30"));
    }
}

