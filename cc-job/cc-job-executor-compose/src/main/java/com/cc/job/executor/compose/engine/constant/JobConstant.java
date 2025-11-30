package com.cc.job.executor.compose.engine.constant;

/**
 * 任务执行常量
 * 
 * @author xiaozhao
 */
public interface JobConstant {
    /** 任务执行成功 */
    String SUCCESS = "SUCCESS";

    /** 任务失败需要重试 */
    String FAIL_RETRY = "FAIL_RETRY";

    /** 任务失败但忽略继续执行 */
    String DO_NOTHING = "DO_NOTHING";

    /** 任务失败完成（不重试） */
    String FAIL_COMPLETE = "FAIL_COMPLETE";

    /** Admin 地址模板 */
    String ADMIN_ADDRESS = "http://%s:%s/xxl-job-admin/";
}
