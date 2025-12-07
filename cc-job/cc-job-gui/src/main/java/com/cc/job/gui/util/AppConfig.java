package com.cc.job.gui.util;

import com.cc.job.gui.infrastructure.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用配置类
 * 
 * @author xiaozhao
 * @deprecated 使用 {@link com.cc.job.gui.infrastructure.config.ApplicationProperties} 替代
 */
@Deprecated
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    
    /**
     * 获取后端服务地址
     */
    public static String getBaseUrl() {
        return ApplicationProperties.Api.getBaseUrl();
    }
    
    /**
     * 获取连接超时时间（秒）
     */
    public static int getConnectTimeout() {
        return ApplicationProperties.Api.getConnectTimeout();
    }
    
    /**
     * 获取读取超时时间（秒）
     */
    public static int getReadTimeout() {
        return ApplicationProperties.Api.getReadTimeout();
    }
}

