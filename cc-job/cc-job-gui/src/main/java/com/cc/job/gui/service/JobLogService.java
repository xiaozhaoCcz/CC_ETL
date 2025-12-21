package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.query.JobLogQuery;
import com.cc.job.xo.model.vo.JobLogVO;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务日志服务
 */
public class JobLogService extends BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobLogService.class);
    
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
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("logId", String.valueOf(logId));
        queryParams.put("fromLineNum", String.valueOf(fromLineNum));
        
        // 后端返回的是 Result<ReturnT<LogResult>> 嵌套结构
        TypeToken<ReturnTWrapper> typeToken = new TypeToken<ReturnTWrapper>(){};
        Result<ReturnTWrapper> result = httpClient.get("/api/v1/jobLogs/logDetailCat", typeToken, queryParams);
        
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
    
    /**
     * 分页查询日志列表
     * @param query 查询参数
     * @return 分页结果
     * @throws IOException 网络异常
     */
    public PageResult<JobLogVO> getJobLogPage(JobLogQuery query) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null) {
            queryParams.put("pageNum", String.valueOf(query.getPageNum()));
            queryParams.put("pageSize", String.valueOf(query.getPageSize()));
            if (query.getJobId() != null) {
                queryParams.put("jobId", String.valueOf(query.getJobId()));
            }
            if (query.getJobGroup() != null) {
                queryParams.put("jobGroup", String.valueOf(query.getJobGroup()));
            }
            if (query.getLogStatus() != null) {
                queryParams.put("logStatus", String.valueOf(query.getLogStatus()));
            }
            if (query.getFilterTime() != null && query.getFilterTime().length == 2) {
                queryParams.put("filterTime[0]", query.getFilterTime()[0]);
                queryParams.put("filterTime[1]", query.getFilterTime()[1]);
            }
        }
        return httpClient.getPage("/api/v1/jobLogs/page", JobLogVO.class, queryParams);
    }
    
    /**
     * 删除日志
     * @param query 查询参数（用于指定删除条件）
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean deleteJobLogs(JobLogQuery query) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null) {
            if (query.getJobId() != null) {
                queryParams.put("jobId", String.valueOf(query.getJobId()));
            }
            if (query.getJobGroup() != null) {
                queryParams.put("jobGroup", String.valueOf(query.getJobGroup()));
            }
            if (query.getLogStatus() != null) {
                queryParams.put("logStatus", String.valueOf(query.getLogStatus()));
            }
            if (query.getFilterTime() != null && query.getFilterTime().length == 2) {
                queryParams.put("filterTime[0]", query.getFilterTime()[0]);
                queryParams.put("filterTime[1]", query.getFilterTime()[1]);
            }
        }
        return httpClient.deleteForBoolean("/api/v1/jobLogs", queryParams);
    }
}


