package com.cc.job.task.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.task.model.entity.JobJdbcDatasource;
import com.cc.job.task.model.form.JobJdbcDatasourceForm;
import com.cc.job.task.model.query.JobJdbcDatasourceQuery;
import com.cc.job.task.model.vo.JobJdbcDatasourceVO;
import jakarta.validation.Valid;

public interface JobJdbcDatasourceService extends IService<JobJdbcDatasource>  {

    IPage<JobJdbcDatasourceVO> getJdbcDatasourcePage(JobJdbcDatasourceQuery queryParams);

    boolean saveJdbcDatasource(@Valid JobJdbcDatasourceForm formData);

    JobJdbcDatasourceForm getJdbcDatasourceFormData(Long id);

    boolean updateJdbcDatasource(Long id, JobJdbcDatasourceForm formData);

    boolean deleteJdbcDatasources(String ids);
}
