package com.cc.job.gui.model;

/**
 * 树形节点数据封装类
 * 用于在TreeView中存储节点的额外信息
 */
public class TreeNodeData {
    
    private Long id;
    private String label;
    private Integer type;
    private String ext1;
    
    public TreeNodeData(Long id, String label, Integer type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }
    
    public TreeNodeData(Long id, String label, Integer type, String ext1) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.ext1 = ext1;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getLabel() {
        return label;
    }
    
    public Integer getType() {
        return type;
    }
    
    public String getExt1() {
        return ext1;
    }
    
    /**
     * 获取类型名称
     */
    public String getTypeName() {
        if (type == null) return "未知";
        return switch (type) {
            case 0 -> "分区";
            case 1 -> "任务组";
            case 2 -> "任务容器";
            case 3 -> "关系容器";
            case 4 -> "任务节点";
            case 5 -> "关系边";
            default -> "未知";
        };
    }
    
    /**
     * 获取类型图标 - 扁平化设计
     */
    public String getTypeIcon() {
        if (type == null) return "?";
        return switch (type) {
            case 0 -> "□";  // 分区 - 方框
            case 1 -> "◇";  // 任务组 - 菱形
            case 2 -> "≡";  // 任务容器 - 三横线
            case 3 -> "⇄";  // 关系容器 - 左右箭头
            case 4 -> "●";  // 任务节点 - 圆点
            case 5 -> "→";  // 关系边 - 箭头
            default -> "?"; // 未知
        };
    }
    
    /**
     * 获取类型颜色
     */
    public String getTypeColor() {
        if (type == null) return "#6B7280";
        return switch (type) {
            case 0 -> "#3B82F6";  // 分区 - 蓝色
            case 1 -> "#8B5CF6";  // 任务组 - 紫色
            case 2 -> "#10B981";  // 任务容器 - 绿色
            case 3 -> "#F59E0B";  // 关系容器 - 橙色
            case 4 -> "#06B6D4";  // 任务节点 - 青色
            case 5 -> "#EC4899";  // 关系边 - 粉色
            default -> "#6B7280"; // 未知 - 灰色
        };
    }
    
    @Override
    public String toString() {
        return label;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TreeNodeData that = (TreeNodeData) obj;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

