package com.cc.job.task.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.form.JobJdbcDatasourceForm;
import com.cc.job.xo.model.query.JobJdbcDatasourceQuery;
import com.cc.job.xo.model.vo.JobJdbcDatasourceVO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public interface JobJdbcDatasourceService extends IService<JobJdbcDatasource>  {

    IPage<JobJdbcDatasourceVO> getJdbcDatasourcePage(JobJdbcDatasourceQuery queryParams);

    boolean saveJdbcDatasource(@Valid JobJdbcDatasourceForm formData);

    JobJdbcDatasourceForm getJdbcDatasourceFormData(Long id);

    boolean updateJdbcDatasource(Long id, JobJdbcDatasourceForm formData);

    boolean deleteJdbcDatasources(String ids);

    List<String> getColumns(Long id, Map<String, String> params);

    List<String> getTables(Long id);

    boolean isConnect(@Valid JobJdbcDatasourceForm formData);
}
