package com.cc.job.executor.compose.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器配置
 * 
 * <p>配置任务组编排执行器作为 XXL-Job 的一个执行器节点
 * 
 * @author xiaozhao
 */
@Configuration
public class XxlJobConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${cc-job.admin.address}")
    private String adminAddresses;

    @Value("${cc-job.access-token:default_token}")
    private String accessToken;

    @Value("${cc-job.executor.appname:cc-job-executor-compose}")
    private String appname;

    @Value("${cc-job.executor.address:}")
    private String address;

    @Value("${cc-job.executor.ip:}")
    private String ip;

    @Value("${cc-job.executor.port:9999}")
    private int port;

    @Value("${cc-job.executor.logpath:/data/applogs/xxl-job/jobhandler}")
    private String logPath;

    @Value("${cc-job.executor.logretentiondays:30}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        logger.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return xxlJobSpringExecutor;
    }
}
