package com.hchen.hooktool.log;

import com.hchen.hooktool.HCInit;
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge;

public final class XposedLog {
    private XposedLog() {
    }

    public static void logE(String tag, String msg) {
        if (HCInit.getLogLevel() < 1) return;
        XposedBridge.log("[" + HCInit.getTag() + "][E][" + tag + "]: " + msg);
    }

    public static void logE(String tag, Throwable t) {
        if (HCInit.getLogLevel() < 1) return;
        XposedBridge.log("[" + HCInit.getTag() + "][E][" + tag + "]: " + t);
        if (t != null) XposedBridge.log(t);
    }

    public static void logE(String tag, String msg, Throwable t) {
        if (HCInit.getLogLevel() < 1) return;
        XposedBridge.log("[" + HCInit.getTag() + "][E][" + tag + "]: " + msg + ", by: " + t);
        if (t != null) XposedBridge.log(t);
    }

    public static void logW(String tag, String msg) {
        if (HCInit.getLogLevel() < 2) return;
        XposedBridge.log("[" + HCInit.getTag() + "][W][" + tag + "]: " + msg);
    }

    public static void logW(String tag, Throwable t) {
        if (HCInit.getLogLevel() < 2) return;
        XposedBridge.log("[" + HCInit.getTag() + "][W][" + tag + "]: " + t);
    }

    public static void logW(String tag, String msg, Throwable t) {
        if (HCInit.getLogLevel() < 2) return;
        XposedBridge.log("[" + HCInit.getTag() + "][W][" + tag + "]: " + msg + ", by: " + t);
    }

    public static void logI(String tag, String msg) {
        if (HCInit.getLogLevel() < 3) return;
        XposedBridge.log("[" + HCInit.getTag() + "][I][" + tag + "]: " + msg);
    }

    public static void logI(String tag, String msg, Throwable t) {
        if (HCInit.getLogLevel() < 3) return;
        XposedBridge.log("[" + HCInit.getTag() + "][I][" + tag + "]: " + msg + ", by: " + t);
    }

    public static void logD(String tag, String msg) {
        if (HCInit.getLogLevel() < 4) return;
        XposedBridge.log("[" + HCInit.getTag() + "][D][" + tag + "]: " + msg);
    }

    public static void logD(String tag, Throwable t) {
        if (HCInit.getLogLevel() < 4) return;
        XposedBridge.log("[" + HCInit.getTag() + "][D][" + tag + "]: " + t);
    }

    public static void logD(String tag, String msg, Throwable t) {
        if (HCInit.getLogLevel() < 4) return;
        XposedBridge.log("[" + HCInit.getTag() + "][D][" + tag + "]: " + msg + ", by: " + t);
    }
}
