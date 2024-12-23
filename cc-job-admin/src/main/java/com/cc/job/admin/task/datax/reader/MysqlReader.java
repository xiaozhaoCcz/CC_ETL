package com.cc.job.admin.task.datax.reader;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cc.job.admin.task.datax.BaseRW;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.model.datax.DataXParams;
import com.cc.job.xo.model.datax.DataxColumn;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.cc.job.xo.constant.DataxConstant.*;


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

        if(dataXParams.getColumns()!=null&&!dataXParams.getColumns().isEmpty()){
            connection.append(TABLE, dataXParams.getTableName());
            parameter.putOnce(COLUMN, dataXParams.getColumns());
            if (dataXParams.getIncrType() == 1) {
                // 增量同步
                JSONArray jsonArray = JSONUtil.parseArray(dataXParams.getIncrContent());
                List<DataxColumn> columnList = jsonArray.toList(DataxColumn.class);
                if (columnList.isEmpty()) {
                    throw new BusinessException(ERROR_INCREMENT_CONTENT_EMPTY);
                }
                StringBuilder sb = new StringBuilder();

                for (DataxColumn dataxColumn : columnList) {
                    sb.append(dataxColumn.getColumnKey())
                            .append(SPACE)
                            .append(GREATER)
                            .append(SPACE)
                            .append(DOLLAR_SIGN)
                            .append(LEFT_CURLY_BRACKET)
                            .append(dataxColumn.getColumnParam())
                            .append(RIGHT_CURLY_BRACKET)
                            .append(SPACE)
                            .append(AND)
                            .append(SPACE);
                }
                sb.delete(sb.length() - 4, sb.length());
                parameter.putOnce(WHERE, sb.toString());
            }
        }else{
             if(StringUtils.isBlank(dataXParams.getQuerySql())) {
                 throw new BusinessException(ERROR_COLUMN_EMPTY);
             }
            connection.append(QUERY_SQL, dataXParams.getQuerySql());
        }
        parameter.append(CONNECTION, connection);
        readerConfig.putOnce(PARAMETER, parameter);
        return readerConfig;
    }
}
