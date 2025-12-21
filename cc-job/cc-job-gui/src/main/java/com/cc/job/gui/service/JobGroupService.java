package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobGroupForm;
import com.cc.job.xo.model.query.JobGroupQuery;
import com.cc.job.xo.model.vo.JobGroupVO;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * JobGroup服务类
 */
public class JobGroupService extends BaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobGroupService.class);
    
    /**
     * 获取所有JobGroup列表
     * @return JobGroup列表
     * @throws IOException 网络异常
     */
    public List<JobGroup> getAllJobGroupList() throws IOException {
        TypeToken<List<JobGroup>> typeToken = new TypeToken<List<JobGroup>>(){};
        Result<List<JobGroup>> result = httpClient.get("/api/v1/jobGroups/getAllJobGroupList", typeToken);
        return httpClient.extractData(result, "获取JobGroup列表失败");
    }
    
    /**
     * 分页查询执行器列表
     * @param query 查询参数
     * @return 分页结果
     * @throws IOException 网络异常
     */
    public PageResult<JobGroupVO> getJobGroupPage(JobGroupQuery query) throws IOException {
        Map<String, Function<JobGroupQuery, Object>> extractors = new HashMap<>();
        extractors.put("pageNum", JobGroupQuery::getPageNum);
        extractors.put("pageSize", JobGroupQuery::getPageSize);
        extractors.put("appName", JobGroupQuery::getAppName);
        extractors.put("title", JobGroupQuery::getTitle);
        
        Map<String, String> queryParams = HttpClientUtil.buildQueryParams(query, extractors);
        return httpClient.getPage("/api/v1/jobGroups/page", JobGroupVO.class, queryParams);
    }
    
    /**
     * 查看执行器注册节点地址列表
     * @param id 执行器ID
     * @return 地址列表
     * @throws IOException 网络异常
     */
    public List<String> findAddressList(Long id) throws IOException {
        TypeToken<List<String>> typeToken = new TypeToken<List<String>>(){};
        Result<List<String>> result = httpClient.get("/api/v1/jobGroups/findAddressList/" + id, typeToken);
        return httpClient.extractData(result, "获取执行器地址列表失败");
    }
    
    /**
     * 新增执行器
     * @param form 表单数据
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean saveJobGroup(JobGroupForm form) throws IOException {
        return httpClient.postForBoolean("/api/v1/jobGroups", form);
    }
    
    /**
     * 更新执行器
     * @param id 执行器ID
     * @param form 表单数据
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean updateJobGroup(Long id, JobGroupForm form) throws IOException {
        return httpClient.putForBoolean("/api/v1/jobGroups/" + id, form);
    }
    
    /**
     * 删除执行器
     * @param ids 执行器ID，多个以逗号分隔
     * @return 是否成功
     * @throws IOException 网络异常
     */
    public boolean deleteJobGroups(String ids) throws IOException {
        return httpClient.deleteForBoolean("/api/v1/jobGroups/" + ids);
    }
    
    /**
     * 获取执行器表单数据
     * @param id 执行器ID
     * @return 表单数据
     * @throws IOException 网络异常
     */
    public JobGroupForm getJobGroupForm(Long id) throws IOException {
        Result<JobGroupForm> result = httpClient.get("/api/v1/jobGroups/" + id + "/form", JobGroupForm.class);
        return httpClient.extractData(result, "获取执行器表单数据失败");
    }
}

