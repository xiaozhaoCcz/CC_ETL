package com.cc.job.gui.view;

import com.cc.job.xo.model.entity.JobGroupSnapshot;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 任务组版本列表对话框：显示手动保存的版本，支持回滚
 */
public class VersionListDialog extends Dialog<Void> {

    private static final String VERSION_PREFIX = "ver_";
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TableView<JobGroupSnapshot> table;
    private Long selectedSnapshotId;

    public VersionListDialog(Stage owner, List<JobGroupSnapshot> versions) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("版本列表与回滚");
        setHeaderText("选择要回滚的版本，点击「回滚」将画布恢复为该版本（恢复后可再保存）");

        table = new TableView<>(FXCollections.observableList(versions != null ? versions : List.of()));
        table.setPlaceholder(new Label("暂无保存的版本"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(280);

        TableColumn<JobGroupSnapshot, String> nameCol = new TableColumn<>("版本名称");
        nameCol.setCellValueFactory(c -> {
            String rid = c.getValue().getRandomId();
            String name = (rid != null && rid.startsWith(VERSION_PREFIX)) ? rid.substring(VERSION_PREFIX.length()) : (rid != null ? rid : "");
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        TableColumn<JobGroupSnapshot, String> timeCol = new TableColumn<>("创建时间");
        timeCol.setCellValueFactory(c -> {
            var t = c.getValue().getCreateTime();
            return new javafx.beans.property.SimpleStringProperty(t != null ? t.format(DF) : "");
        });
        table.getColumns().addAll(nameCol, timeCol);

        ButtonType rollbackType = new ButtonType("回滚", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(rollbackType, ButtonType.CLOSE);

        Button rollbackBtn = (Button) getDialogPane().lookupButton(rollbackType);
        rollbackBtn.setDefaultButton(true);
        rollbackBtn.setOnAction(e -> {
            JobGroupSnapshot sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getId() != null) {
                selectedSnapshotId = sel.getId();
                close();
            }
        });

        setResultConverter(bt -> null);

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.getChildren().add(table);
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(420);
    }

    public Long getSelectedSnapshotId() {
        return selectedSnapshotId;
    }

    /**
     * 显示对话框，若用户点击「回滚」并选中了某行，返回该快照 ID；否则返回 empty
     */
    public static Optional<Long> showAndSelectRollback(Stage owner, List<JobGroupSnapshot> versions) {
        VersionListDialog dialog = new VersionListDialog(owner, versions);
        dialog.showAndWait();
        return Optional.ofNullable(dialog.getSelectedSnapshotId());
    }
}
