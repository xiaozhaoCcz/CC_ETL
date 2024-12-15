package com.cc.job.task.datax.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.BaseRW;
import com.cc.job.task.model.datax.DataXParams;

import java.util.Map;

public class CsvReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        // 生成 CSV reader 配置
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce("name", "txtfilereader");
        JSONObject parameter = new JSONObject();
        parameter.putOnce("column", new JSONObject[]{/* column definitions */});
        if(dataXParams.getOtherParams()!=null){
            for (Map.Entry<String, String> entry : dataXParams.getOtherParams().entrySet()) {
                parameter.putOnce(entry.getKey(), entry.getValue());
            }
        }
        readerConfig.putOnce("parameter", parameter);
        return readerConfig;
    }
}
