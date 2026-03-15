package com.cc.job.gui.manager;

import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.service.JobNodeTemplateApiService;
import com.cc.job.gui.util.ApiUtil;
import com.cc.job.xo.model.entity.JobNodeTemplate;
import com.cc.job.xo.model.form.JobInfoForm;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

/**
 * 节点模板管理器 - 负责节点模板的创建、保存、加载等操作
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class NodeTemplateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeTemplateManager.class);
    
    private final Consumer<String> loggerCallback;
    private final JobInfoService jobInfoService;
    private final JobNodeTemplateApiService templateApiService;
    
    public NodeTemplateManager(Consumer<String> loggerCallback) {
        this.loggerCallback = loggerCallback;
        this.jobInfoService = new JobInfoService();
        this.templateApiService = new JobNodeTemplateApiService();
    }
    
    /**
     * 从节点创建模板对象（不落库）
     *
     * @param node 源节点
     * @param templateName 模板名称
     * @param templateCategory 模板分类
     * @param description 模板描述
     * @param isPublic 是否公开：0-私有，1-公开
     * @return 创建的模板对象
     */
    public JobNodeTemplate createTemplateFromNode(ProcessNode node, String templateName,
                                                  String templateCategory, String description, Integer isPublic) {
        if (node == null || node.getJobId() == null) {
            log("⚠ 节点无效，无法创建模板");
            return null;
        }
        
        try {
            JobInfoForm formData = jobInfoService.getJobNodeFormData(node.getJobId());
            if (formData == null) {
                log("✗ 获取节点配置失败");
                return null;
            }
            
            JobNodeTemplate template = new JobNodeTemplate();
            template.setTemplateName(templateName);
            template.setTemplateType(node.getType());
            template.setTemplateCategory(templateCategory);
            template.setDescription(description);
            template.setIsPublic(isPublic != null ? isPublic : 0);
            
            template.setTemplateConfig(ApiUtil.getInstance().getGson().toJson(formData));
            
            log("✓ 模板已创建: " + templateName);
            return template;
            
        } catch (Exception e) {
            log("✗ 创建模板失败: " + e.getMessage());
            logger.error("创建节点模板失败", e);
            return null;
        }
    }
    
    /**
     * 从模板创建节点
     *
     * @param template 模板
     * @param x 节点X坐标
     * @param y 节点Y坐标
     * @return 创建的节点配置
     */
    public JobInfoForm createNodeFromTemplate(JobNodeTemplate template, double x, double y) {
        if (template == null || template.getTemplateConfig() == null) {
            log("⚠ 模板无效");
            return null;
        }
        
        try {
            Type type = new TypeToken<JobInfoForm>(){}.getType();
            JobInfoForm formData = ApiUtil.getInstance().getGson().fromJson(template.getTemplateConfig(), type);
            
            // 设置节点位置
            formData.setNodePositionX(x);
            formData.setNodePositionY(y);
            
            // 清除ID，使其成为新节点
            formData.setId(null);
            
            log("✓ 从模板创建节点: " + template.getTemplateName());
            return formData;
            
        } catch (Exception e) {
            log("✗ 从模板创建节点失败: " + e.getMessage());
            logger.error("从模板创建节点失败", e);
            return null;
        }
    }
    
    /**
     * 获取模板列表（按分类，从 API 获取）
     *
     * @param category 分类（null表示所有分类）
     * @return 模板列表
     */
    public List<JobNodeTemplate> getTemplates(String category) {
        try {
            return templateApiService.list(category);
        } catch (IOException e) {
            log("✗ 获取模板列表失败: " + e.getMessage());
            logger.error("获取模板列表失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取所有模板分类（从当前模板列表中提取）
     */
    public List<String> getTemplateCategories() {
        Set<String> categories = new HashSet<>();
        for (JobNodeTemplate template : getTemplates(null)) {
            if (template.getTemplateCategory() != null && !template.getTemplateCategory().trim().isEmpty()) {
                categories.add(template.getTemplateCategory());
            }
        }
        return new ArrayList<>(categories);
    }
    
    /**
     * 保存模板到后端
     *
     * @return 是否成功，成功时 template.getId() 会被设置
     */
    public boolean saveTemplate(JobNodeTemplate template) {
        if (template == null) {
            return false;
        }
        try {
            Long id = templateApiService.save(template);
            if (id != null) {
                log("✓ 模板已保存: " + template.getTemplateName());
                return true;
            }
        } catch (IOException e) {
            log("✗ 保存模板失败: " + e.getMessage());
            logger.error("保存模板失败", e);
        }
        return false;
    }
    
    /**
     * 删除模板
     */
    public boolean deleteTemplate(Long templateId) {
        if (templateId == null) {
            return false;
        }
        try {
            boolean ok = templateApiService.delete(templateId);
            if (ok) {
                log("✓ 模板已删除");
                return true;
            }
        } catch (IOException e) {
            log("✗ 删除模板失败: " + e.getMessage());
            logger.error("删除模板失败", e);
        }
        return false;
    }
    
    private void log(String message) {
        if (loggerCallback != null) {
            loggerCallback.accept(message);
        }
        logger.info(message);
    }
}
