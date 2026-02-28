package com.cc.job.gui.history;

import com.cc.job.gui.model.ProcessNode;

import java.util.Objects;

/**
 * 节点样式快照，用于撤销/重做时恢复节点颜色、大小、边框样式与粗细。
 */
public final class NodeStyleSnapshot {

    private final String color;
    private final double width;
    private final double height;
    private final ProcessNode.BorderStyle borderStyle;
    private final double borderWidth;

    public NodeStyleSnapshot(String color, double width, double height,
                             ProcessNode.BorderStyle borderStyle, double borderWidth) {
        this.color = color != null ? color : "#2563EB";
        this.width = width;
        this.height = height;
        this.borderStyle = borderStyle != null ? borderStyle : ProcessNode.BorderStyle.SOLID;
        this.borderWidth = borderWidth;
    }

    public static NodeStyleSnapshot from(ProcessNode node) {
        if (node == null) {
            return new NodeStyleSnapshot("#2563EB", 180, 80, ProcessNode.BorderStyle.SOLID, 2.0);
        }
        return new NodeStyleSnapshot(
            node.getCurrentColor(),
            node.getNodeWidth(),
            node.getNodeHeight(),
            node.getBorderStyle(),
            node.getBorderWidth()
        );
    }

    public void applyTo(ProcessNode node) {
        if (node == null) return;
        node.setNodeColor(color);
        node.setNodeSize(width, height);
        node.setBorderStyle(borderStyle);
        node.setBorderWidth(borderWidth);
    }

    public String getColor() { return color; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public ProcessNode.BorderStyle getBorderStyle() { return borderStyle; }
    public double getBorderWidth() { return borderWidth; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeStyleSnapshot that = (NodeStyleSnapshot) o;
        return Double.compare(that.width, width) == 0
            && Double.compare(that.height, height) == 0
            && Double.compare(that.borderWidth, borderWidth) == 0
            && Objects.equals(color, that.color)
            && borderStyle == that.borderStyle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, width, height, borderStyle, borderWidth);
    }
}
