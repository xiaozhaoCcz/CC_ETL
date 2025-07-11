// 节点样式配置文件
export const NODE_STYLES = {
    // 基础节点样式
    base: {
        fill: "#ffffff",
        stroke: "#e2e8f0",
        strokeWidth: 2,
        radius: 12,
        shadow: "drop-shadow(0 4px 6px rgba(0, 0, 0, 0.1))",
        hoverShadow: "drop-shadow(0 8px 15px rgba(0, 0, 0, 0.15))",
    },

    // 按钮组样式
    buttonGroup: {
        background: "linear-gradient(180deg, rgba(255,255,255,0.95) 0%, rgba(248,250,252,0.95) 100%)",
        border: "#e2e8f0",
        borderWidth: 1,
        radius: 8,
        shadow: "drop-shadow(0 1px 3px rgba(0, 0, 0, 0.1))",
        accentLine: "linear-gradient(90deg, #3b82f6 0%, #8b5cf6 50%, #06b6d4 100%)",
    },

    // 按钮样式
    button: {
        background: "rgba(255, 255, 255, 0.8)",
        border: "rgba(226, 232, 240, 0.8)",
        borderWidth: 1,
        radius: 3,
        hoverBackground: "rgba(255, 255, 255, 0.95)",
        hoverBorder: "rgba(59, 130, 246, 0.3)",
        size: 16,
    },

    // 图标颜色配置
    iconColors: {
        copy: {
            normal: "#64748b",
            hover: "#3b82f6",
        },
        "task-edit": {
            normal: "#64748b",
            hover: "#8b5cf6",
        },
        edit: {
            normal: "#64748b",
            hover: "#06b6d4",
        },
        delete: {
            normal: "#ef4444",
            hover: "#dc2626",
        },
        prop: {
            normal: "#64748b",
            hover: "#10b981",
        },
        "toggle-status": {
            play: {
                normal: "#10b981",
                hover: "#059669",
            },
            pause: {
                normal: "#f59e0b",
                hover: "#d97706",
            },
        },
    },

    // 提示框样式
    tooltip: {
        background: "rgba(15, 23, 42, 0.95)",
        textColor: "#ffffff",
        fontSize: "11px",
        fontWeight: "500",
        radius: 6,
        shadow: "drop-shadow(0 4px 6px rgba(0, 0, 0, 0.15))",
        fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
    },

    // 节点类型特定样式 - 优化后的专业配色
    nodeTypes: {
        java: {
            fill: "#ffffff",
            stroke: "#e74c3c",
            accentColor: "#c0392b",
            iconColor: "#e74c3c",
            gradient: "linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)",
        },
        python: {
            fill: "#ffffff",
            stroke: "#3776ab",
            accentColor: "#ffd43b",
            iconColor: "#3776ab",
            gradient: "linear-gradient(135deg, #3776ab 0%, #2c5aa0 100%)",
        },
        shell: {
            fill: "#ffffff",
            stroke: "#4a5568",
            accentColor: "#68d391",
            iconColor: "#4a5568",
            gradient: "linear-gradient(135deg, #4a5568 0%, #2d3748 100%)",
        },
        php: {
            fill: "#ffffff",
            stroke: "#777bb4",
            accentColor: "#8993be",
            iconColor: "#777bb4",
            gradient: "linear-gradient(135deg, #777bb4 0%, #5a5f9e 100%)",
        },
        nodejs: {
            fill: "#ffffff",
            stroke: "#339933",
            accentColor: "#68d391",
            iconColor: "#339933",
            gradient: "linear-gradient(135deg, #339933 0%, #2d5a2d 100%)",
        },
        api: {
            fill: "#ffffff",
            stroke: "#ff6b35",
            accentColor: "#ff6b35",
            iconColor: "#ff6b35",
            gradient: "linear-gradient(135deg, #ff6b35 0%, #e55a2b 100%)",
        },
        bean: {
            fill: "#ffffff",
            stroke: "#8b5cf6",
            accentColor: "#a78bfa",
            iconColor: "#8b5cf6",
            gradient: "linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)",
        },
        sql: {
            fill: "#ffffff",
            stroke: "#00758f",
            accentColor: "#00758f",
            iconColor: "#00758f",
            gradient: "linear-gradient(135deg, #00758f 0%, #005a6b 100%)",
        },
        powershell: {
            fill: "#ffffff",
            stroke: "#012456",
            accentColor: "#012456",
            iconColor: "#012456",
            gradient: "linear-gradient(135deg, #012456 0%, #001a3a 100%)",
        },
    },

    // 文本样式
    text: {
        fontSize: 12,
        fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
        fontWeight: "500",
        color: "#1f2937",
    },

    // 状态样式
    status: {
        running: {
            fill: "#10b981",
            stroke: "#059669",
        },
        paused: {
            fill: "#f59e0b",
            stroke: "#d97706",
        },
        error: {
            fill: "#ef4444",
            stroke: "#dc2626",
        },
        success: {
            fill: "#10b981",
            stroke: "#059669",
        },
    },

    // 图标配置
    icon: {
        size: 24,
        padding: 8,
        strokeWidth: 2,
        radius: 4,
    },
};

// 定义图标颜色类型
interface IconColor {
    normal: string;
    hover: string;
}

// 获取节点类型样式
export function getNodeTypeStyle(nodeType: string) {
    return NODE_STYLES.nodeTypes[nodeType as keyof typeof NODE_STYLES.nodeTypes] || NODE_STYLES.base;
}

// 获取图标颜色
export function getIconColor(action: string, isPause?: boolean): IconColor {
    if (action === "toggle-status") {
        const status = isPause ? "play" : "pause";
        return NODE_STYLES.iconColors["toggle-status"][status as keyof typeof NODE_STYLES.iconColors["toggle-status"]];
    }
    return NODE_STYLES.iconColors[action as keyof typeof NODE_STYLES.iconColors] as IconColor;
} 