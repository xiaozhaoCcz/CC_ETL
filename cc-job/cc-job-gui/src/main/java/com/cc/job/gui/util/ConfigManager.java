package com.cc.job.gui.util;

import com.cc.job.gui.infrastructure.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 配置管理器
 * 用于保存和读取用户配置（后台地址等）
 * 
 * @author xiaozhao
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private static final String CONFIG_FILE_NAME = "cc-job-gui.properties";
    private static volatile ConfigManager instance;
    
    private final Properties properties;
    private final Path configFilePath;
    
    private ConfigManager() {
        this.properties = new Properties();
        // 配置文件保存在用户目录下
        String userHome = System.getProperty("user.home");
        this.configFilePath = Paths.get(userHome, ".cc-job-gui", CONFIG_FILE_NAME);
        
        // 确保目录存在
        try {
            Files.createDirectories(configFilePath.getParent());
        } catch (IOException e) {
            logger.error("创建配置目录失败", e);
        }
        
        // 加载配置
        loadConfig();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        // 先加载默认配置（从 application.properties）
        String defaultBaseUrl = ApplicationProperties.Api.getBaseUrl();
        properties.setProperty("api.base.url", defaultBaseUrl);
        
        // 然后从用户配置文件加载（如果存在）
        if (Files.exists(configFilePath)) {
            try (InputStream input = Files.newInputStream(configFilePath)) {
                Properties userProps = new Properties();
                userProps.load(input);
                
                // 合并用户配置
                for (String key : userProps.stringPropertyNames()) {
                    properties.setProperty(key, userProps.getProperty(key));
                }
                
                logger.info("用户配置文件加载成功: {}", configFilePath);
            } catch (IOException e) {
                logger.error("加载用户配置文件失败", e);
            }
        } else {
            logger.info("用户配置文件不存在，使用默认配置");
        }
    }
    
    /**
     * 保存配置
     */
    public void saveConfig() {
        try (OutputStream output = Files.newOutputStream(configFilePath)) {
            properties.store(output, "CC Job GUI Configuration");
            logger.info("配置保存成功: {}", configFilePath);
        } catch (IOException e) {
            logger.error("保存配置文件失败", e);
        }
    }
    
    /**
     * 获取后台地址
     */
    public String getBaseUrl() {
        return properties.getProperty("api.base.url", ApplicationProperties.Api.getBaseUrl());
    }
    
    /**
     * 设置后台地址
     */
    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            properties.setProperty("api.base.url", baseUrl.trim());
            saveConfig();
            
            // 更新 ApplicationProperties 中的配置（运行时）
            updateApplicationProperties();
        }
    }
    
    /**
     * 更新 ApplicationProperties 中的配置（运行时）
     */
    private void updateApplicationProperties() {
        try {
            // 使用反射更新 ApplicationProperties 中的 properties
            java.lang.reflect.Field field = ApplicationProperties.class.getDeclaredField("properties");
            field.setAccessible(true);
            Properties appProps = (Properties) field.get(null);
            if (appProps != null) {
                appProps.setProperty("api.base.url", properties.getProperty("api.base.url"));
                logger.info("运行时配置已更新: api.base.url = {}", properties.getProperty("api.base.url"));
            }
        } catch (Exception e) {
            logger.error("更新运行时配置失败", e);
        }
    }
    
    /**
     * 获取配置文件的完整路径
     */
    public String getConfigFilePath() {
        return configFilePath.toString();
    }
    
    /**
     * 获取配置属性
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * 设置配置属性
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }
}

