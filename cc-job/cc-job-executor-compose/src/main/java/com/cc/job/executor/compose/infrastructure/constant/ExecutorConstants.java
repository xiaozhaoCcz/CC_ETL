package com.cc.job.executor.compose.infrastructure.constant;

/**
 * 执行器常量定义
 * 
 * <p>集中管理执行器相关的常量配置
 *
 * @author cc-job-team
 * @since 2025-12-02
 */
public final class ExecutorConstants {

    private ExecutorConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 任务执行结果常量
     */
    public static final class ExecutionResult {
        /** 任务执行成功 */
        public static final String SUCCESS = "SUCCESS";
        
        /** 任务失败需要重试 */
        public static final String FAIL_RETRY = "FAIL_RETRY";
        
        /** 任务失败但忽略继续执行 */
        public static final String DO_NOTHING = "DO_NOTHING";
        
        /** 任务失败完成（不重试） */
        public static final String FAIL_COMPLETE = "FAIL_COMPLETE";

        public static final String TIMEOUT = "TIMEOUT";

        private ExecutionResult() {
        }
    }

    /**
     * 任务状态码常量
     */
    public static final class TaskStatus {
        /** 失败 */
        public static final int FAILED = 0;
        
        /** 成功 */
        public static final int SUCCESS = 1;
        
        /** 执行中 */
        public static final int RUNNING = 2;
        
        /** 完成 */
        public static final int COMPLETED = 5;
        
        /** 预测运行时间 */
        public static final int PREDICTED = 9;

        private TaskStatus() {
        }
    }

    /**
     * 线程池配置常量
     */
    public static final class ThreadPool {
        /** 默认最大线程池大小 */
        public static final int DEFAULT_MAX_POOL_SIZE = 100;
        
        /** 最大队列容量 */
        public static final int MAX_QUEUE_CAPACITY = 10_000;
        
        /** 核心线程数 */
        public static final int CORE_POOL_SIZE = 10;
        
        /** 线程存活时间（秒） */
        public static final long KEEP_ALIVE_TIME = 60L;

        private ThreadPool() {
        }
    }

    /**
     * HTTP 配置常量
     */
    public static final class Http {
        /** 默认超时时间（毫秒） */
        public static final int DEFAULT_TIMEOUT = 30_000;
        
        /** 连接超时时间（毫秒） */
        public static final int CONNECT_TIMEOUT = 10_000;
        
        /** 读取超时时间（毫秒） */
        public static final int READ_TIMEOUT = 30_000;

        private Http() {
        }
    }

    /**
     * 任务编排配置常量
     */
    public static final class Orchestration {
        /** 默认任务超时时间（秒） */
        public static final long DEFAULT_TASK_TIMEOUT = 300L;
        
        /** 最大等待时间（毫秒） */
        public static final long MAX_WAIT_TIME = 5000L;
        
        /** 状态轮询间隔（毫秒） */
        public static final long STATUS_POLL_INTERVAL = 50L;
        
        /** 最大重试次数 */
        public static final int MAX_RETRY_COUNT = 3;

        private Orchestration() {
        }
    }

    /**
     * 日志相关常量
     */
    public static final class Logging {
        /** 日志分隔符 */
        public static final String LOG_SEPARATOR = "==========";
        
        /** 日志标签前缀 */
        public static final String TAG_ORCHESTRATOR = "[TaskOrchestrator]";
        public static final String TAG_TRIGGER = "[TaskTrigger]";
        public static final String TAG_MONITOR = "[TaskMonitor]";
        public static final String TAG_API_CLIENT = "[ApiClient]";

        private Logging() {
        }
    }

    /**
     * 错误消息常量
     */
    public static final class ErrorMessage {
        public static final String EMPTY_PARAM = "执行参数为空";
        public static final String TASK_GROUP_NOT_FOUND = "任务组信息不存在";
        public static final String NO_NODES = "任务组没有节点";
        public static final String EXECUTOR_NOT_REGISTERED = "执行器未注册";
        public static final String TRIGGER_FAILED = "任务触发失败";
        public static final String EXECUTION_TIMEOUT = "任务执行超时";
        public static final String DEPENDENCY_FAILED = "依赖任务执行失败";

        private ErrorMessage() {
        }
    }
}

