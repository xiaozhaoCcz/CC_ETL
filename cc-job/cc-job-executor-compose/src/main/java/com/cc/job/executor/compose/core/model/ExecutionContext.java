package com.cc.job.executor.compose.core.model;

import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobContext;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 任务组执行上下文
 * 
 * <p>封装任务组执行所需的所有数据
 *
 * @author cc-job-team
 */
@Data
@Builder
public class ExecutionContext {
    
    /** 任务组ID */
    private Long taskGroupId;
    
    /** 执行批次ID */
    private String executionBatchId;
    
    /** 任务组信息 */
    private JobInfo taskGroupInfo;
    
    /** 任务节点列表 */
    private List<JobNode> nodes;
    
    /** 任务边列表（依赖关系） */
    private List<JobEdge> edges;
    
    /** XXL-Job 上下文 */
    private XxlJobContext xxlJobContext;
    
    /** 执行键 */
    private String executeKey;
}

