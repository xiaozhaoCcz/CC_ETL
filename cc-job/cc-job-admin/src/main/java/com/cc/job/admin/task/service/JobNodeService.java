package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobNode;

import java.util.Map;

public interface JobNodeService extends IService<JobNode> {
    
    /**
     * 更新节点运行状态
     * @param jobId 任务ID
     * @param triggerStatus 运行状态：0=失败, 1=成功, 2=运行中
     * @return 是否更新成功
     */
    boolean updateNodeStatus(Long jobId, Integer triggerStatus);
    
    /**
     * 批量更新节点运行状态
     * @param statusMap 节点状态映射 Map<jobId, triggerStatus>
     * @return 成功更新的数量
     */
    int batchUpdateNodeStatus(Map<Long, Integer> statusMap);
    
    /**
     * 重置任务组中所有节点的运行状态为未运行状态（-1）
     * @param jobParentId 任务组ID（父任务ID）
     * @return 成功重置的节点数量
     */
    int resetAllNodeStatus(Long jobParentId);

    /**
     * 统计任务组下节点数量（用于判断全量跑批次）
     * @param jobParentId 任务组ID（父任务ID）
     * @return 节点数量
     */
    long countByJobParentId(Long jobParentId);
}
