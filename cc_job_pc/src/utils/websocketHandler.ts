/**
 * WebSocket消息处理器
 * 专门处理任务状态更新、节点颜色变化等业务逻辑
 */

import { WebSocketMessage } from "./websocket";
import { getNodeColor, updateEdgeStyleForTaskGroup } from "./logicflow";
import { DYNAMIC_CUSTOM_GROUP } from "./logicflow";

/**
 * 节点状态更新接口
 */
export interface NodeStatusUpdate {
    jobId: number;
    randomId: string;
    status: number;
    taskGroupId?: number;
    taskGroupLf?: any;
}

/**
 * 任务组完成检查结果
 */
export interface TaskGroupCompletionResult {
    isCompleted: boolean;
    taskGroupId: number | null;
    runningNodesCount: number;
}

/**
 * WebSocket消息处理器类
 */
export class WebSocketMessageHandler {
    private lfInstances: Record<number, any>;
    private usePageStoreHook: any;
    private logTabs: any;
    private runTime: any;
    private jobId: any;

    constructor(
        lfInstances: Record<number, any>,
        usePageStoreHook: any,
        logTabs: any,
        runTime: any,
        jobId: any
    ) {
        this.lfInstances = lfInstances;
        this.usePageStoreHook = usePageStoreHook;
        this.logTabs = logTabs;
        this.runTime = runTime;
        this.jobId = jobId;
    }

    /**
     * 处理WebSocket消息
     */
    public handleMessage(message: WebSocketMessage): void {
        const { jobId: messageJobId, randomId: messageRandomId, status, result } = message;

        console.log(
            `处理WebSocket消息 - jobId: ${messageJobId}, randomId: ${messageRandomId}, status: ${status}`
        );

        // 处理不同类型的消息
        switch (status) {
            case 5:
                this.handleTaskCompletion(messageJobId, messageRandomId);
                break;
            case 9:
                this.handleRuntimeInfo(messageJobId, result);
                break;
            default:
                this.handleNodeStatusUpdate(messageJobId, messageRandomId, status);
                break;
        }
    }

    /**
     * 处理任务完成消息
     */
    private handleTaskCompletion(jobId: number, randomId: string): void {
        const completionResult = this.findTaskGroupByNode(jobId, randomId);

        if (completionResult.taskGroupId && completionResult.taskGroupLf) {
            // 延迟检查任务组是否完全完成
            setTimeout(() => {
                const checkResult = this.checkTaskGroupCompletion(completionResult.taskGroupId!, completionResult.taskGroupLf!);

                if (checkResult.isCompleted) {
                    this.handleTaskGroupCompletion(checkResult.taskGroupId!);
                }
            }, 1000);
        }
    }

    /**
     * 处理运行时信息
     */
    private handleRuntimeInfo(jobId: number, result?: string): void {
        if (jobId === this.jobId.value && result) {
            try {
                this.runTime.value = JSON.parse(result);
            } catch (error) {
                console.error("解析运行时信息失败:", error);
            }
        }
    }

    /**
     * 处理节点状态更新
     */
    private handleNodeStatusUpdate(jobId: number, randomId: string, status: number): void {
        const updateResult = this.updateNodeStatus(jobId, randomId, status);

        if (!updateResult.found) {
            console.warn(
                `未找到对应的节点 - jobId: ${jobId}, randomId: ${randomId}`
            );
            console.warn(`当前所有LogicFlow实例:`, Object.keys(this.lfInstances));
        }
    }

    /**
     * 查找包含指定节点的任务组
     */
    private findTaskGroupByNode(jobId: number, randomId: string): {
        taskGroupId: number | null;
        taskGroupLf: any;
    } {
        for (const [groupId, lfInstance] of Object.entries(this.lfInstances)) {
            if (lfInstance) {
                const nodes = lfInstance.getGraphRawData().nodes;
                const foundNode = nodes.find(
                    (node: any) =>
                        node.properties.jobId == jobId &&
                        node.properties.randomId == randomId
                );

                if (foundNode) {
                    return {
                        taskGroupId: parseInt(groupId),
                        taskGroupLf: lfInstance,
                    };
                }
            }
        }

        return { taskGroupId: null, taskGroupLf: null };
    }

    /**
     * 检查任务组是否完成
     */
    private checkTaskGroupCompletion(taskGroupId: number, taskGroupLf: any): TaskGroupCompletionResult {
        const nodes = taskGroupLf.getGraphRawData().nodes;
        const runningNodes = nodes.filter((node: any) => {
            const nodeModel = taskGroupLf.getNodeModelById(node.id);
            const nodeStyle = nodeModel.getStyle();
            // 检查节点颜色是否为运行中状态（黄色 #FFFF33）
            const isRunning =
                nodeStyle.fill === "#FFFF33" || nodeStyle.stroke === "#FFFF33";
            return isRunning;
        });

        return {
            isCompleted: runningNodes.length === 0,
            taskGroupId,
            runningNodesCount: runningNodes.length,
        };
    }

    /**
     * 处理任务组完成
     */
    private handleTaskGroupCompletion(taskGroupId: number): void {
        console.log(`任务组 ${taskGroupId} 已完成`);

        // 更新任务组运行状态
        this.usePageStoreHook().updatePageRunStatus(taskGroupId, false);

        // 更新该任务组的边样式
        updateEdgeStyleForTaskGroup(
            taskGroupId,
            this.lfInstances[taskGroupId],
            this.usePageStoreHook().getCurrentPageRunStatus
        );

        // 更新对应的日志标签页状态
        const tabId = `${taskGroupId}`;
        const tab = this.logTabs.value.find((t: any) => t.id === tabId);
        if (tab) {
            tab.isRunning = false;
        }
    }

    /**
     * 更新节点状态
     */
    private updateNodeStatus(jobId: number, randomId: string, status: number): {
        found: boolean;
        updatedNodeId?: string;
        taskGroupId?: number;
    } {
        console.log(
            `开始查找节点 - 查找条件: jobId=${jobId}, randomId=${randomId}`
        );

        for (const [groupId, lfInstance] of Object.entries(this.lfInstances)) {
            if (lfInstance) {
                const nodes = lfInstance.getGraphRawData().nodes;

                // 记录调试信息
                console.log(
                    `任务组 ${groupId} 中的所有节点:`,
                    nodes.map((n: any) => ({
                        id: n.id,
                        jobId: n.properties?.jobId,
                        randomId: n.properties?.randomId,
                        type: n.type,
                    }))
                );

                // 首先检查是否有完全匹配的节点（jobId和randomId都匹配）
                const node = nodes.find(
                    (node: any) =>
                        node.properties.jobId == jobId &&
                        node.properties.randomId == randomId
                );

                if (node) {
                    const color = getNodeColor(status);
                    const nodeModel = lfInstance.getNodeModelById(node.id);
                    const style = nodeModel.type === DYNAMIC_CUSTOM_GROUP ? "stroke" : "fill";

                    nodeModel.setStyle(style, color);

                    console.log(
                        `更新任务组 ${groupId} 中节点 ${node.id} 状态为: ${status}, 颜色: ${color}`
                    );

                    return {
                        found: true,
                        updatedNodeId: node.id,
                        taskGroupId: parseInt(groupId),
                    };
                } else {
                    // 如果没有完全匹配，检查是否有jobId匹配但randomId不匹配的节点
                    const nodeWithSameJobId = nodes.find(
                        (node: any) => node.properties.jobId == jobId
                    );

                    if (nodeWithSameJobId) {
                        console.log(
                            `🔍 在任务组 ${groupId} 中找到了相同jobId但randomId不匹配的节点:`,
                            {
                                nodeId: nodeWithSameJobId.id,
                                nodeJobId: nodeWithSameJobId.properties.jobId,
                                nodeRandomId: nodeWithSameJobId.properties.randomId,
                                messageRandomId: randomId,
                                randomIdMatch: nodeWithSameJobId.properties.randomId == randomId,
                            }
                        );
                    }
                }
            }
        }

        return { found: false };
    }

    /**
     * 获取节点状态统计信息
     */
    public getNodeStatusStats(): {
        totalNodes: number;
        runningNodes: number;
        completedNodes: number;
        failedNodes: number;
    } {
        let totalNodes = 0;
        let runningNodes = 0;
        let completedNodes = 0;
        let failedNodes = 0;

        Object.values(this.lfInstances).forEach((lfInstance: any) => {
            if (lfInstance) {
                const nodes = lfInstance.getGraphRawData().nodes;
                totalNodes += nodes.length;

                nodes.forEach((node: any) => {
                    const nodeModel = lfInstance.getNodeModelById(node.id);
                    const nodeStyle = nodeModel.getStyle();
                    const color = nodeStyle.fill || nodeStyle.stroke;

                    switch (color) {
                        case "#FFFF33": // 运行中
                            runningNodes++;
                            break;
                        case "#059669": // 成功
                            completedNodes++;
                            break;
                        case "#DC2626": // 失败
                            failedNodes++;
                            break;
                    }
                });
            }
        });

        return {
            totalNodes,
            runningNodes,
            completedNodes,
            failedNodes,
        };
    }

    /**
     * 清理所有节点状态
     */
    public clearAllNodeStatus(): void {
        Object.values(this.lfInstances).forEach((lfInstance: any) => {
            if (lfInstance) {
                const nodes = lfInstance.getGraphRawData().nodes;
                nodes.forEach((node: any) => {
                    const nodeModel = lfInstance.getNodeModelById(node.id);
                    const style = nodeModel.type === DYNAMIC_CUSTOM_GROUP ? "stroke" : "fill";
                    const defaultColor = nodeModel.type === DYNAMIC_CUSTOM_GROUP ? "#000" : "#fff";
                    nodeModel.setStyle(style, defaultColor);
                });
            }
        });
    }
} 