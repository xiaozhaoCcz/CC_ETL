package com.cc.job.executor.compose.engine.callback;

import com.cc.job.executor.compose.engine.worker.WorkResult;

/**
 * 默认回调实现
 *
 * @author wuweifeng wrote on 2019-11-20
 * @author xiaozhao (migrated to compose executor)
 */
public class DefaultCallback<T, V> implements ICallback<T, V> {

    @Override
    public void result(boolean success, T param, WorkResult<V> workResult) {
        // 默认不做任何处理
    }
}
