/**
 * UI 相关常量定义
 * 
 * @author cc-job-team
 */

/**
 * 表单布局常量
 */
export const FORM_LAYOUT = {
  /** 表单标签宽度 */
  LABEL_WIDTH: '120px',
  /** 输入框宽度 - 小 */
  INPUT_WIDTH_SM: '180px',
  /** 输入框宽度 - 中 */
  INPUT_WIDTH_MD: '240px',
  /** 输入框宽度 - 大 */
  INPUT_WIDTH_LG: '360px',
  /** 输入框宽度 - 全宽 */
  INPUT_WIDTH_FULL: '100%',
} as const;

/**
 * 对话框尺寸
 */
export const DIALOG_SIZE = {
  /** 小对话框 */
  SMALL: '600px',
  /** 中等对话框 */
  MEDIUM: '800px',
  /** 大对话框 */
  LARGE: '1080px',
  /** 超大对话框 */
  XLARGE: '1400px',
} as const;

/**
 * 按钮尺寸
 */
export const BUTTON_SIZE = {
  /** 小按钮 */
  SMALL: 'small',
  /** 默认按钮 */
  DEFAULT: 'default',
  /** 大按钮 */
  LARGE: 'large',
} as const;

/**
 * 表格配置
 */
export const TABLE_CONFIG = {
  /** 默认每页数量 */
  DEFAULT_PAGE_SIZE: 10,
  /** 每页数量选项 */
  PAGE_SIZES: [10, 20, 50, 100],
  /** 默认排序字段 */
  DEFAULT_SORT: 'id',
  /** 默认排序方向 */
  DEFAULT_ORDER: 'descending',
} as const;

/**
 * 画布配置
 */
export const CANVAS_CONFIG = {
  /** 默认节点宽度 */
  NODE_WIDTH: 150,
  /** 默认节点高度 */
  NODE_HEIGHT: 50,
  /** 节点间距 */
  NODE_SPACING: 100,
  /** 网格大小 */
  GRID_SIZE: 20,
  /** 画布背景色 */
  BACKGROUND_COLOR: '#f5f5f5',
} as const;

/**
 * 颜色主题
 */
export const THEME_COLORS = {
  /** 主色 */
  PRIMARY: '#409EFF',
  /** 成功色 */
  SUCCESS: '#67C23A',
  /** 警告色 */
  WARNING: '#E6A23C',
  /** 危险色 */
  DANGER: '#F56C6C',
  /** 信息色 */
  INFO: '#909399',
} as const;

/**
 * 节点状态颜色映射
 */
export const NODE_STATUS_COLORS = {
  0: '#F56C6C',  // 失败 - 红色
  1: '#67C23A',  // 成功 - 绿色
  2: '#409EFF',  // 执行中 - 蓝色
  5: '#909399',  // 完成 - 灰色
  9: '#E6A23C',  // 预测 - 橙色
} as const;

/**
 * 动画时长（毫秒）
 */
export const ANIMATION_DURATION = {
  /** 快速 */
  FAST: 200,
  /** 正常 */
  NORMAL: 300,
  /** 慢速 */
  SLOW: 500,
} as const;

/**
 * Z-Index 层级
 */
export const Z_INDEX = {
  /** 基础层 */
  BASE: 1,
  /** 对话框 */
  DIALOG: 1000,
  /** 消息提示 */
  MESSAGE: 2000,
  /** 加载遮罩 */
  LOADING: 3000,
} as const;

