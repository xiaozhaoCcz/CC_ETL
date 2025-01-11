package com.cc.job.admin.task.service;

import com.cc.job.xo.model.form.JobInfoForm;

import java.util.Map;

public interface JobComposeService {

    boolean saveJobCompose(JobInfoForm formData);

    boolean updateJobCompose(Long id, JobInfoForm formData);

    Map<String,Object> getJobCompose(Long id,Integer type);

    boolean validateJobComposeEdge(String nodes,String edges);
}
