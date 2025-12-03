# CC-Job 代码优化指南

## 一、代码规范

### 1.1 命名规范

#### 类命名
```java
// ✅ 好的命名
public class JobInfoService { }
public class JobGraphBuilder { }
public class JobExecutionHandler { }

// ❌ 不好的命名
public class TaskInfo { }  // task 不够明确
public class XxlJobHelper { }  // 包含第三方库前缀
public class Utils { }  // 过于宽泛
```

#### 方法命名
```java
// ✅ 好的命名
public void buildGraph(Long jobId) { }
public List<JobNode> getStartNodes() { }
public boolean isRunning() { }

// ❌ 不好的命名
public void doSomething() { }  // 不明确
public void process() { }  // 过于宽泛
public List<JobNode> get() { }  // 不完整
```

#### 常量命名
```java
// ✅ 好的命名
public static final String SUCCESS = "SUCCESS";
public static final int MAX_RETRY_COUNT = 3;
public static final long DEFAULT_TIMEOUT = 5000L;

// ❌ 不好的命名
public static final String s = "SUCCESS";  // 太短
public static final int max = 3;  // 应该全大写
public static final String ADMIN_ADDRESS = "http://%s:%s/xxl-job-admin/";  // 可以，但建议放在 AddressFormat 分组中
```

### 1.2 注释规范

#### 类注释
```java
/**
 * 任务图构建器
 * 
 * 负责构建任务依赖关系图，处理任务节点和边的关系
 *
 * @author cc-job
 * @since 2025-12-02
 */
public class JobGraphBuilder {
}
```

#### 方法注释
```java
/**
 * 构建任务图
 *
 * @param jobId 任务ID
 * @param nodes 节点列表
 * @param edges 边列表
 */
public void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
}
```

#### 行内注释
```java
// ✅ 好的注释
// 计算节点的入度和出度
nodes.forEach(node -> {
    node.setNodeInDegree(calculateInDegree(node));
});

// ❌ 不好的注释
// 循环  （太简单，没有意义）
for (int i = 0; i < 10; i++) { }

// 这是一个方法  （没有说明方法的作用）
public void process() { }
```

### 1.3 日志规范

```java
// ✅ 好的日志
log.info("[JobGroup] 开始执行任务组 - jobId: {}, 执行参数: {}", jobId, executeParam);
log.warn("[JobGroup] 任务未找到 - jobId: {}", jobId);
log.error("[JobGroup] 任务执行异常 - jobId: {}, 异常信息: {}", jobId, e.getMessage(), e);
log.debug("[JobGroup] 构建任务图完成 - jobId: {}, 节点数量: {}, 边数量: {}", jobId, nodes.size(), edges.size());

// ❌ 不好的日志
exception.printStackTrace();  // 直接打印，不够专业
log.info("开始");  // 信息不完整
log.error("错误");  // 没有上下文信息
System.out.println("debug");  // 不要使用 System.out
```

## 二、常量管理

### 2.1 使用新的常量类

```java
// ✅ 推荐使用
import static com.cc.job.admin.common.constant.JobConstants.*;

if (status == NodeStatus.SUCCESS) {
    // ...
}

if (result.equals(ExecutionResult.SUCCESS)) {
    // ...
}

// ❌ 不推荐（旧方式，已废弃）
import static com.cc.job.admin.task.handler.JobConstant.*;

if (result.equals(SUCCESS)) {  // 不够语义化
    // ...
}
```

### 2.2 添加新常量

```java
// 在 JobConstants.java 中
public static final class NodeStatus {
    /** 失败 */
    public static final int FAILED = 0;
    /** 成功 */
    public static final int SUCCESS = 1;
    /** 运行中 */
    public static final int RUNNING = 2;
    
    // 如果需要添加新状态，在这里添加
    /** 暂停 */
    public static final int PAUSED = 3;  // ✅ 新增

    private NodeStatus() {
    }
}
```

### 2.3 常量分组原则

```java
// ✅ 按照业务逻辑分组
public static final class ExecutionResult { }  // 执行结果
public static final class JobType { }  // 任务类型
public static final class NodeStatus { }  // 节点状态

// ❌ 所有常量混在一起
public static final String SUCCESS = "SUCCESS";
public static final int JOB_TYPE_NORMAL = 0;
public static final int NODE_STATUS_FAILED = 0;
```

## 三、异常处理

### 3.1 使用自定义异常

```java
// ✅ 推荐
if (jobInfo == null) {
    throw new JobNotFoundException(jobId);
}

if (executorAddress == null) {
    throw new TriggerException(jobId, "执行器地址为空");
}

// ❌ 不推荐
if (jobInfo == null) {
    throw new RuntimeException("任务不存在");  // 信息不足
}

if (executorAddress == null) {
    throw new BusinessException("错误");  // 过于宽泛
}
```

### 3.2 异常处理最佳实践

```java
// ✅ 好的异常处理
try {
    executeJob(jobId);
} catch (JobNotFoundException e) {
    log.warn("任务未找到 - jobId: {}", e.getJobId());
    // 处理特定异常
} catch (TriggerException e) {
    log.error("触发失败 - jobId: {}, 原因: {}", e.getJobId(), e.getMessage(), e);
    // 处理触发异常
} catch (Exception e) {
    log.error("未知异常 - jobId: {}", jobId, e);
    throw e;
}

// ❌ 不好的异常处理
try {
    executeJob(jobId);
} catch (Exception e) {
    e.printStackTrace();  // 直接打印
    // 吞掉异常，没有处理
}
```

### 3.3 创建新的自定义异常

```java
// 在 common/exception/ 目录下创建
public class ScheduleException extends RuntimeException {
    private final Long scheduleId;

    public ScheduleException(Long scheduleId, String message) {
        super(message);
        this.scheduleId = scheduleId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }
}

// 在 GlobalExceptionHandler 中添加处理器
@ExceptionHandler(ScheduleException.class)
@ResponseBody
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public ReturnT<String> handleScheduleException(ScheduleException exception) {
    log.error("调度异常: scheduleId={}", exception.getScheduleId(), exception);
    return new ReturnT<>(ReturnT.FAIL_CODE, "调度失败: " + exception.getMessage());
}
```

## 四、代码分层

### 4.1 Controller 层

**职责**: 接收请求，参数校验，调用 Service，返回结果

```java
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobInfoController {
    
    private final JobInfoService jobInfoService;
    
    // ✅ 好的 Controller 方法
    @PostMapping
    public Result<Long> createJob(@RequestBody @Valid JobInfoForm form) {
        long jobId = jobInfoService.createJob(form);
        return Result.success(jobId);
    }
    
    // ❌ 不好的 Controller 方法
    @PostMapping
    public Result<Long> createJob(@RequestBody JobInfoForm form) {
        // 在 Controller 中写业务逻辑
        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobDesc(form.getJobDesc());
        // ... 更多业务逻辑
        jobInfoMapper.insert(jobInfo);  // 直接调用 Mapper
        return Result.success(jobInfo.getId());
    }
}
```

**规范**:
- ✅ 使用 `@Valid` 进行参数校验
- ✅ 只调用 Service 方法，不写业务逻辑
- ✅ 使用统一的返回格式 `Result<T>`
- ❌ 不要在 Controller 中直接调用 Mapper
- ❌ 不要在 Controller 中写复杂的业务逻辑

### 4.2 Service 层

**职责**: 业务逻辑处理，事务管理

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class JobInfoServiceImpl implements JobInfoService {
    
    private final JobInfoMapper jobInfoMapper;
    
    // ✅ 好的 Service 方法
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createJob(JobInfoForm form) {
        // 参数验证
        validateJobForm(form);
        
        // 业务逻辑
        JobInfo jobInfo = convertToEntity(form);
        jobInfoMapper.insert(jobInfo);
        
        // 后续处理
        notifyJobCreated(jobInfo.getId());
        
        log.info("创建任务成功 - jobId: {}", jobInfo.getId());
        return jobInfo.getId();
    }
    
    // ❌ 不好的 Service 方法
    @Override
    public long createJob(JobInfoForm form) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setJobDesc(form.getJobDesc());
        // ... 大量的属性设置
        jobInfoMapper.insert(jobInfo);
        return jobInfo.getId();
        // 没有日志，没有验证，没有事务
    }
}
```

**规范**:
- ✅ 使用 `@Transactional` 管理事务
- ✅ 添加必要的日志记录
- ✅ 进行业务验证
- ✅ 方法职责单一，过长方法要拆分
- ❌ 不要在 Service 中处理 HTTP 请求/响应

### 4.3 工具类/处理器

**职责**: 提供可复用的工具方法，或处理特定的业务逻辑

```java
// ✅ 好的工具类
@Component
@RequiredArgsConstructor
@Slf4j
public class JobGraphBuilder {
    
    private final JobInfoService jobInfoService;
    
    /**
     * 构建任务图
     */
    public void buildGraph(Long jobId, List<JobNode> nodes, List<JobEdge> edges) {
        log.debug("开始构建任务图 - jobId: {}", jobId);
        // 专注于图构建逻辑
    }
}

// ❌ 不好的工具类
public class Utils {
    // 各种不相关的方法混在一起
    public static void doSomething() { }
    public static void doOtherThing() { }
    public static void doAnotherThing() { }
}
```

**规范**:
- ✅ 类名明确反映职责（如 `JobGraphBuilder`）
- ✅ 使用 Spring 管理（`@Component`），便于依赖注入
- ✅ 方法职责单一
- ❌ 不要创建万能工具类 `Utils`
- ❌ 静态工具方法要慎重使用

## 五、性能优化

### 5.1 数据库查询优化

```java
// ✅ 批量查询
List<Long> jobIds = Arrays.asList(1L, 2L, 3L);
List<JobInfo> jobs = jobInfoService.listByIds(jobIds);

// ❌ 循环查询
List<JobInfo> jobs = new ArrayList<>();
for (Long jobId : jobIds) {
    jobs.add(jobInfoService.getById(jobId));  // N+1 问题
}
```

### 5.2 缓存使用

```java
// ✅ 使用缓存
@Cacheable(value = "jobInfo", key = "#jobId")
public JobInfo getJobInfo(Long jobId) {
    return jobInfoMapper.selectById(jobId);
}

// 缓存失效
@CacheEvict(value = "jobInfo", key = "#jobId")
public void updateJob(Long jobId, JobInfo jobInfo) {
    jobInfoMapper.updateById(jobInfo);
}
```

### 5.3 线程池使用

```java
// ✅ 使用配置的线程池
@Configuration
public class ThreadPoolConfig {
    
    @Bean("jobExecutor")
    public ThreadPoolExecutor jobExecutor() {
        return new ThreadPoolExecutor(
            10, 
            20, 
            60L, 
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder().setNameFormat("job-executor-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}

// 使用
@Resource(name = "jobExecutor")
private ThreadPoolExecutor jobExecutor;

jobExecutor.execute(() -> {
    // 任务逻辑
});

// ❌ 直接创建线程
new Thread(() -> {
    // 任务逻辑
}).start();  // 没有复用，没有管理
```

## 六、测试规范

### 6.1 单元测试

```java
@SpringBootTest
class JobGraphBuilderTest {
    
    @Autowired
    private JobGraphBuilder graphBuilder;
    
    @Test
    void testBuildGraph() {
        // Given
        Long jobId = 1L;
        List<JobNode> nodes = new ArrayList<>();
        List<JobEdge> edges = new ArrayList<>();
        
        // When
        graphBuilder.buildGraph(jobId, nodes, edges);
        
        // Then
        assertThat(nodes).isNotEmpty();
        assertThat(edges).isNotEmpty();
    }
    
    @Test
    void testGetStartNodes() {
        // Given
        List<JobNode> nodes = createTestNodes();
        
        // When
        List<Long> startNodes = graphBuilder.getStartNodes(nodes);
        
        // Then
        assertThat(startNodes).hasSize(2);
    }
}
```

### 6.2 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobInfoControllerTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCreateJob() {
        // Given
        JobInfoForm form = new JobInfoForm();
        form.setJobDesc("测试任务");
        
        // When
        ResponseEntity<Result> response = restTemplate.postForEntity(
            "/api/v1/jobs", 
            form, 
            Result.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(200);
    }
}
```

## 七、常见问题

### 7.1 大类问题

**问题**: 类太大，超过 500 行

**解决方案**:
1. 按职责拆分成多个类
2. 提取工具方法到独立的工具类
3. 使用设计模式（策略、模板方法等）

**示例**: `JobGroupXxlJob` (1240行) → 拆分为:
- `JobGroupXxlJob` - 任务组执行协调
- `JobGraphBuilder` - 图构建逻辑
- `JobExecutionHandler` - 任务执行处理
- `JobStatusMonitor` - 状态监控

### 7.2 方法过长问题

**问题**: 方法超过 50 行

**解决方案**:
1. 提取子方法
2. 使用 Stream API 简化集合操作
3. 减少嵌套层级

```java
// ❌ 过长的方法
public void processJob() {
    // 100+ 行代码
}

// ✅ 拆分后
public void processJob() {
    validateJob();
    prepareExecution();
    executeJob();
    handleResult();
}

private void validateJob() { }
private void prepareExecution() { }
private void executeJob() { }
private void handleResult() { }
```

### 7.3 循环嵌套问题

**问题**: 过多的嵌套循环

**解决方案**:
1. 使用 Stream API
2. 提取方法降低嵌套
3. 提前 return

```java
// ❌ 嵌套过深
for (JobNode node : nodes) {
    for (JobEdge edge : edges) {
        if (edge.getFromNodeId().equals(node.getId())) {
            for (JobNode targetNode : nodes) {
                if (targetNode.getId().equals(edge.getEndNodeId())) {
                    // 处理逻辑
                }
            }
        }
    }
}

// ✅ 优化后
Map<Long, List<JobEdge>> edgeMap = edges.stream()
    .collect(Collectors.groupingBy(JobEdge::getFromNodeId));

nodes.forEach(node -> {
    List<JobEdge> nodeEdges = edgeMap.get(node.getId());
    if (nodeEdges != null) {
        processNodeEdges(node, nodeEdges, nodes);
    }
});
```

## 八、检查清单

提交代码前，请检查:

- [ ] 类名、方法名、变量名符合命名规范
- [ ] 添加了必要的注释（类注释、方法注释）
- [ ] 使用新的常量管理方式（JobConstants/SystemConstants）
- [ ] 使用自定义异常而不是通用异常
- [ ] 添加了适当的日志记录
- [ ] 方法长度合理（< 50行）
- [ ] 类长度合理（< 500行）
- [ ] 没有重复代码
- [ ] 没有注释掉的代码
- [ ] 处理了所有 TODO 标记
- [ ] 编写了单元测试
- [ ] 代码通过了 Lint 检查

---

**文档版本**: v1.0  
**最后更新**: 2025-12-02  
**维护者**: CC-Job Team

