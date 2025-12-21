package com.cc.job.executor.compose.core.model;

import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.xxl.job.core.context.XxlJobContext;

import java.util.List;
import java.util.Objects;

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

    public ExecutionContext() {
    }

    public ExecutionContext(Long taskGroupId, String executionBatchId, JobInfo taskGroupInfo, List<JobNode> nodes, List<JobEdge> edges, XxlJobContext xxlJobContext, String executeKey) {
        this.taskGroupId = taskGroupId;
        this.executionBatchId = executionBatchId;
        this.taskGroupInfo = taskGroupInfo;
        this.nodes = nodes;
        this.edges = edges;
        this.xxlJobContext = xxlJobContext;
        this.executeKey = executeKey;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionContext that = (ExecutionContext) o;
        return Objects.equals(taskGroupId, that.taskGroupId) &&
                Objects.equals(executionBatchId, that.executionBatchId) &&
                Objects.equals(taskGroupInfo, that.taskGroupInfo) &&
                Objects.equals(nodes, that.nodes) &&
                Objects.equals(edges, that.edges) &&
                Objects.equals(xxlJobContext, that.xxlJobContext) &&
                Objects.equals(executeKey, that.executeKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskGroupId, executionBatchId, taskGroupInfo, nodes, edges, xxlJobContext, executeKey);
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "taskGroupId=" + taskGroupId +
                ", executionBatchId='" + executionBatchId + '\'' +
                ", taskGroupInfo=" + taskGroupInfo +
                ", nodes=" + nodes +
                ", edges=" + edges +
                ", xxlJobContext=" + xxlJobContext +
                ", executeKey='" + executeKey + '\'' +
                '}';
    }

    public static class ExecutionContextBuilder {
        private Long taskGroupId;
        private String executionBatchId;
        private JobInfo taskGroupInfo;
        private List<JobNode> nodes;
        private List<JobEdge> edges;
        private XxlJobContext xxlJobContext;
        private String executeKey;

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

        public ExecutionContext build() {
            return new ExecutionContext(taskGroupId, executionBatchId, taskGroupInfo, nodes, edges, xxlJobContext, executeKey);
        }
    }
}

