/**
 * LogicFlow 相关工具函数
 */

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
    console.log("generateNode", node, node.nodeType);
    const properties = JSON.parse(node.properties);

    // 类型判断逻辑
    const nodeType = node.nodeType || (node.nodeType === "CustomGroup" ? "CustomGroup" : "rect");

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
    return {
        id: edge.id,
        sourceNodeId: edge.fromNodeId,
        targetNodeId: edge.endNodeId,
        type: "bezier",
    };
}

/**
 * 根据任务状态获取节点颜色
 * @param status 任务状态码
 * @returns 颜色值
 */
export function getNodeColor(status: number): string {
    switch (status) {
        case 0:
            return "#CC0000"; // 红色 - 失败
        case 1:
            return "#66FF99"; // 绿色 - 成功
        case 2:
            return "#FFFF33"; // 黄色 - 运行中
        default:
            return "#000"; // 黑色 - 默认
    }
}

/**
 * 清理LogicFlow画布数据
 * @param lfInstance LogicFlow实例
 */
export async function clearData(lfInstance: any) {
    if (!lfInstance) {
        console.log("没有可用的LogicFlow实例，跳过清理");
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