package com.cc.job.xo.model.form;

import java.io.Serializable;
import java.util.Objects;

public class JobGlueForm implements Serializable {

    private Long taskId;

    private String glueRemark;

    private String glueSource;
    
    private String glueType;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getGlueRemark() {
        return glueRemark;
    }

    public void setGlueRemark(String glueRemark) {
        this.glueRemark = glueRemark;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobGlueForm that = (JobGlueForm) o;
        return Objects.equals(taskId, that.taskId) &&
                Objects.equals(glueRemark, that.glueRemark) &&
                Objects.equals(glueSource, that.glueSource) &&
                Objects.equals(glueType, that.glueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, glueRemark, glueSource, glueType);
    }

    @Override
    public String toString() {
        return "JobGlueForm{" +
                "taskId=" + taskId +
                ", glueRemark='" + glueRemark + '\'' +
                ", glueSource='" + glueSource + '\'' +
                ", glueType='" + glueType + '\'' +
                '}';
    }
}
