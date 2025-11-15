package com.cc.job.xo.model.form;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobGlueForm implements Serializable {

    private Long taskId;

    private String glueRemark;

    private String glueSource;
    
    private String glueType;
}
