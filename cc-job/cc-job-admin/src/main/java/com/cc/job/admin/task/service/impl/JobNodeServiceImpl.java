package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobNodeMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.service.JobNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobNodeServiceImpl extends ServiceImpl<JobNodeMapper, JobNode> implements JobNodeService {

    private static final Logger log = LoggerFactory.getLogger(JobNodeServiceImpl.class);
    
    private final JobInfoMapper jobInfoMapper;

    public JobNodeServiceImpl(JobInfoMapper jobInfoMapper) {
        this.jobInfoMapper = jobInfoMapper;
    }
    
    /**
     * 更新节点运行状态
     * @param jobId 任务ID
     * @param triggerStatus 运行状态：0=失败, 1=成功, 2=运行中
     * @return 是否更新成功
     */
    @Override
    public boolean updateNodeStatus(Long jobId, Integer triggerStatus) {
        try {
            // 根据jobId查找节点
            LambdaQueryWrapper<JobNode> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(JobNode::getJobId, jobId);
            JobNode node = this.getOne(queryWrapper);
            
            if (node == null) {
                log.warn("节点不存在 - jobId: {}", jobId);
                return false;
            }
            
            // 更新triggerStatus
            LambdaUpdateWrapper<JobNode> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(JobNode::getId, node.getId());
            updateWrapper.set(JobNode::getTriggerStatus, triggerStatus);
            
            boolean success = this.update(updateWrapper);
            
            if (!success) {
                log.error("✗ 节点状态更新失败 - jobId: {}", jobId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("更新节点状态异常 - jobId: {}, 错误: {}", jobId, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 批量更新节点运行状态（优化版本：批量查询，避免N+1问题）
     * @param statusMap 节点状态映射 Map<jobId, triggerStatus>
     * @return 成功更新的数量
     */
    @Override
    public int batchUpdateNodeStatus(Map<Long, Integer> statusMap) {
        if (statusMap == null || statusMap.isEmpty()) {
            log.warn("批量更新节点状态 - 参数为空");
            return 0;
        }
        
        try {
            // 1. 批量查询所有需要更新的节点（一次性查询，避免N+1问题）
            List<Long> jobIds = new ArrayList<>(statusMap.keySet());
            List<JobNode> nodes = this.list(
                new LambdaQueryWrapper<JobNode>().in(JobNode::getJobId, jobIds)
            );
            
            // 2. 构建 jobId -> JobNode 的映射
            Map<Long, JobNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(JobNode::getJobId, n -> n, (existing, replacement) -> existing));
            
            // 3. 批量更新节点状态
            List<JobNode> updateNodes = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : statusMap.entrySet()) {
                Long jobId = entry.getKey();
                Integer triggerStatus = entry.getValue();
                
                JobNode node = nodeMap.get(jobId);
                if (node == null) {
                    log.warn("节点不存在 - jobId: {}", jobId);
                    continue;
                }
                
                // 设置新的状态
                node.setTriggerStatus(triggerStatus);
                updateNodes.add(node);
            }
            
            // 4. 批量更新（一次性更新所有节点）
            if (!updateNodes.isEmpty()) {
                boolean success = this.updateBatchById(updateNodes);
                if (success) {
                    return updateNodes.size();
                } else {
                    log.error("✗ 批量更新节点状态失败");
                    return 0;
                }
            }
            
            return 0;
            
        } catch (Exception e) {
            log.error("批量更新节点状态异常: {}", e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 重置任务组中所有节点的运行状态为未运行状态（-1）
     * 包括嵌套的子任务组中的节点
     * 
     * @param jobParentId 任务组ID（父任务ID）
     * @return 成功重置的节点数量
     */
    @Override
    public int resetAllNodeStatus(Long jobParentId) {
        try {
            // 1. 收集所有需要重置的节点ID（包括嵌套的任务组）
            Set<Long> allJobParentIds = new HashSet<>();
            allJobParentIds.add(jobParentId);
            collectAllJobParentIds(jobParentId, allJobParentIds);
            
            // 2. 批量查询所有节点
            List<JobNode> nodes = this.list(
                new LambdaQueryWrapper<JobNode>().in(JobNode::getJobParentId, allJobParentIds)
            );
            
            if (nodes.isEmpty()) {
                log.warn("未找到需要重置的节点 - jobParentId: {}", jobParentId);
                return 0;
            }
            
            // 3. 批量重置所有节点的状态为 -1（未运行状态）
            for (JobNode node : nodes) {
                node.setTriggerStatus(-1);
            }
            
            // 4. 批量更新数据库
            boolean success = this.updateBatchById(nodes);
            if (success) {
                return nodes.size();
            } else {
                log.error("✗ 重置任务组所有节点状态失败 - jobParentId: {}", jobParentId);
                return 0;
            }
            
        } catch (Exception e) {
            log.error("重置任务组所有节点状态异常 - jobParentId: {}, 错误: {}", jobParentId, e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 递归收集所有任务组ID（包括嵌套的任务组）
     * 修复：使用 JobInfoMapper 直接查询，避免循环依赖
     * 
     * @param jobParentId 当前任务组ID
     * @param allJobParentIds 所有任务组ID集合（输出参数）
     */
    private void collectAllJobParentIds(Long jobParentId, Set<Long> allJobParentIds) {
        // 查询当前任务组下的所有子任务组（直接使用 Mapper，避免循环依赖）
        List<JobInfo> childJobInfos = jobInfoMapper.selectList(
            new LambdaQueryWrapper<JobInfo>()
                .eq(JobInfo::getParentId, jobParentId)
                .eq(JobInfo::getJobType, 2)  // 任务组类型
        );
        
        // 递归收集子任务组的ID
        for (JobInfo childJobInfo : childJobInfos) {
            if (allJobParentIds.add(childJobInfo.getId())) {
                collectAllJobParentIds(childJobInfo.getId(), allJobParentIds);
            }
        }
    }

    @Override
    public long countByJobParentId(Long jobParentId) {
        if (jobParentId == null) {
            return 0;
        }
        LambdaQueryWrapper<JobNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobNode::getJobParentId, jobParentId);
        return this.count(wrapper);
    }
}
