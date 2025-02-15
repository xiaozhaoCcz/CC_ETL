package com.cc.job.admin.task.service.impl;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONObject;
import com.cc.job.admin.task.datax.reader.MysqlReader;
import com.cc.job.admin.task.datax.reader.OracleReader;
import com.cc.job.admin.task.datax.writer.MysqlWriter;
import com.cc.job.admin.task.datax.writer.OracleWriter;
import com.cc.job.admin.task.enums.DatasourceEnum;
import com.cc.job.admin.task.service.JobJdbcDatasourceService;
import com.cc.job.xo.model.datax.DataXParams;
import com.cc.job.admin.task.service.DataxService;
import com.cc.job.xo.model.form.JobJdbcDatasourceForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DataxServiceImpl implements DataxService {

    private final JobJdbcDatasourceService jobJdbcDatasourceService;

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

    @Override
    public String batchBuildJson(Map<String, Object> params) {
        Object readers = params.get("readers");
        Object writers = params.get("writers");

        List<Pair<String,String>> jsonList = new ArrayList<>();

        if(readers instanceof Map<?,?> && writers instanceof Map<?,?>){
            Object dsR = ((Map<?, ?>) readers).get("ds");
            Object dsW = ((Map<?, ?>) writers).get("ds");
            Object jdbcDatasourceIdR = ((Map<?, ?>) readers).get("jdbcDatasourceId");
            Object jdbcDatasourceIdW = ((Map<?,?>) writers).get("jdbcDatasourceId");
            Object tableListR = ((Map<?, ?>) readers).get("tableList");
            Object tableListW = ((Map<?, ?>) writers).get("tableList");
            if(tableListR instanceof List<?>&& tableListW instanceof List<?>){
                int length = ((List<?>) tableListR).size();
                for (int i = 0; i < length; i++) {
                    String readerTableName = ((List<?>) tableListR).get(i).toString();
                    String writerTableName = ((List<?>) tableListW).get(i).toString();
                    JobJdbcDatasourceForm jdbcDatasourceFormDataR = jobJdbcDatasourceService.getJdbcDatasourceFormData(Long.valueOf(String.valueOf(jdbcDatasourceIdR)));
                    JobJdbcDatasourceForm jdbcDatasourceFormDataW = jobJdbcDatasourceService.getJdbcDatasourceFormData(Long.valueOf(String.valueOf(jdbcDatasourceIdW)));

                    DataXParams dataXParamsR = new DataXParams();
                    dataXParamsR.setSourceType(String.valueOf(dsR));
                    dataXParamsR.setUsername(jdbcDatasourceFormDataR.getJdbcUsername());
                    dataXParamsR.setPassword(jdbcDatasourceFormDataR.getJdbcPassword());
                    dataXParamsR.setDbName(jdbcDatasourceFormDataR.getDatabaseName());
                    dataXParamsR.setJdbcUrl(jdbcDatasourceFormDataR.getJdbcUrl());
                    List<String> tableNames = jobJdbcDatasourceService.getColumns(jdbcDatasourceFormDataR.getId(), new HashMap<>() {{
                        put("tableName", readerTableName);
                    }});
                    dataXParamsR.setTableName(readerTableName);
                    dataXParamsR.setColumns(tableNames);
                    dataXParamsR.setType(0);
                    dataXParamsR.setIncrType(0);
                    String readerJson = this.getJson(dataXParamsR);
                    System.out.println(readerJson);

                    DataXParams dataXParamsW = new DataXParams();
                    dataXParamsW.setSourceType(String.valueOf(dsW));
                    dataXParamsW.setUsername(jdbcDatasourceFormDataW.getJdbcUsername());
                    dataXParamsW.setPassword(jdbcDatasourceFormDataW.getJdbcPassword());
                    dataXParamsW.setDbName(jdbcDatasourceFormDataW.getDatabaseName());
                    dataXParamsW.setJdbcUrl(jdbcDatasourceFormDataW.getJdbcUrl());
                    List<String> tableNamesW = jobJdbcDatasourceService.getColumns(jdbcDatasourceFormDataW.getId(), new HashMap<>() {{
                        put("tableName", writerTableName);
                    }});
                    dataXParamsW.setTableName(writerTableName);
                    dataXParamsW.setColumns(tableNamesW);
                    dataXParamsW.setType(0);
                    dataXParamsW.setIncrType(0);
                    dataXParamsW.setWriteMode("update");
                    String writerJson = this.getJson(dataXParamsW);

                    // 构建json文件
                    jsonList.add(Pair.of(readerJson,writerJson));
                }
            }
        }

        return this.getContent(jsonList);
    }

    private String getContent(List<Pair<String,String>> jsonList) {
        StringBuilder sb = new StringBuilder();
        jsonList.forEach(p -> {
            String json = String.format("""
                    {"reader":%s,"writer":%s}
                    """, p.getKey(), p.getValue());
            sb.append(json);
            sb.append(",");
        });
        sb.deleteCharAt(sb.length()-1);
        return String.format("""
               {"job":{"content":[%s],"setting":{"speed":{"channel":3,"byte":-1},"errorLimit":{"record":0,"percentage":0.02} } }}
               """,sb);
    }
}

