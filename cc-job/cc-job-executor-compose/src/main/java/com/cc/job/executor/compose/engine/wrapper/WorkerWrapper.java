package com.cc.job.executor.compose.engine.wrapper;

import com.cc.job.executor.compose.engine.callback.DefaultCallback;
import com.cc.job.executor.compose.engine.callback.ICallback;
import com.cc.job.executor.compose.engine.callback.IWorker;
import com.cc.job.executor.compose.engine.worker.DependWrapper;
import com.cc.job.executor.compose.engine.worker.WorkResult;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对每个worker及callback进行包装，一对一
 *
 * @author wuweifeng wrote on 2019-11-19.
 * @author xiaozhao (migrated to compose executor)
 */
public class WorkerWrapper<T, V> {
    /**
     * 该wrapper的唯一标识
     */
    private String id = UUID.randomUUID().toString();
    
    /**
     * worker将来要处理的param
     */
    private T param;
    
    private IWorker<T, V> worker;
    
    private ICallback<T, V> callback = new DefaultCallback<>();

    /**
     * 在自己后面的wrapper，如果没有，自己就是末尾；如果有一个，就是串行；如果有多个，有几个就需要开几个线程
     * <p>
     * -------2
     * 1
     * -------3
     * 如1后面有2、3
     */
    private List<WorkerWrapper<?, ?>> nextWrappers;
    
    /**
     * 依赖的wrappers，有2种情况：
     * 1:必须依赖的全部完成后，才能执行自己 
     * 2:依赖的任意一个、多个完成了，就可以执行自己
     * 通过must字段来控制是否依赖项必须完成
     * <p>
     * 1
     * -------3
     * 2
     * 1、2执行完毕后才能执行3
     */
    private List<DependWrapper> dependWrappers;
    
    /**
     * 标记该事件是否已经被处理过了，譬如已经超时返回false了，后续rpc又收到返回值了，则不再二次回调
     * 经试验,volatile并不能保证"同一毫秒"内,多线程对该值的修改和拉取
     * <p>
     * 1-finish, 2-error, 3-working
     */
    private AtomicInteger state = new AtomicInteger(0);
    
    /**
     * 也是个钩子变量，用来存临时的结果
     */
    private volatile WorkResult<V> workResult = WorkResult.defaultResult();
    
    /**
     * 是否在执行自己前，去校验nextWrapper的执行结果
     * <p>
     * 1   4
     * -------3
     * 2
     * 如这种在4执行前，可能3已经执行完毕了（被2执行完后触发的），那么4就没必要执行了。
     * 注意，该属性仅在nextWrapper数量<=1时有效，>1时的情况是不存在的
     */
    private volatile boolean needCheckNextWrapperResult = true;

    /**
     * 失败重试次数
     */
    private Integer retryCount;

    /**
     * 超时时间（秒）
     */
    private long timeout = 0;

    /**
     * 当前执行次数
     */
    private int count;

    public WorkerWrapper() {
    }

    public IWorker<T, V> getWorker() {
        return worker;
    }

    public ICallback<T, V> getCallback() {
        return callback;
    }

    public List<DependWrapper> getDependWrappers() {
        return dependWrappers;
    }

    public long getTimeout() {
        return timeout;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public WorkerWrapper<T, V> worker(IWorker<T, V> worker) {
        this.worker = worker;
        return this;
    }

    public WorkerWrapper<T, V> param(T param) {
        this.param = param;
        return this;
    }

    public WorkerWrapper<T, V> id(String id) {
        this.id = id;
        return this;
    }

    public WorkerWrapper<T, V> callback(ICallback<T, V> callback) {
        this.callback = callback;
        return this;
    }

    public WorkerWrapper<T, V> retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public WorkerWrapper<T, V> timeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public WorkerWrapper<T, V> next(WorkerWrapper<?, ?> wrapper) {
        return next(wrapper, true);
    }

    public WorkerWrapper<T, V> next(WorkerWrapper<?, ?> wrapper, boolean selfIsMust) {
        if (wrapper == null) {
            return this;
        }
        wrapper.addDepend(this, selfIsMust);
        this.addNext(wrapper);
        return this;
    }

    public WorkerWrapper<T, V> next(WorkerWrapper<?, ?>... wrappers) {
        if (wrappers == null) {
            return this;
        }
        for (WorkerWrapper<?, ?> wrapper : wrappers) {
            next(wrapper);
        }
        return this;
    }

    public WorkerWrapper<T, V> next(boolean selfIsMust, WorkerWrapper<?, ?>... wrappers) {
        if (wrappers == null) {
            return this;
        }
        for (WorkerWrapper<?, ?> wrapper : wrappers) {
            next(wrapper, selfIsMust);
        }
        return this;
    }

    public void setWorkResult(WorkResult<V> workResult) {
        this.workResult = workResult;
    }

    public WorkResult<V> getWorkResult() {
        return workResult;
    }

    public List<WorkerWrapper<?, ?>> getNextWrappers() {
        return nextWrappers;
    }

    public void setParam(T param) {
        this.param = param;
    }

    private void addDepend(WorkerWrapper<?, ?> workerWrapper, boolean must) {
        addDepend(new DependWrapper(workerWrapper, must));
    }

    private void addDepend(DependWrapper dependWrapper) {
        if (dependWrappers == null) {
            dependWrappers = new ArrayList<>();
        }
        //如果依赖的是重复的同一个，就不重复添加了
        for (DependWrapper wrapper : dependWrappers) {
            if (wrapper.equals(dependWrapper)) {
                return;
            }
        }
        dependWrappers.add(dependWrapper);
    }

    private void addNext(WorkerWrapper<?, ?> workerWrapper) {
        if (nextWrappers == null) {
            nextWrappers = new ArrayList<>();
        }
        //避免添加重复
        for (WorkerWrapper<?, ?> wrapper : nextWrappers) {
            if (workerWrapper.equals(wrapper)) {
                return;
            }
        }
        nextWrappers.add(workerWrapper);
    }

    public int getState() {
        return state.get();
    }

    public void setState(AtomicInteger state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public T getParam() {
        return param;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkerWrapper<?, ?> that = (WorkerWrapper<?, ?>) o;
        return needCheckNextWrapperResult == that.needCheckNextWrapperResult &&
                Objects.equals(param, that.param) &&
                Objects.equals(worker, that.worker) &&
                Objects.equals(callback, that.callback) &&
                Objects.equals(nextWrappers, that.nextWrappers) &&
                Objects.equals(dependWrappers, that.dependWrappers) &&
                Objects.equals(state, that.state) &&
                Objects.equals(workResult, that.workResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(param, worker, callback, nextWrappers, dependWrappers, state, workResult, needCheckNextWrapperResult);
    }
}
