package com.cc.job.admin.task.datax.writer;

import cn.hutool.json.JSONObject;
import com.cc.job.admin.task.datax.BaseRW;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.model.datax.DataXParams;

import static com.cc.job.xo.constant.DataxConstant.*;
import static com.cc.job.xo.constant.DataxConstant.PARAMETER;

public class OracleWriter implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        // 生成 MySQL reader 配置
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce(DataxConstant.NAME, ORACLE_WRITER);
        JSONObject parameter = new JSONObject();
        parameter.putOnce(USERNAME, dataXParams.getUsername());
        parameter.putOnce(PASSWORD, dataXParams.getPassword());
        parameter.append(CONNECTION,new JSONObject()
                .putOnce(JDBC_URL,  String.format(ORACLE_JDBC_URL,dataXParams.getIp(),dataXParams.getPort(),dataXParams.getDbName()))
                .append(TABLE, dataXParams.getTableName()));
        parameter.putOnce(COLUMN, dataXParams.getColumns());
        readerConfig.putOnce(PARAMETER, parameter);
        return readerConfig;
    }
}
