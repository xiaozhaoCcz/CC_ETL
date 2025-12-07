package com.cc.job.admin.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 任务调度管理配置属性
 *
 * @author cc-job
 * @since 2025-12-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "xxl.job")
public class JobAdminProperties {

    /**
     * 国际化配置
     */
    private String i18n = "zh_CN";

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 日志路径
     */
    private String logpath;

    /**
     * 日志保留天数
     */
    private Integer logretentiondays = 30;

    /**
     * 触发器线程池配置
     */
    private TriggerPool triggerpool = new TriggerPool();

    /**
     * 触发器线程池配置
     */
    @Data
    public static class TriggerPool {
        /**
         * 快速线程池最大线程数
         */
        private Fast fast = new Fast();

        /**
         * 慢速线程池最大线程数
         */
        private Slow slow = new Slow();

        @Data
        public static class Fast {
            private Integer max = 200;
        }

        @Data
        public static class Slow {
            private Integer max = 100;
        }
    }
}

