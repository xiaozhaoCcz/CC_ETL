package com.cc.job.gui.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 添加画布书签对话框 - 输入书签名称、描述、类型（位置/节点）
 */
public class AddBookmarkDialog extends Dialog<AddBookmarkDialog.Result> {

    private static final Logger logger = LoggerFactory.getLogger(AddBookmarkDialog.class);

    private TextField nameField;
    private TextArea descriptionArea;
    private ToggleGroup typeGroup;
    private RadioButton positionRadio;
    private RadioButton nodeRadio;

    public static class Result {
        private final String bookmarkName;
        private final String description;
        private final String bookmarkType; // "position" or "node"

        public Result(String bookmarkName, String description, String bookmarkType) {
            this.bookmarkName = bookmarkName;
            this.description = description;
            this.bookmarkType = bookmarkType;
        }

        public String getBookmarkName() { return bookmarkName; }
        public String getDescription() { return description; }
        public String getBookmarkType() { return bookmarkType; }
    }

    public AddBookmarkDialog(Stage owner, boolean hasSelectedNode) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("添加书签");
        setHeaderText("保存当前视口位置或当前选中节点为书签");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(420);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label nameLabel = new Label("书签名称：");
        nameField = new TextField();
        nameField.setPromptText("输入书签名称");
        nameField.setPrefWidth(280);
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        Label descLabel = new Label("描述：");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("可选");
        descriptionArea.setPrefRowCount(2);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefWidth(280);
        grid.add(descLabel, 0, 1);
        grid.add(descriptionArea, 1, 1);

        Label typeLabel = new Label("书签类型：");
        typeGroup = new ToggleGroup();
        positionRadio = new RadioButton("位置书签（当前视口位置）");
        positionRadio.setToggleGroup(typeGroup);
        positionRadio.setUserData("position");
        nodeRadio = new RadioButton("节点书签（当前选中节点）");
        nodeRadio.setToggleGroup(typeGroup);
        nodeRadio.setUserData("node");
        if (hasSelectedNode) {
            nodeRadio.setDisable(false);
            nodeRadio.setSelected(true);
        } else {
            nodeRadio.setDisable(true);
            positionRadio.setSelected(true);
        }
        VBox typeBox = new VBox(5, positionRadio, nodeRadio);
        grid.add(typeLabel, 0, 2);
        grid.add(typeBox, 1, 2);

        content.getChildren().add(grid);

        getDialogPane().setContent(content);
        ButtonType okType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelType, okType);

        setResultConverter(buttonType -> {
            if (buttonType == okType) {
                String name = nameField.getText() != null ? nameField.getText().trim() : "";
                if (name.isEmpty()) {
                    return null;
                }
                Toggle selected = typeGroup.getSelectedToggle();
                String type = selected != null && selected.getUserData() != null
                    ? (String) selected.getUserData() : "position";
                return new Result(
                    name,
                    descriptionArea.getText() != null ? descriptionArea.getText().trim() : "",
                    type
                );
            }
            return null;
        });
    }
}
