package com.cc.job.gui.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * 底部状态栏 - 企业级 IDE 风格，深蓝背景，显示分支、编码、位置等占位信息
 */
public class StatusBar extends HBox {

    private final Label leftLabel;
    private final Label encodingLabel;
    private final Label positionLabel;
    private final Label rightLabel;

    public StatusBar() {
        getStyleClass().add("status-bar");
        setPrefHeight(24);
        setMinHeight(24);
        setMaxHeight(24);
        setSpacing(16);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(2, 8, 2, 8));

        leftLabel = new Label("就绪");
        leftLabel.getStyleClass().add("status-bar-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        encodingLabel = new Label("UTF-8");
        encodingLabel.getStyleClass().add("status-bar-label");

        positionLabel = new Label("Ln 1, Col 1");
        positionLabel.getStyleClass().add("status-bar-label");

        rightLabel = new Label("Java");
        rightLabel.getStyleClass().add("status-bar-label");

        getChildren().addAll(leftLabel, spacer, encodingLabel, positionLabel, rightLabel);
    }

    public void setLeftText(String text) {
        leftLabel.setText(text);
    }

    public void setEncoding(String encoding) {
        encodingLabel.setText(encoding);
    }

    public void setPosition(int line, int col) {
        positionLabel.setText("Ln " + line + ", Col " + col);
    }

    public void setRightText(String text) {
        rightLabel.setText(text);
    }
}
