package com.cc.job.gui.view;

import com.cc.job.gui.service.JobJdbcDatasourceService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.form.JobJdbcDatasourceForm;
import com.cc.job.xo.model.query.JobJdbcDatasourceQuery;
import com.cc.job.xo.model.vo.JobJdbcDatasourceVO;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 数据源管理对话框
 */
public class ShowDatasourceListDialog extends Dialog<Void> {

    private final JobJdbcDatasourceService datasourceService = new JobJdbcDatasourceService();

    private TableView<JobJdbcDatasourceVO> tableView;
    private TableView.TableViewSelectionModel<JobJdbcDatasourceVO> selectionModel;
    private TextField datasourceNameField;
    private ComboBox<String> datasourceTypeCombo;
    private TextField databaseNameField;

    private Label totalLabel;
    private TextField pageField;
    private ComboBox<Integer> pageSizeBox;
    private Button prevBtn;
    private Button nextBtn;

    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int pageNum = 1;
    private int pageSize = 10;
    private long total = 0;

    private static final String[] DATASOURCE_TYPES = {"", "MYSQL", "ORACLE", "POSTGRESQL"};
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ShowDatasourceListDialog(Stage ownerStage) {
        setTitle("数据源管理");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + StyleUtil.BG_SECONDARY + ";");

        root.setTop(createFilterBar());
        root.setCenter(createTable());
        root.setBottom(createPagerBar());

        getDialogPane().setContent(root);

        // 初始加载
        loadPage(true);
    }

    private void styleDialog() {
        getDialogPane().setPrefSize(1300, 700);
        getDialogPane().setPrefWidth(1300);
        getDialogPane().setPrefHeight(700);
        getDialogPane().setMinWidth(1200);
        getDialogPane().setMinHeight(700);
        getDialogPane().setMaxWidth(Double.MAX_VALUE);
        getDialogPane().setMaxHeight(Double.MAX_VALUE);
        setResizable(true);

        

        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                stage.setMinWidth(1200);
                stage.setMinHeight(700);
                String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
                if (css != null && !css.isEmpty()) {
                    stage.getScene().getStylesheets().add(css);
                }
                stage.setOnCloseRequest(event -> close());
            }
        });
    }

    private Node createFilterBar() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(16, 16, 16, 16));
        container.setStyle(
                "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_LG + ";"
        );

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        String labelStyle = StyleUtil.body();

        datasourceNameField = new TextField();
        datasourceNameField.setPromptText("请输入数据源名称");
        datasourceNameField.setStyle(StyleUtil.searchField());

        datasourceTypeCombo = new ComboBox<>(FXCollections.observableArrayList(DATASOURCE_TYPES));
        datasourceTypeCombo.getSelectionModel().selectFirst();
        datasourceTypeCombo.setPromptText("数据源类型");

        databaseNameField = new TextField();
        databaseNameField.setPromptText("请输入数据库名");
        databaseNameField.setStyle(StyleUtil.searchField());

        Button searchBtn = new Button("搜索");
        searchBtn.setStyle(StyleUtil.primaryButton());
        StyleUtil.applyPrimaryButtonHover(searchBtn);
        searchBtn.setOnAction(e -> {
            pageNum = 1;
            loadPage(true);
        });

        Button resetBtn = new Button("重置");
        resetBtn.setStyle(StyleUtil.secondaryButton());
        resetBtn.setOnAction(e -> {
            datasourceNameField.clear();
            datasourceTypeCombo.getSelectionModel().selectFirst();
            databaseNameField.clear();
            pageNum = 1;
            loadPage(true);
        });

        Label nameLabel = new Label("数据源名称");
        nameLabel.setStyle(labelStyle);
        Label typeLabel = new Label("数据源类型");
        typeLabel.setStyle(labelStyle);
        Label dbLabel = new Label("数据库名");
        dbLabel.setStyle(labelStyle);

        grid.add(nameLabel, 0, 0);
        grid.add(datasourceNameField, 1, 0);
        grid.add(typeLabel, 2, 0);
        grid.add(datasourceTypeCombo, 3, 0);
        grid.add(dbLabel, 4, 0);
        grid.add(databaseNameField, 5, 0);

        HBox btnBox = new HBox(10, searchBtn, resetBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(btnBox, 6, 0);

        // 操作按钮区域
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Button addBtn = new Button("+ 新增");
        addBtn.setStyle(StyleUtil.successButton());
        StyleUtil.applySuccessButtonHover(addBtn);
        addBtn.setOnAction(e -> handleAdd());

        Button deleteBtn = new Button("删除");
        deleteBtn.setStyle(StyleUtil.errorButton());
        StyleUtil.applyErrorButtonHover(deleteBtn);
        deleteBtn.setOnAction(e -> handleBatchDelete());

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
        TableColumn<JobJdbcDatasourceVO, Boolean> checkCol = new TableColumn<>();
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
        TableColumn<JobJdbcDatasourceVO, Number> idxCol = new TableColumn<>("序号");
        idxCol.setCellValueFactory(c -> Bindings.createIntegerBinding(
                () -> tableView.getItems().indexOf(c.getValue()) + 1 + (pageNum - 1) * pageSize));
        idxCol.setMaxWidth(60);

        // 数据源名称列
        TableColumn<JobJdbcDatasourceVO, String> nameCol = new TableColumn<>("数据源名称");
        nameCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getDatasourceName())));

        // 数据源类型列
        TableColumn<JobJdbcDatasourceVO, String> typeCol = new TableColumn<>("数据源");
        typeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getDatasource())));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = switch (item) {
                        case "MYSQL" -> StyleUtil.SUCCESS;
                        case "ORACLE" -> StyleUtil.WARNING;
                        case "POSTGRESQL" -> StyleUtil.PRIMARY;
                        default -> StyleUtil.TEXT_PRIMARY;
                    };
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });
        typeCol.setMaxWidth(100);

        // 数据库名列
        TableColumn<JobJdbcDatasourceVO, String> dbCol = new TableColumn<>("数据库名");
        dbCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getDatabaseName())));

        // 用户名列
        TableColumn<JobJdbcDatasourceVO, String> userCol = new TableColumn<>("用户名");
        userCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getJdbcUsername())));

        // JDBC URL列
        TableColumn<JobJdbcDatasourceVO, String> urlCol = new TableColumn<>("JDBC URL");
        urlCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(safe(c.getValue().getJdbcUrl())));
        urlCol.setPrefWidth(250);

        // 创建时间列
        TableColumn<JobJdbcDatasourceVO, String> createTimeCol = new TableColumn<>("创建时间");
        createTimeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                formatTime(c.getValue().getCreateTime())));

        // 更新时间列
        TableColumn<JobJdbcDatasourceVO, String> updateTimeCol = new TableColumn<>("更新时间");
        updateTimeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                formatTime(c.getValue().getUpdateTime())));

        // 操作列
        TableColumn<JobJdbcDatasourceVO, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final HBox actionBox = new HBox(8);
            private final Hyperlink editLink = new Hyperlink("编辑");
            private final Hyperlink testLink = new Hyperlink("测试连接");
            private final Hyperlink deleteLink = new Hyperlink("删除");
            {
                editLink.setStyle("-fx-text-fill: " + StyleUtil.PRIMARY + ";");
                testLink.setStyle("-fx-text-fill: " + StyleUtil.SUCCESS + ";");
                deleteLink.setStyle("-fx-text-fill: " + StyleUtil.ERROR + ";");
                
                editLink.setOnAction(e -> {
                    JobJdbcDatasourceVO item = getTableView().getItems().get(getIndex());
                    if (item != null) handleEdit(item);
                });
                testLink.setOnAction(e -> {
                    JobJdbcDatasourceVO item = getTableView().getItems().get(getIndex());
                    if (item != null) handleTestConnection(item);
                });
                deleteLink.setOnAction(e -> {
                    JobJdbcDatasourceVO item = getTableView().getItems().get(getIndex());
                    if (item != null) handleDeleteSingle(item);
                });
                
                actionBox.getChildren().addAll(editLink, testLink, deleteLink);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });
        actionCol.setPrefWidth(180);
        actionCol.setMinWidth(180);

        tableView.getColumns().addAll(checkCol, idxCol, nameCol, typeCol, dbCol, userCol, urlCol, createTimeCol, updateTimeCol, actionCol);

        return tableView;
    }

    private Node createPagerBar() {
        totalLabel = new Label("共 0 条");
        totalLabel.setStyle(StyleUtil.body());

        prevBtn = new Button("上一页");
        prevBtn.setStyle(StyleUtil.secondaryButton());
        nextBtn = new Button("下一页");
        nextBtn.setStyle(StyleUtil.secondaryButton());
        
        pageField = new TextField(String.valueOf(pageNum));
        pageField.setPrefWidth(60);
        pageField.setStyle(StyleUtil.searchField());
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
        pageSizeLabel.setStyle(StyleUtil.body());
        Label pageLabel = new Label("页码");
        pageLabel.setStyle(StyleUtil.body());

        HBox pager = new HBox(12,
                totalLabel,
                pageSizeLabel, pageSizeBox,
                prevBtn,
                pageLabel, pageField,
                nextBtn
        );
        pager.setAlignment(Pos.CENTER_LEFT);
        pager.setPadding(new Insets(16, 16, 16, 16));
        pager.setStyle(
                "-fx-background-color: " + StyleUtil.BG_PRIMARY + "; " +
                "-fx-background-radius: " + StyleUtil.RADIUS_LG + ";"
        );
        return pager;
    }

    private void loadPage(boolean showLoading) {
        if (loading.getAndSet(true)) return;
        if (showLoading) totalLabel.setText("加载中...");

        JobJdbcDatasourceQuery query = new JobJdbcDatasourceQuery();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        if (!isBlank(datasourceNameField.getText())) {
            query.setDatasourceName(datasourceNameField.getText().trim());
        }
        String typeVal = datasourceTypeCombo.getValue();
        if (!isBlank(typeVal)) {
            query.setDatasource(typeVal);
        }
        if (!isBlank(databaseNameField.getText())) {
            query.setDatabaseName(databaseNameField.getText().trim());
        }

        new Thread(() -> {
            try {
                PageResult<JobJdbcDatasourceVO> page = datasourceService.getDatasourcePage(query);
                List<JobJdbcDatasourceVO> list = page != null && page.getData() != null
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
                    showError("加载数据源列表失败", ex.getMessage());
                });
            } finally {
                loading.set(false);
            }
        }, "load-datasource-list").start();
    }

    private void updatePagerButtons() {
        long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
        prevBtn.setDisable(pageNum <= 1);
        nextBtn.setDisable(pageNum >= maxPage || maxPage == 0);
    }

    private void handleAdd() {
        showDatasourceFormDialog(null);
    }

    private void handleEdit(JobJdbcDatasourceVO item) {
        if (item == null || item.getId() == null) return;
        // 加载表单数据
        runAsync("加载数据", () -> {
            try {
                JobJdbcDatasourceForm form = datasourceService.getFormData(item.getId());
                Platform.runLater(() -> showDatasourceFormDialog(form));
                return "加载成功";
            } catch (Exception e) {
                throw new RuntimeException("加载失败: " + e.getMessage(), e);
            }
        });
    }

    private void showDatasourceFormDialog(JobJdbcDatasourceForm editItem) {
        Dialog<JobJdbcDatasourceForm> dialog = new Dialog<>();
        dialog.setTitle(editItem == null || editItem.getId() == null ? "新增数据源" : "编辑数据源");
        dialog.initOwner(getDialogPane().getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        String labelStyle = StyleUtil.body();

        // 数据源名称
        Label nameLabel = new Label("数据源名称");
        nameLabel.setStyle(labelStyle);
        TextField nameInput = new TextField();
        nameInput.setPrefWidth(300);
        nameInput.setPromptText("请输入数据源名称");
        nameInput.setStyle(StyleUtil.searchField());

        // 数据源类型
        Label typeLabel = new Label("数据源类型");
        typeLabel.setStyle(labelStyle);
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("MYSQL", "ORACLE", "POSTGRESQL");
        typeCombo.getSelectionModel().selectFirst();
        typeCombo.setPrefWidth(300);

        // 数据库名
        Label dbLabel = new Label("数据库名");
        dbLabel.setStyle(labelStyle);
        TextField dbInput = new TextField();
        dbInput.setPrefWidth(300);
        dbInput.setPromptText("请输入数据库名");
        dbInput.setStyle(StyleUtil.searchField());

        // 用户名
        Label userLabel = new Label("用户名");
        userLabel.setStyle(labelStyle);
        TextField userInput = new TextField();
        userInput.setPrefWidth(300);
        userInput.setPromptText("请输入用户名");
        userInput.setStyle(StyleUtil.searchField());

        // 密码
        Label pwdLabel = new Label("密码");
        pwdLabel.setStyle(labelStyle);
        PasswordField pwdInput = new PasswordField();
        pwdInput.setPrefWidth(300);
        pwdInput.setPromptText("请输入密码");
        pwdInput.setStyle(StyleUtil.searchField());

        // JDBC URL
        Label urlLabel = new Label("JDBC URL");
        urlLabel.setStyle(labelStyle);
        TextField urlInput = new TextField();
        urlInput.setPrefWidth(300);
        urlInput.setPromptText("jdbc:mysql://host:port/database");
        urlInput.setStyle(StyleUtil.searchField());

        // 驱动类
        Label driverLabel = new Label("JDBC驱动类");
        driverLabel.setStyle(labelStyle);
        TextField driverInput = new TextField();
        driverInput.setPrefWidth(300);
        driverInput.setPromptText("com.mysql.cj.jdbc.Driver");
        driverInput.setStyle(StyleUtil.searchField());

        // 备注
        Label commentLabel = new Label("备注");
        commentLabel.setStyle(labelStyle);
        TextArea commentInput = new TextArea();
        commentInput.setPrefWidth(300);
        commentInput.setPrefRowCount(3);
        commentInput.setWrapText(true);
        commentInput.setStyle(StyleUtil.searchField());

        // 监听数据源类型变化，自动填充驱动类
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && isBlank(driverInput.getText())) {
                driverInput.setText(getDefaultDriver(newVal));
            }
        });

        grid.add(nameLabel, 0, 0);
        grid.add(nameInput, 1, 0);
        grid.add(typeLabel, 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(dbLabel, 0, 2);
        grid.add(dbInput, 1, 2);
        grid.add(userLabel, 0, 3);
        grid.add(userInput, 1, 3);
        grid.add(pwdLabel, 0, 4);
        grid.add(pwdInput, 1, 4);
        grid.add(urlLabel, 0, 5);
        grid.add(urlInput, 1, 5);
        grid.add(driverLabel, 0, 6);
        grid.add(driverInput, 1, 6);
        grid.add(commentLabel, 0, 7);
        grid.add(commentInput, 1, 7);

        // 填充编辑数据
        if (editItem != null) {
            nameInput.setText(safe(editItem.getDatasourceName()));
            if (editItem.getDatasource() != null) {
                typeCombo.getSelectionModel().select(editItem.getDatasource());
            }
            dbInput.setText(safe(editItem.getDatabaseName()));
            userInput.setText(safe(editItem.getJdbcUsername()));
            pwdInput.setText(safe(editItem.getJdbcPassword()));
            urlInput.setText(safe(editItem.getJdbcUrl()));
            driverInput.setText(safe(editItem.getJdbcDriverClass()));
            commentInput.setText(safe(editItem.getComments()));
        }

        dialog.getDialogPane().setContent(grid);

        ButtonType testBtn = new ButtonType("测试连接", ButtonBar.ButtonData.LEFT);
        ButtonType saveBtn = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(testBtn, saveBtn, cancelBtn);

        // 测试连接按钮处理
        Button testBtnNode = (Button) dialog.getDialogPane().lookupButton(testBtn);
        if (testBtnNode != null) {
            testBtnNode.setStyle(StyleUtil.successButton());
            testBtnNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                event.consume();
                JobJdbcDatasourceForm form = buildFormFromInputs(
                        editItem, nameInput, typeCombo, dbInput, userInput, pwdInput, urlInput, driverInput, commentInput);
                if (form != null) {
                    testConnectionAsync(form);
                }
            });
        }

        Button saveBtnNode = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        if (saveBtnNode != null) {
            saveBtnNode.setStyle(StyleUtil.primaryButton());
        }

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveBtn) {
                return buildFormFromInputs(editItem, nameInput, typeCombo, dbInput, userInput, pwdInput, urlInput, driverInput, commentInput);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(form -> {
            if (form != null) {
                saveDatasource(editItem != null && editItem.getId() != null, editItem != null ? editItem.getId() : null, form);
            }
        });
    }

    private JobJdbcDatasourceForm buildFormFromInputs(
            JobJdbcDatasourceForm editItem,
            TextField nameInput, ComboBox<String> typeCombo, TextField dbInput,
            TextField userInput, PasswordField pwdInput, TextField urlInput,
            TextField driverInput, TextArea commentInput) {
        if (isBlank(nameInput.getText())) {
            showError("验证失败", "请输入数据源名称");
            return null;
        }
        if (isBlank(dbInput.getText())) {
            showError("验证失败", "请输入数据库名");
            return null;
        }
        if (isBlank(userInput.getText())) {
            showError("验证失败", "请输入用户名");
            return null;
        }
        if (isBlank(pwdInput.getText())) {
            showError("验证失败", "请输入密码");
            return null;
        }
        if (isBlank(urlInput.getText())) {
            showError("验证失败", "请输入JDBC URL");
            return null;
        }
        if (isBlank(driverInput.getText())) {
            showError("验证失败", "请输入JDBC驱动类");
            return null;
        }

        JobJdbcDatasourceForm form = new JobJdbcDatasourceForm();
        if (editItem != null) {
            form.setId(editItem.getId());
        }
        form.setDatasourceName(nameInput.getText().trim());
        form.setDatasource(typeCombo.getValue());
        form.setDatabaseName(dbInput.getText().trim());
        form.setJdbcUsername(userInput.getText().trim());
        form.setJdbcPassword(pwdInput.getText());
        form.setJdbcUrl(urlInput.getText().trim());
        form.setJdbcDriverClass(driverInput.getText().trim());
        form.setComments(isBlank(commentInput.getText()) ? "备注" : commentInput.getText().trim());
        return form;
    }

    private void testConnectionAsync(JobJdbcDatasourceForm form) {
        runAsync("测试连接", () -> {
            boolean connected = datasourceService.testConnection(form);
            Platform.runLater(() -> {
                if (connected) {
                    showInfo("连接成功！");
                } else {
                    showError("测试连接", "连接失败，请检查配置");
                }
            });
            return connected ? "连接成功" : "连接失败";
        });
    }

    private void handleTestConnection(JobJdbcDatasourceVO item) {
        if (item == null || item.getId() == null) return;
        runAsync("测试连接", () -> {
            try {
                JobJdbcDatasourceForm form = datasourceService.getFormData(item.getId());
                boolean connected = datasourceService.testConnection(form);
                Platform.runLater(() -> {
                    if (connected) {
                        showInfo("连接成功！");
                    } else {
                        showError("测试连接", "连接失败，请检查配置");
                    }
                });
                return connected ? "连接成功" : "连接失败";
            } catch (Exception e) {
                throw new RuntimeException("测试连接失败: " + e.getMessage(), e);
            }
        });
    }

    private void saveDatasource(boolean isEdit, Long id, JobJdbcDatasourceForm form) {
        runAsync("保存数据源", () -> {
            boolean ok;
            if (isEdit) {
                ok = datasourceService.updateDatasource(id, form);
            } else {
                ok = datasourceService.saveDatasource(form);
            }
            return ok ? "保存成功" : "保存失败";
        });
    }

    private void handleDeleteSingle(JobJdbcDatasourceVO item) {
        if (item == null || item.getId() == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
                "确定要删除数据源 \"" + safe(item.getDatasourceName()) + "\" 吗？", 
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("确认删除");
        confirm.initOwner(getDialogPane().getScene().getWindow());
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.YES) {
                runAsync("删除数据源", () -> {
                    boolean ok = datasourceService.deleteDatasources(String.valueOf(item.getId()));
                    return ok ? "删除成功" : "删除失败";
                });
            }
        });
    }

    private void handleBatchDelete() {
        List<JobJdbcDatasourceVO> selected = selectionModel.getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showError("提示", "请选择要删除的数据源");
            return;
        }
        
        String ids = selected.stream()
                .map(item -> String.valueOf(item.getId()))
                .collect(Collectors.joining(","));
        
        runAsync("删除数据源", () -> {
            boolean ok = datasourceService.deleteDatasources(ids);
            return ok ? "删除成功" : "删除失败";
        });
    }

    private void runAsync(String title, java.util.concurrent.Callable<String> task) {
        if (loading.getAndSet(true)) return;
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
        }, "datasource-action").start();
    }

    private String getDefaultDriver(String datasource) {
        return switch (datasource) {
            case "MYSQL" -> "com.mysql.cj.jdbc.Driver";
            case "ORACLE" -> "oracle.jdbc.driver.OracleDriver";
            case "POSTGRESQL" -> "org.postgresql.Driver";
            default -> "";
        };
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(DTF);
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
