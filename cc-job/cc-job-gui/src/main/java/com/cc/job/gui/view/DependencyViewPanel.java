package com.cc.job.gui.view;

import com.cc.job.gui.manager.DependencyNavigator;
import com.cc.job.gui.model.NodeConnection;
import com.cc.job.gui.model.ProcessNode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 依赖关系视图面板
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class DependencyViewPanel extends Dialog<Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyViewPanel.class);
    
    private DependencyNavigator navigator;
    private List<ProcessNode> nodes;
    private List<NodeConnection> connections;
    private ProcessNode selectedNode;
    private Consumer<ProcessNode> onNodeSelected;
    
    private TreeView<DependencyNodeItem> dependencyTreeView;
    private TableView<CyclePathItem> cyclesTable;
    
    public DependencyViewPanel(Stage owner, List<ProcessNode> nodes, List<NodeConnection> connections,
                              ProcessNode selectedNode, Consumer<ProcessNode> onNodeSelected) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.connections = connections != null ? connections : new ArrayList<>();
        this.selectedNode = selectedNode;
        this.onNodeSelected = onNodeSelected;
        this.navigator = new DependencyNavigator(msg -> logger.info(msg));
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("节点依赖关系");
        setHeaderText(selectedNode != null ? 
            "节点 \"" + selectedNode.getJobHandlerName() + "\" 的依赖关系" : 
            "所有节点的依赖关系");
        
        // 创建对话框内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        // 添加按钮
        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(closeButtonType);
        
        // 设置样式
        styleDialog();
        
        // 构建并显示依赖关系
        buildDependencyView();
    }
    
    private VBox createContent() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(800);
        container.setPrefHeight(600);
        
        // 创建标签页
        TabPane tabPane = new TabPane();
        
        // 依赖树标签页
        Tab treeTab = new Tab("依赖树");
        treeTab.setClosable(false);
        dependencyTreeView = new TreeView<>();
        dependencyTreeView.setPrefHeight(500);
        dependencyTreeView.setCellFactory(tv -> new TreeCell<DependencyNodeItem>() {
            @Override
            protected void updateItem(DependencyNodeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getNodeName());
                    setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2 && onNodeSelected != null) {
                            onNodeSelected.accept(item.getNode());
                        }
                    });
                }
            }
        });
        treeTab.setContent(dependencyTreeView);
        
        // 上游节点标签页
        Tab upstreamTab = new Tab("上游节点");
        upstreamTab.setClosable(false);
        TableView<DependencyNodeItem> upstreamTable = createNodeTable();
        upstreamTab.setContent(upstreamTable);
        
        // 下游节点标签页
        Tab downstreamTab = new Tab("下游节点");
        downstreamTab.setClosable(false);
        TableView<DependencyNodeItem> downstreamTable = createNodeTable();
        downstreamTab.setContent(downstreamTable);
        
        // 循环依赖标签页
        Tab cyclesTab = new Tab("循环依赖");
        cyclesTab.setClosable(false);
        cyclesTable = new TableView<>();
        cyclesTable.setPrefHeight(500);
        
        TableColumn<CyclePathItem, String> cyclePathColumn = new TableColumn<>("循环路径");
        cyclePathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        cyclePathColumn.setPrefWidth(600);
        cyclesTable.getColumns().add(cyclePathColumn);
        cyclesTab.setContent(cyclesTable);
        
        tabPane.getTabs().addAll(treeTab,
                upstreamTab,
                downstreamTab
                //cyclesTab
        );
        
        // 如果选中了节点，显示上游和下游节点
        if (selectedNode != null) {
            DependencyNavigator.DependencyGraph graph = navigator.buildDependencyGraph(nodes, connections);
            Set<ProcessNode> upstream = navigator.getAllUpstreamNodes(graph, selectedNode);
            Set<ProcessNode> downstream = navigator.getAllDownstreamNodes(graph, selectedNode);
            
            // 填充上游节点表格
            javafx.collections.ObservableList<DependencyNodeItem> upstreamData = 
                javafx.collections.FXCollections.observableArrayList();
            for (ProcessNode node : upstream) {
                upstreamData.add(new DependencyNodeItem(node, node.getJobHandlerName(), node.getType()));
            }
            upstreamTable.setItems(upstreamData);
            
            // 填充下游节点表格
            javafx.collections.ObservableList<DependencyNodeItem> downstreamData = 
                javafx.collections.FXCollections.observableArrayList();
            for (ProcessNode node : downstream) {
                downstreamData.add(new DependencyNodeItem(node, node.getJobHandlerName(), node.getType()));
            }
            downstreamTable.setItems(downstreamData);
        }
        
        container.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        return container;
    }
    
    private TableView<DependencyNodeItem> createNodeTable() {
        TableView<DependencyNodeItem> table = new TableView<>();
        table.setPrefHeight(500);
        
        TableColumn<DependencyNodeItem, String> nameColumn = new TableColumn<>("节点名称");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nodeName"));
        nameColumn.setPrefWidth(300);
        
        TableColumn<DependencyNodeItem, String> typeColumn = new TableColumn<>("类型");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("nodeType"));
        typeColumn.setPrefWidth(100);
        
        table.getColumns().addAll(nameColumn, typeColumn);
        
        // 双击定位节点
        table.setRowFactory(tv -> {
            TableRow<DependencyNodeItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && onNodeSelected != null) {
                    DependencyNodeItem item = row.getItem();
                    if (item != null && item.getNode() != null) {
                        onNodeSelected.accept(item.getNode());
                    }
                }
            });
            return row;
        });
        
        return table;
    }
    
    private void buildDependencyView() {
        DependencyNavigator.DependencyGraph graph = navigator.buildDependencyGraph(nodes, connections);
        
        // 构建依赖树
        DependencyNavigator.DependencyTreeNode root = navigator.buildDependencyTree(graph, selectedNode);
        if (root != null) {
            TreeItem<DependencyNodeItem> rootItem = new TreeItem<>(
                new DependencyNodeItem(root.getNode(), root.getNode().getJobHandlerName(), root.getNode().getType())
            );
            buildTreeItems(root, rootItem);
            dependencyTreeView.setRoot(rootItem);
            rootItem.setExpanded(true);
        }
        
        // 检测循环依赖
        List<List<ProcessNode>> cycles = navigator.detectCycles(graph);
        javafx.collections.ObservableList<CyclePathItem> cyclesData = 
            javafx.collections.FXCollections.observableArrayList();
        for (List<ProcessNode> cycle : cycles) {
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 0; i < cycle.size(); i++) {
                if (i > 0) pathBuilder.append(" -> ");
                pathBuilder.append(cycle.get(i).getJobHandlerName());
            }
            cyclesData.add(new CyclePathItem(pathBuilder.toString(), cycle));
        }
        cyclesTable.setItems(cyclesData);
    }
    
    private void buildTreeItems(DependencyNavigator.DependencyTreeNode treeNode, TreeItem<DependencyNodeItem> treeItem) {
        for (DependencyNavigator.DependencyTreeNode child : treeNode.getChildren()) {
            TreeItem<DependencyNodeItem> childItem = new TreeItem<>(
                new DependencyNodeItem(child.getNode(), child.getNode().getJobHandlerName(), child.getNode().getType())
            );
            treeItem.getChildren().add(childItem);
            buildTreeItems(child, childItem);
        }
    }
    
    private void styleDialog() {
        getDialogPane().setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E5E7EB; " +
            "-fx-border-width: 1;"
        );
    }
    
    /**
     * 依赖节点项（用于表格和树显示）
     */
    public static class DependencyNodeItem {
        private ProcessNode node;
        private String nodeName;
        private String nodeType;
        
        public DependencyNodeItem(ProcessNode node, String nodeName, String nodeType) {
            this.node = node;
            this.nodeName = nodeName;
            this.nodeType = nodeType;
        }
        
        public ProcessNode getNode() {
            return node;
        }
        
        public String getNodeName() {
            return nodeName;
        }
        
        public String getNodeType() {
            return nodeType;
        }
    }
    
    /**
     * 循环路径项（用于表格显示）
     */
    public static class CyclePathItem {
        private String path;
        private List<ProcessNode> cycle;
        
        public CyclePathItem(String path, List<ProcessNode> cycle) {
            this.path = path;
            this.cycle = cycle;
        }
        
        public String getPath() {
            return path;
        }
        
        public List<ProcessNode> getCycle() {
            return cycle;
        }
    }
}
