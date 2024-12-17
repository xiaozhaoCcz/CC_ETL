package com.cc.job.task.datax.writer;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.BaseRW;
import com.cc.job.task.model.datax.DataXParams;

import java.util.Map;

public class MysqlWriter implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        // 生成 MySQL reader 配置
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce("name", "mysqlwriter");
        JSONObject parameter = new JSONObject();
        parameter.putOnce("username", dataXParams.getUsername());
        parameter.putOnce("password", dataXParams.getPassword());
        parameter.append("connection",new JSONObject()
                .putOnce("jdbcUrl", "jdbc:mysql://" + dataXParams.getIp() + ":" + dataXParams.getPort()+"/"+dataXParams.getDbName())
                .append("table", dataXParams.getTableName()));
        parameter.putOnce("column", dataXParams.getColumns());
        parameter.putOnce("writeMode", dataXParams.getWriteMode());
        readerConfig.putOnce("parameter", parameter);
        return readerConfig;
    }
}
