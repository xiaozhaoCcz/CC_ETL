package com.cc.job.task.model.dto;

import lombok.Data;

@Data
public class JobInfoTriggerDto {

    private Long id;

    private String executorParam;

    private String addressList;
}
