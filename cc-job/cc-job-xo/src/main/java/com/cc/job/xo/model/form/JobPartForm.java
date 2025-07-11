package com.cc.job.xo.model.form;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Schema(description = "job_part表单对象")
public class JobPartForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String jobPartName;

    private Long sort;
}
