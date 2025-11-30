package com.cc.job.executor.compose.engine.worker;

import com.cc.job.executor.compose.engine.wrapper.WorkerWrapper;

/**
 * 对依赖的wrapper的封装
 * 
 * @author wuweifeng wrote on 2019-12-20
 * @author xiaozhao (migrated to compose executor)
 */
public class DependWrapper {
    /**
     * 依赖的WorkerWrapper
     */
    private WorkerWrapper<?, ?> dependWrapper;

    /**
     * 是否必须依赖（true=必须等待依赖完成，false=任意一个完成即可）
     */
    private boolean must = true;

    public DependWrapper(WorkerWrapper<?, ?> dependWrapper, boolean must) {
        this.dependWrapper = dependWrapper;
        this.must = must;
    }

    public WorkerWrapper<?, ?> getDependWrapper() {
        return dependWrapper;
    }

    public boolean isMust() {
        return must;
    }

    @Override
    public String toString() {
        return "DependWrapper{" +
                "dependWrapper=" + dependWrapper +
                ", must=" + must +
                '}';
    }
}
