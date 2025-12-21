package com.cc.job.gui.infrastructure.config;

import com.cc.job.gui.infrastructure.constant.GuiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 应用配置属性
 * 
 * <p>负责加载和管理应用配置
 *
 * @author cc-job-team
 */
public class ApplicationProperties {
    
    private static final Logger logger = LoggerFactory.getLogger(ApplicationProperties.class);
    
    private static final Properties properties = new Properties();
    private static volatile ApplicationProperties instance;
    
    static {
        loadProperties();
    }
    
    private ApplicationProperties() {
    }
    
    public static ApplicationProperties getInstance() {
        if (instance == null) {
            synchronized (ApplicationProperties.class) {
                if (instance == null) {
                    instance = new ApplicationProperties();
                }
            }
        }
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    private static void loadProperties() {
        try (InputStream input = ApplicationProperties.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("配置文件加载成功");
            } else {
                logger.warn("配置文件不存在，使用默认配置");
            }
        } catch (IOException e) {
            logger.error("加载配置文件失败", e);
        }
    }
    
    /**
     * API 配置
     */
    public static class Api {
        
        /**
         * 获取基础URL（单个，兼容旧代码）
         * @deprecated 使用 getBaseUrls() 获取多个地址
         */
        @Deprecated
        public static String getBaseUrl() {
            String urls = getBaseUrls();
            if (urls != null && !urls.isEmpty()) {
                // 返回第一个地址
                String[] addresses = urls.split(",");
                return addresses[0].trim();
            }
            return GuiConstants.Api.DEFAULT_BASE_URL;
        }
        
        /**
         * 获取基础URL列表（多个，以逗号分隔）
         * @return 多个URL地址，以逗号分隔
         */
        public static String getBaseUrls() {
            return properties.getProperty("api.base.url", 
                    GuiConstants.Api.DEFAULT_BASE_URL);
        }
        
        public static int getConnectTimeout() {
            return Integer.parseInt(properties.getProperty("api.connect.timeout", 
                    String.valueOf(GuiConstants.Api.DEFAULT_CONNECT_TIMEOUT)));
        }
        
        public static int getReadTimeout() {
            return Integer.parseInt(properties.getProperty("api.read.timeout", 
                    String.valueOf(GuiConstants.Api.DEFAULT_READ_TIMEOUT)));
        }
        
        public static int getWriteTimeout() {
            return Integer.parseInt(properties.getProperty("api.write.timeout", 
                    String.valueOf(GuiConstants.Api.DEFAULT_WRITE_TIMEOUT)));
        }
    }
    
    /**
     * WebSocket 配置
     */
    public static class WebSocket {
        
        public static String getPath() {
            return properties.getProperty("websocket.path", 
                    GuiConstants.WebSocket.DEFAULT_PATH);
        }
        
        public static int getHeartbeatInterval() {
            return Integer.parseInt(properties.getProperty("websocket.heartbeat.interval", 
                    String.valueOf(GuiConstants.WebSocket.HEARTBEAT_INTERVAL)));
        }
        
        public static int getReconnectInterval() {
            return Integer.parseInt(properties.getProperty("websocket.reconnect.interval", 
                    String.valueOf(GuiConstants.WebSocket.RECONNECT_INTERVAL)));
        }
        
        public static int getMaxReconnectAttempts() {
            return Integer.parseInt(properties.getProperty("websocket.max.reconnect.attempts", 
                    String.valueOf(GuiConstants.WebSocket.MAX_RECONNECT_ATTEMPTS)));
        }
    }
    
    /**
     * UI 配置
     */
    public static class Ui {
        
        public static double getWindowWidth() {
            return Double.parseDouble(properties.getProperty("ui.window.width", 
                    String.valueOf(GuiConstants.Ui.DEFAULT_WINDOW_WIDTH)));
        }
        
        public static double getWindowHeight() {
            return Double.parseDouble(properties.getProperty("ui.window.height", 
                    String.valueOf(GuiConstants.Ui.DEFAULT_WINDOW_HEIGHT)));
        }
        
        public static double getMinWindowWidth() {
            return Double.parseDouble(properties.getProperty("ui.window.min.width", 
                    String.valueOf(GuiConstants.Ui.MIN_WINDOW_WIDTH)));
        }
        
        public static double getMinWindowHeight() {
            return Double.parseDouble(properties.getProperty("ui.window.min.height", 
                    String.valueOf(GuiConstants.Ui.MIN_WINDOW_HEIGHT)));
        }
    }
}

