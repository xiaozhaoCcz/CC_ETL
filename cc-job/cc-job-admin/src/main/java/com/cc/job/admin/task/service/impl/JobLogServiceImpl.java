package com.cc.job.admin.task.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.scheduler.XxlJobScheduler;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.utils.DateUtils;
import com.cc.job.admin.task.utils.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobLogMapper;
import com.cc.job.admin.task.service.JobLogService;
import com.cc.job.xo.model.entity.JobLog;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * task_log服务实现类
 *
 * @author ccjob
 * @since 2024-11-03 08:20
 */
@Service
public class JobLogServiceImpl extends ServiceImpl<JobLogMapper, JobLog> implements JobLogService {
    private static final Logger log = LoggerFactory.getLogger(JobLogServiceImpl.class);
    
    private final JobInfoService taskInfoService;
    private final JobNodeService jobNodeService;

    public JobLogServiceImpl(JobInfoService taskInfoService, JobNodeService jobNodeService) {
        this.taskInfoService = taskInfoService;
        this.jobNodeService = jobNodeService;
    }

    /**
    * 获取task_log分页列表
    * @param queryParams 查询参数
    * @return {@link IPage< JobLogVO >} task_log分页列表
    */
    @Override
    public IPage<JobLogVO> getJobLogPage(JobLogQuery queryParams) {
        IPage<JobLogVO> pageVO = new Page<>();
        LambdaQueryWrapper<JobLog> wrapper = new LambdaQueryWrapper<>();

        if(queryParams.getJobId()!=null){
            wrapper.eq(JobLog::getJobId,queryParams.getJobId());
        }
        baseWrapper(queryParams, wrapper);
        wrapper.orderByDesc(JobLog::getTriggerTime);

        Page<JobLog> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<JobLog> taskLogList = page.getRecords();
        List<Long> taskIds = taskLogList.stream().map(JobLog::getJobId).toList();

        if(taskIds.isEmpty()){
            return pageVO;
        }
        List<JobInfo> taskInfos = taskInfoService.listByIds(taskIds);
        Map<Long, JobInfo> taskInfoMap = taskInfos.stream().collect(Collectors.toMap(JobInfo::getId, t -> t));

        List<JobLogVO> voList  = new ArrayList<>();

        for (JobLog taskLog : taskLogList) {
            JobLogVO taskLogVO = BeanUtil.copyProperties(taskLog, JobLogVO.class);
            JobInfo taskInfo = taskInfoMap.get(taskLog.getJobId());
            if(taskInfo!=null){
                taskLogVO.setJobDesc(taskInfo.getJobDesc());
                taskLogVO.setJobType(taskInfo.getJobType());
            }
            voList.add(taskLogVO);
        }
        pageVO.setRecords(voList);
        pageVO.setTotal(page.getTotal());
        return pageVO;
    }

    
    /**
     * 删除task_log
     *
     * @param ids task_logID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteJobLogs(JobLogQuery queryParams) {
        LambdaQueryWrapper<JobLog> wrapper = new LambdaQueryWrapper<>();
        // 逻辑删除
        baseWrapper(queryParams, wrapper);
        return this.remove(wrapper);
    }

    @Override
    public ReturnT<LogResult> getLogDetailCat(Long logId, int fromLineNum) {
        try {
            // valid
            JobLog jobLog = this.getById(logId);	// todo, need to improve performance
            if (jobLog == null) {
                return new ReturnT<LogResult>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_logid_unvalid"));
            }

            // log cat
            // ⚠️ 检查执行器地址是否为空
            String executorAddress = jobLog.getExecutorAddress();
            if (executorAddress == null || executorAddress.trim().isEmpty()) {
                String errorMsg = "执行器不可用，无法获取日志。执行器地址: null（任务可能已完成，执行器已下线）";
                log.warn("❌ 执行器地址为空，无法获取日志 - logId: {}, jobId: {}", logId, jobLog.getJobId());
                // 如果任务已完成，返回空的日志结果，标记为结束
                if (jobLog.getHandleCode() != null && jobLog.getHandleCode() > 0) {
                    LogResult emptyResult = new LogResult(fromLineNum, fromLineNum, "", true);
                    return new ReturnT<LogResult>(emptyResult);
                }
                return new ReturnT<LogResult>(ReturnT.FAIL_CODE, errorMsg);
            }
            
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(executorAddress);
            if (executorBiz == null) {
                String errorMsg = "执行器不可用，无法获取日志。执行器地址: " + executorAddress + "（执行器可能已下线）";
                log.warn("❌ 执行器不可用，无法获取日志 - logId: {}, jobId: {}, 执行器地址: {}", 
                        logId, jobLog.getJobId(), executorAddress);
                // 如果任务已完成，返回空的日志结果，标记为结束
                if (jobLog.getHandleCode() != null && jobLog.getHandleCode() > 0) {
                    LogResult emptyResult = new LogResult(fromLineNum, fromLineNum, "", true);
                    return new ReturnT<LogResult>(emptyResult);
                }
                return new ReturnT<LogResult>(ReturnT.FAIL_CODE, errorMsg);
            }
            ReturnT<LogResult> logResult = executorBiz.log(new LogParam(jobLog.getTriggerTime().toInstant(ZoneOffset.of("+8")).toEpochMilli(), logId, fromLineNum));

            // is end
            if (logResult.getContent()!=null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

            // fix xss
            if (logResult.getContent()!=null && StringUtils.hasText(logResult.getContent().getLogContent())) {
                String newLogContent = logResult.getContent().getLogContent();
                newLogContent = HtmlUtils.htmlEscape(newLogContent, "UTF-8");
                logResult.getContent().setLogContent(newLogContent);
            }
            return logResult;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ReturnT<LogResult>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    private void baseWrapper(JobLogQuery queryParams, LambdaQueryWrapper<JobLog> wrapper) {
        if(queryParams.getJobGroup()!=null){
            wrapper.eq(JobLog::getJobGroup, queryParams.getJobGroup());
        }

        if(queryParams.getLogStatus()!=null){
            if(queryParams.getLogStatus()==1){
                wrapper.eq(JobLog::getHandleCode, 200);
            }else if(queryParams.getLogStatus()==2){
                wrapper.and(
                        e -> e.notIn(JobLog::getTriggerCode, 0, 200).or()
                                .notIn(JobLog::getHandleCode,0, 200));
            }else if(queryParams.getLogStatus()==3){
                wrapper.eq(JobLog::getTriggerCode,200).eq(JobLog::getHandleCode,0);
            }
        }

        if(queryParams.getFilterTime()!=null&&queryParams.getFilterTime().length>0){
            wrapper.between(JobLog::getTriggerTime, DateUtils.formatDate(queryParams.getFilterTime()[0]),  DateUtils.formatDate(queryParams.getFilterTime()[1]));
        }
    }
    
    /**
     * 保存节点执行状态到 job_log 表
     * 
     * @param taskGroupId 任务组ID
     * @param executionBatchId 执行批次ID（即 executorParam）
     * @param nodeStatusJson 节点状态JSON字符串
     * @return 是否保存成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveNodeStatus(Long taskGroupId, String executionBatchId, String nodeStatusJson) {
        try {
            log.info("[JobLogService] 保存节点状态 - taskGroupId: {}, batchId: {}", taskGroupId, executionBatchId);
            
            // 根据任务组ID和执行批次ID查找对应的 job_log 记录
            // executorParam 字段存储的是 executionBatchId（randomId）
            LambdaQueryWrapper<JobLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(JobLog::getJobId, taskGroupId)
                   .eq(JobLog::getExecutorParam, executionBatchId)
                   .orderByDesc(JobLog::getTriggerTime)
                   .last("LIMIT 1");
            
            JobLog jobLog = this.getOne(wrapper);
            
            // 如果通过 executorParam 找不到，尝试查找最近的一条记录（兼容旧数据）
            if (jobLog == null) {
                log.warn("[JobLogService] 通过 executorParam 未找到记录，尝试查找最近的一条 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(JobLog::getJobId, taskGroupId)
                       .orderByDesc(JobLog::getTriggerTime)
                       .last("LIMIT 1");
                jobLog = this.getOne(wrapper);
            }
            
            if (jobLog == null) {
                log.warn("[JobLogService] 未找到对应的 job_log 记录 - taskGroupId: {}, batchId: {}", 
                        taskGroupId, executionBatchId);
                return false;
            }
            
            // 更新 nodeStatus 字段到 JobLog
            jobLog.setNodeStatus(nodeStatusJson);
            boolean success = this.updateById(jobLog);
            
            if (success) {
                // ⭐ 重要：将节点状态同步更新到 job_node 表
                updateJobNodeStatuses(taskGroupId, nodeStatusJson);
                
                // 简单统计节点数量（通过计算JSON中的节点数）
                int nodeCount = 0;
                if (nodeStatusJson != null && !nodeStatusJson.isEmpty()) {
                    try {
                        // 通过计算 "jobId" 出现的次数来估算节点数（不准确但简单）
                        nodeCount = (nodeStatusJson.split("\"jobId\"").length - 1);
                    } catch (Exception e) {
                        // 忽略统计错误
                    }
                }
                log.info("[JobLogService] 节点状态保存成功 - logId: {}, taskGroupId: {}, 节点数: {}", 
                        jobLog.getId(), taskGroupId, nodeCount);
            } else {
                log.error("[JobLogService] 节点状态保存失败 - logId: {}, taskGroupId: {}", 
                        jobLog.getId(), taskGroupId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("[JobLogService] 保存节点状态异常 - taskGroupId: {}, batchId: {}", 
                    taskGroupId, executionBatchId, e);
            return false;
        }
    }

    @Override
    public long countLogSuccess() {
        return this.count(new LambdaQueryWrapper<JobLog>().eq(JobLog::getHandleCode, 200));
    }

    @Override
    public long countLogFail() {
        return this.count(new LambdaQueryWrapper<JobLog>()
                .ne(JobLog::getHandleCode, 0)
                .ne(JobLog::getHandleCode, 200));
    }

    @Override
    public long countLogRunning() {
        return this.count(new LambdaQueryWrapper<JobLog>().eq(JobLog::getHandleCode, 0));
    }
    
    /**
     * 将节点状态更新到 job_node 表
     * 
     * @param taskGroupId 任务组ID
     * @param nodeStatusJson 节点状态JSON（格式：{"nodeId": {"jobId": xxx, "status": 0/1/2}, ...}）
     */
    private void updateJobNodeStatuses(Long taskGroupId, String nodeStatusJson) {
        try {
            if (nodeStatusJson == null || nodeStatusJson.isEmpty()) {
                return;
            }
            
            // 解析 JSON 获取节点状态映射
            JSONObject statusMap = JSONUtil.parseObj(nodeStatusJson);
            if (statusMap == null || statusMap.isEmpty()) {
                return;
            }
            
            log.info("[JobLogService] 开始更新节点状态到 job_node 表 - taskGroupId: {}, 节点数: {}", 
                    taskGroupId, statusMap.size());
            
            // 获取所有子节点
            List<JobNode> childNodes = jobNodeService.list(
                new LambdaQueryWrapper<JobNode>()
                    .eq(JobNode::getJobParentId, taskGroupId)
                    .eq(JobNode::getIsDeleted, 0)
            );
            
            if (childNodes == null || childNodes.isEmpty()) {
                log.warn("[JobLogService] 未找到任务组的子节点 - taskGroupId: {}", taskGroupId);
                return;
            }
            
            // 创建 jobId 到 JobNode 的映射
            Map<Long, JobNode> jobNodeMap = childNodes.stream()
                .collect(Collectors.toMap(JobNode::getJobId, node -> node));
            
            int updatedCount = 0;
            
            // 遍历状态JSON，更新对应节点的状态
            for (Map.Entry<String, Object> entry : statusMap.entrySet()) {
                try {
                    if (!(entry.getValue() instanceof JSONObject)) {
                        continue;
                    }
                    
                    JSONObject nodeInfo = (JSONObject) entry.getValue();
                    
                    Long jobId = nodeInfo.getLong("jobId");
                    Integer status = nodeInfo.getInt("status");
                    
                    // 如果有 jobId 和 status，更新对应节点
                    if (jobId != null && status != null && jobNodeMap.containsKey(jobId)) {
                        JobNode node = jobNodeMap.get(jobId);
                        node.setTriggerStatus(status);
                        updatedCount++;
                    }
                    
                } catch (Exception e) {
                    log.warn("[JobLogService] 解析单个节点状态失败 - nodeId: {}", entry.getKey(), e);
                }
            }
            
            // ⭐ 关键修复：逐个更新而不是批量更新，避免 "Transaction not enabled" 警告
            if (updatedCount > 0) {
                int successCount = 0;
                for (JobNode node : childNodes) {
                    if (node.getTriggerStatus() != null) {
                        boolean updated = jobNodeService.updateById(node);
                        if (updated) {
                            successCount++;
                            log.debug("[JobLogService] 更新节点状态 - nodeId: {}, jobId: {}, status: {}", 
                                node.getId(), node.getJobId(), node.getTriggerStatus());
                        } else {
                            log.warn("[JobLogService] 节点状态更新失败 - nodeId: {}, jobId: {}", 
                                node.getId(), node.getJobId());
                        }
                    }
                }
                
                if (successCount > 0) {
                    log.info("[JobLogService] ✓ 成功将 {} 个节点的最终状态更新到 job_node 表 - taskGroupId: {}", 
                            successCount, taskGroupId);
                } else {
                    log.error("[JobLogService] ✗ 所有节点状态更新均失败 - taskGroupId: {}", taskGroupId);
                }
            }
            
        } catch (Exception e) {
            log.error("[JobLogService] 更新节点状态到 job_node 表异常 - taskGroupId: {}", taskGroupId, e);
            // 不抛出异常，避免影响主流程
        }
    }

}
