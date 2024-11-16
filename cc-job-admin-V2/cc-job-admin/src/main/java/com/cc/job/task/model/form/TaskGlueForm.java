package com.cc.job.task.model.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class TaskGlueForm implements Serializable {

    private Long taskId;

    private String glueRemark;

    private String glueSource;
}
