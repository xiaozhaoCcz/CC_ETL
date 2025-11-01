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
     * ReturnT包装类（用于解析后端返回的ReturnT结构）
     */
    private static class ReturnTWrapper {
        private int code;
        private String msg;
        private LogResultWrapper content;
        
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
        
        public LogResultWrapper getContent() {
            return content;
        }
        
        public void setContent(LogResultWrapper content) {
            this.content = content;
        }
    }
    
    /**
     * LogResult包装类（用于解析后端返回的LogResult结构）
     */
    private static class LogResultWrapper {
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
     * 获取任务执行日志
     * @param logId 日志ID
     * @param fromLineNum 起始行号
     * @return 日志详细响应
     * @throws IOException 网络异常
     */
    public LogDetailResponse getLogDetail(Long logId, int fromLineNum) throws IOException {
        // 修复1: 使用正确的API路径（jobLogs复数）
        String url = apiUtil.getBaseUrl() + "/api/v1/jobLogs/logDetailCat";
        
        // 修复2: 使用正确的参数名（logId而不是id）
        url += "?logId=" + logId + "&fromLineNum=" + fromLineNum;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = apiUtil.getClient().newCall(request).execute()) {
            String responseBody = response.body().string();
            
            // 打印完整的响应以便调试
            System.out.println("getLogDetail API URL: " + url);
            System.out.println("getLogDetail API 响应状态: " + response.code());
            System.out.println("getLogDetail API 响应 (截取): " + 
                (responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody));
            
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: HTTP " + response.code() + " - " + responseBody);
            }
            
            // 修复3: 后端返回的是 Result<ReturnT<LogResult>> 嵌套结构
            // 需要解析嵌套结构
            // 注意：Result的code是String类型，值为"200"
            Type resultType = new TypeToken<Result<ReturnTWrapper>>(){}.getType();
            Result<ReturnTWrapper> result = apiUtil.getGson().fromJson(responseBody, resultType);
            
            if (result == null || !Result.isSuccess(result)) {
                throw new IOException("API 返回错误: " + (result != null ? result.getMsg() : "未知错误"));
            }
            
            // 提取内部的ReturnT数据
            ReturnTWrapper returnTWrapper = result.getData();
            if (returnTWrapper == null) {
                throw new IOException("API 返回数据为空");
            }
            
            // 检查ReturnT的状态码
            if (returnTWrapper.getCode() != 200) {
                throw new IOException("任务日志获取失败: " + returnTWrapper.getMsg());
            }
            
            // 提取LogResult内容
            LogResultWrapper logResult = returnTWrapper.getContent();
            if (logResult == null) {
                throw new IOException("日志内容为空");
            }
            
            // 转换为LogDetailResponse格式
            LogDetailResponse logResponse = new LogDetailResponse();
            logResponse.setCode(200);
            logResponse.setMsg("success");
            
            LogContent content = new LogContent();
            content.setFromLineNum(logResult.getFromLineNum());
            content.setToLineNum(logResult.getToLineNum());
            content.setLogContent(logResult.getLogContent());
            content.setEnd(logResult.isEnd());
            
            logResponse.setContent(content);
            
            return logResponse;
        }
    }
}

