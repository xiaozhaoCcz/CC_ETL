package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("job_compose")
public class JobCompose implements Serializable {

    private static final long serialVersionUID = 1L;



    @TableId(type = IdType.AUTO)
    private Long id;


    private String appName;

    private String executorAddress;
    private String executorServerAddress;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;



    @TableLogic
    private Integer isDeleted;
}
