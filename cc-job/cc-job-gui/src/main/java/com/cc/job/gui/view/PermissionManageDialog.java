package com.cc.job.gui.view;

import com.cc.job.gui.service.JobInfoService;
import com.cc.job.gui.service.JobPartService;
import com.cc.job.gui.service.PermissionManageService;
import com.cc.job.gui.util.NotificationToast;
import com.cc.job.gui.util.ThemeManager;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.xo.model.entity.JobPermission;
import com.cc.job.xo.model.entity.JobRole;
import com.cc.job.xo.model.vo.JobPartVo;
import com.cc.job.xo.model.vo.UserListVO;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

    // 角色权限 Tab（配置角色拥有哪些权限）
    private ComboBox<JobRole> rolePermRoleCombo;
    private TableView<PermissionCheckItem> rolePermTable;
    private ObservableList<PermissionCheckItem> rolePermItems;
    private Button saveRolePermButton;

    // 角色管理 Tab（新增/编辑/删除角色）
    private TableView<JobRole> roleManageTable;
    private ObservableList<JobRole> roleManageItems;

    /** 权限项（权限 + 是否勾选），用于角色权限配置表格 */
    public static class PermissionCheckItem {
        private final JobPermission permission;
        private final BooleanProperty selected = new SimpleBooleanProperty(false);
        public PermissionCheckItem(JobPermission permission) { this.permission = permission; }
        public JobPermission getPermission() { return permission; }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean v) { selected.set(v); }
    }

    public PermissionManageDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("权限管理");
        setHeaderText("用户与角色、资源权限管理（仅管理员可见）");

        TabPane tabPane = new TabPane();
        Tab roleTab = new Tab("用户与角色", createRoleTabContent());
        roleTab.setClosable(false);
        Tab roleManageTab = new Tab("角色管理", createRoleManageTabContent());
        roleManageTab.setClosable(false);
        Tab resourceTab = new Tab("资源权限", createResourcePermissionTabContent());
        resourceTab.setClosable(false);
        Tab rolePermTab = new Tab("角色权限", createRolePermissionTabContent());
        rolePermTab.setClosable(false);
        tabPane.getTabs().addAll(roleTab, roleManageTab, resourceTab, rolePermTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((o, old, tab) -> {
            if (tab != null && "资源权限".equals(tab.getText())) loadTreeForResourcePicker();
            if (tab != null && "角色权限".equals(tab.getText())) loadRolePermissionTabData();
            if (tab != null && "角色管理".equals(tab.getText())) loadRoleManageTable();
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

        // 左侧：用户列表 + 新增/删除/重置密码
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
        HBox userButtons = new HBox(8);
        Button addUserBtn = new Button("新增用户");
        addUserBtn.setOnAction(e -> showAddUserDialog());
        Button deleteUserBtn = new Button("删除用户");
        deleteUserBtn.setOnAction(e -> deleteSelectedUser());
        Button resetPwdBtn = new Button("重置密码");
        resetPwdBtn.setOnAction(e -> showResetPasswordDialog());
        userButtons.getChildren().addAll(addUserBtn, deleteUserBtn, resetPwdBtn);
        left.getChildren().add(userList);
        left.getChildren().add(userButtons);

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

    private VBox createRolePermissionTabContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        Label hint = new Label("说明：选择角色后勾选该角色拥有的权限，点击「保存」生效。管理员（角色ID=1）建议保留全部权限。");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint-label");
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getChildren().add(new Label("角色:"));
        rolePermRoleCombo = new ComboBox<>();
        rolePermRoleCombo.setMinWidth(220);
        rolePermRoleCombo.setPromptText("请选择角色");
        rolePermRoleCombo.setButtonCell(roleListCell());
        rolePermRoleCombo.setCellFactory(lv -> roleListCell());
        rolePermRoleCombo.getSelectionModel().selectedItemProperty().addListener((o, old, role) -> {
            if (role != null) loadRolePermissionsForRole(role.getId());
        });
        saveRolePermButton = new Button("保存当前角色权限");
        saveRolePermButton.setOnAction(e -> saveRolePermissions());
        top.getChildren().addAll(rolePermRoleCombo, saveRolePermButton);
        rolePermItems = FXCollections.observableArrayList();
        rolePermTable = new TableView<>(rolePermItems);
        rolePermTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<PermissionCheckItem, Boolean> colSel = new TableColumn<>("拥有");
        colSel.setMinWidth(60);
        colSel.setCellValueFactory(c -> c.getValue().selectedProperty());
        colSel.setCellFactory(tc -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            { check.setOnAction(ev -> {
                PermissionCheckItem item = getTableRow() != null ? getTableRow().getItem() : null;
                if (item != null) item.setSelected(check.isSelected());
            });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                PermissionCheckItem row = getTableRow().getItem();
                check.selectedProperty().unbindBidirectional(row.selectedProperty());
                check.selectedProperty().bindBidirectional(row.selectedProperty());
                setGraphic(check);
            }
        });
        TableColumn<PermissionCheckItem, String> colName = new TableColumn<>("权限名称");
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPermission().getName() != null ? c.getValue().getPermission().getName() : ""));
        TableColumn<PermissionCheckItem, String> colCode = new TableColumn<>("权限码");
        colCode.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPermission().getPermissionCode() != null ? c.getValue().getPermission().getPermissionCode() : ""));
        TableColumn<PermissionCheckItem, String> colDesc = new TableColumn<>("描述");
        colDesc.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPermission().getDescription() != null ? c.getValue().getPermission().getDescription() : ""));
        rolePermTable.getColumns().addAll(colSel, colName, colCode, colDesc);
        rolePermTable.setPrefHeight(320);
        root.getChildren().addAll(hint, top, rolePermTable);
        VBox.setVgrow(rolePermTable, Priority.ALWAYS);
        return root;
    }

    private VBox createRoleManageTabContent() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        Label hint = new Label("说明：可新增角色、编辑角色名称/编码/描述，或删除角色。超级管理员角色（ID=1）不可删除，仅可修改描述。");
        hint.setWrapText(true);
        hint.getStyleClass().add("hint-label");
        HBox buttons = new HBox(8);
        Button addRoleBtn = new Button("新增角色");
        addRoleBtn.setOnAction(e -> showAddRoleDialog());
        Button editRoleBtn = new Button("编辑选中角色");
        editRoleBtn.setOnAction(e -> editSelectedRole());
        Button deleteRoleBtn = new Button("删除选中角色");
        deleteRoleBtn.setOnAction(e -> deleteSelectedRole());
        buttons.getChildren().addAll(addRoleBtn, editRoleBtn, deleteRoleBtn);
        roleManageItems = FXCollections.observableArrayList();
        roleManageTable = new TableView<>(roleManageItems);
        roleManageTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<JobRole, String> colName = new TableColumn<>("角色名称");
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRoleName() != null ? c.getValue().getRoleName() : ""));
        TableColumn<JobRole, String> colCode = new TableColumn<>("角色编码");
        colCode.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRoleCode() != null ? c.getValue().getRoleCode() : ""));
        TableColumn<JobRole, String> colDesc = new TableColumn<>("描述");
        colDesc.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDescription() != null ? c.getValue().getDescription() : ""));
        roleManageTable.getColumns().addAll(colName, colCode, colDesc);
        roleManageTable.setPrefHeight(320);
        root.getChildren().addAll(hint, buttons, roleManageTable);
        VBox.setVgrow(roleManageTable, Priority.ALWAYS);
        return root;
    }

    private void loadRoleManageTable() {
        new Thread(() -> {
            try {
                List<JobRole> list = permissionService.listRoles();
                Platform.runLater(() -> {
                    roleManageItems.clear();
                    if (list != null) roleManageItems.addAll(list);
                });
            } catch (IOException ex) {
                logger.warn("加载角色列表失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取角色列表失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void showAddRoleDialog() {
        Dialog<Map<String, String>> d = new Dialog<>();
        d.setTitle("新增角色");
        d.initOwner(getDialogPane().getScene().getWindow());
        d.initModality(Modality.APPLICATION_MODAL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        TextField nameField = new TextField();
        nameField.setPromptText("角色名称");
        nameField.setPrefWidth(240);
        TextField codeField = new TextField();
        codeField.setPromptText("角色编码（唯一）");
        codeField.setPrefWidth(240);
        TextField descField = new TextField();
        descField.setPromptText("描述");
        descField.setPrefWidth(240);
        grid.add(new Label("角色名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("角色编码:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("描述:"), 0, 2);
        grid.add(descField, 1, 2);
        d.getDialogPane().setContent(grid);
        d.getDialogPane().getButtonTypes().addAll(new ButtonType("确定", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        d.setResultConverter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? Map.of("roleName", nameField.getText().trim(), "roleCode", codeField.getText().trim(), "description", descField.getText().trim())
                : null);
        d.showAndWait().ifPresent(result -> {
            String code = result.get("roleCode");
            if (code == null || code.isEmpty()) {
                NotificationToast.showWarning("角色编码不能为空");
                return;
            }
            new Thread(() -> {
                try {
                    permissionService.createRole(result.get("roleName"), code, result.get("description"));
                    Platform.runLater(() -> {
                        loadRoleManageTable();
                        loadRoles();
                        if (rolePermRoleCombo != null) rolePermRoleCombo.setItems(FXCollections.observableArrayList(allRolesItems));
                        NotificationToast.showSuccess("角色已创建");
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> NotificationToast.showError("创建失败: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void editSelectedRole() {
        JobRole role = roleManageTable.getSelectionModel().getSelectedItem();
        if (role == null) {
            NotificationToast.showWarning("请先选择要编辑的角色");
            return;
        }
        Dialog<Map<String, String>> d = new Dialog<>();
        d.setTitle("编辑角色");
        d.initOwner(getDialogPane().getScene().getWindow());
        d.initModality(Modality.APPLICATION_MODAL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        TextField nameField = new TextField(role.getRoleName() != null ? role.getRoleName() : "");
        nameField.setPrefWidth(240);
        TextField codeField = new TextField(role.getRoleCode() != null ? role.getRoleCode() : "");
        codeField.setPrefWidth(240);
        if (role.getId() != null && role.getId() == SUPER_ADMIN_ROLE_ID) {
            codeField.setDisable(true);
            nameField.setDisable(true);
        }
        TextField descField = new TextField(role.getDescription() != null ? role.getDescription() : "");
        descField.setPrefWidth(240);
        grid.add(new Label("角色名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("角色编码:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("描述:"), 0, 2);
        grid.add(descField, 1, 2);
        d.getDialogPane().setContent(grid);
        d.getDialogPane().getButtonTypes().addAll(new ButtonType("确定", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        d.setResultConverter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE
                ? Map.of("roleName", nameField.getText().trim(), "roleCode", codeField.getText().trim(), "description", descField.getText().trim())
                : null);
        Long roleId = role.getId();
        d.showAndWait().ifPresent(result -> {
            new Thread(() -> {
                try {
                    permissionService.updateRole(roleId, result.get("roleName"), result.get("roleCode"), result.get("description"));
                    Platform.runLater(() -> {
                        loadRoleManageTable();
                        loadRoles();
                        loadRolePermissionTabData();
                        NotificationToast.showSuccess("已保存");
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> NotificationToast.showError("保存失败: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void deleteSelectedRole() {
        JobRole role = roleManageTable.getSelectionModel().getSelectedItem();
        if (role == null) {
            NotificationToast.showWarning("请先选择要删除的角色");
            return;
        }
        if (role.getId() != null && role.getId() == SUPER_ADMIN_ROLE_ID) {
            NotificationToast.showWarning("不能删除超级管理员角色");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("删除角色 \"" + (role.getRoleName() != null ? role.getRoleName() : role.getRoleCode()) + "\"？");
        confirm.initOwner(getDialogPane().getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        new Thread(() -> {
            try {
                permissionService.deleteRole(role.getId());
                Platform.runLater(() -> {
                    loadRoleManageTable();
                    loadRoles();
                    loadRolePermissionTabData();
                    NotificationToast.showSuccess("已删除角色");
                });
            } catch (IOException ex) {
                Platform.runLater(() -> NotificationToast.showError("删除失败: " + ex.getMessage()));
            }
        }).start();
    }

    private ListCell<JobRole> roleListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(JobRole item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getRoleName() != null ? item.getRoleName() : item.getRoleCode()) + " (ID:" + (item.getId() != null ? item.getId() : "") + ")");
            }
        };
    }

    private void loadRolePermissionTabData() {
        if (rolePermRoleCombo == null) return;
        rolePermRoleCombo.setItems(FXCollections.observableArrayList(allRolesItems));
        new Thread(() -> {
            try {
                List<JobPermission> perms = permissionService.listPermissions();
                Platform.runLater(() -> {
                    rolePermItems.clear();
                    if (perms != null) {
                        for (JobPermission p : perms) {
                            rolePermItems.add(new PermissionCheckItem(p));
                        }
                    }
                    JobRole sel = rolePermRoleCombo.getSelectionModel().getSelectedItem();
                    if (sel != null) loadRolePermissionsForRole(sel.getId());
                });
            } catch (IOException ex) {
                logger.warn("加载权限列表失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取权限列表失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void loadRolePermissionsForRole(Long roleId) {
        if (roleId == null || rolePermItems.isEmpty()) return;
        new Thread(() -> {
            try {
                List<Long> permIds = permissionService.getRolePermissions(roleId);
                Platform.runLater(() -> {
                    for (PermissionCheckItem item : rolePermItems) {
                        Long id = item.getPermission().getId();
                        item.setSelected(id != null && permIds.contains(id));
                    }
                });
            } catch (IOException ex) {
                logger.warn("加载角色权限失败: {}", ex.getMessage());
                Platform.runLater(() -> NotificationToast.showError("获取角色权限失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void saveRolePermissions() {
        JobRole role = rolePermRoleCombo != null ? rolePermRoleCombo.getSelectionModel().getSelectedItem() : null;
        if (role == null || role.getId() == null) {
            NotificationToast.showWarning("请先选择角色");
            return;
        }
        List<Long> selectedIds = rolePermItems.stream()
                .filter(PermissionCheckItem::isSelected)
                .map(item -> item.getPermission().getId())
                .filter(id -> id != null)
                .collect(Collectors.toList());
        final Long roleId = role.getId();
        saveRolePermButton.setDisable(true);
        new Thread(() -> {
            try {
                permissionService.updateRolePermissions(roleId, selectedIds);
                Platform.runLater(() -> {
                    NotificationToast.showSuccess("已保存角色权限");
                    saveRolePermButton.setDisable(false);
                });
            } catch (IOException ex) {
                logger.warn("保存角色权限失败: {}", ex.getMessage());
                Platform.runLater(() -> {
                    NotificationToast.showError("保存失败: " + ex.getMessage());
                    saveRolePermButton.setDisable(false);
                });
            }
        }).start();
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

    private void showAddUserDialog() {
        Dialog<Map<String, String>> d = new Dialog<>();
        d.setTitle("新增用户");
        d.initOwner(getDialogPane().getScene().getWindow());
        d.initModality(Modality.APPLICATION_MODAL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        TextField usernameField = new TextField();
        usernameField.setPromptText("3-20个字符");
        usernameField.setPrefWidth(240);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("至少6位");
        passwordField.setPrefWidth(240);
        grid.add(new Label("用户名:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("初始密码:"), 0, 1);
        grid.add(passwordField, 1, 1);
        d.getDialogPane().setContent(grid);
        ButtonType ok = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        d.setResultConverter(bt -> bt == ok ? Map.of("username", usernameField.getText().trim(), "password", passwordField.getText()) : null);
        d.showAndWait().ifPresent(result -> {
            String username = result.get("username");
            String password = result.get("password");
            if (username == null || username.length() < 3) {
                NotificationToast.showWarning("用户名至少3个字符");
                return;
            }
            if (password == null || password.length() < 6) {
                NotificationToast.showWarning("密码至少6位");
                return;
            }
            new Thread(() -> {
                try {
                    permissionService.createUser(username, password);
                    Platform.runLater(() -> {
                        loadUsers();
                        NotificationToast.showSuccess("用户已创建");
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> NotificationToast.showError("创建失败: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void deleteSelectedUser() {
        UserListVO user = userList.getSelectionModel().getSelectedItem();
        if (user == null) {
            NotificationToast.showWarning("请先选择要删除的用户");
            return;
        }
        if (user.getId() != null && user.getId() == SUPER_ADMIN_USER_ID) {
            NotificationToast.showWarning("不能删除超级管理员用户");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("删除用户 \"" + (user.getUsername() != null ? user.getUsername() : "") + "\"？");
        confirm.setContentText("将同时解除该用户的角色与资源权限，且不可恢复。");
        confirm.initOwner(getDialogPane().getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        new Thread(() -> {
            try {
                permissionService.deleteUser(user.getId());
                Platform.runLater(() -> {
                    loadUsers();
                    currentUserLabel.setText("请选择用户");
                    userRolesItems.clear();
                    NotificationToast.showSuccess("已删除用户");
                });
            } catch (IOException ex) {
                Platform.runLater(() -> NotificationToast.showError("删除失败: " + ex.getMessage()));
            }
        }).start();
    }

    private void showResetPasswordDialog() {
        UserListVO user = userList.getSelectionModel().getSelectedItem();
        if (user == null) {
            NotificationToast.showWarning("请先选择要重置密码的用户");
            return;
        }
        Dialog<String> d = new Dialog<>();
        d.setTitle("重置密码");
        d.initOwner(getDialogPane().getScene().getWindow());
        d.initModality(Modality.APPLICATION_MODAL);
        VBox v = new VBox(10);
        v.setPadding(new Insets(20));
        v.getChildren().add(new Label("用户: " + (user.getUsername() != null ? user.getUsername() : user.getId())));
        PasswordField pwd = new PasswordField();
        pwd.setPromptText("新密码（至少6位）");
        pwd.setPrefWidth(240);
        v.getChildren().add(pwd);
        d.getDialogPane().setContent(v);
        d.getDialogPane().getButtonTypes().addAll(new ButtonType("确定", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        d.setResultConverter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE ? pwd.getText() : null);
        d.showAndWait().ifPresent(newPassword -> {
            if (newPassword == null || newPassword.length() < 6) {
                NotificationToast.showWarning("密码至少6位");
                return;
            }
            new Thread(() -> {
                try {
                    permissionService.resetPassword(user.getId(), newPassword);
                    Platform.runLater(() -> NotificationToast.showSuccess("密码已重置"));
                } catch (IOException ex) {
                    Platform.runLater(() -> NotificationToast.showError("重置失败: " + ex.getMessage()));
                }
            }).start();
        });
    }
}
