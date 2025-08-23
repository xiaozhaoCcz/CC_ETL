/**
 * LogicFlow 相关工具函数
 */

import { usePageStoreHook } from "@/store";

/**
 * 计算数组平均值
 * @param array 数字数组
 * @returns 平均值
 */
export function avg(array: number[]): number {
    if (!array || array.length === 0) return 0;
    return array.reduce((sum, val) => sum + val, 0) / array.length;
}

/**
 * 根据节点数据生成LogicFlow节点对象
 * @param node 节点数据
 * @returns LogicFlow节点对象
 */
export function generateNode(node: any) {
    const properties = JSON.parse(node.properties);

    // 类型判断逻辑
    const nodeType =
        node.nodeType || (node.nodeType === "CustomGroup" ? "CustomGroup" : "rect");

    if (node.nodeType === "CustomGroup") {
        properties.children = JSON.parse(properties.children);
    }

    return {
        id: node.id,
        text: node.jobName,
        type: nodeType,
        x: node.nodePositionX,
        y: node.nodePositionY,
        properties: properties,
        children: node.children != null ? JSON.parse(node.children) : node.children,
    };
}

/**
 * 根据边数据生成LogicFlow边对象
 * @param edge 边数据
 * @returns LogicFlow边对象
 */
export function generateEdge(edge: any) {
    // 验证输入参数
    if (!edge || typeof edge !== 'object') {
        console.warn('generateEdge: 无效的边数据', edge);
        return null;
    }

    // 确保边数据包含必要的锚点信息
    const edgeData: any = {
        id: edge.id || `edge_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        sourceNodeId: edge.fromNodeId || edge.sourceNodeId,
        targetNodeId: edge.endNodeId || edge.targetNodeId,
        type: edge.type || "bezier",
    };

    // 验证必要的属性
    if (!edgeData.sourceNodeId || !edgeData.targetNodeId) {
        console.warn('generateEdge: 边数据缺少必要的节点ID', edge);
        return null;
    }

    // 如果原始边数据包含锚点信息，则保留
    if (edge.anchors && Array.isArray(edge.anchors)) {
        edgeData.anchors = edge.anchors;
    }

    // 如果原始边数据包含起点和终点锚点，则保留
    if (edge.startPoint && typeof edge.startPoint === 'object') {
        edgeData.startPoint = edge.startPoint;
    }
    if (edge.endPoint && typeof edge.endPoint === 'object') {
        edgeData.endPoint = edge.endPoint;
    }

    // 如果原始边数据包含控制点，则保留（对于贝塞尔曲线）
    if (edge.controlPoints && Array.isArray(edge.controlPoints)) {
        edgeData.controlPoints = edge.controlPoints;
    }

    // 添加默认的锚点配置（如果完全没有锚点信息）
    if (!edgeData.anchors && !edgeData.startPoint && !edgeData.endPoint) {
        // 为贝塞尔曲线添加默认锚点
        if (edgeData.type === 'bezier') {
            edgeData.anchors = [
                { x: 0, y: 0, id: 'start' },
                { x: 0, y: 0, id: 'end' }
            ];
        }
    }

    return edgeData;
}

/**
 * 清理LogicFlow画布数据
 * @param lfInstance LogicFlow实例
 */
export async function clearData(lfInstance: any) {
    if (!lfInstance) {
        return;
    }

    try {
        const graphData = lfInstance.getGraphRawData();
        if (!graphData) return;

        const nodes = graphData.nodes || [];
        const edges = graphData.edges || [];
        const graphModel = lfInstance.graphModel;

        // 清理节点和边
        nodes.forEach((node: any) => {
            const _node = graphModel.getNodeModelById(node.id);
            if (_node) {
                graphModel.deleteNode(_node.id);
            }
        });

        edges.forEach((edge: any) => {
            const _edge = graphModel.getEdgeModelById(edge.id);
            if (_edge) {
                graphModel.deleteEdgeById(_edge.id);
            }
        });
    } catch (err) {
        console.error("清理画布时出错:", err);
    }
}

/**
 * 转换日志内容，处理HTML实体
 * @param str 原始字符串
 * @returns 转换后的字符串
 */
export function convertContent(str: string): string {
    return str
        .replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, "'")
        .replace(/&#39;/g, "'")
        .replace(/&quot;/g, '"');
}

/**
 * 选择元素
 * 获取当前选中的元素
 */
export function selectElements(lfInstances: any, lf: any): void {
    const currentPageId = usePageStoreHook().getCurrentPage();
    const currentLf = lfInstances.value[currentPageId] || lf.value;
    const elements = currentLf.graphModel.getSelectElements(true);
}

// ================== 新增工具函数 ==================

/**
 * 节点类型映射表
 */
export const GLUE_NODE_TYPE_MAP: Record<string, string> = {
    SQL: "custom-sql",
    API: "custom-api",
    BEAN: "custom-bean",
    GLUE_GROOVY: "custom-java",
    GLUE_SHELL: "custom-shell",
    GLUE_PYTHON: "custom-python",
    GLUE_PHP: "custom-php",
    GLUE_NODEJS: "custom-nodejs",
    GLUE_POWERSHELL: "custom-powershell",
};

/**
 * 节点状态颜色映射
 */
export const NODE_STATUS_COLORS = {
    0: "#CC0000", // 红色 - 失败
    1: "#66FF99", // 绿色 - 成功
    2: "#FFFF33", // 黄色 - 运行中
    default: "#000", // 黑色 - 默认
};

/**
 * 平台高度限制
 */
export const PLATFORM_HEIGHT_LIMITS = {
    MIN: 30,
    MAX: 80,
    DEFAULT: 60,
};

/**
 * WebSocket重连配置
 */
export const WEBSOCKET_CONFIG = {
    MAX_RECONNECT_ATTEMPTS: 3,
    RECONNECT_DELAY: 3000,
};

/**
 * 动态任务组类型标识
 */
export const DYNAMIC_CUSTOM_GROUP = "CustomGroup";

/**
 * 清除节点高亮状态
 * @param highlightedElement 高亮的元素
 * @param highlightedType 高亮类型
 * @param originalStyle 原始样式
 */
export function clearHighlight(
    highlightedElement: any,
    highlightedType: string | null,
    originalStyle: any
): void {
    if (highlightedElement) {
        // 恢复原始样式
        if (highlightedType === "node") {
            highlightedElement.setStyle("fill", originalStyle?.fill || "#fff");
        } else if (highlightedType === "edge") {
            highlightedElement.setStyle("stroke", originalStyle?.stroke || "#333");
        }
        document.removeEventListener(
            "mousedown",
            () => clearHighlight(highlightedElement, highlightedType, originalStyle),
            true
        );
    }
}

/**
 * 更新指定任务组的边样式
 * @param taskGroupId 任务组ID
 * @param targetLf 目标LogicFlow实例
 * @param getPageRunStatus 获取页面运行状态的函数
 */
export function updateEdgeStyleForTaskGroup(
    taskGroupId: number,
    targetLf: any,
    getPageRunStatus: (id: number) => boolean
): void {
    if (!targetLf) {
        console.warn(`无法找到任务组 ${taskGroupId} 对应的LogicFlow实例`);
        return;
    }

    const { edges } = targetLf.getGraphRawData() ?? {};
    const isRunning = getPageRunStatus(taskGroupId);

    console.log(`更新任务组 ${taskGroupId} 的边样式，运行状态: ${isRunning}`);

    if (isRunning) {
        edges?.forEach(({ id }: any) => {
            targetLf.openEdgeAnimation(id);
        });
    } else {
        edges?.forEach(({ id }: any) => {
            targetLf.closeEdgeAnimation(id);
        });
    }
}

/**
 * 节点自动布局方法
 * @param direction 布局方向：horizontal(水平) 或 vertical(垂直)
 * @param currentLf 当前LogicFlow实例
 */
export function layoutNodes(direction: "horizontal" | "vertical", currentLf: any): void {
    if (!currentLf) {
        console.warn("请先选择一个任务组");
        return;
    }

    const elements = currentLf.graphModel.getSelectElements(true);
    const nodes = elements.nodes;
    if (!nodes || nodes.length === 0) return;

    // 获取所有与选中节点相关的边
    const allEdges = currentLf.getGraphRawData()?.edges || [];
    const selectedNodeIds = nodes.map((node: any) => node.id);

    // 保存与选中节点相关的边的配置
    const relatedEdges = allEdges.filter((edge: any) =>
        selectedNodeIds.includes(edge.sourceNodeId) || selectedNodeIds.includes(edge.targetNodeId)
    );

    // 删除与选中节点相关的边
    relatedEdges.forEach((edge: any) => {
        const edgeModel = currentLf.graphModel.getEdgeModelById(edge.id);
        if (edgeModel) {
            currentLf.graphModel.deleteEdgeById(edge.id);
        }
    });

    if (direction === "horizontal") {
        const arrY = nodes.map((n: any) => n.y);
        const avgY = avg(arrY);
        nodes.forEach((node: any) => {
            const _node = currentLf.getNodeModelById(node.id);
            _node.moveTo(_node.x, avgY);
        });
    } else {
        const arrX = nodes.map((n: any) => n.x);
        const avgX = avg(arrX);
        nodes.forEach((node: any) => {
            const _node = currentLf.getNodeModelById(node.id);
            _node.moveTo(avgX, _node.y);
        });
    }

    // 重新添加之前删除的边
    relatedEdges.forEach((edge: any) => {
        currentLf.addEdge({
            id: edge.id,
            sourceNodeId: edge.sourceNodeId,
            targetNodeId: edge.targetNodeId,
            type: edge.type || "bezier"
        });
    });
}

/**
 * 清除画布
 * @param currentLf 当前LogicFlow实例
 */
export function clearCanvas(currentLf: any): void {
    if (!currentLf) {
        return;
    }

    try {
        // 获取当前画布的所有数据
        const graphData = currentLf.getGraphRawData();
        if (!graphData) {
            return;
        }

        const nodes = graphData.nodes || [];
        const edges = graphData.edges || [];
        const graphModel = currentLf.graphModel;

        // 清除所有边
        edges.forEach((edge: any) => {
            const edgeModel = graphModel.getEdgeModelById(edge.id);
            if (edgeModel) {
                graphModel.deleteEdgeById(edge.id);
            }
        });

        // 清除所有节点
        nodes.forEach((node: any) => {
            const nodeModel = graphModel.getNodeModelById(node.id);
            if (nodeModel) {
                graphModel.deleteNode(node.id);
            }
        });
    } catch (err: any) {
        throw new Error("清除画布失败");
    }
}

/**
 * 选择节点
 * @param currentLf 当前LogicFlow实例
 */
export function selectNodes(currentLf: any): void {
    if (!currentLf) {
        return;
    }
    currentLf.extension.selectionSelect.openSelectionSelect();
    currentLf.once("selection:selected", () => {
        currentLf.extension.selectionSelect.closeSelectionSelect();
    });
}

/**
 * 根据任务状态获取节点颜色
 * @param status 任务状态码
 * @returns 颜色值
 */
export function getNodeColor(status: number): string {
    return (
        NODE_STATUS_COLORS[status as keyof typeof NODE_STATUS_COLORS] ||
        NODE_STATUS_COLORS.default
    );
}

/**
 * 画布操作处理函数
 */
export const canvasOperations = {
    /**
     * 撤销操作
     */
    undo: (currentLf: any) => {
        if (!currentLf) {
            return;
        }
        currentLf.undo();
    },

    /**
     * 重做操作
     */
    redo: (currentLf: any) => {
        if (!currentLf) {
            return;
        }
        currentLf.redo();
    },

    /**
     * 适应画布
     */
    fit: (currentLf: any) => {
        if (!currentLf) {
            return;
        }
        const { transformModel } = currentLf.graphModel;
        transformModel.resetZoom();
    },

    /**
     * 放大画布
     */
    zoomIn: (currentLf: any) => {
        if (!currentLf) {
            return;
        }
        const { transformModel } = currentLf.graphModel;
        transformModel.zoom(true);
    },

    /**
     * 缩小画布
     */
    zoomOut: (currentLf: any) => {
        if (!currentLf) {
            return;
        }
        const { transformModel } = currentLf.graphModel;
        transformModel.zoom(false);
    },
};

/**
 * 安全地创建LogicFlow边，包含完整的错误处理
 * @param lfInstance LogicFlow实例
 * @param edgeData 边数据
 * @returns 是否创建成功
 */
export function safeAddEdge(lfInstance: any, edgeData: any): boolean {
    if (!lfInstance || !edgeData) {
        console.warn('safeAddEdge: 缺少必要参数', { lfInstance, edgeData });
        return false;
    }

    try {
        // 验证边数据的基本结构
        if (!edgeData.sourceNodeId || !edgeData.targetNodeId) {
            console.warn('safeAddEdge: 边数据缺少必要的节点ID', edgeData);
            return false;
        }

        // 检查源节点和目标节点是否存在
        const sourceNode = lfInstance.graphModel.getNodeModelById(edgeData.sourceNodeId);
        const targetNode = lfInstance.graphModel.getNodeModelById(edgeData.targetNodeId);
        
        if (!sourceNode || !targetNode) {
            console.warn('safeAddEdge: 源节点或目标节点不存在', {
                sourceNodeId: edgeData.sourceNodeId,
                targetNodeId: edgeData.targetNodeId,
                sourceExists: !!sourceNode,
                targetExists: !!targetNode
            });
            return false;
        }

        // 构建完整的边数据
        const completeEdgeData = {
            id: edgeData.id || `edge_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            sourceNodeId: edgeData.sourceNodeId,
            targetNodeId: edgeData.targetNodeId,
            type: edgeData.type || "bezier",
            // 确保包含必要的锚点信息
            anchors: edgeData.anchors || [
                { x: 0, y: 0, id: 'start' },
                { x: 0, y: 0, id: 'end' }
            ]
        };

        // 添加边到画布
        lfInstance.addEdge(completeEdgeData);
        return true;
    } catch (error) {
        console.error('safeAddEdge: 创建边失败', error, edgeData);
        return false;
    }
}
