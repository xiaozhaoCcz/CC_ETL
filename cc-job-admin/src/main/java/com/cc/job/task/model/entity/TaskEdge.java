package com.cc.job.task.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("task_edge")
@ToString
public class TaskEdge implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskParentId;

    private Long fromNodeId;

    private Long endNodeId;

    @TableField(fill = FieldFill.INSERT)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
