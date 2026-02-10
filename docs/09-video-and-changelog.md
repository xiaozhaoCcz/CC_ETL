# 视频教程与更新日志 (Video Tutorials & Update Log)

---

## 视频教程

项目提供演示视频，包含操作演示与功能介绍，便于快速上手。

- **B 站观看**：[Cc-ETL 项目演示视频](https://www.bilibili.com/video/BV1TBCkBzEex/?spm_id_from=333.1387.upload.video_card.click)

内容可涵盖：环境与启动、PC 端登录与配置、任务与任务组创建、画布编排与连线、运行与日志查看、数据源与 DataX 同步等（以实际视频为准）。

---

## 更新日志

以下根据项目 [README - 发展规划](../README.md#-发展规划) 与版本信息整理，便于了解当前完成度与后续方向。

### 版本与维护信息

- **文档/README 最后更新**：2025-01-07  
- **当前版本**：v2.0.0（以仓库 README 为准）  
- **维护者**：Cc-ETL Team  

### 已完成功能（节选）

- **任务编排引擎**：重构与拓扑编排，Executor-Compose 独立部署。
- **可视化任务编排**：PC 端拖拽画布、节点与边、10 种运行模式、高级配置（路由/阻塞/失败/超时/重试）、撤销重做、快捷键；489 个 GUI 测试用例。
- **多任务类型**：Bean、API、SQL、Shell、DataX、GLUE(Java/Shell/Python/PHP/Nodejs/PowerShell/C#)。
- **DataX 数据同步**：Admin 侧 Reader/Writer（MySQL、Oracle、PostgreSQL），Executor 侧执行。
- **双端界面**：以 PC 端为主，多面板（树形视图、画布、小地图、日志）、面板管理（展开/收起/弹出）。
- **技术栈**：Spring Boot 3、Java 17、数据库与结构优化。

### 进行中

- 调度中心相关页面/功能向 PC 端迁移。
- 用户体验与界面交互优化。
- 权限管理（用户与任务权限）。
- 条件节点功能（任务流程条件判断）。

### 未来规划（节选）

- **数据源**：Redis、MongoDB、ES、云数据库、Hadoop/Spark 等。
- **高级特性**：工作流引擎增强、监控告警增强、高并发与缓存优化。
- **生态**：插件扩展、REST API/SDK、国际化。
- **文档与社区**：API 文档、最佳实践、视频教程、开发者文档。

更细的条目与状态以仓库 [README - 发展规划](../README.md#-发展规划) 为准；具体版本号与日期以各 release 或 tag 为准。

---

[返回文档首页](README.md) | [上一章：项目扩展](08-extension.md)
