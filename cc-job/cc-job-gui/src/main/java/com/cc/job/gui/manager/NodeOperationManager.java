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
     * 复制节点 - 直接创建新节点并添加到画布
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
                
                // 创建新节点的表单数据
                JobInfoForm copyForm = deepCopyJobInfoForm(originalForm);
                copyForm.setId(null);
                copyForm.setParentId(currentTaskGroupId);
                copyForm.setJobDesc(generateCopyName(originalForm.getJobDesc()));
                
                // 计算新节点的位置（在源节点右侧）
                double[] position = calculateCopyPosition(sourceNode);
                copyForm.setNodePositionX(position[0]);
                copyForm.setNodePositionY(position[1]);
                copyForm.setGlueUpdateTime(null);
                
                // 保存新节点到后端
                JobNode newJobNode = jobInfoService.saveJobNode(copyForm);
                if (newJobNode != null) {
                    Platform.runLater(() -> {
                        addNodeToCanvas(newJobNode, copyForm);
                        logPanel.success("✓ 节点复制成功: " + copyForm.getJobDesc());
                    });
                } else {
                    Platform.runLater(() -> logPanel.error("✗ 节点复制失败：后端返回空"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 复制失败: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 计算复制节点的位置（在源节点右侧）
     */
    private double[] calculateCopyPosition(ProcessNode sourceNode) {
        double sourceX = sourceNode.getLayoutX();
        double sourceY = sourceNode.getLayoutY();
        double nodeWidth = sourceNode.getWidth() > 0 ? sourceNode.getWidth() : sourceNode.getPrefWidth();
        
        // 在源节点右侧，间隔50像素
        double newX = sourceX + nodeWidth + 50;
        double newY = sourceY;
        
        return new double[]{newX, newY};
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
                    data.nodeForms.add(nodeData);
                    
                    double nodeX = node.getLayoutX();
                    double nodeY = node.getLayoutY();
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
                        addNodeToCanvas(newJobNode, pasteForm);
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
                
                // 创建节点
                for (CopiedNodesData.NodeFormData nodeData : copiedNodesData.nodeForms) {
                    JobInfoForm pasteForm = deepCopyJobInfoForm(nodeData.form);
                    pasteForm.setId(null);
                    pasteForm.setParentId(currentTaskGroupId);
                    pasteForm.setJobDesc(generateCopyName(nodeData.form.getJobDesc()));
                    
                    double originalX = nodeData.form.getNodePositionX() != null ? nodeData.form.getNodePositionX() : 0;
                    double originalY = nodeData.form.getNodePositionY() != null ? nodeData.form.getNodePositionY() : 0;
                    pasteForm.setNodePositionX(originalX + offsetX);
                    pasteForm.setNodePositionY(originalY + offsetY);
                    
                    JobNode newJobNode = jobInfoService.saveJobNode(pasteForm);
                    if (newJobNode != null) {
                        Long originalJobId = nodeData.originalJobId;
                        Platform.runLater(() -> {
                            ProcessNode newNode = addNodeToCanvas(newJobNode, pasteForm);
                            if (newNode != null) {
                                newNodes.add(newNode);
                                oldJobIdToNewNode.put(originalJobId, newNode);
                            }
                        });
                    }
                }
                
                Thread.sleep(500);
                
                // 恢复连接
                Platform.runLater(() -> {
                    for (CopiedNodesData.ConnectionInfo connInfo : copiedNodesData.connections) {
                        ProcessNode src = oldJobIdToNewNode.get(connInfo.sourceJobId);
                        ProcessNode tgt = oldJobIdToNewNode.get(connInfo.targetJobId);
                        if (src != null && tgt != null) {
                            canvas.addConnection(src, tgt);
                        }
                    }
                    canvas.selectNodes(newNodes);
                    logPanel.success("✓ " + newNodes.size() + " 个节点粘贴成功");
                });
            } catch (Exception e) {
                Platform.runLater(() -> logPanel.error("✗ 粘贴失败: " + e.getMessage()));
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
        double[] position = calculateNewNodePosition();
        double x = jobNode.getNodePositionX() != null ? jobNode.getNodePositionX() : position[0];
        double y = jobNode.getNodePositionY() != null ? jobNode.getNodePositionY() : position[1];
        
        ProcessNode node = new ProcessNode(String.valueOf(jobNode.getId()), formData.getJobDesc(), x, y);
        node.setJobId(jobNode.getJobId());
        node.setType(getNodeTypeIcon(formData.getGlueType()));
        canvas.addNode(node, true);
        
        // 配置节点的业务逻辑回调（编辑、复制、查看详情等）
        if (nodeCallbackConfigurator != null && formData.getParentId() != null) {
            nodeCallbackConfigurator.configureNodeCallbacks(node, formData.getParentId());
        }
        
        return node;
    }
    
    private double[] calculatePastePosition() {
        return new double[]{300, 200};
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
        }
        
        static class ConnectionInfo {
            Long sourceJobId;
            Long targetJobId;
            String sourceAnchor;
            String targetAnchor;
        }
    }
}

