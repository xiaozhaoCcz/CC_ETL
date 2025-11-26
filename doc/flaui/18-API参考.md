# 18 - API 参考

本章提供 FlaUI 常用 API 的快速参考。

---

## 核心类

### AutomationBase

```csharp
using FlaUI.UIA3;

// 创建自动化引擎
var automation = new UIA3Automation();

// 获取桌面
var desktop = automation.GetDesktop();

// 释放资源
automation.Dispose();
```

### Application

```csharp
// 启动应用
var app = Application.Launch("notepad.exe");
var app = Application.Launch(@"C:\Path\To\App.exe");
var app = Application.Launch("notepad.exe", "file.txt");

// 附加到运行中的应用
var app = Application.Attach("notepad.exe");
var app = Application.Attach(processId);

// 获取主窗口
var window = app.GetMainWindow(automation);
var window = app.GetMainWindow(automation, TimeSpan.FromSeconds(10));

// 获取所有窗口
var windows = app.GetAllTopLevelWindows(automation);

// 关闭应用
app.Close();
app.Kill();

// 检查状态
bool hasExited = app.HasExited;
int processId = app.ProcessId;
```

### Window

```csharp
// 窗口属性
string title = window.Title;
string className = window.ClassName;
string automationId = window.AutomationId;
bool isEnabled = window.IsEnabled;
bool isOffscreen = window.IsOffscreen;

// 窗口操作
window.SetForeground();
window.Close();
window.Move(100, 100);
window.SetWindowVisualState(WindowVisualState.Maximized);

// 窗口状态
var state = window.WindowVisualState;
bool isModal = window.IsModal;
bool isTopmost = window.IsTopmost;
```

---

## 元素查找

### FindFirst/FindAll

```csharp
// 查找第一个匹配的子元素
var child = element.FindFirstChild(cf => cf.ByName("按钮"));

// 查找第一个匹配的后代元素
var descendant = element.FindFirstDescendant(cf => cf.ByName("按钮"));

// 查找所有匹配的子元素
var children = element.FindAllChildren(cf => cf.ByControlType(ControlType.Button));

// 查找所有匹配的后代元素
var descendants = element.FindAllDescendants(cf => cf.ByControlType(ControlType.Button));
```

### 查找条件

```csharp
// 按名称
cf.ByName("按钮")

// 按 AutomationId
cf.ByAutomationId("btnSave")

// 按类名
cf.ByClassName("Button")

// 按控件类型
cf.ByControlType(ControlType.Button)

// 按帮助文本
cf.ByHelpText("点击保存")

// 组合条件
cf.ByName("保存").And(cf.ByControlType(ControlType.Button))
cf.ByName("确定").Or(cf.ByName("OK"))
```

---

## 控件类型

### Button（按钮）

```csharp
var button = element.AsButton();

// 点击
button.Invoke();
button.Click();

// 检查状态
bool isEnabled = button.IsEnabled;
```

### TextBox（文本框）

```csharp
var textBox = element.AsTextBox();

// 设置文本
textBox.Text = "Hello";

// 获取文本
string text = textBox.Text;

// 追加文本
textBox.Enter("World");

// 检查是否只读
bool isReadOnly = textBox.IsReadOnly;
```

### CheckBox（复选框）

```csharp
var checkBox = element.AsCheckBox();

// 设置状态
checkBox.IsChecked = true;
checkBox.IsChecked = false;

// 切换状态
checkBox.Toggle();

// 获取状态
bool? isChecked = checkBox.IsChecked;
```

### RadioButton（单选按钮）

```csharp
var radioButton = element.AsRadioButton();

// 选择
radioButton.IsChecked = true;
radioButton.Select();

// 获取状态
bool isSelected = radioButton.IsChecked;
```

### ComboBox（下拉框）

```csharp
var comboBox = element.AsComboBox();

// 展开/折叠
comboBox.Expand();
comboBox.Collapse();

// 选择项
comboBox.Select("选项1");
comboBox.Select(0);  // 按索引

// 获取选中项
var selectedItem = comboBox.SelectedItem;

// 获取所有选项
var items = comboBox.Items;
```

### ListBox（列表框）

```csharp
var listBox = element.AsListBox();

// 获取所有项
var items = listBox.Items;

// 选择项
listBox.Select("项目1");
listBox.Select(new[] { "项目1", "项目2" });  // 多选

// 获取选中项
var selectedItems = listBox.SelectedItems;
```

### Tree（树形控件）

```csharp
var tree = element.AsTree();

// 获取根节点
var items = tree.Items;

// 操作树节点
var treeItem = items[0].AsTreeItem();
treeItem.Expand();
treeItem.Collapse();
treeItem.Select();

// 获取子节点
var children = treeItem.Items;
```

### DataGrid（数据表格）

```csharp
var grid = element.AsDataGridView();

// 获取行和列
var rows = grid.Rows;
var columns = grid.Columns;

// 获取单元格
var cell = grid[0, 0];  // 第一行第一列
var cellValue = cell.Name;

// 选择行
rows[0].Select();
```

### Slider（滑块）

```csharp
var slider = element.AsSlider();

// 设置值
slider.Value = 50;

// 获取值
double value = slider.Value;

// 获取范围
double min = slider.Minimum;
double max = slider.Maximum;

// 增加/减少值
slider.LargeIncrement();
slider.SmallIncrement();
slider.LargeDecrement();
slider.SmallDecrement();
```

---

## 鼠标操作

```csharp
using FlaUI.Core.Input;
using FlaUI.Core.WindowsAPI;

// 移动鼠标
Mouse.MoveTo(500, 300);

// 点击
Mouse.Click();
Mouse.RightClick();
Mouse.DoubleClick();

// 按下和释放
Mouse.Down(MouseButton.Left);
Mouse.Up(MouseButton.Left);

// 拖拽
Mouse.Drag(TimeSpan.FromMilliseconds(500), 
    new Point(100, 100), 
    new Point(300, 300));
```

---

## 键盘操作

```csharp
using FlaUI.Core.Input;
using FlaUI.Core.WindowsAPI;

// 输入文本
Keyboard.Type("Hello World");

// 按键
Keyboard.Press(VirtualKeyShort.ENTER);
Keyboard.Press(VirtualKeyShort.TAB);
Keyboard.Press(VirtualKeyShort.ESCAPE);

// 组合键
Keyboard.TypeSimultaneously(VirtualKeyShort.CONTROL, VirtualKeyShort.KEY_C);
Keyboard.TypeSimultaneously(VirtualKeyShort.CONTROL, VirtualKeyShort.KEY_V);

// 按下和释放
Keyboard.Down(VirtualKeyShort.SHIFT);
Keyboard.Press(VirtualKeyShort.KEY_A);
Keyboard.Up(VirtualKeyShort.SHIFT);
```

---

## 模式 (Patterns)

### Invoke Pattern

```csharp
if (element.Patterns.Invoke.IsSupported)
{
    element.Patterns.Invoke.Pattern.Invoke();
}
```

### Value Pattern

```csharp
if (element.Patterns.Value.IsSupported)
{
    var pattern = element.Patterns.Value.Pattern;
    pattern.SetValue("新值");
    string value = pattern.Value.Value;
    bool isReadOnly = pattern.IsReadOnly.Value;
}
```

### Toggle Pattern

```csharp
if (element.Patterns.Toggle.IsSupported)
{
    var pattern = element.Patterns.Toggle.Pattern;
    pattern.Toggle();
    var state = pattern.ToggleState.Value;  // On, Off, Indeterminate
}
```

### Selection Pattern

```csharp
if (element.Patterns.Selection.IsSupported)
{
    var pattern = element.Patterns.Selection.Pattern;
    var selectedItems = pattern.Selection.Value;
    bool canSelectMultiple = pattern.CanSelectMultiple.Value;
}
```

### ExpandCollapse Pattern

```csharp
if (element.Patterns.ExpandCollapse.IsSupported)
{
    var pattern = element.Patterns.ExpandCollapse.Pattern;
    pattern.Expand();
    pattern.Collapse();
    var state = pattern.ExpandCollapseState.Value;
}
```

### RangeValue Pattern

```csharp
if (element.Patterns.RangeValue.IsSupported)
{
    var pattern = element.Patterns.RangeValue.Pattern;
    pattern.SetValue(50.0);
    double value = pattern.Value.Value;
    double min = pattern.Minimum.Value;
    double max = pattern.Maximum.Value;
}
```

---

## 截图

```csharp
using FlaUI.Core.Capturing;

// 截取整个屏幕
var capture = Capture.Screen();
capture.ToFile("screenshot.png");

// 截取元素
var capture = Capture.Element(element);
capture.ToFile("element.png");

// 截取区域
var rectangle = new System.Drawing.Rectangle(100, 100, 800, 600);
var capture = Capture.Rectangle(rectangle);
capture.ToFile("region.png");
```

---

## 等待

```csharp
// 等待元素出现
public static AutomationElement WaitForElement(
    AutomationElement parent,
    Func<ConditionFactory, ConditionBase> condition,
    int timeoutMs = 5000)
{
    var startTime = DateTime.Now;
    
    while ((DateTime.Now - startTime).TotalMilliseconds < timeoutMs)
    {
        var element = parent.FindFirstDescendant(condition);
        if (element != null)
        {
            return element;
        }
        System.Threading.Thread.Sleep(500);
    }
    
    throw new TimeoutException("元素未找到");
}

// 等待条件
public static bool WaitUntil(Func<bool> condition, int timeoutMs = 5000)
{
    var startTime = DateTime.Now;
    
    while ((DateTime.Now - startTime).TotalMilliseconds < timeoutMs)
    {
        if (condition())
        {
            return true;
        }
        System.Threading.Thread.Sleep(500);
    }
    
    return false;
}
```

---

## 常用枚举

### ControlType

```csharp
ControlType.Button
ControlType.Edit
ControlType.CheckBox
ControlType.RadioButton
ControlType.ComboBox
ControlType.List
ControlType.Menu
ControlType.MenuItem
ControlType.Window
ControlType.Pane
ControlType.Tree
ControlType.DataGrid
ControlType.Tab
ControlType.TabItem
```

### WindowVisualState

```csharp
WindowVisualState.Normal
WindowVisualState.Maximized
WindowVisualState.Minimized
```

### ToggleState

```csharp
ToggleState.Off
ToggleState.On
ToggleState.Indeterminate
```

### ExpandCollapseState

```csharp
ExpandCollapseState.Collapsed
ExpandCollapseState.Expanded
ExpandCollapseState.PartiallyExpanded
ExpandCollapseState.LeafNode
```

---

## 常用扩展方法

```csharp
// 转换为特定控件类型
element.AsButton()
element.AsTextBox()
element.AsCheckBox()
element.AsRadioButton()
element.AsComboBox()
element.AsListBox()
element.AsTree()
element.AsWindow()

// 查找方法
element.FindFirstChild(condition)
element.FindFirstDescendant(condition)
element.FindAllChildren(condition)
element.FindAllDescendants(condition)
element.FindFirstByXPath(xpath)
element.FindAllByXPath(xpath)
```

---

## 实用工具方法

### 打印元素信息

```csharp
public static void PrintElementInfo(AutomationElement element)
{
    Console.WriteLine($"Name: {element.Name}");
    Console.WriteLine($"AutomationId: {element.AutomationId}");
    Console.WriteLine($"ClassName: {element.ClassName}");
    Console.WriteLine($"ControlType: {element.ControlType}");
    Console.WriteLine($"IsEnabled: {element.IsEnabled}");
    Console.WriteLine($"IsOffscreen: {element.IsOffscreen}");
    Console.WriteLine($"BoundingRectangle: {element.BoundingRectangle}");
}
```

### 重试执行

```csharp
public static T Retry<T>(Func<T> action, int maxAttempts = 3, int delayMs = 1000)
{
    for (int i = 0; i < maxAttempts; i++)
    {
        try
        {
            return action();
        }
        catch (Exception ex)
        {
            if (i == maxAttempts - 1)
            {
                throw;
            }
            Thread.Sleep(delayMs);
        }
    }
    
    throw new Exception("不应该到达这里");
}
```

---

## 小结

本章提供了 FlaUI 的核心 API 快速参考，包括：

- ✅ 核心类（Application、Window、AutomationElement）
- ✅ 元素查找方法
- ✅ 各种控件类型的 API
- ✅ 鼠标和键盘操作
- ✅ 模式（Patterns）
- ✅ 截图功能
- ✅ 等待机制
- ✅ 常用枚举
- ✅ 实用工具方法

## 下一步

- 查看 [常用代码片段](./19-常用代码片段.md)
- 返回 [目录](./00-目录.md)
