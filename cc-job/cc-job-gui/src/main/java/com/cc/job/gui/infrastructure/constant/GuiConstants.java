package com.cc.job.gui.infrastructure.constant;

/**
 * GUI 应用常量定义
 *
 * @author cc-job-team
 */
public final class GuiConstants {

    private GuiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * API 配置常量
     */
    public static final class Api {
        /** 默认基础URL */
        public static final String DEFAULT_BASE_URL = "http://localhost:8080";
        
        /** 默认连接超时（秒） */
        public static final int DEFAULT_CONNECT_TIMEOUT = 10;
        
        /** 默认读取超时（秒） */
        public static final int DEFAULT_READ_TIMEOUT = 30;
        
        /** 默认写入超时（秒） */
        public static final int DEFAULT_WRITE_TIMEOUT = 30;

        private Api() {
        }
    }

    /**
     * WebSocket 配置常量
     */
    public static final class WebSocket {
        /** 默认WebSocket路径 */
        public static final String DEFAULT_PATH = "/ws/jobLog";
        
        /** 心跳间隔（秒） */
        public static final int HEARTBEAT_INTERVAL = 30;
        
        /** 重连间隔（秒） */
        public static final int RECONNECT_INTERVAL = 5;
        
        /** 最大重连次数 */
        public static final int MAX_RECONNECT_ATTEMPTS = 3;

        private WebSocket() {
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
        
        /** 执行中 */
        public static final int RUNNING = 2;
        
        /** 完成 */
        public static final int COMPLETED = 5;
        
        /** 预测时间 */
        public static final int PREDICTED = 9;

        private NodeStatus() {
        }
    }

    /**
     * UI 配置常量
     */
    public static final class Ui {
        /** 默认窗口宽度 */
        public static final double DEFAULT_WINDOW_WIDTH = 1200.0;
        
        /** 默认窗口高度 */
        public static final double DEFAULT_WINDOW_HEIGHT = 800.0;
        
        /** 最小窗口宽度 */
        public static final double MIN_WINDOW_WIDTH = 800.0;
        
        /** 最小窗口高度 */
        public static final double MIN_WINDOW_HEIGHT = 600.0;
        
        /** 默认节点宽度 */
        public static final double DEFAULT_NODE_WIDTH = 150.0;
        
        /** 默认节点高度 */
        public static final double DEFAULT_NODE_HEIGHT = 50.0;

        private Ui() {
        }
    }

    /**
     * 任务类型常量
     */
    public static final class TaskType {
        /** 普通任务 */
        public static final int NORMAL = 0;
        
        /** DataX任务 */
        public static final int DATAX = 1;
        
        /** 任务组 */
        public static final int GROUP = 2;

        private TaskType() {
        }
    }

    /**
     * 会话配置常量
     */
    public static final class Session {
        /** 令牌过期时间（毫秒） */
        public static final long TOKEN_EXPIRE_TIME = 24 * 60 * 60 * 1000L;
        
        /** 会话键 */
        public static final String SESSION_KEY = "user_session";
        
        /** 令牌键 */
        public static final String TOKEN_KEY = "auth_token";

        private Session() {
        }
    }

    /**
     * 日志标签常量
     */
    public static final class Logging {
        public static final String TAG_API = "[ApiClient]";
        public static final String TAG_SERVICE = "[Service]";
        public static final String TAG_VIEW = "[View]";
        public static final String TAG_WEBSOCKET = "[WebSocket]";
        public static final String TAG_SSE = "[SSE]";

        private Logging() {
        }
    }

    /**
     * 帮助与外部链接常量（用户手册、问题反馈、在线帮助、检查更新等）
     */
    public static final class Help {
        /** 用户手册 / 文档（与在线帮助可共用） */
        public static final String USER_MANUAL_URL = "https://github.com/xiaozhaoCcz/CC_ETL#readme";
        /** 报告问题（GitHub Issues） */
        public static final String REPORT_ISSUE_URL = "https://github.com/xiaozhaoCcz/CC_ETL/issues";
        /** 反馈建议（GitHub Issues 新建） */
        public static final String FEEDBACK_URL = "https://github.com/xiaozhaoCcz/CC_ETL/issues/new";
        /** 在线帮助（与用户手册一致，可后续改为独立文档站） */
        public static final String ONLINE_HELP_URL = "https://github.com/xiaozhaoCcz/CC_ETL#readme";
        /** 检查更新 - Releases 页面 */
        public static final String RELEASES_URL = "https://github.com/xiaozhaoCcz/CC_ETL/releases";

        private Help() {
        }
    }

    /**
     * 错误消息常量
     */
    public static final class ErrorMessage {
        public static final String CONNECTION_FAILED = "连接服务器失败";
        public static final String REQUEST_FAILED = "请求失败";
        public static final String INVALID_RESPONSE = "响应数据无效";
        public static final String LOGIN_REQUIRED = "请先登录";
        public static final String UNAUTHORIZED = "未授权访问";
        public static final String TOKEN_EXPIRED = "令牌已过期";

        private ErrorMessage() {
        }
    }
}

