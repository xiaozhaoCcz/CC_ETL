package com.cc.job.gui.view;

import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.service.JobPartService;
import com.cc.job.gui.service.PermissionManageService;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.entity.JobRole;
import com.cc.job.xo.model.vo.JobPartVo;
import com.cc.job.xo.model.vo.UserListVO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 权限管理对话框：用户与角色分配、任务分区/任务组/任务节点资源权限配置。
 * 超级管理员（用户ID=1）不可取消管理员角色（角色ID=1）。
 */
public class PermissionManageDialog extends Dialog<Void> {

    private static final Logger logger = LoggerFactory.getLogger(PermissionManageDialog.class);

    /** 超级管理员用户ID，不可删除、不可取消管理员角色 */
    private static final long SUPER_ADMIN_USER_ID = 1L;
    /** 超级管理员角色ID，不可从用户1移除 */
    private static final long SUPER_ADMIN_ROLE_ID = 1L;

    private static final String RESOURCE_PART = "PART";
    private static final String RESOURCE_JOB_INFO = "JOB_INFO";
    private static final String RESOURCE_JOB_NODE = "JOB_NODE";
    private static final String PERM_VIEW = "VIEW";
    private static final String PERM_EDIT = "EDIT";
    private static final String PERM_DELETE = "DELETE";
    private static final String PERM_EXECUTE = "EXECUTE";

    private final PermissionManageService permissionService = new PermissionManageService();
    private final JobPartService jobPartService = new JobPartService();
    private final JobInfoService jobInfoService = new JobInfoService();

    private ListView<UserListVO> userList;
    private ListView<JobRole> userRolesList;
    private ListView<JobRole> availableRolesList;
    private ObservableList<UserListVO> userItems;
    private ObservableList<JobRole> userRolesItems;
    private ObservableList<JobRole> allRolesItems;
    private ObservableList<JobRole> availableRolesItems;
    private Label currentUserLabel;
    private Button assignRoleButton;
    private Button removeRoleButton;

    // 资源权限 Tab
    private ComboBox<UserListVO> resourceUserCombo;
    private ComboBox<String> resourceTypeCombo;
    private ComboBox<String> permissionTypeCombo;
    private TableView<Map<String, Object>> resourcePermissionTable;
    private ObservableList<Map<String, Object>> resourcePermissionItems;
    private VBox resourcePickerPane;
    private ComboBox<JobPartVo> partOrJobCombo;
    private ComboBox<JobPartVo> taskGroupForNodeCombo;
    private ListView<JobNode> nodeListView;
    private Button grantResourceButton;
    private List<JobPartVo> flatParts = new ArrayList<>();
    private List<JobPartVo> flatTaskGroups = new ArrayList<>();

    public PermissionManageDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("权限管理");
        setHeaderText("用户与角色、资源权限管理（仅管理员可见）");

        TabPane tabPane = new TabPane();
        Tab roleTab = new Tab("用户与角色", createRoleTabContent());
        roleTab.setClosable(false);
        Tab resourceTab = new Tab("资源权限", createResourcePermissionTabContent());
        resourceTab.setClosable(false);
        tabPane.getTabs().addAll(roleTab, resourceTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((o, old, tab) -> {
            if (tab != null && "资源权限".equals(tab.getText())) loadTreeForResourcePicker();
        });
        getDialogPane().setContent(tabPane);
        String cssUrl = ThemeManager.getInstance().getStylesheetUrl();
        if (cssUrl != null && !cssUrl.isEmpty()) {
            getDialogPane().getStylesheets().add(cssUrl);
        }

        ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().add(closeButtonType);
        getDialogPane().setPrefWidth(920);
        getDialogPane().setPrefHeight(640);
        getDialogPane().setMinWidth(520);
        getDialogPane().setMinHeight(400);
        setOnShown(e -> {
            javafx.stage.Window w = getDialogPane().getScene().getWindow();
            if (w instanceof Stage) ((Stage) w).setResizable(true);
        });

        loadUsers();
        loadRoles();
    }

    private VBox createRoleTabContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        Label hint = new Label("说明：超级管理员（用户ID=1）不可删除，且不可取消其管理员角色。");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint-label");

        HBox main = new HBox(16);
        main.setAlignment(Pos.CENTER_LEFT);

        // 左侧：用户列表
        VBox left = new VBox(8);
        left.setMinWidth(220);
        left.getChildren().add(new Label("用户列表"));
        userItems = FXCollections.observableArrayList();
        userList = new ListView<>(userItems);
        userList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserListVO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername() + " (ID:" + item.getId() + ")");
            }
        });
        userList.getSelectionModel().selectedItemProperty().addListener((o, old, selected) -> onUserSelected(selected));
        VBox.setVgrow(userList, Priority.ALWAYS);
        left.getChildren().add(userList);

        // 中间：当前用户已分配角色 + 操作按钮
        VBox center = new VBox(8);
        center.setMinWidth(240);
        currentUserLabel = new Label("请选择用户");
        userRolesItems = FXCollections.observableArrayList();
        userRolesList = new ListView<>(userRolesItems);
        userRolesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JobRole item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String suffix = (item.getId() != null && item.getId() == SUPER_ADMIN_ROLE_ID) ? " [超级管理员]" : "";
                setText((item.getRoleName() != null ? item.getRoleName() : item.getRoleCode()) + suffix);
            }
        });
        removeRoleButton = new Button("移除选中角色");
        removeRoleButton.setOnAction(e -> removeSelectedRole());
        center.getChildren().addAll(currentUserLabel, userRolesList, removeRoleButton);
        VBox.setVgrow(userRolesList, Priority.ALWAYS);

        // 右侧：可分配角色列表（当前用户尚未拥有的角色）+ 分配按钮
        VBox right = new VBox(8);
        right.setMinWidth(220);
        right.getChildren().add(new Label("可分配角色"));
        allRolesItems = FXCollections.observableArrayList();
        availableRolesItems = FXCollections.observableArrayList();
        availableRolesList = new ListView<>(availableRolesItems);
        availableRolesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JobRole item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getRoleName() != null ? item.getRoleName() : item.getRoleCode()));
            }
        });
        assignRoleButton = new Button("分配选中角色");
        assignRoleButton.setOnAction(e -> assignSelectedRole());
        right.getChildren().addAll(availableRolesList, assignRoleButton);
        VBox.setVgrow(availableRolesList, Priority.ALWAYS);

        main.getChildren().addAll(left, center, right);
        HBox.setHgrow(main, Priority.ALWAYS);
        root.getChildren().addAll(hint, main);
        VBox.setVgrow(main, Priority.ALWAYS);
        return root;
    }

    private VBox createResourcePermissionTabContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        // 用户、资源类型、权限类型
        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        resourceUserCombo = new ComboBox<>();
        resourceUserCombo.setPromptText("选择用户");
        resourceUserCombo.setMinWidth(180);
        resourceUserCombo.setButtonCell(userListCell());
        resourceUserCombo.setCellFactory(lv -> userListCell());
        resourceTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "任务分区(PART)", "任务组(JOB_INFO)", "任务节点(JOB_NODE)"));
        resourceTypeCombo.getSelectionModel().selectFirst();
        resourceTypeCombo.setMinWidth(160);
        permissionTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "查看(VIEW)", "编辑(EDIT)", "删除(DELETE)", "执行(EXECUTE)"));
        permissionTypeCombo.getSelectionModel().selectFirst();
        permissionTypeCombo.setMinWidth(120);
        resourceUserCombo.getSelectionModel().selectedItemProperty().addListener((o, old, u) -> {
            if (u != null) loadResourcePermissions(u.getId());
        });
        resourceTypeCombo.getSelectionModel().selectedItemProperty().addListener((o, old, t) -> {
            onResourceTypeChanged();
            UserListVO u = resourceUserCombo.getSelectionModel().getSelectedItem();
            if (u != null) loadResourcePermissions(u.getId());
        });
        filterRow.getChildren().addAll(
                new Label("用户:"), resourceUserCombo,
                new Label("资源类型:"), resourceTypeCombo,
                new Label("权限类型:"), permissionTypeCombo);

        // 当前授权列表
        Label listLabel = new Label("当前授权列表");
        resourcePermissionItems = FXCollections.observableArrayList();
        resourcePermissionTable = new TableView<>(resourcePermissionItems);
        resourcePermissionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Map<String, Object>, String> colType = new TableColumn<>("资源类型");
        colType.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue() != null && c.getValue().get("resourceType") != null ? c.getValue().get("resourceType").toString() : ""));
        TableColumn<Map<String, Object>, String> colId = new TableColumn<>("资源ID");
        colId.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue() != null && c.getValue().get("resourceId") != null ? c.getValue().get("resourceId").toString() : ""));
        TableColumn<Map<String, Object>, String> colPerm = new TableColumn<>("权限类型");
        colPerm.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue() != null && c.getValue().get("permissionType") != null ? c.getValue().get("permissionType").toString() : ""));
        TableColumn<Map<String, Object>, Void> colAction = new TableColumn<>("操作");
        colAction.setCellFactory(tc -> new TableCell<>() {
            private final Button revokeBtn = new Button("撤销");
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Map<String, Object> row = getTableRow().getItem();
                revokeBtn.setOnAction(e -> revokeResourcePermissionRow(row));
                setGraphic(revokeBtn);
            }
        });
        resourcePermissionTable.getColumns().addAll(colType, colId, colPerm, colAction);
        resourcePermissionTable.setPrefHeight(140);

        // 新增授权：资源选择器 + 授权按钮
        Label addLabel = new Label("新增授权");
        resourcePickerPane = new VBox(8);
        partOrJobCombo = new ComboBox<>();
        partOrJobCombo.setMinWidth(280);
        taskGroupForNodeCombo = new ComboBox<>();
        taskGroupForNodeCombo.setMinWidth(280);
        taskGroupForNodeCombo.setPromptText("先选择任务组");
        taskGroupForNodeCombo.getSelectionModel().selectedItemProperty().addListener((o, old, g) -> {
            if (g != null) loadNodesForTaskGroup(g.getId());
        });
        ObservableList<JobNode> nodeItems = FXCollections.observableArrayList();
        nodeListView = new ListView<>(nodeItems);
        nodeListView.setMinHeight(120);
        nodeListView.setPrefHeight(160);
        nodeListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JobNode node, boolean empty) {
                super.updateItem(node, empty);
                if (empty || node == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                String id = node.getId() != null ? node.getId().toString() : "-";
                String taskName = getNodeTaskName(node);
                String type = node.getNodeType() != null && !node.getNodeType().isEmpty() ? node.getNodeType() : null;
                String remark = node.getRemark() != null && !node.getRemark().isEmpty() ? node.getRemark() : null;
                Long jobId = node.getJobId();
                StringBuilder sb = new StringBuilder();
                sb.append("任务名称:").append(taskName != null && !taskName.isEmpty() ? taskName : "未命名");
                sb.append(" | ID:").append(id);
                if (type != null) sb.append(" | 类型:").append(type);
                if (jobId != null) sb.append(" | 任务组:").append(jobId);
                if (remark != null) sb.append(" | 备注:").append(remark.length() > 15 ? remark.substring(0, 15) + "…" : remark);
                setText(sb.toString());
                String tooltip = "任务名称: " + (taskName != null && !taskName.isEmpty() ? taskName : "未命名")
                        + "\n节点ID: " + id
                        + (type != null ? "\n类型: " + type : "")
                        + (jobId != null ? "\n任务组ID: " + jobId : "")
                        + (remark != null ? "\n备注: " + remark : "");
                setTooltip(new Tooltip(tooltip));
            }
        });
        grantResourceButton = new Button("授权");
        grantResourceButton.setOnAction(e -> grantResourcePermission());
        buildResourcePickerContent();
        resourceTypeCombo.getSelectionModel().selectedItemProperty().addListener((o, old, t) -> buildResourcePickerContent());

        VBox addSection = new VBox(8);
        addSection.getChildren().addAll(addLabel, resourcePickerPane, grantResourceButton);

        root.getChildren().addAll(filterRow, listLabel, resourcePermissionTable, addSection);
        VBox.setVgrow(resourcePermissionTable, Priority.SOMETIMES);
        return root;
    }

    private void buildResourcePickerContent() {
        resourcePickerPane.getChildren().clear();
        String sel = resourceTypeCombo.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (sel.startsWith("任务分区")) {
            resourcePickerPane.getChildren().add(new Label("选择分区:"));
            partOrJobCombo.setItems(FXCollections.observableArrayList(flatParts));
            partOrJobCombo.setButtonCell(partJobCell());
            partOrJobCombo.setCellFactory(lv -> partJobCell());
            resourcePickerPane.getChildren().add(partOrJobCombo);
        } else if (sel.startsWith("任务组")) {
            resourcePickerPane.getChildren().add(new Label("选择任务组:"));
            partOrJobCombo.setItems(FXCollections.observableArrayList(flatTaskGroups));
            partOrJobCombo.setButtonCell(partJobCell());
            partOrJobCombo.setCellFactory(lv -> partJobCell());
            resourcePickerPane.getChildren().add(partOrJobCombo);
        } else {
            resourcePickerPane.getChildren().add(new Label("选择任务组:"));
            taskGroupForNodeCombo.setItems(FXCollections.observableArrayList(flatTaskGroups));
            taskGroupForNodeCombo.setButtonCell(partJobCell());
            taskGroupForNodeCombo.setCellFactory(lv -> partJobCell());
            resourcePickerPane.getChildren().add(taskGroupForNodeCombo);
            resourcePickerPane.getChildren().add(new Label("选择节点:"));
            resourcePickerPane.getChildren().add(nodeListView);
        }
    }

    private ListCell<UserListVO> userListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(UserListVO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getUsername() != null ? item.getUsername() : "") + " (ID:" + (item.getId() != null ? item.getId() : "") + ")");
            }
        };
    }

    private ListCell<JobPartVo> partJobCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(JobPartVo item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getLabel() != null ? item.getLabel() : "") + " (ID:" + item.getId() + ")");
            }
        };
    }

    private void onResourceTypeChanged() {
        partOrJobCombo.getSelectionModel().clearSelection();
        taskGroupForNodeCombo.getSelectionModel().clearSelection();
        nodeListView.getItems().clear();
        loadTreeForResourcePicker();
    }

    private void loadTreeForResourcePicker() {
        new Thread(() -> {
            try {
                List<JobPartVo> tree = jobPartService.getTree();
                Platform.runLater(() -> {
                    flatParts.clear();
                    flatTaskGroups.clear();
                    flattenByType(flatParts, tree, 0);
                    flattenByType(flatTaskGroups, tree, 1);
                    buildResourcePickerContent();
                });
            } catch (IOException ex) {
                logger.warn("加载分区树失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取分区/任务组列表失败: " + ex.getMessage()));
            }
        }).start();
    }

    private static void flattenByType(List<JobPartVo> out, List<JobPartVo> tree, int type) {
        if (tree == null) return;
        for (JobPartVo n : tree) {
            if (n.getType() != null && n.getType() == type) out.add(n);
            flattenByType(out, n.getChildren() != null ? n.getChildren() : new ArrayList<>(), type);
        }
    }

    /** 从节点 properties JSON 中解析任务名称（优先 jobDesc、labelText、executorHandler） */
    private static String getNodeTaskName(JobNode node) {
        String props = node.getProperties();
        if (props == null || props.isEmpty()) return null;
        try {
            JsonObject o = new Gson().fromJson(props, JsonObject.class);
            if (o == null) return null;
            if (o.has("jobDesc")) {
                String v = o.get("jobDesc").getAsString();
                if (v != null && !v.isEmpty()) return v;
            }
            if (o.has("labelText")) {
                String v = o.get("labelText").getAsString();
                if (v != null && !v.isEmpty()) return v;
            }
            if (o.has("executorHandler")) {
                String v = o.get("executorHandler").getAsString();
                if (v != null && !v.isEmpty()) return v;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private void loadNodesForTaskGroup(Long taskGroupId) {
        nodeListView.getItems().clear();
        new Thread(() -> {
            try {
                List<JobNode> nodes = jobInfoService.getJobNodes(taskGroupId);
                Platform.runLater(() -> nodeListView.getItems().setAll(nodes != null ? nodes : List.of()));
            } catch (IOException ex) {
                logger.warn("加载节点列表失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取节点列表失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void loadResourcePermissions(Long userId) {
        String resourceType = getSelectedResourceTypeCode();
        new Thread(() -> {
            try {
                List<Map<String, Object>> list = permissionService.listResourcePermissions(userId, resourceType);
                Platform.runLater(() -> {
                    resourcePermissionItems.clear();
                    if (list != null) resourcePermissionItems.addAll(list);
                });
            } catch (IOException ex) {
                logger.warn("加载资源授权列表失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取资源授权列表失败: " + ex.getMessage()));
            }
        }).start();
    }

    private String getSelectedResourceTypeCode() {
        String sel = resourceTypeCombo.getSelectionModel().getSelectedItem();
        if (sel == null) return null;
        if (sel.contains("PART")) return RESOURCE_PART;
        if (sel.contains("JOB_INFO")) return RESOURCE_JOB_INFO;
        if (sel.contains("JOB_NODE")) return RESOURCE_JOB_NODE;
        return null;
    }

    private String getSelectedPermissionTypeCode() {
        String sel = permissionTypeCombo.getSelectionModel().getSelectedItem();
        if (sel == null) return PERM_VIEW;
        if (sel.contains("VIEW")) return PERM_VIEW;
        if (sel.contains("EDIT")) return PERM_EDIT;
        if (sel.contains("DELETE")) return PERM_DELETE;
        if (sel.contains("EXECUTE")) return PERM_EXECUTE;
        return PERM_VIEW;
    }

    private void revokeResourcePermissionRow(Map<String, Object> row) {
        Object idObj = row.get("id");
        Long id = idObj instanceof Number ? ((Number) idObj).longValue() : null;
        if (id == null) return;
        new Thread(() -> {
            try {
                permissionService.revokeResourcePermission(id);
                UserListVO u = resourceUserCombo.getSelectionModel().getSelectedItem();
                if (u != null) Platform.runLater(() -> loadResourcePermissions(u.getId()));
                Platform.runLater(() -> NotificationToast.showSuccess("已撤销授权"));
            } catch (IOException ex) {
                logger.warn("撤销授权失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("撤销授权失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void grantResourcePermission() {
        UserListVO user = resourceUserCombo.getSelectionModel().getSelectedItem();
        if (user == null) {
            NotificationToast.showWarning("请先选择用户");
            return;
        }
        String resourceType = getSelectedResourceTypeCode();
        String permissionType = getSelectedPermissionTypeCode();
        Long resourceId = null;
        String sel = resourceTypeCombo.getSelectionModel().getSelectedItem();
        if (sel != null && sel.startsWith("任务分区")) {
            JobPartVo part = partOrJobCombo.getSelectionModel().getSelectedItem();
            if (part == null) { NotificationToast.showWarning("请选择分区"); return; }
            resourceId = part.getId();
        } else if (sel != null && sel.startsWith("任务组")) {
            JobPartVo job = partOrJobCombo.getSelectionModel().getSelectedItem();
            if (job == null) { NotificationToast.showWarning("请选择任务组"); return; }
            resourceId = job.getId();
        } else if (sel != null && sel.startsWith("任务节点")) {
            JobNode node = nodeListView.getSelectionModel().getSelectedItem();
            if (node == null || node.getId() == null) { NotificationToast.showWarning("请选择任务组并选择节点"); return; }
            resourceId = node.getId();
        }
        if (resourceId == null) return;
        final Long finalResourceId = resourceId;
        new Thread(() -> {
            try {
                permissionService.grantResourcePermission(user.getId(), resourceType, finalResourceId, permissionType);
                Platform.runLater(() -> loadResourcePermissions(user.getId()));
                Platform.runLater(() -> NotificationToast.showSuccess("已授权"));
            } catch (IOException ex) {
                logger.warn("授权失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("授权失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                List<UserListVO> list = permissionService.listUsers();
                Platform.runLater(() -> {
                    userItems.clear();
                    if (list != null) userItems.addAll(list);
                    if (resourceUserCombo != null) resourceUserCombo.setItems(userItems);
                });
            } catch (IOException ex) {
                logger.warn("加载用户列表失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取用户列表失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void loadRoles() {
        new Thread(() -> {
            try {
                List<JobRole> list = permissionService.listRoles();
                Platform.runLater(() -> {
                    allRolesItems.clear();
                    if (list != null) allRolesItems.addAll(list);
                });
            } catch (IOException ex) {
                logger.warn("加载角色列表失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取角色列表失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void onUserSelected(UserListVO user) {
        if (user == null) {
            currentUserLabel.setText("请选择用户");
            userRolesItems.clear();
            assignRoleButton.setDisable(true);
            removeRoleButton.setDisable(true);
            return;
        }
        currentUserLabel.setText("当前用户: " + user.getUsername() + " (ID:" + user.getId() + ")");
        assignRoleButton.setDisable(false);
        removeRoleButton.setDisable(false);
        loadUserRoles(user.getId());
    }

    private void loadUserRoles(Long userId) {
        userRolesItems.clear();
        new Thread(() -> {
            try {
                List<Long> roleIds = permissionService.getUserRoles(userId);
                List<JobRole> assigned = allRolesItems.stream()
                        .filter(r -> r.getId() != null && roleIds.contains(r.getId()))
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    userRolesItems.addAll(assigned);
                    refreshAvailableRolesList();
                });
            } catch (IOException ex) {
                logger.warn("加载用户角色失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取用户角色失败: " + ex.getMessage()));
            }
        }).start();
    }

    /** 右侧「可分配角色」仅显示当前用户尚未拥有的角色 */
    private void refreshAvailableRolesList() {
        List<Long> assignedIds = userRolesItems.stream()
                .map(JobRole::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        List<JobRole> available = allRolesItems.stream()
                .filter(r -> r.getId() != null && !assignedIds.contains(r.getId()))
                .collect(Collectors.toList());
        availableRolesItems.clear();
        availableRolesItems.addAll(available);
    }

    private void assignSelectedRole() {
        UserListVO user = userList.getSelectionModel().getSelectedItem();
        JobRole role = availableRolesList.getSelectionModel().getSelectedItem();
        if (user == null || role == null) {
            NotificationToast.showWarning("请先选择用户和要分配的角色");
            return;
        }
        Long roleId = role.getId();
        if (roleId == null) return;
        new Thread(() -> {
            try {
                permissionService.assignRole(user.getId(), roleId);
                Platform.runLater(() -> {
                    if (!userRolesItems.contains(role)) userRolesItems.add(role);
                    refreshAvailableRolesList();
                    NotificationToast.showSuccess("已分配角色");
                });
            } catch (IOException ex) {
                logger.warn("分配角色失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("分配角色失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void removeSelectedRole() {
        UserListVO user = userList.getSelectionModel().getSelectedItem();
        JobRole role = userRolesList.getSelectionModel().getSelectedItem();
        if (user == null || role == null) {
            NotificationToast.showWarning("请先选择用户和要移除的角色");
            return;
        }
        Long userId = user.getId();
        Long roleId = role.getId();
        if (roleId == null) return;
        // 禁止移除超级管理员的管理员角色
        if (userId == SUPER_ADMIN_USER_ID && roleId == SUPER_ADMIN_ROLE_ID) {
            NotificationToast.showWarning("超级管理员不可取消管理员角色");
            return;
        }
        new Thread(() -> {
            try {
                permissionService.removeRole(userId, roleId);
                Platform.runLater(() -> {
                    userRolesItems.remove(role);
                    refreshAvailableRolesList();
                    NotificationToast.showSuccess("已移除角色");
                });
            } catch (IOException ex) {
                logger.warn("移除角色失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("移除角色失败: " + ex.getMessage()));
            }
        }).start();
    }
}
