package com.cc.job.gui.view;

import com.cc.job.gui.manager.NodeSearchManager;
import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.util.IconUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.function.Consumer;

/**
 * 高级节点搜索对话框
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class NodeSearchDialog extends Dialog<ProcessNode> {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeSearchDialog.class);
    
    private NodeSearchManager searchManager;
    private List<ProcessNode> allNodes;
    private Consumer<ProcessNode> onNodeSelected;
    
    // 搜索条件控件
    private TextField keywordField;
    private ComboBox<String> typeCombo;
    private ComboBox<ProcessNode.NodeStatus> statusCombo;
    private ListView<String> tagsList;
    private TextField tagInputField;
    private TextField remarkKeywordField;
    
    // 搜索结果
    private TableView<SearchResultItem> resultsTable;
    private ObservableList<SearchResultItem> resultsData;
    
    public NodeSearchDialog(Stage owner, List<ProcessNode> nodes, Consumer<ProcessNode> onNodeSelected) {
        this.allNodes = nodes != null ? nodes : new ArrayList<>();
        this.onNodeSelected = onNodeSelected;
        this.searchManager = new NodeSearchManager(msg -> logger.info(msg));
        
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("搜索节点");
        setHeaderText("使用多种条件搜索节点");
        
        // 创建对话框内容
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        // 添加按钮
        ButtonType searchButtonType = new ButtonType("搜索", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, searchButtonType);
        
        // 设置样式
        styleDialog();
        
        // 设置结果转换器
        setResultConverter(buttonType -> {
            if (buttonType == searchButtonType) {
                SearchResultItem selected = resultsTable.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getNode() != null) {
                    return selected.getNode();
                }
            }
            return null;
        });
        
        // 执行初始搜索（如果有默认条件）
        performSearch();
    }
    
    private VBox createContent() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setPrefWidth(700);
        container.setPrefHeight(600);
        
        // 搜索条件区域
        VBox criteriaBox = createCriteriaBox();
        
        // 搜索结果区域
        VBox resultsBox = createResultsBox();
        
        container.getChildren().addAll(criteriaBox, resultsBox);
        VBox.setVgrow(resultsBox, Priority.ALWAYS);
        
        return container;
    }
    
    private VBox createCriteriaBox() {
        VBox box = new VBox(10);
        box.getStyleClass().add("dialog-section");
        
        Label titleLabel = new Label("搜索条件");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // 关键字搜索
        Label keywordLabel = new Label("关键字：");
        keywordField = new TextField();
        keywordField.setPromptText("输入节点名称关键字");
        keywordField.setPrefWidth(200);
        keywordField.setOnAction(e -> performSearch());
        
        // 节点类型
        Label typeLabel = new Label("节点类型：");
        typeCombo = new ComboBox<>();
        typeCombo.setPrefWidth(200);
        typeCombo.getItems().addAll("", "Bean", "API", "SQL", "Java", "Shell", "Python", "PHP", "Node", "PS");
        typeCombo.setOnAction(e -> performSearch());
        
        // 节点状态
        Label statusLabel = new Label("节点状态：");
        statusCombo = new ComboBox<>();
        statusCombo.setPrefWidth(200);
        statusCombo.getItems().addAll(null, ProcessNode.NodeStatus.IDLE, ProcessNode.NodeStatus.RUNNING, 
                                     ProcessNode.NodeStatus.SUCCESS, ProcessNode.NodeStatus.FAILED);
        statusCombo.setOnAction(e -> performSearch());
        
        // 标签搜索
        Label tagsLabel = new Label("标签：");
        HBox tagsBox = new HBox(5);
        tagInputField = new TextField();
        tagInputField.setPromptText("输入标签，按回车添加");
        tagInputField.setPrefWidth(150);
        tagInputField.setOnAction(e -> addTag());
        Button addTagButton = new Button("添加");
        addTagButton.setOnAction(e -> addTag());
        tagsList = new ListView<>();
        tagsList.setPrefHeight(80);
        ObservableList<String> tags = FXCollections.observableArrayList();
        tagsList.setItems(tags);
        
        // 备注搜索
        Label remarkLabel = new Label("备注关键字：");
        remarkKeywordField = new TextField();
        remarkKeywordField.setPromptText("在备注中搜索");
        remarkKeywordField.setPrefWidth(200);
        remarkKeywordField.setOnAction(e -> performSearch());
        
        // 搜索按钮
        Button searchButton = new Button("搜索");
        searchButton.setOnAction(e -> performSearch());
        
        grid.add(keywordLabel, 0, 0);
        grid.add(keywordField, 1, 0);
        grid.add(typeLabel, 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(statusLabel, 0, 2);
        grid.add(statusCombo, 1, 2);
        grid.add(remarkLabel, 0, 3);
        grid.add(remarkKeywordField, 1, 3);
        grid.add(tagsLabel, 0, 4);
        grid.add(tagsBox, 1, 4);
        grid.add(tagsList, 1, 5);
        grid.add(searchButton, 0, 6);
        
        box.getChildren().addAll(titleLabel, grid);
        
        return box;
    }
    
    private VBox createResultsBox() {
        VBox box = new VBox(10);
        
        Label titleLabel = new Label("搜索结果");
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600;");
        
        resultsData = FXCollections.observableArrayList();
        resultsTable = new TableView<>(resultsData);
        resultsTable.setPrefHeight(300);
        
        // 创建列
        TableColumn<SearchResultItem, String> nameColumn = new TableColumn<>("节点名称");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nodeName"));
        nameColumn.setPrefWidth(200);
        
        TableColumn<SearchResultItem, String> typeColumn = new TableColumn<>("类型");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("nodeType"));
        typeColumn.setPrefWidth(100);
        
        TableColumn<SearchResultItem, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("nodeStatus"));
        statusColumn.setPrefWidth(100);
        
        TableColumn<SearchResultItem, Double> scoreColumn = new TableColumn<>("匹配度");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("matchScore"));
        scoreColumn.setPrefWidth(100);
        scoreColumn.setCellFactory(column -> new TableCell<SearchResultItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.0f%%", item * 100));
                }
            }
        });
        
        TableColumn<SearchResultItem, String> reasonsColumn = new TableColumn<>("匹配原因");
        reasonsColumn.setCellValueFactory(new PropertyValueFactory<>("matchReasons"));
        reasonsColumn.setPrefWidth(200);
        
        resultsTable.getColumns().addAll(nameColumn, typeColumn, statusColumn, scoreColumn, reasonsColumn);
        
        // 双击定位节点
        resultsTable.setRowFactory(tv -> {
            TableRow<SearchResultItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    SearchResultItem item = row.getItem();
                    if (item != null && item.getNode() != null && onNodeSelected != null) {
                        onNodeSelected.accept(item.getNode());
                        close();
                    }
                }
            });
            return row;
        });
        
        box.getChildren().addAll(titleLabel, resultsTable);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        
        return box;
    }
    
    private void addTag() {
        String tag = tagInputField.getText().trim();
        if (tag.isEmpty()) {
            return;
        }
        
        ObservableList<String> tags = tagsList.getItems();
        if (!tags.contains(tag)) {
            tags.add(tag);
            tagInputField.clear();
            performSearch();
        }
    }
    
    private void performSearch() {
        NodeSearchManager.SearchCriteria criteria = new NodeSearchManager.SearchCriteria();
        
        // 设置搜索条件
        if (keywordField != null && keywordField.getText() != null && !keywordField.getText().trim().isEmpty()) {
            criteria.setKeyword(keywordField.getText().trim());
        }
        
        if (typeCombo != null && typeCombo.getValue() != null && !typeCombo.getValue().trim().isEmpty()) {
            criteria.setNodeType(typeCombo.getValue());
        }
        
        if (statusCombo != null && statusCombo.getValue() != null) {
            criteria.setNodeStatus(statusCombo.getValue());
        }
        
        if (tagsList != null && !tagsList.getItems().isEmpty()) {
            criteria.setTags(new ArrayList<>(tagsList.getItems()));
        }
        
        if (remarkKeywordField != null && remarkKeywordField.getText() != null && !remarkKeywordField.getText().trim().isEmpty()) {
            criteria.setRemarkKeyword(remarkKeywordField.getText().trim());
        }
        
        // 执行搜索
        List<NodeSearchManager.SearchResult> results = searchManager.search(allNodes, criteria);
        
        // 更新结果表格
        resultsData.clear();
        for (NodeSearchManager.SearchResult result : results) {
            ProcessNode node = result.getNode();
            String reasons = String.join(", ", result.getMatchReasons());
            resultsData.add(new SearchResultItem(node, node.getJobHandlerName(), node.getType(), 
                                                node.getStatus().toString(), result.getMatchScore(), reasons));
        }
    }
    
    private void styleDialog() {
        String cssUrl = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            getDialogPane().getStylesheets().add(cssUrl);
        }
    }
    
    /**
     * 搜索结果项（用于表格显示）
     */
    public static class SearchResultItem {
        private ProcessNode node;
        private String nodeName;
        private String nodeType;
        private String nodeStatus;
        private Double matchScore;
        private String matchReasons;
        
        public SearchResultItem(ProcessNode node, String nodeName, String nodeType, 
                               String nodeStatus, double matchScore, String matchReasons) {
            this.node = node;
            this.nodeName = nodeName;
            this.nodeType = nodeType;
            this.nodeStatus = nodeStatus;
            this.matchScore = matchScore;
            this.matchReasons = matchReasons;
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
        
        public String getNodeStatus() {
            return nodeStatus;
        }
        
        public Double getMatchScore() {
            return matchScore;
        }
        
        public String getMatchReasons() {
            return matchReasons;
        }
    }
}
