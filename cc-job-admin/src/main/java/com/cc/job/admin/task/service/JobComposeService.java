package com.cc.job.admin.task.service;

import com.cc.job.xo.model.form.JobInfoForm;

public interface JobComposeService {

    boolean saveJobCompose(JobInfoForm formData);

    boolean updateJobCompose(Long id, JobInfoForm formData);
}
