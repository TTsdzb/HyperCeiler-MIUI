package com.sevtinge.hyperceiler.xposedcompat;

import com.sevtinge.hyperceiler.xposedcompat.callbacks.XC_LoadPackage;

public interface IXposedHookLoadPackage {
    void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
}
