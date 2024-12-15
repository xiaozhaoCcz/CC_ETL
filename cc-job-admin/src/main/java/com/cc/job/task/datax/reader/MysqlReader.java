package com.cc.job.task.datax.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.BaseRW;
import com.cc.job.task.model.datax.DataXParams;

import java.util.Map;


public class MysqlReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        // 生成 MySQL reader 配置
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce("name", "mysqlreader");
        JSONObject parameter = new JSONObject();
        parameter.putOnce("username", dataXParams.getUsername());
        parameter.putOnce("password", dataXParams.getPassword());
        parameter.append("connection",new JSONObject()
                .append("jdbcUrl", "jdbc:mysql://" + dataXParams.getIp() + ":" + dataXParams.getPort()+"/"+dataXParams.getDbName())
                .append("table", dataXParams.getTableName()));
        parameter.putOnce("column", dataXParams.getColumns());
        parameter.putOnce("splitPk", "id");
        if(dataXParams.getOtherParams()!=null){
            for (Map.Entry<String, String> entry : dataXParams.getOtherParams().entrySet()) {
                parameter.putOnce(entry.getKey(), entry.getValue());
            }
        }
        readerConfig.putOnce("parameter", parameter);
        return readerConfig;
    }
}
