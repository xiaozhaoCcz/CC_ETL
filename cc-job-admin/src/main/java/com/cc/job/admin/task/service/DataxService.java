package com.cc.job.admin.task.service;

import com.cc.job.xo.model.datax.DataXParams;

import java.util.List;
import java.util.Map;

public interface DataxService {
    String getJson(DataXParams dataXParams);

    List<String> batchBuildJson(Map<String, Object> params);
}
