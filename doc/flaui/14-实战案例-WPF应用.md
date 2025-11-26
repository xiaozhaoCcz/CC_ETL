# 14 - 实战案例：测试 WPF 应用

本章演示如何使用 FlaUI 测试 WPF（Windows Presentation Foundation）应用程序。

---

## WPF 应用的特点

### 与传统 Win32 应用的区别

1. **控件结构**：WPF 使用更复杂的可视树
2. **AutomationId**：WPF 开发者通常会设置明确的 AutomationId
3. **自定义控件**：可能包含自定义控件类型
4. **XAML 属性**：可以访问 XAML 定义的属性

### 测试 WPF 应用的优势

- 更好的 UI Automation 支持
- 更清晰的元素层次结构
- 更容易定位元素（如果设置了 AutomationId）

---

## 示例 WPF 应用

### 简单的 WPF 应用（用于测试）

```xml
<!-- MainWindow.xaml -->
<Window x:Class="WpfApp.MainWindow"
        xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="WPF 测试应用" Height="400" Width="600"
        AutomationProperties.AutomationId="MainWindow">
    
    <Grid>
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto"/>
            <RowDefinition Height="*"/>
            <RowDefinition Height="Auto"/>
        </Grid.RowDefinitions>
        
        <!-- 顶部输入区 -->
        <StackPanel Grid.Row="0" Margin="10">
            <Label Content="用户名:"/>
            <TextBox x:Name="txtUsername" 
                     AutomationProperties.AutomationId="UsernameTextBox"
                     Height="25" Margin="0,5,0,0"/>
            
            <Label Content="密码:" Margin="0,10,0,0"/>
            <PasswordBox x:Name="txtPassword"
                        AutomationProperties.AutomationId="PasswordBox"
                        Height="25" Margin="0,5,0,0"/>
            
            <CheckBox x:Name="chkRemember"
                     Content="记住我"
                     AutomationProperties.AutomationId="RememberCheckBox"
                     Margin="0,10,0,0"/>
            
            <Button x:Name="btnLogin"
                   Content="登录"
                   AutomationProperties.AutomationId="LoginButton"
                   Width="100" Height="30"
                   Margin="0,10,0,0"
                   Click="BtnLogin_Click"/>
        </StackPanel>
        
        <!-- 中间内容区 -->
        <ListBox x:Name="lstItems" Grid.Row="1"
                AutomationProperties.AutomationId="ItemsListBox"
                Margin="10"/>
        
        <!-- 底部状态栏 -->
        <StatusBar Grid.Row="2">
            <StatusBarItem>
                <TextBlock x:Name="txtStatus"
                          AutomationProperties.AutomationId="StatusText"
                          Text="就绪"/>
            </StatusBarItem>
        </StatusBar>
    </Grid>
</Window>
```

---

## WPF 页面对象

### WpfAppPage.cs

```csharp
using System;
using FlaUI.Core;
using FlaUI.Core.AutomationElements;
using FlaUI.Core.Definitions;
using FlaUI.UIA3;

namespace WpfTests.PageObjects
{
    public class WpfAppPage : IDisposable
    {
        private readonly UIA3Automation _automation;
        private readonly Application _app;
        private readonly Window _window;
        
        public WpfAppPage(string appPath)
        {
            _automation = new UIA3Automation();
            _app = Application.Launch(appPath);
            System.Threading.Thread.Sleep(1000);
            _window = _app.GetMainWindow(_automation);
        }
        
        /// <summary>
        /// 通过 AutomationId 查找元素
        /// </summary>
        private AutomationElement FindByAutomationId(string automationId)
        {
            var element = _window.FindFirstDescendant(cf => 
                cf.ByAutomationId(automationId));
            
            if (element == null)
            {
                throw new Exception($"未找到元素: {automationId}");
            }
            
            return element;
        }
        
        /// <summary>
        /// 输入用户名
        /// </summary>
        public void SetUsername(string username)
        {
            var textBox = FindByAutomationId("UsernameTextBox").AsTextBox();
            textBox.Text = username;
        }
        
        /// <summary>
        /// 获取用户名
        /// </summary>
        public string GetUsername()
        {
            var textBox = FindByAutomationId("UsernameTextBox").AsTextBox();
            return textBox.Text;
        }
        
        /// <summary>
        /// 输入密码（注意：PasswordBox 的文本无法读取）
        /// </summary>
        public void SetPassword(string password)
        {
            var passwordBox = FindByAutomationId("PasswordBox");
            
            // PasswordBox 不支持 Value 模式，需要使用键盘输入
            passwordBox.Click();
            System.Threading.Thread.Sleep(300);
            
            FlaUI.Core.Input.Keyboard.Type(password);
        }
        
        /// <summary>
        /// 勾选"记住我"
        /// </summary>
        public void SetRememberMe(bool remember)
        {
            var checkBox = FindByAutomationId("RememberCheckBox").AsCheckBox();
            checkBox.IsChecked = remember;
        }
        
        /// <summary>
        /// 获取"记住我"状态
        /// </summary>
        public bool GetRememberMe()
        {
            var checkBox = FindByAutomationId("RememberCheckBox").AsCheckBox();
            return checkBox.IsChecked ?? false;
        }
        
        /// <summary>
        /// 点击登录按钮
        /// </summary>
        public void ClickLogin()
        {
            var button = FindByAutomationId("LoginButton").AsButton();
            button.Invoke();
            System.Threading.Thread.Sleep(500);
        }
        
        /// <summary>
        /// 获取列表框中的项
        /// </summary>
        public string[] GetListItems()
        {
            var listBox = FindByAutomationId("ItemsListBox").AsListBox();
            var items = listBox.Items;
            
            var result = new string[items.Length];
            for (int i = 0; i < items.Length; i++)
            {
                result[i] = items[i].Name;
            }
            
            return result;
        }
        
        /// <summary>
        /// 选择列表框中的项
        /// </summary>
        public void SelectListItem(int index)
        {
            var listBox = FindByAutomationId("ItemsListBox").AsListBox();
            var items = listBox.Items;
            
            if (index >= 0 && index < items.Length)
            {
                items[index].Select();
            }
        }
        
        /// <summary>
        /// 获取状态栏文本
        /// </summary>
        public string GetStatusText()
        {
            var statusText = FindByAutomationId("StatusText");
            return statusText.Name;
        }
        
        /// <summary>
        /// 等待状态文本包含特定内容
        /// </summary>
        public bool WaitForStatus(string expectedText, int timeoutMs = 5000)
        {
            var startTime = DateTime.Now;
            
            while ((DateTime.Now - startTime).TotalMilliseconds < timeoutMs)
            {
                string statusText = GetStatusText();
                if (statusText.Contains(expectedText))
                {
                    return true;
                }
                System.Threading.Thread.Sleep(500);
            }
            
            return false;
        }
        
        /// <summary>
        /// 获取窗口标题
        /// </summary>
        public string GetWindowTitle()
        {
            return _window.Title;
        }
        
        /// <summary>
        /// 检查登录按钮是否可用
        /// </summary>
        public bool IsLoginButtonEnabled()
        {
            var button = FindByAutomationId("LoginButton").AsButton();
            return button.IsEnabled;
        }
        
        public void Dispose()
        {
            _app?.Close();
            _automation?.Dispose();
        }
    }
}
```

---

## 测试用例

### WpfLoginTests.cs

```csharp
using Xunit;
using WpfTests.PageObjects;

namespace WpfTests.Tests
{
    public class WpfLoginTests
    {
        private readonly string _appPath = @"C:\Path\To\WpfApp.exe";
        
        [Fact]
        public void TestSetUsername_InputText_TextAppears()
        {
            using var app = new WpfAppPage(_appPath);
            
            // Act
            app.SetUsername("testuser");
            
            // Assert
            Assert.Equal("testuser", app.GetUsername());
        }
        
        [Fact]
        public void TestSetPassword_InputPassword_NoError()
        {
            using var app = new WpfAppPage(_appPath);
            
            // Act & Assert (不会抛出异常)
            app.SetPassword("password123");
        }
        
        [Fact]
        public void TestRememberMeCheckbox_ToggleState_StateChanges()
        {
            using var app = new WpfAppPage(_appPath);
            
            // 初始状态应该是未勾选
            Assert.False(app.GetRememberMe());
            
            // Act
            app.SetRememberMe(true);
            
            // Assert
            Assert.True(app.GetRememberMe());
            
            // Act
            app.SetRememberMe(false);
            
            // Assert
            Assert.False(app.GetRememberMe());
        }
        
        [Fact]
        public void TestLoginButton_ClickWithCredentials_LoginProcessed()
        {
            using var app = new WpfAppPage(_appPath);
            
            // Arrange
            app.SetUsername("admin");
            app.SetPassword("admin123");
            app.SetRememberMe(true);
            
            // Act
            app.ClickLogin();
            
            // Assert
            bool statusUpdated = app.WaitForStatus("登录成功", 5000);
            Assert.True(statusUpdated, "状态栏应显示登录成功");
        }
        
        [Fact]
        public void TestWindowTitle_OnStartup_ShowsCorrectTitle()
        {
            using var app = new WpfAppPage(_appPath);
            
            // Assert
            string title = app.GetWindowTitle();
            Assert.Contains("WPF 测试应用", title);
        }
    }
}
```

### WpfListBoxTests.cs

```csharp
using Xunit;
using WpfTests.PageObjects;

namespace WpfTests.Tests
{
    public class WpfListBoxTests
    {
        private readonly string _appPath = @"C:\Path\To\WpfApp.exe";
        
        [Fact]
        public void TestListBox_GetItems_ReturnsAllItems()
        {
            using var app = new WpfAppPage(_appPath);
            
            // Act
            string[] items = app.GetListItems();
            
            // Assert
            Assert.NotNull(items);
            Assert.NotEmpty(items);
        }
        
        [Fact]
        public void TestListBox_SelectItem_ItemSelected()
        {
            using var app = new WpfAppPage(_appPath);
            
            // Act
            app.SelectListItem(0);
            
            // Assert
            string statusText = app.GetStatusText();
            Assert.Contains("已选择", statusText);
        }
        
        [Theory]
        [InlineData(0)]
        [InlineData(1)]
        [InlineData(2)]
        public void TestListBox_SelectDifferentItems_StatusUpdates(int index)
        {
            using var app = new WpfAppPage(_appPath);
            
            // Act
            app.SelectListItem(index);
            
            // Assert
            string statusText = app.GetStatusText();
            Assert.NotEmpty(statusText);
        }
    }
}
```

---

## 处理自定义控件

### 查找自定义控件

```csharp
// 如果 WPF 应用有自定义控件
public class CustomControlHelper
{
    private readonly Window _window;
    
    public CustomControlHelper(Window window)
    {
        _window = window;
    }
    
    /// <summary>
    /// 通过 ClassName 查找自定义控件
    /// </summary>
    public AutomationElement FindCustomControl(string className, string automationId = null)
    {
        if (!string.IsNullOrEmpty(automationId))
        {
            return _window.FindFirstDescendant(cf => 
                cf.ByClassName(className).And(cf.ByAutomationId(automationId)));
        }
        
        return _window.FindFirstDescendant(cf => cf.ByClassName(className));
    }
    
    /// <summary>
    /// 获取自定义控件的属性
    /// </summary>
    public string GetCustomProperty(AutomationElement element, string propertyName)
    {
        // 根据属性名获取不同的属性
        return propertyName switch
        {
            "Name" => element.Name,
            "ClassName" => element.ClassName,
            "AutomationId" => element.AutomationId,
            _ => element.HelpText
        };
    }
}
```

---

## WPF 数据绑定测试

### 测试数据绑定

```csharp
using Xunit;
using System.Linq;

public class WpfDataBindingTests
{
    [Fact]
    public void TestDataGrid_LoadData_RowsDisplayed()
    {
        using var app = new WpfAppPage(_appPath);
        
        // 假设有一个 DataGrid
        var dataGrid = app.FindByAutomationId("DataGrid").AsDataGridView();
        
        // Act
        var rows = dataGrid.Rows;
        
        // Assert
        Assert.NotEmpty(rows);
        Assert.True(rows.Length > 0, "DataGrid 应该包含数据行");
    }
    
    [Fact]
    public void TestComboBox_SelectItem_BoundPropertyUpdates()
    {
        using var app = new WpfAppPage(_appPath);
        
        // Arrange
        var comboBox = app.FindByAutomationId("CategoryComboBox").AsComboBox();
        
        // Act
        comboBox.Select(1);  // 选择第二项
        
        // Assert
        var selectedItem = comboBox.SelectedItem;
        Assert.NotNull(selectedItem);
        
        // 验证其他绑定的控件是否更新
        string statusText = app.GetStatusText();
        Assert.Contains("类别已更改", statusText);
    }
}
```

---

## WPF 样式和模板测试

### 测试可视树

```csharp
public class WpfVisualTreeHelper
{
    /// <summary>
    /// 打印可视树结构
    /// </summary>
    public static void PrintVisualTree(AutomationElement element, int depth = 0)
    {
        string indent = new string(' ', depth * 2);
        
        Console.WriteLine($"{indent}[{element.ControlType}] {element.Name} " +
                         $"(Class: {element.ClassName}, ID: {element.AutomationId})");
        
        if (depth < 5)  // 限制深度
        {
            var children = element.FindAllChildren();
            foreach (var child in children)
            {
                PrintVisualTree(child, depth + 1);
            }
        }
    }
    
    /// <summary>
    /// 查找特定深度的元素
    /// </summary>
    public static AutomationElement[] FindAtDepth(AutomationElement root, int targetDepth)
    {
        var results = new System.Collections.Generic.List<AutomationElement>();
        FindAtDepthRecursive(root, targetDepth, 0, results);
        return results.ToArray();
    }
    
    private static void FindAtDepthRecursive(
        AutomationElement element, 
        int targetDepth, 
        int currentDepth, 
        System.Collections.Generic.List<AutomationElement> results)
    {
        if (currentDepth == targetDepth)
        {
            results.Add(element);
            return;
        }
        
        var children = element.FindAllChildren();
        foreach (var child in children)
        {
            FindAtDepthRecursive(child, targetDepth, currentDepth + 1, results);
        }
    }
}

// 使用示例
[Fact]
public void TestVisualTree_PrintStructure()
{
    using var app = new WpfAppPage(_appPath);
    
    var window = app.GetWindow();
    WpfVisualTreeHelper.PrintVisualTree(window);
}
```

---

## 异步操作测试

### 等待异步操作完成

```csharp
public class WpfAsyncTests
{
    [Fact]
    public void TestAsyncOperation_LoadData_DataLoaded()
    {
        using var app = new WpfAppPage(_appPath);
        
        // Act: 触发异步加载
        app.ClickButton("LoadDataButton");
        
        // Assert: 等待加载完成
        bool loaded = app.WaitForStatus("数据加载完成", 10000);
        Assert.True(loaded, "数据应该在 10 秒内加载完成");
        
        // 验证数据
        var items = app.GetListItems();
        Assert.NotEmpty(items);
    }
    
    [Fact]
    public void TestProgressBar_DuringOperation_ShowsProgress()
    {
        using var app = new WpfAppPage(_appPath);
        
        // Arrange
        var progressBar = app.FindByAutomationId("ProgressBar");
        
        // Act
        app.ClickButton("StartOperationButton");
        System.Threading.Thread.Sleep(1000);
        
        // Assert
        if (progressBar.Patterns.RangeValue.IsSupported)
        {
            var value = progressBar.Patterns.RangeValue.Pattern.Value.Value;
            Assert.True(value > 0, "进度条应该显示进度");
        }
        
        // 等待操作完成
        app.WaitForStatus("操作完成", 30000);
    }
}
```

---

## 小结

本章学习了：

- ✅ WPF 应用的特点和优势
- ✅ WPF 页面对象的实现
- ✅ 测试 WPF 控件（TextBox、CheckBox、ListBox 等）
- ✅ 处理密码框（PasswordBox）
- ✅ 处理自定义控件
- ✅ 测试数据绑定
- ✅ 可视树操作
- ✅ 异步操作测试

## 下一步

- 学习 [编写可维护的测试代码](./15-编写可维护的测试代码.md)
- 了解 [性能优化技巧](./16-性能优化技巧.md)
- 查看 [常见问题与解决方案](./17-常见问题与解决方案.md)
