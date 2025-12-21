package com.cc.job.executor.compose.config;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.util.IpUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


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

    @Value("${server.port:8500}")
    private int httpPort;

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

        removeExecutorRegistry();

        logger.info(">>>>>>>>>>> xxl-job executor destroyed.");

        try {
            super.destroy();
        } catch (Exception e) {
            logger.warn("销毁父类资源异常", e);
        }
    }

    /**
     * 停止时删除注册到 admin 的执行器信息
     */
    private void removeExecutorRegistry() {
        String executorIp = (ip != null && !ip.trim().isEmpty()) ? ip : IpUtil.getIp();
        String executorAddress = "http://" + executorIp + ":" + executorPort + "/";
        String executorServerAddress = "http://" + executorIp + ":" + httpPort + "/";

        Map<String, String> params = new HashMap<>();
        params.put("appName", appname);
        params.put("executorAddress", executorAddress);
        params.put("executorServerAddress", executorServerAddress);

        boolean removed = false;
        String[] adminAddressArray = adminAddresses.split(",");
        for (String adminAddress : adminAddressArray) {
            try {
                String baseUrl = adminAddress.trim();
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }
                String url = baseUrl + "api/removeRegistryValue";

                HttpResponse response = HttpRequest.post(url)
                        .header("Content-Type", "application/json")
                        .body(JSONUtil.toJsonStr(params))
                        .timeout(5000)
                        .execute();

                if (response.isOk()) {
                    removed = true;
                    logger.info("成功删除注册信息 - executorAddress: {}, httpPort: {}", executorAddress, httpPort);
                    break;
                } else {
                    logger.warn("删除注册信息失败 - adminAddress: {}, status: {}, body: {}",
                            adminAddress, response.getStatus(), response.body());
                }
            } catch (Exception e) {
                logger.warn("调用admin接口删除注册信息失败 - adminAddress: {}", adminAddress, e);
            }
        }

        if (!removed) {
            logger.warn("删除注册信息未成功，已尝试所有 adminAddresses");
        }
    }

}
