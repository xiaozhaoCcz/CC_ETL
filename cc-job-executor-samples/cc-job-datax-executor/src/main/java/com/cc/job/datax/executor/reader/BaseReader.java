package com.cc.job.datax.executor.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.datax.executor.model.DataXParams;

public interface BaseReader {

    JSONObject buildJson(DataXParams dataXParams);
}
