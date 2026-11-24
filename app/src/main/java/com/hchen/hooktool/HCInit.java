package com.hchen.hooktool;

import com.sevtinge.hyperceiler.xposedcompat.IXposedHookZygoteInit;
import com.sevtinge.hyperceiler.xposedcompat.callbacks.XC_LoadPackage;

public final class HCInit {
    private static volatile String sModulePackageName = "";
    private static volatile int sLogLevel = 3;
    private static volatile String sTag = "HyperCeiler";
    private static volatile IXposedHookZygoteInit.StartupParam sStartupParam;
    private static volatile XC_LoadPackage.LoadPackageParam sLoadPackageParam;

    private HCInit() {
    }

    public static void initBasicData(BasicData basicData) {
        if (basicData == null) {
            return;
        }
        if (basicData.modulePackageName != null) {
            sModulePackageName = basicData.modulePackageName;
        }
        sLogLevel = basicData.logLevel;
        if (basicData.tag != null) {
            sTag = basicData.tag;
        }
    }

    public static void initStartupParam(IXposedHookZygoteInit.StartupParam startupParam) {
        sStartupParam = startupParam;
    }

    public static void initLoadPackageParam(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        sLoadPackageParam = loadPackageParam;
    }

    public static String getModulePackageName() {
        return sModulePackageName;
    }

    public static int getLogLevel() {
        return sLogLevel;
    }

    public static String getTag() {
        return sTag;
    }

    public static IXposedHookZygoteInit.StartupParam getStartupParam() {
        return sStartupParam;
    }

    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        return sLoadPackageParam;
    }

    public static final class BasicData {
        private String modulePackageName = "";
        private int logLevel = 3;
        private String tag = "HyperCeiler";

        public BasicData setModulePackageName(String modulePackageName) {
            this.modulePackageName = modulePackageName;
            return this;
        }

        public BasicData setLogLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public BasicData setTag(String tag) {
            this.tag = tag;
            return this;
        }
    }
}
