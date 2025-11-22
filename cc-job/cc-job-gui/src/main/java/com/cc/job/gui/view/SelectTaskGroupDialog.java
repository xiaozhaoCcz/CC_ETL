package com.cc.job.gui.view;

import com.cc.job.gui.service.JobPartService;
import com.cc.job.xo.model.vo.JobPartVo;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 选择任务组弹窗（两级级联：分区 -> 任务组）
 */
public class SelectTaskGroupDialog extends Dialog<SelectTaskGroupDialog.Selection> {
    
    public static class Selection {
        public final Long taskGroupId;
        public final String taskGroupName;
        public Selection(Long id, String name) {
            this.taskGroupId = id;
            this.taskGroupName = name;
        }
    }
    
    private final JobPartService jobPartService = new JobPartService();
    
    private ComboBox<JobPartVo> partCombo;
    private ComboBox<JobPartVo> groupCombo;
    private ButtonType okButtonType;
    private final Map<Long, List<JobPartVo>> partToGroups = new HashMap<>();
    private List<JobPartVo> parts = new ArrayList<>();
    
    public SelectTaskGroupDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("选择任务组");
        setHeaderText(null);
        
        VBox content = createContent();
        getDialogPane().setContent(content);
        
        okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancel, okButtonType);
        styleDialog();
        
        // 载入数据
        loadDataAsync();
        
        setResultConverter(bt -> {
            if (bt == okButtonType) {
                JobPartVo group = groupCombo.getValue();
                if (group != null && group.getType() != null && group.getType() == 1) {
                    return new Selection(group.getId(), group.getLabel());
                }
            }
            return null;
        });
    }
    
    private VBox createContent() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(16));
        container.setPrefWidth(520);
        
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent;");
        
        VBox form = new VBox(16);
        form.setPadding(new Insets(8));
        
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));
        
        Label partLabel = createFormLabel("分区", true);
        partCombo = new ComboBox<>();
        partCombo.setPromptText("请选择分区");
        partCombo.setPrefWidth(340);
        partCombo.setCellFactory(cb -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(JobPartVo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        partCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(JobPartVo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        partCombo.valueProperty().addListener((obs, o, n) -> onPartChanged(n));
        
        Label groupLabel = createFormLabel("任务组", true);
        groupCombo = new ComboBox<>();
        groupCombo.setPromptText("请选择任务组");
        groupCombo.setPrefWidth(340);
        groupCombo.setCellFactory(cb -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(JobPartVo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        groupCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(JobPartVo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getLabel());
            }
        });
        
        grid.add(partLabel, 0, 0);
        grid.add(partCombo, 1, 0);
        grid.add(groupLabel, 0, 1);
        //grid.add(groupCombo, 1, 1);
        
        form.getChildren().add(grid);
        sp.setContent(form);
        VBox.setVgrow(sp, Priority.ALWAYS);
        container.getChildren().add(sp);
        return container;
    }
    
    private void onPartChanged(JobPartVo part) {
        groupCombo.getItems().clear();
        if (part == null) {
            return;
        }
        List<JobPartVo> groups = partToGroups.getOrDefault(part.getId(), new ArrayList<>());
        groupCombo.getItems().addAll(groups);
        if (!groups.isEmpty()) {
            groupCombo.setValue(groups.get(0));
        }
        updateOkButtonState();
    }
    
    private void loadDataAsync() {
        new Thread(() -> {
            try {
                List<JobPartVo> tree = jobPartService.getTree();
                Platform.runLater(() -> fillFromTree(tree));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    // 简单降级为空
                    fillFromTree(new ArrayList<>());
                });
            }
        }).start();
    }
    
    private void fillFromTree(List<JobPartVo> tree) {
        parts.clear();
        partToGroups.clear();
        if (tree != null) {
            for (JobPartVo part : tree) {
                if (part.getType() != null && part.getType() == 0) {
                    parts.add(part);
                    List<JobPartVo> groups = new ArrayList<>();
                    if (part.getChildren() != null) {
                        for (JobPartVo child : part.getChildren()) {
                            if (child.getType() != null && child.getType() == 1) {
                                groups.add(child);
                            }
                        }
                    }
                    partToGroups.put(part.getId(), groups);
                }
            }
        }
        partCombo.getItems().setAll(parts);
        if (!parts.isEmpty()) {
            partCombo.setValue(parts.get(0));
        }
        updateOkButtonState();
    }
    
    private void updateOkButtonState() {
        Button okBtn = (Button) getDialogPane().lookupButton(okButtonType);
        if (okBtn != null) {
            okBtn.setDisable(groupCombo.getValue() == null);
        }
    }
    
    private Label createFormLabel(String text, boolean required) {
        Label label = new Label();
        label.setMinWidth(120);
        label.setPrefWidth(120);
        label.setMaxWidth(120);
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        if (required) {
            HBox box = new HBox(2);
            box.setAlignment(Pos.CENTER_LEFT);
            Label t = new Label(text);
            Label star = new Label("*");
            star.setStyle("-fx-text-fill:#EF4444;");
            box.getChildren().addAll(t, star);
            label.setGraphic(box);
            label.setTooltip(new Tooltip(text + " *"));
        } else {
            label.setText(text);
            label.setTooltip(new Tooltip(text));
        }
        return label;
    }
    
    private void styleDialog() {
        getDialogPane().setPrefWidth(560);
        getDialogPane().setPrefHeight(220);
        getDialogPane().setMinWidth(520);
        getDialogPane().setMinHeight(200);
        setResizable(false);
        
        Platform.runLater(() -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            if (stage != null) {
                stage.setResizable(false);
            }
        });
    }
    
    public static Optional<Selection> showDialog(Stage owner) {
        SelectTaskGroupDialog dialog = new SelectTaskGroupDialog(owner);
        return dialog.showAndWait();
    }
}


