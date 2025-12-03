package com.cc.job.admin.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件配置属性
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {

    /**
     * 发件人邮箱
     */
    private String from;

    /**
     * SMTP服务器地址
     */
    private String host;

    /**
     * SMTP服务器端口
     */
    private Integer port = 25;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 编码
     */
    private String defaultEncoding = "UTF-8";
}

