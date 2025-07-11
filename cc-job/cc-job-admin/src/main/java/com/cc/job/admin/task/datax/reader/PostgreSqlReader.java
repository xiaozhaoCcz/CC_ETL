package com.cc.job.admin.task.datax.reader;

import cn.hutool.json.JSONObject;
import com.cc.job.admin.task.datax.BaseRW;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.model.datax.DataXParams;
import org.apache.commons.lang3.StringUtils;

import static com.cc.job.xo.constant.DataxConstant.*;

/**
 * @author astro
 * @description: PostgreSqlReader
 * @date 2025/2/1 16:34
 */
public class PostgreSqlReader implements BaseRW {
    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce(DataxConstant.NAME,POSTGRESQL_READER);
        JSONObject parameter = new JSONObject();
        parameter.putOnce(USERNAME, dataXParams.getUsername());
        parameter.putOnce(PASSWORD, dataXParams.getPassword());
        JSONObject connection = new JSONObject();
        connection.append(JDBC_URL, String.format(POSTGRESQL_JDBC_URL, dataXParams.getIp(), dataXParams.getPort(), dataXParams.getDbName()));

        if(dataXParams.getColumns() != null && !dataXParams.getColumns().isEmpty()) {
            connection.append(TABLE, dataXParams.getTableName());
            parameter.putOnce(COLUMN, dataXParams.getColumns());
        } else {
            if (StringUtils.isBlank(dataXParams.getQuerySql())) {
                throw new BusinessException(ERROR_COLUMN_EMPTY);
            }
            connection.append(QUERY_SQL, dataXParams.getQuerySql());
        }

        parameter.append(CONNECTION, connection);
        readerConfig.putOnce(PARAMETER, parameter);
        return readerConfig;
    }
}
