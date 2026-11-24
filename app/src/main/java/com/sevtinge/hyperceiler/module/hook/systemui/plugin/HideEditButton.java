package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import static com.sevtinge.hyperceiler.xposedcompat.XC_MethodReplacement.returnConstant;

import com.sevtinge.hyperceiler.xposedcompat.XposedHelpers;

public class HideEditButton {
    public static void initHideEditButton(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.qs.EditButtonController", classLoader, "available", boolean.class, returnConstant(false));
        XposedHelpers.findAndHookMethod("miui.systemui.controlcenter.panel.main.qs.EditButtonController", classLoader, "available", boolean.class, "miui.systemui.controlcenter.panel.main.MainPanelModeController$MainPanelMode", returnConstant(false));
    }
}
