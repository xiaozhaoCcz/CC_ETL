package com.cc.job.task.datax.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.BaseRW;
import com.cc.job.task.model.datax.DataXParams;

import java.util.Map;

public class OracleReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        // 生成 MySQL reader 配置
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce("name", "oraclereader");
        JSONObject parameter = new JSONObject();
        parameter.putOnce("username", dataXParams.getUsername());
        parameter.putOnce("password", dataXParams.getPassword());
        parameter.append("connection",new JSONObject()
                .append("jdbcUrl", "jdbc:oracle:thin:@" + dataXParams.getIp() + ":" + dataXParams.getPort()+"/"+dataXParams.getDbName())
                .append("table", dataXParams.getTableName()));
        parameter.putOnce("column", dataXParams.getColumns());
        if(dataXParams.getOtherParams()!=null){
            for (Map.Entry<String, String> entry : dataXParams.getOtherParams().entrySet()) {
                parameter.putOnce(entry.getKey(), entry.getValue());
            }
        }
        readerConfig.putOnce("parameter", parameter);
        return readerConfig;
    }
}
