package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.mapper.JobValidationMapper;
import com.cc.job.xo.model.entity.JobValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 任务同步后数据质量校验配置
 */
@Tag(name = "数据质量校验配置")
@RestController
@RequestMapping("/api/v1/jobValidations")
public class JobValidationController {

    private final JobValidationMapper jobValidationMapper;

    public JobValidationController(JobValidationMapper jobValidationMapper) {
        this.jobValidationMapper = jobValidationMapper;
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "按任务ID获取校验配置")
    @GetMapping("/byJob/{jobId}")
    public Result<JobValidation> getByJobId(@Parameter(description = "任务ID") @PathVariable Long jobId) {
        LambdaQueryWrapper<JobValidation> q = new LambdaQueryWrapper<>();
        q.eq(JobValidation::getJobId, jobId).eq(JobValidation::getIsDeleted, 0).last("LIMIT 1");
        JobValidation one = jobValidationMapper.selectOne(q);
        return Result.success(one);
    }

    @RequirePermission(PermissionConstants.JOB_INFO_EDIT)
    @Operation(summary = "保存或更新校验配置")
    @PostMapping
    public Result<Long> save(@RequestBody JobValidation validation) {
        if (validation.getJobId() == null || validation.getValidationSql() == null || validation.getValidationSql().isBlank()
                || validation.getJdbcDatasourceId() == null) {
            return Result.failed("jobId、validationSql、jdbcDatasourceId 不能为空");
        }
        if (validation.getExpectedMinRows() == null) {
            validation.setExpectedMinRows(0);
        }
        LambdaQueryWrapper<JobValidation> q = new LambdaQueryWrapper<>();
        q.eq(JobValidation::getJobId, validation.getJobId()).eq(JobValidation::getIsDeleted, 0).last("LIMIT 1");
        JobValidation existing = jobValidationMapper.selectOne(q);
        if (existing != null) {
            existing.setValidationSql(validation.getValidationSql());
            existing.setExpectedMinRows(validation.getExpectedMinRows());
            existing.setJdbcDatasourceId(validation.getJdbcDatasourceId());
            jobValidationMapper.updateById(existing);
            return Result.success(existing.getId());
        }
        validation.setIsDeleted(0);
        jobValidationMapper.insert(validation);
        return Result.success(validation.getId());
    }

    @RequirePermission(PermissionConstants.JOB_INFO_EDIT)
    @Operation(summary = "删除校验配置")
    @DeleteMapping("/byJob/{jobId}")
    public Result<Void> deleteByJobId(@Parameter(description = "任务ID") @PathVariable Long jobId) {
        LambdaQueryWrapper<JobValidation> q = new LambdaQueryWrapper<>();
        q.eq(JobValidation::getJobId, jobId).eq(JobValidation::getIsDeleted, 0);
        JobValidation one = jobValidationMapper.selectOne(q);
        if (one != null) {
            one.setIsDeleted(1);
            jobValidationMapper.updateById(one);
        }
        return Result.success();
    }
}
