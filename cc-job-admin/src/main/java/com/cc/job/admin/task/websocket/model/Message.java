package com.cc.job.admin.task.websocket.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Message implements Serializable {

    private Long parentJobId;

//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long nodeId;

//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long jobId;

    private Integer status;

    private String randomId;
}
