package com.xxl.job.core.glue;

import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * C# GLUE 工厂类
 * 负责编译和执行 C# 代码
 * 
 * @author cc-job-team
 */
public class CSharpGlueFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(CSharpGlueFactory.class);
    
    /**
     * C# 代码模板
     * 将用户代码包装成可执行的 C# 程序
     * 提供 XxlJobHelper 类，与 Java 版本的方法名称保持一致
     */
    private static final String CSHARP_TEMPLATE = 
        "using System;\n" +
        "using System.IO;\n" +
        "\n" +
        "// XxlJobHelper 类，提供与 Java 版本一致的 API\n" +
        "public static class XxlJobHelper\n" +
        "{\n" +
        "    // 记录日志（对应 Java 的 XxlJobHelper.log）\n" +
        "    public static void log(string message, params object[] args)\n" +
        "    {\n" +
        "        string logMessage = args.Length > 0 ? string.Format(message, args) : message;\n" +
        "        Console.WriteLine($\"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] {logMessage}\");\n" +
        "    }\n" +
        "    \n" +
        "    // 记录日志（大写版本，兼容性）\n" +
        "    public static void Log(string message, params object[] args)\n" +
        "    {\n" +
        "        log(message, args);\n" +
        "    }\n" +
        "    \n" +
        "    // 获取任务参数（对应 Java 的 XxlJobHelper.getJobParam()）\n" +
        "    public static string getJobParam()\n" +
        "    {\n" +
        "        return Environment.GetEnvironmentVariable(\"XXL_JOB_PARAM\") ?? \"\";\n" +
        "    }\n" +
        "    \n" +
        "    // 获取任务参数（大写版本，兼容性）\n" +
        "    public static string GetJobParam()\n" +
        "    {\n" +
        "        return getJobParam();\n" +
        "    }\n" +
        "    \n" +
        "    // 获取分片序号（对应 Java 的 XxlJobHelper.getShardIndex()）\n" +
        "    public static int getShardIndex()\n" +
        "    {\n" +
        "        string index = Environment.GetEnvironmentVariable(\"XXL_JOB_SHARD_INDEX\") ?? \"0\";\n" +
        "        return int.TryParse(index, out int result) ? result : 0;\n" +
        "    }\n" +
        "    \n" +
        "    // 获取分片序号（大写版本，兼容性）\n" +
        "    public static int GetShardIndex()\n" +
        "    {\n" +
        "        return getShardIndex();\n" +
        "    }\n" +
        "    \n" +
        "    // 获取分片总数（对应 Java 的 XxlJobHelper.getShardTotal()）\n" +
        "    public static int getShardTotal()\n" +
        "    {\n" +
        "        string total = Environment.GetEnvironmentVariable(\"XXL_JOB_SHARD_TOTAL\") ?? \"1\";\n" +
        "        return int.TryParse(total, out int result) ? result : 1;\n" +
        "    }\n" +
        "    \n" +
        "    // 获取分片总数（大写版本，兼容性）\n" +
        "    public static int GetShardTotal()\n" +
        "    {\n" +
        "        return getShardTotal();\n" +
        "    }\n" +
        "    \n" +
        "    // 获取执行上下文数据（JSON格式）\n" +
        "    public static string getContextJson()\n" +
        "    {\n" +
        "        return Environment.GetEnvironmentVariable(\"XXL_JOB_CONTEXT\") ?? \"{}\";\n" +
        "    }\n" +
        "    \n" +
        "    // 获取执行上下文数据（JSON格式，大写版本）\n" +
        "    public static string GetContextJson()\n" +
        "    {\n" +
        "        return getContextJson();\n" +
        "    }\n" +
        "}\n" +
        "\n" +
        "public class DynamicJobHandler\n" +
        "{\n" +
        "    public static void Main(string[] args)\n" +
        "    {\n" +
        "        try\n" +
        "        {\n" +
        "            // 用户代码\n" +
        "            %USER_CODE%\n" +
        "            \n" +
        "            Environment.Exit(0);\n" +
        "        }\n" +
        "        catch (Exception ex)\n" +
        "        {\n" +
        "            Console.Error.WriteLine($\"Error: {ex.Message}\");\n" +
        "            Console.Error.WriteLine(ex.StackTrace);\n" +
        "            Environment.Exit(1);\n" +
        "        }\n" +
        "    }\n" +
        "}";

    /**
     * 执行 C# GLUE 代码
     * 
     * @param codeSource C# 源代码
     * @param jobId 任务ID
     * @param glueUpdatetime GLUE更新时间
     * @throws Exception 执行异常
     */
    public static void executeCSharpGlue(String codeSource, int jobId, long glueUpdatetime) throws Exception {
        executeCSharpGlue(codeSource, jobId, glueUpdatetime, null);
    }
    
    /**
     * 执行 C# GLUE 代码（带上下文数据）
     * 
     * @param codeSource C# 源代码
     * @param jobId 任务ID
     * @param glueUpdatetime GLUE更新时间
     * @param contextData 执行上下文数据（JSON格式）
     * @throws Exception 执行异常
     */
    public static void executeCSharpGlue(String codeSource, int jobId, long glueUpdatetime, String contextData) throws Exception {
        logger.info("[CSharpGlueFactory] 开始执行 C# GLUE 任务 - jobId: {}", jobId);
        
        // 1. 提取用户代码
        String userCode = extractUserCode(codeSource);
        
        // 2. 生成完整的 C# 程序
        String fullCode = CSHARP_TEMPLATE.replace("%USER_CODE%", userCode);
        
        // 3. 创建临时文件目录
        String tempDir = System.getProperty("java.io.tmpdir");
        String fileName = jobId + "_" + glueUpdatetime + "_" + UUID.randomUUID().toString().substring(0, 8);
        String csFile = tempDir + File.separator + fileName + ".cs";
        
        // 4. 写入 C# 文件
        try (FileOutputStream fos = new FileOutputStream(csFile)) {
            fos.write(fullCode.getBytes(StandardCharsets.UTF_8));
        }
        
        // 5. 获取日志文件路径
        String logFile = XxlJobContext.getXxlJobContext().getJobLogFileName();
        
        // 6. 编译并执行
        try {
            compileAndRun(csFile, logFile, contextData);
        } finally {
            // 7. 清理临时文件
            File tempFile = new File(csFile);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * 从用户代码中提取可执行代码
     * 支持两种格式：
     * 1. 完整的类定义（类似 Java GLUE），提取 Execute 方法体
     * 2. 直接的方法体代码
     */
    private static String extractUserCode(String codeSource) {
        if (codeSource == null || codeSource.trim().isEmpty()) {
            return "Log(\"C# GLUE 代码为空\");";
        }
        
        String trimmed = codeSource.trim();
        
        // 如果包含类定义和 Execute 方法，尝试提取 Execute 方法体
        if (trimmed.contains("class") && trimmed.contains("Execute()")) {
            // 查找 Execute 方法
            int executeIndex = trimmed.indexOf("Execute()");
            if (executeIndex > 0) {
                // 找到 Execute 方法后的第一个左大括号
                int startBrace = trimmed.indexOf("{", executeIndex);
                if (startBrace > 0) {
                    // 找到匹配的右大括号
                    int braceCount = 1;
                    int endBrace = startBrace + 1;
                    while (endBrace < trimmed.length() && braceCount > 0) {
                        char c = trimmed.charAt(endBrace);
                        if (c == '{') braceCount++;
                        if (c == '}') braceCount--;
                        if (braceCount > 0) endBrace++;
                    }
                    
                    if (braceCount == 0) {
                        String methodBody = trimmed.substring(startBrace + 1, endBrace).trim();
                        // 如果方法体为空或只包含注释，返回默认代码
                        if (methodBody.isEmpty() || methodBody.replaceAll("//.*|/\\*[\\s\\S]*?\\*/", "").trim().isEmpty()) {
                            return "XxlJobHelper.log(\"XXL-JOB, Hello C# World.\");";
                        }
                        // 不需要替换 XxlJobHelper 的调用，因为模板中已经定义了 XxlJobHelper 类
                        return methodBody;
                    }
                }
            }
        }
        
        // 如果找不到 Execute 方法，假设整个代码就是可执行代码
        // 检查是否包含类定义，如果包含则提取类内容
        if (trimmed.contains("class") && trimmed.contains("{")) {
            int classStart = trimmed.indexOf("class");
            int firstBrace = trimmed.indexOf("{", classStart);
            if (firstBrace > 0) {
                // 找到类的结束大括号
                int braceCount = 1;
                int endBrace = firstBrace + 1;
                while (endBrace < trimmed.length() && braceCount > 0) {
                    char c = trimmed.charAt(endBrace);
                    if (c == '{') braceCount++;
                    if (c == '}') braceCount--;
                    if (braceCount > 0) endBrace++;
                }
                if (braceCount == 0) {
                    // 提取类内容
                    String classContent = trimmed.substring(firstBrace + 1, endBrace).trim();
                    // 不需要替换 XxlJobHelper 的调用，因为模板中已经定义了 XxlJobHelper 类
                    return classContent;
                }
            }
        }
        
        // 如果都不匹配，直接返回代码（可能是纯方法体代码）
        // 不需要替换 XxlJobHelper 的调用，因为模板中已经定义了 XxlJobHelper 类
        return trimmed;
    }
    
    /**
     * 编译并运行 C# 代码
     * 使用 dotnet-script 或 dotnet run 执行
     */
    private static void compileAndRun(String csFile, String logFile) throws Exception {
        compileAndRun(csFile, logFile, null);
    }
    
    /**
     * 编译并运行 C# 代码（带上下文数据）
     * 使用 dotnet-script 或 dotnet run 执行
     */
    private static void compileAndRun(String csFile, String logFile, String contextData) throws Exception {
        // 检查是否安装了 dotnet-script
        String command = "dotnet-script";
        String scriptFile = csFile;
        
        // 如果 dotnet-script 不可用，尝试使用 dotnet run
        if (!isCommandAvailable(command)) {
            logger.warn("[CSharpGlueFactory] dotnet-script 不可用，尝试使用 dotnet run");
            // 创建临时项目并编译运行
            compileAndRunWithDotnet(csFile, logFile, contextData);
            return;
        }
        
        // 使用 dotnet-script 执行
        XxlJobHelper.log("----------- C# script file:" + scriptFile + " -----------");
        
        // 设置环境变量
        ProcessBuilder pb = new ProcessBuilder(command, scriptFile);
        pb.environment().put("XXL_JOB_PARAM", XxlJobHelper.getJobParam());
        pb.environment().put("XXL_JOB_SHARD_INDEX", 
            String.valueOf(XxlJobContext.getXxlJobContext().getShardIndex()));
        pb.environment().put("XXL_JOB_SHARD_TOTAL", 
            String.valueOf(XxlJobContext.getXxlJobContext().getShardTotal()));
        
        // 设置上下文数据（如果存在）
        if (contextData != null && !contextData.isEmpty()) {
            pb.environment().put("XXL_JOB_CONTEXT", contextData);
            XxlJobHelper.log("----------- 设置执行上下文数据到环境变量 -----------");
        }
        
        // 执行脚本
        Process process = pb.start();
        
        // 处理输出流
        Thread inputThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int len;
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile, true)) {
                    while ((len = process.getInputStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        fos.flush();
                    }
                }
            } catch (IOException e) {
                logger.error("读取 C# 输出流失败", e);
            }
        });
        
        Thread errThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int len;
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile, true)) {
                    while ((len = process.getErrorStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        fos.flush();
                    }
                }
            } catch (IOException e) {
                logger.error("读取 C# 错误流失败", e);
            }
        });
        
        inputThread.start();
        errThread.start();
        
        int exitValue = process.waitFor();
        inputThread.join();
        errThread.join();
        
        if (exitValue != 0) {
            throw new RuntimeException("C# 脚本执行失败，退出码: " + exitValue);
        }
    }
    
    /**
     * 使用 dotnet run 编译并运行
     */
    private static void compileAndRunWithDotnet(String csFile, String logFile) throws Exception {
        compileAndRunWithDotnet(csFile, logFile, null);
    }
    
    /**
     * 使用 dotnet run 编译并运行（带上下文数据）
     */
    private static void compileAndRunWithDotnet(String csFile, String logFile, String contextData) throws Exception {
        // 创建临时项目目录
        File csFileObj = new File(csFile);
        String projectDir = csFileObj.getParent();
        String projectName = "XxlJobCSharp_" + System.currentTimeMillis();
        String projectPath = projectDir + File.separator + projectName;
        
        // 创建项目目录
        new File(projectPath).mkdirs();
        
        // 检测系统中已安装的 .NET 版本
        String targetFramework = detectDotNetFramework();
        logger.info("[CSharpGlueFactory] 检测到 .NET 框架版本: {}", targetFramework);
        
        // 创建 .csproj 文件
        String csprojContent = 
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<Project Sdk=\"Microsoft.NET.Sdk\">\n" +
            "  <PropertyGroup>\n" +
            "    <OutputType>Exe</OutputType>\n" +
            "    <TargetFramework>" + targetFramework + "</TargetFramework>\n" +
            "    <Nullable>enable</Nullable>\n" +
            "  </PropertyGroup>\n" +
            "</Project>";
        
        String csprojFile = projectPath + File.separator + projectName + ".csproj";
        try (FileOutputStream fos = new FileOutputStream(csprojFile)) {
            fos.write(csprojContent.getBytes(StandardCharsets.UTF_8));
        }
        
        // 复制 C# 文件到项目目录
        String targetCsFile = projectPath + File.separator + "Program.cs";
        try (java.io.FileInputStream fis = new java.io.FileInputStream(csFile);
             FileOutputStream fos = new FileOutputStream(targetCsFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        
        // 编译并运行
        ProcessBuilder pb = new ProcessBuilder("dotnet", "run", "--project", csprojFile);
        pb.directory(new File(projectPath));
        pb.environment().put("XXL_JOB_PARAM", XxlJobHelper.getJobParam());
        pb.environment().put("XXL_JOB_SHARD_INDEX", 
            String.valueOf(XxlJobContext.getXxlJobContext().getShardIndex()));
        pb.environment().put("XXL_JOB_SHARD_TOTAL", 
            String.valueOf(XxlJobContext.getXxlJobContext().getShardTotal()));
        
        // 设置上下文数据（如果存在）
        if (contextData != null && !contextData.isEmpty()) {
            pb.environment().put("XXL_JOB_CONTEXT", contextData);
            XxlJobHelper.log("----------- 设置执行上下文数据到环境变量 -----------");
        }
        
        Process process = pb.start();
        
        // 处理输出
        Thread inputThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int len;
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile, true)) {
                    while ((len = process.getInputStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        fos.flush();
                    }
                }
            } catch (IOException e) {
                logger.error("读取 dotnet 输出流失败", e);
            }
        });
        
        Thread errThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int len;
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(logFile, true)) {
                    while ((len = process.getErrorStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        fos.flush();
                    }
                }
            } catch (IOException e) {
                logger.error("读取 dotnet 错误流失败", e);
            }
        });
        
        inputThread.start();
        errThread.start();
        
        int exitValue = process.waitFor();
        inputThread.join();
        errThread.join();
        
        // 清理项目目录
        deleteDirectory(new File(projectPath));
        
        if (exitValue != 0) {
            throw new RuntimeException("C# 编译执行失败，退出码: " + exitValue);
        }
    }
    
    /**
     * 检查命令是否可用
     */
    private static boolean isCommandAvailable(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{command, "--version"});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测系统中已安装的 .NET 框架版本
     * 返回类似 "net8.0", "net7.0", "net6.0" 的字符串
     */
    private static String detectDotNetFramework() {
        try {
            // 执行 dotnet --list-runtimes 命令获取已安装的运行时
            Process process = Runtime.getRuntime().exec(new String[]{"dotnet", "--list-runtimes"});
            
            // 读取输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            
            String line;
            String latestVersion = null;
            int latestMajor = 0;
            
            while ((line = reader.readLine()) != null) {
                // 查找 Microsoft.NETCore.App 运行时
                // 输出格式类似: Microsoft.NETCore.App 8.0.21 [/Users/xiaozhao/.dotnet/shared/Microsoft.NETCore.App]
                if (line.contains("Microsoft.NETCore.App")) {
                    // 提取版本号，例如 "8.0.21"
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        String version = parts[1];
                        // 提取主版本号
                        String[] versionParts = version.split("\\.");
                        if (versionParts.length >= 1) {
                            try {
                                int major = Integer.parseInt(versionParts[0]);
                                if (major > latestMajor) {
                                    latestMajor = major;
                                    latestVersion = "net" + major + ".0";
                                }
                            } catch (NumberFormatException e) {
                                // 忽略解析错误
                            }
                        }
                    }
                }
            }
            
            process.waitFor();
            reader.close();
            
            // 如果找到了版本，返回它
            if (latestVersion != null) {
                logger.info("[CSharpGlueFactory] 检测到 .NET 版本: {}", latestVersion);
                return latestVersion;
            }
        } catch (Exception e) {
            logger.warn("[CSharpGlueFactory] 检测 .NET 版本失败: {}", e.getMessage());
        }
        
        // 如果检测失败，按优先级尝试常见版本（从高到低）
        // 优先使用较新的版本，因为新版本通常向后兼容
        logger.info("[CSharpGlueFactory] 使用默认版本 net8.0（如果不可用，将在运行时失败并提示）");
        return "net8.0";
    }
    
    /**
     * 递归删除目录
     */
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}

