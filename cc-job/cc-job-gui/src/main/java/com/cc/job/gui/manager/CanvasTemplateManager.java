package com.cc.job.gui.manager;

import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.view.NodeCanvas;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.Set;
import java.util.HashSet;

/**
 * 画布模板管理器 - 负责画布模板的创建、保存、加载等操作
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class CanvasTemplateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CanvasTemplateManager.class);
    
    private final Consumer<String> loggerCallback;
    private final Gson gson = new Gson();
    
    // 内存中的模板缓存
    private Map<String, CanvasTemplate> templateCache = new HashMap<>();
    
    public CanvasTemplateManager(Consumer<String> loggerCallback) {
        this.loggerCallback = loggerCallback;
    }
    
    /**
     * 画布模板数据结构
     */
    public static class CanvasTemplate {
        private String templateName;
        private String templateCategory;
        private String description;
        private List<TemplateNode> nodes;
        private List<TemplateConnection> connections;
        private Map<String, Object> metadata; // 元数据（如创建时间、作者等）
        
        public String getTemplateName() {
            return templateName;
        }
        
        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }
        
        public String getTemplateCategory() {
            return templateCategory;
        }
        
        public void setTemplateCategory(String templateCategory) {
            this.templateCategory = templateCategory;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public List<TemplateNode> getNodes() {
            return nodes;
        }
        
        public void setNodes(List<TemplateNode> nodes) {
            this.nodes = nodes;
        }
        
        public List<TemplateConnection> getConnections() {
            return connections;
        }
        
        public void setConnections(List<TemplateConnection> connections) {
            this.connections = connections;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
    
    /**
     * 模板节点数据
     */
    public static class TemplateNode {
        private String nodeId;
        private String nodeType;
        private String nodeName;
        private double x;
        private double y;
        private Map<String, Object> properties;
        
        public String getNodeId() {
            return nodeId;
        }
        
        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
        
        public String getNodeType() {
            return nodeType;
        }
        
        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }
        
        public String getNodeName() {
            return nodeName;
        }
        
        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }
        
        public double getX() {
            return x;
        }
        
        public void setX(double x) {
            this.x = x;
        }
        
        public double getY() {
            return y;
        }
        
        public void setY(double y) {
            this.y = y;
        }
        
        public Map<String, Object> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
    
    /**
     * 模板连接数据
     */
    public static class TemplateConnection {
        private String sourceNodeId;
        private String targetNodeId;
        private String startPoint;
        private String endPoint;
        
        public String getSourceNodeId() {
            return sourceNodeId;
        }
        
        public void setSourceNodeId(String sourceNodeId) {
            this.sourceNodeId = sourceNodeId;
        }
        
        public String getTargetNodeId() {
            return targetNodeId;
        }
        
        public void setTargetNodeId(String targetNodeId) {
            this.targetNodeId = targetNodeId;
        }
        
        public String getStartPoint() {
            return startPoint;
        }
        
        public void setStartPoint(String startPoint) {
            this.startPoint = startPoint;
        }
        
        public String getEndPoint() {
            return endPoint;
        }
        
        public void setEndPoint(String endPoint) {
            this.endPoint = endPoint;
        }
    }
    
    /**
     * 从当前画布创建模板
     *
     * @param canvas 画布
     * @param templateName 模板名称
     * @param templateCategory 模板分类
     * @param description 模板描述
     * @return 创建的模板
     */
    public CanvasTemplate createTemplateFromCanvas(NodeCanvas canvas, String templateName, 
                                                   String templateCategory, String description) {
        if (canvas == null) {
            log("⚠ 画布无效，无法创建模板");
            return null;
        }
        
        CanvasTemplate template = new CanvasTemplate();
        template.setTemplateName(templateName);
        template.setTemplateCategory(templateCategory);
        template.setDescription(description);
        
        // 提取节点数据
        List<TemplateNode> templateNodes = new ArrayList<>();
        for (ProcessNode node : canvas.getNodes()) {
            TemplateNode templateNode = new TemplateNode();
            templateNode.setNodeId(node.getNodeId());
            templateNode.setNodeType(node.getType());
            templateNode.setNodeName(node.getJobHandlerName());
            templateNode.setX(node.getLayoutX());
            templateNode.setY(node.getLayoutY());
            
            // 提取节点属性
            Map<String, Object> properties = new HashMap<>();
            properties.put("color", node.getCurrentColor());
            properties.put("tags", node.getTags());
            properties.put("remark", node.getRemark());
            templateNode.setProperties(properties);
            
            templateNodes.add(templateNode);
        }
        template.setNodes(templateNodes);
        
        // 提取连接数据
        List<TemplateConnection> templateConnections = new ArrayList<>();
        for (NodeConnection conn : canvas.getConnections()) {
            TemplateConnection templateConn = new TemplateConnection();
            // 获取源节点和目标节点的ID
            if (conn.getSourceNode() != null) {
                templateConn.setSourceNodeId(conn.getSourceNode().getNodeId());
            }
            if (conn.getTargetNode() != null) {
                templateConn.setTargetNodeId(conn.getTargetNode().getNodeId());
            }
            templateConn.setStartPoint("right");
            templateConn.setEndPoint("left");
            templateConnections.add(templateConn);
        }
        template.setConnections(templateConnections);
        
        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("createTime", System.currentTimeMillis());
        metadata.put("nodeCount", templateNodes.size());
        metadata.put("connectionCount", templateConnections.size());
        template.setMetadata(metadata);
        
        log("✓ 画布模板已创建: " + templateName);
        return template;
    }
    
    /**
     * 从画布当前选中的节点及它们之间的连线创建模板
     */
    public CanvasTemplate createTemplateFromSelection(NodeCanvas canvas, String templateName,
                                                      String templateCategory, String description) {
        if (canvas == null) {
            log("⚠ 画布无效");
            return null;
        }
        Set<ProcessNode> selected = canvas.getSelectedNodes();
        if (selected == null || selected.isEmpty()) {
            log("⚠ 请先选中要保存为模板的节点");
            return null;
        }
        CanvasTemplate template = new CanvasTemplate();
        template.setTemplateName(templateName);
        template.setTemplateCategory(templateCategory);
        template.setDescription(description);
        List<TemplateNode> templateNodes = new ArrayList<>();
        for (ProcessNode node : selected) {
            TemplateNode tn = new TemplateNode();
            tn.setNodeId(node.getNodeId());
            tn.setNodeType(node.getType());
            tn.setNodeName(node.getJobHandlerName());
            tn.setX(node.getLayoutX());
            tn.setY(node.getLayoutY());
            Map<String, Object> props = new HashMap<>();
            props.put("color", node.getCurrentColor());
            props.put("tags", node.getTags());
            props.put("remark", node.getRemark());
            tn.setProperties(props);
            templateNodes.add(tn);
        }
        template.setNodes(templateNodes);
        Set<String> selectedIds = new HashSet<>();
        for (ProcessNode n : selected) {
            if (n.getNodeId() != null) selectedIds.add(n.getNodeId());
        }
        List<TemplateConnection> templateConnections = new ArrayList<>();
        for (NodeConnection conn : canvas.getConnections()) {
            String srcId = conn.getSourceNode() != null ? conn.getSourceNode().getNodeId() : null;
            String tgtId = conn.getTargetNode() != null ? conn.getTargetNode().getNodeId() : null;
            if (srcId != null && tgtId != null && selectedIds.contains(srcId) && selectedIds.contains(tgtId)) {
                TemplateConnection tc = new TemplateConnection();
                tc.setSourceNodeId(srcId);
                tc.setTargetNodeId(tgtId);
                tc.setStartPoint("right");
                tc.setEndPoint("left");
                templateConnections.add(tc);
            }
        }
        template.setConnections(templateConnections);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("createTime", System.currentTimeMillis());
        metadata.put("nodeCount", templateNodes.size());
        metadata.put("connectionCount", templateConnections.size());
        template.setMetadata(metadata);
        log("✓ 已从选中创建画布模板: " + templateName);
        return template;
    }

    /**
     * 应用模板到画布（在指定偏移处插入）
     *
     * @param canvas 画布
     * @param template 模板
     * @param offsetX 插入位置 X 偏移
     * @param offsetY 插入位置 Y 偏移
     * @return 是否成功
     */
    public boolean applyTemplateToCanvas(NodeCanvas canvas, CanvasTemplate template, double offsetX, double offsetY) {
        if (canvas == null || template == null || template.getNodes() == null) {
            log("⚠ 画布或模板无效");
            return false;
        }
        return canvas.appendTemplateToCanvas(template, offsetX, offsetY, this::log);
    }

    /**
     * 应用模板到画布（默认偏移 0,0）
     */
    public boolean applyTemplateToCanvas(NodeCanvas canvas, CanvasTemplate template) {
        return applyTemplateToCanvas(canvas, template, 0, 0);
    }
    
    /**
     * 保存模板到内存缓存（实际应该保存到数据库）
     */
    public void saveTemplate(CanvasTemplate template) {
        if (template == null || template.getTemplateName() == null) {
            return;
        }
        
        // TODO: 保存到数据库
        // 这里先保存到内存缓存
        templateCache.put(template.getTemplateName(), template);
        log("✓ 模板已保存: " + template.getTemplateName());
    }
    
    /**
     * 获取模板列表（按分类）
     *
     * @param category 分类（null表示所有分类）
     * @return 模板列表
     */
    public List<CanvasTemplate> getTemplates(String category) {
        List<CanvasTemplate> templates = new ArrayList<>(templateCache.values());
        
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
        for (CanvasTemplate template : templateCache.values()) {
            if (template.getTemplateCategory() != null) {
                categories.add(template.getTemplateCategory());
            }
        }
        return new ArrayList<>(categories);
    }
    
    /**
     * 删除模板
     */
    public boolean deleteTemplate(String templateName) {
        if (templateName == null) {
            return false;
        }
        
        CanvasTemplate removed = templateCache.remove(templateName);
        if (removed != null) {
            log("✓ 模板已删除: " + templateName);
            return true;
        }
        return false;
    }
    
    /**
     * 导出模板为JSON文件
     */
    public String exportTemplateToJson(CanvasTemplate template) {
        if (template == null) {
            return null;
        }
        
        return gson.toJson(template);
    }
    
    /**
     * 从JSON文件导入模板
     */
    public CanvasTemplate importTemplateFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return gson.fromJson(json, CanvasTemplate.class);
        } catch (Exception e) {
            log("✗ 导入模板失败: " + e.getMessage());
            logger.error("导入模板失败", e);
            return null;
        }
    }
    
    private void log(String message) {
        if (loggerCallback != null) {
            loggerCallback.accept(message);
        }
        logger.info(message);
    }
}
