package com.cc.job.executor.infrastructure.constant;

/**
 * 执行器常量定义
 *
 * @author cc-job-team
 */
public final class ExecutorConstants {

    private ExecutorConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * HTTP 请求类型
     */
    public static final class HttpMethod {
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String PUT = "PUT";
        public static final String DELETE = "DELETE";

        private HttpMethod() {
        }
    }

    /**
     * SQL 类型
     */
    public static final class SqlType {
        public static final String SELECT = "SELECT";
        public static final String INSERT = "INSERT";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String CALL = "CALL";

        private SqlType() {
        }
    }

    /**
     * DataX 任务类型
     */
    public static final class DataxType {
        /** 全量同步 */
        public static final int FULL = 0;
        /** 增量同步 */
        public static final int INCREMENTAL = 1;
        /** 参数增量同步（多自定义参数，执行后刷新游标） */
        public static final int PARAM_INCREMENTAL = 2;

        private DataxType() {
        }
    }

    /**
     * 日期时间格式
     */
    public static final class DateFormat {
        public static final String DATETIME_1 = "yyyy-MM-dd HH:mm:ss";
        public static final String DATETIME_2 = "yyyy/MM/dd HH:mm:ss";
        public static final String DATE_1 = "yyyy-MM-dd";
        public static final String DATE_2 = "yyyy/MM/dd";

        private DateFormat() {
        }
    }

    /**
     * 数据源类型
     */
    public static final class DataSourceType {
        public static final String MYSQL = "com.mysql.cj.jdbc.Driver";
        public static final String ORACLE = "oracle.jdbc.driver.OracleDriver";
        public static final String POSTGRESQL = "org.postgresql.Driver";

        private DataSourceType() {
        }
    }

    /**
     * 错误消息
     */
    public static final class ErrorMessage {
        public static final String JOB_NOT_FOUND = "任务信息不存在";
        public static final String DATASOURCE_NOT_FOUND = "数据源不存在";
        public static final String SQL_EMPTY = "SQL语句为空";
        public static final String INVALID_SQL = "SQL语法错误";
        public static final String CONNECTION_FAILED = "数据库连接失败";
        public static final String EXECUTION_FAILED = "执行失败";

        private ErrorMessage() {
        }
    }
}

