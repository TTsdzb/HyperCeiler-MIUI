package com.sevtinge.hyperceiler.xposedcompat.callbacks;

import android.content.pm.ApplicationInfo;

import com.sevtinge.hyperceiler.xposedcompat.IXposedHookLoadPackage;

public abstract class XC_LoadPackage extends XCallback implements IXposedHookLoadPackage {
    public XC_LoadPackage() {
        super();
    }

    public XC_LoadPackage(int priority) {
        super(priority);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
    }

    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstApplication;

        public LoadPackageParam() {
        }
    }
}
