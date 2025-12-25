package com.cc.job.gui.manager;

import com.cc.job.gui.service.*;
import com.cc.job.gui.view.*;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.form.JobInfoForm;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

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
    
    // 对话框实例缓存，避免重复创建
    private ShowJobListDialog jobListDialog;
    private ShowExecutorListDialog executorListDialog;
    private ShowJobLogListDialog jobLogListDialog;
    private ShowDatasourceListDialog datasourceListDialog;
    private ShowDataxSyncDialog dataxSyncDialog;
    private ShowDataxGroupSyncDialog dataxGroupSyncDialog;
    
    public DialogManager(Stage ownerStage, LogPanel logPanel) {
        this.ownerStage = ownerStage;
        this.logPanel = logPanel;
        this.jobInfoService = new JobInfoService();
        this.jobGroupService = new JobGroupService();
        this.jobPartService = new JobPartService();
    }
    
    /**
     * 设置任务执行管理器（用于获取预测时间）
     */
    public void setTaskExecutionManager(TaskExecutionManager taskExecutionManager) {
        this.taskExecutionManager = taskExecutionManager;
    }
    
    /**
     * 显示新建分区对话框
     */
    public void showNewPartitionDialog(Runnable onSuccess) {
        try {
            NewPartitionDialog dialog = new NewPartitionDialog(ownerStage);
            Optional<String> result = dialog.showAndWait();
            
            result.ifPresent(partitionName -> {
                logPanel.info("📝 创建新分区: " + partitionName);
                
                new Thread(() -> {
                    try {
                        boolean success = jobPartService.saveJobPart(partitionName);
                        Platform.runLater(() -> {
                            if (success) {
                                logPanel.success("✓ 分区创建成功: " + partitionName);
                                if (onSuccess != null) onSuccess.run();
                            } else {
                                logPanel.error("✗ 分区创建失败");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> logPanel.error("✗ 创建失败: " + e.getMessage()));
                    }
                }).start();
            });
        } catch (Exception e) {
            logPanel.error("✗ 打开对话框失败: " + e.getMessage());
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
                                            logPanel.success(editData == null ? "✓ 任务组创建成功" : "✓ 任务组更新成功");
                                            if (onSuccess != null) onSuccess.run();
                                        } else {
                                            logPanel.error("✗ 操作失败");
                                        }
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> logPanel.error("✗ 保存失败: " + e.getMessage()));
                                }
                            }).start();
                        });
                    } catch (Exception e) {
                        logPanel.error("✗ 打开对话框失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 加载执行器列表失败: " + e.getMessage()));
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
                                            logPanel.success(editData == null ? "✓ 节点创建成功" : "✓ 节点更新成功");
                                            if (onSuccess != null) onSuccess.run();
                                        } else {
                                            logPanel.error("✗ 操作失败");
                                        }
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> logPanel.error("✗ 保存失败: " + e.getMessage()));
                                }
                            }).start();
                        });
                    } catch (Exception e) {
                        logPanel.error("✗ 打开对话框失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 加载执行器列表失败: " + e.getMessage()));
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
                                        if (jobId > 0) {
                                            logPanel.success(editData == null ? "✓ 任务创建成功" : "✓ 任务更新成功");
                                            if (onSuccess != null) onSuccess.run();
                                        } else {
                                            logPanel.error("✗ 操作失败");
                                        }
                                    });
                                } catch (Exception e) {
                                    Platform.runLater(() -> logPanel.error("✗ 保存失败: " + e.getMessage()));
                                }
                            }).start();
                        });
                    } catch (Exception e) {
                        logPanel.error("✗ 打开对话框失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 加载执行器列表失败: " + e.getMessage()));
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
        
        // 创建新对话框
        jobListDialog = new ShowJobListDialog(ownerStage);
        
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
}

