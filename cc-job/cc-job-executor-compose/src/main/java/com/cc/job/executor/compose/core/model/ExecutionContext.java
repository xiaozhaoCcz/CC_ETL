package com.cc.job.executor.compose.core.model;

import com.cc.job.executor.compose.core.context.DataContext;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobContext;

import java.util.List;
import java.util.Map;

/**
 * 任务组执行上下文
 * 
 * <p>封装任务组执行所需的所有数据
 *
 * @author cc-job-team
 */
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

    
    /** 数据上下文 */
    private DataContext dataContext;
    
    /** jobName 到 jobId 的映射（用于参数解析） */
    private Map<String, Long> jobNameMap;

    /** 恢复执行：从该节点ID之后继续（审批通过后触发） */
    private Long resumeFromNodeId;
    /** 恢复执行的批次ID */
    private String resumeBatchId;

    public ExecutionContext() {
    }

    public ExecutionContext(Long taskGroupId, String executionBatchId, JobInfo taskGroupInfo, List<JobNode> nodes, List<JobEdge> edges, XxlJobContext xxlJobContext, String executeKey,List<Integer> jobPauseStatusIds) {
        this.taskGroupId = taskGroupId;
        this.executionBatchId = executionBatchId;
        this.taskGroupInfo = taskGroupInfo;
        this.nodes = nodes;
        this.edges = edges;
        this.xxlJobContext = xxlJobContext;
        this.executeKey = executeKey;
    }
    
    public ExecutionContext(Long taskGroupId, String executionBatchId, JobInfo taskGroupInfo, List<JobNode> nodes, List<JobEdge> edges, XxlJobContext xxlJobContext, String executeKey, DataContext dataContext, Map<String, Long> jobNameMap) {
        this.taskGroupId = taskGroupId;
        this.executionBatchId = executionBatchId;
        this.taskGroupInfo = taskGroupInfo;
        this.nodes = nodes;
        this.edges = edges;
        this.xxlJobContext = xxlJobContext;
        this.executeKey = executeKey;
        this.dataContext = dataContext;
        this.jobNameMap = jobNameMap;
    }

    public static ExecutionContextBuilder builder() {
        return new ExecutionContextBuilder();
    }

    public Long getTaskGroupId() {
        return taskGroupId;
    }

    public void setTaskGroupId(Long taskGroupId) {
        this.taskGroupId = taskGroupId;
    }

    public String getExecutionBatchId() {
        return executionBatchId;
    }

    public void setExecutionBatchId(String executionBatchId) {
        this.executionBatchId = executionBatchId;
    }

    public JobInfo getTaskGroupInfo() {
        return taskGroupInfo;
    }

    public void setTaskGroupInfo(JobInfo taskGroupInfo) {
        this.taskGroupInfo = taskGroupInfo;
    }

    public List<JobNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<JobNode> nodes) {
        this.nodes = nodes;
    }

    public List<JobEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<JobEdge> edges) {
        this.edges = edges;
    }

    public XxlJobContext getXxlJobContext() {
        return xxlJobContext;
    }

    public void setXxlJobContext(XxlJobContext xxlJobContext) {
        this.xxlJobContext = xxlJobContext;
    }

    public String getExecuteKey() {
        return executeKey;
    }

    public void setExecuteKey(String executeKey) {
        this.executeKey = executeKey;
    }

    public DataContext getDataContext() {
        return dataContext;
    }
    
    public void setDataContext(DataContext dataContext) {
        this.dataContext = dataContext;
    }
    
    public Map<String, Long> getJobNameMap() {
        return jobNameMap;
    }
    
    public void setJobNameMap(Map<String, Long> jobNameMap) {
        this.jobNameMap = jobNameMap;
    }

    public Long getResumeFromNodeId() {
        return resumeFromNodeId;
    }

    public void setResumeFromNodeId(Long resumeFromNodeId) {
        this.resumeFromNodeId = resumeFromNodeId;
    }

    public String getResumeBatchId() {
        return resumeBatchId;
    }

    public void setResumeBatchId(String resumeBatchId) {
        this.resumeBatchId = resumeBatchId;
    }

    public static class ExecutionContextBuilder {
        private Long taskGroupId;
        private String executionBatchId;
        private JobInfo taskGroupInfo;
        private List<JobNode> nodes;
        private List<JobEdge> edges;
        private XxlJobContext xxlJobContext;
        private String executeKey;
        private List<Integer> jobPauseStatusIds;
        private DataContext dataContext;
        private Map<String, Long> jobNameMap;

        ExecutionContextBuilder() {
        }

        public ExecutionContextBuilder taskGroupId(Long taskGroupId) {
            this.taskGroupId = taskGroupId;
            return this;
        }

        public ExecutionContextBuilder executionBatchId(String executionBatchId) {
            this.executionBatchId = executionBatchId;
            return this;
        }

        public ExecutionContextBuilder taskGroupInfo(JobInfo taskGroupInfo) {
            this.taskGroupInfo = taskGroupInfo;
            return this;
        }

        public ExecutionContextBuilder nodes(List<JobNode> nodes) {
            this.nodes = nodes;
            return this;
        }

        public ExecutionContextBuilder edges(List<JobEdge> edges) {
            this.edges = edges;
            return this;
        }

        public ExecutionContextBuilder xxlJobContext(XxlJobContext xxlJobContext) {
            this.xxlJobContext = xxlJobContext;
            return this;
        }

        public ExecutionContextBuilder executeKey(String executeKey) {
            this.executeKey = executeKey;
            return this;
        }

        public ExecutionContextBuilder jobPauseStatusIds(List<Integer> jobPauseStatusIds){
            this.jobPauseStatusIds = jobPauseStatusIds;
            return this;
        }
        
        public ExecutionContextBuilder dataContext(DataContext dataContext) {
            this.dataContext = dataContext;
            return this;
        }
        
        public ExecutionContextBuilder jobNameMap(Map<String, Long> jobNameMap) {
            this.jobNameMap = jobNameMap;
            return this;
        }

        public ExecutionContext build() {
            return new ExecutionContext(taskGroupId, executionBatchId, taskGroupInfo, nodes, edges, xxlJobContext, executeKey, dataContext, jobNameMap);
        }
    }
}

