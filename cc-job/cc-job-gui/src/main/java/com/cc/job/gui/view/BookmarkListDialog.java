package com.cc.job.gui.view;

import com.cc.job.gui.service.JobCanvasBookmarkApiService;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.xo.model.entity.JobCanvasBookmark;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * 书签列表面板/对话框 - 按任务组列出书签，点击跳转（位置/节点），可删除
 */
public class BookmarkListDialog extends Dialog<Void> {

    private static final Logger logger = LoggerFactory.getLogger(BookmarkListDialog.class);

    private final Stage owner;
    private final Long taskGroupId;
    private final JobCanvasBookmarkApiService bookmarkApiService;
    private final Consumer<double[]> onJumpToPosition; // (hvalue, vvalue)
    private final Consumer<Long> onJumpToNode;         // target_node_id (job_info id)
    private ListView<JobCanvasBookmark> listView;

    public BookmarkListDialog(Stage owner, Long taskGroupId,
                              Consumer<double[]> onJumpToPosition, Consumer<Long> onJumpToNode) {
        this.owner = owner;
        this.taskGroupId = taskGroupId;
        this.bookmarkApiService = new JobCanvasBookmarkApiService();
        this.onJumpToPosition = onJumpToPosition;
        this.onJumpToNode = onJumpToNode;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("书签");
        setHeaderText("当前任务组书签列表，点击跳转");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefWidth(380);
        content.setPrefHeight(400);

        listView = new ListView<>();
        listView.setPrefHeight(320);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JobCanvasBookmark item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String typeLabel = "position".equals(item.getBookmarkType()) ? "位置" : "节点";
                    setText(item.getBookmarkName() + " [" + typeLabel + "]");
                }
            }
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                JobCanvasBookmark b = listView.getSelectionModel().getSelectedItem();
                if (b != null) jumpToBookmark(b);
            }
        });

        Button jumpBtn = new Button("跳转");
        jumpBtn.setOnAction(e -> {
            JobCanvasBookmark b = listView.getSelectionModel().getSelectedItem();
            if (b != null) jumpToBookmark(b); else NotificationToast.showWarning("请选择一条书签");
        });
        Button deleteBtn = new Button("删除");
        deleteBtn.setOnAction(e -> deleteSelected());
        Button refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> loadBookmarks());

        content.getChildren().addAll(listView, new javafx.scene.layout.HBox(10, jumpBtn, deleteBtn, refreshBtn));

        getDialogPane().setContent(content);
        String css = com.cc.job.gui.util.ThemeManager.getInstance().getStylesheetUrl();
        if (css != null && !css.isEmpty()) {
            getDialogPane().getStylesheets().add(css);
        }
        getDialogPane().getButtonTypes().add(new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE));

        loadBookmarks();
    }

    private void loadBookmarks() {
        new Thread(() -> {
            try {
                List<JobCanvasBookmark> list = bookmarkApiService.listByTaskGroupId(taskGroupId);
                Platform.runLater(() -> listView.setItems(FXCollections.observableArrayList(list)));
            } catch (Exception ex) {
                logger.error("加载书签列表失败", ex);
                Platform.runLater(() -> NotificationToast.showError("加载书签失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void jumpToBookmark(JobCanvasBookmark b) {
        if ("position".equals(b.getBookmarkType()) && b.getCanvasPositionX() != null && b.getCanvasPositionY() != null) {
            if (onJumpToPosition != null) {
                onJumpToPosition.accept(new double[]{b.getCanvasPositionX(), b.getCanvasPositionY()});
            }
            close();
        } else if ("node".equals(b.getBookmarkType()) && b.getTargetNodeId() != null) {
            if (onJumpToNode != null) {
                onJumpToNode.accept(b.getTargetNodeId());
            }
            close();
        } else {
            NotificationToast.showWarning("该书签数据不完整，无法跳转");
        }
    }

    private void deleteSelected() {
        JobCanvasBookmark b = listView.getSelectionModel().getSelectedItem();
        if (b == null || b.getId() == null) {
            NotificationToast.showWarning("请选择一条书签");
            return;
        }
        new Thread(() -> {
            try {
                boolean ok = bookmarkApiService.delete(b.getId());
                Platform.runLater(() -> {
                    if (ok) {
                        loadBookmarks();
                        NotificationToast.showSuccess("已删除书签");
                    } else {
                        NotificationToast.showError("删除失败");
                    }
                });
            } catch (Exception ex) {
                logger.error("删除书签失败", ex);
                Platform.runLater(() -> NotificationToast.showError("删除失败: " + ex.getMessage()));
            }
        }).start();
    }
}
