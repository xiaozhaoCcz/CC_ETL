package com.cc.job.gui.manager;

import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.ThemeManager;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.*;
import java.util.function.Consumer;

/**
 * 画布选择管理器 - 负责框选、多选等操作
 */
public class CanvasSelectionManager {
    
    private final Pane canvas;
    private final List<ProcessNode> nodes;
    private final List<NodeConnection> connections;
    private final Consumer<String> logger;
    private final Runnable notifyChanged;
    
    private boolean selectionMode = false;
    private Rectangle selectionRect;
    private Rectangle selectionBoundingBox;
    private double selectionStartX;
    private double selectionStartY;
    private Set<ProcessNode> selectedNodes = new HashSet<>();
    private Set<NodeConnection> selectedConnections = new HashSet<>();
    private Map<ProcessNode, double[]> selectionOriginalPositions = new HashMap<>();
    
    private boolean isMovingSelection = false;
    private ProcessNode dragStartNode;
    /** 每个选中节点一个红框，避免一个大框包住多节点造成误解 */
    private final List<Rectangle> perNodeSelectionBoxes = new ArrayList<>();

    public CanvasSelectionManager(Pane canvas, List<ProcessNode> nodes, List<NodeConnection> connections,
                                  Consumer<String> logger, Runnable notifyChanged) {
        this.canvas = canvas;
        this.nodes = nodes;
        this.connections = connections;
        this.logger = logger;
        this.notifyChanged = notifyChanged;
        initializeSelectionRectangles();
    }
    
    private static boolean isDarkTheme() {
        return "dark".equals(ThemeManager.getInstance().getTheme());
    }
    
    private static void applySelectionRectColors(Rectangle rect, String fillStrokeHex) {
        rect.setFill(Color.web(fillStrokeHex, 0.1));
        rect.setStroke(Color.web(fillStrokeHex));
    }
    
    private static String getSelectionRectColor() {
        return isDarkTheme() ? "#3B82F6" : "#2563EB";
    }
    
    private static String getSelectionBoundingBoxStrokeColor() {
        return isDarkTheme() ? "#F87171" : "#EF4444";
    }
    
    private static String getSelectionHighlightShadowRgba() {
        return isDarkTheme() ? "rgba(96,165,250,0.5)" : "rgba(37,99,235,0.5)";
    }
    
    private void initializeSelectionRectangles() {
        selectionRect = new Rectangle();
        applySelectionRectColors(selectionRect, getSelectionRectColor());
        selectionRect.setStrokeWidth(2);
        selectionRect.getStrokeDashArray().addAll(5.0, 5.0);
        selectionRect.setVisible(false);
        selectionRect.setMouseTransparent(true);
        canvas.getChildren().add(selectionRect);
        
        selectionBoundingBox = new Rectangle();
        selectionBoundingBox.setFill(Color.TRANSPARENT);
        selectionBoundingBox.setStroke(Color.web(getSelectionBoundingBoxStrokeColor()));
        selectionBoundingBox.setStrokeWidth(2);
        selectionBoundingBox.getStrokeDashArray().addAll(8.0, 4.0);
        selectionBoundingBox.setVisible(false);
        selectionBoundingBox.setMouseTransparent(true);
        canvas.getChildren().add(selectionBoundingBox);
    }
    
    public boolean isSelectionMode() {
        return selectionMode;
    }
    
    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        if (!enabled) {
            clearSelection();
        }
    }
    
    public void startSelection(double x, double y) {
        selectionStartX = x;
        selectionStartY = y;
        selectionRect.setX(x);
        selectionRect.setY(y);
        selectionRect.setWidth(0);
        selectionRect.setHeight(0);
        selectionRect.setVisible(true);
        canvas.getChildren().remove(selectionRect);
        canvas.getChildren().add(selectionRect);
        selectionRect.toFront();
    }
    
    public void updateSelection(double currentX, double currentY) {
        double rectX = Math.min(selectionStartX, currentX);
        double rectY = Math.min(selectionStartY, currentY);
        double rectWidth = Math.abs(currentX - selectionStartX);
        double rectHeight = Math.abs(currentY - selectionStartY);
        
        selectionRect.setX(rectX);
        selectionRect.setY(rectY);
        selectionRect.setWidth(rectWidth);
        selectionRect.setHeight(rectHeight);
        selectionRect.toFront();
        
        updateSelectionDuringDrag(rectX, rectY, rectWidth, rectHeight);
    }
    
    public void finishSelection(double currentX, double currentY) {
        double rectX = Math.min(selectionStartX, currentX);
        double rectY = Math.min(selectionStartY, currentY);
        double rectWidth = Math.abs(currentX - selectionStartX);
        double rectHeight = Math.abs(currentY - selectionStartY);
        
        selectionRect.setVisible(false);
        
        if (rectWidth > 5 && rectHeight > 5) {
            performSelection(rectX, rectY, rectWidth, rectHeight);
        } else {
            clearSelection();
        }
    }
    
    private void updateSelectionDuringDrag(double rectX, double rectY, double rectWidth, double rectHeight) {
        for (ProcessNode node : selectedNodes) {
            highlightNode(node, false);
        }
        for (NodeConnection connection : selectedConnections) {
            connection.setSelected(false);
        }
        
        selectedNodes.clear();
        selectedConnections.clear();
        selectionOriginalPositions.clear();
        
        for (ProcessNode node : nodes) {
            if (rectIntersects(rectX, rectY, rectWidth, rectHeight,
                              node.getLayoutX(), node.getLayoutY(), 
                              node.getPrefWidth(), node.getPrefHeight())) {
                selectedNodes.add(node);
                highlightNode(node, true);
                selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        }
        
        for (NodeConnection connection : connections) {
            ProcessNode sourceNode = connection.getSourceNode();
            ProcessNode targetNode = connection.getTargetNode();
            if (sourceNode != null && targetNode != null &&
                selectedNodes.contains(sourceNode) && selectedNodes.contains(targetNode)) {
                selectedConnections.add(connection);
                connection.setSelected(true);
            }
        }
    }
    
    private void performSelection(double rectX, double rectY, double rectWidth, double rectHeight) {
        clearSelectionVisual();
        selectedNodes.clear();
        selectedConnections.clear();
        selectionOriginalPositions.clear();
        
        for (ProcessNode node : nodes) {
            if (rectIntersects(rectX, rectY, rectWidth, rectHeight,
                              node.getLayoutX(), node.getLayoutY(),
                              node.getPrefWidth(), node.getPrefHeight())) {
                selectedNodes.add(node);
                highlightNode(node, true);
                selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        }
        
        for (NodeConnection connection : connections) {
            Node sourceOwner = connection.getSourceOwner();
            Node targetOwner = connection.getTargetOwner();
            
            if (sourceOwner instanceof ProcessNode && targetOwner instanceof ProcessNode) {
                ProcessNode sourceNode = (ProcessNode) sourceOwner;
                ProcessNode targetNode = (ProcessNode) targetOwner;
                
                if (selectedNodes.contains(sourceNode) && selectedNodes.contains(targetNode)) {
                    selectedConnections.add(connection);
                    connection.setSelected(true);
                }
            }
        }
        
        updateSelectionBoundingBox();
    }
    
    public void updateSelectionBoundingBox() {
        selectionBoundingBox.setVisible(false);
        for (Rectangle box : perNodeSelectionBoxes) {
            canvas.getChildren().remove(box);
        }
        perNodeSelectionBoxes.clear();

        if (selectedNodes.isEmpty()) {
            return;
        }

        double padding = 10;
        String strokeColor = getSelectionBoundingBoxStrokeColor();
        for (ProcessNode node : selectedNodes) {
            double x = node.getLayoutX();
            double y = node.getLayoutY();
            double w = node.getPrefWidth() > 0 ? node.getPrefWidth() : 120;
            double h = node.getPrefHeight() > 0 ? node.getPrefHeight() : 40;
            Rectangle box = new Rectangle();
            box.setFill(Color.TRANSPARENT);
            box.setStroke(Color.web(strokeColor));
            box.setStrokeWidth(2);
            box.getStrokeDashArray().addAll(8.0, 4.0);
            box.setMouseTransparent(true);
            box.setX(x - padding);
            box.setY(y - padding);
            box.setWidth(w + padding * 2);
            box.setHeight(h + padding * 2);
            canvas.getChildren().add(box);
            box.toFront();
            perNodeSelectionBoxes.add(box);
        }
    }
    
    public void clearSelection() {
        clearSelectionVisual();
        selectedNodes.clear();
        selectedConnections.clear();
        selectionRect.setVisible(false);
        selectionBoundingBox.setVisible(false);
        isMovingSelection = false;
        dragStartNode = null;
        selectionOriginalPositions.clear();
    }
    
    private void clearSelectionVisual() {
        for (ProcessNode node : selectedNodes) {
            highlightNode(node, false);
        }
        for (NodeConnection connection : selectedConnections) {
            connection.setSelected(false);
        }
        selectionBoundingBox.setVisible(false);
        for (Rectangle box : perNodeSelectionBoxes) {
            canvas.getChildren().remove(box);
        }
        perNodeSelectionBoxes.clear();
    }
    
    public void highlightNode(ProcessNode node, boolean highlight) {
        if (highlight) {
            String shadowRgba = getSelectionHighlightShadowRgba();
            node.setStyle("-fx-effect: dropshadow(gaussian, " + shadowRgba + ", 10, 0, 0, 0);");
        } else {
            node.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        }
    }
    
    public void selectNode(ProcessNode node) {
        if (node == null || !nodes.contains(node)) return;
        
        clearSelection();
        selectedNodes.add(node);
        highlightNode(node, true);
        selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
        updateSelectionBoundingBox();
    }
    
    public void selectNodes(Collection<ProcessNode> nodesToSelect) {
        if (nodesToSelect == null || nodesToSelect.isEmpty()) {
            clearSelection();
            return;
        }
        
        clearSelection();
        for (ProcessNode node : nodesToSelect) {
            if (node != null && nodes.contains(node)) {
                selectedNodes.add(node);
                highlightNode(node, true);
                selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
            }
        }
        updateSelectionBoundingBox();
    }
    
    /**
     * Shift+点击多选：切换节点的选中状态（已选则取消，未选则加入）
     */
    public void toggleNodeSelection(ProcessNode node) {
        if (node == null || !nodes.contains(node)) return;
        if (selectedNodes.contains(node)) {
            selectedNodes.remove(node);
            highlightNode(node, false);
            selectionOriginalPositions.remove(node);
            if (selectedNodes.isEmpty()) {
                clearSelection();
                return;
            }
        } else {
            selectedNodes.add(node);
            highlightNode(node, true);
            selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
        }
        updateSelectionBoundingBox();
    }
    
    /**
     * Shift+点击多选：切换边的选中状态（已选则取消，未选则加入）
     */
    public void toggleConnectionSelection(NodeConnection conn) {
        if (conn == null || !connections.contains(conn)) return;
        if (selectedConnections.contains(conn)) {
            selectedConnections.remove(conn);
            conn.setSelected(false);
        } else {
            selectedConnections.add(conn);
            conn.setSelected(true);
        }
        updateSelectionBoundingBox();
    }
    
    /**
     * 单选一条边时：只选该边及其两端节点（用于复制/剪切该子图）
     */
    public void selectNodesAndConnections(Collection<ProcessNode> nodesToSelect, Collection<NodeConnection> connectionsToSelect) {
        clearSelection();
        if (nodesToSelect != null) {
            for (ProcessNode node : nodesToSelect) {
                if (node != null && nodes.contains(node)) {
                    selectedNodes.add(node);
                    highlightNode(node, true);
                    selectionOriginalPositions.put(node, new double[]{node.getLayoutX(), node.getLayoutY()});
                }
            }
        }
        if (connectionsToSelect != null) {
            for (NodeConnection conn : connectionsToSelect) {
                if (conn != null && connections.contains(conn)) {
                    selectedConnections.add(conn);
                    conn.setSelected(true);
                }
            }
        }
        updateSelectionBoundingBox();
    }
    
    public void alignHorizontal() {
        if (selectedNodes.size() < 2) {
            return;
        }
        
        double avgY = 0.0;
        for (ProcessNode node : selectedNodes) {
            avgY += node.getLayoutY();
        }
        
        final double finalAvgY = Math.max(0, avgY / selectedNodes.size());
        for (ProcessNode node : selectedNodes) {
            node.setLayoutY(finalAvgY);
        }
        
        updateSelectionBoundingBox();
        notifyChanged.run();
    }
    
    public void alignVertical() {
        if (selectedNodes.size() < 2) {
            return;
        }
        
        double avgX = 0.0;
        for (ProcessNode node : selectedNodes) {
            avgX += node.getLayoutX();
        }
        
        final double finalAvgX = Math.max(0, avgX / selectedNodes.size());
        for (ProcessNode node : selectedNodes) {
            node.setLayoutX(finalAvgX);
        }
        
        updateSelectionBoundingBox();
        notifyChanged.run();
    }
    
    private boolean rectIntersects(double x1, double y1, double w1, double h1,
                                   double x2, double y2, double w2, double h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
    
    public Set<ProcessNode> getSelectedNodes() {
        return new HashSet<>(selectedNodes);
    }
    
    public Set<NodeConnection> getSelectedConnections() {
        return new HashSet<>(selectedConnections);
    }
    
    public javafx.scene.shape.Rectangle getSelectionRect() {
        return selectionRect;
    }
    
    public boolean isMovingSelection() {
        return isMovingSelection;
    }
    
    public void setMovingSelection(boolean moving) {
        this.isMovingSelection = moving;
    }
    
    public ProcessNode getDragStartNode() {
        return dragStartNode;
    }
    
    public void setDragStartNode(ProcessNode node) {
        this.dragStartNode = node;
    }
    
    public Map<ProcessNode, double[]> getSelectionOriginalPositions() {
        return selectionOriginalPositions;
    }
    
    public javafx.scene.shape.Rectangle getSelectionBoundingBox() {
        return selectionBoundingBox;
    }
    
    /**
     * 主题切换后刷新选择框、外框及选中节点/连线的颜色。
     */
    public void refreshThemeColors() {
        applySelectionRectColors(selectionRect, getSelectionRectColor());
        selectionBoundingBox.setStroke(Color.web(getSelectionBoundingBoxStrokeColor()));
        for (ProcessNode node : selectedNodes) {
            highlightNode(node, true);
        }
        for (NodeConnection conn : selectedConnections) {
            conn.setSelected(true);
        }
        updateSelectionBoundingBox();
    }

    /**
     * 重新添加选择矩形到画布（在清空后调用）
     */
    public void reattachToCanvas() {
        if (!canvas.getChildren().contains(selectionRect)) {
            canvas.getChildren().add(selectionRect);
        }
        if (!canvas.getChildren().contains(selectionBoundingBox)) {
            canvas.getChildren().add(selectionBoundingBox);
        }
        if (!selectedNodes.isEmpty()) {
            updateSelectionBoundingBox();
        }
    }
}

