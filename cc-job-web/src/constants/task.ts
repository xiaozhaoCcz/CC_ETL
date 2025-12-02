/**
 * 任务相关常量定义
 * 
 * @author cc-job-team
 */

/**
 * 任务类型
 */
export const TASK_TYPE = {
  /** 普通任务 */
  NORMAL: 0,
  /** DataX任务 */
  DATAX: 1,
  /** 任务组 */
  GROUP: 2,
} as const;

/**
 * 任务状态
 */
export const TASK_STATUS = {
  /** 停止 */
  STOPPED: 0,
  /** 运行中 */
  RUNNING: 1,
} as const;

/**
 * 节点状态
 */
export const NODE_STATUS = {
  /** 失败 */
  FAILED: 0,
  /** 成功 */
  SUCCESS: 1,
  /** 执行中 */
  RUNNING: 2,
  /** 完成 */
  COMPLETED: 5,
  /** 预测时间 */
  PREDICTED: 9,
} as const;

/**
 * 调度类型
 */
export const SCHEDULE_TYPE = {
  /** CRON表达式 */
  CRON: 'CRON',
  /** 固定速率 */
  FIX_RATE: 'FIX_RATE',
  /** 固定延迟 */
  FIX_DELAY: 'FIX_DELAY',
} as const;

/**
 * 路由策略
 */
export const ROUTE_STRATEGY = {
  /** 第一个 */
  FIRST: 'FIRST',
  /** 最后一个 */
  LAST: 'LAST',
  /** 轮询 */
  ROUND: 'ROUND',
  /** 随机 */
  RANDOM: 'RANDOM',
  /** 一致性HASH */
  CONSISTENT_HASH: 'CONSISTENT_HASH',
  /** 最不经常使用 */
  LEAST_FREQUENTLY_USED: 'LEAST_FREQUENTLY_USED',
  /** 最近最久未使用 */
  LEAST_RECENTLY_USED: 'LEAST_RECENTLY_USED',
  /** 故障转移 */
  FAILOVER: 'FAILOVER',
  /** 忙碌转移 */
  BUSYOVER: 'BUSYOVER',
  /** 分片广播 */
  SHARDING_BROADCAST: 'SHARDING_BROADCAST',
} as const;

/**
 * 阻塞处理策略
 */
export const BLOCK_STRATEGY = {
  /** 单机串行 */
  SERIAL_EXECUTION: 'SERIAL_EXECUTION',
  /** 丢弃后续调度 */
  DISCARD_LATER: 'DISCARD_LATER',
  /** 覆盖之前调度 */
  COVER_EARLY: 'COVER_EARLY',
  /** 失败时忽略 */
  DO_NOTHING: 'DO_NOTHING',
} as const;

/**
 * GLUE 类型
 */
export const GLUE_TYPE = {
  /** BEAN模式 */
  BEAN: 'BEAN',
  /** SHELL脚本 */
  GLUE_SHELL: 'GLUE_SHELL',
  /** PYTHON脚本 */
  GLUE_PYTHON: 'GLUE_PYTHON',
  /** PHP脚本 */
  GLUE_PHP: 'GLUE_PHP',
  /** NODEJS脚本 */
  GLUE_NODEJS: 'GLUE_NODEJS',
  /** POWERSHELL脚本 */
  GLUE_POWERSHELL: 'GLUE_POWERSHELL',
  /** GROOVY脚本 */
  GLUE_GROOVY: 'GLUE_GROOVY',
  /** HTTP任务 */
  HTTP: 'HTTP',
} as const;

/**
 * 任务类型选项列表
 */
export const TASK_TYPE_OPTIONS = [
  { label: '普通任务', value: TASK_TYPE.NORMAL },
  { label: 'DataX任务', value: TASK_TYPE.DATAX },
  { label: '任务组', value: TASK_TYPE.GROUP },
];

/**
 * 调度类型选项列表
 */
export const SCHEDULE_TYPE_OPTIONS = [
  { type: SCHEDULE_TYPE.CRON, title: 'CRON' },
  { type: SCHEDULE_TYPE.FIX_RATE, title: '固定速率' },
  { type: SCHEDULE_TYPE.FIX_DELAY, title: '固定延迟' },
];

/**
 * 路由策略选项列表
 */
export const ROUTE_STRATEGY_OPTIONS = [
  { strategy: ROUTE_STRATEGY.FIRST, title: '第一个' },
  { strategy: ROUTE_STRATEGY.LAST, title: '最后一个' },
  { strategy: ROUTE_STRATEGY.ROUND, title: '轮询' },
  { strategy: ROUTE_STRATEGY.RANDOM, title: '随机' },
  { strategy: ROUTE_STRATEGY.CONSISTENT_HASH, title: '一致性HASH' },
  { strategy: ROUTE_STRATEGY.LEAST_FREQUENTLY_USED, title: '最不经常使用' },
  { strategy: ROUTE_STRATEGY.LEAST_RECENTLY_USED, title: '最近最久未使用' },
  { strategy: ROUTE_STRATEGY.FAILOVER, title: '故障转移' },
  { strategy: ROUTE_STRATEGY.BUSYOVER, title: '忙碌转移' },
  { strategy: ROUTE_STRATEGY.SHARDING_BROADCAST, title: '分片广播' },
];

/**
 * 阻塞处理策略选项列表
 */
export const BLOCK_STRATEGY_OPTIONS = [
  { strategy: BLOCK_STRATEGY.SERIAL_EXECUTION, title: '单机串行' },
  { strategy: BLOCK_STRATEGY.DISCARD_LATER, title: '丢弃后续调度' },
  { strategy: BLOCK_STRATEGY.COVER_EARLY, title: '覆盖之前调度' },
  { strategy: BLOCK_STRATEGY.DO_NOTHING, title: '失败时忽略' },
];

/**
 * GLUE 类型选项列表
 */
export const GLUE_TYPE_OPTIONS = [
  { value: GLUE_TYPE.BEAN, label: 'BEAN模式(类形式)' },
  { value: GLUE_TYPE.GLUE_SHELL, label: 'SHELL(shell脚本)' },
  { value: GLUE_TYPE.GLUE_PYTHON, label: 'PYTHON(python脚本)' },
  { value: GLUE_TYPE.GLUE_PHP, label: 'PHP(php脚本)' },
  { value: GLUE_TYPE.GLUE_NODEJS, label: 'NODEJS(nodejs脚本)' },
  { value: GLUE_TYPE.GLUE_POWERSHELL, label: 'POWERSHELL(powershell脚本)' },
  { value: GLUE_TYPE.GLUE_GROOVY, label: 'GLUE(Java代码)' },
  { value: GLUE_TYPE.HTTP, label: 'HTTP(http请求)' },
];

/**
 * 失败重试次数选项
 */
export const RETRY_COUNT_OPTIONS = Array.from({ length: 11 }, (_, i) => ({
  label: i === 0 ? '不重试' : `${i}次`,
  value: i,
}));

/**
 * 超时时间选项（秒）
 */
export const TIMEOUT_OPTIONS = [
  { label: '不限制', value: 0 },
  { label: '30秒', value: 30 },
  { label: '60秒', value: 60 },
  { label: '180秒', value: 180 },
  { label: '300秒', value: 300 },
  { label: '600秒', value: 600 },
];

