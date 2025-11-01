package com.example.nodefx.service;

import com.cc.job.xo.common.result.Result;
import com.google.gson.reflect.TypeToken;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 任务日志服务
 */
public class JobLogService extends BaseService {
    
    /**
     * 日志内容数据结构
     */
    public static class LogContent {
        private int fromLineNum;
        private int toLineNum;
        private String logContent;
        private boolean end;
        
        public int getFromLineNum() {
            return fromLineNum;
        }
        
        public void setFromLineNum(int fromLineNum) {
            this.fromLineNum = fromLineNum;
        }
        
        public int getToLineNum() {
            return toLineNum;
        }
        
        public void setToLineNum(int toLineNum) {
            this.toLineNum = toLineNum;
        }
        
        public String getLogContent() {
            return logContent;
        }
        
        public void setLogContent(String logContent) {
            this.logContent = logContent;
        }
        
        public boolean isEnd() {
            return end;
        }
        
        public void setEnd(boolean end) {
            this.end = end;
        }
    }
    
    /**
     * 日志详细响应数据结构
     */
    public static class LogDetailResponse {
        private int code;
        private String msg;
        private LogContent content;
        
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMsg() {
            return msg;
        }
        
        public void setMsg(String msg) {
            this.msg = msg;
        }
        
        public LogContent getContent() {
            return content;
        }
        
        public void setContent(LogContent content) {
            this.content = content;
        }
        
        public boolean isSuccess() {
            return code == 200;
        }
    }
    
    /**
     * 获取任务执行日志
     * @param logId 日志ID
     * @param fromLineNum 起始行号
     * @return 日志详细响应
     * @throws IOException 网络异常
     */
    public LogDetailResponse getLogDetail(Long logId, int fromLineNum) throws IOException {
        String url = apiUtil.getBaseUrl() + "/api/v1/jobLog/logDetailCat";
        
        // 添加查询参数
        url += "?id=" + logId + "&fromLineNum=" + fromLineNum;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response);
            }
            
            String responseBody = response.body().string();
            // 由于日志内容可能很长，这里只打印部分信息
            System.out.println("getLogDetail API 响应 (截取): " + 
                (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody));
            
            // 解析 JSON 响应
            LogDetailResponse logResponse = apiUtil.getGson().fromJson(responseBody, LogDetailResponse.class);
            
            return logResponse;
        }
    }
}

