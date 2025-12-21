package com.cc.job.admin.task.service.impl;



import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.enums.DatasourceEnum;
import com.cc.job.xo.common.exception.BusinessException;
import com.cc.job.admin.task.command.JdbcCommand;
import com.cc.job.xo.mapper.JobJdbcDatasourceMapper;
import com.cc.job.xo.model.entity.JobJdbcDatasource;
import com.cc.job.xo.model.form.JobJdbcDatasourceForm;
import com.cc.job.xo.model.query.JobJdbcDatasourceQuery;
import com.cc.job.xo.model.vo.JobJdbcDatasourceVO;
import com.cc.job.admin.task.service.JobJdbcDatasourceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * jdbc数据源配置服务实现类
 *
 * @author ccjob
 * @since 2024-12-14 17:12
 */
@Service
public class JobJdbcDatasourceServiceImpl extends ServiceImpl<JobJdbcDatasourceMapper, JobJdbcDatasource> implements JobJdbcDatasourceService {

    public JobJdbcDatasourceServiceImpl() {
    }

    /**
     * 获取jdbc数据源配置分页列表
     *
     * @param queryParams 查询参数
     * @return {@link IPage<JobJdbcDatasourceVO>} jdbc数据源配置分页列表
     */
    @Override
    public IPage<JobJdbcDatasourceVO> getJdbcDatasourcePage(JobJdbcDatasourceQuery queryParams) {
        Page<JobJdbcDatasourceVO> pageVO = new Page<>();
        LambdaQueryWrapper<JobJdbcDatasource> wrapper = new LambdaQueryWrapper<>();

        if(StringUtils.isNotBlank(queryParams.getDatasource())){
            wrapper.eq(JobJdbcDatasource::getDatasource,queryParams.getDatasource());
        }

        if (StringUtils.isNotBlank(queryParams.getDatasourceName())){
            wrapper.like(JobJdbcDatasource::getDatasourceName,queryParams.getDatasourceName());
        }

        if(StringUtils.isNotBlank(queryParams.getDatabaseName())){
            wrapper.like(JobJdbcDatasource::getDatabaseName,queryParams.getDatabaseName());
        }

        wrapper.orderByDesc(JobJdbcDatasource::getUpdateTime);

        Page<JobJdbcDatasource> page = this.page(new Page<>(queryParams.getPageNum(), queryParams.getPageSize()), wrapper);
        List<JobJdbcDatasource> list = page.getRecords();

        List<JobJdbcDatasourceVO> voList = list.stream()
                .map(entity -> BeanUtil.copyProperties(entity, JobJdbcDatasourceVO.class))
                .toList();
        pageVO.setRecords(voList);
        pageVO.setTotal(page.getTotal());
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

    @Override
    public List<String> getColumns(Long id, Map<String, String> params) {
        List<String> columns = new ArrayList<>();
        JobJdbcDatasource jobJdbcDatasource = this.getById(id);
        JdbcCommand jdbcCommand = new JdbcCommand(jobJdbcDatasource.getJdbcDriverClass(), jobJdbcDatasource.getJdbcUrl(), jobJdbcDatasource.getJdbcUsername(), jobJdbcDatasource.getJdbcPassword());
        Connection con = jdbcCommand.getConnection();
        String tableName = params.get("tableName");
        String sql = params.get("querySql");
        if (StringUtils.isBlank(sql)) {
            String schemaName = StringUtils.isBlank(jobJdbcDatasource.getSchemaName()) ? "" : jobJdbcDatasource.getSchemaName() + ".";
            sql = "select * from " + schemaName + tableName + " t";
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                columns.add(columnName);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            JdbcCommand.close(rs);
            JdbcCommand.close(ps);
        }
        return columns;
    }

    @Override
    public List<String> getTables(Long id) {
        List<String> tables = new ArrayList<>();
        JobJdbcDatasource jobJdbcDatasource = this.getById(id);
        JdbcCommand jdbcCommand = null;
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            if(DatasourceEnum.ORACLE.getTitle().equalsIgnoreCase(jobJdbcDatasource.getDatasource())){
                jdbcCommand = new JdbcCommand(jobJdbcDatasource.getJdbcDriverClass(), jobJdbcDatasource.getJdbcUrl(), jobJdbcDatasource.getJdbcUsername(), jobJdbcDatasource.getJdbcPassword(),jobJdbcDatasource.getSchemaName());
                 con = jdbcCommand.getConnection();
                 stmt = con.createStatement();
                 rs = stmt.executeQuery("SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = '"+jobJdbcDatasource.getSchemaName()+"'");
                 while (rs.next()) {
                     String tableName = rs.getString("TABLE_NAME");
                     tables.add(tableName);
                 }
                 return tables;
            }
            jdbcCommand = new JdbcCommand(jobJdbcDatasource.getJdbcDriverClass(), jobJdbcDatasource.getJdbcUrl(), jobJdbcDatasource.getJdbcUsername(), jobJdbcDatasource.getJdbcPassword());
            con = jdbcCommand.getConnection();
            DatabaseMetaData metaData = con.getMetaData();
            rs = metaData.getTables(con.getCatalog(), null, null, new String[]{"TABLE"});
            // 遍历结果集并打印表名
            while (rs!=null&&rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.add(tableName);
            }
        } catch (SQLException e) {
             throw new BusinessException(e);
        }finally {
            JdbcCommand.close(rs);
            JdbcCommand.close(stmt);
            JdbcCommand.close(con);
        }
        return tables;
    }

    @Override
    public boolean isConnect(JobJdbcDatasourceForm formData) {
        JdbcCommand jdbcCommand = new JdbcCommand(formData.getJdbcDriverClass(), formData.getJdbcUrl(), formData.getJdbcUsername(), formData.getJdbcPassword(),formData.getSchemaName());
        Connection con = null;
        try {
            con = jdbcCommand.getConnection();
            return con!=null;
        }catch (Exception e){
            throw new BusinessException(e);
        }finally {
            JdbcCommand.close(con);
        }
    }

}

