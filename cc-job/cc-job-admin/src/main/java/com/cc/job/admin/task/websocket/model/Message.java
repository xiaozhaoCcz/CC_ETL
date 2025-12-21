package com.cc.job.admin.task.websocket.model;

import java.io.Serializable;
import java.util.Objects;

public class Message implements Serializable {

    private Long parentJobId;

//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long nodeId;

//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long jobId;

    private Integer status;

    private String randomId;

    private String result;

    public Long getParentJobId() {
        return parentJobId;
    }

    public void setParentJobId(Long parentJobId) {
        this.parentJobId = parentJobId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRandomId() {
        return randomId;
    }

    public void setRandomId(String randomId) {
        this.randomId = randomId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(parentJobId, message.parentJobId) &&
                Objects.equals(nodeId, message.nodeId) &&
                Objects.equals(jobId, message.jobId) &&
                Objects.equals(status, message.status) &&
                Objects.equals(randomId, message.randomId) &&
                Objects.equals(result, message.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentJobId, nodeId, jobId, status, randomId, result);
    }

    @Override
    public String toString() {
        return "Message{" +
                "parentJobId=" + parentJobId +
                ", nodeId=" + nodeId +
                ", jobId=" + jobId +
                ", status=" + status +
                ", randomId='" + randomId + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}
