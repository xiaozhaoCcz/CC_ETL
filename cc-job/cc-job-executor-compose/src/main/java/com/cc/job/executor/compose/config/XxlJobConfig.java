package com.cc.job.executor.compose.config;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.xo.mapper.JobComposeMapper;
import com.cc.job.xo.model.entity.JobCompose;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.util.IpUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
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
public class XxlJobConfig extends XxlJobSpringExecutor{
    
    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

    @Value("${cc-job.job.admin.addresses}")
    private String adminAddresses;

    @Value("${cc-job.job.accessToken}")
    private String accessToken;

    @Value("${cc-job.job.executor.appname}")
    private String appname;

    @Value("${cc-job.job.executor.address}")
    private String address;

    @Value("${cc-job.job.executor.ip}")
    private String ip;

    @Value("${cc-job.job.executor.port}")
    private int executorPort;

    @Value("${cc-job.job.executor.logpath}")
    private String logPath;

    @Value("${cc-job.job.executor.logretentiondays}")
    private int logRetentionDays;


    /**
     * 初始化方法，相当于 @Bean(initMethod = "init")
     */
    @PostConstruct
    public void init() {
        logger.info(">>>>>>>>>>> xxl-job config init.");

        // 设置执行器属性
        this.setAdminAddresses(adminAddresses);
        this.setAppname(appname);
        this.setAddress(address);
        this.setIp(ip);
        this.setPort(executorPort);
        this.setAccessToken(accessToken);
        this.setLogPath(logPath);
        this.setLogRetentionDays(logRetentionDays);


        logger.info(">>>>>>>>>>> xxl-job executor initialized.");
    }



    @PreDestroy
    public void destroyXxlJobExecutor() {
        logger.info(">>>>>>>>>>> xxl-job executor destroying...");

        // TODO停止更新注册信息的线程

        logger.info(">>>>>>>>>>> xxl-job executor destroyed.");
    }

}
