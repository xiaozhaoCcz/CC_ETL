package com.cc.job.gui.view.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 一体化日期时间选择组件：可选择年月日、时分秒。
 * 主界面为「日期」+「一个时间控件」；点击时间控件后弹出窗口，在弹窗内选择时、分、秒。
 */
public class DateTimePicker extends HBox {

    private static final String TIME_FORMAT = "%02d:%02d:%02d";

    private final DatePicker datePicker;
    private final TextField timeField;
    private final int defaultHour;
    private final int defaultMin;
    private final int defaultSec;

    private int currentHour;
    private int currentMin;
    private int currentSec;

    /**
     * @param defaultHour 默认时 (0-23)
     * @param defaultMin  默认分 (0-59)
     * @param defaultSec  默认秒 (0-59)
     */
    public DateTimePicker(int defaultHour, int defaultMin, int defaultSec) {
        this.defaultHour = clamp(0, 23, defaultHour);
        this.defaultMin = clamp(0, 59, defaultMin);
        this.defaultSec = clamp(0, 59, defaultSec);
        this.currentHour = this.defaultHour;
        this.currentMin = this.defaultMin;
        this.currentSec = this.defaultSec;

        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);

        datePicker = new DatePicker();
        datePicker.setPrefWidth(140);

        timeField = new TextField(formatTime(currentHour, currentMin, currentSec));
        timeField.setPrefWidth(90);
        timeField.setEditable(false);
        timeField.setPromptText("时:分:秒");
        timeField.setOnMouseClicked(e -> showTimePopup());
        timeField.setOnAction(e -> showTimePopup());

        Button timeBtn = new Button("选择");
        timeBtn.setOnAction(e -> showTimePopup());

        getChildren().addAll(datePicker, timeField, timeBtn);
    }

    private static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatTime(int h, int m, int s) {
        return String.format(TIME_FORMAT, h, m, s);
    }

    private void refreshTimeField() {
        timeField.setText(formatTime(currentHour, currentMin, currentSec));
    }

    private static final StringConverter<Integer> TWO_DIGIT_CONVERTER = new StringConverter<Integer>() {
        @Override
        public String toString(Integer i) {
            return i == null ? "0" : String.format("%02d", i);
        }
        @Override
        public Integer fromString(String s) {
            if (s == null || s.trim().isEmpty()) return 0;
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    };

    private void showTimePopup() {
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, currentHour);
        Spinner<Integer> minSpinner = new Spinner<>(0, 59, currentMin);
        Spinner<Integer> secSpinner = new Spinner<>(0, 59, currentSec);
        for (Spinner<Integer> s : Arrays.asList(hourSpinner, minSpinner, secSpinner)) {
            s.setPrefWidth(88);
            s.setMinWidth(80);
            s.setEditable(true);
            s.getValueFactory().setConverter(TWO_DIGIT_CONVERTER);
            TextField editor = s.getEditor();
            editor.setMinWidth(64);
            editor.setPrefWidth(72);
            editor.setStyle("-fx-font-size: 14px; -fx-padding: 6 8;");
        }
        hourSpinner.getValueFactory().setValue(currentHour);
        minSpinner.getValueFactory().setValue(currentMin);
        secSpinner.getValueFactory().setValue(currentSec);

        Popup popup = new Popup();
        Button confirmBtn = new Button("确定");
        Button cancelBtn = new Button("取消");
        confirmBtn.setOnAction(e -> {
            currentHour = hourSpinner.getValue();
            currentMin = minSpinner.getValue();
            currentSec = secSpinner.getValue();
            refreshTimeField();
            popup.hide();
        });
        cancelBtn.setOnAction(e -> popup.hide());

        VBox content = new VBox(10);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1;");
        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        timeRow.getChildren().addAll(
                new Label("时"), hourSpinner,
                new Label("分"), minSpinner,
                new Label("秒"), secSpinner
        );
        HBox btnRow = new HBox(8, confirmBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().addAll(timeRow, btnRow);

        popup.getContent().add(content);
        popup.setAutoHide(true);

        popup.setOnShown(ev -> {
            hourSpinner.getEditor().setText(TWO_DIGIT_CONVERTER.toString(hourSpinner.getValue()));
            minSpinner.getEditor().setText(TWO_DIGIT_CONVERTER.toString(minSpinner.getValue()));
            secSpinner.getEditor().setText(TWO_DIGIT_CONVERTER.toString(secSpinner.getValue()));
        });

        javafx.geometry.Bounds bounds = timeField.localToScreen(timeField.getBoundsInLocal());
        popup.setX(bounds.getMinX());
        popup.setY(bounds.getMaxY());
        popup.show(timeField.getScene().getWindow());
    }

    /**
     * 获取当前选择的日期时间；未选日期时返回 null。
     */
    public LocalDateTime getDateTime() {
        LocalDate date = datePicker.getValue();
        if (date == null) return null;
        return date.atTime(currentHour, currentMin, currentSec);
    }

    /**
     * 设置日期时间；null 表示仅清空日期，时间恢复为默认值。
     */
    public void setDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            datePicker.setValue(null);
            currentHour = defaultHour;
            currentMin = defaultMin;
            currentSec = defaultSec;
            refreshTimeField();
            return;
        }
        datePicker.setValue(dateTime.toLocalDate());
        currentHour = dateTime.getHour();
        currentMin = dateTime.getMinute();
        currentSec = dateTime.getSecond();
        refreshTimeField();
    }

    /**
     * 清空日期并将时分秒恢复为默认值。
     */
    public void clear() {
        datePicker.setValue(null);
        currentHour = defaultHour;
        currentMin = defaultMin;
        currentSec = defaultSec;
        refreshTimeField();
    }

    /**
     * 是否已选择日期（仅判断日期，时分秒始终有值）。
     */
    public boolean hasDate() {
        return datePicker.getValue() != null;
    }
}
