# MainView 重构指南

## 概述

原 `MainView.java` 有 5009 行代码，已重构为：
- **MainViewSimplified.java**: 650 行（主协调器）
- **TaskExecutionManager.java**: 400 行（任务执行管理）
- **NodeOperationManager.java**: 350 行（节点操作管理）
- **DialogManager.java**: 150 行（对话框管理）
- **DataManager.java**: 250 行（数据加载/保存）

## 架构设计

```
MainViewSimplified (主协调器，650行)
    ├── TaskExecutionManager (任务执行管理器)
    │   ├── triggerJobExecution() - 触发任务执行
    │   ├── stopJobExecution() - 停止任务
    │   ├── handleSSEMessage() - 处理SSE消息
    │   └── 日志轮询、状态更新等
    │
    ├── NodeOperationManager (节点操作管理器)
    │   ├── copyNodeToClipboard() - 复制节点
    │   ├── pasteNodes() - 粘贴节点
    │   ├── editNode() - 编辑节点
    │   └── 节点位置计算等
    │
    ├── DialogManager (对话框管理器)
    │   ├── showNewPartitionDialog() - 新建分区
    │   ├── showJobGroupDialog() - 任务组对话框
    │   ├── showJobNodeDialog() - 任务节点对话框
    │   └── 各种列表对话框
    │
    └── DataManager (数据管理器)
        ├── loadTaskGroupData() - 加载任务组数据
        ├── saveOrUpdateJob() - 保存任务组
        └── refreshTreeView() - 刷新树形视图
```

## 如何迁移

### 步骤 1: 创建管理器文件夹

```bash
mkdir -p cc-job/cc-job-gui/src/main/java/com/cc/job/gui/manager
```

### 步骤 2: 复制新文件

将以下文件复制到项目中：
- `TaskExecutionManager.java` → `manager/TaskExecutionManager.java`
- `NodeOperationManager.java` → `manager/NodeOperationManager.java`
- `DialogManager.java` → `manager/DialogManager.java`
- `DataManager.java` → `manager/DataManager.java`

### 步骤 3: 替换 MainView

有两种方式：

**方式1: 完全替换（推荐）**
```bash
# 备份原文件
mv MainView.java MainView.java.backup

# 使用新文件
mv MainViewSimplified.java MainView.java
```

**方式2: 保留两个版本（用于测试）**
```java
// 在你的应用入口（如 Main.java）中选择使用哪个版本
// MainView mainView = new MainView();  // 旧版本
MainView mainView = new MainViewSimplified();  // 新版本
```

### 步骤 4: 验证功能

启动应用，测试以下功能：
- ✅ 任务执行和停止
- ✅ 节点复制粘贴（Ctrl+C / Ctrl+V）
- ✅ 节点编辑
- ✅ 任务组加载和保存
- ✅ 对话框显示
- ✅ 树形视图操作

## 优势

### 1. **职责单一**
每个管理器只负责一块功能，易于理解和维护

### 2. **代码复用**
管理器可以在其他地方复用，例如：
```java
// 在其他视图中复用任务执行管理器
TaskExecutionManager executor = new TaskExecutionManager(canvas, logPanel, navBar, toolBar);
executor.triggerJobExecution(jobId, jobName);
```

### 3. **易于测试**
可以独立测试每个管理器：
```java
@Test
public void testTaskExecution() {
    TaskExecutionManager manager = new TaskExecutionManager(mockCanvas, mockLogPanel, mockNavBar, mockToolBar);
    manager.triggerJobExecution(123L, "测试任务");
    // 验证行为
}
```

### 4. **易于扩展**
添加新功能只需修改对应的管理器，不影响其他模块

## 注意事项

### 1. 初始化顺序
确保在调用管理器方法前，所有UI组件已初始化：
```java
initializeUI();          // 先初始化UI
initializeManagers();    // 再初始化管理器
setupCallbacks();        // 最后设置回调
```

### 2. 线程安全
所有UI更新必须在JavaFX主线程：
```java
Platform.runLater(() -> {
    logPanel.success("操作成功");
});
```

### 3. 资源清理
应用关闭时调用 cleanup()：
```java
@Override
public void stop() {
    mainView.cleanup();  // 清理任务执行管理器中的资源
}
```

## 扩展示例

### 添加新功能：批量删除节点

1. 在 `NodeOperationManager` 中添加方法：
```java
public void deleteNodes(Set<ProcessNode> nodes) {
    logPanel.info("正在删除 " + nodes.size() + " 个节点...");
    
    new Thread(() -> {
        for (ProcessNode node : nodes) {
            try {
                jobInfoService.deleteJobNode(node.getJobId());
            } catch (Exception e) {
                logger.error("删除节点失败", e);
            }
        }
        
        Platform.runLater(() -> {
            canvas.removeNodes(nodes);
            logPanel.success("✓ 删除完成");
        });
    }).start();
}
```

2. 在 `MainViewSimplified` 中调用：
```java
// 在工具栏回调中添加
@Override
public void onDeleteSelected() {
    Set<ProcessNode> selected = canvas.getSelectedNodes();
    if (!selected.isEmpty()) {
        nodeOperationManager.deleteNodes(selected);
    }
}
```

## 性能对比

| 指标 | 原版本 | 重构版本 |
|------|--------|----------|
| 主类代码行数 | 5009 行 | 650 行 |
| 单文件最大行数 | 5009 行 | 650 行 |
| 类的职责数量 | 10+ | 1（协调） |
| 可测试性 | 低 | 高 |
| 可维护性 | 低 | 高 |

## 常见问题

### Q: 为什么不使用 Spring 等依赖注入框架？
A: 为保持轻量级和简单性，使用构造函数注入。如需使用Spring，可轻松改造。

### Q: 管理器之间如何通信？
A: 通过回调机制和共享的UI组件（如 LogPanel）进行通信。

### Q: 是否需要修改其他文件？
A: 不需要。新架构完全兼容现有的Service层和Model层。

### Q: 性能是否受影响？
A: 不会。重构只是代码组织方式的改变，运行时行为完全相同。

## 总结

通过将 5009 行的巨型类拆分为 5 个职责单一的类，代码的可读性、可维护性和可测试性都得到了显著提升，同时主类代码量降至 650 行，远低于 800 行的目标。

