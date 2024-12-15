package com.cc.job.task.service.impl;

import cn.hutool.json.JSONObject;
import com.cc.job.task.datax.reader.MysqlReader;
import com.cc.job.task.datax.reader.OracleReader;
import com.cc.job.task.datax.writer.MysqlWriter;
import com.cc.job.task.datax.writer.OracleWriter;
import com.cc.job.task.enums.DatasourceEnum;
import com.cc.job.task.model.datax.DataXParams;
import com.cc.job.task.service.DataxService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DataxServiceImpl implements DataxService {
    @Override
    public String getJson(DataXParams dataXParams) {
        JSONObject jsonObject;
        DatasourceEnum datasourceEnum = DatasourceEnum.getSourceType(dataXParams.getSourceType());
        jsonObject = switch (Objects.requireNonNull(datasourceEnum)) {
            case MYSQL ->
                    dataXParams.getType() == 0 ? new MysqlReader().buildJson(dataXParams) : new MysqlWriter().buildJson(dataXParams);
            case ORACLE ->
                    dataXParams.getType() == 0 ? new OracleReader().buildJson(dataXParams) : new OracleWriter().buildJson(dataXParams);
        };
        return jsonObject.toString();
    }
}
