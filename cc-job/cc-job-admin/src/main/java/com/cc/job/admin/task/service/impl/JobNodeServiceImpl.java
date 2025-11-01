package com.cc.job.admin.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cc.job.xo.mapper.JobNodeMapper;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.service.JobNodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * 批量更新节点运行状态
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
        
        int successCount = 0;
        
        try {
            for (java.util.Map.Entry<Long, Integer> entry : statusMap.entrySet()) {
                Long jobId = entry.getKey();
                Integer triggerStatus = entry.getValue();
                
                // 根据jobId查找节点
                LambdaQueryWrapper<JobNode> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(JobNode::getJobId, jobId);
                JobNode node = this.getOne(queryWrapper);
                
                if (node == null) {
                    log.warn("节点不存在 - jobId: {}", jobId);
                    continue;
                }
                
                // 更新triggerStatus
                LambdaUpdateWrapper<JobNode> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(JobNode::getId, node.getId());
                updateWrapper.set(JobNode::getTriggerStatus, triggerStatus);
                
                boolean success = this.update(updateWrapper);
                if (success) {
                    successCount++;
                } else {
                    log.warn("节点状态更新失败 - jobId: {}", jobId);
                }
            }
            
            log.info("✓ 批量更新节点状态完成 - 成功: {}/{}", successCount, statusMap.size());
            return successCount;
            
        } catch (Exception e) {
            log.error("批量更新节点状态异常: {}", e.getMessage());
            e.printStackTrace();
            return successCount;
        }
    }
}
