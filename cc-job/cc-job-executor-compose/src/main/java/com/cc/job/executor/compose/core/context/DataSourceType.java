package com.cc.job.executor.compose.core.context;

/**
 * 数据来源类型枚举
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
public enum DataSourceType {
    /**
     * 当前运行产生的数据
     */
    CURRENT_RUNNING,
    
    /**
     * 从数据库获取的历史数据
     */
    DATABASE
}

