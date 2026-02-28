package com.cc.job.gui.util;

import javafx.application.Platform;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 主题管理器：维护当前 light/dark/auto，提供样式表 URL，与 ConfigManager 同步。
 * 默认主题为浅色（light）；仅在用户选择深色或自动并保存后才持久化 theme 配置。
 * 配置键 theme 统一为 "light" | "dark" | "auto"；兼容旧配置 "浅色"/"深色"/"自动"。
 */
public class ThemeManager {

    private static final String CONFIG_KEY_THEME = "theme";
    private static final String LIGHT = "light";
    private static final String DARK = "dark";
    private static final String AUTO = "auto";

    private static volatile ThemeManager instance;
    private final ConfigManager configManager;
    private final List<Runnable> onThemeChangedListeners = new CopyOnWriteArrayList<>();

    private ThemeManager() {
        this.configManager = ConfigManager.getInstance();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    /**
     * 从配置读取并规范为主题键：light / dark / auto。
     * 兼容旧值：浅色->light, 深色->dark, 自动->auto。
     */
    private String normalizeThemeKey(String raw) {
        if (raw == null || raw.isEmpty()) return LIGHT;
        switch (raw.trim()) {
            case "浅色": return LIGHT;
            case "深色": return DARK;
            case "自动": return AUTO;
            case LIGHT:
            case DARK:
            case AUTO:
                return raw.trim();
            default:
                return LIGHT;
        }
    }

    /**
     * 返回当前生效的主题：light 或 dark。
     * auto 时首版固定为 light，后续可改为根据系统外观解析。
     */
    public String getTheme() {
        String key = normalizeThemeKey(configManager.getProperty(CONFIG_KEY_THEME, LIGHT));
        if (AUTO.equals(key)) {
            return resolveAutoTheme();
        }
        return key;
    }

    /**
     * 自动主题解析，首版固定为 light。
     */
    private String resolveAutoTheme() {
        return LIGHT;
    }

    /**
     * 返回当前主题对应的样式表 classpath URL，供 Scene/Dialog 使用。
     */
    public String getStylesheetUrl() {
        String theme = getTheme();
        String path = DARK.equals(theme) ? "/styles-dark.css" : "/styles.css";
        URL url = ThemeManager.class.getResource(path);
        return url != null ? url.toExternalForm() : "";
    }

    /**
     * 设置主题并持久化。theme 应为 "light" | "dark" | "auto"。
     * 会通知所有已注册的主题变更监听器（在 JavaFX 线程执行）。
     */
    public void setTheme(String theme) {
        String key = normalizeThemeKey(theme != null ? theme : LIGHT);
        configManager.setProperty(CONFIG_KEY_THEME, key);
        Platform.runLater(() -> {
            for (Runnable r : onThemeChangedListeners) {
                try {
                    r.run();
                } catch (Exception e) {
                    // 避免单个监听器异常影响其他监听器
                }
            }
        });
    }

    /**
     * 注册主题变更监听器。主题变更后会在 JavaFX 线程回调。
     */
    public void addOnThemeChanged(Runnable listener) {
        if (listener != null && !onThemeChangedListeners.contains(listener)) {
            onThemeChangedListeners.add(listener);
        }
    }

    /**
     * 移除主题变更监听器。
     */
    public void removeOnThemeChanged(Runnable listener) {
        onThemeChangedListeners.remove(listener);
    }

    /**
     * 返回当前配置的原始主题键（light/dark/auto），用于设置对话框显示与保存。
     */
    public String getThemeConfigKey() {
        return normalizeThemeKey(configManager.getProperty(CONFIG_KEY_THEME, LIGHT));
    }
}
