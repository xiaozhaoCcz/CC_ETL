package com.cc.job.admin.task.datax.reader;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cc.job.admin.task.constant.DataxConstant;
import com.cc.job.admin.task.datax.BaseRW;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.model.datax.DataXParams;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import static com.cc.job.admin.task.constant.DataxConstant.*;


public class MysqlReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        // 生成 MySQL reader 配置
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce(DataxConstant.NAME, MYSQL_READER);
        JSONObject parameter = new JSONObject();
        parameter.putOnce(USERNAME, dataXParams.getUsername());
        parameter.putOnce(PASSWORD, dataXParams.getPassword());
        JSONObject connection = new JSONObject();
        connection.append(JDBC_URL, String.format(MYSQL_JDBC_URL, dataXParams.getIp(), dataXParams.getPort(), dataXParams.getDbName()));

        // 如果有查询语句，就使用查询语句
        if (StringUtils.isNotBlank(dataXParams.getQuerySql())) {
            connection.append(QUERY_SQL, dataXParams.getQuerySql());
        } else {
            connection.append(TABLE, dataXParams.getTableName());
            if (dataXParams.getColumns() == null || dataXParams.getColumns().isEmpty()) {
                throw new BusinessException(ERROR_COLUMN_EMPTY);
            }
            parameter.putOnce(COLUMN, dataXParams.getColumns());
            if (dataXParams.getIncrType() == 1) {
                // 增量同步
                JSONArray jsonArray = JSONUtil.parseArray(dataXParams.getIncrContent());
                List<Map> columnMap = jsonArray.toList(Map.class);
                if (columnMap.isEmpty()) {
                    throw new BusinessException(ERROR_INCREMENT_CONTENT_EMPTY);
                }
                StringBuilder sb = new StringBuilder();
                for (Map data : columnMap) {
                    String columnKey = (String) data.get(COLUMN_KEY);
                    String columnValue = (String) data.get(COLUMN_VALUE);
                    sb.append(columnKey).append(SPACE).append(GREATER).append(SPACE).append(columnValue).append(SPACE).append(AND).append(SPACE);
                }
                sb.delete(sb.length() - 4, sb.length());
                parameter.putOnce(WHERE, sb.toString());
            }
        }
        parameter.append(CONNECTION, connection);
        readerConfig.putOnce(PARAMETER, parameter);
        return readerConfig;
    }
}
