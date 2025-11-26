# 02 - 第一个 FlaUI 程序

本章将带你编写第一个完整的 FlaUI 自动化程序，逐步了解 FlaUI 的基本使用方式。

## 目标

我们将创建一个简单的程序，用于：
1. 启动 Windows 计算器应用
2. 自动点击按钮执行计算
3. 验证计算结果
4. 关闭应用

---

## 完整代码示例

### 示例 1：启动并操作计算器

创建一个新的控制台项目，编写以下代码：

```csharp
using System;
using System.Threading;
using FlaUI.Core;
using FlaUI.Core.AutomationElements;
using FlaUI.UIA3;

namespace FlaUIFirstDemo
{
    class Program
    {
        static void Main(string[] args)
        {
            // 1. 创建自动化引擎
            using (var automation = new UIA3Automation())
            {
                // 2. 启动计算器应用
                var app = FlaUI.Core.Application.Launch("calc.exe");
                
                // 等待应用启动
                Thread.Sleep(1000);
                
                // 3. 获取主窗口
                var mainWindow = app.GetMainWindow(automation);
                Console.WriteLine($"窗口标题: {mainWindow.Title}");
                
                // 4. 查找并点击按钮
                // 点击数字 "2"
                var button2 = mainWindow.FindFirstDescendant(cf => cf.ByName("二").And(cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button)))?.AsButton();
                button2?.Invoke();
                Thread.Sleep(300);
                
                // 点击 "+"
                var buttonPlus = mainWindow.FindFirstDescendant(cf => cf.ByName("加").And(cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button)))?.AsButton();
                buttonPlus?.Invoke();
                Thread.Sleep(300);
                
                // 点击数字 "3"
                var button3 = mainWindow.FindFirstDescendant(cf => cf.ByName("三").And(cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button)))?.AsButton();
                button3?.Invoke();
                Thread.Sleep(300);
                
                // 点击 "="
                var buttonEquals = mainWindow.FindFirstDescendant(cf => cf.ByName("等于").And(cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button)))?.AsButton();
                buttonEquals?.Invoke();
                Thread.Sleep(500);
                
                // 5. 获取计算结果
                var resultElement = mainWindow.FindFirstDescendant(cf => cf.ByAutomationId("CalculatorResults"));
                if (resultElement != null)
                {
                    Console.WriteLine($"计算结果: {resultElement.Name}");
                }
                
                // 6. 等待用户查看结果
                Console.WriteLine("按任意键关闭计算器...");
                Console.ReadKey();
                
                // 7. 关闭应用
                app.Close();
            }
            
            Console.WriteLine("程序执行完成！");
        }
    }
}
```

---

## 代码详解

### 1. 创建自动化引擎

```csharp
using (var automation = new UIA3Automation())
{
    // 所有自动化操作都在这里进行
}
```

**说明**：
- `UIA3Automation` 是 FlaUI 提供的自动化引擎
- 使用 `using` 语句确保资源正确释放
- 如果 UIA3 不可用，可以尝试 `UIA2Automation`

### 2. 启动应用程序

```csharp
var app = FlaUI.Core.Application.Launch("calc.exe");
```

**说明**：
- `Application.Launch()` 用于启动应用程序
- 参数可以是：
  - 可执行文件名（如 "calc.exe"）
  - 完整路径（如 "C:\\Program Files\\MyApp\\app.exe"）
  - 带参数的启动命令

**其他启动方式**：

```csharp
// 方式 1：附加到已运行的应用
var app = Application.Attach("notepad.exe");

// 方式 2：通过进程 ID 附加
var app = Application.Attach(12345);

// 方式 3：启动并传递参数
var app = Application.Launch("notepad.exe", "C:\\test.txt");
```

### 3. 获取主窗口

```csharp
var mainWindow = app.GetMainWindow(automation);
Console.WriteLine($"窗口标题: {mainWindow.Title}");
```

**说明**：
- `GetMainWindow()` 获取应用程序的主窗口
- `mainWindow.Title` 获取窗口标题
- 其他常用属性：
  - `mainWindow.Name` - 窗口名称
  - `mainWindow.ClassName` - 窗口类名
  - `mainWindow.BoundingRectangle` - 窗口位置和大小

### 4. 查找元素

```csharp
var button2 = mainWindow.FindFirstDescendant(cf => 
    cf.ByName("二").And(cf.ByControlType(FlaUI.Core.Definitions.ControlType.Button)))?.AsButton();
```

**说明**：
- `FindFirstDescendant()` 查找第一个匹配的子元素
- `cf.ByName()` 按名称查找
- `cf.ByControlType()` 按控件类型查找
- `.And()` 组合多个查找条件
- `.AsButton()` 将元素转换为按钮类型

**其他查找方式**：

```csharp
// 按 AutomationId 查找
var element = mainWindow.FindFirstDescendant(cf => cf.ByAutomationId("myButton"));

// 按类名查找
var element = mainWindow.FindFirstDescendant(cf => cf.ByClassName("Button"));

// 查找所有匹配元素
var allButtons = mainWindow.FindAllDescendants(cf => cf.ByControlType(ControlType.Button));
```

### 5. 操作元素

```csharp
button2?.Invoke();  // 点击按钮
```

**说明**：
- `Invoke()` 触发按钮的点击事件
- `?.` 是空值检查，避免元素未找到时抛出异常

**其他常用操作**：

```csharp
// 输入文本
textBox.Enter("Hello World");

// 选择下拉框
comboBox.Select("选项1");

// 勾选复选框
checkBox.IsChecked = true;

// 移动滑块
slider.Value = 50;
```

### 6. 读取元素信息

```csharp
var resultElement = mainWindow.FindFirstDescendant(cf => cf.ByAutomationId("CalculatorResults"));
Console.WriteLine($"计算结果: {resultElement.Name}");
```

**说明**：
- 通过元素的 `Name` 属性获取文本内容
- 不同类型的元素可能需要不同的属性来获取值

### 7. 关闭应用

```csharp
app.Close();
```

**其他关闭方式**：

```csharp
// 强制终止
app.Kill();

// 关闭主窗口
mainWindow.Close();
```

---

## 示例 2：更简洁的写法

使用辅助方法简化代码：

```csharp
using System;
using System.Threading;
using FlaUI.Core;
using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.UIA3;

namespace FlaUISimpleDemo
{
    class Program
    {
        static void Main(string[] args)
        {
            using var automation = new UIA3Automation();
            var app = Application.Launch("calc.exe");
            Thread.Sleep(1000);
            
            var window = app.GetMainWindow(automation);
            
            // 使用辅助方法
            ClickButton(window, "二");
            ClickButton(window, "加");
            ClickButton(window, "三");
            ClickButton(window, "等于");
            
            Thread.Sleep(500);
            
            var result = GetCalculatorResult(window);
            Console.WriteLine($"计算结果: {result}");
            
            Console.ReadKey();
            app.Close();
        }
        
        // 辅助方法：点击按钮
        static void ClickButton(Window window, string buttonName)
        {
            var button = window.FindFirstDescendant(cf => 
                cf.ByName(buttonName).And(cf.ByControlType(ControlType.Button)))?.AsButton();
            
            if (button != null)
            {
                button.Invoke();
                Thread.Sleep(300);
                Console.WriteLine($"已点击: {buttonName}");
            }
            else
            {
                Console.WriteLine($"未找到按钮: {buttonName}");
            }
        }
        
        // 辅助方法：获取计算结果
        static string GetCalculatorResult(Window window)
        {
            var resultElement = window.FindFirstDescendant(cf => cf.ByAutomationId("CalculatorResults"));
            return resultElement?.Name ?? "无法获取结果";
        }
    }
}
```

---

## 示例 3：使用英文系统的计算器

如果你的 Windows 是英文系统，按钮名称需要修改：

```csharp
using System;
using System.Threading;
using FlaUI.Core;
using FlaUI.Core.Application;
using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.UIA3;

namespace FlaUIEnglishCalc
{
    class Program
    {
        static void Main(string[] args)
        {
            using var automation = new UIA3Automation();
            var app = Application.Launch("calc.exe");
            Thread.Sleep(1000);
            
            var window = app.GetMainWindow(automation);
            Console.WriteLine($"Window Title: {window.Title}");
            
            // 英文系统的按钮名称
            ClickButton(window, "Two");
            ClickButton(window, "Plus");
            ClickButton(window, "Three");
            ClickButton(window, "Equals");
            
            Thread.Sleep(500);
            
            // 获取结果
            var result = window.FindFirstDescendant(cf => cf.ByAutomationId("CalculatorResults"));
            Console.WriteLine($"Result: {result?.Name}");
            
            Console.ReadKey();
            app.Close();
        }
        
        static void ClickButton(Window window, string buttonName)
        {
            var button = window.FindFirstDescendant(cf => 
                cf.ByName(buttonName).And(cf.ByControlType(ControlType.Button)))?.AsButton();
            button?.Invoke();
            Thread.Sleep(300);
        }
    }
}
```

---

## 使用 Inspect 工具查找元素

在编写自动化脚本前，强烈建议使用 Inspect 工具查看元素属性：

### 步骤：

1. **打开 Inspect.exe**
   - 位置：`C:\Program Files (x86)\Windows Kits\10\bin\<version>\x64\inspect.exe`
   
2. **启动目标应用**（如计算器）

3. **使用 Inspect 查看元素**
   - 将鼠标悬停在计算器的按钮上
   - 查看 Inspect 窗口中显示的属性：
     - **Name**：元素的名称（如 "二"、"加"）
     - **AutomationId**：唯一标识符（推荐使用）
     - **ClassName**：类名
     - **ControlType**：控件类型

4. **在代码中使用这些属性**

```csharp
// 使用 Name
var element = window.FindFirstDescendant(cf => cf.ByName("二"));

// 使用 AutomationId（更稳定）
var element = window.FindFirstDescendant(cf => cf.ByAutomationId("num2Button"));

// 使用 ClassName
var element = window.FindFirstDescendant(cf => cf.ByClassName("Button"));
```

---

## 常见问题

### 问题 1：找不到元素

**原因**：
- 元素名称错误（中英文系统不同）
- 应用尚未完全加载
- 使用了错误的查找条件

**解决方案**：
```csharp
// 1. 增加等待时间
Thread.Sleep(2000);

// 2. 使用更宽松的查找条件
var element = window.FindFirstDescendant(cf => cf.ByName("二"));

// 3. 检查是否找到元素
if (element == null)
{
    Console.WriteLine("元素未找到！");
}
```

### 问题 2：操作失败

**原因**：
- 元素不可见或被禁用
- 操作过快，应用未响应

**解决方案**：
```csharp
// 检查元素状态
if (button.IsEnabled && button.IsOffscreen == false)
{
    button.Invoke();
}

// 增加操作间隔
Thread.Sleep(500);
```

### 问题 3：应用未启动

**原因**：
- 应用路径错误
- 需要管理员权限

**解决方案**：
```csharp
// 使用完整路径
var app = Application.Launch(@"C:\Windows\System32\calc.exe");

// 捕获异常
try
{
    var app = Application.Launch("calc.exe");
}
catch (Exception ex)
{
    Console.WriteLine($"启动失败: {ex.Message}");
}
```

---

## 最佳实践

1. **总是使用 `using` 语句**：确保资源正确释放
2. **添加适当的等待时间**：避免因应用加载速度而失败
3. **使用 AutomationId 优先**：比 Name 更稳定，不受语言影响
4. **检查元素是否存在**：使用 `?.` 或 `if (element != null)` 进行空值检查
5. **添加日志输出**：便于调试和问题追踪

---

## 小结

本章学习了：

- ✅ 如何创建自动化引擎
- ✅ 如何启动和附加应用程序
- ✅ 如何获取窗口和查找元素
- ✅ 如何操作 UI 元素
- ✅ 如何读取元素信息
- ✅ 如何关闭应用

## 下一步

- 学习 [UI 自动化基础概念](./03-UI自动化基础概念.md)
- 深入了解 [元素定位详解](./04-元素定位详解.md)
- 查看更多 [常用控件操作](./05-常用控件操作.md)
