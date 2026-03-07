package com.cc.job.admin.task.controller;

import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.admin.task.service.JobNodeTemplateService;
import com.cc.job.admin.task.utils.JwtUtil;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.model.entity.JobNodeTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Tag(name = "节点模板接口")
@RestController
@RequestMapping("/api/v1/jobNodeTemplates")
public class JobNodeTemplateController {

    private final JobNodeTemplateService jobNodeTemplateService;

    public JobNodeTemplateController(JobNodeTemplateService jobNodeTemplateService) {
        this.jobNodeTemplateService = jobNodeTemplateService;
    }

    @RequirePermission(PermissionConstants.JOB_NODE_VIEW)
    @Operation(summary = "获取模板列表")
    @GetMapping
    public Result<List<JobNodeTemplate>> list(
            @Parameter(description = "模板分类") @RequestParam(required = false) String category,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<JobNodeTemplate> list = jobNodeTemplateService.listByCategoryAndUser(category, userId);
        return Result.success(list);
    }

    @RequirePermission(PermissionConstants.JOB_NODE_EDIT)
    @Operation(summary = "新增模板")
    @PostMapping
    public Result<Long> save(@RequestBody JobNodeTemplate template, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId != null && template.getCreateUserId() == null) {
            template.setCreateUserId(userId);
        }
        boolean ok = jobNodeTemplateService.save(template);
        return ok ? Result.success(template.getId()) : Result.failed("保存失败");
    }

    @RequirePermission(PermissionConstants.JOB_NODE_EDIT)
    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @RequestBody JobNodeTemplate template,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId != null && template.getCreateUserId() == null) {
            template.setCreateUserId(userId);
        }
        template.setId(id);
        boolean ok = jobNodeTemplateService.updateById(template);
        return Result.judge(ok);
    }

    @RequirePermission(PermissionConstants.JOB_NODE_EDIT)
    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "模板ID") @PathVariable Long id) {
        boolean ok = jobNodeTemplateService.removeById(id);
        return Result.judge(ok);
    }

    @RequirePermission(PermissionConstants.JOB_NODE_VIEW)
    @Operation(summary = "根据ID获取模板")
    @GetMapping("/{id}")
    public Result<JobNodeTemplate> getById(@Parameter(description = "模板ID") @PathVariable Long id) {
        JobNodeTemplate template = jobNodeTemplateService.getById(id);
        return Result.success(template);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        if (request == null) return null;
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return JwtUtil.getUserIdFromToken(token);
        }
        return null;
    }
}
