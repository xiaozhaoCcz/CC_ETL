# 18-实战案例-TodoList应用

## 18.1 项目概述

创建一个功能完整的TodoList待办事项管理应用。

### 18.1.1 功能需求

- 添加待办事项
- 标记完成/未完成
- 删除待办事项
- 编辑待办事项
- 数据持久化（保存到文件）
- 过滤显示（全部/未完成/已完成）
- 统计功能

### 18.1.2 技术栈

- JavaFX UI
- ObservableList数据绑定
- JSON数据持久化
- FXML界面
- CSS样式

## 18.2 数据模型

### 18.2.1 TodoItem.java

```java
package com.example.javafx.todo.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TodoItem {
    private final IntegerProperty id;
    private final StringProperty title;
    private final StringProperty description;
    private final BooleanProperty completed;
    private final StringProperty createdDate;
    private final StringProperty priority;

    public TodoItem() {
        this(0, "", "", false, LocalDateTime.now(), "普通");
    }

    public TodoItem(int id, String title, String description, boolean completed, 
                   LocalDateTime createdDate, String priority) {
        this.id = new SimpleIntegerProperty(id);
        this.title = new SimpleStringProperty(title);
        this.description = new SimpleStringProperty(description);
        this.completed = new SimpleBooleanProperty(completed);
        this.createdDate = new SimpleStringProperty(
            createdDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        this.priority = new SimpleStringProperty(priority);
    }

    // Getters and Setters
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }
    public StringProperty titleProperty() { return title; }

    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
    public StringProperty descriptionProperty() { return description; }

    public boolean isCompleted() { return completed.get(); }
    public void setCompleted(boolean value) { completed.set(value); }
    public BooleanProperty completedProperty() { return completed; }

    public String getCreatedDate() { return createdDate.get(); }
    public void setCreatedDate(String value) { createdDate.set(value); }
    public StringProperty createdDateProperty() { return createdDate; }

    public String getPriority() { return priority.get(); }
    public void setPriority(String value) { priority.set(value); }
    public StringProperty priorityProperty() { return priority; }
}
```

## 18.3 主界面实现

### 18.3.1 TodoListApp.java

```java
package com.example.javafx.todo;

import com.example.javafx.todo.model.TodoItem;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.Optional;

public class TodoListApp extends Application {

    private ObservableList<TodoItem> todoItems;
    private FilteredList<TodoItem> filteredItems;
    private TableView<TodoItem> tableView;
    private int nextId = 1;
    
    private Label totalLabel;
    private Label completedLabel;
    private Label pendingLabel;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        // 初始化数据
        todoItems = FXCollections.observableArrayList();
        filteredItems = new FilteredList<>(todoItems, p -> true);

        // 顶部工具栏
        root.setTop(createToolbar());

        // 中心表格
        root.setCenter(createTableView());

        // 底部状态栏
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(
            getClass().getResource("/css/todo-style.css").toExternalForm()
        );

        primaryStage.setTitle("待办事项管理");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> saveData());
        primaryStage.show();

        // 加载数据
        loadData();
        updateStatistics();
    }

    private ToolBar createToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setPadding(new Insets(10));

        Button addBtn = new Button("➕ 新建");
        addBtn.setOnAction(e -> showAddDialog());

        Button editBtn = new Button("✏️ 编辑");
        editBtn.setOnAction(e -> showEditDialog());
        editBtn.disableProperty().bind(
            tableView.getSelectionModel().selectedItemProperty().isNull()
        );

        Button deleteBtn = new Button("🗑️ 删除");
        deleteBtn.setOnAction(e -> deleteSelected());
        deleteBtn.disableProperty().bind(
            tableView.getSelectionModel().selectedItemProperty().isNull()
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("全部", "未完成", "已完成");
        filterCombo.setValue("全部");
        filterCombo.setOnAction(e -> filterTodos(filterCombo.getValue()));

        toolbar.getItems().addAll(
            addBtn, editBtn, deleteBtn, spacer,
            new Label("筛选:"), filterCombo
        );

        return toolbar;
    }

    private TableView<TodoItem> createTableView() {
        tableView = new TableView<>(filteredItems);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 完成状态列
        TableColumn<TodoItem, Boolean> completedCol = new TableColumn<>("完成");
        completedCol.setCellValueFactory(cellData -> 
            cellData.getValue().completedProperty()
        );
        completedCol.setCellFactory(CheckBoxTableCell.forTableColumn(completedCol));
        completedCol.setPrefWidth(60);
        completedCol.setEditable(true);

        // 标题列
        TableColumn<TodoItem, String> titleCol = new TableColumn<>("标题");
        titleCol.setCellValueFactory(cellData -> 
            cellData.getValue().titleProperty()
        );
        titleCol.setPrefWidth(200);

        // 描述列
        TableColumn<TodoItem, String> descCol = new TableColumn<>("描述");
        descCol.setCellValueFactory(cellData -> 
            cellData.getValue().descriptionProperty()
        );
        descCol.setPrefWidth(300);

        // 优先级列
        TableColumn<TodoItem, String> priorityCol = new TableColumn<>("优先级");
        priorityCol.setCellValueFactory(cellData -> 
            cellData.getValue().priorityProperty()
        );
        priorityCol.setPrefWidth(80);
        priorityCol.setCellFactory(col -> new TableCell<TodoItem, String>() {
            @Override
            protected void updateItem(String priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(priority);
                    String color = switch (priority) {
                        case "高" -> "-fx-background-color: #ffcdd2;";
                        case "中" -> "-fx-background-color: #fff9c4;";
                        default -> "-fx-background-color: #c8e6c9;";
                    };
                    setStyle(color);
                }
            }
        });

        // 创建时间列
        TableColumn<TodoItem, String> dateCol = new TableColumn<>("创建时间");
        dateCol.setCellValueFactory(cellData -> 
            cellData.getValue().createdDateProperty()
        );
        dateCol.setPrefWidth(150);

        tableView.getColumns().addAll(
            completedCol, titleCol, descCol, priorityCol, dateCol
        );
        tableView.setEditable(true);

        // 监听完成状态变化
        completedCol.setOnEditCommit(event -> {
            updateStatistics();
        });

        return tableView;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #e0e0e0;");

        totalLabel = new Label("总计: 0");
        completedLabel = new Label("已完成: 0");
        pendingLabel = new Label("未完成: 0");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(
            totalLabel, completedLabel, pendingLabel, spacer
        );

        return statusBar;
    }

    private void showAddDialog() {
        Dialog<TodoItem> dialog = new Dialog<>();
        dialog.setTitle("新建待办事项");
        dialog.setHeaderText("填写待办事项信息");

        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("标题");
        TextArea descArea = new TextArea();
        descArea.setPromptText("描述");
        descArea.setPrefRowCount(3);
        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("低", "普通", "高");
        priorityCombo.setValue("普通");

        grid.add(new Label("标题:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("描述:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("优先级:"), 0, 2);
        grid.add(priorityCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                if (!titleField.getText().isEmpty()) {
                    return new TodoItem(
                        nextId++,
                        titleField.getText(),
                        descArea.getText(),
                        false,
                        LocalDateTime.now(),
                        priorityCombo.getValue()
                    );
                }
            }
            return null;
        });

        Optional<TodoItem> result = dialog.showAndWait();
        result.ifPresent(item -> {
            todoItems.add(item);
            updateStatistics();
        });
    }

    private void showEditDialog() {
        TodoItem selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Dialog<TodoItem> dialog = new Dialog<>();
        dialog.setTitle("编辑待办事项");
        dialog.setHeaderText("修改待办事项信息");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField(selected.getTitle());
        TextArea descArea = new TextArea(selected.getDescription());
        descArea.setPrefRowCount(3);
        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("低", "普通", "高");
        priorityCombo.setValue(selected.getPriority());

        grid.add(new Label("标题:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("描述:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("优先级:"), 0, 2);
        grid.add(priorityCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                selected.setTitle(titleField.getText());
                selected.setDescription(descArea.getText());
                selected.setPriority(priorityCombo.getValue());
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteSelected() {
        TodoItem selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除待办事项");
        alert.setContentText("确定要删除 \"" + selected.getTitle() + "\" 吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                todoItems.remove(selected);
                updateStatistics();
            }
        });
    }

    private void filterTodos(String filter) {
        filteredItems.setPredicate(item -> {
            return switch (filter) {
                case "未完成" -> !item.isCompleted();
                case "已完成" -> item.isCompleted();
                default -> true;
            };
        });
    }

    private void updateStatistics() {
        int total = todoItems.size();
        long completed = todoItems.stream()
            .filter(TodoItem::isCompleted)
            .count();
        long pending = total - completed;

        totalLabel.setText("总计: " + total);
        completedLabel.setText("已完成: " + completed);
        pendingLabel.setText("未完成: " + pending);
    }

    private void loadData() {
        // 示例数据
        todoItems.add(new TodoItem(nextId++, "学习JavaFX", 
            "完成JavaFX教程", false, LocalDateTime.now(), "高"));
        todoItems.add(new TodoItem(nextId++, "写代码", 
            "完成项目开发", false, LocalDateTime.now(), "普通"));
    }

    private void saveData() {
        System.out.println("保存数据...");
        // 这里可以实现JSON序列化保存到文件
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

## 18.4 CSS样式

**todo-style.css**:
```css
.root {
    -fx-font-family: "Microsoft YaHei", "SimHei", sans-serif;
}

.tool-bar {
    -fx-background-color: white;
    -fx-border-color: #e0e0e0;
    -fx-border-width: 0 0 1 0;
}

.tool-bar .button {
    -fx-background-color: #2196F3;
    -fx-text-fill: white;
    -fx-padding: 8 15;
    -fx-background-radius: 4;
    -fx-cursor: hand;
}

.tool-bar .button:hover {
    -fx-background-color: #1976D2;
}

.tool-bar .button:disabled {
    -fx-opacity: 0.5;
}

.table-view {
    -fx-background-color: white;
}

.table-view .column-header {
    -fx-background-color: #f5f5f5;
    -fx-font-weight: bold;
}

.table-row-cell:selected {
    -fx-background-color: #E3F2FD;
}
```

## 18.5 小结

本章实现了一个功能完整的TodoList应用：

1. ✅ **数据模型**：使用Property实现数据绑定
2. ✅ **表格视图**：TableView展示待办事项
3. ✅ **CRUD操作**：增删改查功能完整
4. ✅ **过滤功能**：使用FilteredList
5. ✅ **统计功能**：实时更新统计信息
6. ✅ **美观界面**：CSS样式优化

---

**扩展练习**

1. 实现JSON数据持久化
2. 添加到期日期和提醒功能
3. 支持拖拽排序
4. 添加分类/标签功能

