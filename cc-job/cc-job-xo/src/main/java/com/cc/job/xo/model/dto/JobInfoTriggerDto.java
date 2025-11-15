package com.cc.job.xo.model.dto;

import lombok.Data;

@Data
public class JobInfoTriggerDto {

    private Long id;

    private String executorParam;

    private String addressList;
    
    /**
     * 触发任务的用户ID
     */
    private Integer triggerUserId;
}
