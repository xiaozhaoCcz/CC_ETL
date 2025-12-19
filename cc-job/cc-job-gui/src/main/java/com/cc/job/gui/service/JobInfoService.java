package com.cc.job.gui.service;

import com.cc.job.gui.util.SessionManager;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.dto.JobInfoTriggerDto;
import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobLogglue;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobEdgeForm;
import com.cc.job.xo.model.form.JobGlueForm;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 任务信息服务
 */
public class JobInfoService extends BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobInfoService.class);
    
    /**
     * 触发任务执行
     * @param jobId 任务组ID
     * @param executorParam 执行参数（randomId）
     * @return 执行日志ID
     * @throws IOException 网络异常
     */
    public Long triggerJob(Long jobId, String executorParam) throws IOException {
        // 构建请求参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("id", jobId);
        requestMap.put("executorParam", executorParam);
        
        // 添加触发用户ID（从SessionManager获取）
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn() && session.getUserId() != null) {
            try {
                Integer triggerUserId = Integer.parseInt(session.getUserId());
                requestMap.put("triggerUserId", triggerUserId);
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
        
        Result<String> result = httpClient.post("/api/v1/jobInfos/trigger", requestMap, String.class);
        String data = httpClient.extractData(result, "触发任务执行失败");
        
        if (data.trim().isEmpty()) {
            throw new IOException("API 返回成功但数据为空，无法获取日志ID");
        }
        try {
            return Long.parseLong(data.trim());
        } catch (NumberFormatException e) {
            throw new IOException("API 返回的数据格式错误，无法解析为日志ID: " + data, e);
        }
    }
    
    /**
     * 停止任务组
     * @param jobId 任务组ID
     * @param randomId 执行批次ID
     * @throws IOException 网络异常
     */
    public void stopJobCompose(Long jobId, String randomId) throws IOException {
        String path = "/api/v1/jobInfos/stopJobCompose/" + jobId + "/" + randomId;
        Result<Void> result = httpClient.get(path, Void.class);
        if (!Result.isSuccess(result)) {
            throw new IOException("API 返回错误: " + result.getMsg());
        }
    }
    
    /**
     * 获取任务运行状态
     * @param jobId 任务组ID
     * @return 是否正在运行
     * @throws IOException 网络异常
     */
    public boolean getJobStatus(Long jobId) throws IOException {
        String path = "/api/v1/jobInfos/getJobStatus/" + jobId;
        Result<Boolean> result = httpClient.get(path, Boolean.class);
        Boolean data = httpClient.extractDataOrNull(result, "获取任务运行状态失败");
        return data != null && data;
    }

    /**
     * 分页查询任务列表
     * @param query 查询参数
     * @return 分页结果
     * @throws IOException 网络异常
     */
    public PageResult<JobInfoVO> getJobInfoPage(JobInfoQuery query) throws IOException {
        Map<String, Function<JobInfoQuery, Object>> extractors = new HashMap<>();
        extractors.put("pageNum", JobInfoQuery::getPageNum);
        extractors.put("pageSize", JobInfoQuery::getPageSize);
        extractors.put("jobGroup", JobInfoQuery::getJobGroup);
        extractors.put("triggerStatus", JobInfoQuery::getTriggerStatus);
        extractors.put("jobDesc", JobInfoQuery::getJobDesc);
        extractors.put("executorHandler", JobInfoQuery::getExecutorHandler);
        extractors.put("author", JobInfoQuery::getAuthor);
        
        Map<String, String> queryParams = HttpClientUtil.buildQueryParams(query, extractors);
        return httpClient.getPage("/api/v1/jobInfos/page", JobInfoVO.class, queryParams);
    }

    /**
     * 启动作业
     */
    public boolean startJob(Long jobId) throws IOException {
        String path = "/api/v1/jobInfos/startJob/" + jobId;
        Result<Void> result = httpClient.get(path, Void.class);
        return Result.isSuccess(result);
    }

    /**
     * 停止作业
     */
    public boolean stopJob(Long jobId) throws IOException {
        String path = "/api/v1/jobInfos/stopJob/" + jobId;
        Result<Void> result = httpClient.get(path, Void.class);
        return Result.isSuccess(result);
    }

    /**
     * 执行一次
     */
    public String triggerOnce(Long jobId, String executorParam) throws IOException {
        JobInfoTriggerDto dto = new JobInfoTriggerDto();
        dto.setId(jobId);
        dto.setExecutorParam(executorParam);

        // 添加触发用户ID（从SessionManager获取）
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn() && session.getUserId() != null) {
            try {
                Integer triggerUserId = Integer.parseInt(session.getUserId());
                dto.setTriggerUserId(triggerUserId);
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }

        Result<String> result = httpClient.post("/api/v1/jobInfos/trigger", dto, String.class);
        return httpClient.extractDataOrNull(result, "执行任务失败");
    }
    
    /**
     * 保存任务组
     * @param formData 任务组表单数据
     * @return 是否保存成功
     * @throws IOException 网络异常
     */
    public boolean saveJobCompose(JobInfoForm formData) throws IOException {
        return httpClient.postForBoolean("/api/v1/jobInfos/saveJobCompose", formData);
    }
    
    /**
     * 更新任务组
     * @param id 任务组ID
     * @param formData 任务组表单数据
     * @return 是否更新成功
     * @throws IOException 网络异常
     */
    public boolean updateJobCompose(Long id, JobInfoForm formData) throws IOException {
        return httpClient.putForBoolean("/api/v1/jobInfos/updateJobCompose/" + id, formData);
    }

    /**
     * 保存任务节点
     * @param formData 任务节点表单数据
     * @return 节点信息（包含节点ID和jobId）
     * @throws IOException 网络异常
     */
    public long saveJobInfo(JobInfoForm formData) throws IOException {
        Result<Long> result = httpClient.post("/api/v1/jobInfos", formData, Long.class);
        Long data = httpClient.extractData(result, "保存任务节点失败");
        return data;
    }

    /**
     * 保存任务节点
     * @param formData 任务节点表单数据
     * @return 节点信息（包含节点ID和jobId）
     * @throws IOException 网络异常
     */
    public JobNode saveJobNode(JobInfoForm formData) throws IOException {
        Result<JobNode> result = httpClient.post("/api/v1/jobInfos/saveJobNode", formData, JobNode.class);
        return httpClient.extractData(result, "保存任务节点失败");
    }
    
    /**
     * 更新任务节点
     * @param id 任务ID
     * @param formData 任务节点表单数据
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean updateJobNode(Long id, JobInfoForm formData) throws IOException {
        return httpClient.putForBoolean("/api/v1/jobInfos/" + id, formData);
    }
    
    /**
     * 获取任务表单数据（用于保存时获取现有数据）
     * @param id 任务ID
     * @return 任务表单数据
     * @throws IOException 网络异常
     */
    public JobInfoForm getFormData(Long id) throws IOException {
        return getJobNodeFormData(id);
    }
    
    /**
     * 获取任务节点表单数据
     * @param id 任务ID
     * @return 任务表单数据
     * @throws IOException 网络异常
     */
    public JobInfoForm getJobNodeFormData(Long id) throws IOException {
        String path = "/api/v1/jobInfos/" + id + "/form";
        Result<JobInfoForm> result = httpClient.get(path, JobInfoForm.class);
        return httpClient.extractData(result, "获取任务节点表单数据失败");
    }
    
    /**
     * 更新节点运行状态
     * @param jobId 任务ID
     * @param triggerStatus 运行状态：0=失败, 1=成功, 2=运行中
     * @throws IOException 网络异常
     */
    public void updateNodeStatus(Long jobId, Integer triggerStatus) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("jobId", String.valueOf(jobId));
        queryParams.put("triggerStatus", String.valueOf(triggerStatus));
        
        Result<Boolean> result = httpClient.post("/api/v1/jobInfos/updateNodeStatus", 
            new HashMap<>(), Boolean.class);
        if (!Result.isSuccess(result)) {
            throw new IOException("API 返回错误: " + result.getMsg());
        }
    }
    
    /**
     * 批量更新节点运行状态
     * @param statusMap 节点状态映射 Map<jobId, triggerStatus>
     * @throws IOException 网络异常
     */
    public void batchUpdateNodeStatus(java.util.Map<Long, Integer> statusMap) throws IOException {
        if (statusMap == null || statusMap.isEmpty()) {
            return;
        }
        
        Result<Integer> result = httpClient.post("/api/v1/jobInfos/batchUpdateNodeStatus", 
            statusMap, Integer.class);
        if (!Result.isSuccess(result)) {
            throw new IOException("API 返回错误: " + result.getMsg());
        }
    }
    
    /**
     * 获取下一次运行时间
     * @param scheduleType 调度类型
     * @param scheduleConf 调度配置
     * @return 下一次运行时间列表（最多5个）
     * @throws IOException 网络异常
     */
    public List<String> getNextTriggerTime(String scheduleType, String scheduleConf) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("scheduleType", scheduleType);
        queryParams.put("scheduleConf", scheduleConf);
        
        TypeToken<List<String>> typeToken = new TypeToken<List<String>>(){};
        Result<List<String>> result = httpClient.get("/api/v1/jobInfos/nextTriggerTime", typeToken, queryParams);
        return httpClient.extractData(result, "获取下一次运行时间失败");
    }
    
    /**
     * 暂停/启用任务
     * @param jobId 任务ID
     * @param pauseStatus 是否暂停：0=启用, 1=禁用
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean pauseJob(Long jobId, Integer pauseStatus) throws IOException {
        if (jobId == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (pauseStatus == null || (pauseStatus != 0 && pauseStatus != 1)) {
            throw new IllegalArgumentException("pause_status 参数必须为 0（启用）或 1（禁用）");
        }
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("pauseStatus", String.valueOf(pauseStatus));
        String path = "/api/v1/jobInfos/pauseJob/" + jobId;
        Result<Void> result = httpClient.get(path, Void.class, queryParams);
        if (!Result.isSuccess(result)) {
            throw new IOException("API 返回错误: " + result.getMsg());
        }
        return true;
    }
    
    /**
     * 保存GLUE源代码
     * @param formData GLUE表单数据
     * @return 是否保存成功
     * @throws IOException 网络异常
     */
    public boolean saveGlueSource(JobGlueForm formData) throws IOException {
        Result<Void> result = httpClient.post("/api/v1/jobInfos/saveGlueSource", formData, Void.class);
        if (!Result.isSuccess(result)) {
            throw new IOException("API 返回错误: " + result.getMsg());
        }
        return true;
    }
    
    /**
     * 获取GLUE历史记录列表
     * @param id 任务ID
     * @return GLUE历史记录列表
     * @throws IOException 网络异常
     */
    public List<JobLogglue> getGlueList(Long id) throws IOException {
        return getGlueList(id, null);
    }
    
    /**
     * 根据任务ID和GLUE类型获取历史记录列表
     * @param id 任务ID
     * @param glueType GLUE类型（可选，如果为空则返回所有类型）
     * @return GLUE历史记录列表
     * @throws IOException 网络异常
     */
    public List<JobLogglue> getGlueList(Long id, String glueType) throws IOException {
        String path;
        if (glueType != null && !glueType.trim().isEmpty()) {
            path = "/api/v1/jobInfos/getGlueList/" + id + "/" + 
                   URLEncoder.encode(glueType, StandardCharsets.UTF_8);
        } else {
            path = "/api/v1/jobInfos/getGlueList/" + id;
        }
        
        TypeToken<List<JobLogglue>> typeToken = new TypeToken<List<JobLogglue>>(){};
        Result<List<JobLogglue>> result = httpClient.get(path, typeToken);
        return httpClient.extractData(result, "获取GLUE历史记录列表失败");
    }
    
    /**
     * 保存连线
     * @param formData 连线表单数据
     * @return 保存后的连线实体
     * @throws IOException 网络异常
     */
    public JobEdge saveJobEdge(JobEdgeForm formData) throws IOException {
        Result<JobEdge> result = httpClient.post("/api/v1/jobInfos/saveJobEdge", formData, JobEdge.class);
        return httpClient.extractData(result, "保存连线失败");
    }
}

