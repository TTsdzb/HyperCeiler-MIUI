package com.hchen.hooktool.tool.additional;

import com.hchen.hooktool.tool.CoreTool;
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge;

public final class SystemPropTool {
    private static final String CLASS_SYSTEM_PROPERTIES = "android.os.SystemProperties";

    private SystemPropTool() {
    }

    public static String getProp(String key) {
        return getProp(key, "");
    }

    public static String getProp(String key, String def) {
        Object res = CoreTool.callStaticMethod(CLASS_SYSTEM_PROPERTIES, XposedBridge.BOOTCLASSLOADER, "get", key, def);
        return res instanceof String ? (String) res : def;
    }

    public static boolean getProp(String key, boolean def) {
        Object res = CoreTool.callStaticMethod(CLASS_SYSTEM_PROPERTIES, XposedBridge.BOOTCLASSLOADER, "getBoolean", key, def);
        return res instanceof Boolean ? (Boolean) res : def;
    }

    public static int getProp(String key, int def) {
        Object res = CoreTool.callStaticMethod(CLASS_SYSTEM_PROPERTIES, XposedBridge.BOOTCLASSLOADER, "getInt", key, def);
        return res instanceof Integer ? (Integer) res : def;
    }

    public static long getProp(String key, long def) {
        Object res = CoreTool.callStaticMethod(CLASS_SYSTEM_PROPERTIES, XposedBridge.BOOTCLASSLOADER, "getLong", key, def);
        return res instanceof Long ? (Long) res : def;
    }
}
