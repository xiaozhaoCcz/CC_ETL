package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobGroupSnapshot;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务组快照/版本 API 调用（列表版本、保存为版本、按 ID 获取快照）
 */
public class JobGroupSnapshotService extends BaseService {

    /**
     * 列出任务组的手动版本列表
     */
    public List<JobGroupSnapshot> listVersions(Long jobId, int limit) throws IOException {
        String path = "/api/v1/jobGroupSnapshots/versions?jobId=" + jobId + "&limit=" + limit;
        Result<List<JobGroupSnapshot>> result = httpClient.get(path, new TypeToken<List<JobGroupSnapshot>>(){});
        return httpClient.extractData(result, "获取版本列表失败");
    }

    /**
     * 保存当前画布为版本
     * @return 快照 ID
     */
    public Long saveAsVersion(Long jobId, String versionName, String nodesJson, String edgesJson) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("jobId", jobId);
        body.put("versionName", versionName != null ? versionName : "");
        body.put("nodesJson", nodesJson);
        body.put("edgesJson", edgesJson);
        Result<Long> result = httpClient.post("/api/v1/jobGroupSnapshots/saveVersion", body, new TypeToken<Long>(){});
        return httpClient.extractData(result, "保存版本失败");
    }

    /**
     * 根据快照 ID 获取快照内容（用于回滚）
     */
    public JobGroupSnapshot getSnapshot(Long snapshotId) throws IOException {
        String path = "/api/v1/jobGroupSnapshots/" + snapshotId;
        Result<JobGroupSnapshot> result = httpClient.get(path, new TypeToken<JobGroupSnapshot>(){});
        return httpClient.extractData(result, "获取快照失败");
    }
}
