# 任务组执行器Bug修复说明

## 问题描述

任务组执行器抽离到 `cc-job-executor-compose` 模块后,重新运行任务组时出现以下错误:

```
[AdminApiClient] 获取任务信息失败 - jobId: 25785, status: 404, body: <html>...
[JobGroupExecutorComplete] 获取任务信息失败 - jobId: 25785
[AdminApiClient] 上报状态失败 - jobId: 25785, status: 5, responseStatus: 404
```

## 根本原因

1. **API路径不匹配**: `AdminApiClient` 使用的API路径 (`/api/job/*`) 与Admin模块实际提供的路径 (`/api/v1/jobInfos/*`) 不一致
2. **缺失API接口**: Admin模块没有为executor-compose提供独立的获取任务信息、节点、边和上报状态的API接口
3. **返回格式不匹配**: Admin模块返回的是包装在 `Result` 对象中的数据,而 `AdminApiClient` 直接解析为实体对象

## 修复内容

### 1. 在Admin模块添加API接口

**文件**: `cc-job/cc-job-admin/src/main/java/com/cc/job/admin/task/controller/JobInfoController.java`

添加了以下供执行器调用的API接口:

```java
// 获取任务信息
@GetMapping("/{id}")
public Result<JobInfo> getJobInfoById(@PathVariable Long id)

// 获取任务组的所有节点
@GetMapping("/nodes/{jobId}")
public Result<List<JobNode>> getJobNodes(@PathVariable Long jobId)

// 获取任务组的所有边
@GetMapping("/edges/{jobId}")
public Result<List<JobEdge>> getJobEdges(@PathVariable Long jobId)

// 上报任务执行状态
@PostMapping("/status")
public Result<Void> reportStatus(@RequestBody Map<String, Object> statusData)
```

### 2. 修复AdminApiClient的API路径

**文件**: `cc-job/cc-job-executor-compose/src/main/java/com/cc/job/executor/compose/client/AdminApiClient.java`

修改了所有API调用路径和响应解析逻辑:

| 原路径 | 新路径 | 说明 |
|--------|--------|------|
| `/api/job/info/{jobId}` | `/api/v1/jobInfos/{id}` | 获取任务信息 |
| `/api/job/nodes/{jobId}` | `/api/v1/jobInfos/nodes/{jobId}` | 获取节点列表 |
| `/api/job/edges/{jobId}` | `/api/v1/jobInfos/edges/{jobId}` | 获取边列表 |
| `/api/job/status` | `/api/v1/jobInfos/status` | 上报状态 |

同时添加了对 `Result` 包装对象的解析:

```java
// 解析Result包装的响应
Map<String, Object> resultMap = JSONUtil.toBean(response.body(), Map.class);
Object data = resultMap.get("data");
if (data != null) {
    return JSONUtil.toBean(JSONUtil.toJsonStr(data), JobInfo.class);
}
```

### 3. 实现停止任务组功能

**文件**: `cc-job/cc-job-admin/src/main/java/com/cc/job/admin/task/service/impl/JobInfoServiceImpl.java`

完善了 `stopJobCompose` 方法的实现:

```java
@Override
public boolean stopJobCompose(Long id, String randomId) {
    // 1. 更新任务状态
    int flag = jobInfoMapper.stopJobCompose(id);
    
    // 2. 获取任务信息和执行器组信息
    JobInfo jobInfo = this.getById(id);
    JobGroup jobGroup = jobGroupService.getById(jobInfo.getJobGroup());
    
    // 3. 调用executor-compose的停止接口
    String addressList = jobGroup.getAddressList();
    if (StringUtils.isNotBlank(addressList)) {
        String[] addresses = addressList.split(",");
        for (String address : addresses) {
            String url = address.trim() + "/api/jobgroup/stop";
            HttpResponse response = HttpRequest.post(url)
                    .form("jobId", id)
                    .form("randomId", randomId)
                    .timeout(5000)
                    .execute();
        }
    }
    
    return flag > 0;
}
```

### 4. 停止任务组的完整流程

executor-compose模块已经实现了完整的停止功能:

1. **Controller接口**: `JobGroupControlController.stopJobGroup()` - 接收停止请求
2. **执行器处理**: `JobGroupExecutorComplete.stopJobGroup()` - 停止任务组
3. **引擎停止**: `Async.stopWork()` - 将所有任务状态置为失败(state=3)

## 验证步骤

1. 启动Admin模块
2. 启动executor-compose模块
3. 创建一个任务组
4. 触发任务组执行
5. 验证任务组正常运行
6. 调用停止接口验证任务组能正确停止

## API接口说明

### executor-compose提供的接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 停止任务组 | POST | `/api/jobgroup/stop` | 停止指定任务组 |
| 查询任务组状态 | GET | `/api/jobgroup/status` | 查询任务组运行状态 |
| 获取运行中的任务组 | GET | `/api/jobgroup/running` | 获取所有运行中的任务组 |
| 健康检查 | GET | `/api/jobgroup/health` | 服务健康检查 |

### Admin提供给executor的接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取任务信息 | GET | `/api/v1/jobInfos/{id}` | 获取任务详细信息 |
| 获取任务节点 | GET | `/api/v1/jobInfos/nodes/{jobId}` | 获取任务组的所有节点 |
| 获取任务边 | GET | `/api/v1/jobInfos/edges/{jobId}` | 获取任务组的所有边 |
| 上报状态 | POST | `/api/v1/jobInfos/status` | 上报任务执行状态 |

## 状态码说明

任务执行状态码:
- `0`: 失败
- `1`: 成功
- `2`: 执行中
- `5`: 任务组完成

## 注意事项

1. 确保executor-compose的地址配置在执行器组的 `addressList` 中
2. 确保Admin和executor-compose之间网络畅通
3. 确保 `accessToken` 配置正确
4. API调用超时设置为30秒,如果任务执行时间过长需要调整

## 相关文件清单

### 修改的文件
1. `cc-job/cc-job-admin/src/main/java/com/cc/job/admin/task/controller/JobInfoController.java`
2. `cc-job/cc-job-admin/src/main/java/com/cc/job/admin/task/service/impl/JobInfoServiceImpl.java`
3. `cc-job/cc-job-executor-compose/src/main/java/com/cc/job/executor/compose/client/AdminApiClient.java`

### 已存在的关键文件
1. `cc-job/cc-job-executor-compose/src/main/java/com/cc/job/executor/compose/handler/JobGroupExecutorComplete.java` - 任务组执行器
2. `cc-job/cc-job-executor-compose/src/main/java/com/cc/job/executor/compose/controller/JobGroupControlController.java` - 控制接口
3. `cc-job/cc-job-executor-compose/src/main/java/com/cc/job/executor/compose/engine/Async.java` - 异步执行引擎

## 测试建议

1. 单元测试: 测试API接口的正确性
2. 集成测试: 测试Admin与executor-compose的交互
3. 端到端测试: 完整的任务组创建、执行、停止流程
4. 压力测试: 测试多任务组并发执行和停止

## 后续优化建议

1. 添加重试机制,提高API调用的可靠性
2. 添加缓存机制,减少重复的API调用
3. 优化状态上报,支持批量上报
4. 添加监控和告警功能
5. 支持任务组的暂停和恢复功能

---

修复完成日期: 2025-11-30
修复人员: AI Assistant
