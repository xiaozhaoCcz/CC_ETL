package com.cc.job.gui.history;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 简单的撤销/重做管理器
 */
public class UndoRedoManager {

    private final Deque<CanvasAction> undoStack = new ArrayDeque<>();
    private final Deque<CanvasAction> redoStack = new ArrayDeque<>();

    private Runnable changeListener;

    public void push(CanvasAction action) {
        if (action == null) {
            return;
        }
        undoStack.push(action);
        redoStack.clear();
        notifyChange();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }
        CanvasAction action = undoStack.pop();
        action.undo();
        redoStack.push(action);
        notifyChange();
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }
        CanvasAction action = redoStack.pop();
        action.redo();
        undoStack.push(action);
        notifyChange();
    }

    public void clear() {
        if (undoStack.isEmpty() && redoStack.isEmpty()) {
            return;
        }
        undoStack.clear();
        redoStack.clear();
        notifyChange();
    }

    public void setOnChange(Runnable listener) {
        this.changeListener = listener;
        notifyChange();
    }

    private void notifyChange() {
        if (changeListener != null) {
            changeListener.run();
        }
    }
}

