package com.cc.job.admin.task.sse;

/**
 * SSE 多实例广播提供者接口
 * 用于将节点状态事件写入广播媒介（如数据库），由各实例轮询后在本机推送
 *
 * @author cc-job
 */
public interface SseBroadcastProvider {

    /**
     * 发布节点状态消息到广播表/队列
     * 各 Admin 实例通过轮询获取后在本机调用 sendMessageToLocalOnly 推送
     *
     * @param connectionKey 连接键 parentJobId:randomId
     * @param messageJson   消息体 JSON
     */
    void publishNodeStatus(String connectionKey, String messageJson);
}
