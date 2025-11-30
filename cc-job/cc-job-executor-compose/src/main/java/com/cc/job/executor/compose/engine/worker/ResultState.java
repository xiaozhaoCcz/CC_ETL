package com.cc.job.executor.compose.engine.worker;

/**
 * 结果状态枚举
 * 
 * @author wuweifeng wrote on 2019-11-19.
 * @author xiaozhao (migrated to compose executor)
 */
public enum ResultState {
    /** 成功 */
    SUCCESS,
    /** 超时 */
    TIMEOUT,
    /** 异常 */
    EXCEPTION,
    /** 默认状态 */
    DEFAULT
}
