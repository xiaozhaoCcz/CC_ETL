package com.cc.job.admin.common.constant;

/**
 * 系统级常量定义
 *
 * @author cc-job
 * @since 2025-12-02
 */
public final class SystemConstants {

    private SystemConstants() {
        // 防止实例化
    }

    /**
     * 国际化支持的语言
     */
    public static final class I18n {
        public static final String ZH_CN = "zh_CN";
        public static final String ZH_TC = "zh_TC";
        public static final String EN = "en";
        public static final String DEFAULT = ZH_CN;

        private I18n() {
        }
    }

    /**
     * 默认配置值
     */
    public static final class DefaultConfig {
        /** 触发器快速线程池最小值 */
        public static final int MIN_TRIGGER_POOL_FAST = 200;
        /** 触发器慢速线程池最小值 */
        public static final int MIN_TRIGGER_POOL_SLOW = 100;
        /** 日志保留天数最小值 */
        public static final int MIN_LOG_RETENTION_DAYS = 7;
        /** 日志保留天数关闭值 */
        public static final int LOG_RETENTION_DISABLED = -1;

        private DefaultConfig() {
        }
    }

    /**
     * HTTP 状态码
     */
    public static final class HttpStatus {
        public static final int SUCCESS = 200;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_SERVER_ERROR = 500;

        private HttpStatus() {
        }
    }

    /**
     * 日期时间格式
     */
    public static final class DateFormat {
        public static final String DATETIME = "yyyy-MM-dd HH:mm:ss";
        public static final String DATE = "yyyy-MM-dd";
        public static final String TIME = "HH:mm:ss";

        private DateFormat() {
        }
    }
}

