package com.cc.job.executor.compose.engine.callback;

import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;

import java.util.Map;

/**
 * 每个最小执行单元需要实现该接口
 *
 * @author wuweifeng wrote on 2019-11-19.
 * @author xiaozhao (migrated to compose executor)
 */
@FunctionalInterface
public interface IWorker<T, V> {
    /**
     * 在这里做耗时操作，如rpc请求、IO等
     *
     * @param object 参数
     * @param allWrappers 所有任务包装器映射
     * @return 执行结果
     */
    V action(T object, Map<String, WorkerWrapper> allWrappers);

    /**
     * 超时、异常时，返回的默认值
     *
     * @return 默认值
     */
    default V defaultValue() {
        return null;
    }
}
