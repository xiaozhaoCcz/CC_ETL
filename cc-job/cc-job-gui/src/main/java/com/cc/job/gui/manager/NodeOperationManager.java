package com.cc.job.gui.manager;

import com.cc.job.gui.model.*;
import com.cc.job.gui.service.*;
import com.cc.job.gui.util.*;
import com.cc.job.gui.view.*;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.entity.JobNode;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * 节点操作管理器 - 负责节点的复制、粘贴、编辑等操作
 */
public class NodeOperationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeOperationManager.class);
    
    private final NodeCanvas canvas;
    private final LogPanel logPanel;
    private final TaskTreeView treeView;
    private final Stage ownerStage;
    
    private final JobInfoService jobInfoService;
    private final JobGroupService jobGroupService;
    
    private JobInfoForm copiedNodeForm = null;
    private CopiedNodesData copiedNodesData = null;
    
    // 保存复制时的原节点位置（用于计算粘贴位置）
    private Double copiedNodeX = null;
    private Double copiedNodeY = null;
    
    // 节点回调配置器（用于配置编辑、复制等业务逻辑回调）
    private NodeCallbackConfigurator nodeCallbackConfigurator;
    
    public NodeOperationManager(NodeCanvas canvas, LogPanel logPanel, TaskTreeView treeView, Stage ownerStage) {
        this.canvas = canvas;
        this.logPanel = logPanel;
        this.treeView = treeView;
        this.ownerStage = ownerStage;
        this.jobInfoService = new JobInfoService();
        this.jobGroupService = new JobGroupService();
    }
    
    /**
     * 设置节点回调配置器
     */
    public void setNodeCallbackConfigurator(NodeCallbackConfigurator configurator) {
        this.nodeCallbackConfigurator = configurator;
    }
    
    /**
     * 将「从任务列表添加」得到的 JobNode 在本地加入画布并入栈，支持撤销/重做。
     * 在后台线程拉取 JobInfoForm 后于 JavaFX 线程执行 addNode(recordHistory=true)。
     */
    public void addExistingJobNodeToCanvas(JobNode jobNode, Long taskGroupId) {
        if (jobNode == null || taskGroupId == null) {
            Platform.runLater(() -> logPanel.warn("⚠ 节点或任务组ID为空"));
            return;
        }
        new Thread(() -> {
            try {
                JobInfoForm formData = jobInfoService.getJobNodeFormData(jobNode.getJobId());
                if (formData == null) {
                    Platform.runLater(() -> logPanel.error("✗ 获取任务表单失败"));
                    return;
                }
                formData.setNodeId(String.valueOf(jobNode.getId()));
                formData.setNodePositionX(jobNode.getNodePositionX());
                formData.setNodePositionY(jobNode.getNodePositionY());
                formData.setParentId(taskGroupId);
                JobNode nodeRef = jobNode;
                JobInfoForm formRef = formData;
                Platform.runLater(() -> addNodeToCanvas(nodeRef, formRef, true));
            } catch (Exception e) {
                logger.error("从任务列表添加节点到画布失败", e);
                Platform.runLater(() -> logPanel.error("✗ 添加节点失败: " + e.getMessage()));
            }
        }, "add-existing-job-to-canvas").start();
    }
    
    /**
     * 直接复制并创建新节点（右键菜单使用）- 复制+粘贴一步完成
     */
    public void duplicateNode(ProcessNode sourceNode, Long currentTaskGroupId) {
        if (sourceNode == null || sourceNode.getJobId() == null) {
            Platform.runLater(() -> logPanel.warn("⚠ 节点无效或未绑定任务，无法复制"));
            return;
        }
        
        if (currentTaskGroupId == null) {
            Platform.runLater(() -> logPanel.warn("⚠ 当前任务组ID无效，无法复制节点"));
            return;
        }
        
        Platform.runLater(() -> logPanel.info("📋 正在复制节点: " + sourceNode.getJobHandlerName()));
        
        new Thread(() -> {
            try {
                JobInfoForm originalForm = jobInfoService.getJobNodeFormData(sourceNode.getJobId());
                if (originalForm == null) {
                    Platform.runLater(() -> logPanel.error("✗ 获取节点数据失败"));
                    return;
                }
                
                // 创建新节点表单
                JobInfoForm duplicateForm = deepCopyJobInfoForm(originalForm);
                duplicateForm.setId(null);
                duplicateForm.setParentId(currentTaskGroupId);
                duplicateForm.setJobDesc(generateCopyName(originalForm.getJobDesc()));
                
                // 基于原节点位置计算新位置（偏移80像素）
                double originalX = sourceNode.getLayoutX();
                double originalY = sourceNode.getLayoutY();
                double[] newPosition = calculateDuplicatePosition(originalX, originalY);
                duplicateForm.setNodePositionX(newPosition[0]);
                duplicateForm.setNodePositionY(newPosition[1]);
                duplicateForm.setGlueUpdateTime(null);
                
                // 保存新节点
                JobNode newJobNode = jobInfoService.saveJobNode(duplicateForm);
                if (newJobNode != null) {
                    Platform.runLater(() -> {
                        ProcessNode newNode = addNodeToCanvas(newJobNode, duplicateForm);
                        if (newNode != null) {
                            // 选中新创建的节点
                            canvas.selectNodes(Collections.singleton(newNode));
                            // 定位到新节点
                            canvas.locateNode(newNode);
                            logPanel.success("✓ 节点复制成功");
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 复制失败: " + e.getMessage()));
                logger.error("复制节点失败", e);
            }
        }).start();
    }
    
    /**
     * 复制节点到剪贴板 - 只保存到剪贴板，不创建节点
     */
    public void copyNodeToClipboard(ProcessNode sourceNode, Long currentTaskGroupId) {
        if (sourceNode == null || sourceNode.getJobId() == null) {
            Platform.runLater(() -> logPanel.warn("⚠ 节点无效或未绑定任务，无法复制"));
            return;
        }
        
        if (currentTaskGroupId == null) {
            Platform.runLater(() -> logPanel.warn("⚠ 当前任务组ID无效，无法复制节点"));
            return;
        }
        
        Platform.runLater(() -> logPanel.info("📋 正在复制节点: " + sourceNode.getJobHandlerName()));
        
        new Thread(() -> {
            try {
                JobInfoForm originalForm = jobInfoService.getJobNodeFormData(sourceNode.getJobId());
                if (originalForm == null) {
                    Platform.runLater(() -> logPanel.error("✗ 获取节点数据失败"));
                    return;
                }
                
                // 只保存到剪贴板，不创建节点
                copiedNodeForm = deepCopyJobInfoForm(originalForm);
                copiedNodesData = null; // 清空多个节点的数据
                
                // 保存原节点位置，用于计算粘贴位置
                copiedNodeX = sourceNode.getLayoutX();
                copiedNodeY = sourceNode.getLayoutY();
                
                Platform.runLater(() -> {
                    logPanel.success("✓ 节点已复制到剪贴板，按 Ctrl+V 粘贴");
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 复制失败: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 复制多个节点到剪贴板
     */
    public void copyNodesToClipboard(Set<ProcessNode> sourceNodes, Long currentTaskGroupId) {
        if (sourceNodes == null || sourceNodes.isEmpty()) {
            logPanel.warn("⚠ 没有选中的节点");
            return;
        }
        
        logPanel.info("📋 正在复制 " + sourceNodes.size() + " 个节点...");
        
        new Thread(() -> {
            try {
                CopiedNodesData data = new CopiedNodesData();
                
                // 复制节点数据
                for (ProcessNode node : sourceNodes) {
                    if (node == null || node.getJobId() == null) continue;
                    
                    JobInfoForm originalForm = jobInfoService.getJobNodeFormData(node.getJobId());
                    if (originalForm == null) continue;
                    
                    JobInfoForm copyForm = deepCopyJobInfoForm(originalForm);
                    CopiedNodesData.NodeFormData nodeData = new CopiedNodesData.NodeFormData();
                    nodeData.form = copyForm;
                    nodeData.originalJobId = node.getJobId();
                    nodeData.originalNodeId = node.getNodeId();
                    
                    // 保存节点的实际布局位置
                    double nodeX = node.getLayoutX();
                    double nodeY = node.getLayoutY();
                    nodeData.originalX = nodeX;
                    nodeData.originalY = nodeY;
                    
                    data.nodeForms.add(nodeData);
                    data.minX = Math.min(data.minX, nodeX);
                    data.minY = Math.min(data.minY, nodeY);
                }
                
                // 复制连接关系
                for (NodeConnection conn : canvas.getConnections()) {
                    if (conn.getSourceOwner() instanceof ProcessNode && conn.getTargetOwner() instanceof ProcessNode) {
                        ProcessNode src = (ProcessNode) conn.getSourceOwner();
                        ProcessNode tgt = (ProcessNode) conn.getTargetOwner();
                        if (sourceNodes.contains(src) && sourceNodes.contains(tgt)) {
                            CopiedNodesData.ConnectionInfo connInfo = new CopiedNodesData.ConnectionInfo();
                            connInfo.sourceJobId = src.getJobId();
                            connInfo.targetJobId = tgt.getJobId();
                            data.connections.add(connInfo);
                        }
                    }
                }
                
                copiedNodesData = data;
                copiedNodeForm = null;
                
                Platform.runLater(() -> {
                    logPanel.success("✓ " + data.nodeForms.size() + " 个节点已复制");
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 复制失败: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 粘贴节点
     */
    public void pasteNodes(Long currentTaskGroupId) {
        if (copiedNodesData != null && !copiedNodesData.nodeForms.isEmpty()) {
            pasteMultipleNodes(currentTaskGroupId);
        } else if (copiedNodeForm != null) {
            pasteSingleNode(currentTaskGroupId);
        } else {
            logPanel.warn("⚠ 没有可粘贴的节点");
        }
    }
    
    private void pasteSingleNode(Long currentTaskGroupId) {
        logPanel.info("📋 正在粘贴节点...");
        
        new Thread(() -> {
            try {
                JobInfoForm pasteForm = deepCopyJobInfoForm(copiedNodeForm);
                pasteForm.setId(null);
                pasteForm.setParentId(currentTaskGroupId);
                pasteForm.setJobDesc(generateCopyName(copiedNodeForm.getJobDesc()));
                
                double[] position = calculatePastePosition();
                pasteForm.setNodePositionX(position[0]);
                pasteForm.setNodePositionY(position[1]);
                pasteForm.setGlueUpdateTime(null);
                
                JobNode newJobNode = jobInfoService.saveJobNode(pasteForm);
                if (newJobNode != null) {
                    Platform.runLater(() -> {
                        ProcessNode newNode = addNodeToCanvas(newJobNode, pasteForm, false);
                        if (newNode != null) {
                            canvas.notifyPasteCompleted(Collections.singletonList(newNode), Collections.emptyList());
                        }
                        logPanel.success("✓ 节点粘贴成功");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 粘贴失败: " + e.getMessage()));
            }
        }).start();
    }
    
    private void pasteMultipleNodes(Long currentTaskGroupId) {
        logPanel.info("📋 正在粘贴 " + copiedNodesData.nodeForms.size() + " 个节点...");
        
        new Thread(() -> {
            try {
                double[] pastePosition = calculatePastePosition();
                double offsetX = pastePosition[0] - copiedNodesData.minX;
                double offsetY = pastePosition[1] - copiedNodesData.minY;
                
                List<ProcessNode> newNodes = new ArrayList<>();
                Map<Long, ProcessNode> oldJobIdToNewNode = new HashMap<>();
                
                // 使用 CountDownLatch 等待所有节点创建完成
                CountDownLatch latch = new CountDownLatch(copiedNodesData.nodeForms.size());
                List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
                
                // 创建节点
                for (CopiedNodesData.NodeFormData nodeData : copiedNodesData.nodeForms) {
                    JobInfoForm pasteForm = deepCopyJobInfoForm(nodeData.form);
                    pasteForm.setId(null);
                    pasteForm.setParentId(currentTaskGroupId);
                    pasteForm.setJobDesc(generateCopyName(nodeData.form.getJobDesc()));
                    
                    // 使用保存的实际布局位置，而不是表单中的位置
                    double originalX = nodeData.originalX;
                    double originalY = nodeData.originalY;
                    pasteForm.setNodePositionX(originalX + offsetX);
                    pasteForm.setNodePositionY(originalY + offsetY);
                    pasteForm.setGlueUpdateTime(null);
                    
                    JobNode newJobNode = jobInfoService.saveJobNode(pasteForm);
                    if (newJobNode != null) {
                        Long originalJobId = nodeData.originalJobId;
                        Platform.runLater(() -> {
                            try {
                                ProcessNode newNode = addNodeToCanvas(newJobNode, pasteForm, false);
                                if (newNode != null) {
                                    synchronized (newNodes) {
                                        newNodes.add(newNode);
                                        oldJobIdToNewNode.put(originalJobId, newNode);
                                    }
                                }
                            } catch (Exception e) {
                                errors.add(e);
                                logger.error("创建节点失败", e);
                            } finally {
                                latch.countDown();
                            }
                        });
                    } else {
                        latch.countDown();
                    }
                }
                
                // 等待所有节点创建完成
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> logPanel.error("✗ 粘贴被中断: " + e.getMessage()));
                    return;
                }
                
                // 检查是否有错误
                if (!errors.isEmpty()) {
                    Platform.runLater(() -> logPanel.error("✗ 部分节点创建失败"));
                }
                
                // 恢复连接（不记录历史，最后统一 notifyPasteCompleted 入栈一步撤销）
                Platform.runLater(() -> {
                    List<NodeConnection> newConnections = new ArrayList<>();
                    for (CopiedNodesData.ConnectionInfo connInfo : copiedNodesData.connections) {
                        ProcessNode src = oldJobIdToNewNode.get(connInfo.sourceJobId);
                        ProcessNode tgt = oldJobIdToNewNode.get(connInfo.targetJobId);
                        if (src != null && tgt != null) {
                            NodeConnection conn = canvas.addConnection(
                                src, src.getRightConnector(), tgt, tgt.getLeftConnector(), false);
                            if (conn != null) {
                                newConnections.add(conn);
                            }
                        }
                    }
                    canvas.notifyPasteCompleted(newNodes, newConnections);
                    canvas.selectNodes(newNodes);
                    logPanel.success("✓ " + newNodes.size() + " 个节点粘贴成功");
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 粘贴失败: " + e.getMessage()));
                logger.error("粘贴多个节点失败", e);
            }
        }).start();
    }
    
    /**
     * 编辑节点
     */
    public void editNode(Long jobId, ProcessNode node, Long taskGroupId) {
        if (jobId == null) {
            logPanel.error("✗ jobId 为 null，无法编辑");
            return;
        }
        
        new Thread(() -> {
            try {
                JobInfoForm formData = jobInfoService.getJobNodeFormData(jobId);
                List<JobGroup> jobGroupList = jobGroupService.getAllJobGroupList();
                
                Platform.runLater(() -> {
                    NewJobNodeDialog dialog = new NewJobNodeDialog(ownerStage, taskGroupId, formData, jobGroupList);
                    dialog.showAndWait().ifPresent(updatedFormData -> {
                        new Thread(() -> {
                            try {
                                boolean success = jobInfoService.updateJobNode(jobId, updatedFormData);
                                Platform.runLater(() -> {
                                    if (success) {
                                        logPanel.success("✓ 节点更新成功");
                                        if (node != null) {
                                            node.updateNodeInfo(updatedFormData.getJobDesc(), 
                                                getNodeTypeIcon(updatedFormData.getGlueType()));
                                        }
                                        if (treeView != null) {
                                            treeView.updateJobNode(jobId, updatedFormData.getJobDesc());
                                        }
                                    }
                                });
                            } catch (Exception e) {
                                Platform.runLater(() -> logPanel.error("✗ 更新失败: " + e.getMessage()));
                            }
                        }).start();
                    });
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 获取节点数据失败: " + e.getMessage()));
            }
        }).start();
    }
    
    private ProcessNode addNodeToCanvas(JobNode jobNode, JobInfoForm formData) {
        return addNodeToCanvas(jobNode, formData, true);
    }
    
    private ProcessNode addNodeToCanvas(JobNode jobNode, JobInfoForm formData, boolean recordHistory) {
        double[] position = calculateNewNodePosition();
        double x = jobNode.getNodePositionX() != null ? jobNode.getNodePositionX() : position[0];
        double y = jobNode.getNodePositionY() != null ? jobNode.getNodePositionY() : position[1];
        
        ProcessNode node = new ProcessNode(String.valueOf(jobNode.getId()), formData.getJobDesc(), x, y);
        node.setJobId(jobNode.getJobId());
        node.setType(getNodeTypeIcon(formData.getGlueType()));
        canvas.addNode(node, recordHistory);
        
        // 配置节点的业务逻辑回调（编辑、复制、查看详情等）
        if (nodeCallbackConfigurator != null && formData.getParentId() != null) {
            nodeCallbackConfigurator.configureNodeCallbacks(node, formData.getParentId());
        }
        
        return node;
    }
    
    /**
     * 计算粘贴位置 - 基于原节点位置计算偏移量
     */
    private double[] calculatePastePosition() {
        // 如果有保存的原节点位置，基于该位置计算偏移
        if (copiedNodeX != null && copiedNodeY != null) {
            return calculateDuplicatePosition(copiedNodeX, copiedNodeY);
        }
        
        // 如果没有原节点位置信息，使用默认位置
        return new double[]{300, 200};
    }
    
    /**
     * 计算复制节点的新位置 - 基于原节点位置偏移
     * @param originalX 原节点X坐标
     * @param originalY 原节点Y坐标
     * @return 新节点的位置 [x, y]
     */
    private double[] calculateDuplicatePosition(double originalX, double originalY) {
        // 偏移量：向右下角偏移80像素
        double offsetX = 80.0;
        double offsetY = 80.0;
        
        double newX = originalX + offsetX;
        double newY = originalY + offsetY;
        
        // 确保新节点位置在画布可视区域内
        // 获取画布的可视区域（如果有ScrollPane）
        if (canvas.getScene() != null && canvas.getScene().getWindow() != null) {
            javafx.scene.control.ScrollPane scrollPane = findScrollPane(canvas);
            if (scrollPane != null) {
                javafx.geometry.Bounds viewportBounds = scrollPane.getViewportBounds();
                double viewportWidth = viewportBounds.getWidth();
                double viewportHeight = viewportBounds.getHeight();
                
                // 获取当前可视区域的左上角坐标
                double canvasWidth = canvas.getPrefWidth();
                double canvasHeight = canvas.getPrefHeight();
                double scrollableWidth = Math.max(0, canvasWidth - viewportWidth);
                double scrollableHeight = Math.max(0, canvasHeight - viewportHeight);
                
                double viewportLeft = scrollableWidth > 0 ? scrollPane.getHvalue() * scrollableWidth : 0;
                double viewportTop = scrollableHeight > 0 ? scrollPane.getVvalue() * scrollableHeight : 0;
                double viewportRight = viewportLeft + viewportWidth;
                double viewportBottom = viewportTop + viewportHeight;
                
                // 估算节点大小（假设节点宽度约200，高度约100）
                double estimatedNodeWidth = 200.0;
                double estimatedNodeHeight = 100.0;
                
                // 如果新节点会超出可视区域右边界，调整到可视区域内
                if (newX + estimatedNodeWidth > viewportRight) {
                    newX = Math.max(viewportLeft + 20, viewportRight - estimatedNodeWidth - 20);
                }
                
                // 如果新节点会超出可视区域下边界，调整到可视区域内
                if (newY + estimatedNodeHeight > viewportBottom) {
                    newY = Math.max(viewportTop + 20, viewportBottom - estimatedNodeHeight - 20);
                }
                
                // 确保新节点位置在画布范围内
                newX = Math.max(20, Math.min(newX, canvasWidth - estimatedNodeWidth - 20));
                newY = Math.max(20, Math.min(newY, canvasHeight - estimatedNodeHeight - 20));
            }
        }
        
        return new double[]{newX, newY};
    }
    
    /**
     * 查找包含画布的ScrollPane
     */
    private javafx.scene.control.ScrollPane findScrollPane(javafx.scene.Node node) {
        javafx.scene.Node parent = node.getParent();
        while (parent != null) {
            if (parent instanceof javafx.scene.control.ScrollPane) {
                return (javafx.scene.control.ScrollPane) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    private double[] calculateNewNodePosition() {
        List<ProcessNode> existingNodes = canvas.getNodes();
        if (existingNodes == null || existingNodes.isEmpty()) {
            return new double[]{100, 100};
        }
        
        int nodeCount = existingNodes.size();
        int row = nodeCount / 4;
        int col = nodeCount % 4;
        
        double x = 100 + col * 280;
        double y = 100 + row * 180;
        
        return new double[]{x, y};
    }
    
    private String generateCopyName(String originalName) {
        String base = (originalName == null || originalName.trim().isEmpty()) ? "新任务" : originalName.trim();
        Set<String> existingNames = new HashSet<>();
        for (ProcessNode node : canvas.getNodes()) {
            if (node.getJobHandlerName() != null) {
                existingNames.add(node.getJobHandlerName());
            }
        }
        
        String candidate = base + "_copy";
        int index = 2;
        while (existingNames.contains(candidate)) {
            candidate = base + "_copy" + index++;
        }
        return candidate;
    }
    
    private String getNodeTypeIcon(String glueType) {
        if (glueType == null) return "Bean";
        switch (glueType) {
            case "BEAN": return "Bean";
            case "API": return "API";
            case "SQL": return "SQL";
            case "GLUE_GROOVY": return "Java";
            case "GLUE_SHELL": return "Shell";
            case "GLUE_PYTHON": return "Python";
            case "GLUE_PHP": return "PHP";
            case "GLUE_NODEJS": return "Node";
            case "GLUE_POWERSHELL": return "PS";
            case "GLUE_CSHARP": return "C#";
            default: return "Bean";
        }
    }
    
    /**
     * 批量编辑节点
     */
    public void batchEditNodes(Set<ProcessNode> nodes, com.cc.job.gui.view.BatchEditDialog.BatchEditResult result) {
        if (nodes == null || nodes.isEmpty() || result == null) {
            Platform.runLater(() -> logPanel.warn("⚠ 没有选中的节点或操作无效"));
            return;
        }
        
        Platform.runLater(() -> logPanel.info("🔄 正在批量编辑 " + nodes.size() + " 个节点..."));
        
        new Thread(() -> {
            try {
                int successCount = 0;
                int failCount = 0;
                
                switch (result.getOperation()) {
                    case REPLACE_NAME:
                        successCount = batchReplaceName(nodes, result.getFindText(), result.getReplaceText());
                        failCount = nodes.size() - successCount;
                        break;
                    case MODIFY_PROPERTY:
                        successCount = batchModifyProperty(nodes, result.getPropertyName(), result.getPropertyValue());
                        failCount = nodes.size() - successCount;
                        break;
                    case TOGGLE_ENABLED:
                        successCount = batchToggleEnabled(nodes, result.getEnabled());
                        failCount = nodes.size() - successCount;
                        break;
                    case DELETE:
                        Platform.runLater(() -> {
                            canvas.removeNodesAsBatch(nodes);
                        });
                        successCount = nodes.size();
                        failCount = 0;
                        break;
                }

                int finalFailCount = failCount;
                int finalSuccessCount = successCount;
                Platform.runLater(() -> {
                    if (finalFailCount == 0) {
                        logPanel.success("✓ 批量操作完成: " + finalSuccessCount + " 个节点");
                    } else {
                        logPanel.warn("⚠ 批量操作完成: 成功 " + finalSuccessCount + " 个，失败 " + finalFailCount + " 个");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 批量操作失败: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 批量替换节点名称
     */
    private int batchReplaceName(Set<ProcessNode> nodes, String findText, String replaceText) {
        if (findText == null || findText.trim().isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (ProcessNode node : nodes) {
            try {
                String currentName = node.getJobHandlerName();
                if (currentName != null && currentName.contains(findText)) {
                    String newName = currentName.replace(findText, replaceText != null ? replaceText : "");
                    node.updateJobHandlerName(newName);
                    count++;
                }
            } catch (Exception e) {
                logger.error("替换节点名称失败: " + node.getJobHandlerName(), e);
            }
        }
        return count;
    }
    
    /**
     * 批量修改属性
     */
    private int batchModifyProperty(Set<ProcessNode> nodes, String propertyName, String propertyValue) {
        if (propertyName == null || propertyName.trim().isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (ProcessNode node : nodes) {
            try {
                Long jobId = node.getJobId();
                if (jobId == null) continue;
                
                JobInfoForm formData = jobInfoService.getJobNodeFormData(jobId);
                if (formData == null) continue;
                
                // 根据属性名称修改对应的字段
                switch (propertyName) {
                    case "执行器":
                        // 这里需要根据propertyValue找到对应的JobGroup ID
                        // 简化处理，暂时跳过
                        break;
                    case "路由策略":
                        formData.setExecutorRouteStrategy(propertyValue);
                        break;
                    case "阻塞策略":
                        formData.setExecutorBlockStrategy(propertyValue);
                        break;
                    case "失败策略":
                        formData.setFailStrategy(propertyValue);
                        break;
                }
                
                boolean success = jobInfoService.updateJobInfo(jobId, formData);
                if (success) {
                    count++;
                }
            } catch (Exception e) {
                logger.error("修改节点属性失败: " + node.getJobHandlerName(), e);
            }
        }
        return count;
    }
    
    /**
     * 批量启用/禁用节点
     */
    private int batchToggleEnabled(Set<ProcessNode> nodes, Boolean enabled) {
        if (enabled == null) {
            return 0;
        }
        
        int count = 0;
        for (ProcessNode node : nodes) {
            try {
                Long jobId = node.getJobId();
                if (jobId == null) continue;
                
                Integer isPause = enabled ? 0 : 1;
                boolean success = jobInfoService.pauseJob(jobId, isPause);
                if (success) {
                    node.restoreEnabledState(enabled);
                    count++;
                }
            } catch (Exception e) {
                logger.error("启用/禁用节点失败: " + node.getJobHandlerName(), e);
            }
        }
        return count;
    }
    
    /**
     * 批量删除节点
     */
    private int batchDeleteNodes(Set<ProcessNode> nodes) {
        int count = 0;
        for (ProcessNode node : nodes) {
            try {
                canvas.removeNode(node, true);
                count++;
            } catch (Exception e) {
                logger.error("删除节点失败: " + node.getJobHandlerName(), e);
            }
        }
        return count;
    }
    
    private JobInfoForm deepCopyJobInfoForm(JobInfoForm original) {
        String json = ApiUtil.getInstance().getGson().toJson(original);
        return ApiUtil.getInstance().getGson().fromJson(json, JobInfoForm.class);
    }
    
    // 内部类：复制的节点数据
    private static class CopiedNodesData {
        List<NodeFormData> nodeForms = new ArrayList<>();
        List<ConnectionInfo> connections = new ArrayList<>();
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        
        static class NodeFormData {
            JobInfoForm form;
            Long originalJobId;
            String originalNodeId;
            double originalX;  // 节点的实际布局X坐标
            double originalY;  // 节点的实际布局Y坐标
        }
        
        static class ConnectionInfo {
            Long sourceJobId;
            Long targetJobId;
            String sourceAnchor;
            String targetAnchor;
        }
    }
}

