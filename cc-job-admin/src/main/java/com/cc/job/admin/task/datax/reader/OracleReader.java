package com.cc.job.admin.task.datax.reader;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cc.job.admin.task.datax.BaseRW;
import com.cc.job.admin.task.enums.DatasourceEnum;
import com.cc.job.admin.task.utils.DataxUtils;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.xo.constant.DataxConstant;
import com.cc.job.xo.model.datax.DataXParams;
import com.cc.job.xo.model.datax.DataxColumn;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.cc.job.xo.constant.DataxConstant.*;

public class OracleReader implements BaseRW {

    @Override
    public JSONObject buildJson(DataXParams dataXParams) {
        JSONObject readerConfig = new JSONObject();
        readerConfig.putOnce(DataxConstant.NAME, ORACLE_READER);
        JSONObject parameter = new JSONObject();
        parameter.putOnce(USERNAME, dataXParams.getUsername());
        parameter.putOnce(PASSWORD, dataXParams.getPassword());
        JSONObject connection = new JSONObject();

        if (StringUtils.isNoneBlank(dataXParams.getJdbcUrl())) {
            connection.append(JDBC_URL, dataXParams.getJdbcUrl());
        } else {
            connection.append(JDBC_URL, String.format(ORACLE_JDBC_URL, dataXParams.getIp(), dataXParams.getPort(), dataXParams.getDbName()));
        }

        if (dataXParams.getColumns() != null && !dataXParams.getColumns().isEmpty()) {
            String tableName = dataXParams.getSchemaName() + "." + dataXParams.getTableName();
            connection.append(TABLE, tableName);
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
                            .append(SPACE);

                    if (dataxColumn.getColumnType() == 1 && !"x".equalsIgnoreCase(dataxColumn.getColumnTimeFormat())) {
                        sb.append(TO_DATE)
                                .append(LEFT_PARENTHESIS)
                                .append(TO_CHAR)
                                .append(LEFT_PARENTHESIS)
                                .append(DOLLAR_SIGN)
                                .append(LEFT_CURLY_BRACKET)
                                .append(dataxColumn.getColumnParam())
                                .append(RIGHT_CURLY_BRACKET)
                                .append(SPACE)
                                .append(OBLIQUE)
                                .append(LEFT_PARENTHESIS)
                                .append(60 * 60 * 24)
                                .append(RIGHT_PARENTHESIS)
                                .append(ORACLE_DATE)
                                .append(RIGHT_PARENTHESIS)
                                .append(SPLIT)
                                .append(SINGLE_QUOTE)
                                .append(TIME_FORMAT1)
                                .append(SINGLE_QUOTE)
                                .append(RIGHT_PARENTHESIS)
                                .append(SPLIT)
                                .append(SINGLE_QUOTE)
                                .append(DataxUtils.getDateFormat(DatasourceEnum.ORACLE, dataxColumn.getColumnTimeFormat()))
                                .append(SINGLE_QUOTE)
                                .append(RIGHT_PARENTHESIS);
                    } else {
                        // 非字符串类型
                        sb.append(DOLLAR_SIGN)
                                .append(LEFT_CURLY_BRACKET)
                                .append(dataxColumn.getColumnParam())
                                .append(RIGHT_CURLY_BRACKET);
                    }

                    sb.append(SPACE)
                            .append(AND)
                            .append(SPACE);
                }
                sb.delete(sb.length() - 4, sb.length());
                parameter.putOnce(WHERE, sb.toString());
            }
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
