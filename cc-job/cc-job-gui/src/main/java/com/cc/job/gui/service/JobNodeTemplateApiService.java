package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobNodeTemplate;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 节点模板 API 服务 - 封装节点模板后端接口调用
 */
public class JobNodeTemplateApiService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(JobNodeTemplateApiService.class);

    public JobNodeTemplateApiService() {
        super();
    }

    /**
     * 获取模板列表（按分类，当前用户可见：自己创建的 + 公开的）
     *
     * @param category 分类（null 表示全部）
     * @return 模板列表
     */
    public List<JobNodeTemplate> list(String category) throws IOException {
        Map<String, String> params = null;
        if (category != null && !category.trim().isEmpty()) {
            params = Map.of("category", category.trim());
        }
        TypeToken<List<JobNodeTemplate>> typeToken = new TypeToken<List<JobNodeTemplate>>(){};
        Result<List<JobNodeTemplate>> result = params != null
            ? httpClient.get("/api/v1/jobNodeTemplates", typeToken, params)
            : httpClient.get("/api/v1/jobNodeTemplates", typeToken);
        List<JobNodeTemplate> data = httpClient.extractDataOrNull(result, "获取模板列表失败");
        return data != null ? data : Collections.emptyList();
    }

    /**
     * 新增模板
     *
     * @param template 模板
     * @return 新模板 ID，失败返回 null
     */
    public Long save(JobNodeTemplate template) throws IOException {
        Result<Long> result = httpClient.post("/api/v1/jobNodeTemplates", template, Long.class);
        Long id = httpClient.extractDataOrNull(result, "保存模板失败");
        if (id != null && template != null) {
            template.setId(id);
        }
        return id;
    }

    /**
     * 更新模板
     */
    public boolean update(Long id, JobNodeTemplate template) throws IOException {
        if (id == null) return false;
        template.setId(id);
        return httpClient.putForBoolean("/api/v1/jobNodeTemplates/" + id, template);
    }

    /**
     * 删除模板
     */
    public boolean delete(Long id) throws IOException {
        return httpClient.deleteForBoolean("/api/v1/jobNodeTemplates/" + id);
    }

    /**
     * 根据 ID 获取模板
     */
    public JobNodeTemplate getById(Long id) throws IOException {
        Result<JobNodeTemplate> result = httpClient.get("/api/v1/jobNodeTemplates/" + id, JobNodeTemplate.class);
        return httpClient.extractDataOrNull(result, "获取模板失败");
    }
}
