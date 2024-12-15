package com.cc.job.task.datax;

import cn.hutool.json.JSONObject;
import com.cc.job.task.model.datax.DataXParams;


public interface BaseRW {

    JSONObject buildJson(DataXParams dataXParams);
}
