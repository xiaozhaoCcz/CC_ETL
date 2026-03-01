package com.cc.job.admin.task.service;

import com.cc.job.xo.model.datax.DataXParams;

import java.util.List;
import java.util.Map;

public interface DataxService {
    String getJson(DataXParams dataXParams);

    /**
     * 生成仅读取前 limit 条的 Reader JSON（用于预览/dry-run），仅支持 MySQL/PostgreSQL 等带 LIMIT 的库
     */
    String getPreviewReaderJson(DataXParams dataXParams, int limit);

    List<String> batchBuildJson(Map<String, Object> params);
}
