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
 * 保存为节点模板对话框 - 输入模板名称、分类、描述、是否公开
 */
public class SaveNodeTemplateDialog extends Dialog<SaveNodeTemplateDialog.Result> {

    private static final Logger logger = LoggerFactory.getLogger(SaveNodeTemplateDialog.class);

    private TextField nameField;
    private TextField categoryField;
    private TextArea descriptionArea;
    private CheckBox isPublicCheck;

    public static class Result {
        private final String templateName;
        private final String templateCategory;
        private final String description;
        private final boolean isPublic;

        public Result(String templateName, String templateCategory, String description, boolean isPublic) {
            this.templateName = templateName;
            this.templateCategory = templateCategory;
            this.description = description;
            this.isPublic = isPublic;
        }

        public String getTemplateName() { return templateName; }
        public String getTemplateCategory() { return templateCategory; }
        public String getDescription() { return description; }
        public boolean isPublic() { return isPublic; }
    }

    public SaveNodeTemplateDialog(Stage owner, String nodeDisplayName) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("保存为节点模板");
        setHeaderText("将节点 \"" + (nodeDisplayName != null ? nodeDisplayName : "") + "\" 保存为可复用模板");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label nameLabel = new Label("模板名称：");
        nameField = new TextField();
        nameField.setPromptText("输入模板名称");
        nameField.setPrefWidth(300);
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        Label categoryLabel = new Label("模板分类：");
        categoryField = new TextField();
        categoryField.setPromptText("如：数据同步、API 任务");
        categoryField.setPrefWidth(300);
        grid.add(categoryLabel, 0, 1);
        grid.add(categoryField, 1, 1);

        Label descLabel = new Label("描述：");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText("可选");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefWidth(300);
        grid.add(descLabel, 0, 2);
        grid.add(descriptionArea, 1, 2);

        isPublicCheck = new CheckBox("公开（其他用户可见）");
        grid.add(isPublicCheck, 1, 3);

        content.getChildren().add(grid);

        getDialogPane().setContent(content);
        getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        ButtonType okType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelType, okType);

        setResultConverter(buttonType -> {
            if (buttonType == okType) {
                String name = nameField.getText() != null ? nameField.getText().trim() : "";
                if (name.isEmpty()) {
                    return null;
                }
                return new Result(
                    name,
                    categoryField.getText() != null ? categoryField.getText().trim() : "",
                    descriptionArea.getText() != null ? descriptionArea.getText().trim() : "",
                    isPublicCheck.isSelected()
                );
            }
            return null;
        });
    }
}
