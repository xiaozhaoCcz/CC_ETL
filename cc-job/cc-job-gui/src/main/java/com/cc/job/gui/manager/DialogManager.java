package com.cc.job.gui.manager;

import com.cc.job.gui.model.JobComposeData;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.service.*;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.view.*;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 对话框管理器 - 负责各种对话框的显示和处理
 */
public class DialogManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DialogManager.class);
    
    private final Stage ownerStage;
    private final LogPanel logPanel;
    private TaskExecutionManager taskExecutionManager;
    
    private final JobInfoService jobInfoService;
    private final JobGroupService jobGroupService;
    private final JobPartService jobPartService;
    private final JobGroupSnapshotService jobGroupSnapshotService;
    private final NodeTemplateManager nodeTemplateManager;
    
    private NodeOperationManager nodeOperationManager;

    /** 从任务列表跳转到任务组的回调（主窗口切换任务组） */
    private BiConsumer<Long, String> onGoToTaskGroupCallback;
    
    // 对话框实例缓存，避免重复创建
    private ShowJobListDialog jobListDialog;
    private ShowExecutorListDialog executorListDialog;
    private ShowJobLogListDialog jobLogListDialog;
    private TaskReportDialog taskReportDialog;
    private ShowDatasourceListDialog datasourceListDialog;
    private ShowDataxSyncDialog dataxSyncDialog;
    private ShowDataxGroupSyncDialog dataxGroupSyncDialog;
    
    public DialogManager(Stage ownerStage, LogPanel logPanel) {
        this.ownerStage = ownerStage;
        this.logPanel = logPanel;
        this.jobInfoService = new JobInfoService();
        this.jobGroupService = new JobGroupService();
        this.jobPartService = new JobPartService();
        this.jobGroupSnapshotService = new JobGroupSnapshotService();
        this.nodeTemplateManager = new NodeTemplateManager(msg -> {
            if (logPanel != null) logPanel.appendText(msg + "\n");
        });
    }
    
    /**
     * 设置任务执行管理器（用于获取预测时间）
     */
    public void setTaskExecutionManager(TaskExecutionManager taskExecutionManager) {
        this.taskExecutionManager = taskExecutionManager;
    }
    
    /**
     * 设置节点操作管理器（用于从任务列表添加节点时本地入画布并支持撤销）
     */
    public void setNodeOperationManager(NodeOperationManager nodeOperationManager) {
        this.nodeOperationManager = nodeOperationManager;
    }

    /**
     * 设置从任务列表跳转到任务组的回调（主窗口切换任务组后，任务列表会关闭）
     */
    public void setOnGoToTaskGroupCallback(BiConsumer<Long, String> callback) {
        this.onGoToTaskGroupCallback = callback;
    }
    
    /**
     * 显示新建分区对话框
     */
    public void showNewPartitionDialog(Runnable onSuccess) {
        try {
            NewPartitionDialog dialog = new NewPartitionDialog(ownerStage);
            Optional<String> result = dialog.showAndWait();
            
            result.ifPresent(partitionName -> {
                logger.info("📝 创建新分区: " + partitionName);
                
                new Thread(() -> {
                    try {
                        boolean success = jobPartService.saveJobPart(partitionName);
                        Platform.runLater(() -> {
                            if (success) {
                                logger.info("✓ 分区创建成功: " + partitionName);
                                if (onSuccess != null) onSuccess.run();
                            } else {
                                NotificationToast.showError("✗ 分区创建失败");
                                logger.error("✗ 分区创建失败");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 创建失败: " + e.getMessage());
                        });
                        logger.error("✗ 创建失败: {}", e.getMessage());
                    }
                }).start();
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                NotificationToast.showError("✗ 打开对话框失败: " + e.getMessage());
            });
            logger.error("✗ 打开对话框失败: {}", e.getMessage());
        }
    }
    
    /**
     * 显示编辑分区对话框
     * @param partitionId 分区ID
     * @param partitionName 分区名称
     * @param onSuccess 成功回调
     */
    public void showEditPartitionDialog(Long partitionId, String partitionName, Runnable onSuccess) {
        try {
            NewPartitionDialog dialog = new NewPartitionDialog(ownerStage, partitionId, partitionName);
            Optional<String> result = dialog.showAndWait();
            
            result.ifPresent(newPartitionName -> {
                logger.info("📝 编辑分区: " + partitionName + " -> " + newPartitionName);
                
                new Thread(() -> {
                    try {
                        boolean success = jobPartService.updateJobPart(partitionId, newPartitionName);
                        Platform.runLater(() -> {
                            if (success) {
                                logger.info("✓ 分区更新成功: " + newPartitionName);
                                if (onSuccess != null) onSuccess.run();
                            } else {
                                NotificationToast.showError("✗ 分区更新失败");
                                logger.error("✗ 分区更新失败");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 更新失败: " + e.getMessage());
                        });
                        logger.error("✗ 更新失败: {}", e.getMessage());
                    }
                }).start();
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                NotificationToast.showError("✗ 打开对话框失败: " + e.getMessage());
            });
            logger.error("✗ 打开对话框失败: {}", e.getMessage());
        }
    }
    
    /**
     * 显示新建/编辑任务组对话框
     */
    public void showJobGroupDialog(Long partitionId, String partitionName, JobInfoForm editData, Runnable onSuccess) {
        new Thread(() -> {
            try {
                List<JobGroup> jobGroupList = jobGroupService.getAllJobGroupList();
                
                Platform.runLater(() -> {
                    try {
                        NewJobGroupDialog dialog = new NewJobGroupDialog(ownerStage, partitionId, editData, jobGroupList);
                        Optional<JobInfoForm> result = dialog.showAndWait();
                        
                        result.ifPresent(formData -> {
                            new Thread(() -> {
                                try {
                                    boolean success = editData == null 
                                        ? jobInfoService.saveJobCompose(formData)
                                        : jobInfoService.updateJobCompose(editData.getId(), formData);
                                    
                                    Platform.runLater(() -> {
                                        if (success) {
                                            logger.info(editData == null ? "✓ 任务组创建成功" : "✓ 任务组更新成功");
                                            if (onSuccess != null) onSuccess.run();
                                        } else {
                                            NotificationToast.showError("✗ 操作失败");
                                            logger.error("✗ 操作失败");
                                        }
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        NotificationToast.showError("✗ 保存失败: " + e.getMessage());
                                    });
                                    logger.error("✗ 保存失败: {}", e.getMessage());
                                }
                            }).start();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 打开对话框失败: " + e.getMessage());
                        });
                        logger.error("✗ 打开对话框失败: {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationToast.showError("✗ 加载执行器列表失败: " + e.getMessage());
                });
                logger.error("✗ 加载执行器列表失败: {}", e.getMessage());
            }
        }).start();
    }
    
    /**
     * 显示新建/编辑任务节点对话框
     */
    public void showJobNodeDialog(Long taskGroupId, String taskGroupName, JobInfoForm editData, Runnable onSuccess) {
        new Thread(() -> {
            try {
                List<JobGroup> jobGroupList = jobGroupService.getAllJobGroupList();
                
                Platform.runLater(() -> {
                    try {
                        NewJobNodeDialog dialog = new NewJobNodeDialog(ownerStage, taskGroupId, editData, jobGroupList);
                        Optional<JobInfoForm> result = dialog.showAndWait();
                        
                        result.ifPresent(formData -> {
                            new Thread(() -> {
                                try {
                                    JobNode jobNode = jobInfoService.saveJobNode(formData);
                                    Platform.runLater(() -> {
                                        if (jobNode != null) {
                                            logger.info(editData == null ? "✓ 节点创建成功" : "✓ 节点更新成功");
                                            if (onSuccess != null) onSuccess.run();
                                        } else {
                                            NotificationToast.showError("✗ 操作失败");
                                            logger.error("✗ 操作失败");
                                        }
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        NotificationToast.showError("✗ 保存失败: " + e.getMessage());
                                    });
                                    logger.error("✗ 保存失败: {}", e.getMessage());
                                }
                            }).start();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 打开对话框失败: " + e.getMessage());
                        });
                        logger.error("✗ 打开对话框失败: {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationToast.showError("✗ 加载执行器列表失败: " + e.getMessage());
                });
                logger.error("✗ 加载执行器列表失败: {}", e.getMessage());
            }
        }).start();
    }
    
    /**
     * 显示新建/编辑普通任务对话框
     * 用于导航栏"新增任务"功能，包含调度配置和子任务ID
     */
    public void showJobDialog(JobInfoForm editData, Runnable onSuccess) {
        new Thread(() -> {
            try {
                List<JobGroup> jobGroupList = jobGroupService.getAllJobGroupList();
                
                Platform.runLater(() -> {
                    try {
                        NewJobDialog dialog = new NewJobDialog(ownerStage, editData, jobGroupList);
                        Optional<JobInfoForm> result = dialog.showAndWait();
                        
                        result.ifPresent(formData -> {
                            new Thread(() -> {
                                try {
                                    long jobId = jobInfoService.saveJobInfo(formData);
                                    Platform.runLater(() -> {
                                        if (jobId > 0 && onSuccess != null) {
                                            onSuccess.run();
                                        } else {
                                            NotificationToast.showError("✗ 操作失败");
                                        }
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> {
                                        NotificationToast.showError("✗ 保存失败: " + e.getMessage());
                                    });
                                    logger.error("✗ 保存失败: {}", e.getMessage());
                                }
                            }).start();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            NotificationToast.showError("✗ 打开对话框失败: " + e.getMessage());
                        });
                        logger.error("✗ 打开对话框失败: {}", e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    NotificationToast.showError("✗ 加载执行器列表失败: " + e.getMessage());
                });
                logger.error("✗ 加载执行器列表失败: {}", e.getMessage());
            }
        }).start();
    }
    
    /**
     * 显示保存为节点模板对话框
     * @param node 当前节点
     * @param onSuccess 保存成功回调（可为 null）
     */
    public void showSaveAsTemplateDialog(ProcessNode node, Runnable onSuccess) {
        if (node == null) {
            NotificationToast.showWarning("⚠ 节点无效");
            return;
        }
        SaveNodeTemplateDialog dialog = new SaveNodeTemplateDialog(ownerStage, node.getJobHandlerName());
        Optional<SaveNodeTemplateDialog.Result> resultOpt = dialog.showAndWait();
        resultOpt.ifPresent(result -> {
            new Thread(() -> {
                try {
                    com.cc.job.xo.model.entity.JobNodeTemplate template = nodeTemplateManager.createTemplateFromNode(
                        node,
                        result.getTemplateName(),
                        result.getTemplateCategory(),
                        result.getDescription(),
                        result.isPublic() ? 1 : 0
                    );
                    if (template != null && nodeTemplateManager.saveTemplate(template)) {
                        Platform.runLater(() -> {
                            NotificationToast.showSuccess("✓ 已保存为节点模板: " + result.getTemplateName());
                            if (onSuccess != null) onSuccess.run();
                        });
                    } else {
                        Platform.runLater(() -> NotificationToast.showError("✗ 保存模板失败"));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> NotificationToast.showError("✗ 保存模板失败: " + e.getMessage()));
                    logger.error("保存为节点模板失败", e);
                }
            }).start();
        });
    }

    /**
     * 获取节点模板管理器（供从模板创建节点等使用）
     */
    public NodeTemplateManager getNodeTemplateManager() {
        return nodeTemplateManager;
    }

    /**
     * 显示从模板创建节点对话框
     * @param taskGroupId 当前任务组 ID
     * @param taskGroupName 任务组名称（用于日志）
     * @param positionX 节点放置 X 坐标
     * @param positionY 节点放置 Y 坐标
     * @param onSuccess 创建成功回调（可为 null，通常用于刷新画布）
     */
    public void showCreateNodeFromTemplateDialog(Long taskGroupId, String taskGroupName,
                                                  double positionX, double positionY, Runnable onSuccess) {
        Optional<com.cc.job.xo.model.entity.JobNodeTemplate> templateOpt =
            CreateNodeFromTemplateDialog.showAndSelect(ownerStage, nodeTemplateManager);
        templateOpt.ifPresent(template -> {
            new Thread(() -> {
                try {
                    com.cc.job.xo.model.form.JobInfoForm formData = nodeTemplateManager.createNodeFromTemplate(
                        template, positionX, positionY);
                    if (formData == null) {
                        Platform.runLater(() -> NotificationToast.showError("✗ 从模板创建节点失败"));
                        return;
                    }
                    formData.setParentId(taskGroupId);
                    com.cc.job.xo.model.entity.JobNode jobNode = jobInfoService.saveJobNode(formData);
                    if (jobNode != null) {
                        Platform.runLater(() -> {
                            NotificationToast.showSuccess("✓ 已从模板创建节点: " + template.getTemplateName());
                            if (onSuccess != null) onSuccess.run();
                        });
                    } else {
                        Platform.runLater(() -> NotificationToast.showError("✗ 保存节点失败"));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> NotificationToast.showError("✗ 创建失败: " + e.getMessage()));
                    logger.error("从模板创建节点失败", e);
                }
            }).start();
        });
    }

    /**
     * 显示从任务列表添加节点对话框：选择单任务后加入当前任务组画布
     * @param taskGroupId 当前任务组ID
     * @param taskGroupName 任务组名称（用于提示）
     * @param posX 节点放置 X 坐标
     * @param posY 节点放置 Y 坐标
     * @param onSuccess 添加成功回调（可为 null，通常用于刷新画布）
     */
    public void showAddJobFromListDialog(Long taskGroupId, String taskGroupName,
                                         double posX, double posY, Runnable onSuccess) {
        Optional<com.cc.job.xo.model.vo.JobInfoVO> selectedOpt =
            AddJobFromListDialog.showAndSelect(ownerStage);
        selectedOpt.ifPresent(selected -> {
            if (selected.getId() == null) {
                Platform.runLater(() -> NotificationToast.showError("✗ 未选择有效任务"));
                return;
            }
            new Thread(() -> {
                try {
                    com.cc.job.xo.model.entity.JobNode jobNode = jobInfoService.addExistingJobToCompose(
                        selected.getId(), taskGroupId, posX, posY);
                    if (jobNode != null) {
                        Platform.runLater(() -> {
                            NotificationToast.showSuccess("✓ 已添加任务到画布: " + selected.getJobDesc());
                            if (nodeOperationManager != null) {
                                nodeOperationManager.addExistingJobNodeToCanvas(jobNode, taskGroupId);
                            }
                            if (onSuccess != null) onSuccess.run();
                        });
                    } else {
                        Platform.runLater(() -> NotificationToast.showError("✗ 添加失败"));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> NotificationToast.showError("✗ 添加失败: " + e.getMessage()));
                    logger.error("从任务列表添加节点失败", e);
                }
            }).start();
        });
    }

    /**
     * 保存当前画布为版本（需在主线程调用前准备好 nodesJson、edgesJson）
     * @param jobId 任务组ID
     * @param nodesJson 节点 JSON
     * @param edgesJson 边 JSON
     * @param onSuccess 保存成功回调
     */
    public void showSaveAsVersionDialog(Long jobId, String nodesJson, String edgesJson, Runnable onSuccess) {
        TextInputDialog input = new TextInputDialog("v1");
        input.setTitle("保存为版本");
        input.setHeaderText("输入版本名称（将覆盖同名校验）");
        input.setContentText("版本名称：");
        Optional<String> nameOpt = input.showAndWait();
        nameOpt.ifPresent(versionName -> {
            new Thread(() -> {
                try {
                    Long snapshotId = jobGroupSnapshotService.saveAsVersion(jobId, versionName, nodesJson, edgesJson);
                    Platform.runLater(() -> {
                        NotificationToast.showSuccess("✓ 已保存为版本: " + versionName);
                        if (onSuccess != null) onSuccess.run();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> NotificationToast.showError("✗ 保存版本失败: " + e.getMessage()));
                    logger.error("保存版本失败", e);
                }
            }).start();
        });
    }

    /**
     * 显示版本列表对话框，选择版本后可回滚（将画布恢复为该版本内容）
     * @param jobId 任务组ID
     * @param taskGroupName 任务组名称（提示用）
     * @param onRollback 回滚时回调，传入解析后的 JobComposeData，由调用方加载到画布并可选保存
     */
    public void showVersionListDialog(Long jobId, String taskGroupName, Consumer<JobComposeData> onRollback) {
        new Thread(() -> {
            try {
                List<com.cc.job.xo.model.entity.JobGroupSnapshot> versions = jobGroupSnapshotService.listVersions(jobId, 50);
                Platform.runLater(() -> {
                    Optional<Long> snapshotIdOpt = VersionListDialog.showAndSelectRollback(ownerStage, versions);
                    snapshotIdOpt.ifPresent(snapshotId -> {
                        new Thread(() -> {
                            try {
                                com.cc.job.xo.model.entity.JobGroupSnapshot snapshot = jobGroupSnapshotService.getSnapshot(snapshotId);
                                if (snapshot != null && snapshot.getNodesJson() != null && snapshot.getEdgesJson() != null) {
                                    JobComposeData composeData = jobPartService.parseComposeFromGuiSnapshot(
                                        snapshot.getNodesJson(), snapshot.getEdgesJson());
                                    Platform.runLater(() -> {
                                        if (onRollback != null) onRollback.accept(composeData);
                                    });
                                } else {
                                    Platform.runLater(() -> NotificationToast.showError("✗ 快照内容无效"));
                                }
                            } catch (Exception e) {
                                Platform.runLater(() -> NotificationToast.showError("✗ 获取快照失败: " + e.getMessage()));
                                logger.error("获取快照失败", e);
                            }
                        }).start();
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> NotificationToast.showError("✗ 获取版本列表失败: " + e.getMessage()));
                logger.error("获取版本列表失败", e);
            }
        }).start();
    }

    /**
     * 显示任务列表对话框
     */
    public void showJobListDialog() {
        // 如果对话框已经打开，直接将其置于前台
        if (jobListDialog != null && jobListDialog.isShowing()) {
            jobListDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        
        // 创建新对话框（传入跳转回调，任务组行显示「跳转」）
        jobListDialog = new ShowJobListDialog(ownerStage, onGoToTaskGroupCallback);
        
        // 监听对话框关闭事件，清空缓存
        jobListDialog.setOnHidden(event -> jobListDialog = null);
        
        jobListDialog.show();
    }
    
    /**
     * 显示执行器列表对话框
     */
    public void showJobGroupListDialog() {
        // 如果对话框已经打开，直接将其置于前台
        if (executorListDialog != null && executorListDialog.isShowing()) {
            executorListDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        
        // 创建新对话框
        executorListDialog = new ShowExecutorListDialog(ownerStage);
        
        // 监听对话框关闭事件，清空缓存
        executorListDialog.setOnHidden(event -> executorListDialog = null);
        
        executorListDialog.show();
    }
    
    /**
     * 显示任务日志列表对话框
     */
    public void showJobLogListDialog() {
        // 如果对话框已经打开，直接将其置于前台
        if (jobLogListDialog != null && jobLogListDialog.isShowing()) {
            jobLogListDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        
        // 创建新对话框
        jobLogListDialog = new ShowJobLogListDialog(ownerStage);
        
        // 监听对话框关闭事件，清空缓存
        jobLogListDialog.setOnHidden(event -> jobLogListDialog = null);
        
        jobLogListDialog.show();
    }

    /**
     * 显示任务报表对话框
     */
    public void showTaskReportDialog() {
        if (taskReportDialog != null && taskReportDialog.isShowing()) {
            taskReportDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        taskReportDialog = new TaskReportDialog(ownerStage);
        taskReportDialog.setOnHidden(event -> taskReportDialog = null);
        taskReportDialog.show();
    }

    /**
     * 显示统计大屏（全屏/大窗口）
     */
    public void showDashboardBigScreen() {
        DashboardBigScreenStage stage = new DashboardBigScreenStage(ownerStage);
        stage.show();
    }
    
    /**
     * 显示数据源管理对话框
     */
    public void showDatasourceListDialog() {
        // 如果对话框已经打开，直接将其置于前台
        if (datasourceListDialog != null && datasourceListDialog.isShowing()) {
            datasourceListDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        
        // 创建新对话框
        datasourceListDialog = new ShowDatasourceListDialog(ownerStage);
        
        // 监听对话框关闭事件，清空缓存
        datasourceListDialog.setOnHidden(event -> datasourceListDialog = null);
        
        datasourceListDialog.show();
    }
    
    /**
     * 显示数据源同步对话框
     */
    public void showDataxSyncDialog() {
        // 如果对话框已经打开，直接将其置于前台
        if (dataxSyncDialog != null && dataxSyncDialog.isShowing()) {
            dataxSyncDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        
        // 创建新对话框
        dataxSyncDialog = new ShowDataxSyncDialog(ownerStage);
        
        // 监听对话框关闭事件，清空缓存
        dataxSyncDialog.setOnHidden(event -> dataxSyncDialog = null);
        
        dataxSyncDialog.show();
    }
    
    /**
     * 显示多数据源同步对话框
     */
    public void showDataxGroupSyncDialog() {
        // 如果对话框已经打开，直接将其置于前台
        if (dataxGroupSyncDialog != null && dataxGroupSyncDialog.isShowing()) {
            dataxGroupSyncDialog.getDialogPane().getScene().getWindow().requestFocus();
            return;
        }
        
        // 创建新对话框
        dataxGroupSyncDialog = new ShowDataxGroupSyncDialog(ownerStage);
        
        // 监听对话框关闭事件，清空缓存
        dataxGroupSyncDialog.setOnHidden(event -> dataxGroupSyncDialog = null);
        
        dataxGroupSyncDialog.show();
    }
    
    /**
     * 显示节点详情对话框
     * @param jobId 任务ID
     * @param taskGroupId 任务组ID
     * @param nodeName 节点名称
     * @param nodeId 节点ID
     */
    public void showNodeDetailsDialog(Long jobId, Long taskGroupId, String nodeName, String nodeId) {
        NodeDetailsDialog dialog = new NodeDetailsDialog(ownerStage, jobId, taskGroupId, nodeName, nodeId, taskExecutionManager);
        dialog.show();
    }

    /**
     * 显示待办审批对话框
     */
    public void showApprovalPendingDialog() {
        ApprovalPendingDialog dialog = new ApprovalPendingDialog(ownerStage);
        dialog.showAndWait();
    }
}

