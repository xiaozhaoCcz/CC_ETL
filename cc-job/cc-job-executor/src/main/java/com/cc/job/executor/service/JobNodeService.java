package com.cc.job.executor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.job.xo.mapper.JobInfoMapper;
import com.cc.job.xo.mapper.JobNodeMapper;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JobNodeService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final JobInfoMapper jobInfoMapper;

    private final JobNodeMapper jobNodeMapper;

    public int resetAllNodeStatus(Long jobParentId) {
        try {
            // 1. 收集所有需要重置的节点ID（包括嵌套的任务组）
            Set<Long> allJobParentIds = new HashSet<>();
            allJobParentIds.add(jobParentId);
            collectAllJobParentIds(jobParentId, allJobParentIds);

            // 2. 批量查询所有节点
            List<JobNode> nodes = jobNodeMapper.selectList(
                    new LambdaQueryWrapper<JobNode>().in(JobNode::getJobParentId, allJobParentIds)
            );

            if (nodes.isEmpty()) {
                logger.warn("未找到需要重置的节点 - jobParentId: {}", jobParentId);
                return 0;
            }

            // 3. 批量重置所有节点的状态为 -1（未运行状态）
            // TODO 使用批量更新
            for (JobNode node : nodes) {
                node.setTriggerStatus(-1);
                jobNodeMapper.updateById(node);
            }
            return 0;
        } catch (Exception e) {
            logger.error("重置任务组所有节点状态异常 - jobParentId: {}, 错误: {}", jobParentId, e.getMessage());
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

    public boolean updateNodeStatus(Long jobId, Integer triggerStatus) {
        try {
            // 根据jobId查找节点
            LambdaQueryWrapper<JobNode> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(JobNode::getJobId, jobId);
            JobNode node = jobNodeMapper.selectOne(queryWrapper);

            if (node == null) {
                logger.warn("节点不存在 - jobId: {}", jobId);
                return false;
            }

            // 更新triggerStatus
            LambdaUpdateWrapper<JobNode> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(JobNode::getId, node.getId());
            updateWrapper.set(JobNode::getTriggerStatus, triggerStatus);

            int success = jobNodeMapper.update(node,updateWrapper);

            if (success<=0) {
                logger.error("✗ 节点状态更新失败 - jobId: {}", jobId);
            }

            return success>0;

        } catch (Exception e) {
            logger.error("更新节点状态异常 - jobId: {}, 错误: {}", jobId, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
