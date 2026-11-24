package com.hchen.hooktool.hook;

import com.hchen.hooktool.tool.ParamTool;
import com.sevtinge.hyperceiler.xposedcompat.XC_MethodHook;
import com.sevtinge.hyperceiler.xposedcompat.callbacks.XCallback;

public abstract class IHook extends ParamTool {
    public void before() throws Throwable {
    }

    public void after() throws Throwable {
    }

    public final XC_MethodHook toMethodHook() {
        return toMethodHook(XCallback.PRIORITY_DEFAULT);
    }

    public final XC_MethodHook toMethodHook(int priority) {
        final IHook self = this;
        return new XC_MethodHook(priority) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MethodHookParam prev = self.swapParam(param);
                try {
                    self.before();
                } finally {
                    self.swapParam(prev);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                MethodHookParam prev = self.swapParam(param);
                try {
                    self.after();
                } finally {
                    self.swapParam(prev);
                }
            }
        };
    }
}
