package com.xxl.job.core.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobGroupTool {

    private static final Logger logger = LoggerFactory.getLogger(JobGroupTool.class);
    /**
     * 存储任务执行状态映射
     * Key: jobId:randomId, Value: 是否成功执行
     */
    public static final Map<Integer, Integer> JOB_MAP = new ConcurrentHashMap<>();

    /**
     * 移除任务数据
     *
     * @param jobId 任务ID
     */
    public static void removeJobData(int jobId) {
        JOB_MAP.remove(jobId);
    }

    /**
     * 添加任务数据
     *
     * @param jobId     任务ID
     * @param status 运行状态
     */
    public static void addJobData(int jobId, int status) {
        logger.info(">>>>>>>>>>> addJobData, jobId:{}, status:{}", jobId, status);
        JOB_MAP.put(jobId, status);
    }
}
