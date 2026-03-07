package com.cc.job.gui.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * GUI 国际化：根据当前语言获取文案。
 * 语言通过 ConfigManager "locale" 持久化，值为 zh_CN / en。
 */
public class I18n {

    private static final String BUNDLE_BASE = "i18n.messages";
    private static volatile Locale currentLocale;

    public static Locale getLocale() {
        if (currentLocale != null) return currentLocale;
        String code = ConfigManager.getInstance().getProperty("locale", "zh_CN");
        if ("en".equalsIgnoreCase(code) || "en_US".equalsIgnoreCase(code)) {
            currentLocale = Locale.ENGLISH;
        } else {
            currentLocale = Locale.SIMPLIFIED_CHINESE;
        }
        return currentLocale;
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        ConfigManager.getInstance().setProperty("locale", currentLocale.toLanguageTag().replace("-", "_"));
    }

    public static String get(String key) {
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE, getLocale()).getString(key);
        } catch (Exception e) {
            return key;
        }
    }
}
