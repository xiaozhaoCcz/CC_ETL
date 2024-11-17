package com.cc.job.task.websocket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

@Data
public class Message implements Serializable {

    private Long parentTaskId;

//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long nodeId;

//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long taskId;

    private Integer status;
}
