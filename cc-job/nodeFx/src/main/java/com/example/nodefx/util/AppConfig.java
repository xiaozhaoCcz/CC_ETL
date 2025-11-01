package com.example.nodefx.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置类
 */
public class AppConfig {
    
    private static final Properties properties = new Properties();
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    
    static {
        // 尝试加载配置文件
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                System.out.println("✓ 配置文件加载成功");
            } else {
                System.out.println("⚠ 未找到配置文件，使用默认配置");
            }
        } catch (IOException e) {
            System.err.println("⚠ 加载配置文件失败: " + e.getMessage());
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

