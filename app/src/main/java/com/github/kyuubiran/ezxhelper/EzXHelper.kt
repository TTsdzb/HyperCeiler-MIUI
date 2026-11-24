package com.github.kyuubiran.ezxhelper

import android.content.Context
import android.content.res.AssetManager
import com.sevtinge.hyperceiler.xposedcompat.AndroidAppHelper
import com.sevtinge.hyperceiler.xposedcompat.IXposedHookZygoteInit
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge
import com.sevtinge.hyperceiler.xposedcompat.callbacks.XC_LoadPackage

object EzXHelper {
    @JvmStatic
    lateinit var modulePath: String

    val isModulePathInited: Boolean
        get() = this::modulePath.isInitialized

    @JvmStatic
    var classLoader: ClassLoader = XposedBridge.BOOTCLASSLOADER

    @JvmStatic
    lateinit var hostPackageName: String

    @JvmStatic
    val isHostPackageNameInited: Boolean
        get() = this::hostPackageName.isInitialized

    @JvmStatic
    lateinit var appContext: Context

    @JvmStatic
    val isAppContextInited: Boolean
        get() = this::appContext.isInitialized

    @JvmStatic
    var toastTag: String = "EzXHelper"
        private set

    @JvmStatic
    fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        if (startupParam.modulePath != null) {
            modulePath = startupParam.modulePath
        }
    }

    @JvmStatic
    fun initHandleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.classLoader != null) {
            classLoader = lpparam.classLoader
        }
        if (lpparam.packageName != null) {
            hostPackageName = lpparam.packageName
        }
    }

    @JvmStatic
    @JvmOverloads
    fun initAppContext(context: Context? = null, addModuleAssetPath: Boolean = false) {
        val ctx = context ?: AndroidAppHelper.currentApplication() ?: return
        appContext = ctx
        if (!isHostPackageNameInited) {
            ctx.packageName?.let { hostPackageName = it }
        }
        if (addModuleAssetPath && isModulePathInited) {
            addModuleAssetPath(ctx)
        }
    }

    @JvmStatic
    fun addModuleAssetPath(context: Context) {
        if (!isModulePathInited) return
        try {
            val addAssetPath = AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java)
            addAssetPath.isAccessible = true
            addAssetPath.invoke(context.resources.assets, modulePath)
        } catch (t: Throwable) {
            Log.e(t)
        }
    }

    @JvmStatic
    fun setLogTag(tag: String) {
        Log.currentLogTag = tag
    }

    @JvmStatic
    fun setToastTag(tag: String) {
        toastTag = tag
    }
}
