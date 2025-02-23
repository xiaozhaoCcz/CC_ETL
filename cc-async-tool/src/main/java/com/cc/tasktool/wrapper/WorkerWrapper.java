package com.cc.tasktool.wrapper;

import com.cc.tasktool.callback.DefaultCallback;
import com.cc.tasktool.callback.ICallback;
import com.cc.tasktool.callback.IWorker;
import com.cc.tasktool.exception.SkippedException;
import com.cc.tasktool.executor.timer.SystemClock;
import com.cc.tasktool.worker.DependWrapper;
import com.cc.tasktool.worker.ResultState;
import com.cc.tasktool.worker.WorkResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对每个worker及callback进行包装，一对一
 *
 * @author wuweifeng wrote on 2019-11-19.
 */
public class WorkerWrapper<T, V> {
    /**
     * 该wrapper的唯一标识
     */
    private String id =UUID.randomUUID().toString();
    /**
     * worker将来要处理的param
     */
    private T param;
    private IWorker<T, V> worker;
    private ICallback<T, V> callback = new DefaultCallback<>();
    /**
     * 在自己后面的wrapper，如果没有，自己就是末尾；如果有一个，就是串行；如果有多个，有几个就需要开几个线程</p>
     * -------2
     * 1
     * -------3
     * 如1后面有2、3
     */
    private List<WorkerWrapper<?, ?>> nextWrappers;
    /**
     * 依赖的wrappers，有2种情况，1:必须依赖的全部完成后，才能执行自己 2:依赖的任何一个、多个完成了，就可以执行自己
     * 通过must字段来控制是否依赖项必须完成
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
     * 该map存放所有wrapper的id和wrapper映射
     */
    private Map<String, WorkerWrapper> forParamUseWrappers;
    /**
     * 也是个钩子变量，用来存临时的结果
     */
    private volatile WorkResult<V> workResult = WorkResult.defaultResult();
    /**
     * 是否在执行自己前，去校验nextWrapper的执行结果<p>
     * 1   4
     * -------3
     * 2
     * 如这种在4执行前，可能3已经执行完毕了（被2执行完后触发的），那么4就没必要执行了。
     * 注意，该属性仅在nextWrapper数量<=1时有效，>1时的情况是不存在的
     */
    private volatile boolean needCheckNextWrapperResult = true;

    private Integer retryCount;

    private long timeout = 0;

    private static final int FINISH = 1;
    private static final int ERROR = 2;
    private static final int WORKING = 3;
    private static final int INIT = 0;

    public WorkerWrapper(){

    }

    /**
     * 存储强依赖于自己的wrapper集合
     */
    private Set<WorkerWrapper<?, ?>> selfIsMustSet;


    public WorkerWrapper<T, V> worker(IWorker<T, V> worker) {
        this.worker = worker;
        return this;
    }

    public WorkerWrapper<T, V> param(T param) {
        this.param = param;
        return this;
    }

    public  WorkerWrapper<T, V> id(String id) {
        this.id = id;
        return this;
    }

    public WorkerWrapper<T, V> needCheckNextWrapperResult(boolean needCheckNextWrapperResult) {
        this.needCheckNextWrapperResult = needCheckNextWrapperResult;
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

    public WorkerWrapper<T, V> depend(WorkerWrapper<?, ?>... wrappers) {
        if (wrappers == null) {
            return this;
        }
        for (WorkerWrapper<?, ?> wrapper : wrappers) {
            depend(wrapper);
        }
        return this;
    }

    public WorkerWrapper<T, V> depend(WorkerWrapper<?, ?> wrapper) {
        return depend(wrapper, true);
    }

    public WorkerWrapper<T, V> depend(WorkerWrapper<?, ?> wrapper, boolean isMust) {
        if (wrapper == null) {
            return this;
        }
        DependWrapper dependWrapper = new DependWrapper(wrapper, isMust);
        dependWrapper.getDependWrapper().addNext(this);
        this.addDepend(dependWrapper);
        return this;
    }

    public WorkerWrapper<T, V> next(WorkerWrapper<?, ?> wrapper) {
        return next(wrapper, true);
    }

    public WorkerWrapper<T, V> next(WorkerWrapper<?, ?> wrapper, boolean selfIsMust) {
        if (wrapper == null) {
            return this;
        }
        wrapper.addDepend(this,selfIsMust);
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

    public WorkerWrapper<T, V> next(boolean selfIsMust,WorkerWrapper<?, ?>... wrappers) {
        if (wrappers == null) {
            return this;
        }
        for (WorkerWrapper<?, ?> wrapper : wrappers) {
            next(wrapper,selfIsMust);
        }
        return this;
    }
    /**
     * 开始工作
     * fromWrapper代表这次work是由哪个上游wrapper发起的
     */
    private void work(ExecutorService executorService, WorkerWrapper fromWrapper, long remainTime, Map<String, WorkerWrapper> forParamUseWrappers) {
        this.forParamUseWrappers = forParamUseWrappers;
        //将自己放到所有wrapper的集合里去
        forParamUseWrappers.put(id, this);
        long now = SystemClock.now();
        //总的已经超时了，就快速失败，进行下一个
        if (remainTime <= 0) {
            fastFail(INIT, null);
            beginNext(executorService, now, remainTime);
            return;
        }
        //如果自己已经执行过了。
        //可能有多个依赖，其中的一个依赖已经执行完了，并且自己也已开始执行或执行完毕。当另一个依赖执行完毕，又进来该方法时，就不重复处理了
        if (getState() == FINISH || getState() == ERROR) {
            beginNext(executorService, now, remainTime);
            return;
        }

        //如果在执行前需要校验nextWrapper的状态
        if (needCheckNextWrapperResult) {
            //如果自己的next链上有已经出结果或已经开始执行的任务了，自己就不用继续了
            if (!checkNextWrapperResult()) {
                fastFail(INIT, new SkippedException());
                beginNext(executorService, now, remainTime);
                return;
            }
        }

        //如果没有任何依赖，说明自己就是第一批要执行的
        if (dependWrappers == null || dependWrappers.size() == 0) {
            fire();
            beginNext(executorService, now, remainTime);
            return;
        }

        /*如果有前方依赖，存在两种情况
         一种是前面只有一个wrapper。即 A  ->  B
        一种是前面有多个wrapper。A C D ->   B。需要A、C、D都完成了才能轮到B。但是无论是A执行完，还是C执行完，都会去唤醒B。
        所以需要B来做判断，必须A、C、D都完成，自己才能执行 */

        //只有一个依赖
        if (dependWrappers.size() == 1) {
            doDependsOneJob(fromWrapper);
            beginNext(executorService, now, remainTime);
        } else {
            //有多个依赖时
            doDependsJobs(executorService, dependWrappers, fromWrapper, now, remainTime);
        }

    }


    public void work(ExecutorService executorService, long remainTime, Map<String, WorkerWrapper> forParamUseWrappers) {
        work(executorService, null, remainTime, forParamUseWrappers);
    }

    /**
     * 总控制台超时，停止所有任务
     */
    public void stopNow() {
        if (getState() == INIT || getState() == WORKING) {
            fastFail(getState(), null);
        }
    }

    /**
     * 判断自己下游链路上，是否存在已经出结果的或已经开始执行的
     * 如果没有返回true，如果有返回false
     */
    private boolean checkNextWrapperResult() {
        //如果自己就是最后一个，或者后面有并行的多个，就返回true
        if (nextWrappers == null || nextWrappers.size() != 1) {
            return getState() == INIT;
        }
        WorkerWrapper nextWrapper = nextWrappers.get(0);
        boolean state = nextWrapper.getState() == INIT;
        //继续校验自己的next的状态
        return state && nextWrapper.checkNextWrapperResult();
    }

    /**
     * 进行下一个任务
     */
    private void beginNext(ExecutorService executorService, long now, long remainTime) {
        //花费的时间
        long costTime = SystemClock.now() - now;
        if (nextWrappers == null) {
            return;
        }
        if (nextWrappers.size() == 1) {
            nextWrappers.get(0).work(executorService, WorkerWrapper.this, remainTime - costTime, forParamUseWrappers);
            return;
        }
        CompletableFuture[] futures = new CompletableFuture[nextWrappers.size()];
        for (int i = 0; i < nextWrappers.size(); i++) {
            int finalI = i;
            futures[i] = CompletableFuture.runAsync(() -> nextWrappers.get(finalI)
                    .work(executorService, WorkerWrapper.this, remainTime - costTime, forParamUseWrappers), executorService);
        }
        try {
            CompletableFuture.allOf(futures).get(remainTime - costTime, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDependsOneJob(WorkerWrapper dependWrapper) {
        if (ResultState.TIMEOUT == dependWrapper.getWorkResult().getResultState()) {
            workResult = defaultResult();
            fastFail(INIT, null);
        } else if (ResultState.EXCEPTION == dependWrapper.getWorkResult().getResultState()) {
            workResult = defaultExResult(dependWrapper.getWorkResult().getEx());
            fastFail(INIT, null);
        } else {
            //前面任务正常完毕了，该自己了
            fire();
        }
    }

    private synchronized void doDependsJobs(ExecutorService executorService, List<DependWrapper> dependWrappers, WorkerWrapper fromWrapper, long now, long remainTime) {
        //如果当前任务已经完成了，依赖的其他任务拿到锁再进来时，不需要执行下面的逻辑了。
        if (getState() != INIT) {
            return;
        }
        boolean nowDependIsMust = false;
        //创建必须完成的上游wrapper集合
        Set<DependWrapper> mustWrapper = new HashSet<>();
        for (DependWrapper dependWrapper : dependWrappers) {
            if (dependWrapper.isMust()) {
                mustWrapper.add(dependWrapper);
            }
            if (dependWrapper.getDependWrapper().equals(fromWrapper)) {
                nowDependIsMust = dependWrapper.isMust();
            }
        }

        //如果全部是不必须的条件，那么只要到了这里，就执行自己。
        if (mustWrapper.size() == 0) {
            if (ResultState.TIMEOUT == fromWrapper.getWorkResult().getResultState()) {
                fastFail(INIT, null);
            } else {
                fire();
            }
            beginNext(executorService, now, remainTime);
            return;
        }

        //如果存在需要必须完成的，且fromWrapper不是必须的，就什么也不干
        if (!nowDependIsMust) {
            return;
        }

        //如果fromWrapper是必须的
        boolean existNoFinish = false;
        boolean hasError = false;
        //先判断前面必须要执行的依赖任务的执行结果，如果有任何一个失败，那就不用走action了，直接给自己设置为失败，进行下一步就是了
        for (DependWrapper dependWrapper : mustWrapper) {
            WorkerWrapper workerWrapper = dependWrapper.getDependWrapper();
            WorkResult tempWorkResult = workerWrapper.getWorkResult();
            //为null或者isWorking，说明它依赖的某个任务还没执行到或没执行完
            if (workerWrapper.getState() == INIT || workerWrapper.getState() == WORKING) {
                existNoFinish = true;
                break;
            }
            if (ResultState.TIMEOUT == tempWorkResult.getResultState()) {
                workResult = defaultResult();
                hasError = true;
                break;
            }
            if (ResultState.EXCEPTION == tempWorkResult.getResultState()) {
                workResult = defaultExResult(workerWrapper.getWorkResult().getEx());
                hasError = true;
                break;
            }

        }
        //只要有失败的
        if (hasError) {
            fastFail(INIT, null);
            beginNext(executorService, now, remainTime);
            return;
        }

        //如果上游都没有失败，分为两种情况，一种是都finish了，一种是有的在working
        //都finish的话
        if (!existNoFinish) {
            //上游都finish了，进行自己
            fire();
            beginNext(executorService, now, remainTime);
            return;
        }
    }

    /**
     * 执行自己的job.具体的执行是在另一个线程里,但判断阻塞超时是在work线程
     */
    private void fire() {
        //阻塞取结果
        workResult = workerDoJob();
    }

    /**
     * 快速失败
     */
    private boolean fastFail(int expect, Exception e) {
        //试图将它从expect状态,改成Error
        if (!compareAndSetState(expect, ERROR)) {
            return false;
        }

        //尚未处理过结果
        if (checkIsNullResult()) {
            if (e == null) {
                workResult = defaultResult();
            } else {
                workResult = defaultExResult(e);
            }
        }

        callback.result(false, param, workResult);
        return true;
    }

    private int count = 0;

    /**
     * 具体的单个worker执行任务
     */
    private WorkResult<V> workerDoJob() {
        //避免重复执行
        if (!checkIsNullResult()) {
            return workResult;
        }
        try {
            //如果已经不是init状态了，说明正在被执行或已执行完毕。这一步很重要，可以保证任务不被重复执行
            if (!compareAndSetState(INIT, WORKING)) {
                return workResult;
            }

            callback.begin(param);

            V resultValue;

            if (timeout > 0) {
                Thread thread = null;
                try {
                    FutureTask<V> futureTask = new FutureTask<>(() -> worker.action(param, forParamUseWrappers));
                    thread = new Thread(futureTask);
                    thread.start();
                    resultValue = futureTask.get(timeout, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    thread.interrupt();
                }
            } else {
                resultValue = worker.action(param, forParamUseWrappers);
            }

            //如果状态不是在working,说明别的地方已经修改了
            if (!compareAndSetState(WORKING, FINISH)) {
                return workResult;
            }

            if ("FAIL_RETRY".equals(resultValue) && retryCount != null && ++count <= retryCount) {
                this.state.set(INIT);
                workerDoJob();
                return workResult;
            }

            workResult.setResultState(ResultState.SUCCESS);
            workResult.setResult(resultValue);
            //回调成功
            callback.result(true, param, workResult);

            return workResult;
        } catch (Exception e) {

            //避免重复回调
            if (!checkIsNullResult()) {
                return workResult;
            }
            fastFail(WORKING, e);
            return workResult;
        }
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

    private boolean checkIsNullResult() {
        return ResultState.DEFAULT == workResult.getResultState();
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
        for (WorkerWrapper wrapper : nextWrappers) {
            if (workerWrapper.equals(wrapper)) {
                return;
            }
        }
        nextWrappers.add(workerWrapper);
    }

    private void addNextWrappers(List<WorkerWrapper<?, ?>> wrappers) {
        if (wrappers == null) {
            return;
        }
        for (WorkerWrapper<?, ?> wrapper : wrappers) {
            addNext(wrapper);
        }
    }

    private void addDependWrappers(List<DependWrapper> dependWrappers) {
        if (dependWrappers == null) {
            return;
        }
        for (DependWrapper wrapper : dependWrappers) {
            addDepend(wrapper);
        }
    }

    private WorkResult<V> defaultResult() {
        workResult.setResultState(ResultState.TIMEOUT);
        workResult.setResult(worker.defaultValue());
        return workResult;
    }

    private WorkResult<V> defaultExResult(Exception ex) {
        workResult.setResultState(ResultState.EXCEPTION);
        workResult.setResult(worker.defaultValue());
        workResult.setEx(ex);
        return workResult;
    }


    private int getState() {
        return state.get();
    }

    public String getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    public T getParam() {
        return param;
    }

    private boolean compareAndSetState(int expect, int update) {
        return this.state.compareAndSet(expect, update);
    }

    private void setNeedCheckNextWrapperResult(boolean needCheckNextWrapperResult) {
        this.needCheckNextWrapperResult = needCheckNextWrapperResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)  {
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
