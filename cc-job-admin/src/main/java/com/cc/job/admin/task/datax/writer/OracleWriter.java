package com.cc.job.admin.task.datax.writer;

import cn.hutool.json.JSONObject;
import com.cc.job.admin.task.datax.BaseRW;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.model.datax.DataXParams;
import org.apache.commons.lang3.StringUtils;

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
        JSONObject connection = new JSONObject();
        if(StringUtils.isNoneBlank(dataXParams.getJdbcUrl())){
            connection.putOnce(JDBC_URL, dataXParams.getJdbcUrl());
        }else{
            connection.putOnce(JDBC_URL,  String.format(ORACLE_JDBC_URL,dataXParams.getIp(),dataXParams.getPort(),dataXParams.getDbName()));
        }
        String tableName = dataXParams.getSchemaName()+"."+dataXParams.getTableName();
        connection.append(TABLE, tableName);
        parameter.append(CONNECTION,connection);
        parameter.putOnce(COLUMN, dataXParams.getColumns());
        readerConfig.putOnce(PARAMETER, parameter);
        return readerConfig;
    }
}
