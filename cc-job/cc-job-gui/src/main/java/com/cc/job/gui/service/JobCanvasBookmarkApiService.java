package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobCanvasBookmark;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 画布书签 API 服务 - 封装画布书签后端接口调用
 */
public class JobCanvasBookmarkApiService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(JobCanvasBookmarkApiService.class);

    public JobCanvasBookmarkApiService() {
        super();
    }

    /**
     * 按任务组获取书签列表
     */
    public List<JobCanvasBookmark> listByTaskGroupId(Long taskGroupId) throws IOException {
        if (taskGroupId == null) return Collections.emptyList();
        Map<String, String> params = Map.of("taskGroupId", String.valueOf(taskGroupId));
        TypeToken<List<JobCanvasBookmark>> typeToken = new TypeToken<List<JobCanvasBookmark>>(){};
        Result<List<JobCanvasBookmark>> result = httpClient.get("/api/v1/jobCanvasBookmarks", typeToken, params);
        List<JobCanvasBookmark> data = httpClient.extractDataOrNull(result, "获取书签列表失败");
        return data != null ? data : Collections.emptyList();
    }

    /**
     * 新增书签
     */
    public Long save(JobCanvasBookmark bookmark) throws IOException {
        Result<Long> result = httpClient.post("/api/v1/jobCanvasBookmarks", bookmark, Long.class);
        Long id = httpClient.extractDataOrNull(result, "保存书签失败");
        if (id != null && bookmark != null) {
            bookmark.setId(id);
        }
        return id;
    }

    /**
     * 删除书签
     */
    public boolean delete(Long id) throws IOException {
        return httpClient.deleteForBoolean("/api/v1/jobCanvasBookmarks/" + id);
    }

    /**
     * 根据 ID 获取书签
     */
    public JobCanvasBookmark getById(Long id) throws IOException {
        Result<JobCanvasBookmark> result = httpClient.get("/api/v1/jobCanvasBookmarks/" + id, JobCanvasBookmark.class);
        return httpClient.extractDataOrNull(result, "获取书签失败");
    }
}
