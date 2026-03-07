package com.cc.job.admin.task.service.impl;

import cn.hutool.json.JSONObject;
import com.cc.job.admin.task.datax.reader.MysqlReader;
import com.cc.job.admin.task.datax.reader.OracleReader;
import com.cc.job.admin.task.datax.reader.PostgreSqlReader;
import com.cc.job.admin.task.datax.writer.MysqlWriter;
import com.cc.job.admin.task.datax.writer.OracleWriter;
import com.cc.job.admin.task.datax.writer.PostgreSqlWriter;
import com.cc.job.admin.task.enums.DatasourceEnum;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobJdbcDatasourceService;
import com.cc.job.xo.model.datax.DataXParams;
import com.cc.job.admin.task.service.DataxService;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.form.JobJdbcDatasourceForm;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataxServiceImpl implements DataxService {

    private final JobJdbcDatasourceService jobJdbcDatasourceService;

    private final JobInfoService jobInfoService;

    public DataxServiceImpl(JobJdbcDatasourceService jobJdbcDatasourceService, JobInfoService jobInfoService) {
        this.jobJdbcDatasourceService = jobJdbcDatasourceService;
        this.jobInfoService = jobInfoService;
    }

    @Override
    public String getJson(DataXParams dataXParams) {
        JSONObject jsonObject;
        DatasourceEnum datasourceEnum = DatasourceEnum.getSourceType(dataXParams.getSourceType());
        if (datasourceEnum == null) {
            return "";
        }
        jsonObject = switch (datasourceEnum) {
            case MYSQL ->
                    dataXParams.getType() == 0 ? new MysqlReader().buildJson(dataXParams) : new MysqlWriter().buildJson(dataXParams);
            case ORACLE ->
                    dataXParams.getType() == 0 ? new OracleReader().buildJson(dataXParams) : new OracleWriter().buildJson(dataXParams);
            case POSTGRESQL ->
                    dataXParams.getType() == 0 ? new PostgreSqlReader().buildJson(dataXParams) : new PostgreSqlWriter().buildJson(dataXParams);
            default -> null;
        };
        return jsonObject != null ? jsonObject.toString() : "";
    }

    @Override
    public String getPreviewReaderJson(DataXParams dataXParams, int limit) {
        if (dataXParams.getType() == null || dataXParams.getType() != 0) {
            return getJson(dataXParams);
        }
        DatasourceEnum datasourceEnum = DatasourceEnum.getSourceType(dataXParams.getSourceType());
        if (datasourceEnum == null) {
            return getJson(dataXParams);
        }
        DataXParams copy = new DataXParams();
        copy.setType(0);
        copy.setSourceType(dataXParams.getSourceType());
        copy.setUsername(dataXParams.getUsername());
        copy.setPassword(dataXParams.getPassword());
        copy.setDbName(dataXParams.getDbName());
        copy.setJdbcUrl(dataXParams.getJdbcUrl());
        copy.setTableName(dataXParams.getTableName());
        copy.setSchemaName(dataXParams.getSchemaName());
        copy.setIncrementType(0);
        List<String> columns = dataXParams.getColumns();
        if (columns == null || columns.isEmpty()) {
            return getJson(dataXParams);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 10000);
        if (datasourceEnum == DatasourceEnum.MYSQL || datasourceEnum == DatasourceEnum.POSTGRESQL) {
            String table = dataXParams.getTableName() != null ? dataXParams.getTableName() : "";
            String colList = String.join(", ", columns);
            copy.setQuerySql("SELECT " + colList + " FROM " + table + " LIMIT " + safeLimit);
            copy.setColumns(null);
        } else if (datasourceEnum == DatasourceEnum.ORACLE) {
            String table = dataXParams.getTableName() != null ? dataXParams.getTableName() : "";
            String colList = String.join(", ", columns);
            copy.setQuerySql("SELECT " + colList + " FROM (SELECT " + colList + " FROM " + table + ") WHERE ROWNUM <= " + safeLimit);
            copy.setColumns(null);
        } else {
            return getJson(dataXParams);
        }
        return getJson(copy);
    }

    @Override
    public List<String> batchBuildJson(Map<String, Object> params) {
        Object formData = params.get("formData");
        Object readers = params.get("readers");
        Object writers = params.get("writers");
        Object tableList = params.get("tableList");

        List<String> jsonList = new ArrayList<>();

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
                    dataXParamsR.setSchemaName(jdbcDatasourceFormDataR.getSchemaName());
                    List<String> tableNames = jobJdbcDatasourceService.getColumns(jdbcDatasourceFormDataR.getId(), new HashMap<>() {{
                        put("tableName", readerTableName);
                    }});
                    dataXParamsR.setTableName(readerTableName);
                    dataXParamsR.setColumns(tableNames);
                    dataXParamsR.setType(0);
                    dataXParamsR.setIncrementType(0);
                    String readerJson = this.getJson(dataXParamsR);

                    DataXParams dataXParamsW = new DataXParams();
                    dataXParamsW.setSourceType(String.valueOf(dsW));
                    dataXParamsW.setUsername(jdbcDatasourceFormDataW.getJdbcUsername());
                    dataXParamsW.setPassword(jdbcDatasourceFormDataW.getJdbcPassword());
                    dataXParamsW.setDbName(jdbcDatasourceFormDataW.getDatabaseName());
                    dataXParamsW.setJdbcUrl(jdbcDatasourceFormDataW.getJdbcUrl());
                    dataXParamsW.setSchemaName(jdbcDatasourceFormDataW.getSchemaName());
                    List<String> tableNamesW = jobJdbcDatasourceService.getColumns(jdbcDatasourceFormDataW.getId(), new HashMap<>() {{
                        put("tableName", writerTableName);
                    }});
                    dataXParamsW.setTableName(writerTableName);
                    dataXParamsW.setColumns(tableNamesW);
                    dataXParamsW.setType(1);
                    dataXParamsW.setIncrementType(0);
                    dataXParamsW.setWriteMode("update");
                    String writerJson = this.getJson(dataXParamsW);

                    String content = getContent(readerJson, writerJson);

                    if(tableList instanceof List<?>){
                        Object obj = ((List<?>) tableList).get(i);
                        JobInfoForm jobInfoFormData = getJobInfoFormData(formData);
                        if(obj instanceof Map<?, ?>){
                            jobInfoFormData.setJobDesc(String.valueOf(((Map<?, ?>) obj).get("jobDesc")));
                        }
                        jobInfoFormData.setExecutorParam(content);
                        jobInfoService.saveJobInfo(jobInfoFormData);
                    }
                }
            }
        }

        return jsonList;
    }


    private JobInfoForm getJobInfoFormData(Object formData){
        JobInfoForm jobInfoForm = new JobInfoForm();
        if(formData instanceof Map<?, ?>){
            jobInfoForm.setJobGroup(Long.valueOf(String.valueOf(((Map<?, ?>) formData).get("jobGroup"))));
            jobInfoForm.setAuthor(String.valueOf(((Map<?, ?>) formData).get("author")));
            jobInfoForm.setAlarmEmail(String.valueOf(((Map<?,?>) formData).get("alarmEmail")));
            jobInfoForm.setScheduleType(String.valueOf(((Map<?,?>) formData).get("scheduleType")));
            jobInfoForm.setScheduleConf(String.valueOf(((Map<?,?>) formData).get("scheduleConf")));
            jobInfoForm.setMisfireStrategy(String.valueOf(((Map<?,?>) formData).get("misfireStrategy")));
            jobInfoForm.setExecutorRouteStrategy(String.valueOf(((Map<?,?>) formData).get("executorRouteStrategy")));
            jobInfoForm.setExecutorBlockStrategy(String.valueOf(((Map<?,?>) formData).get("executorBlockStrategy")));
            jobInfoForm.setGlueType("DATAX");
            jobInfoForm.setExecutorHandler("runDataxHandler");
            jobInfoForm.setIncrementType(0);
        }
        return jobInfoForm;
    }

    private String getContent(String readerJson,String writerJson) {
        StringBuilder sb = new StringBuilder();
        String json = String.format("""
                {"reader":%s,"writer":%s}
                """, readerJson, writerJson);
        sb.append(json);
        return String.format("""
               {"job":{"content":[%s],"setting":{"speed":{"channel":3,"byte":-1},"errorLimit":{"record":0,"percentage":0.02} } }}
               """,sb);
    }
}

