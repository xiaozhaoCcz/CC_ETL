# Cc-ETL 开放 API 说明

以下为常用开放接口，可用于集成、自动化与运维。完整接口见 Knife4j：`http://{admin-host}:8989/xxl-job-admin/doc.html`。

## 1. 触发任务

**POST** `/api/v1/jobInfos/trigger`

请求体示例：
```json
{
  "id": 123,
  "executorParam": ""
}
```

curl 示例：
```bash
curl -X POST "http://localhost:8989/xxl-job-admin/api/v1/jobInfos/trigger" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"id":123,"executorParam":""}'
```

返回为本次执行的日志 ID（logId）。

## 2. 任务报表统计

**GET** `/api/v1/dashboard/stats?filterTimeStart=2025-01-01T00:00:00&filterTimeEnd=2025-01-31T23:59:59&jobId=`

返回：任务组数、任务数、执行成功/失败/运行中次数。

## 3. 按日趋势

**GET** `/api/v1/dashboard/trend?filterTimeStart=2025-01-01&filterTimeEnd=2025-01-31&jobId=`

返回：按日的成功/失败/运行中统计列表。

## 4. 健康度与失败 Top N

**GET** `/api/v1/dashboard/health?filterTimeStart=2025-01-01T00:00:00&filterTimeEnd=2025-01-31T23:59:59&jobId=&topN=10&recentLogLimit=20`

返回：成功率、失败率、失败任务 Top N、最近失败日志 ID 列表（便于跳转）。

## 5. 执行时长与 SLA

**GET** `/api/v1/dashboard/execution-stats?filterTimeStart=2025-01-01T00:00:00&filterTimeEnd=2025-01-31T23:59:59&jobId=&timeoutThresholdSeconds=300&slowLogLimit=20`

返回：平均耗时、P99、超时次数、慢日志列表。

## 6. 导出报表 Excel

**GET** `/api/v1/dashboard/export?filterTimeStart=&filterTimeEnd=&jobId=`

返回：Excel 文件流（汇总 + 按日趋势）。

## 7. 任务组执行状态

**GET** `/api/v1/jobInfos/getJobStatus/{id}`

返回：该任务（或任务组）是否正在运行。

## 8. 日志分页与检索

**GET** `/api/v1/jobLogs/page`

查询参数：`pageNum`、`pageSize`、`jobId`（可选）、`jobGroup`（可选）、`logStatus`（可选，1=成功 2=失败 3=运行中）、`filterTime`（可选，时间范围数组 [start, end]）、`keyword`（可选，按执行结果 handle_msg 模糊匹配）。

返回：执行日志分页列表。

**POST** `/api/v1/jobLogs/archive` 日志归档

请求体：`{ "olderThanDays": 90 }`，表示删除早于 90 天的日志，保留最近 N 天。返回本次删除条数。可用于定期清理或归档策略。

## 9. 审批/人工节点待办

- **GET** `/api/v1/approvals/pending?jobId=` 待审批列表
- **POST** `/api/v1/approvals/{id}/approve` 审批通过（body 可选 `{"remark":""}`）
- **POST** `/api/v1/approvals/{id}/reject` 审批拒绝

执行到审批节点时需在业务侧创建 `job_approval_pending` 记录；审批后可通过上述接口更新状态，并自行触发后续节点。

## 10. 任务组版本与回滚

- **GET** `/api/v1/jobGroupSnapshots/versions?jobId={jobId}&limit=50`  
  列出该任务组的手动保存版本（randomId 以 `ver_` 开头）。

- **POST** `/api/v1/jobGroupSnapshots/saveVersion`  
  保存当前画布为版本。请求体：`{ "jobId", "versionName", "nodesJson", "edgesJson" }`。

- **GET** `/api/v1/jobGroupSnapshots/{id}`  
  根据快照 ID 获取快照内容（nodesJson、edgesJson），用于回滚到该版本。

## 11. 参数与密钥

- **GET** `/api/v1/jobParams` 列表
- **POST** `/api/v1/jobParams` 新增或更新（body: paramKey, paramValue, comment）
- **DELETE** `/api/v1/jobParams/key/{key}` 按 key 删除
- **POST** `/api/v1/jobParams/resolve` body: `{"text": "jdbc:${db_url}"}` 解析占位符（执行时自动替换 executorParam 中的 ${key}）

## 12. 数据质量校验配置

- **GET** `/api/v1/jobValidations/byJob/{jobId}` 按任务获取校验配置
- **POST** `/api/v1/jobValidations` 保存校验配置（body: jobId, validationSql, expectedMinRows, jdbcDatasourceId）
- **DELETE** `/api/v1/jobValidations/byJob/{jobId}` 删除校验配置

---

## 认证说明

- 若启用登录，请求头需带 `Authorization: Bearer <JWT>`。
- 未启用时可直接调用（仅内网环境建议）。

## 告警扩展（SPI）

新增告警渠道：实现 `com.cc.job.admin.task.alarm.JobAlarm` 接口，实现 `doAlarm(JobInfo info, JobLog jobLog)`，并标注 `@Component`，Admin 启动时会自动注册，任务失败时会依次调用所有 JobAlarm 实现（邮件、Webhook、钉钉、企微、飞书等）。
