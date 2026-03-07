package com.cc.job.executor.compose.core.exception;

/**
 * 执行到审批节点且需要挂起等待审批时抛出
 */
public class WaitingApprovalException extends RuntimeException {

    public WaitingApprovalException(String message) {
        super(message);
    }
}
