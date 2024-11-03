package com.cc.job.task.model.dto;

import lombok.Data;

@Data
public class TaskInfoTriggerDto {

    private Long id;

    private String executorParam;

    private String addressList;
}
