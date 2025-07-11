package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cc.job.xo.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

@Data
@TableName("job_part")
public class JobPart extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String jobPartName;

    private Integer sort;
}
