package com.cc.job.gui.view;

import com.cc.job.gui.service.JobGroupService;
import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.util.StyleUtil;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.common.result.PageResult;
import com.cc.job.xo.model.entity.JobGroup;
import com.cc.job.xo.model.query.JobInfoQuery;
import com.cc.job.xo.model.vo.JobInfoVO;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 从任务列表选择单任务并添加到画布
 * 仅展示单任务（jobType=0），支持搜索，支持主题切换
 */
public class AddJobFromListDialog extends Dialog<JobInfoVO> {

    private final JobInfoService jobInfoService = new JobInfoService();
    private final JobGroupService jobGroupService = new JobGroupService();
    private final Stage ownerStage;

    private TableView<JobInfoVO> tableView;
    private TableView.TableViewSelectionModel<JobInfoVO> selectionModel;

    private ComboBox<JobGroup> jobGroupCombo;
    private TextField jobDescField;
    private TextField handlerField;
    private TextField authorField;
    private ComboBox<String> statusCombo;

    private Label totalLabel;
    private TextField pageField;
    private ComboBox<Integer> pageSizeBox;
    private Button prevBtn;
    private Button nextBtn;

    private List<JobGroup> jobGroupList = new ArrayList<>();
    private final AtomicBoolean loading = new AtomicBoolean(false);

    private int pageNum = 1;
    private int pageSize = 10;
    private long total = 0;

    private Runnable themeChangedListener;

    public AddJobFromListDialog(Stage ownerStage) {
        this.ownerStage = ownerStage;
        setTitle("从任务列表添加节点");
        setHeaderText("选择单任务，将添加到当前任务组画布（仅显示单任务）");
        initOwner(ownerStage);
        initModality(Modality.WINDOW_MODAL);

        styleDialog();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-content-root");
        root.setPadding(new Insets(16));

        root.setTop(createTopSection());
        root.setCenter(createTable());
        root.setBottom(createPagerBar());

        getDialogPane().setContent(root);

        ButtonType addType = new ButtonType("添加到画布", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelType, addType);

        Node addButton = getDialogPane().lookupButton(addType);
        if (addButton instanceof Button) {
            ((Button) addButton).disableProperty().bind(
                Bindings.isNull(selectionModel.selectedItemProperty()));
        }

        setResultConverter(buttonType -> {
            if (buttonType == addType) {
                return selectionModel.getSelectedItem();
            }
            return null;
        });

        setOnHidden(e -> {
            if (themeChangedListener != null) {
                ThemeManager.getInstance().removeOnThemeChanged(themeChangedListener);
            }
        });

        loadJobGroupList();
        loadPage(true);
    }

    private void styleDialog() {
        getDialogPane().setPrefSize(900, 560);
        getDialogPane().setMinWidth(800);
        getDialogPane().setMinHeight(500);
        setResizable(true);

        String dialogCss = ThemeManager.getInstance().getStylesheetUrl();
        if (dialogCss != null && !dialogCss.isEmpty()) {
            getDialogPane().getStylesheets().add(dialogCss);
        }

        themeChangedListener = () -> {
            javafx.scene.Scene scene = getDialogPane().getScene();
            if (scene != null) {
                scene.getStylesheets().clear();
                String url = ThemeManager.getInstance().getStylesheetUrl();
                if (url != null && !url.isEmpty()) {
                    scene.getStylesheets().add(url);
                }
            }
        };
        ThemeManager.getInstance().addOnThemeChanged(themeChangedListener);

        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(true);
                String css = ThemeManager.getInstance().getStylesheetUrl();
                if (css != null && !css.isEmpty()) {
                    stage.getScene().getStylesheets().add(css);
                }
            }
        });
    }

    private Node createTopSection() {
        VBox top = new VBox(12);
        top.getChildren().add(createFilterBar());
        top.setPadding(new Insets(0, 0, 12, 0));
        return top;
    }

    private Node createFilterBar() {
        FlowPane pane = new FlowPane();
        pane.getStyleClass().add("dialog-section");
        pane.setHgap(12);
        pane.setVgap(10);
        pane.setPadding(new Insets(16, 16, 16, 16));

        String labelStyle = StyleUtil.bodyFontOnly();

        jobGroupCombo = new ComboBox<>();
        jobGroupCombo.setPrefWidth(180);
        jobGroupCombo.setCellFactory(lv -> new ListCell<JobGroup>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "全部" : item.getTitle());
            }
        });
        jobGroupCombo.setButtonCell(new ListCell<JobGroup>() {
            @Override
            protected void updateItem(JobGroup item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "全部" : item.getTitle());
            }
        });

        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("全部", "运行", "停止");
        statusCombo.getSelectionModel().selectFirst();
        statusCombo.setPrefWidth(100);

        jobDescField = new TextField();
        jobDescField.setPromptText("任务描述");
        jobDescField.setPrefWidth(160);
        jobDescField.setOnAction(e -> handleSearch());

        handlerField = new TextField();
        handlerField.setPromptText("JobHandler");
        handlerField.setPrefWidth(140);
        handlerField.setOnAction(e -> handleSearch());

        authorField = new TextField();
        authorField.setPromptText("负责人");
        authorField.setPrefWidth(120);
        authorField.setOnAction(e -> handleSearch());

        Button searchBtn = new Button("搜索");
        searchBtn.getStyleClass().add("dialog-button-primary");
        searchBtn.setOnAction(e -> handleSearch());

        Button resetBtn = new Button("重置");
        resetBtn.getStyleClass().add("dialog-button-secondary");
        resetBtn.setOnAction(e -> handleReset());

        Label executorLabel = new Label("执行器");
        executorLabel.setStyle(labelStyle);
        Label statusLabel = new Label("状态");
        statusLabel.setStyle(labelStyle);
        Label descLabel = new Label("任务描述");
        descLabel.setStyle(labelStyle);
        Label handlerLabel = new Label("JobHandler");
        handlerLabel.setStyle(labelStyle);
        Label authorLabel = new Label("负责人");
        authorLabel.setStyle(labelStyle);

        pane.getChildren().addAll(
            new HBox(12, executorLabel, jobGroupCombo),
            new HBox(12, statusLabel, statusCombo),
            new HBox(12, descLabel, jobDescField),
            new HBox(12, handlerLabel, handlerField),
            new HBox(12, authorLabel, authorField),
            new HBox(10, searchBtn, resetBtn)
        );
        return pane;
    }

    private void handleSearch() {
        pageNum = 1;
        loadPage(true);
    }

    private void handleReset() {
        jobGroupCombo.getSelectionModel().clearSelection();
        jobDescField.clear();
        handlerField.clear();
        authorField.clear();
        statusCombo.getSelectionModel().selectFirst();
        pageNum = 1;
        loadPage(true);
    }

    private Node createTable() {
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        selectionModel = tableView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);

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
        idxCol.setMinWidth(50);

        TableColumn<JobInfoVO, String> descCol = new TableColumn<>("任务描述");
        descCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getJobDesc())));
        descCol.setMinWidth(140);

        TableColumn<JobInfoVO, String> handlerCol = new TableColumn<>("JobHandler");
        handlerCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getExecutorHandler())));
        handlerCol.setMinWidth(120);

        TableColumn<JobInfoVO, String> authorCol = new TableColumn<>("负责人");
        authorCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getAuthor())));
        authorCol.setMinWidth(80);

        TableColumn<JobInfoVO, String> glueCol = new TableColumn<>("运行模式");
        glueCol.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getGlueType())));
        glueCol.setMinWidth(90);

        TableColumn<JobInfoVO, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getTriggerStatus() != null && c.getValue().getTriggerStatus() == 1 ? "运行" : "停止"));
        statusCol.setMinWidth(60);

        tableView.getColumns().addAll(idxCol, descCol, handlerCol, authorCol, glueCol, statusCol);
        return tableView;
    }

    private Node createPagerBar() {
        HBox pager = new HBox(12);
        pager.getStyleClass().add("dialog-section");
        totalLabel = new Label("共 0 条");
        pageField = new TextField("1");
        pageField.setPrefWidth(50);
        pageSizeBox = new ComboBox<>(FXCollections.observableArrayList(10, 20, 50));
        pageSizeBox.setValue(10);
        pageSizeBox.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
            if (newVal != null) {
                pageSize = newVal;
                pageNum = 1;
                loadPage(true);
            }
        });
        prevBtn = new Button("上一页");
        prevBtn.setOnAction(e -> {
            if (pageNum > 1) {
                pageNum--;
                loadPage(false);
            }
        });
        nextBtn = new Button("下一页");
        nextBtn.setOnAction(e -> {
            long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
            if (pageNum < maxPage) {
                pageNum++;
                loadPage(false);
            }
        });
        pager.getChildren().addAll(totalLabel, prevBtn, pageField, nextBtn, new Label("每页"), pageSizeBox);
        pager.setAlignment(Pos.CENTER_LEFT);
        pager.setPadding(new Insets(16));
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
        query.setJobType(0); // 仅单任务

        if (jobGroupCombo.getValue() != null) {
            query.setJobGroup(jobGroupCombo.getValue().getId());
        }
        if (!isBlank(jobDescField.getText())) {
            query.setJobDesc(jobDescField.getText().trim());
        }
        if (!isBlank(handlerField.getText())) {
            query.setExecutorHandler(handlerField.getText().trim());
        }
        if (!isBlank(authorField.getText())) {
            query.setAuthor(authorField.getText().trim());
        }
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
        }, "load-single-job-list").start();
    }

    private void updatePagerButtons() {
        long maxPage = (long) Math.ceil(total * 1.0 / pageSize);
        prevBtn.setDisable(pageNum <= 1);
        nextBtn.setDisable(pageNum >= maxPage || maxPage == 0);
    }

    private void loadJobGroupList() {
        new Thread(() -> {
            try {
                jobGroupList = jobGroupService.getAllJobGroupList();
                Platform.runLater(() -> {
                    javafx.collections.ObservableList<JobGroup> items = FXCollections.observableArrayList();
                    items.add(null);
                    if (jobGroupList != null) {
                        items.addAll(jobGroupList);
                    }
                    jobGroupCombo.setItems(items);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("加载执行器列表失败", ex.getMessage()));
            }
        }, "load-job-groups-picker").start();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(title);
        if (getDialogPane().getScene() != null && getDialogPane().getScene().getWindow() != null) {
            alert.initOwner(getDialogPane().getScene().getWindow());
        }
        alert.showAndWait();
    }

    /**
     * 显示对话框并返回选中的单任务（用户点击「添加到画布」时）
     */
    public static Optional<JobInfoVO> showAndSelect(Stage owner) {
        AddJobFromListDialog dialog = new AddJobFromListDialog(owner);
        return dialog.showAndWait();
    }
}
