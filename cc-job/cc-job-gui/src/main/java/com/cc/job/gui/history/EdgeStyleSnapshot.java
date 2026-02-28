package com.cc.job.gui.history;

import com.cc.job.gui.model.NodeConnection;

import java.util.Objects;

/**
 * 连线样式快照，用于撤销/重做时恢复连线样式、颜色与标签。
 */
public final class EdgeStyleSnapshot {

    private final NodeConnection.EdgeStyle edgeStyle;
    private final String edgeColor;
    private final String labelText;

    public EdgeStyleSnapshot(NodeConnection.EdgeStyle edgeStyle, String edgeColor, String labelText) {
        this.edgeStyle = edgeStyle != null ? edgeStyle : NodeConnection.EdgeStyle.SOLID;
        this.edgeColor = edgeColor != null ? edgeColor : "#374151";
        this.labelText = labelText != null ? labelText : "";
    }

    public static EdgeStyleSnapshot from(NodeConnection conn) {
        if (conn == null) {
            return new EdgeStyleSnapshot(NodeConnection.EdgeStyle.SOLID, "#374151", "");
        }
        return new EdgeStyleSnapshot(
            conn.getEdgeStyle(),
            conn.getEdgeColor(),
            conn.getLabelText()
        );
    }

    public void applyTo(NodeConnection conn) {
        if (conn == null) return;
        conn.setEdgeStyle(edgeStyle);
        conn.setEdgeColor(edgeColor);
        conn.setLabelText(labelText);
    }

    public NodeConnection.EdgeStyle getEdgeStyle() { return edgeStyle; }
    public String getEdgeColor() { return edgeColor; }
    public String getLabelText() { return labelText; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgeStyleSnapshot that = (EdgeStyleSnapshot) o;
        return edgeStyle == that.edgeStyle
            && Objects.equals(edgeColor, that.edgeColor)
            && Objects.equals(labelText, that.labelText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edgeStyle, edgeColor, labelText);
    }
}
