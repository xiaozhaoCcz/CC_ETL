package com.cc.job.gui.history;

/**
 * 表示画布上的一次可撤销操作
 */
public interface CanvasAction {

    /**
     * 撤销当前操作
     */
    void undo();

    /**
     * 重做当前操作
     */
    void redo();
}

