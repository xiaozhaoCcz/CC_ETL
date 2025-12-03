package com.cc.job.admin.task.executor.worker;


import com.cc.job.admin.task.executor.wrapper.WorkerWrapper;

/**
 * 对依赖的wrapper的封装
 * @author wuweifeng wrote on 2019-12-20
 * @version 1.0
 */
public class DependWrapper {
    private WorkerWrapper<?, ?> dependWrapper;

    private boolean must = true;

    public DependWrapper(WorkerWrapper<?, ?> dependWrapper, boolean must) {
        this.dependWrapper = dependWrapper;
        this.must = must;
    }


    public WorkerWrapper<?, ?> getDependWrapper() {
        return dependWrapper;
    }


    @Override
    public String toString() {
        return "DependWrapper{" +
                "dependWrapper=" + dependWrapper +
                ", must=" + must +
                '}';
    }
}
