package com.cc.job.gui.manager;

import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.xo.model.entity.JobNodeTemplate;
import com.cc.job.xo.model.form.JobInfoForm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    // 内存中的模板缓存
    private Map<Long, JobNodeTemplate> templateCache = new HashMap<>();
    
    public NodeTemplateManager(Consumer<String> loggerCallback) {
        this.loggerCallback = loggerCallback;
        this.jobInfoService = new JobInfoService();
    }
    
    /**
     * 从节点创建模板
     *
     * @param node 源节点
     * @param templateName 模板名称
     * @param templateCategory 模板分类
     * @param description 模板描述
     * @return 创建的模板
     */
    public JobNodeTemplate createTemplateFromNode(ProcessNode node, String templateName,
                                                  String templateCategory, String description) {
        if (node == null || node.getJobId() == null) {
            log("⚠ 节点无效，无法创建模板");
            return null;
        }
        
        try {
            // 获取节点的完整配置
            JobInfoForm formData = jobInfoService.getJobNodeFormData(node.getJobId());
            if (formData == null) {
                log("✗ 获取节点配置失败");
                return null;
            }
            
            // 创建模板对象
            JobNodeTemplate template = new JobNodeTemplate();
            template.setTemplateName(templateName);
            template.setTemplateType(node.getType());
            template.setTemplateCategory(templateCategory);
            template.setDescription(description);
            template.setIsPublic(0); // 默认私有
            
            // 将节点配置序列化为JSON
            Gson gson = new Gson();
            String configJson = gson.toJson(formData);
            template.setTemplateConfig(configJson);
            
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
            Gson gson = new Gson();
            Type type = new TypeToken<JobInfoForm>(){}.getType();
            JobInfoForm formData = gson.fromJson(template.getTemplateConfig(), type);
            
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
     * 获取模板列表（按分类）
     *
     * @param category 分类（null表示所有分类）
     * @return 模板列表
     */
    public List<JobNodeTemplate> getTemplates(String category) {
        // TODO: 从数据库或API获取模板列表
        // 这里返回内存中的模板
        List<JobNodeTemplate> templates = new ArrayList<>(templateCache.values());
        
        if (category != null && !category.trim().isEmpty()) {
            templates.removeIf(t -> !category.equals(t.getTemplateCategory()));
        }
        
        return templates;
    }
    
    /**
     * 获取所有模板分类
     *
     * @return 分类列表
     */
    public List<String> getTemplateCategories() {
        Set<String> categories = new HashSet<>();
        for (JobNodeTemplate template : templateCache.values()) {
            if (template.getTemplateCategory() != null) {
                categories.add(template.getTemplateCategory());
            }
        }
        return new ArrayList<>(categories);
    }
    
    /**
     * 保存模板到内存缓存（实际应该保存到数据库）
     */
    public void saveTemplate(JobNodeTemplate template) {
        if (template == null) {
            return;
        }
        
        // TODO: 保存到数据库
        // 这里先保存到内存缓存
        if (template.getId() == null) {
            template.setId(System.currentTimeMillis()); // 临时ID
        }
        templateCache.put(template.getId(), template);
        log("✓ 模板已保存: " + template.getTemplateName());
    }
    
    /**
     * 删除模板
     */
    public boolean deleteTemplate(Long templateId) {
        if (templateId == null) {
            return false;
        }
        
        // TODO: 从数据库删除
        // 这里先从内存缓存删除
        JobNodeTemplate removed = templateCache.remove(templateId);
        if (removed != null) {
            log("✓ 模板已删除: " + removed.getTemplateName());
            return true;
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
