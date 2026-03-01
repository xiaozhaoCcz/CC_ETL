package com.cc.job.gui.service;

import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobApprovalPending;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批待办 API
 */
public class JobApprovalService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(JobApprovalService.class);

    public List<JobApprovalPending> listPending(Long jobId) throws IOException {
        String path = "/api/v1/approvals/pending";
        if (jobId != null) {
            path += "?jobId=" + jobId;
        }
        Result<List<JobApprovalPending>> result = httpClient.get(path, new TypeToken<List<JobApprovalPending>>() {});
        return httpClient.extractData(result, "获取待办列表失败");
    }

    public void approve(Long id, String remark) throws IOException {
        Map<String, String> body = new HashMap<>();
        if (remark != null && !remark.isEmpty()) {
            body.put("remark", remark);
        }
        Result<Void> result = httpClient.post("/api/v1/approvals/" + id + "/approve", body, Void.class);
        httpClient.extractData(result, "审批通过失败");
    }

    public void reject(Long id, String remark) throws IOException {
        Map<String, String> body = new HashMap<>();
        if (remark != null && !remark.isEmpty()) {
            body.put("remark", remark);
        }
        Result<Void> result = httpClient.post("/api/v1/approvals/" + id + "/reject", body, Void.class);
        httpClient.extractData(result, "审批拒绝失败");
    }
}
