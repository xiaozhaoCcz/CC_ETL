package com.cc.job.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 任务调度管理配置属性
 *
 * @author cc-job
 * @since 2025-12-02
 */
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
    public static class TriggerPool {
        /**
         * 快速线程池最大线程数
         */
        private Fast fast = new Fast();

        /**
         * 慢速线程池最大线程数
         */
        private Slow slow = new Slow();

        public Fast getFast() {
            return fast;
        }

        public void setFast(Fast fast) {
            this.fast = fast;
        }

        public Slow getSlow() {
            return slow;
        }

        public void setSlow(Slow slow) {
            this.slow = slow;
        }

        public static class Fast {
            private Integer max = 200;

            public Integer getMax() {
                return max;
            }

            public void setMax(Integer max) {
                this.max = max;
            }
        }

        public static class Slow {
            private Integer max = 100;

            public Integer getMax() {
                return max;
            }

            public void setMax(Integer max) {
                this.max = max;
            }
        }
    }

    public String getI18n() {
        return i18n;
    }

    public void setI18n(String i18n) {
        this.i18n = i18n;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getLogpath() {
        return logpath;
    }

    public void setLogpath(String logpath) {
        this.logpath = logpath;
    }

    public Integer getLogretentiondays() {
        return logretentiondays;
    }

    public void setLogretentiondays(Integer logretentiondays) {
        this.logretentiondays = logretentiondays;
    }

    public TriggerPool getTriggerpool() {
        return triggerpool;
    }

    public void setTriggerpool(TriggerPool triggerpool) {
        this.triggerpool = triggerpool;
    }
}

