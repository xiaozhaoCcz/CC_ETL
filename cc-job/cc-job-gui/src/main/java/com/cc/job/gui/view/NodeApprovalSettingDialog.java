package com.cc.job.gui.view;

import com.cc.job.gui.model.ProcessNode;
import com.cc.job.gui.service.PermissionManageService;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.model.vo.UserListVO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点审批设置对话框：是否需要审批、指定审批人
 */
public class NodeApprovalSettingDialog extends Dialog<Void> {

    private static final Logger logger = LoggerFactory.getLogger(NodeApprovalSettingDialog.class);

    private final ProcessNode node;
    private CheckBox requireApprovalCheck;
    private ListView<UserListVO> userListView;
    private final List<Long> selectedUserIds = new ArrayList<>();

    public NodeApprovalSettingDialog(Stage owner, ProcessNode node) {
        this.node = node;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("审批设置");
        setHeaderText("节点 \"" + (node != null ? node.getJobHandlerName() : "") + "\" 的审批设置");

        VBox content = createContent();
        getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelButtonType, okButtonType);

        String cssUrl = ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            getDialogPane().getStylesheets().add(cssUrl);
        }

        setResultConverter(buttonType -> {
            if (buttonType == okButtonType) {
                if (node != null) {
                    node.setRequireApproval(requireApprovalCheck != null && requireApprovalCheck.isSelected());
                    node.setApproverUserIds(selectedUserIds);
                }
            }
            return null;
        });

        loadUsersAsync();
    }

    private VBox createContent() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(20));
        container.setPrefWidth(420);
        container.setPrefHeight(380);

        requireApprovalCheck = new CheckBox("执行到本节点时需要审批，审批通过后继续执行");
        requireApprovalCheck.setSelected(node != null && node.isRequireApproval());

        Label approverLabel = new Label("审批人（可多选，不选表示不限制）：");
        approverLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600;");
        userListView = new ListView<>();
        userListView.setPrefHeight(220);
        userListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        userListView.setPlaceholder(new Label("加载用户列表中..."));

        container.getChildren().addAll(requireApprovalCheck, approverLabel, userListView);
        return container;
    }

    private void loadUsersAsync() {
        new Thread(() -> {
            try {
                PermissionManageService service = new PermissionManageService();
                List<UserListVO> users = service.listUsers();
                Platform.runLater(() -> {
                    if (node != null && node.getApproverUserIds() != null) {
                        selectedUserIds.clear();
                        selectedUserIds.addAll(node.getApproverUserIds());
                    }
                    userListView.setItems(FXCollections.observableList(users != null ? users : new ArrayList<>()));
                    userListView.setCellFactory(lv -> new ListCell<>() {
                        @Override
                        protected void updateItem(UserListVO item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                CheckBox cb = new CheckBox(item.getUsername() != null ? item.getUsername() : "id:" + item.getId());
                                cb.setSelected(selectedUserIds.contains(item.getId()));
                                cb.setOnAction(e -> {
                                    if (cb.isSelected()) {
                                        if (!selectedUserIds.contains(item.getId())) selectedUserIds.add(item.getId());
                                    } else {
                                        selectedUserIds.remove(item.getId());
                                    }
                                });
                                setGraphic(cb);
                                setText(null);
                            }
                        }
                    });
                    userListView.setPlaceholder(new Label("暂无用户数据"));
                });
            } catch (Exception e) {
                logger.warn("加载用户列表失败: {}", e.getMessage());
                Platform.runLater(() -> {
                    userListView.setPlaceholder(new Label("无法加载用户列表，请检查权限或网络"));
                    userListView.setItems(FXCollections.observableArrayList());
                });
            }
        }).start();
    }
}
