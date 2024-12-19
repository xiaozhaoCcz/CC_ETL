package com.cc.job.task.datax.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.BaseRW;
import com.cc.job.xo.model.datax.DataXParams;
import org.apache.commons.lang3.StringUtils;


public class MysqlReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        // 生成 MySQL reader 配置
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce("name", "mysqlreader");
        JSONObject parameter = new JSONObject();
        parameter.putOnce("username", dataXParams.getUsername());
        parameter.putOnce("password", dataXParams.getPassword());
        JSONObject connection = new JSONObject();
        connection.append("jdbcUrl", "jdbc:mysql://" + dataXParams.getIp() + ":" + dataXParams.getPort()+"/"+dataXParams.getDbName());
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
