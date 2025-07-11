package com.cc.job.admin.task.datax;

import cn.hutool.json.JSONObject;
import com.cc.job.xo.model.datax.DataXParams;


public interface BaseRW {

    JSONObject buildJson(DataXParams dataXParams);
}
