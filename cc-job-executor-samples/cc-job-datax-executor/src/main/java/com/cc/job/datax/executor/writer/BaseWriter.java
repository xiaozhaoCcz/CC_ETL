package com.cc.job.datax.executor.writer;

import cn.hutool.json.JSONObject;
import com.cc.job.datax.executor.model.DataXParams;

public interface BaseWriter {
    JSONObject buildJson(DataXParams dataXParams);
}
