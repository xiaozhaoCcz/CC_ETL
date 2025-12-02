package com.cc.job.admin.task.service;

import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobEdgeForm;
import com.cc.job.xo.model.form.JobInfoForm;

import java.util.List;
import java.util.Map;

public interface JobComposeService {

    boolean saveJobCompose(JobInfoForm formData);

    boolean updateJobCompose(Long id, JobInfoForm formData);

    Map<String,Object> getJobCompose(Map<String,Object> formMap);

    boolean validateJobComposeEdge(String nodes,String edges);

    JobNode saveJobNode(JobInfoForm formData);

    Long updateJobNode(Long jobId, Long nodeId);

    List<Long> pauseJobs(Long[] jobIds);

    void deleteJobNode(Long nodeId);

    Map<String, Object> saveJobNodeAndJobEdges(Map<String, Object> formMap);

    /**
     * 保存连线
     * @param formData 连线表单数据
     * @return 保存后的连线实体
     */
    JobEdge saveJobEdge(JobEdgeForm formData);
}
