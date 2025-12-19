package com.cc.job.gui.manager;

import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.view.LogPanel;
import com.cc.job.gui.view.NodeCanvas;
import javafx.application.Platform;

/**
 * 节点回调配置器 - 为节点配置编辑、复制、查看详情等回调
 */
public class NodeCallbackConfigurator {
    
    private final NodeOperationManager nodeOperationManager;
    private final NodeCanvas canvas;
    private final LogPanel logPanel;
    private DialogManager dialogManager;
    
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
                Platform.runLater(() -> logPanel.warn("⚠ 该节点未绑定后端任务，无法编辑"));
                return;
            }
            nodeOperationManager.editNode(jobId, node, currentTaskGroupId);
        });
        
        // 复制回调 - 动态获取 jobId
        node.setOnCopy(() -> {
            Long jobId = node.getJobId();
            if (jobId == null) {
                Platform.runLater(() -> logPanel.warn("⚠ 该节点未绑定后端任务，无法复制"));
                return;
            }
            nodeOperationManager.copyNodeToClipboard(node, currentTaskGroupId);
        });
        
        // 查看详情回调 - 动态获取 jobId
        node.setOnShowDetails(() -> {
            Long jobId = node.getJobId();
            if (jobId == null) {
                Platform.runLater(() -> logPanel.warn("⚠ 该节点未绑定后端任务，无法查看详情"));
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
                Platform.runLater(() -> logPanel.warn("⚠ 对话框管理器未设置，无法显示详情"));
            }
        });
        
        // 禁用/启用回调 - nodeJobId 已经是参数传入的，不需要修改
        node.setOnDisable(new ProcessNode.DisableNodeCallback() {
            @Override
            public void onDisableNode(Long nodeJobId, boolean isDisabled) {
                if (nodeJobId == null) {
                    logPanel.warn("⚠ 该节点未绑定后端任务，无法禁用/启用");
                    Platform.runLater(() -> node.restoreEnabledState(!isDisabled));
                    return;
                }
                
                String action = isDisabled ? "禁用" : "启用";
                logPanel.info("正在" + action + "节点: " + node.getJobHandlerName());
                
                new Thread(() -> {
                    try {
                        JobInfoService jobInfoService = new JobInfoService();
                        Integer isPause = isDisabled ? 1 : 0;
                        boolean success = jobInfoService.pauseJob(nodeJobId, isPause);
                        
                        Platform.runLater(() -> {
                            if (success) {
                                logPanel.success("✓ 节点已" + action);
                            } else {
                                logPanel.error("✗ 节点" + action + "失败");
                                node.restoreEnabledState(!isDisabled);
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            logPanel.error("✗ 节点" + action + "失败: " + e.getMessage());
                            node.restoreEnabledState(!isDisabled);
                        });
                    }
                }).start();
            }
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

