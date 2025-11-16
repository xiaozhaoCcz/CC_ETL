package com.cc.job.admin.task.handler;

import com.cc.job.admin.task.executor.Async;
import com.cc.job.admin.task.executor.callback.IWorker;
import com.cc.job.admin.task.executor.worker.DependWrapper;
import com.cc.job.admin.task.executor.worker.WorkResult;
import com.cc.job.admin.task.executor.wrapper.WorkerWrapper;
import com.cc.job.xo.model.entity.JobInfo;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.cc.job.admin.task.handler.JobConstant.DO_NOTHING;

/**
 * @author xiaozhao
 */
@Component
public class JobGroupUtils {

    /**
     * 预测任务的执行时间
     * @param workerWrappers
     * @param jobInfoMap     id:jobNodeId
     * @return
     */
    public String[][] getNextRunTime(List<WorkerWrapper<Long, String>> workerWrappers, Map<Long, JobInfo> jobInfoMap, long timeout, List<Long> startNodes, Long jobId) {
        Long currentTime = System.currentTimeMillis();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<WorkerWrapper<Long, Long[]>> timeWorkerWrappers = new ArrayList<>();
        Map<String, List<String>> nextMap = new HashMap<>();
        for (WorkerWrapper<Long, String> workerWrapper : workerWrappers) {
            WorkerWrapper<Long, Long[]> worker = new WorkerWrapper<Long, Long[]>()
                    .id(workerWrapper.getId())
                    .param(Long.valueOf(workerWrapper.getId()))
                    .worker(new IWorker<>() {
                        /**
                         *
                         * @param id      object
                         * @param allWrappers 任务包装
                         * @return 开始时间和结束时间
                         */
                        @Override
                        public Long[] action(Long id, Map<String, WorkerWrapper> allWrappers) {
                            List<DependWrapper> dependWrappers = workerWrapper.getDependWrappers();
                            JobInfo jobInfo = jobInfoMap.get(id);
                            if (dependWrappers == null || dependWrappers.isEmpty()) {
                                //开始节点,运行完成时间和开始时间
                                return new Long[]{currentTime, currentTime + jobInfo.getRunTime()};
                            }
                            List<? extends WorkerWrapper<?, ?>> workerWrapperList = dependWrappers.stream().map(DependWrapper::getDependWrapper).toList();
                            List<String> nodeIds = new ArrayList<>(workerWrapperList.stream().map(WorkerWrapper::getId).toList());

                            List<String> removeIds = new ArrayList<>();
                            jobInfoMap.entrySet().stream().filter(v -> nodeIds.contains(String.valueOf(v.getKey()))).forEach(entry -> {
                                JobInfo jobInfo1 = entry.getValue();
                                if (jobInfo1 != null && DO_NOTHING.equalsIgnoreCase(jobInfo1.getExecutorBlockStrategy())) {
                                    removeIds.add(entry.getKey().toString());
                                }
                                if (jobInfo1 != null && jobInfo1.getExecutorFailRetryCount() > 0) {
                                    jobInfo1.setRunTime(jobInfo1.getRunTime() * jobInfo1.getExecutorFailRetryCount());
                                    jobInfoMap.put(entry.getKey(), jobInfo1);
                                }
                            });
                            nodeIds.removeAll(removeIds);

                            final long[] maxTime = {0};
                            // 如果移除后已无依赖，则视为起始节点（使用当前时间作为开始时间）
                            if (nodeIds.isEmpty()) {
                                return new Long[]{currentTime, currentTime + jobInfo.getRunTime()};
                            }
                            allWrappers.entrySet().stream().filter(v -> nodeIds.contains(v.getKey())).forEach(entry -> {
                                WorkerWrapper worker = entry.getValue();
                                WorkResult workResult = worker.getWorkResult();
                                Long[] times = (Long[]) workResult.getResult();
                                if (times[1] > maxTime[0]) {
                                    maxTime[0] = times[1];
                                }
                            });

                            // 防御：如果依赖结果异常导致maxTime仍为0，也按起始节点处理
                            long startTs = (maxTime[0] == 0L) ? currentTime : maxTime[0];
                            return new Long[]{startTs, startTs + jobInfo.getRunTime()};
                        }

                        @Override
                        public Long[] defaultValue() {
                            return new Long[]{0L, 0L};
                        }
                    });
            timeWorkerWrappers.add(worker);
            List<WorkerWrapper<?, ?>> nextWrappers = workerWrapper.getNextWrappers();
            if (nextWrappers != null) {
                List<String> nextIds = nextWrappers.stream().map(WorkerWrapper::getId).toList();
                nextMap.put(workerWrapper.getId(), nextIds);
            }
        }

        for (WorkerWrapper<Long, Long[]> timeWorkerWrapper : timeWorkerWrappers) {
            List<String> nextIds = nextMap.get(timeWorkerWrapper.getId());
            if (nextIds != null) {
                List<WorkerWrapper<Long, Long[]>> nextWorkers = timeWorkerWrappers.stream().filter(workerWrapper -> nextIds.contains(workerWrapper.getId())).toList();
                timeWorkerWrapper.next(nextWorkers.toArray(new WorkerWrapper[0]));
            }
        }

        try {
            Async.beginWork(timeout,(List<WorkerWrapper>) (List<?>) timeWorkerWrappers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<String[]> resList = new ArrayList<>();
        for (WorkerWrapper<Long, Long[]> timeWorkerWrapper : timeWorkerWrappers) {
            resList.add(new String []{timeWorkerWrapper.getId(), sdf.format(timeWorkerWrapper.getWorkResult().getResult()[0]), sdf.format(timeWorkerWrapper.getWorkResult().getResult()[1])});
        }
        return resList.toArray(new String[0][0]);
    }
}
