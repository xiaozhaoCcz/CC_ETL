# Cc-ETL

<p align=center>
    基于xxl-job设计的可视化任务调度工具
</p>
<p align="center">


## 项目介绍
基于xxl-job改造的可视化定时任务调度工具，支持任务可视化拖拽，支持任务失败重试，任务暂停和预测任务到达时间，设定超时时间等。支持单任务多任务串并联运行，可视化观察每一个任务运行的状况，并集成datax工具实现不同数据源的数据同步。

## 数据库地址
/doc/cc_job_admin.sql

## 项目功能
- **对任务编排模块前端进行重写，支持任务和任务组的编辑**
- xxl-job的所有功能都已集成
- 新增datax数据同步功能
  - 目前支持mysql和oracle数据全量和增量同步
  - 后续会支持更多数据源
- 支持存储过程调用和sql调用
- 支持api任务调度
- 可视化任务编排
  1. **新增预测任务节点开始运行的时间和运行完成的时间**
  2.  **新增任务节点暂停运行**
  3. 支持定义任务节点超时时间
  4. 支持任务失败重试
  5. 支持失败任务忽略运行
  6. 任务节点运行状态可视化展示
  7. 支持任务组节点串并联运行

## 任务调度
1. 新建任务或任务组，支持拖拽的形式创建任务和任务组，**选择节点后要点击编辑选择任务或任务组**

   | ![image-20241219212046125](./doc/img2/main/1.png) | ![image-20241219212046125](./doc/img2/main/2.png) |
   | ------------------------------------------------- | ------------------------------------------------- |
   | ![image-20241219212046125](./doc/img2/main/3.png) | ![image-20241219212046125](./doc/img2/main/4.png) |

2. 保存任务组

   | ![image-20241219212046125](./doc/img2/main/5.png) | ![image-20241219212046125](./doc/img2/main/6.png) |
   | ------------------------------------------------- | ------------------------------------------------- |
   | ![image-20241219212046125](./doc/img2/main/7.png) | ![image-20241219212046125](./doc/img2/main/8.png) |

3. 运行任务组

   **测试任务组3**，支持任务组中串联任务组，运行过程中状态也会发生改变

   - 运行成功的任务状态会变成绿色
   - 运行失败的任务状态会变成红色
   - 运行中的任务状态会变成黄色
   - 运作中任务组节点的边框也会发生改变

   | ![image-20241219212046125](./doc/img2/main/9.png)  | ![image-20241219212046125](./doc/img2/main/10.png) |
   | -------------------------------------------------- | -------------------------------------------------- |
   | ![image-20241219212046125](./doc/img2/main/11.png) | ![image-20241219212046125](./doc/img2/main/12.png) |

4. 修改任务

   将任务4修改成sql任务

   | ![image-20241219212046125](./doc/img2/main/13.png) | ![image-20241219212046125](./doc/img2/main/14.png) |
   | -------------------------------------------------- | -------------------------------------------------- |
   | ![image-20241219212046125](./doc/img2/main/15.png) | ![image-20241219212046125](./doc/img2/main/16.png) |

5. 查看日志

   也可以在任务日志中查看当前运行的日志信息

   | ![image-20241219212046125](./doc/img2/main/17.png) | ![image-20241219212046125](./doc/img2/main/18.png) |
   | -------------------------------------------------- | -------------------------------------------------- |

   


## 项目文档
http://175.178.249.190/blog/post/298

## 本地文档
`/doc/cc-job`

[01项目介绍.md](doc/cc-job/01%E9%A1%B9%E7%9B%AE%E4%BB%8B%E7%BB%8D.md)
![image-20241219212046125](./doc/cc-job/images/01-ccjob.png)

[02快速开始.md](doc/cc-job/02%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B.md)
![image-20241219212046125](./doc/cc-job/images/02-ccjob.png)

[03功能介绍.md](doc/cc-job/03%E5%8A%9F%E8%83%BD%E4%BB%8B%E7%BB%8D.md)
![image-20241219212046125](./doc/cc-job/images/03-ccjob.png)
### 修改配置
与xxl-job后端的配置一样，只不过`admin.addresses`地址要换成`8989`，并且执行器端口不能为`9999`
路径地址也需要进行配置

示例配置（这是xxl-job-executor-sample-springboot的配置）
```yaml
### xxl-job admin address list, such as "http://address" or "http://address01,http://address02"
xxl.job.admin.addresses=http://127.0.0.1:8989/xxl-job-admin

### xxl-job, access token
xxl.job.accessToken=default_token

### xxl-job executor appname
xxl.job.executor.appname=xxl-job-executor-sample
### xxl-job executor registry-address: default use address to registry , otherwise use ip:port if address is null
xxl.job.executor.address=
### xxl-job executor server-info
xxl.job.executor.ip=
xxl.job.executor.port=10000
### xxl-job executor log-path
xxl.job.executor.logpath= #路径地址
### xxl-job executor log-retention-days
xxl.job.executor.logretentiondays=30
```

admin模块配置 !!! 需要配置日志路径，最好与executor的路径一致
```yaml
# xxl-job 定时任务配置
xxl:
  job:
    i18n: zh_CN
    accessToken: default_token
    triggerpool:
      fast:
        max: 200
      slow:
        max: 200
    logretentiondays: 30
    logpath:  #日志路径
```
## 项目模块

![image-20241219212046125](./doc/img/01img/image-20241219212046125.png)

- cc-async-tool 异步任务调度工具，通过这个工具可以实现任务的重复调用，超时策略  **gitee地址**：https://gitee.com/xzjsccz/async-task-tool
- cc-job-admin 任务注册中心，所有的任务和任务组都在当前注册中心进行注册
- cc-job-core 任务执行的核心代码块
- c c-job-executor: 任务执行器，包含datax任务，api任务，jdbc任务
- cc-job-executor-sample：测试样例任务执行器
- cc-job-xo：存储mapper，entity类
- vue3-cc-job-admin：前端

## 简单演示

### datax数据同步任务

简单演示mysql-mysql全量同步功能

首先创建2个mysql数据源**test1**和**test2**，其中**test2**中的表**stu**无数据，现在演示**test1**数据库的**stu**表数据全量同步到**test2**中的**stu**表中

<img src="./doc/img/01img/02.png" alt="02" style="zoom:50%;" />

在cc-job中创建好datax任务，项目启动流程和任务构建在**第2章**，这里只是做简单演示

- 创建reader

<img src="./doc/img/01img/03.png" alt="image-20241221222732953" style="zoom: 25%;" />

- 创建writer

  <img src="./doc/img/01img/04.png" alt="image-20241221222945799" style="zoom:25%;" />

- 数据源同步配置

  <img src="./doc/img/01img/05.png" alt="image-20241221223200946" style="zoom: 20%;" />

- 在任务列表点击执行，观察最终日志和运行结果

  <img src="./doc/img/01img/06.png" alt="image-20241221223328191" style="zoom:20%;" />

<img src="./doc/img/01img/07.png" alt="image-20241221223559400" style="zoom:20%;" />

- 观察数据库中的数据是否同步成功

  <img src="./doc/img/01img/08.png" alt="image-20241221223750176" style="zoom:50%;" />




## 项目截图

|                                 |                                 |
|:-------------------------------:|:-------------------------------:|
| ![image text](./doc/img/11.png) | ![image text](./doc/img/12.png) |
| ![image text](./doc/img/13.png) | ![image text](./doc/img/14.png) |
| ![image text](./doc/img/15.png) | ![image text](./doc/img/16.png) |
| ![image text](./doc/img/17.png) | ![image text](./doc/img/18.png) |

