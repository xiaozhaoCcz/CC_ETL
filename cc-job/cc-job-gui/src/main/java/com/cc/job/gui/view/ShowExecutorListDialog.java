package com.cc.job.gui.view;

import com.cc.job.gui.service.JobGroupService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.form.JobGroupForm;
import com.cc.job.xo.model.query.JobGroupQuery;
import com.cc.job.xo.model.vo.JobGroupVO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 展示执行器列表
 */
public class ShowExecutorListDialog extends Dialog<Void> {

    private final JobGroupService jobGroupService = new JobGroupService();

    private TableView<JobGroupVO> tableView;
    private TableView.TableViewSelectionModel<JobGroupVO> selectionModel;
    private TextField appNameField;
    private TextField titleField;

    private Label totalLabel;
    private TextField pageField;
    private ComboBox<Integer> pageSizeBox;
    private Button prevBtn;
    private Button nextBtn;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int pageNum = 1;
    private int pageSize = 10;
    private long total = 0;

    public ShowExecutorListDialog(Stage ownerStage) {
        setTitle("执行器管理");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

        root.setTop(createFilterBar());
        root.setCenter(createTable());
        root.setBottom(createPagerBar());

        getDialogPane().setContent(root);

        // 初始加载
        loadPage(true);
    }

    /**
     * 设置对话框样式
     */
    private void styleDialog() {
        getDialogPane().setPrefSize(1200, 700);
        getDialogPane().setPrefWidth(1200);
        getDialogPane().setPrefHeight(700);
        getDialogPane().setMinWidth(1200);
        getDialogPane().setMinHeight(700);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);

        String dialogCss = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            getDialogPane().getStylesheets().add(dialogCss);
        }

        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(1200);
                stage.setMinHeight(700);
                
                // 加载当前主题样式
                String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                if (css != null && !css.isEmpty()) {
                    stage.getScene().getStylesheets().add(css);
                }
                
                stage.setOnCloseRequest(event -> {
                    close();
                });
            }
        });
    }

    private Node createFilterBar() {
        VBox container = new VBox(12);
        container.getStyleClass().add("dialog-section");
        container.setPadding(new Insets(16, 16, 16, 16));

        // 过滤输入区域
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        String labelStyle = StyleUtil.bodyFontOnly();

        appNameField = new TextField();
        appNameField.setPromptText("请输入AppName");
        appNameField.getStyleClass().add("dialog-search-field");

        titleField = new TextField();
        titleField.setPromptText("请输入执行器名称");
        titleField.getStyleClass().add("dialog-search-field");

        Button searchBtn = new Button("搜索");
        searchBtn.getStyleClass().add("dialog-button-primary");
        searchBtn.setOnAction(e -> {
            pageNum = 1;
            loadPage(true);
        });

        Button resetBtn = new Button("重置");
        resetBtn.getStyleClass().add("dialog-button-secondary");
        resetBtn.setOnAction(e -> {
            appNameField.clear();
            titleField.clear();
            pageNum = 1;
            loadPage(true);
        });

        Label appNameLabel = new Label("AppName");
        appNameLabel.setStyle(labelStyle);
        Label titleLabel = new Label("执行器名称");
        titleLabel.setStyle(labelStyle);

        grid.add(appNameLabel, 0, 0);
        grid.add(appNameField, 1, 0);
        grid.add(titleLabel, 2, 0);
        grid.add(titleField, 3, 0);

        HBox btnBox = new HBox(10, searchBtn, resetBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(btnBox, 4, 0);

        // 操作按钮区域
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("+ 新增");
        addBtn.getStyleClass().add("dialog-button-success");
        addBtn.setOnAction(e -> handleAdd());

        Button deleteBtn = new Button("删除");
        deleteBtn.getStyleClass().add("dialog-button-error");
        deleteBtn.setOnAction(e -> handleDelete());

        actionBox.getChildren().addAll(addBtn, deleteBtn);

        container.getChildren().addAll(grid, actionBox);

        return container;
    }

    @SuppressWarnings("unchecked")
    private Node createTable() {
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        // 复选框列
        TableColumn<JobGroupVO, Boolean> checkCol = new TableColumn<>();
        checkCol.setCellValueFactory(c -> Bindings.createBooleanBinding(() -> 
                selectionModel.isSelected(tableView.getItems().indexOf(c.getValue())),
                selectionModel.selectedItemProperty()));
        checkCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    if (checkBox.isSelected()) {
                        selectionModel.select(getIndex());
                    } else {
                        selectionModel.clearSelection(getIndex());
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(selectionModel.isSelected(getIndex()));
                    setGraphic(checkBox);
                }
            }
        });
        checkCol.setMaxWidth(50);
        checkCol.setMinWidth(50);

        // 序号列
        TableColumn<JobGroupVO, Number> idxCol = new TableColumn<>("序号");
        idxCol.setCellValueFactory(c -> Bindings.createIntegerBinding(
                () -> tableView.getItems().indexOf(c.getValue()) + 1 + (pageNum - 1) * pageSize));
        idxCol.setMaxWidth(80);

        // AppName列
        TableColumn<JobGroupVO, String> appNameCol = new TableColumn<>("执行器AppName");
        appNameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getAppName())));

        // 执行器名称列
        TableColumn<JobGroupVO, String> titleCol = new TableColumn<>("执行器名称");
        titleCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getTitle())));

        // 执行器类型列
        TableColumn<JobGroupVO, String> typeCol = new TableColumn<>("执行器类型");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                mapAddressType(c.getValue().getAddressType())));

        // 在线机器地址列
        TableColumn<JobGroupVO, String> addressCol = new TableColumn<>("OnLine机器地址");
        addressCol.setCellValueFactory(c -> {
            String addressList = safe(c.getValue().getAddressList());
            if (addressList == null || addressList.isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty("无");
            } else {
                return new javafx.beans.property.SimpleStringProperty("查看");
            }
        });
        addressCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink viewLink = new Hyperlink("查看");
            {
                viewLink.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                viewLink.setOnAction(e -> {
                    JobGroupVO item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        handleViewAddress(item);
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "无".equals(item)) {
                    setGraphic(null);
                    setText(empty ? null : item);
                } else {
                    setGraphic(viewLink);
                    setText(null);
                }
            }
        });

        // 操作列
        TableColumn<JobGroupVO, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final HBox actionBox = new HBox(8);
            private final Hyperlink viewLink = new Hyperlink("查看");
            private final Hyperlink editLink = new Hyperlink("编辑");
            private final Hyperlink deleteLink = new Hyperlink("删除");
            {
                viewLink.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                editLink.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                deleteLink.setStyle("-fx-text-fill: " + StyleUtil.ERROR + ";");
                
                viewLink.setOnAction(e -> {
                    JobGroupVO item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        handleViewAddress(item);
                    }
                });
                editLink.setOnAction(e -> {
                    JobGroupVO item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        handleEdit(item);
                    }
                });
                deleteLink.setOnAction(e -> {
                    JobGroupVO item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        handleDeleteSingle(item);
                    }
                });
                
                actionBox.getChildren().addAll(viewLink, editLink, deleteLink);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionBox);
                }
            }
        });
        actionCol.setPrefWidth(150);
        actionCol.setMinWidth(150);

        tableView.getColumns().addAll(checkCol, idxCol, appNameCol, titleCol, typeCol, addressCol, actionCol);

        return tableView;
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
        pageField.getStyleClass().add("dialog-search-field");
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

        JobGroupQuery query = new JobGroupQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        if (!isBlank(appNameField.getText())) {
            query.setAppName(appNameField.getText().trim());
        }
        if (!isBlank(titleField.getText())) {
            query.setTitle(titleField.getText().trim());
        }

        new Thread(() -> {
            try {
                PageResult<JobGroupVO> page = jobGroupService.getJobGroupPage(query);
                List<JobGroupVO> list = page != null && page.getData() != null
                        ? page.getData().getList() : Collections.emptyList();
                total = page != null && page.getData() != null ? page.getData().getTotal() : 0;
                Platform.runLater(() -> {
                    tableView.setItems(FXCollections.observableArrayList(list));
                    totalLabel.setText("共 " + total + " 条，当前页 " + pageNum);
                    pageField.setText(String.valueOf(pageNum));
                    updatePagerButtons();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    totalLabel.setText("加载失败: " + ex.getMessage());
                    showError("加载执行器列表失败", ex.getMessage());
                });
            } finally {
                loading.set(false);
            }
        }, "load-executor-list").start();
    }

    private void updatePagerButtons() {
        long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
        prevBtn.setDisable(pageNum <= 1);
        nextBtn.setDisable(pageNum >= maxPage || maxPage == 0);
    }

    private void handleAdd() {
        showExecutorFormDialog(null);
    }
    
    /**
     * 显示执行器表单对话框（新增/编辑）
     */
    private void showExecutorFormDialog(JobGroupVO editItem) {
        Dialog<JobGroupForm> dialog = new Dialog<>();
        dialog.setTitle(editItem == null ? "新增执行器" : "编辑执行器");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        
        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        
        String labelStyle = StyleUtil.bodyFontOnly();
        
        // AppName
        Label appNameLabel = new Label("AppName");
        appNameLabel.setStyle(labelStyle);
        TextField appNameInput = new TextField();
        appNameInput.setPrefWidth(300);
        appNameInput.setPromptText("请输入执行器AppName");
        appNameInput.getStyleClass().add("dialog-search-field");
        
        // 执行器名称
        Label titleLabel = new Label("执行器名称");
        titleLabel.setStyle(labelStyle);
        TextField titleInput = new TextField();
        titleInput.setPrefWidth(300);
        titleInput.setPromptText("请输入执行器名称");
        titleInput.getStyleClass().add("dialog-search-field");
        
        // 注册方式
        Label typeLabel = new Label("注册方式");
        typeLabel.setStyle(labelStyle);
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("自动注册", "手动录入");
        typeCombo.getSelectionModel().selectFirst();
        typeCombo.setPrefWidth(300);
        
        // 机器地址
        Label addressLabel = new Label("机器地址");
        addressLabel.setStyle(labelStyle);
        TextArea addressInput = new TextArea();
        addressInput.setPrefWidth(300);
        addressInput.setPrefRowCount(3);
        addressInput.setPromptText("多个地址换行分隔");
        addressInput.setWrapText(true);
        
        // 监听注册方式变化，控制地址输入框可用性
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            addressInput.setDisable("自动注册".equals(newVal));
        });
        addressInput.setDisable(true); // 默认自动注册，禁用地址输入
        
        grid.add(appNameLabel, 0, 0);
        grid.add(appNameInput, 1, 0);
        grid.add(titleLabel, 0, 1);
        grid.add(titleInput, 1, 1);
        grid.add(typeLabel, 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(addressLabel, 0, 3);
        grid.add(addressInput, 1, 3);
        
        // 填充编辑数据
        if (editItem != null) {
            appNameInput.setText(safe(editItem.getAppName()));
            titleInput.setText(safe(editItem.getTitle()));
            Integer addrType = editItem.getAddressType();
            if (addrType != null && addrType == 1) {
                typeCombo.getSelectionModel().select("手动录入");
                addressInput.setDisable(false);
            }
            addressInput.setText(safe(editItem.getAddressList()));
        }
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveBtn = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, cancelBtn);
        
        // 设置按钮样式
        Button saveBtnNode = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        if (saveBtnNode != null) {
            saveBtnNode.getStyleClass().add("dialog-button-primary");
        }
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveBtn) {
                // 验证
                if (isBlank(appNameInput.getText())) {
                    showError("验证失败", "请输入执行器AppName");
                    return null;
                }
                if (isBlank(titleInput.getText())) {
                    showError("验证失败", "请输入执行器名称");
                    return null;
                }
                
                JobGroupForm form = new JobGroupForm();
                if (editItem != null) {
                    form.setId(editItem.getId());
                }
                form.setAppName(appNameInput.getText().trim());
                form.setTitle(titleInput.getText().trim());
                form.setAddressType("手动录入".equals(typeCombo.getValue()) ? 1 : 0);
                form.setAddressList(addressInput.getText() != null ? 
                        addressInput.getText().trim().replace("\n", ",") : "");
                return form;
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(form -> {
            if (form != null) {
                saveExecutor(editItem != null, editItem != null ? editItem.getId() : null, form);
            }
        });
    }
    
    /**
     * 保存执行器
     */
    private void saveExecutor(boolean isEdit, Long id, JobGroupForm form) {
        runAsync("保存执行器", () -> {
            boolean ok;
            if (isEdit) {
                ok = jobGroupService.updateJobGroup(id, form);
            } else {
                ok = jobGroupService.saveJobGroup(form);
            }
            return ok ? "保存成功" : "保存失败";
        });
    }

    private void handleDelete() {
        List<JobGroupVO> selected = selectionModel.getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showError("提示", "请选择要删除的执行器");
            return;
        }
        
        String ids = selected.stream()
                .map(item -> String.valueOf(item.getId()))
                .collect(Collectors.joining(","));
        
        runAsync("删除执行器", () -> {
            boolean ok = jobGroupService.deleteJobGroups(ids);
            return ok ? "删除成功" : "删除失败";
        });
    }

    private void handleDeleteSingle(JobGroupVO item) {
        if (item == null || item.getId() == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "确定要删除执行器 \"" + safe(item.getTitle()) + "\" 吗？", 
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("确认删除");
        confirm.initOwner(getDialogPane().getScene().getWindow());
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.YES) {
                runAsync("删除执行器", () -> {
                    boolean ok = jobGroupService.deleteJobGroups(String.valueOf(item.getId()));
                    return ok ? "删除成功" : "删除失败";
                });
            }
        });
    }

    private void handleEdit(JobGroupVO item) {
        if (item == null || item.getId() == null) return;
        showExecutorFormDialog(item);
    }

    private void handleViewAddress(JobGroupVO item) {
        if (item == null || item.getId() == null) return;
        
        runAsync("加载地址列表", () -> {
            try {
                List<String> addresses = jobGroupService.findAddressList(item.getId());
                Platform.runLater(() -> showAddressDialog(item, addresses));
                return "加载成功";
            } catch (Exception e) {
                throw new RuntimeException("加载地址列表失败: " + e.getMessage(), e);
            }
        });
    }

    private void showAddressDialog(JobGroupVO item, List<String> addresses) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("注册节点 - " + safe(item.getTitle()));
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);

        if (addresses == null || addresses.isEmpty()) {
            Label emptyLabel = new Label("暂无注册节点");
            emptyLabel.setStyle(StyleUtil.bodyFontOnly());
            content.getChildren().add(emptyLabel);
        } else {
            for (String address : addresses) {
                Label addressLabel = new Label(address);
                addressLabel.setStyle(StyleUtil.bodyFontOnly());
                content.getChildren().add(addressLabel);
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
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
                Platform.runLater(() -> {
                    showError(title + "失败", ex.getMessage());
                    loading.set(false);
                });
            }
        }, "executor-action").start();
    }

    private String mapAddressType(Integer type) {
        if (type == null) return "未知";
        return type == 0 ? "自动注册" : "手动录入";
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

