# CC-Job 架构迁移指南

## 📖 概述

本文档指导您如何将任务组编排逻辑从 `cc-job-admin` 模块迁移到独立的 `cc-job-executor-compose` 模块。

---

## 🎯 迁移目标

### 当前架构（Before）

```
cc-job-admin/
└── task/
    ├── handler/
    │   └── JobGroupXxlJob.java  ← 包含1200+行编排逻辑
    └── executor/
        ├── Async.java
        ├── WorkerWrapper.java
        └── ...
```

**问题：**
- ❌ Admin 职责过重
- ❌ 编排逻辑与 Admin 紧耦合
- ❌ 难以独立扩展和部署

### 目标架构（After）

```
cc-job-admin/
└── task/
    └── handler/
        └── JobGroupTrigger.java  ← 轻量级触发器（通过API调用）

cc-job-executor-compose/  ← 新增模块
├── engine/               ← 编排引擎（从admin迁移）
├── handler/
│   └── JobGroupExecutor.java  ← 编排执行器
└── client/
    └── AdminApiClient.java    ← Admin API客户端
```

**优势：**
- ✅ 职责清晰：Admin 专注调度，Executor-Compose 专注编排
- ✅ 低耦合：通过 REST API 通信
- ✅ 易扩展：编排逻辑可独立升级
- ✅ 灵活部署：可独立部署和扩展

---

## 📋 迁移步骤

### 第1步：准备工作

#### 1.1 备份代码

```bash
# 创建备份分支
git checkout -b backup/before-migration
git push origin backup/before-migration

# 切换回主分支
git checkout develop
```

#### 1.2 确认环境

- [x] JDK 17+
- [x] Maven 3.6+
- [x] cc-job-admin 正常运行
- [x] cc-job-executor 正常运行

---

### 第2步：在 Admin 中添加 API 接口

为了让 `cc-job-executor-compose` 能够获取任务信息，需要在 Admin 中添加 REST API 接口。

#### 2.1 创建 API Controller

在 `cc-job-admin` 中创建 `JobApiController.java`：

```java
package com.cc.job.admin.task.controller;

import com.cc.job.xo.model.entity.JobEdge;
import com.cc.job.xo.model.entity.JobInfo;
import com.cc.job.xo.model.entity.JobNode;
import com.cc.job.admin.task.service.JobInfoService;
import com.cc.job.admin.task.service.JobNodeService;
import com.cc.job.admin.task.service.JobEdgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Job API Controller - 提供给 Executor-Compose 调用的接口
 */
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobApiController {
    
    private final JobInfoService jobInfoService;
    private final JobNodeService jobNodeService;
    private final JobEdgeService jobEdgeService;
    
    /**
     * 获取任务信息
     */
    @GetMapping("/info/{jobId}")
    public JobInfo getJobInfo(@PathVariable Long jobId) {
        return jobInfoService.getById(jobId);
    }
    
    /**
     * 获取任务节点
     */
    @GetMapping("/nodes/{jobId}")
    public List<JobNode> getJobNodes(@PathVariable Long jobId) {
        return jobNodeService.getNodesByJobId(jobId);
    }
    
    /**
     * 获取任务边
     */
    @GetMapping("/edges/{jobId}")
    public List<JobEdge> getJobEdges(@PathVariable Long jobId) {
        return jobEdgeService.getEdgesByJobId(jobId);
    }
    
    /**
     * 上报任务状态
     */
    @PostMapping("/status")
    public Map<String, Object> reportStatus(@RequestBody Map<String, Object> params) {
        Long jobId = Long.valueOf(params.get("jobId").toString());
        String randomId = params.get("randomId").toString();
        Integer status = Integer.valueOf(params.get("status").toString());
        String message = params.get("message").toString();
        
        // TODO: 实现状态上报逻辑
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }
    
    /**
     * 获取快照
     */
    @GetMapping("/snapshot/{jobId}/{randomId}")
    public Map<String, String> getSnapshot(@PathVariable Long jobId, @PathVariable String randomId) {
        // TODO: 实现快照获取逻辑
        return null;
    }
}
```

#### 2.2 更新 Service 接口

在对应的 Service 中添加必要的方法：

```java
// JobNodeService.java
public interface JobNodeService {
    List<JobNode> getNodesByJobId(Long jobId);
}

// JobEdgeService.java
public interface JobEdgeService {
    List<JobEdge> getEdgesByJobId(Long jobId);
}
```

---

### 第3步：部署 Executor-Compose

#### 3.1 编译模块

```bash
cd cc-job
mvn clean package -pl cc-job-executor-compose -am -DskipTests
```

#### 3.2 配置执行器

编辑 `cc-job-executor-compose/src/main/resources/application.yml`：

```yaml
server:
  port: 8500

cc-job:
  admin:
    address: http://localhost:8989/xxl-job-admin
  access-token: default_token
  executor:
    appname: cc-job-executor-compose
    port: 10001
```

#### 3.3 启动执行器

```bash
java -jar cc-job-executor-compose/target/cc-job-executor-compose.jar
```

或使用 Maven：

```bash
mvn spring-boot:run -pl cc-job-executor-compose
```

#### 3.4 验证启动

检查日志是否出现：

```
CC-Job 任务组编排执行器启动成功！
```

访问健康检查：

```bash
curl http://localhost:8500/actuator/health
```

---

### 第4步：在 Admin 中注册执行器

#### 4.1 登录 Admin 管理界面

访问：`http://localhost:8989/xxl-job-admin`

#### 4.2 添加执行器

进入"执行器管理" -> "新增执行器"：

| 字段 | 值 |
|------|------|
| AppName | `cc-job-executor-compose` |
| 名称 | `任务组编排执行器` |
| 注册方式 | `自动注册` |
| 排序 | `1` |

保存后，稍等片刻，查看"机器地址"是否出现已注册的执行器。

---

### 第5步：迁移现有任务组

#### 5.1 更新任务组配置

将现有任务组的执行器改为 `cc-job-executor-compose`：

1. 进入"任务管理"
2. 找到类型为"任务组"的任务
3. 编辑任务
4. 将"执行器"改为 `cc-job-executor-compose`
5. 将"JobHandler"改为 `runJobGroupHandler`
6. 保存

#### 5.2 测试任务组执行

点击"执行"按钮，测试任务组是否正常运行。

---

### 第6步：重构 Admin 中的 JobGroupXxlJob（可选）

如果您希望完全移除 Admin 中的编排逻辑，可以按以下步骤操作：

#### 6.1 保留轻量级触发器

将 `JobGroupXxlJob` 重构为轻量级触发器：

```java
package com.cc.job.admin.task.handler;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

/**
 * 任务组触发器（轻量级）
 * 
 * 仅负责触发 Executor-Compose，不再包含编排逻辑
 */
@Component
public class JobGroupTrigger {
    
    @XxlJob("runJobGroupXxlJob")
    public void trigger() {
        // TODO: 通过 API 调用 Executor-Compose
        // 或直接删除，由 Executor-Compose 完全接管
    }
}
```

#### 6.2 移除旧代码（谨慎操作）

⚠️ **警告：在确保新架构完全正常后再执行此步骤！**

```bash
# 移除旧的编排引擎代码
rm -rf cc-job-admin/src/main/java/com/cc/job/admin/task/executor/
```

---

### 第7步：验证迁移

#### 7.1 功能验证清单

- [ ] 任务组可以正常创建
- [ ] 任务节点可以正常添加
- [ ] 任务依赖关系配置正确
- [ ] 任务组可以正常触发
- [ ] 任务按依赖顺序执行
- [ ] 任务状态正确上报
- [ ] 任务日志正常记录
- [ ] 任务失败重试正常
- [ ] 任务超时控制正常
- [ ] 任务暂停/恢复正常

#### 7.2 性能验证

对比迁移前后的性能指标：

| 指标 | 迁移前 | 迁移后 | 备注 |
|------|--------|--------|------|
| 任务组启动时间 | - | - | 应相近或更快 |
| 内存占用 | - | - | Admin 应减少 |
| CPU 占用 | - | - | 应分散到两个服务 |
| 并发任务组数 | - | - | 应相同或更多 |

#### 7.3 回归测试

运行完整的回归测试套件，确保所有功能正常。

---

## 🔄 回滚方案

如果迁移遇到问题，可以按以下步骤回滚：

### 1. 停止新服务

```bash
# 停止 Executor-Compose
kill $(ps aux | grep 'cc-job-executor-compose' | awk '{print $2}')
```

### 2. 恢复任务组配置

将任务组的执行器改回原来的配置。

### 3. 切换回备份分支

```bash
git checkout backup/before-migration
mvn clean package -DskipTests
# 重新部署 Admin
```

---

## 🐛 常见问题

### Q1: Executor-Compose 无法注册到 Admin

**A**: 检查以下几点：
1. Admin 地址配置是否正确
2. 访问令牌是否一致
3. 网络是否连通
4. 端口是否被占用

### Q2: 获取任务信息失败

**A**: 确认 Admin 是否实现了 API 接口：
```bash
curl http://localhost:8989/xxl-job-admin/api/job/info/1
```

### Q3: 任务组执行失败

**A**: 查看两边的日志：
- Admin 日志：`/data/applogs/cc-job/admin/`
- Executor-Compose 日志：`/data/applogs/cc-job/executor-compose/`

---

## 📊 迁移检查表

### 迁移前

- [ ] 代码已备份
- [ ] 环境已确认
- [ ] 测试环境已搭建
- [ ] 迁移计划已评审

### 迁移中

- [ ] Admin API 接口已实现
- [ ] Executor-Compose 已部署
- [ ] 执行器已注册
- [ ] 任务组已迁移
- [ ] 功能测试通过

### 迁移后

- [ ] 性能指标正常
- [ ] 监控告警配置完成
- [ ] 文档已更新
- [ ] 团队已培训
- [ ] 旧代码已清理（可选）

---

## 📚 相关资源

- [Executor-Compose README](../cc-job/cc-job-executor-compose/README.md)
- [架构分析报告](./architecture-analysis.md)
- [API 接口文档](./api-specification.md)

---

## 🆘 获取帮助

如果在迁移过程中遇到问题，可以：

1. 查看详细日志
2. 提交 GitHub Issue
3. 联系技术支持

---

**最后更新**：2025-11-30  
**作者**：CC-ETL Team
