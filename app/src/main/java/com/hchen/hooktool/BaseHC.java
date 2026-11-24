package com.hchen.hooktool;

import com.hchen.hooktool.log.XposedLog;
import com.hchen.hooktool.tool.CoreTool;
import com.sevtinge.hyperceiler.xposedcompat.callbacks.XC_LoadPackage;

public abstract class BaseHC extends CoreTool {
    protected String TAG = getClass().getSimpleName();
    public XC_LoadPackage.LoadPackageParam lpparam;
    public ClassLoader classLoader;

    protected abstract void init();

    public final void onLoadPackage() {
        this.lpparam = HCInit.getLoadPackageParam();
        this.classLoader = this.lpparam != null ? this.lpparam.classLoader : null;
        try {
            init();
        } catch (Throwable t) {
            XposedLog.logE(TAG, t);
        }
    }
}
