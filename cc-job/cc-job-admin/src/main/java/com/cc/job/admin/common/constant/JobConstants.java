package com.cc.job.admin.common.constant;

/**
 * 任务相关常量定义
 *
 * @author cc-job
 * @since 2025-12-02
 */
public final class JobConstants {

    private JobConstants() {
        // 防止实例化
    }

    /**
     * 任务执行结果常量
     */
    public static final class ExecutionResult {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAIL_RETRY = "FAIL_RETRY";
        public static final String DO_NOTHING = "DO_NOTHING";
        public static final String FAIL_COMPLETE = "FAIL_COMPLETE";

        private ExecutionResult() {
        }
    }

    /**
     * 任务类型常量
     */
    public static final class JobType {
        /** 普通任务 */
        public static final int NORMAL = 0;
        /** DataX任务 */
        public static final int DATAX = 1;
        /** 任务组 */
        public static final int GROUP = 2;

        private JobType() {
        }
    }

    /**
     * 任务状态常量
     */
    public static final class JobStatus {
        /** 停止 */
        public static final int STOPPED = 0;
        /** 运行中 */
        public static final int RUNNING = 1;
        /** 未运行 */
        public static final int NOT_RUNNING = -1;

        private JobStatus() {
        }
    }

    /**
     * 节点状态常量
     */
    public static final class NodeStatus {
        /** 失败 */
        public static final int FAILED = 0;
        /** 成功 */
        public static final int SUCCESS = 1;
        /** 运行中 */
        public static final int RUNNING = 2;
        /** 完成（用于通知前端） */
        public static final int COMPLETED = 5;
        /** 预测运行时间 */
        public static final int PREDICTED = 9;

        private NodeStatus() {
        }
    }

    /**
     * 节点标识常量
     */
    public static final class NodeFlag {
        /** 是节点 */
        public static final String YES = "Y";
        /** 不是节点 */
        public static final String NO = "N";

        private NodeFlag() {
        }
    }

    /**
     * 暂停状态常量
     */
    public static final class PauseStatus {
        /** 未暂停 */
        public static final int NOT_PAUSED = 0;
        /** 已暂停 */
        public static final int PAUSED = 1;

        private PauseStatus() {
        }
    }

    /**
     * 地址格式常量
     */
    public static final class AddressFormat {
        public static final String ADMIN_ADDRESS = "http://%s:%s/xxl-job-admin/";

        private AddressFormat() {
        }
    }

    /**
     * 节点类型映射常量
     */
    public static final class NodeTypeMapping {
        public static final String SQL = "custom-sql";
        public static final String API = "custom-api";
        public static final String BEAN = "custom-bean";
        public static final String GLUE_GROOVY = "custom-java";

        private NodeTypeMapping() {
        }
    }

    /**
     * 默认超时时间（毫秒）
     */
    public static final long DEFAULT_PAUSE_TIMEOUT = 5 * 60 * 1000; // 5分钟

    /**
     * 监听任务状态的轮询间隔（毫秒）
     */
    public static final long JOB_STATUS_POLL_INTERVAL = 50;
}

