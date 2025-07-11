/**
 * 专业化的颜色常量定义
 * 用于统一管理节点状态、高亮、边框等颜色
 */

// 节点状态颜色
export const NODE_STATUS_COLORS = {
    FAILED: "#DC2626",      // 失败状态 - 专业红色
    SUCCESS: "#059669",      // 成功状态 - 专业绿色
    RUNNING: "#EA580C",      // 运行中状态 - 专业橙色
    WAITING: "#2563EB",      // 等待状态 - 专业蓝色
    PAUSED: "#6B7280",       // 暂停状态 - 专业灰色
    DEFAULT: "#374151",      // 默认状态 - 深灰色
} as const;

// 高亮颜色
export const HIGHLIGHT_COLORS = {
    PRIMARY: "#3B82F6",      // 主要高亮色 - 专业蓝色
    SECONDARY: "#6366F1",    // 次要高亮色 - 靛蓝色
} as const;

// 背景颜色
export const BACKGROUND_COLORS = {
    NORMAL: "#F9FAFB",       // 正常背景 - 浅灰色
    PAUSED: "#F3F4F6",       // 暂停背景 - 更浅的灰色
    SELECTED: "#EFF6FF",     // 选中背景 - 浅蓝色
} as const;

// 边框颜色
export const BORDER_COLORS = {
    NORMAL: "#374151",       // 正常边框 - 深灰色
    ACTIVE: "#3B82F6",       // 激活边框 - 蓝色
    DISABLED: "#9CA3AF",     // 禁用边框 - 中灰色
} as const;

// 文本颜色
export const TEXT_COLORS = {
    PRIMARY: "#111827",      // 主要文本 - 深色
    SECONDARY: "#6B7280",    // 次要文本 - 中灰色
    ACCENT: "#3B82F6",       // 强调文本 - 蓝色
    INVERSE: "#FFFFFF",      // 反色文本 - 白色
} as const;

// 状态颜色映射函数
export const getNodeStatusColor = (status: number): string => {
    switch (status) {
        case 0: return NODE_STATUS_COLORS.FAILED;
        case 1: return NODE_STATUS_COLORS.SUCCESS;
        case 2: return NODE_STATUS_COLORS.RUNNING;
        case 3: return NODE_STATUS_COLORS.WAITING;
        case 4: return NODE_STATUS_COLORS.PAUSED;
        default: return NODE_STATUS_COLORS.DEFAULT;
    }
};

// 颜色主题配置
export const COLOR_THEME = {
    nodeStatus: NODE_STATUS_COLORS,
    highlight: HIGHLIGHT_COLORS,
    background: BACKGROUND_COLORS,
    border: BORDER_COLORS,
    text: TEXT_COLORS,
} as const; 