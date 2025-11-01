package com.example.nodefx.model;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/**
 * 节点连接线，使用贝塞尔曲线
 */
public class NodeConnection extends Group {
    
    private ProcessNode sourceNode;
    private Circle sourceConnector;
    private ProcessNode targetNode;
    private Circle targetConnector;
    
    private CubicCurve curve;
    private Polygon arrowHead;
    
    /**
     * 创建连接（指定具体的连接点）
     */
    public NodeConnection(ProcessNode sourceNode, Circle sourceConnector,
                         ProcessNode targetNode, Circle targetConnector) {
        this.sourceNode = sourceNode;
        this.sourceConnector = sourceConnector;
        this.targetNode = targetNode;
        this.targetConnector = targetConnector;
        
        initializeUI();
        bindConnection();
    }
    
    private void initializeUI() {
        // 创建贝塞尔曲线
        curve = new CubicCurve();
        curve.setStroke(Color.web("#374151"));
        curve.setStrokeWidth(2.5);
        curve.setFill(Color.TRANSPARENT);
        
        // 创建更明显的箭头（加大尺寸）
        // 箭头指向右侧：(0,0) 是箭头尖端
        arrowHead = new Polygon(
            0.0, 0.0,        // 箭头尖端
            -12.0, 8.0,      // 左下角
            -12.0, -8.0      // 左上角
        );
        arrowHead.setFill(Color.web("#374151"));
        arrowHead.setStroke(Color.web("#374151"));
        arrowHead.setStrokeWidth(1);
        
        this.getChildren().addAll(curve, arrowHead);
        
        // 鼠标悬停效果
        setupHoverEffect();
    }
    
    private void bindConnection() {
        // 计算起点坐标（源连接点的中心）
        DoubleBinding startX = createConnectorCenterXBinding(sourceNode, sourceConnector);
        DoubleBinding startY = createConnectorCenterYBinding(sourceNode, sourceConnector);
        
        // 计算终点坐标（目标连接点的中心）
        DoubleBinding endX = createConnectorCenterXBinding(targetNode, targetConnector);
        DoubleBinding endY = createConnectorCenterYBinding(targetNode, targetConnector);
        
        curve.startXProperty().bind(startX);
        curve.startYProperty().bind(startY);
        curve.endXProperty().bind(endX);
        curve.endYProperty().bind(endY);
        
        // 计算控制点，创建平滑的曲线
        DoubleBinding distance = new DoubleBinding() {
            { super.bind(startX, startY, endX, endY); }
            @Override
            protected double computeValue() {
                double dx = endX.get() - startX.get();
                double dy = endY.get() - startY.get();
                return Math.sqrt(dx * dx + dy * dy) / 3;
            }
        };
        
        curve.controlX1Property().bind(startX.add(distance));
        curve.controlY1Property().bind(startY);
        
        curve.controlX2Property().bind(endX.subtract(distance));
        curve.controlY2Property().bind(endY);
        
        // 绑定箭头位置和旋转
        bindArrowHead(endX, endY);
    }
    
    /**
     * 创建连接点中心X坐标的绑定
     */
    private DoubleBinding createConnectorCenterXBinding(ProcessNode node, Circle connector) {
        return new DoubleBinding() {
            {
                super.bind(node.layoutXProperty(), connector.layoutXProperty());
            }
            @Override
            protected double computeValue() {
                // Circle在Pane中：layoutX/layoutY 指定的是圆心位置
                javafx.geometry.Point2D connectorCenter = new javafx.geometry.Point2D(
                    connector.getLayoutX(),
                    connector.getLayoutY()
                );
                javafx.geometry.Point2D nodeLocal = node.getConnectorPane().localToParent(connectorCenter);
                javafx.geometry.Point2D parentLocal = node.localToParent(nodeLocal);
                return parentLocal.getX();
            }
        };
    }
    
    /**
     * 创建连接点中心Y坐标的绑定
     */
    private DoubleBinding createConnectorCenterYBinding(ProcessNode node, Circle connector) {
        return new DoubleBinding() {
            {
                super.bind(node.layoutYProperty(), connector.layoutYProperty());
            }
            @Override
            protected double computeValue() {
                // Circle在Pane中：layoutX/layoutY 指定的是圆心位置
                javafx.geometry.Point2D connectorCenter = new javafx.geometry.Point2D(
                    connector.getLayoutX(),
                    connector.getLayoutY()
                );
                javafx.geometry.Point2D nodeLocal = node.getConnectorPane().localToParent(connectorCenter);
                javafx.geometry.Point2D parentLocal = node.localToParent(nodeLocal);
                return parentLocal.getY();
            }
        };
    }
    
    private void bindArrowHead(DoubleBinding endX, DoubleBinding endY) {
        // 箭头跟随终点位置
        arrowHead.layoutXProperty().bind(endX);
        arrowHead.layoutYProperty().bind(endY);
        
        // 监听坐标变化，更新箭头旋转角度
        endX.addListener((obs, oldVal, newVal) -> updateArrowRotation());
        endY.addListener((obs, oldVal, newVal) -> updateArrowRotation());
        curve.controlX2Property().addListener((obs, oldVal, newVal) -> updateArrowRotation());
        curve.controlY2Property().addListener((obs, oldVal, newVal) -> updateArrowRotation());
        
        updateArrowRotation();
    }
    
    private void updateArrowRotation() {
        double endX = curve.getEndX();
        double endY = curve.getEndY();
        double ctrlX2 = curve.getControlX2();
        double ctrlY2 = curve.getControlY2();
        
        double angle = Math.toDegrees(Math.atan2(endY - ctrlY2, endX - ctrlX2));
        
        arrowHead.getTransforms().clear();
        arrowHead.getTransforms().add(new javafx.scene.transform.Rotate(angle, 0, 0));
    }
    
    private void setupHoverEffect() {
        // 整个Group的悬停效果
        this.setOnMouseEntered(e -> {
            curve.setStroke(Color.web("#8B5CF6"));
            curve.setStrokeWidth(3.5);
            arrowHead.setFill(Color.web("#8B5CF6"));
            arrowHead.setStroke(Color.web("#8B5CF6"));
        });
        
        this.setOnMouseExited(e -> {
            curve.setStroke(Color.web("#374151"));
            curve.setStrokeWidth(2.5);
            arrowHead.setFill(Color.web("#374151"));
            arrowHead.setStroke(Color.web("#374151"));
        });
    }
    
    // Getters
    public ProcessNode getSourceNode() {
        return sourceNode;
    }
    
    public Circle getSourceConnector() {
        return sourceConnector;
    }
    
    public ProcessNode getTargetNode() {
        return targetNode;
    }
    
    public Circle getTargetConnector() {
        return targetConnector;
    }
}
