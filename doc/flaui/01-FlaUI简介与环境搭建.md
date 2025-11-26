# 01 - FlaUI 简介与环境搭建

## 什么是 FlaUI？

FlaUI 是一个基于 .NET 的 UI 自动化库，用于自动化测试 Windows 桌面应用程序。它是对 Microsoft UI Automation 的封装，提供了更加简洁、易用的 API。

### 核心特性

- **跨技术支持**：支持 WinForms、WPF、Win32、Qt 等多种 UI 技术
- **双引擎支持**：同时支持 UIA2 和 UIA3 自动化引擎
- **现代化 API**：基于 .NET Standard 2.0，支持 .NET Framework 和 .NET Core/.NET 5+
- **活跃维护**：持续更新和维护，社区活跃
- **易于使用**：简洁的 API 设计，学习曲线平缓

### FlaUI vs 其他工具

| 特性 | FlaUI | Selenium | Appium | WinAppDriver |
|------|-------|----------|--------|--------------|
| Windows 桌面应用 | ✅ | ❌ | ✅ | ✅ |
| Web 应用 | ❌ | ✅ | ✅ | ❌ |
| .NET 原生支持 | ✅ | 部分 | ❌ | ✅ |
| 开源免费 | ✅ | ✅ | ✅ | ✅ |
| 学习成本 | 低 | 中 | 高 | 中 |

### 应用场景

1. **自动化测试**：为 Windows 桌面应用编写自动化测试
2. **回归测试**：持续集成中的自动化回归测试
3. **RPA**：机器人流程自动化
4. **桌面应用爬虫**：自动化操作桌面应用获取数据

---

## 环境准备

### 系统要求

- **操作系统**：Windows 7 及以上版本
- **开发环境**：
  - Visual Studio 2019/2022（推荐）
  - Visual Studio Code + .NET SDK
  - JetBrains Rider

- **.NET 版本**：
  - .NET Framework 4.6.1+ 或
  - .NET Core 3.1+ 或
  - .NET 5.0+

### 安装步骤

#### 方式一：使用 NuGet 包管理器

1. **创建新项目**

打开 Visual Studio，创建一个新的控制台应用或测试项目：

```
文件 -> 新建 -> 项目 -> 控制台应用 (.NET 6.0)
```

2. **安装 FlaUI NuGet 包**

在 NuGet 包管理器控制台中执行：

```powershell
Install-Package FlaUI.Core
Install-Package FlaUI.UIA3  # UIA3 自动化引擎
```

或者使用 .NET CLI：

```bash
dotnet add package FlaUI.Core
dotnet add package FlaUI.UIA3
```

#### 方式二：使用 Visual Studio 包管理器界面

1. 右键点击项目 -> 管理 NuGet 程序包
2. 搜索 "FlaUI"
3. 安装以下包：
   - `FlaUI.Core` - 核心库
   - `FlaUI.UIA3` - UIA3 自动化引擎（推荐）
   - `FlaUI.UIA2` - UIA2 自动化引擎（可选）

### 验证安装

创建一个简单的测试文件 `Program.cs`：

```csharp
using System;
using FlaUI.Core;
using FlaUI.UIA3;

namespace FlaUIDemo
{
    class Program
    {
        static void Main(string[] args)
        {
            // 创建 UIA3 自动化引擎
            using (var automation = new UIA3Automation())
            {
                Console.WriteLine($"FlaUI 自动化引擎已初始化: {automation.GetType().Name}");
                Console.WriteLine("FlaUI 安装成功！");
            }
        }
    }
}
```

运行程序，如果看到输出信息，说明安装成功。

---

## 推荐的包

根据不同的需求，可以安装以下包：

### 核心包

```bash
# 必需
dotnet add package FlaUI.Core

# 自动化引擎（选择一个或两个都安装）
dotnet add package FlaUI.UIA3  # 推荐，适用于 Windows 7+
dotnet add package FlaUI.UIA2  # 备选，兼容性更好
```

### 测试框架集成

```bash
# 如果使用 xUnit
dotnet add package xunit
dotnet add package xunit.runner.visualstudio

# 如果使用 NUnit
dotnet add package NUnit
dotnet add package NUnit3TestAdapter

# 如果使用 MSTest
dotnet add package MSTest.TestFramework
dotnet add package MSTest.TestAdapter
```

---

## 项目结构建议

创建一个完整的测试项目时，建议使用以下结构：

```
MyAutomationProject/
├── src/
│   ├── PageObjects/          # 页面对象模式
│   ├── Helpers/              # 辅助类
│   └── TestData/             # 测试数据
├── tests/
│   ├── UnitTests/            # 单元测试
│   └── IntegrationTests/     # 集成测试
├── MyAutomationProject.csproj
└── README.md
```

### 创建项目示例

```bash
# 创建解决方案
dotnet new sln -n MyAutomationProject

# 创建控制台项目
dotnet new console -n MyAutomationProject.Console
dotnet sln add MyAutomationProject.Console

# 创建测试项目
dotnet new xunit -n MyAutomationProject.Tests
dotnet sln add MyAutomationProject.Tests

# 添加 FlaUI 包
cd MyAutomationProject.Console
dotnet add package FlaUI.Core
dotnet add package FlaUI.UIA3

cd ../MyAutomationProject.Tests
dotnet add package FlaUI.Core
dotnet add package FlaUI.UIA3
```

---

## 开发工具推荐

### 1. **Inspect.exe**

Windows SDK 自带的工具，用于检查 UI 元素属性：

- 位置：`C:\Program Files (x86)\Windows Kits\10\bin\<version>\x64\inspect.exe`
- 功能：查看元素的 AutomationId、Name、ClassName 等属性
- 使用：运行 Inspect.exe，将鼠标悬停在要检查的 UI 元素上

### 2. **FlaUInspect**

FlaUI 官方提供的检查工具：

- GitHub：https://github.com/FlaUI/FlaUInspect
- 功能：更友好的界面，可以实时查看元素树结构
- 优势：专为 FlaUI 设计，显示的属性与代码中使用的一致

### 3. **Visual Studio 扩展**

推荐安装以下扩展：

- **Test Explorer**：运行和管理测试
- **Test Generator**：快速生成测试代码
- **CodeMaid**：代码清理和格式化

---

## 配置编译环境

### .csproj 配置示例

```xml
<Project Sdk="Microsoft.NET.Sdk">

  <PropertyGroup>
    <OutputType>Exe</OutputType>
    <TargetFramework>net6.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
    <PlatformTarget>x64</PlatformTarget> <!-- 重要：某些应用需要匹配位数 -->
  </PropertyGroup>

  <ItemGroup>
    <PackageReference Include="FlaUI.Core" Version="4.0.0" />
    <PackageReference Include="FlaUI.UIA3" Version="4.0.0" />
  </ItemGroup>

</Project>
```

### 重要配置项

1. **PlatformTarget**：某些应用程序需要匹配测试程序的位数（x86 或 x64）
2. **LangVersion**：如果使用新的 C# 特性，确保设置正确的语言版本

---

## 故障排查

### 常见安装问题

#### 问题 1：找不到 FlaUI 命名空间

**解决方案**：
- 确认已安装 `FlaUI.Core` 和 `FlaUI.UIA3`
- 重新生成解决方案
- 检查 .csproj 文件中的包引用

#### 问题 2：运行时找不到 DLL

**解决方案**：
- 清理并重新生成项目：`dotnet clean && dotnet build`
- 检查输出目录中是否有 FlaUI 相关 DLL

#### 问题 3：UIA3 不可用

**解决方案**：
- UIA3 需要 Windows 7 及以上版本
- 尝试使用 UIA2：`using FlaUI.UIA2;`

---

## 下一步

现在你已经完成了 FlaUI 的环境搭建，接下来可以：

1. 学习 [第一个 FlaUI 程序](./02-第一个FlaUI程序.md)
2. 了解 [UI 自动化基础概念](./03-UI自动化基础概念.md)
3. 查看 [常见问题与解决方案](./17-常见问题与解决方案.md)

---

## 参考资源

- **官方文档**：https://github.com/FlaUI/FlaUI
- **示例代码**：https://github.com/FlaUI/FlaUI/tree/master/src/FlaUI.Core.UITests
- **NuGet 页面**：https://www.nuget.org/packages/FlaUI.Core/
- **社区论坛**：https://github.com/FlaUI/FlaUI/discussions
