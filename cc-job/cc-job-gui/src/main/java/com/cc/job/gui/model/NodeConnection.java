package com.cc.job.gui.model;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Polygon;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点连接线，使用贝塞尔曲线
 */
public class NodeConnection extends Group {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeConnection.class);
    
    private ProcessNode sourceNode;
    private ProcessNode targetNode;
    private Circle sourceConnector;
    private Circle targetConnector;
    // 新增：通用所有者与连接点父层，支持 GroupContainer 等
    private Node sourceOwner;
    private Pane sourceConnectorParent;
    private Node targetOwner;
    private Pane targetConnectorParent;
    private final SimpleObjectProperty<String> edgeId = new SimpleObjectProperty<>();
    
    private CubicCurve curve;
    private Polygon arrowHead;
    private Timeline dashAnimation;
    private boolean isRunning = false; // 是否处于运行状态
    private Timeline locateAnimation;
    
    // 连线标签相关
    private javafx.scene.control.Label edgeLabel; // 连线标签
    private String labelText = ""; // 标签文本
    private boolean labelVisible = false; // 标签是否可见
    
    /**
     * 创建连接（指定具体的连接点）
     */
    public NodeConnection(ProcessNode sourceNode, Circle sourceConnector,
                         ProcessNode targetNode, Circle targetConnector) {
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.sourceConnector = sourceConnector;
        this.targetConnector = targetConnector;
        this.sourceOwner = sourceNode;
        this.targetOwner = targetNode;
        this.sourceConnectorParent = sourceNode.getConnectorPane();
        this.targetConnectorParent = targetNode.getConnectorPane();
        
        initializeUI();
        bindConnection();
        updateStyle();
    }

    /**
     * 新增：通用构造，允许任意拥有者与连接点父层（例如 GroupContainer）
     */
    public NodeConnection(Node sourceOwner, Pane sourceConnectorParent, Circle sourceConnector,
                          Node targetOwner, Pane targetConnectorParent, Circle targetConnector) {
        this.sourceOwner = sourceOwner;
        this.targetOwner = targetOwner;
        this.sourceConnectorParent = sourceConnectorParent;
        this.targetConnectorParent = targetConnectorParent;
        this.sourceConnector = sourceConnector;
        this.targetConnector = targetConnector;
        
        // 如果拥有者是 ProcessNode，同时设置 sourceNode 和 targetNode
        if (sourceOwner instanceof ProcessNode) {
            this.sourceNode = (ProcessNode) sourceOwner;
        }
        if (targetOwner instanceof ProcessNode) {
            this.targetNode = (ProcessNode) targetOwner;
        }

        initializeUI();
        bindConnection();
        updateStyle();
    }

    public void setEdgeId(String edgeId) {
        this.edgeId.set(edgeId);
    }

    public String getEdgeId() {
        return edgeId.get();
    }

    public SimpleObjectProperty<String> edgeIdProperty() {
        return edgeId;
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
        
        // 创建连线标签（默认隐藏）
        edgeLabel = new javafx.scene.control.Label();
        edgeLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-font-weight: 500; " +
            "-fx-text-fill: #6366F1; " +
            "-fx-background-color: rgba(255, 255, 255, 0.9); " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 2 6 2 6;"
        );
        edgeLabel.setVisible(false);
        edgeLabel.setMouseTransparent(true);
        
        this.getChildren().addAll(curve, arrowHead, edgeLabel);
        
        // 鼠标悬停效果
        setupHoverEffect();
        
        // 绑定标签位置到连线中点
        bindLabelPosition();
    }
    
    private void bindConnection() {
        // 计算起点坐标（源连接点的中心）
        DoubleBinding startX = createConnectorCenterXBinding(sourceOwner, sourceConnectorParent, sourceConnector);
        DoubleBinding startY = createConnectorCenterYBinding(sourceOwner, sourceConnectorParent, sourceConnector);
        
        // 计算终点坐标（目标连接点的中心）
        DoubleBinding endX = createConnectorCenterXBinding(targetOwner, targetConnectorParent, targetConnector);
        DoubleBinding endY = createConnectorCenterYBinding(targetOwner, targetConnectorParent, targetConnector);
        
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
    private DoubleBinding createConnectorCenterXBinding(Node owner, Pane connectorParent, Circle connector) {
        return new DoubleBinding() {
            {
                super.bind(owner.layoutXProperty(), connector.layoutXProperty());
            }
            @Override
            protected double computeValue() {
                // Circle在Pane中：layoutX/layoutY 指定的是圆心位置
                Point2D connectorCenter = new Point2D(
                    connector.getLayoutX(),
                    connector.getLayoutY()
                );
                Point2D nodeLocal = connectorParent.localToParent(connectorCenter);
                Point2D parentLocal = owner.localToParent(nodeLocal);
                return parentLocal.getX();
            }
        };
    }
    
    /**
     * 创建连接点中心Y坐标的绑定
     */
    private DoubleBinding createConnectorCenterYBinding(Node owner, Pane connectorParent, Circle connector) {
        return new DoubleBinding() {
            {
                super.bind(owner.layoutYProperty(), connector.layoutYProperty());
            }
            @Override
            protected double computeValue() {
                // Circle在Pane中：layoutX/layoutY 指定的是圆心位置
                Point2D connectorCenter = new Point2D(
                    connector.getLayoutX(),
                    connector.getLayoutY()
                );
                Point2D nodeLocal = connectorParent.localToParent(connectorCenter);
                Point2D parentLocal = owner.localToParent(nodeLocal);
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
        arrowHead.getTransforms().add(new Rotate(angle, 0, 0));
    }
    
    private void setupHoverEffect() {
        // 整个Group的悬停效果
        this.setOnMouseEntered(e -> {
            if (!isRunning && !isSelected() && !isBlocked) {
                // 只有在非阻塞状态下才显示紫色悬停效果
                curve.setStroke(Color.web("#8B5CF6"));
                curve.setStrokeWidth(3.0);
                arrowHead.setFill(Color.web("#8B5CF6"));
                arrowHead.setStroke(Color.web("#8B5CF6"));
            }
            // 阻塞状态下保持灰色，不改变颜色
        });
        
        this.setOnMouseExited(e -> {
            if (!isRunning && !isSelected()) {
                // 根据当前状态恢复颜色
                if (isBlocked) {
                    // 阻塞状态：保持灰色
                    curve.setStroke(Color.web("#9CA3AF"));
                    curve.setStrokeWidth(2.5);
                    arrowHead.setFill(Color.web("#9CA3AF"));
                    arrowHead.setStroke(Color.web("#9CA3AF"));
                } else {
                    // 普通状态：恢复为深灰色
                    curve.setStroke(Color.web("#374151"));
                    curve.setStrokeWidth(2.5);
                    arrowHead.setFill(Color.web("#374151"));
                    arrowHead.setStroke(Color.web("#374151"));
                }
            }
        });
    }
    
    /**
     * 设置运行状态（运行时显示虚线并添加动画）
     * @param running 是否运行中
     */
    public void setRunning(boolean running) {
        this.isRunning = running;
        updateStyle();
        // ⭐ 修复：使用 getSourceOwner() 和 getTargetOwner()，支持任务组容器
        String sourceName = getOwnerName(sourceOwner);
        String targetName = getOwnerName(targetOwner);
    }
    
    /**
     * 获取所有者名称（支持 ProcessNode 和 GroupContainer）
     */
    private String getOwnerName(Node owner) {
        if (owner instanceof ProcessNode) {
            return ((ProcessNode) owner).getJobHandlerName();
        } else if (owner instanceof GroupContainer) {
            return ((GroupContainer) owner).getGroupName();
        }
        return "未知";
    }
    
    /**
     * 启动虚线滚动动画（改进版：更流畅的动画效果）
     */
    private void startDashAnimation() {
        if (dashAnimation != null) {
            dashAnimation.stop();
        }
        
        // 创建更流畅的动画：虚线沿着路径方向（从源节点到目标节点）向前滚动
        // 使用负的偏移量变化，让虚线看起来是向前流动的
        dashAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(curve.strokeDashOffsetProperty(), 15)),
            new KeyFrame(Duration.millis(1000), new KeyValue(curve.strokeDashOffsetProperty(), 0))
        );
        
        dashAnimation.setCycleCount(Timeline.INDEFINITE);
        dashAnimation.play();
    }
    
    /**
     * 停止虚线滚动动画
     */
    private void stopDashAnimation() {
        if (dashAnimation != null) {
            dashAnimation.stop();
            dashAnimation = null;
        }
        curve.setStrokeDashOffset(0); // 重置偏移量
    }

    private boolean selected = false;
    private boolean isBlocked = false; // 是否处于阻塞状态（被开始/终止/阻塞节点影响）

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateStyle();
    }

    public boolean isSelected() {
        return selected;
    }
    
    /**
     * 设置阻塞状态（显示虚线，表示连接被阻塞）
     * @param blocked 是否阻塞
     */
    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
        updateStyle();
    }
    
    public boolean isBlocked() {
        return isBlocked;
    }

    private void updateStyle() {
        if (selected) {
            stopDashAnimation();
            curve.getStrokeDashArray().clear();
            curve.setStroke(Color.web("#2563EB"));
            curve.setStrokeWidth(3.5);
            arrowHead.setFill(Color.web("#2563EB"));
            arrowHead.setStroke(Color.web("#2563EB"));
        } else if (isBlocked) {
            // 阻塞状态：灰色虚线，不带动画（优先级高于运行状态）
            // 即使设置了运行状态，如果连接线连接到阻塞节点，也不显示运行动画
            stopDashAnimation();
            curve.getStrokeDashArray().clear();
            curve.getStrokeDashArray().addAll(8.0, 4.0); // 虚线样式
            curve.setStroke(Color.web("#9CA3AF")); // 灰色，与阻塞节点颜色一致
            curve.setStrokeWidth(2.5);
            arrowHead.setFill(Color.web("#9CA3AF"));
            arrowHead.setStroke(Color.web("#9CA3AF"));
            arrowHead.setStrokeWidth(1);
        } else if (isRunning) {
            // 运行状态：橙色虚线，带滚动动画
            // 只有在非阻塞状态下才显示运行动画
            curve.getStrokeDashArray().clear();
            curve.getStrokeDashArray().addAll(10.0, 5.0);
            curve.setStroke(Color.web("#F59E0B"));
            curve.setStrokeWidth(3.5); // 稍微加粗，使动画更明显
            arrowHead.setFill(Color.web("#F59E0B"));
            arrowHead.setStroke(Color.web("#F59E0B"));
            arrowHead.setStrokeWidth(2); // 箭头也加粗
            startDashAnimation();
        } else {
            stopDashAnimation();
            curve.getStrokeDashArray().clear();
            curve.setStroke(Color.web("#374151"));
            curve.setStrokeWidth(2.5);
            arrowHead.setFill(Color.web("#374151"));
            arrowHead.setStroke(Color.web("#374151"));
            arrowHead.setStrokeWidth(1); // 恢复正常箭头粗细
        }
    }

    public void playLocateAnimation() {
        if (locateAnimation != null) {
            locateAnimation.stop();
        }
        Color originalColor = (Color) curve.getStroke();
        double originalWidth = curve.getStrokeWidth();
        Color highlight = Color.web("#2563EB");

        locateAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    curve.setStroke(highlight);
                    curve.setStrokeWidth(originalWidth + 1.5);
                    arrowHead.setFill(highlight);
                    arrowHead.setStroke(highlight);
                }),
                new KeyFrame(Duration.millis(250), e -> {
                    curve.setStroke(originalColor);
                    curve.setStrokeWidth(originalWidth);
                    arrowHead.setFill(originalColor);
                    arrowHead.setStroke(originalColor);
                })
        );
        locateAnimation.setCycleCount(4);
        locateAnimation.setAutoReverse(true);
        locateAnimation.setOnFinished(e -> updateStyle());
        locateAnimation.play();
    }
    
    /**
     * 检查是否处于运行状态
     */
    public boolean isRunning() {
        return isRunning;
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
    
    // 新增：通用拥有者与父层访问器（用于前端保存时识别连接两端）
    public Node getSourceOwner() { return sourceOwner; }
    public Node getTargetOwner() { return targetOwner; }
    
    // ==================== 连线标签相关方法 ====================
    
    /**
     * 绑定标签位置到连线中点
     */
    private void bindLabelPosition() {
        if (edgeLabel == null) return;
        
        // 计算连线中点位置
        javafx.beans.binding.DoubleBinding midX = new javafx.beans.binding.DoubleBinding() {
            {
                super.bind(curve.startXProperty(), curve.endXProperty(), 
                          curve.controlX1Property(), curve.controlY1Property(),
                          curve.controlX2Property(), curve.controlY2Property());
            }
            @Override
            protected double computeValue() {
                // 计算贝塞尔曲线中点（使用t=0.5）
                double t = 0.5;
                double x = Math.pow(1-t, 3) * curve.getStartX() +
                          3 * Math.pow(1-t, 2) * t * curve.getControlX1() +
                          3 * (1-t) * Math.pow(t, 2) * curve.getControlX2() +
                          Math.pow(t, 3) * curve.getEndX();
                return x;
            }
        };
        
        javafx.beans.binding.DoubleBinding midY = new javafx.beans.binding.DoubleBinding() {
            {
                super.bind(curve.startYProperty(), curve.endYProperty(),
                          curve.controlX1Property(), curve.controlY1Property(),
                          curve.controlX2Property(), curve.controlY2Property());
            }
            @Override
            protected double computeValue() {
                double t = 0.5;
                double y = Math.pow(1-t, 3) * curve.getStartY() +
                          3 * Math.pow(1-t, 2) * t * curve.getControlY1() +
                          3 * (1-t) * Math.pow(t, 2) * curve.getControlY2() +
                          Math.pow(t, 3) * curve.getEndY();
                return y;
            }
        };
        
        edgeLabel.layoutXProperty().bind(midX.subtract(edgeLabel.widthProperty().divide(2)));
        edgeLabel.layoutYProperty().bind(midY.subtract(edgeLabel.heightProperty().divide(2)));
    }
    
    /**
     * 设置连线标签文本
     */
    public void setLabelText(String text) {
        labelText = text != null ? text : "";
        if (edgeLabel != null) {
            edgeLabel.setText(labelText);
            updateLabelVisibility();
        }
    }
    
    /**
     * 获取连线标签文本
     */
    public String getLabelText() {
        return labelText;
    }
    
    /**
     * 设置标签可见性
     */
    public void setLabelVisible(boolean visible) {
        labelVisible = visible;
        updateLabelVisibility();
    }
    
    /**
     * 获取标签可见性
     */
    public boolean isLabelVisible() {
        return labelVisible;
    }
    
    /**
     * 更新标签可见性
     */
    private void updateLabelVisibility() {
        if (edgeLabel != null) {
            edgeLabel.setVisible(labelVisible && !labelText.isEmpty());
        }
    }
}
