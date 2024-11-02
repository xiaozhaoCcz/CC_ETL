
<div align="center">
   <img alt="logo" width="100" height="100" src="https://foruda.gitee.com/images/1724259461244885014/4de96569_716974.png">
   <h2>youlai-boot</h2>
   <img alt="有来技术" src="https://img.shields.io/badge/Java -17-brightgreen.svg"/>
   <img alt="有来技术" src="https://img.shields.io/badge/SpringBoot-3.3.4-green.svg"/>
   <a href="https://gitee.com/youlaiorg/youlai-boot" target="_blank">
     <img alt="有来技术" src="https://gitee.com/youlaiorg/youlai-boot/badge/star.svg"/>
   </a>     
   <a href="https://github.com/haoxianrui" target="_blank">
     <img alt="有来技术" src="https://img.shields.io/github/stars/haoxianrui/youlai-boot.svg?style=social&label=Stars"/>
   </a>
   <br/>
   <img alt="有来技术" src="https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg"/>
   <a href="https://gitee.com/youlaiorg" target="_blank">
     <img alt="有来技术" src="https://img.shields.io/badge/Author-有来开源组织-orange.svg"/>
   </a>
</div>

![](https://raw.gitmirror.com/youlaitech/image/main/docs/rainbow.png)

<div align="center">
  <a target="_blank" href="https://admin.youlai.tech/">🔍 在线预览</a> |  <a target="_blank" href="https://doc.youlai.tech/%E5%89%8D%E5%90%8E%E7%AB%AF%E6%A8%A1%E6%9D%BF/%E5%90%8E%E7%AB%AF%E6%89%8B%E5%86%8C/%E9%A1%B9%E7%9B%AE%E7%AE%80%E4%BB%8B.html">📖 阅读文档</a> | <a href="./README.en-US.md">🌐English</a>
</div>

## 📢 项目简介

**在线预览**: [https://vue3.youlai.tech](https://vue3.youlai.tech)

基于 JDK 17、Spring Boot 3、Spring Security 6、JWT、Redis、Mybatis-Plus、Knife4j、Vue 3、Element-Plus 构建的前后端分离单体权限管理系统。

- **🚀 开发框架**: 使用 Spring Boot 3 和 Vue 3，以及 Element-Plus 等主流技术栈，实时更新。

- **🔐 安全认证**: 结合 Spring Security 和 JWT 提供安全、无状态、分布式友好的身份验证和授权机制。

- **🔑 权限管理**: 基于 RBAC 模型，实现细粒度的权限控制，涵盖接口方法和按钮级别。

- **🛠️ 功能模块**: 包括用户管理、角色管理、菜单管理、部门管理、字典管理等多个功能。

- **📘 接口文档**: 自动生成接口文档，支持在线调试，提高开发效率。

## 📁 项目目录
```
youlai-boot
├── sql                                 # SQL脚本
│   ├── mysql5                          # MySQL5 脚本
│   └── mysql8                          # MySQL8 脚本
├── src                                 # 源码目录
│   ├── common                          # 公共模块
│   │   ├── annotation                  # 注解定义
│   │   ├── base                        # 基础类
│   │   ├── constant                    # 常量
│   │   ├── enums                       # 枚举类型
│   │   ├── exception                   # 异常处理
│   │   ├── model                       # 数据模型
│   │   ├── result                      # 结果封装
│   │   └── util                        # 工具类
│   ├── config                          # 自动装配配置
│   │   ├── property                    # 配置属性
│   │   │   ├── AliyunSmsProperties     # 阿里云短信配置属性
│   │   │   ├── CaptchaProperties       # 验证码配置属性
│   │   │   ├── CodegenProperties       # 文件配置属性
│   │   │   ├── MailProperties          # 邮件配置属性
│   │   │   ├── SecurityProperties      # 安全配置属性
│   │   ├── CorsConfig                  # 跨域共享配置
│   │   ├── MybatisConfig               # Mybatis 自动装配配置
│   │   ├── RedisCacheConfig            # Redis 缓存自动装配配置
│   │   ├── RedisConfig                 # Redis 自动装配配置
│   │   ├── SecurityConfig              # Spring Security 自动装配配置
│   │   ├── SwaggerConfig               # API 接口文档配置
│   │   ├── WebMvcConfig                # WebMvc 配置
│   │   ├── WebSocketConfig             # WebSocket 自动装配配置
│   │   └── XxlJobConfig                # XXL-JOB 自动装配配置
│   ├── core                            # 核心功能
│   │   ├── aspect                      # 切面
│   │   │   ├── LogAspect               # 日志切面
│   │   │   └── RepeatSubmitAspect      # 防重提交切面
│   │   ├── filter                      # 过滤器
│   │   │   ├── RateLimiterFilter       # 限流过滤器
│   │   │   └── RequestLogFilter        # 请求日志过滤器
│   │   ├── handler                     # 处理器
│   │   │   ├── MyDataPermissionHandler # 数据权限处理器
│   │   │   └── MyMetaObjectHandler     # 元对象字段填充处理器
│   │   └── security                    # Security 安全中心
│   ├── modules                         # 业务模块
│   │   ├── member                      # 会员模块【业务模块演示】
│   │   ├── order                       # 订单模块【业务模块演示】
│   │   ├── product                     # 商品模块【业务模块演示】
│   ├── shared                          # 共享模块
│   │   ├── auth                        # 认证模块
│   │   ├── file                        # 文件模块
│   │   ├── codegen                     # 代码生成模块
│   │   ├── mail                        # 邮件模块
│   │   ├── sms                         # 短信模块
│   │   └── websocket                   # WebSocket 模块
│   ├── system                          # 系统模块
│   │   ├── controller                  # 控制层
│   │   ├── converter                   # MapStruct 转换器
│   │   ├── event                       # 事件处理
│   │   ├── handler                     # 处理器
│   │   ├── listener                    # 监听器
│   │   ├── model                       # 模型层
│   │   │   ├── bo                      # 业务对象
│   │   │   ├── dto                     # 数据传输对象
│   │   │   ├── entity                  # 实体对象
│   │   │   ├── form                    # 表单对象
│   │   │   ├── query                   # 查询参数对象
│   │   │   └── vo                      # 视图对象
│   │   ├── mapper                      # 数据库访问层
│   │   └── service                     # 业务逻辑层
│   └── YouLaiApplication               # 启动类
└── end                             
```

## 🌺 前端工程
| Gitee | Github |
|-------|------|
| [vue3-element-admin](https://gitee.com/youlaiorg/vue3-element-admin)  | [vue3-element-admin](https://github.com/youlaitech/vue3-element-admin)  |


## 🌈 接口文档

- `knife4j` 接口文档：[http://localhost:8989/doc.html](http://localhost:8989/doc.html)
- `swagger` 接口文档：[http://localhost:8989/swagger-ui/index.html](http://localhost:8989/swagger-ui/index.html)
- `apifox`  在线接口文档：[https://www.apifox.cn/apidoc](https://www.apifox.cn/apidoc/shared-195e783f-4d85-4235-a038-eec696de4ea5)


## 🚀 项目启动

1. **数据库初始化**

    执行 [youlai_boot.sql](sql/mysql8/youlai_boot.sql) 脚本完成数据库创建、表结构和基础数据的初始化。

2. **修改配置**

    [application-dev.yml](src/main/resources/application-dev.yml) 修改MySQL、Redis连接配置；

3. **启动项目**

    执行 [SystemApplication.java](src/main/java/com/cc/job/YouLaiApplication.java) 的 main 方法完成后端项目启动；

    访问接口文档地址 [http://localhost:8989/doc.html](http://localhost:8989/doc.html) 验证项目启动是否成功。

## ✅ 项目统计

![Alt](https://repobeats.axiom.co/api/embed/544c5c0b5b3611a6c4d5ef0faa243a9066b89659.svg "Repobeats analytics image")

Thanks to all the contributors!

[![contributors](https://contrib.rocks/image?repo=haoxianrui/youlai-boot)](https://github.com/haoxianrui/youlai-boot/graphs/contributors)


## 💖 加交流群

> 关注公众号【有来技术】，获取交流群二维码，不想关注公众号或二维码过期欢迎加我微信(`haoxianrui`)备注【有来】即可，拉你进群。

| ![](https://s2.loli.net/2022/11/19/OGjum9wr8f6idLX.png) |
|---------------------------------------------------------|


