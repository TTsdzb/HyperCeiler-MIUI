package com.github.kyuubiran.ezxhelper.interfaces

import com.sevtinge.hyperceiler.xposedcompat.XC_MethodHook

fun interface IMethodHookCallback {
    fun onMethodHooked(param: XC_MethodHook.MethodHookParam)
}
