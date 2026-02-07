package com.cc.job.gui.manager;

import com.cc.job.gui.view.NodeCanvas;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * 画布导出管理器 - 负责画布截图和导出功能
 *
 * @author cc-job-team
 * @since 2025-01-XX
 */
public class CanvasExportManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CanvasExportManager.class);
    
    private final Consumer<String> loggerCallback;
    
    public CanvasExportManager(Consumer<String> loggerCallback) {
        this.loggerCallback = loggerCallback;
    }
    
    /**
     * 导出格式枚举
     */
    public enum ExportFormat {
        PNG("png", "PNG图片"),
        JPEG("jpg", "JPEG图片"),
        SVG("svg", "SVG矢量图");
        
        private final String extension;
        private final String description;
        
        ExportFormat(String extension, String description) {
            this.extension = extension;
            this.description = description;
        }
        
        public String getExtension() {
            return extension;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 导出配置
     */
    public static class ExportConfig {
        private ExportFormat format = ExportFormat.PNG;
        private double scale = 1.0; // 缩放比例
        private boolean includeBackground = true; // 是否包含背景
        private Bounds exportBounds; // 导出区域（null表示导出整个画布）
        private Color backgroundColor = Color.WHITE; // 背景颜色
        
        public ExportFormat getFormat() {
            return format;
        }
        
        public void setFormat(ExportFormat format) {
            this.format = format;
        }
        
        public double getScale() {
            return scale;
        }
        
        public void setScale(double scale) {
            this.scale = Math.max(0.1, Math.min(5.0, scale)); // 限制在0.1-5.0之间
        }
        
        public boolean isIncludeBackground() {
            return includeBackground;
        }
        
        public void setIncludeBackground(boolean includeBackground) {
            this.includeBackground = includeBackground;
        }
        
        public Bounds getExportBounds() {
            return exportBounds;
        }
        
        public void setExportBounds(Bounds exportBounds) {
            this.exportBounds = exportBounds;
        }
        
        public Color getBackgroundColor() {
            return backgroundColor;
        }
        
        public void setBackgroundColor(Color backgroundColor) {
            this.backgroundColor = backgroundColor;
        }
    }
    
    /**
     * 导出画布为图片
     *
     * @param canvas 画布节点
     * @param outputFile 输出文件
     * @param config 导出配置
     * @return 是否成功
     */
    public boolean exportCanvas(NodeCanvas canvas, File outputFile, ExportConfig config) {
        if (canvas == null || outputFile == null || config == null) {
            log("⚠ 导出参数无效");
            return false;
        }
        
        try {
            // 计算导出区域
            Bounds bounds = config.getExportBounds();
            if (bounds == null) {
                // 导出整个画布
                bounds = canvas.getBoundsInLocal();
            }
            
            // 创建快照参数：包含背景时用透明 fill，由画布临时背景色渲染整图，避免仅边缘变色
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            
            // 计算快照尺寸
            int width = (int) (bounds.getWidth() * config.getScale());
            int height = (int) (bounds.getHeight() * config.getScale());
            
            final Bounds boundsFinal = bounds;
            final SnapshotParameters paramsFinal = params;
            final int widthFinal = width;
            final int heightFinal = height;
            final WritableImage[] imageHolder = new WritableImage[1];
            
            Runnable snapshotRunnable = () -> {
                if (boundsFinal.equals(canvas.getBoundsInLocal())) {
                    imageHolder[0] = canvas.snapshot(paramsFinal, null);
                } else {
                    imageHolder[0] = exportRegion(canvas, boundsFinal, paramsFinal, widthFinal, heightFinal);
                }
            };
            
            if (config.isIncludeBackground()) {
                canvas.runWithExportBackground(config.getBackgroundColor(), snapshotRunnable);
            } else {
                snapshotRunnable.run();
            }
            
            WritableImage image = imageHolder[0];
            
            // 缩放图片（如果需要）
            if (config.getScale() != 1.0) {
                image = scaleImage(image, config.getScale());
            }
            
            // 保存图片
            String formatName = config.getFormat().getExtension();
            boolean success = ImageIO.write(SwingFXUtils.fromFXImage(image, null), formatName, outputFile);
            
            if (success) {
                log("✓ 画布已导出到: " + outputFile.getAbsolutePath());
            } else {
                log("✗ 导出失败: 不支持的格式 " + formatName);
            }
            
            return success;
        } catch (IOException e) {
            log("✗ 导出失败: " + e.getMessage());
            logger.error("导出画布失败", e);
            return false;
        }
    }
    
    /**
     * 导出指定区域
     */
    private WritableImage exportRegion(NodeCanvas canvas, Bounds bounds, 
                                      SnapshotParameters params, int width, int height) {
        // 创建临时视图，只包含指定区域的内容
        // 这里简化处理，直接导出整个画布然后裁剪
        WritableImage fullImage = canvas.snapshot(params, null);
        
        // 计算裁剪区域
        int x = (int) bounds.getMinX();
        int y = (int) bounds.getMinY();
        int w = Math.min(width, (int) fullImage.getWidth() - x);
        int h = Math.min(height, (int) fullImage.getHeight() - y);
        
        // 创建裁剪后的图片
        WritableImage croppedImage = new WritableImage(w, h);
        croppedImage.getPixelWriter().setPixels(0, 0, w, h, 
            fullImage.getPixelReader(), x, y);
        
        return croppedImage;
    }
    
    /**
     * 缩放图片
     */
    private WritableImage scaleImage(WritableImage original, double scale) {
        int newWidth = (int) (original.getWidth() * scale);
        int newHeight = (int) (original.getHeight() * scale);
        
        // 使用Canvas和GraphicsContext来绘制缩放后的图片
        Canvas canvas = new Canvas(newWidth, newHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(original, 0, 0, newWidth, newHeight);
        
        // 创建快照
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
    
    /**
     * 快速导出（使用默认配置）
     *
     * @param canvas 画布节点
     * @param outputFile 输出文件
     * @return 是否成功
     */
    public boolean quickExport(NodeCanvas canvas, File outputFile) {
        ExportConfig config = new ExportConfig();
        config.setFormat(ExportFormat.PNG);
        config.setScale(1.0);
        return exportCanvas(canvas, outputFile, config);
    }
    
    private void log(String message) {
        if (loggerCallback != null) {
            loggerCallback.accept(message);
        }
        logger.info(message);
    }
}
