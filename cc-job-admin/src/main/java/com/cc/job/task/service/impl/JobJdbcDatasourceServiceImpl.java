package com.cc.job.task.service.impl;



import cn.hutool.core.bean.BeanUtil;
import com.cc.job.task.mapper.JobJdbcDatasourceMapper;
import com.cc.job.task.model.entity.JobJdbcDatasource;
import com.cc.job.task.model.form.JobJdbcDatasourceForm;
import com.cc.job.task.model.query.JobJdbcDatasourceQuery;
import com.cc.job.task.model.vo.JobJdbcDatasourceVO;
import com.cc.job.task.service.JobJdbcDatasourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * jdbc数据源配置服务实现类
 *
 * @author ccjob
 * @since 2024-12-14 17:12
 */
@Service
@RequiredArgsConstructor
public class JobJdbcDatasourceServiceImpl extends ServiceImpl<JobJdbcDatasourceMapper, JobJdbcDatasource> implements JobJdbcDatasourceService {

    /**
     * 获取jdbc数据源配置分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<JobJdbcDatasourceVO>} jdbc数据源配置分页列表
     */
    @Override
    public IPage<JobJdbcDatasourceVO> getJdbcDatasourcePage(JobJdbcDatasourceQuery queryParams) {
        Page<JobJdbcDatasourceVO> pageVO = new Page<>();
        return pageVO;
    }

    /**
     * 获取jdbc数据源配置表单数据
     *
     * @param id jdbc数据源配置ID
     * @return
     */
    @Override
    public JobJdbcDatasourceForm getJdbcDatasourceFormData(Long id) {
        JobJdbcDatasource entity = this.getById(id);
        return BeanUtil.copyProperties(entity, JobJdbcDatasourceForm.class);
    }

    /**
     * 新增jdbc数据源配置
     *
     * @param formData jdbc数据源配置表单对象
     * @return
     */
    @Override
    public boolean saveJdbcDatasource(JobJdbcDatasourceForm formData) {
        JobJdbcDatasource entity = BeanUtil.copyProperties(formData, JobJdbcDatasource.class);
        return this.save(entity);
    }

    /**
     * 更新jdbc数据源配置
     *
     * @param id   jdbc数据源配置ID
     * @param formData jdbc数据源配置表单对象
     * @return
     */
    @Override
    public boolean updateJdbcDatasource(Long id,JobJdbcDatasourceForm formData) {
        JobJdbcDatasource entity = BeanUtil.copyProperties(formData, JobJdbcDatasource.class);
        return this.updateById(entity);
    }

    /**
     * 删除jdbc数据源配置
     *
     * @param ids jdbc数据源配置ID，多个以英文逗号(,)分割
     * @return
     */
    @Override
    public boolean deleteJdbcDatasources(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的jdbc数据源配置数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}

