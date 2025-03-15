package com.cc.job.admin.task.datax.writer;

import cn.hutool.json.JSONObject;
import com.cc.job.admin.task.datax.BaseRW;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.model.datax.DataXParams;

import static com.cc.job.xo.constant.DataxConstant.*;

/**
 * @author astro
 * @description: PostgreSqlWriter
 * @date 2025/2/23 15:59
 */
public class PostgreSqlWriter implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce(DataxConstant.NAME, POSTGRESQL_WRITER);
        JSONObject parameter = new JSONObject();
        parameter.putOnce(USERNAME, dataXParams.getUsername());
        parameter.putOnce(PASSWORD, dataXParams.getPassword());
        parameter.append(CONNECTION,new JSONObject()
                .putOnce(JDBC_URL,  String.format(POSTGRESQL_JDBC_URL,dataXParams.getIp(),dataXParams.getPort(),dataXParams.getDbName()))
                .append(TABLE, dataXParams.getTableName()));
        parameter.putOnce(COLUMN, dataXParams.getColumns());
        readerConfig.putOnce(PARAMETER, parameter);
        return readerConfig;
    }
}
