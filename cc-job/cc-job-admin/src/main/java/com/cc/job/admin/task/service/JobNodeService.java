package com.cc.job.admin.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobNode;

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
    int batchUpdateNodeStatus(java.util.Map<Long, Integer> statusMap);
}
