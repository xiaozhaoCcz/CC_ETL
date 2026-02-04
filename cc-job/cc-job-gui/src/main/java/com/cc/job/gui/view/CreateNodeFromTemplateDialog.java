package com.cc.job.gui.view;

import com.cc.job.gui.manager.NodeTemplateManager;
import com.cc.job.xo.model.entity.JobNodeTemplate;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 从模板创建节点对话框 - 列出模板，选择后创建节点
 */
public class CreateNodeFromTemplateDialog extends Dialog<JobNodeTemplate> {

    private static final Logger logger = LoggerFactory.getLogger(CreateNodeFromTemplateDialog.class);

    private final ListView<JobNodeTemplate> templateList;
    private final ComboBox<String> categoryCombo;
    private final NodeTemplateManager nodeTemplateManager;

    public CreateNodeFromTemplateDialog(Stage owner, NodeTemplateManager nodeTemplateManager) {
        this.nodeTemplateManager = nodeTemplateManager;
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("从模板创建节点");
        setHeaderText("选择节点模板，将在当前任务组画布上创建新节点");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        content.setPrefHeight(450);

        GridPane topBar = new GridPane();
        topBar.setHgap(10);
        topBar.setVgap(8);
        Label categoryLabel = new Label("分类筛选：");
        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("全部");
        categoryCombo.setPrefWidth(200);
        categoryCombo.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> refreshList());
        topBar.add(categoryLabel, 0, 0);
        topBar.add(categoryCombo, 1, 0);
        content.getChildren().add(topBar);

        Label listLabel = new Label("模板列表：");
        listLabel.setStyle("-fx-font-weight: bold;");
        content.getChildren().add(listLabel);

        templateList = new ListView<>();
        templateList.setPrefHeight(280);
        templateList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(JobNodeTemplate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String cat = item.getTemplateCategory() != null ? " [" + item.getTemplateCategory() + "]" : "";
                    setText(item.getTemplateName() + cat);
                }
            }
        });
        content.getChildren().add(templateList);

        getDialogPane().setContent(content);
        getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        ButtonType createType = new ButtonType("创建节点", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(cancelType, createType);

        setResultConverter(buttonType -> {
            if (buttonType == createType) {
                return templateList.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        refreshCategories();
        refreshList();
    }

    private void refreshCategories() {
        List<String> categories = nodeTemplateManager.getTemplateCategories();
        List<String> options = new ArrayList<>();
        options.add("");
        if (categories != null) options.addAll(categories);
        categoryCombo.setItems(FXCollections.observableArrayList(options));
        categoryCombo.getSelectionModel().select(0);
    }

    private void refreshList() {
        String category = categoryCombo.getSelectionModel().getSelectedItem();
        if (category != null && category.isEmpty()) category = null;
        List<JobNodeTemplate> list = nodeTemplateManager.getTemplates(category);
        templateList.setItems(FXCollections.observableArrayList(list != null ? list : new ArrayList<>()));
    }

    /**
     * 显示对话框并返回选中的模板（用户点击「创建节点」时）
     */
    public static Optional<JobNodeTemplate> showAndSelect(Stage owner, NodeTemplateManager nodeTemplateManager) {
        CreateNodeFromTemplateDialog dialog = new CreateNodeFromTemplateDialog(owner, nodeTemplateManager);
        return dialog.showAndWait();
    }
}
