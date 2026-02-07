package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobNodeResultMapper;
import com.cc.job.xo.model.entity.JobNodeResult;
import com.cc.job.admin.task.service.JobNodeResultService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.util.FileStorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 节点执行结果服务实现类
 *
 * @author cc-job-team
 * @since 2026-01-06
 */
@Service
public class JobNodeResultServiceImpl extends ServiceImpl<JobNodeResultMapper, JobNodeResult> implements JobNodeResultService {

    private static final Logger log = LoggerFactory.getLogger(JobNodeResultServiceImpl.class);
    
    private final FileStorageUtil fileStorageUtil;
    private final JobNodeService jobNodeService;
    
    public JobNodeResultServiceImpl(FileStorageUtil fileStorageUtil, JobNodeService jobNodeService) {
        this.fileStorageUtil = fileStorageUtil;
        this.jobNodeService = jobNodeService;
    }
    
    /**
     * 重写removeById方法，删除数据库记录时同步删除本地文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        JobNodeResult result = this.getById(id);
        if (result != null && result.getFilePath() != null && !result.getFilePath().isEmpty()) {
            // 删除本地文件
            fileStorageUtil.deleteFile(result.getFilePath());
        }
        return super.removeById(id);
    }
    
    /**
     * 重写removeByIds方法，批量删除数据库记录时同步删除本地文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> idList) {
        if (idList != null && !idList.isEmpty()) {
            @SuppressWarnings("unchecked")
            Collection<java.io.Serializable> serializableIds = (Collection<java.io.Serializable>) (Collection<?>) idList;
            List<JobNodeResult> results = this.listByIds(serializableIds);
            for (JobNodeResult result : results) {
                if (result != null && result.getFilePath() != null && !result.getFilePath().isEmpty()) {
                    // 删除本地文件
                    fileStorageUtil.deleteFile(result.getFilePath());
                }
            }
        }
        return super.removeByIds(idList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNodeResult(Long taskGroupId, String executionBatchId, Long jobId, String jobName, String resultData) {
        try {
            JobNodeResult result = new JobNodeResult();
            result.setTaskGroupId(taskGroupId);
            result.setExecutionBatchId(executionBatchId);
            result.setJobId(jobId);
            result.setJobName(jobName);
            result.setResultData(resultData);

            boolean success = this.save(result);
            if (success) {
                log.info("[JobNodeResult] 保存节点执行结果成功 - taskGroupId: {}, batchId: {}, jobId: {}", 
                        taskGroupId, executionBatchId, jobId);
            } else {
                log.error("[JobNodeResult] 保存节点执行结果失败 - taskGroupId: {}, batchId: {}, jobId: {}", 
                        taskGroupId, executionBatchId, jobId);
            }
            return success;
        } catch (Exception e) {
            log.error("[JobNodeResult] 保存节点执行结果异常 - taskGroupId: {}, batchId: {}, jobId: {}", 
                    taskGroupId, executionBatchId, jobId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveNodeResults(Long taskGroupId, String executionBatchId, List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            log.warn("[JobNodeResult] 批量保存节点结果 - 结果列表为空");
            return 0;
        }

        try {
            List<JobNodeResult> resultList = results.stream()
                    .map(result -> {
                        JobNodeResult nodeResult = new JobNodeResult();
                        nodeResult.setTaskGroupId(taskGroupId);
                        nodeResult.setExecutionBatchId(executionBatchId);
                        nodeResult.setJobId(Long.valueOf(result.get("jobId").toString()));
                        nodeResult.setJobName(result.get("jobName") != null ? result.get("jobName").toString() : null);
                        nodeResult.setResultData(result.get("resultData") != null ? result.get("resultData").toString() : null);
                        nodeResult.setFilePath(result.get("filePath") != null ? result.get("filePath").toString() : null);
                        nodeResult.setDataSize(result.get("dataSize") != null ? Long.valueOf(result.get("dataSize").toString()) : null);
                        return nodeResult;
                    })
                    .collect(Collectors.toList());

            boolean success = this.saveBatch(resultList);
            if (success) {
                log.info("[JobNodeResult] 批量保存节点执行结果成功 - taskGroupId: {}, batchId: {}, 数量: {}", 
                        taskGroupId, executionBatchId, resultList.size());
                return resultList.size();
            } else {
                log.error("[JobNodeResult] 批量保存节点执行结果失败 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return 0;
            }
        } catch (Exception e) {
            log.error("[JobNodeResult] 批量保存节点执行结果异常 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId, e);
            return 0;
        }
    }

    @Override
    public List<JobNodeResult> getNodeResultsByBatch(Long taskGroupId, String executionBatchId) {
        try {
            LambdaQueryWrapper<JobNodeResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(JobNodeResult::getTaskGroupId, taskGroupId)
                    .eq(JobNodeResult::getExecutionBatchId, executionBatchId)
                    .orderByAsc(JobNodeResult::getCreateTime);

            List<JobNodeResult> results = this.list(queryWrapper);
            log.debug("[JobNodeResult] 根据批次获取节点结果 - taskGroupId: {}, batchId: {}, 数量: {}", 
                    taskGroupId, executionBatchId, results.size());
            return results;
        } catch (Exception e) {
            log.error("[JobNodeResult] 根据批次获取节点结果异常 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId, e);
            return List.of();
        }
    }

    @Override
    public String getLatestBatchId(Long taskGroupId) {
        try {
            LambdaQueryWrapper<JobNodeResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(JobNodeResult::getTaskGroupId, taskGroupId)
                    .orderByDesc(JobNodeResult::getCreateTime)
                    .last("LIMIT 1");

            JobNodeResult result = this.getOne(queryWrapper);
            if (result != null) {
                log.debug("[JobNodeResult] 获取最近一次批次ID - taskGroupId: {}, batchId: {}", 
                        taskGroupId, result.getExecutionBatchId());
                return result.getExecutionBatchId();
            } else {
                log.debug("[JobNodeResult] 未找到最近一次批次ID - taskGroupId: {}", taskGroupId);
                return null;
            }
        } catch (Exception e) {
            log.error("[JobNodeResult] 获取最近一次批次ID异常 - taskGroupId: {}", taskGroupId, e);
            return null;
        }
    }

    @Override
    public JobNodeResult getLatestNodeResult(Long taskGroupId, Long jobId) {
        try {
            LambdaQueryWrapper<JobNodeResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(JobNodeResult::getTaskGroupId, taskGroupId)
                    .eq(JobNodeResult::getJobId, jobId)
                    .orderByDesc(JobNodeResult::getCreateTime)
                    .last("LIMIT 1");

            JobNodeResult result = this.getOne(queryWrapper);
            if (result != null) {
                log.debug("[JobNodeResult] 获取最近一次节点结果 - taskGroupId: {}, jobId: {}", 
                        taskGroupId, jobId);
            } else {
                log.debug("[JobNodeResult] 未找到最近一次节点结果 - taskGroupId: {}, jobId: {}", 
                        taskGroupId, jobId);
            }
            return result;
        } catch (Exception e) {
            log.error("[JobNodeResult] 获取最近一次节点结果异常 - taskGroupId: {}, jobId: {}", 
                    taskGroupId, jobId, e);
            return null;
        }
    }

    @Override
    public String getLatestFullRunBatchId(Long taskGroupId) {
        if (taskGroupId == null) {
            return null;
        }
        try {
            long nodeCount = jobNodeService.countByJobParentId(taskGroupId);
            if (nodeCount <= 0) {
                log.debug("[JobNodeResult] 任务组无节点，无法判定全量跑批次 - taskGroupId: {}", taskGroupId);
                return null;
            }
            String batchId = baseMapper.selectLatestFullRunBatchId(taskGroupId, nodeCount);
            if (batchId != null) {
                log.debug("[JobNodeResult] 获取最近一次全量跑批次ID - taskGroupId: {}, batchId: {}", taskGroupId, batchId);
            }
            return batchId;
        } catch (Exception e) {
            log.error("[JobNodeResult] 获取最近一次全量跑批次ID异常 - taskGroupId: {}", taskGroupId, e);
            return null;
        }
    }
}

