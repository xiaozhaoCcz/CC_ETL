package com.cc.job.admin.task.service;

import com.cc.job.xo.model.datax.DataXParams;

import java.util.Map;

public interface DataxService {
    String getJson(DataXParams dataXParams);

    String batchBuildJson(Map<String, Object> params);
}
