package com.cc.job.admin.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.job.admin.task.auth.PermissionConstants;
import com.cc.job.admin.task.auth.RequirePermission;
import com.cc.job.xo.common.result.Result;
import com.cc.job.xo.mapper.JobParamMapper;
import com.cc.job.xo.model.entity.JobParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数与密钥管理：统一存储，任务/DataX 中可用 ${key} 引用
 */
@Tag(name = "参数与密钥")
@RestController
@RequestMapping("/api/v1/jobParams")
public class JobParamController {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)\\}");

    private final JobParamMapper paramMapper;

    public JobParamController(JobParamMapper paramMapper) {
        this.paramMapper = paramMapper;
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "列表")
    @GetMapping
    public Result<List<JobParam>> list() {
        LambdaQueryWrapper<JobParam> q = new LambdaQueryWrapper<>();
        q.eq(JobParam::getIsDeleted, 0).orderByAsc(JobParam::getParamKey);
        return Result.success(paramMapper.selectList(q));
    }

    @RequirePermission(PermissionConstants.JOB_INFO_EDIT)
    @Operation(summary = "新增或更新（按 key）")
    @PostMapping
    public Result<Long> save(@RequestBody JobParam param) {
        if (param.getParamKey() == null || param.getParamKey().trim().isEmpty()) {
            return Result.failed("paramKey 不能为空");
        }
        String key = param.getParamKey().trim();
        LambdaQueryWrapper<JobParam> q = new LambdaQueryWrapper<>();
        q.eq(JobParam::getParamKey, key).eq(JobParam::getIsDeleted, 0);
        JobParam existing = paramMapper.selectOne(q);
        if (existing != null) {
            existing.setParamValue(param.getParamValue());
            existing.setComment(param.getComment());
            paramMapper.updateById(existing);
            return Result.success(existing.getId());
        }
        param.setParamKey(key);
        param.setIsDeleted(0);
        paramMapper.insert(param);
        return Result.success(param.getId());
    }

    @RequirePermission(PermissionConstants.JOB_INFO_EDIT)
    @Operation(summary = "删除（按 key）")
    @DeleteMapping("/key/{key}")
    public Result<Void> deleteByKey(@Parameter(description = "参数名") @PathVariable String key) {
        LambdaQueryWrapper<JobParam> q = new LambdaQueryWrapper<>();
        q.eq(JobParam::getParamKey, key).eq(JobParam::getIsDeleted, 0);
        JobParam one = paramMapper.selectOne(q);
        if (one != null) {
            one.setIsDeleted(1);
            paramMapper.updateById(one);
        }
        return Result.success();
    }

    @RequirePermission(PermissionConstants.JOB_INFO_VIEW)
    @Operation(summary = "解析字符串中的 ${key} 占位符（用于预览，不返回敏感值）")
    @PostMapping("/resolve")
    public Result<String> resolve(@RequestBody ResolveRequest request) {
        if (request.getText() == null) return Result.success("");
        LambdaQueryWrapper<JobParam> q = new LambdaQueryWrapper<>();
        q.eq(JobParam::getIsDeleted, 0);
        List<JobParam> all = paramMapper.selectList(q);
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (JobParam p : all) {
            if (p.getParamKey() != null) map.put(p.getParamKey(), p.getParamValue() != null ? p.getParamValue() : "");
        }
        String out = replacePlaceholders(request.getText(), map);
        return Result.success(out);
    }

    /** 将 text 中的 ${key} 替换为 map 中的值 */
    public static String replacePlaceholders(String text, java.util.Map<String, String> map) {
        if (text == null || map == null) return text;
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String k = m.group(1);
            String v = map.getOrDefault(k, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static class ResolveRequest {
        private String text;
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
