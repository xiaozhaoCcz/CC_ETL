package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobNodeMapper;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.service.JobNodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class JobNodeServiceImpl extends ServiceImpl<JobNodeMapper, JobNode> implements JobNodeService {
    
    /**
     * 更新节点运行状态
     * @param jobId 任务ID
     * @param triggerStatus 运行状态：0=失败, 1=成功, 2=运行中
     * @return 是否更新成功
     */
    @Override
    public boolean updateNodeStatus(Long jobId, Integer triggerStatus) {
        log.info("更新节点运行状态 - jobId: {}, triggerStatus: {}", jobId, triggerStatus);
        
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
            
            if (success) {
                log.info("✓ 节点状态更新成功 - jobId: {}, triggerStatus: {}", jobId, triggerStatus);
            } else {
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
    public int batchUpdateNodeStatus(java.util.Map<Long, Integer> statusMap) {
        if (statusMap == null || statusMap.isEmpty()) {
            log.warn("批量更新节点状态 - 参数为空");
            return 0;
        }
        
        log.info("批量更新节点状态 - 共 {} 个节点", statusMap.size());
        
        try {
            // 1. 批量查询所有需要更新的节点（一次性查询，避免N+1问题）
            List<Long> jobIds = new ArrayList<>(statusMap.keySet());
            List<JobNode> nodes = this.list(
                new LambdaQueryWrapper<JobNode>().in(JobNode::getJobId, jobIds)
            );
            
            // 2. 构建 jobId -> JobNode 的映射
            Map<Long, JobNode> nodeMap = nodes.stream()
                .collect(java.util.stream.Collectors.toMap(JobNode::getJobId, n -> n, (existing, replacement) -> existing));
            
            // 3. 批量更新节点状态
            List<JobNode> updateNodes = new ArrayList<>();
            for (java.util.Map.Entry<Long, Integer> entry : statusMap.entrySet()) {
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
                    log.info("✓ 批量更新节点状态完成 - 成功: {}/{}", updateNodes.size(), statusMap.size());
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
}
