package com.cc.job.gui.manager;

import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.view.LogPanel;
import com.cc.job.gui.view.NodeCanvas;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点回调配置器 - 为节点配置编辑、复制、查看详情等回调
 */
public class NodeCallbackConfigurator {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeCallbackConfigurator.class);
    
    private final NodeOperationManager nodeOperationManager;
    private final NodeCanvas canvas;
    private final LogPanel logPanel;
    private DialogManager dialogManager;
    private Runnable onColorChangedCallback; // 颜色变更时的保存回调
    private javafx.stage.Stage ownerStage; // 主窗口Stage
    
    public NodeCallbackConfigurator(NodeOperationManager nodeOperationManager, 
                                    NodeCanvas canvas, 
                                    LogPanel logPanel) {
        this.nodeOperationManager = nodeOperationManager;
        this.canvas = canvas;
        this.logPanel = logPanel;
    }
    
    /**
     * 设置对话框管理器
     */
    public void setDialogManager(DialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }
    
    /**
     * 设置主窗口Stage
     */
    public void setOwnerStage(javafx.stage.Stage ownerStage) {
        this.ownerStage = ownerStage;
    }
    
    /**
     * 设置颜色变更回调(用于保存颜色到数据库)
     */
    public void setOnColorChangedCallback(Runnable callback) {
        this.onColorChangedCallback = callback;
    }
    
    /**
     * 为节点配置所有回调
     */
    public void configureNodeCallbacks(ProcessNode node, Long currentTaskGroupId) {
        if (node == null) {
            return;
        }
        
        // 编辑回调 - 动态获取 jobId，避免闭包捕获问题
        node.setOnEdit(() -> {
            Long jobId = node.getJobId();
            if (jobId == null) {
                Platform.runLater(() -> {
                    NotificationToast.showWarning("⚠ 该节点未绑定后端任务，无法编辑");
                });
                logger.warn("⚠ 该节点未绑定后端任务，无法编辑");
                return;
            }
            nodeOperationManager.editNode(jobId, node, currentTaskGroupId);
        });
        
        // 复制回调 - 动态获取 jobId
        // 右键菜单复制：直接创建新节点（复制+粘贴一步完成）
        node.setOnCopy(() -> {
            Long jobId = node.getJobId();
            if (jobId == null) {
                Platform.runLater(() -> {
                    NotificationToast.showWarning("⚠ 该节点未绑定后端任务，无法复制");
                });
                logger.warn("⚠ 该节点未绑定后端任务，无法复制");
                return;
            }
            nodeOperationManager.duplicateNode(node, currentTaskGroupId);
        });
        
        // 查看详情回调 - 动态获取 jobId
        node.setOnShowDetails(() -> {
            Long jobId = node.getJobId();
            if (jobId == null) {
                Platform.runLater(() -> {
                    NotificationToast.showWarning("⚠ 该节点未绑定后端任务，无法查看详情");
                });
                logger.warn("⚠ 该节点未绑定后端任务，无法查看详情");
                return;
            }
            // 显示节点详情对话框
            if (dialogManager != null) {
                Platform.runLater(() -> {
                    dialogManager.showNodeDetailsDialog(
                        jobId, 
                        currentTaskGroupId, 
                        node.getJobHandlerName(), 
                        node.getNodeId()
                    );
                });
            } else {
                Platform.runLater(() -> {
                    NotificationToast.showWarning("⚠ 对话框管理器未设置，无法显示详情");
                });
                logger.warn("⚠ 对话框管理器未设置，无法显示详情");
            }
        });
        
        // 查看依赖回调
        node.setOnShowDependencies(() -> {
            Platform.runLater(() -> {
                javafx.stage.Stage stage = ownerStage;
                if (stage == null && canvas.getScene() != null) {
                    stage = (javafx.stage.Stage) canvas.getScene().getWindow();
                }
                if (stage == null) {
                    NotificationToast.showWarning("⚠ 无法获取主窗口，无法显示依赖关系面板");
                    logger.warn("⚠ 无法获取主窗口，无法显示依赖关系面板");
                    return;
                }
                com.cc.job.gui.view.DependencyViewPanel dependencyPanel = new com.cc.job.gui.view.DependencyViewPanel(
                    stage,
                    canvas.getNodes(),
                    canvas.getConnections(),
                    node,
                    selectedNode -> {
                        // 定位到选中的节点
                        canvas.locateNode(selectedNode);
                        logger.info("✓ 已定位到节点: " + selectedNode.getJobHandlerName());
                    }
                );
                dependencyPanel.showAndWait();
            });
        });
        
        // 禁用/启用回调 - nodeJobId 已经是参数传入的，不需要修改
        node.setOnDisable(new ProcessNode.DisableNodeCallback() {
            @Override
            public void onDisableNode(Long nodeJobId, boolean isDisabled) {
                if (nodeJobId == null) {
                    Platform.runLater(() -> {
                        NotificationToast.showWarning("⚠ 该节点未绑定后端任务，无法禁用/启用");
                    });
                    logger.warn("⚠ 该节点未绑定后端任务，无法禁用/启用");
                    Platform.runLater(() -> node.restoreEnabledState(!isDisabled));
                    return;
                }
                
                String action = isDisabled ? "禁用" : "启用";
                logger.info("正在" + action + "节点: " + node.getJobHandlerName());
                
                new Thread(() -> {
                    try {
                        JobInfoService jobInfoService = new JobInfoService();
                        Integer isPause = isDisabled ? 1 : 0;
                        boolean success = jobInfoService.pauseJob(nodeJobId, isPause);
                        
                        Platform.runLater(() -> {
                            if (success) {
                                logger.info("✓ 节点已" + action);
                            } else {
                                NotificationToast.showError("✗ 节点" + action + "失败");
                                logger.error("✗ 节点" + action + "失败");
                                node.restoreEnabledState(!isDisabled);
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 节点" + action + "失败: " + e.getMessage());
                        });
                        logger.error("✗ 节点" + action + "失败: {}", e.getMessage());
                        Platform.runLater(() -> node.restoreEnabledState(!isDisabled));
                    }
                }).start();
            }
        });
        
        // 颜色变更回调 - 保存颜色到数据库
        node.setOnColorChanged(() -> {
            if (onColorChangedCallback != null) {
                onColorChangedCallback.run();
            }
        });
        
        // 样式设置回调
        node.setOnChangeStyle(() -> {
            Platform.runLater(() -> {
                javafx.stage.Stage stage = ownerStage;
                if (stage == null && canvas.getScene() != null) {
                    stage = (javafx.stage.Stage) canvas.getScene().getWindow();
                }
                if (stage == null) {
                    NotificationToast.showWarning("⚠ 无法获取主窗口，无法显示样式设置对话框");
                    logger.warn("⚠ 无法获取主窗口，无法显示样式设置对话框");
                    return;
                }
                com.cc.job.gui.view.NodeStyleDialog dialog = new com.cc.job.gui.view.NodeStyleDialog(stage, node);
                dialog.showAndWait().ifPresent(result -> {
                    // 对话框已经应用了样式，这里只需要触发保存回调
                    if (onColorChangedCallback != null) {
                        onColorChangedCallback.run();
                    }
                    logger.info("✓ 节点样式已更新");
                });
            });
        });
        
        // 编辑标签回调
        node.setOnEditTags(() -> {
            Platform.runLater(() -> {
                javafx.stage.Stage stage = ownerStage;
                if (stage == null && canvas.getScene() != null) {
                    stage = (javafx.stage.Stage) canvas.getScene().getWindow();
                }
                if (stage == null) {
                    NotificationToast.showWarning("⚠ 无法获取主窗口，无法显示标签编辑对话框");
                    logger.warn("⚠ 无法获取主窗口，无法显示标签编辑对话框");
                    return;
                }
                com.cc.job.gui.view.NodeLabelDialog dialog = new com.cc.job.gui.view.NodeLabelDialog(stage, node);
                dialog.showAndWait().ifPresent(tags -> {
                    if (tags != null) {
                        node.setTags(tags);
                        // 保存到数据库
                        if (onColorChangedCallback != null) {
                            onColorChangedCallback.run();
                        }
                        logger.info("✓ 节点标签已更新");
                    }
                });
            });
        });
        
        // 编辑备注回调
        node.setOnEditRemark(() -> {
            Platform.runLater(() -> {
                javafx.stage.Stage stage = ownerStage;
                if (stage == null && canvas.getScene() != null) {
                    stage = (javafx.stage.Stage) canvas.getScene().getWindow();
                }
                if (stage == null) {
                    NotificationToast.showWarning("⚠ 无法获取主窗口，无法显示备注编辑对话框");
                    logger.warn("⚠ 无法获取主窗口，无法显示备注编辑对话框");
                    return;
                }
                com.cc.job.gui.view.NodeRemarkDialog dialog = new com.cc.job.gui.view.NodeRemarkDialog(stage, node);
                dialog.showAndWait().ifPresent(remark -> {
                    if (remark != null) {
                        node.setRemark(remark);
                        // 保存到数据库
                        if (onColorChangedCallback != null) {
                            onColorChangedCallback.run();
                        }
                        logger.info("✓ 节点备注已更新");
                    }
                });
            });
        });
    }
    
    /**
     * 为所有节点配置回调
     */
    public void configureAllNodeCallbacks(Long currentTaskGroupId) {
        for (ProcessNode node : canvas.getNodes()) {
            configureNodeCallbacks(node, currentTaskGroupId);
        }
    }
}

