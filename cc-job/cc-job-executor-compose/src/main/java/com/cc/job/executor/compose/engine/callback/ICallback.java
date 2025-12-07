package com.cc.job.executor.compose.engine.callback;

import com.cc.job.executor.compose.engine.worker.WorkResult;

/**
 * 每个执行单元执行完毕后，会回调该接口
 * 需要监听执行结果的，实现该接口即可
 *
 * @author wuweifeng wrote on 2019-11-19.
 * @author xiaozhao (migrated to compose executor)
 */
@FunctionalInterface
public interface ICallback<T, V> {

    /**
     * 任务开始的监听
     * 
     * @param param 参数
     */
    default void begin(T param) {

    }

    /**
     * 耗时操作执行完毕后，就给value注入值
     * 
     * @param success 是否成功
     * @param param 参数
     * @param workResult 执行结果
     */
    void result(boolean success, T param, WorkResult<V> workResult);
}
