package com.cc.job.executor.compose.config;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.enums.RegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    
    private Thread updateRegistryThread;
    private volatile boolean toStop = false;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        logger.info(">>>>>>>>>>> xxl-job config init.");
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(executorPort);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        // ⭐ 执行器启动后，持续尝试更新注册信息，添加HTTP端口信息
        // 采用与XXL-Job注册机制相同的持续循环方式，解决启动顺序问题
        startUpdateRegistryThread();

        return xxlJobSpringExecutor;
    }
    
    /**
     * 启动更新注册信息的线程（持续循环，直到成功）
     * 采用与ExecutorRegistryThread相同的机制
     */
    private void startUpdateRegistryThread() {
        updateRegistryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 首次延迟，等待执行器注册完成
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                // 持续循环，直到更新成功或线程被停止
                while (!toStop) {
                    try {
                        // 尝试更新注册信息
                        if (updateRegistryWithHttpPort()) {
                            logger.info("成功更新注册信息（HTTP端口），停止更新线程");
                            break; // 成功则退出循环
                        } else {
                            logger.debug("更新注册信息失败，将在{}秒后重试", RegistryConfig.BEAT_TIMEOUT);
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.debug("更新注册信息异常，将在{}秒后重试: {}", RegistryConfig.BEAT_TIMEOUT, e.getMessage());
                        }
                    }
                    
                    // 等待BEAT_TIMEOUT秒后重试（与executor注册周期一致）
                    try {
                        if (!toStop) {
                            TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                        }
                    } catch (InterruptedException e) {
                        if (!toStop) {
                            logger.warn(">>>>>>>>>>> xxl-job, executor-compose update registry thread interrupted, error msg:{}", e.getMessage());
                        }
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                logger.info(">>>>>>>>>>> xxl-job, executor-compose update registry thread destroy.");
            }
        }, "xxl-job, executor-compose UpdateRegistryThread");
        
        updateRegistryThread.setDaemon(true);
        updateRegistryThread.start();
    }
    
    /**
     * 停止更新注册信息线程
     */
    public void stopUpdateRegistryThread() {
        toStop = true;
        
        // interrupt and wait
        if (updateRegistryThread != null) {
            updateRegistryThread.interrupt();
            try {
                updateRegistryThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
    
    /**
     * 更新注册信息，将registryValue更新为JSON格式，包含执行器地址和HTTP端口
     * @return true表示成功，false表示失败
     */
    private boolean updateRegistryWithHttpPort() {
        try {
            // 构建执行器地址（Netty端口）
            String executorIp = (ip != null && !ip.trim().isEmpty()) ? ip : IpUtil.getIp();
            String executorAddress = "http://" + executorIp + ":" + executorPort + "/";
            
            // 构建JSON格式的registryValue
            Map<String, Object> registryValueMap = new HashMap<>();
            registryValueMap.put("executorAddress", executorAddress);
            registryValueMap.put("httpPort", httpPort);
            String newRegistryValue = JSONUtil.toJsonStr(registryValueMap);
            
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("registryGroup", "EXECUTOR");
            params.put("registryKey", appname);
            params.put("oldRegistryValue", executorAddress);
            params.put("newRegistryValue", newRegistryValue);
            
            // 调用admin接口更新注册信息
            // adminAddresses格式可能是 "http://127.0.0.1:8989/xxl-job-admin,http://127.0.0.1:8990/xxl-job-admin"
            String[] adminAddressArray = adminAddresses.split(",");
            for (String adminAddress : adminAddressArray) {
                try {
                    // 确保adminAddress以/结尾
                    String baseUrl = adminAddress.trim();
                    if (!baseUrl.endsWith("/")) {
                        baseUrl += "/";
                    }
                    String url = baseUrl + "api/updateRegistryValue";
                    
                    HttpResponse response = HttpRequest.post(url)
                            .header("Content-Type", "application/json")
                            .body(JSONUtil.toJsonStr(params))
                            .timeout(5000)
                            .execute();
                    
                    if (response.isOk()) {
                        logger.info("成功更新注册信息 - executorAddress: {}, httpPort: {}", executorAddress, httpPort);
                        return true; // 成功则返回true
                    } else {
                        logger.debug("更新注册信息失败 - adminAddress: {}, status: {}, body: {}", 
                                adminAddress, response.getStatus(), response.body());
                    }
                } catch (Exception e) {
                    logger.debug("调用admin接口更新注册信息失败 - adminAddress: {}, error: {}", adminAddress, e.getMessage());
                }
            }
            return false; // 所有admin地址都失败
        } catch (Exception e) {
            logger.error("更新注册信息异常", e);
            return false;
        }
    }
}
