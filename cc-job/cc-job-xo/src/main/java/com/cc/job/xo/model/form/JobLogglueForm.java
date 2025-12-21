package com.cc.job.xo.model.form;

import java.io.Serial;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

/**
 * task_logglue表单对象
 *
 * @author ccjob
 * @since 2024-11-03 08:19
 */
@Schema(description = "task_logglue表单对象")
public class JobLogglueForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;

    @Schema(description = "任务，主键ID")
    private Integer jobId;

    @Schema(description = "GLUE类型")
    @NotBlank(message = "GLUE类型不能为空")
    @Size(max=50, message="GLUE类型长度不能超过50个字符")
    private String glueType;

    @Schema(description = "GLUE源代码")
    @NotNull(message = "GLUE源代码不能为空")
    @Size(max=16777215, message="GLUE源代码长度不能超过16777215个字符")
    private String glueSource;

    @Schema(description = "GLUE备注")
    @Size(max=128, message="GLUE备注长度不能超过128个字符")
    private String glueRemark;

    @NotNull(message = "不能为空")
    private LocalDateTime addTime;

    @NotNull(message = "不能为空")
    private LocalDateTime updateTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getGlueType() {
        return glueType;
    }

    public void setGlueType(String glueType) {
        this.glueType = glueType;
    }

    public String getGlueSource() {
        return glueSource;
    }

    public void setGlueSource(String glueSource) {
        this.glueSource = glueSource;
    }

    public String getGlueRemark() {
        return glueRemark;
    }

    public void setGlueRemark(String glueRemark) {
        this.glueRemark = glueRemark;
    }

    public LocalDateTime getAddTime() {
        return addTime;
    }

    public void setAddTime(LocalDateTime addTime) {
        this.addTime = addTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
