package com.cc.job.gui.view;

import com.cc.job.gui.service.JobGroupService;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.form.JobInfoForm;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 展示任务列表
 * 与Web端job-info页面功能保持一致
 */
public class ShowJobListDialog extends Dialog<Void> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobInfoService jobInfoService = new JobInfoService();
    private final JobGroupService jobGroupService = new JobGroupService();
    private final Stage ownerStage;

    private TableView<JobInfoVO> tableView;
    private TableView.TableViewSelectionModel<JobInfoVO> selectionModel;
    
    // 筛选区控件
    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField handlerField;
    private TextField authorField;
    private ComboBox<String> statusCombo;

    // 分页区控件
    private Label totalLabel;
    private TextField pageField;
    private ComboBox<Integer> pageSizeBox;
    private Button prevBtn;
    private Button nextBtn;

    // 执行器列表缓存
    private List<JobGroup> jobGroupList = new ArrayList<>();

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int pageNum = 1;
    private int pageSize = 10;
    private long total = 0;

    private Runnable themeChangedListener;

    public ShowJobListDialog(Stage ownerStage) {
        this.ownerStage = ownerStage;
        setTitle("任务列表");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        setOnHidden(e -> {
            if (themeChangedListener != null) {
                com.cc.job.gui.util.ThemeManager.getInstance().removeOnThemeChanged(themeChangedListener);
            }
        });
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

        root.setTop(createTopSection());
        root.setCenter(createTable());
        root.setBottom(createPagerBar());

        getDialogPane().setContent(root);

        // 先加载执行器列表，再加载数据
        loadJobGroupList();
        loadPage(true);
    }

    /**
     * 创建顶部区域：筛选栏 + 操作按钮
     */
    private Node createTopSection() {
        VBox topSection = new VBox(12);
        topSection.getChildren().addAll(createFilterBar(), createActionBar());
        topSection.setPadding(new Insets(0, 0, 12, 0)); // 底部间距12px
        return topSection;
    }

    /**
     * 创建操作按钮栏：新增、删除
     */
    private Node createActionBar() {
        HBox actionBar = new HBox(12);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        actionBar.setPadding(new Insets(0, 16, 0, 16));

        Button addBtn = new Button("新增");
        addBtn.getStyleClass().add("dialog-button-success");
        addBtn.setOnAction(e -> handleAdd());

        Button deleteBtn = new Button("删除");
        deleteBtn.getStyleClass().add("dialog-button-error");
        deleteBtn.setOnAction(e -> handleBatchDelete());

        actionBar.getChildren().addAll(addBtn, deleteBtn);
        return actionBar;
    }

    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        getDialogPane().setPrefSize(1080, 680);
        // 设置对话框大小
        getDialogPane().setPrefWidth(1080);
        getDialogPane().setPrefHeight(580);
        getDialogPane().setMinWidth(1080);
        getDialogPane().setMinHeight(580);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);

        String dialogCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            getDialogPane().getStylesheets().add(dialogCss);
        }

        themeChangedListener = () -> {
            javafx.scene.Scene scene = getDialogPane().getScene();
            if (scene != null) {
                scene.getStylesheets().clear();
                String url = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                if (url != null && !url.isEmpty()) {
                    scene.getStylesheets().add(url);
                }
            }
        };
        com.cc.job.gui.util.ThemeManager.getInstance().addOnThemeChanged(themeChangedListener);

        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(1080);
                stage.setMinHeight(580);
                
                // 加载当前主题样式
                String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                if (css != null && !css.isEmpty()) {
                    stage.getScene().getStylesheets().add(css);
                }
                
                stage.setOnCloseRequest(event -> {
                    // 这里可以添加关闭前的确认逻辑，例如：
                    // if (!dataIsSaved) {
                    //     event.consume(); // 阻止关闭
                    //     showSaveDialog();
                    // }
                    close(); // 调用Dialog的close方法
                });
            }
        });
    }

    private Node createFilterBar() {
        FlowPane pane = new FlowPane();
        pane.getStyleClass().add("dialog-section");
        pane.setHgap(12);
        pane.setVgap(10);
        pane.setPadding(new Insets(16, 16, 16, 16));

        // 创建标签样式
        String labelStyle = StyleUtil.bodyFontOnly();

        // 执行器下拉框
        jobGroupCombo = new ComboBox<>();
        jobGroupCombo.setPrefWidth(180);
        jobGroupCombo.setCellFactory(listView -> new ListCell<JobGroup>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("全部");
                } else {
                    setText(item.getTitle());
                }
            }
        });
        jobGroupCombo.setButtonCell(new ListCell<JobGroup>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("全部");
                } else {
                    setText(item.getTitle());
                }
            }
        });

        // 任务状态下拉框
        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("全部", "运行", "停止");
        statusCombo.getSelectionModel().selectFirst();
        statusCombo.setPrefWidth(100);

        // 任务描述输入框
        jobDescField = new TextField();
        jobDescField.setPromptText("请输入任务描述");
        jobDescField.setPrefWidth(180);
        jobDescField.setOnAction(e -> handleSearch());

        // JobHandler输入框
        handlerField = new TextField();
        handlerField.setPromptText("请输入JobHandler");
        handlerField.setPrefWidth(160);
        handlerField.setOnAction(e -> handleSearch());

        // 负责人输入框
        authorField = new TextField();
        authorField.setPromptText("请输入负责人");
        authorField.setPrefWidth(140);
        authorField.setOnAction(e -> handleSearch());

        // 搜索按钮
        Button searchBtn = new Button("搜索");
        searchBtn.getStyleClass().add("dialog-button-primary");
        searchBtn.setOnAction(e -> handleSearch());

        // 重置按钮
        Button resetBtn = new Button("重置");
        resetBtn.getStyleClass().add("dialog-button-secondary");
        resetBtn.setOnAction(e -> handleReset());

        // 创建标签
        Label executorLabel = new Label("执行器");
        executorLabel.setStyle(labelStyle);
        Label statusLabel = new Label("任务状态");
        statusLabel.setStyle(labelStyle);
        Label descLabel = new Label("任务描述");
        descLabel.setStyle(labelStyle);
        Label authorLabel = new Label("负责人");
        authorLabel.setStyle(labelStyle);
        Label handlerLabel = new Label("JobHandler");
        handlerLabel.setStyle(labelStyle);

        HBox jobGroupHBox =  new HBox(12, executorLabel, jobGroupCombo);
        jobGroupHBox.setAlignment(Pos.CENTER_LEFT);
        HBox statusHBox =  new HBox(12, statusLabel, statusCombo);
        statusHBox.setAlignment(Pos.CENTER_LEFT);
        HBox descHBox =  new HBox(12, descLabel, jobDescField);
        descHBox.setAlignment(Pos.CENTER_LEFT);
        HBox handlerHBox =  new HBox(12, handlerLabel, handlerField);
        handlerHBox.setAlignment(Pos.CENTER_LEFT);
        HBox authorHBox =  new HBox(12, authorLabel, authorField);
        authorHBox.setAlignment(Pos.CENTER_LEFT);


        HBox btnBox = new HBox(10, searchBtn, resetBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        pane.getChildren().addAll(jobGroupHBox,statusHBox, descHBox, handlerHBox, authorHBox,btnBox);

        return pane;
    }

    /**
     * 搜索处理
     */
    private void handleSearch() {
        pageNum = 1;
        loadPage(true);
    }

    /**
     * 重置筛选条件
     */
    private void handleReset() {
        jobGroupCombo.getSelectionModel().clearSelection();
        jobDescField.clear();
        handlerField.clear();
        authorField.clear();
        statusCombo.getSelectionModel().selectFirst();
        pageNum = 1;
        loadPage(true);
    }

    @SuppressWarnings("unchecked")
    private Node createTable() {
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        // 序号列
        TableColumn<JobInfoVO, Number> idxCol = new TableColumn<>("序号");
        idxCol.setCellValueFactory(c -> Bindings.createIntegerBinding(
                () -> tableView.getItems().indexOf(c.getValue()) + 1));
        idxCol.setCellFactory(col -> new TableCell<JobInfoVO, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item.intValue()));
                setAlignment(Pos.CENTER);
            }
        });
        idxCol.setMinWidth(60);

        // 任务描述列
        TableColumn<JobInfoVO, String> descCol = new TableColumn<>("任务描述");
        descCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getJobDesc())));
        descCol.setCellFactory(col -> new TableCell<JobInfoVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
        descCol.setMinWidth(120);

        // 调度类型列
        TableColumn<JobInfoVO, String> scheduleTypeCol = new TableColumn<>("调度类型");
        scheduleTypeCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getScheduleType())));
        scheduleTypeCol.setCellFactory(col -> new TableCell<JobInfoVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
        scheduleTypeCol.setMinWidth(80);

        // 调度配置列
        TableColumn<JobInfoVO, String> scheduleConfCol = new TableColumn<>("调度配置");
        scheduleConfCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getScheduleConf())));
        scheduleConfCol.setCellFactory(col -> new TableCell<JobInfoVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
        scheduleConfCol.setMinWidth(100);

        // 运行模式列 (glueType)
        TableColumn<JobInfoVO, String> glueTypeCol = new TableColumn<>("运行模式");
        glueTypeCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getGlueType())));
        glueTypeCol.setCellFactory(col -> new TableCell<JobInfoVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
        glueTypeCol.setMinWidth(80);

        // JobHandler列
        TableColumn<JobInfoVO, String> handlerCol = new TableColumn<>("JobHandler");
        handlerCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getExecutorHandler())));
        handlerCol.setCellFactory(col -> new TableCell<JobInfoVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
        handlerCol.setMinWidth(120);

        // 负责人列
        TableColumn<JobInfoVO, String> authorCol = new TableColumn<>("负责人");
        authorCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getAuthor())));
        authorCol.setCellFactory(col -> new TableCell<JobInfoVO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
        authorCol.setMinWidth(80);

        // 任务类型列 - 使用标签显示
        TableColumn<JobInfoVO, Void> typeCol = new TableColumn<>("任务类型");
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                JobInfoVO job = getTableRow().getItem();
                Label tag = createJobTypeTag(job.getJobType());
                setGraphic(tag);
            }
        });
        typeCol.setMinWidth(80);

        // 状态列 - 使用标签显示
        TableColumn<JobInfoVO, Void> statusCol = new TableColumn<>("状态");
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                JobInfoVO job = getTableRow().getItem();
                Label tag = createStatusTag(job.getTriggerStatus());
                setGraphic(tag);
            }
        });
        statusCol.setMinWidth(70);

        // 创建时间列
        TableColumn<JobInfoVO, String> createTimeCol = new TableColumn<>("创建时间");
        createTimeCol.setCellValueFactory(c -> {
            if (c.getValue().getCreateTime() != null) {
                return new SimpleStringProperty(c.getValue().getCreateTime().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
        });
        createTimeCol.setMinWidth(140);

        // 修改时间列
        TableColumn<JobInfoVO, String> updateTimeCol = new TableColumn<>("修改时间");
        updateTimeCol.setCellValueFactory(c -> {
            if (c.getValue().getUpdateTime() != null) {
                return new SimpleStringProperty(c.getValue().getUpdateTime().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
        });
        updateTimeCol.setMinWidth(140);

        // 操作列
        TableColumn<JobInfoVO, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> createActionCell());
        actionCol.setPrefWidth(100);
        actionCol.setMinWidth(100);
        actionCol.setMaxWidth(100);

        // 复选框列 - 放在最前面
        TableColumn<JobInfoVO, Boolean> checkBoxCol = new TableColumn<>("");
        // 创建全选复选框
        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.setOnAction(e -> {
            boolean selected = selectAllCheckBox.isSelected();
            if (selected) {
                selectionModel.selectAll();
            } else {
                selectionModel.clearSelection();
            }
        });
        // 监听选择变化，更新全选复选框状态
        selectionModel.getSelectedItems().addListener((javafx.collections.ListChangeListener.Change<? extends JobInfoVO> c) -> {
            Platform.runLater(() -> {
                int totalItems = tableView.getItems().size();
                int selectedItems = selectionModel.getSelectedItems().size();
                selectAllCheckBox.setSelected(totalItems > 0 && selectedItems == totalItems);
                selectAllCheckBox.setIndeterminate(selectedItems > 0 && selectedItems < totalItems);
            });
        });
        
        checkBoxCol.setGraphic(selectAllCheckBox);
        checkBoxCol.setCellValueFactory(c -> new javafx.beans.property.SimpleBooleanProperty(false));
        checkBoxCol.setCellFactory(col -> new TableCell<JobInfoVO, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            
            {
                checkBox.setOnAction(e -> {
                    JobInfoVO job = getTableRow().getItem();
                    if (job != null) {
                        Boolean selected = checkBox.isSelected();
                        // 同步到表格选择模型
                        if (selected) {
                            selectionModel.select(job);
                        } else {
                            int index = tableView.getItems().indexOf(job);
                            if (index >= 0) {
                                selectionModel.clearSelection(index);
                            }
                        }
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    JobInfoVO job = getTableRow().getItem();
                    // 同步复选框状态与选择模型
                    int index = tableView.getItems().indexOf(job);
                    checkBox.setSelected(index >= 0 && selectionModel.isSelected(index));
                    setGraphic(checkBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });
        checkBoxCol.setPrefWidth(50);
        checkBoxCol.setMinWidth(50);
        checkBoxCol.setMaxWidth(50);
        checkBoxCol.setResizable(false);
        checkBoxCol.setSortable(false);

        tableView.getColumns().addAll(checkBoxCol, idxCol, descCol, scheduleTypeCol, scheduleConfCol,
                glueTypeCol, handlerCol, authorCol, typeCol, statusCol, 
                createTimeCol, updateTimeCol, actionCol);

        return tableView;
    }

    /**
     * 创建任务类型标签
     */
    private Label createJobTypeTag(Integer jobType) {
        Label tag = new Label();
        if (jobType != null && (jobType == 0 || jobType == 2)) {
            if (jobType == 2) {
                tag.setText("任务组");
                tag.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; " +
                        "-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            } else {
                tag.setText("任务");
                tag.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; " +
                        "-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            }
        } else {
            tag.setText("任务");
            tag.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; " +
                    "-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px;");
        }
        return tag;
    }

    /**
     * 创建状态标签
     */
    private Label createStatusTag(Integer status) {
        Label tag = new Label();
        if (status != null && status == 1) {
            tag.setText("运行");
            tag.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; " +
                    "-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px;");
        } else {
            tag.setText("停止");
            tag.setStyle("-fx-background-color: #9CA3AF; -fx-text-fill: white; " +
                    "-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px;");
        }
        return tag;
    }

    /**
     * 创建操作列单元格
     */
    private TableCell<JobInfoVO, Void> createActionCell() {
        return new TableCell<>() {
            private final MenuButton actionMenuBtn = new MenuButton("操作");
            private final MenuItem runItem = new MenuItem("执行一次");
            private final MenuItem logItem = new MenuItem("查询日志");
            private final MenuItem nextTimeItem = new MenuItem("下次执行时间");
            private final SeparatorMenuItem sep1 = new SeparatorMenuItem();
            private final MenuItem startItem = new MenuItem("启动");
            private final MenuItem stopItem = new MenuItem("停止");
            private final SeparatorMenuItem sep2 = new SeparatorMenuItem();
            private final MenuItem editItem = new MenuItem("编辑");
            private final MenuItem deleteItem = new MenuItem("删除");
            private final MenuItem copyItem = new MenuItem("复制");

            {
                actionMenuBtn.getItems().addAll(runItem, logItem, nextTimeItem, sep1,
                        startItem, stopItem, sep2, editItem, deleteItem, copyItem);
                actionMenuBtn.setPrefWidth(90);
                actionMenuBtn.setMinWidth(90);
                actionMenuBtn.setMaxWidth(90);
                actionMenuBtn.setPrefHeight(26);
                actionMenuBtn.setGraphicTextGap(4);
                actionMenuBtn.getStyleClass().add("dialog-table-action-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setAlignment(null);
                    setPadding(Insets.EMPTY);
                    return;
                }

                final JobInfoVO currentJob = getTableRow().getItem();

                // 根据状态设置菜单项禁用状态
                boolean running = currentJob.getTriggerStatus() != null && currentJob.getTriggerStatus() == 1;
                boolean isTaskGroup = currentJob.getJobType() != null && currentJob.getJobType() == 2;

                // 执行一次：任务组不支持，运行中禁用
                runItem.setDisable(running || isTaskGroup);
                startItem.setDisable(running);
                stopItem.setDisable(!running);
                editItem.setDisable(running);
                deleteItem.setDisable(running);

                // 绑定事件处理器
                runItem.setOnAction(e -> handleRun(currentJob));
                logItem.setOnAction(e -> handleViewLog(currentJob));
                nextTimeItem.setOnAction(e -> handleNextTriggerTime(currentJob));
                startItem.setOnAction(e -> handleStart(currentJob));
                stopItem.setOnAction(e -> handleStop(currentJob));
                editItem.setOnAction(e -> handleEdit(currentJob));
                deleteItem.setOnAction(e -> handleDelete(currentJob));
                copyItem.setOnAction(e -> handleCopy(currentJob));

                // 设置单元格上下间距
                setPadding(new Insets(4, 0, 4, 0));
                setGraphic(actionMenuBtn);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private Node createPagerBar() {
        totalLabel = new Label("共 0 条");
        totalLabel.setStyle(StyleUtil.bodyFontOnly());

        prevBtn = new Button("上一页");
        prevBtn.getStyleClass().add("dialog-button-secondary");
        nextBtn = new Button("下一页");
        nextBtn.getStyleClass().add("dialog-button-secondary");
        
        pageField = new TextField(String.valueOf(pageNum));
        pageField.setPrefWidth(60);
        pageField.setOnAction(e -> {
            try {
                int p = Integer.parseInt(pageField.getText().trim());
                pageNum = Math.max(p, 1);
                loadPage(false);
            } catch (NumberFormatException ex) {
                pageField.setText(String.valueOf(pageNum));
            }
        });

        pageSizeBox = new ComboBox<>(FXCollections.observableArrayList(10, 20, 50));
        pageSizeBox.getSelectionModel().select(Integer.valueOf(pageSize));
        pageSizeBox.setOnAction(e -> {
            Integer size = pageSizeBox.getValue();
            if (size != null) {
                pageSize = size;
                pageNum = 1;
                loadPage(false);
            }
        });

        prevBtn.setOnAction(e -> {
            if (pageNum > 1) {
                pageNum--;
                loadPage(false);
            }
        });
        nextBtn.setOnAction(e -> {
            long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
            if (pageNum < maxPage) {
                pageNum++;
                loadPage(false);
            }
        });

        Label pageSizeLabel = new Label("每页");
        pageSizeLabel.setStyle(StyleUtil.bodyFontOnly());
        Label pageLabel = new Label("页码");
        pageLabel.setStyle(StyleUtil.bodyFontOnly());

        HBox pager = new HBox(12,
                totalLabel,
                pageSizeLabel, pageSizeBox,
                prevBtn,
                pageLabel, pageField,
                nextBtn
        );
        pager.getStyleClass().add("dialog-section");
        pager.setAlignment(Pos.CENTER_LEFT);
        pager.setPadding(new Insets(16, 16, 16, 16));
        return pager;
    }

    private void loadPage(boolean showLoading) {
        if (loading.getAndSet(true)) {
            return;
        }
        if (showLoading) {
            totalLabel.setText("加载中...");
        }

        JobInfoQuery query = new JobInfoQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        
        // 执行器筛选
        JobGroup selectedGroup = jobGroupCombo.getValue();
        if (selectedGroup != null) {
            query.setJobGroup(selectedGroup.getId());
        }
        
        // 任务描述筛选
        if (!isBlank(jobDescField.getText())) {
            query.setJobDesc(jobDescField.getText().trim());
        }
        
        // JobHandler筛选
        if (!isBlank(handlerField.getText())) {
            query.setExecutorHandler(handlerField.getText().trim());
        }
        
        // 负责人筛选
        if (!isBlank(authorField.getText())) {
            query.setAuthor(authorField.getText().trim());
        }
        
        // 状态筛选
        String status = statusCombo.getValue();
        if ("运行".equals(status)) {
            query.setTriggerStatus(1);
        } else if ("停止".equals(status)) {
            query.setTriggerStatus(0);
        }

        new Thread(() -> {
            try {
                PageResult<JobInfoVO> page = jobInfoService.getJobInfoPage(query);
                List<JobInfoVO> list = page != null && page.getData() != null
                        ? page.getData().getList() : Collections.emptyList();
                total = page != null && page.getData() != null ? page.getData().getTotal() : 0;
                Platform.runLater(() -> {
                    tableView.setItems(FXCollections.observableArrayList(list));
                    totalLabel.setText("共 " + total + " 条");
                    pageField.setText(String.valueOf(pageNum));
                    updatePagerButtons();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    totalLabel.setText("加载失败");
                    showError("加载任务列表失败", ex.getMessage());
                });
            } finally {
                loading.set(false);
            }
        }, "load-job-list").start();
    }

    /**
     * 加载执行器列表
     */
    private void loadJobGroupList() {
        new Thread(() -> {
            try {
                jobGroupList = jobGroupService.getAllJobGroupList();
                Platform.runLater(() -> {
                    ObservableList<JobGroup> items = FXCollections.observableArrayList();
                    items.add(null); // 第一项为"全部"
                    if (jobGroupList != null) {
                        items.addAll(jobGroupList);
                    }
                    jobGroupCombo.setItems(items);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("加载执行器列表失败", ex.getMessage());
                });
            }
        }, "load-job-groups").start();
    }

    private void updatePagerButtons() {
        long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
        prevBtn.setDisable(pageNum <= 1);
        nextBtn.setDisable(pageNum >= maxPage || maxPage == 0);
    }

    private void handleRun(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        
        // 显示执行参数输入对话框
        TextInputDialog dialog = new TextInputDialog(safe(job.getExecutorParam()));
        dialog.setTitle("执行一次");
        dialog.setHeaderText("任务: " + safe(job.getJobDesc()));
        dialog.setContentText("执行参数:");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        
        dialog.showAndWait().ifPresent(param -> {
            runAsync("执行任务", () -> {
                jobInfoService.triggerOnce(job.getId(), param);
                return "触发成功";
            });
        });
    }

    /**
     * 查看任务日志
     */
    private void handleViewLog(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        try {
            // 打开日志列表对话框，传递任务ID以只显示该任务的日志
            ShowJobLogListDialog logDialog = new ShowJobLogListDialog(ownerStage, job.getId());
            logDialog.showAndWait();
        } catch (Exception e) {
            showError("打开日志对话框失败", e.getMessage());
        }
    }

    /**
     * 查看下次执行时间
     */
    private void handleNextTriggerTime(JobInfoVO job) {
        if (job == null) return;
        String scheduleType = job.getScheduleType();
        String scheduleConf = job.getScheduleConf();
        
        if (isBlank(scheduleType) || isBlank(scheduleConf)) {
            showInfo("该任务未配置调度");
            return;
        }
        
        new Thread(() -> {
            try {
                List<String> times = jobInfoService.getNextTriggerTime(scheduleType, scheduleConf);
                Platform.runLater(() -> {
                    if (times == null || times.isEmpty()) {
                        showInfo("没有下次执行时间");
                    } else {
                        showNextTimeDialog(job, times);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("获取下次执行时间失败", ex.getMessage()));
            }
        }, "get-next-time").start();
    }

    /**
     * 显示下次执行时间对话框
     */
    private void showNextTimeDialog(JobInfoVO job, List<String> times) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("下次执行时间");
        alert.setHeaderText("任务: " + safe(job.getJobDesc()));
        alert.setContentText("下次执行时间:\n" + String.join("\n", times));
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }

    private void handleStart(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        runAsync("启动任务", () -> {
            boolean ok = jobInfoService.startJob(job.getId());
            return ok ? "启动成功" : "启动失败";
        });
    }

    private void handleStop(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        runAsync("停止任务", () -> {
            boolean ok = jobInfoService.stopJob(job.getId());
            return ok ? "停止成功" : "停止失败";
        });
    }

    /**
     * 编辑任务
     */
    private void handleEdit(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        
        // 异步加载任务表单数据
        totalLabel.setText("加载任务数据...");
        new Thread(() -> {
            try {
                // 获取任务表单数据
                JobInfoForm formData = jobInfoService.getFormData(job.getId());
                // 获取执行器列表
                List<JobGroup> groupList = jobGroupService.getAllJobGroupList();
                
                Platform.runLater(() -> {
                    totalLabel.setText("共 " + total + " 条");
                    
                    // 根据任务类型打开不同的对话框
                    boolean isTaskGroup = job.getJobType() != null && job.getJobType() == 2;
                    
                    if (isTaskGroup) {
                        // 任务组 - 使用 NewJobGroupDialog
                        NewJobGroupDialog dialog = new NewJobGroupDialog(ownerStage, 
                                formData.getJobPartId() != null ? formData.getJobPartId().longValue() : null, 
                                formData, groupList);
                        dialog.showAndWait().ifPresent(result -> {
                            if (result != null) {
                                saveEditedJob(job.getId(), result, true);
                            }
                        });
                    } else {
                        // 普通任务 - 使用 NewJobDialog
                        NewJobDialog dialog = new NewJobDialog(ownerStage, formData, groupList);
                        dialog.showAndWait().ifPresent(result -> {
                            if (result != null) {
                                saveEditedJob(job.getId(), result, false);
                            }
                        });
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    totalLabel.setText("共 " + total + " 条");
                    showError("加载任务数据失败", ex.getMessage());
                });
            }
        }, "load-job-form").start();
    }
    
    /**
     * 保存编辑后的任务
     */
    private void saveEditedJob(Long id, JobInfoForm formData, boolean isTaskGroup) {
        runAsync("保存任务", () -> {
            formData.setId(id);
            if (isTaskGroup) {
                boolean success = jobInfoService.updateJobCompose(id, formData);
                return success ? "保存成功" : "保存失败";
            } else {
                boolean success = jobInfoService.updateJobInfo(id, formData);
                return success ? "保存成功" : "保存失败";
            }
        });
    }

    /**
     * 删除单个任务
     */
    private void handleDelete(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "确定要删除任务 \"" + safe(job.getJobDesc()) + "\" 吗？", 
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("确认删除");
        confirm.initOwner(getDialogPane().getScene().getWindow());
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.YES) {
                runAsync("删除任务", () -> {
                    boolean success = jobInfoService.deleteByIds(String.valueOf(job.getId()));
                    return success ? "删除成功" : "删除失败";
                });
            }
        });
    }

    /**
     * 复制任务
     */
    private void handleCopy(JobInfoVO job) {
        if (job == null || job.getId() == null) return;
        
        // 异步加载任务表单数据
        totalLabel.setText("加载任务数据...");
        new Thread(() -> {
            try {
                // 获取任务表单数据
                JobInfoForm formData = jobInfoService.getFormData(job.getId());
                // 清除ID，作为新任务创建
                formData.setId(null);
                formData.setJobDesc(formData.getJobDesc() + " - 复制");
                
                // 获取执行器列表
                List<JobGroup> groupList = jobGroupService.getAllJobGroupList();
                
                Platform.runLater(() -> {
                    totalLabel.setText("共 " + total + " 条");
                    
                    // 根据任务类型打开不同的对话框
                    boolean isTaskGroup = job.getJobType() != null && job.getJobType() == 2;
                    
                    if (isTaskGroup) {
                        // 任务组 - 使用 NewJobGroupDialog
                        NewJobGroupDialog dialog = new NewJobGroupDialog(ownerStage, 
                                formData.getJobPartId() != null ? formData.getJobPartId().longValue() : null, 
                                formData, groupList);
                        dialog.setTitle("复制任务组");
                        dialog.showAndWait().ifPresent(result -> {
                            if (result != null) {
                                saveNewJob(result, true);
                            }
                        });
                    } else {
                        // 普通任务 - 使用 NewJobDialog
                        NewJobDialog dialog = new NewJobDialog(ownerStage, formData, groupList);
                        dialog.setTitle("复制任务");
                        dialog.showAndWait().ifPresent(result -> {
                            if (result != null) {
                                saveNewJob(result, false);
                            }
                        });
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    totalLabel.setText("共 " + total + " 条");
                    showError("加载任务数据失败", ex.getMessage());
                });
            }
        }, "copy-job").start();
    }

    /**
     * 新增任务
     */
    private void handleAdd() {
        // 异步加载执行器列表
        totalLabel.setText("加载数据...");
        new Thread(() -> {
            try {
                List<JobGroup> groupList = jobGroupService.getAllJobGroupList();
                Platform.runLater(() -> {
                    totalLabel.setText("共 " + total + " 条");
                    // 打开新建任务对话框
                    NewJobDialog dialog = new NewJobDialog(ownerStage, null, groupList);
                    dialog.showAndWait().ifPresent(result -> {
                        if (result != null) {
                            saveNewJob(result, false);
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    totalLabel.setText("共 " + total + " 条");
                    showError("加载执行器列表失败", ex.getMessage());
                });
            }
        }, "load-groups-for-add").start();
    }
    
    /**
     * 保存新建任务
     */
    private void saveNewJob(JobInfoForm formData, boolean isTaskGroup) {
        runAsync("保存任务", () -> {
            if (isTaskGroup) {
                boolean success = jobInfoService.saveJobCompose(formData);
                return success ? "创建成功" : "创建失败";
            } else {
                long id = jobInfoService.saveJobInfo(formData);
                return id > 0 ? "创建成功" : "创建失败";
            }
        });
    }

    /**
     * 批量删除任务
     */
    private void handleBatchDelete() {
        List<JobInfoVO> selected = selectionModel.getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showError("提示", "请选择要删除的任务");
            return;
        }
        
        // 检查是否有运行中的任务
        List<JobInfoVO> runningJobs = selected.stream()
                .filter(j -> j.getTriggerStatus() != null && j.getTriggerStatus() == 1)
                .collect(Collectors.toList());
        if (!runningJobs.isEmpty()) {
            showError("提示", "选中的任务中包含运行中的任务，请先停止");
            return;
        }
        
        String names = selected.stream()
                .map(j -> safe(j.getJobDesc()))
                .limit(3)
                .collect(Collectors.joining(", "));
        if (selected.size() > 3) {
            names += " 等" + selected.size() + "个任务";
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "确定要删除以下任务吗？\n" + names, 
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("确认批量删除");
        confirm.initOwner(getDialogPane().getScene().getWindow());
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.YES) {
                String ids = selected.stream()
                        .map(j -> String.valueOf(j.getId()))
                        .collect(Collectors.joining(","));
                runAsync("批量删除任务", () -> {
                    boolean success = jobInfoService.deleteByIds(ids);
                    return success ? "删除成功" : "删除失败";
                });
            }
        });
    }

    private void runAsync(String title, java.util.concurrent.Callable<String> task) {
        if (loading.getAndSet(true)) {
            return;
        }
        totalLabel.setText(title + "...");
        new Thread(() -> {
            try {
                String msg = task.call();
                Platform.runLater(() -> {
                    showInfo(msg);
                    loading.set(false);
                    loadPage(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(title + "失败", ex.getMessage()));
            } finally {
                if (loading.get()) {
                    loading.set(false);
                }
            }
        }, "job-action").start();
    }


    private String safe(String v) {
        return v == null ? "" : v;
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle("提示");
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
}
