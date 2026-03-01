package com.cc.job.gui.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 新手引导：简要使用步骤
 */
public class NewUserGuideDialog extends Dialog<Void> {

    public NewUserGuideDialog(Stage owner) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("新手引导");
        setHeaderText("Cc-ETL 画布编排简要步骤");

        Label content = new Label();
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 13; -fx-line-spacing: 4;");
        content.setText("""
            1. 创建分区与任务组：文件 → 新建 → 新建分区 / 新建任务；或通过任务列表创建任务组。
            2. 在画布中编排：从左侧任务树选中任务组打开画布，拖拽节点、连线配置依赖。
            3. 保存与运行：保存后点击运行；可查看日志与统计大屏。
            4. 版本与模板：文件 → 版本 可保存为版本、回滚；查看 → 保存选中为画布模板 / 从画布模板插入 可复用子流程。
            5. 数据同步：任务 → 数据源同步 / 多数据源同步 配置 DataX 同步；支持预览（前 N 条）与同步后数据质量校验。
            """);

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.getChildren().add(content);

        getDialogPane().setContent(box);
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        setResultConverter(bt -> null);
    }
}
