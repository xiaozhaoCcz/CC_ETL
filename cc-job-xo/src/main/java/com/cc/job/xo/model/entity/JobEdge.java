package com.cc.job.xo.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("job_edge")
@ToString
public class JobEdge implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobParentId;

    private Long fromNodeId;

    private Long endNodeId;

    private String pointList;

    private String properties;

    private String startPoint;

    private String endPoint;

    @TableField(fill = FieldFill.INSERT)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
