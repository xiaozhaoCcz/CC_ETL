package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataX同步服务
 */
public class JobDataxService extends BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobDataxService.class);
    
    private static final String BASE_API = "/api/v1/datax";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    /**
     * 获取数据源的表列表
     */
    public List<String> getTables(Long datasourceId) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API + "/getTables/" + datasourceId;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String responseBody = response.body().string();
            Type resultType = new TypeToken<Result<List<String>>>(){}.getType();
            Result<List<String>> result = apiUtil.getGson().fromJson(responseBody, resultType);
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 获取表的字段列表
     */
    public List<String> getColumns(Long datasourceId, String tableName, String querySql) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API + "/getColumns/" + datasourceId;
        
        Map<String, String> params = new HashMap<>();
        if (tableName != null && !tableName.isEmpty()) {
            params.put("tableName", tableName);
        }
        if (querySql != null && !querySql.isEmpty()) {
            params.put("querySql", querySql);
        }
        
        String json = apiUtil.getGson().toJson(params);
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON))
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String responseBody = response.body().string();
            Type resultType = new TypeToken<Result<List<String>>>(){}.getType();
            Result<List<String>> result = apiUtil.getGson().fromJson(responseBody, resultType);
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 生成DataX JSON配置
     */
    public String getJson(DataXParams params) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API + "/getJson";
        String json = apiUtil.getGson().toJson(params);
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON))
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String responseBody = response.body().string();
            Type resultType = new TypeToken<Result<String>>(){}.getType();
            Result<String> result = apiUtil.getGson().fromJson(responseBody, resultType);
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * 批量生成DataX JSON配置
     */
    public String batchBuildJson(List<DataXParams> paramsList) throws IOException {
        String url = apiUtil.getBaseUrl() + BASE_API + "/batchBuildJson";
        String json = apiUtil.getGson().toJson(paramsList);
        
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, JSON))
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            String responseBody = response.body().string();
            Type resultType = new TypeToken<Result<String>>(){}.getType();
            Result<String> result = apiUtil.getGson().fromJson(responseBody, resultType);
            if (Result.isSuccess(result)) {
                return result.getData();
            } else {
                throw new IOException("API 返回错误: " + result.getMsg());
            }
        }
    }
    
    /**
     * DataX参数对象
     */
    public static class DataXParams {
        private List<String> columns;
        private String sourceType;
        private String username;
        private String password;
        private String dbName;
        private String tableName;
        private String ip;
        private String port;
        private String querySql;
        private Integer type; // 0=reader, 1=writer
        private Integer incrType;
        private String incrContent;
        private String schemaName;
        private String writeMode;
        
        // Getters and setters
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDbName() { return dbName; }
        public void setDbName(String dbName) { this.dbName = dbName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public String getPort() { return port; }
        public void setPort(String port) { this.port = port; }
        public String getQuerySql() { return querySql; }
        public void setQuerySql(String querySql) { this.querySql = querySql; }
        public Integer getType() { return type; }
        public void setType(Integer type) { this.type = type; }
        public Integer getIncrType() { return incrType; }
        public void setIncrType(Integer incrType) { this.incrType = incrType; }
        public String getIncrContent() { return incrContent; }
        public void setIncrContent(String incrContent) { this.incrContent = incrContent; }
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getWriteMode() { return writeMode; }
        public void setWriteMode(String writeMode) { this.writeMode = writeMode; }
    }
}
