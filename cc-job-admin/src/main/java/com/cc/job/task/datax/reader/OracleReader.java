package com.cc.job.task.datax.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.BaseRW;
import com.cc.job.task.model.datax.DataXParams;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class OracleReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce("name", "oraclereader");
        JSONObject parameter = new JSONObject();
        parameter.putOnce("username", dataXParams.getUsername());
        parameter.putOnce("password", dataXParams.getPassword());
        JSONObject connection = new JSONObject();
        connection.append("jdbcUrl", "jdbc:oracle:thin:@" + dataXParams.getIp() + ":" + dataXParams.getPort()+"/"+dataXParams.getDbName());
        if(StringUtils.isNotBlank(dataXParams.getTableName())){
            connection.append("table", dataXParams.getTableName());
        }

        if(StringUtils.isNotBlank(dataXParams.getQuerySql())){
            connection.append("querySql", dataXParams.getQuerySql());
        }

        parameter.append("connection",connection);
        if(dataXParams.getColumns()!=null&&!dataXParams.getColumns().isEmpty()){
            parameter.putOnce("column", dataXParams.getColumns());
        }
        readerConfig.putOnce("parameter", parameter);
        return readerConfig;
    }
}
