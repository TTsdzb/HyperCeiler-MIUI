/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler;

import static com.sevtinge.hyperceiler.module.base.tool.AppsTool.getPackageVersionCode;
import static com.sevtinge.hyperceiler.module.base.tool.AppsTool.getPackageVersionName;
import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getMiuiVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMiuiVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevelDesc;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;
import static com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.mPrefsMap;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.hchen.hooktool.HCInit;
import com.sevtinge.hyperceiler.module.app.VariousThirdApps;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.DataBase;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool;
import com.sevtinge.hyperceiler.module.hook.systemframework.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.module.hook.systemframework.AllowUninstall;
import com.sevtinge.hyperceiler.module.hook.systemframework.BackgroundBlurDrawable;
import com.sevtinge.hyperceiler.module.hook.systemframework.CleanOpenMenu;
import com.sevtinge.hyperceiler.module.hook.systemframework.CleanShareMenu;
import com.sevtinge.hyperceiler.module.hook.systemframework.ScreenRotation;
import com.sevtinge.hyperceiler.module.hook.systemframework.ToastBlur;
import com.sevtinge.hyperceiler.module.hook.systemframework.UnlockAlwaysOnDisplay;
import com.sevtinge.hyperceiler.module.hook.systemsettings.VolumeSeparateControlForSettings;
import com.sevtinge.hyperceiler.module.skip.SystemFrameworkForCorePatch;
import com.sevtinge.hyperceiler.safe.CrashHook;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.log.LogManager;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.xposedcompat.IXposedHookZygoteInit;
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge;
import com.sevtinge.hyperceiler.xposedcompat.XposedCompatRuntime;
import com.sevtinge.hyperceiler.xposedcompat.callbacks.XC_LoadPackage;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import io.github.libxposed.api.XposedModule;

public class XposedInit extends XposedModule {
    private static final String TAG = "HyperCeiler";
    public static String mModulePath = null;
    public static ResourcesTool mResHook;

    // public static XmlTool mXmlTool;
    public final VariousThirdApps mVariousThirdApps = new VariousThirdApps();

    private volatile String mProcessName = null;
    private IXposedHookZygoteInit.StartupParam mStartupParam = null;
    private volatile boolean mRemotePrefsLoaded = false;
    private boolean mWarnedRemotePrefsUnavailable = false;
    private boolean mZygoteHooksLoaded = false;
    private final Set<String> mLoadedPackages = ConcurrentHashMap.newKeySet();

    public XposedInit() {
        super();
    }

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        XposedCompatRuntime.init(this);
        mProcessName = param.getProcessName();
        mModulePath = getModuleApplicationInfo().sourceDir;
        mResHook = new ResourcesTool(mModulePath);
        // mXmlTool = new XmlTool(startupParam);

        IXposedHookZygoteInit.StartupParam startupParam = new IXposedHookZygoteInit.StartupParam();
        startupParam.modulePath = mModulePath;
        startupParam.startsSystemServer = param.isSystemServer();
        mStartupParam = startupParam;

        // load EzXHelper and set log tag
        EzXHelper.initZygote(startupParam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);

        // load remote SharedPreferences before LogManager/zygote hooks read mPrefsMap
        ensureRemotePrefsLoaded();

        // load HCInit
        HCInit.initBasicData(new HCInit.BasicData()
                .setModulePackageName(BuildConfig.APPLICATION_ID)
                .setLogLevel(LogManager.getLogLevel())
                .setTag(TAG)
        );
        HCInit.initStartupParam(startupParam);

        if (mRemotePrefsLoaded) {
            ensureZygoteHooksLoaded(param.isSystemServer());
        }
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        ensureRemotePrefsLoaded();
        ensureZygoteHooksLoaded(true);

        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam();
        lpparam.packageName = "android";
        lpparam.processName = "android";
        lpparam.classLoader = param.getClassLoader();
        lpparam.appInfo = null;
        lpparam.isFirstApplication = true;

        if (isInSafeMode("android")) return;

        EzXHelper.initHandleLoadPackage(lpparam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);

        if (mPrefsMap.getBoolean("system_framework_allow_uninstall")) {
            try {
                new AllowUninstall().hookServicesClassLoader(param.getClassLoader());
            } catch (Throwable t) {
                logE(TAG, t);
            }
        }

        try {
            IXposedHookZygoteInit.StartupParam systemStartupParam = new IXposedHookZygoteInit.StartupParam();
            systemStartupParam.modulePath = mModulePath;
            systemStartupParam.startsSystemServer = true;
            SystemFrameworkForCorePatch corePatch = new SystemFrameworkForCorePatch();
            corePatch.initZygote(systemStartupParam);
            corePatch.handleLoadPackage(lpparam);
        } catch (Throwable t) {
            logE(TAG, t);
        }

        init(lpparam);
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        String packageName = param.getPackageName();
        if (ProjectApi.mAppModulePkg.equals(packageName)) {
            return;
        }

        ensureRemotePrefsLoaded();
        ensureZygoteHooksLoaded(false);

        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam();
        lpparam.packageName = packageName;
        lpparam.processName = mProcessName != null ? mProcessName : packageName;
        lpparam.classLoader = param.getClassLoader();
        lpparam.appInfo = param.getApplicationInfo();
        lpparam.isFirstApplication = param.isFirstPackage();

        String dedupKey = lpparam.packageName + "@" + System.identityHashCode(lpparam.classLoader);
        if (!mLoadedPackages.add(dedupKey)) {
            return;
        }

        if (isInSafeMode(lpparam.packageName)) return;

        EzXHelper.initHandleLoadPackage(lpparam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);

        init(lpparam);
    }

    private synchronized void ensureRemotePrefsLoaded() {
        if (mRemotePrefsLoaded) {
            return;
        }
        try {
            SharedPreferences remotePrefs = getRemotePreferences(PrefsUtils.REMOTE_PREFS_GROUP);
            if (remotePrefs != null) {
                PrefsUtils.initForHook(remotePrefs);
                mRemotePrefsLoaded = true;
                return;
            }
        } catch (Throwable t) {
            if (!mWarnedRemotePrefsUnavailable) {
                mWarnedRemotePrefsUnavailable = true;
                log(Log.WARN, TAG, "Cannot read remote preferences at bootstrap, will retry", t);
            }
            return;
        }
        if (!mWarnedRemotePrefsUnavailable) {
            mWarnedRemotePrefsUnavailable = true;
            log(Log.WARN, TAG, "Remote preferences returned null at bootstrap, will retry");
        }
    }

    private synchronized void ensureZygoteHooksLoaded(boolean startsSystemServer) {
        if (!mRemotePrefsLoaded || mZygoteHooksLoaded) {
            return;
        }
        mZygoteHooksLoaded = true;

        IXposedHookZygoteInit.StartupParam startupParam = mStartupParam;
        if (startupParam == null) {
            startupParam = new IXposedHookZygoteInit.StartupParam();
            startupParam.modulePath = mModulePath;
            startupParam.startsSystemServer = startsSystemServer;
            mStartupParam = startupParam;
        }

        try {
            if (mPrefsMap.getBoolean("system_framework_screen_all_rotations")) ScreenRotation.initRes();
            if (mPrefsMap.getBoolean("system_framework_clean_share_menu")) CleanShareMenu.initRes();
            if (mPrefsMap.getBoolean("system_framework_clean_open_menu")) CleanOpenMenu.initRes();
            if (mPrefsMap.getBoolean("system_framework_volume_separate_control"))
                VolumeSeparateControlForSettings.initRes();
        } catch (Throwable t) {
            logE(TAG, t);
        }

        try {
            new BackgroundBlurDrawable().initZygote(startupParam);
        } catch (Throwable t) {
            logE(TAG, t);
        }
        if (mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications")) {
            try {
                new AllowManageAllNotifications().initZygote(startupParam);
            } catch (Throwable t) {
                logE(TAG, t);
            }
        }
        if (mPrefsMap.getBoolean("system_framework_background_blur_toast")) {
            try {
                new ToastBlur().initZygote(startupParam);
            } catch (Throwable t) {
                logE(TAG, t);
            }
        }
        if (mPrefsMap.getBoolean("aod_unlock_always_on_display_hyper")) {
            try {
                new UnlockAlwaysOnDisplay().initZygote(startupParam);
            } catch (Throwable t) {
                logE(TAG, t);
            }
        }
    }

    private void init(XC_LoadPackage.LoadPackageParam lpparam) {
        String packageName = lpparam.packageName;
        if (Objects.equals(packageName, "android"))
            logI(packageName, "androidVersion = " + getAndroidVersion() + ", miuiVersion = " + getMiuiVersion() + ", hyperosVersion = " + getHyperOSVersion());
        else
            logI(packageName, "versionName = " + getPackageVersionName(lpparam) + ", versionCode = " + getPackageVersionCode(lpparam));

        invokeInit(lpparam);
        androidCrashEventHook(lpparam);
    }

    private void invokeInit(XC_LoadPackage.LoadPackageParam lpparam) {
        String mPkgName = lpparam.packageName;
        if (mPkgName == null) return;

        if (ProjectApi.mAppModulePkg.equals(mPkgName)) {
            return;
        }

        if (isOtherRestrictions(mPkgName)) return;

        HashMap<String, DataBase> dataMap = DataBase.get();
        if (dataMap.values().stream().noneMatch(dataBase -> dataBase.mTargetPackage.equals(mPkgName))) {
            mVariousThirdApps.init(lpparam);
            return;
        }

        dataMap.forEach(new BiConsumer<>() {
            @Override
            public void accept(String s, DataBase dataBase) {
                if (!mPkgName.equals(dataBase.mTargetPackage))
                    return;
                if (!(dataBase.mTargetSdk == -1) && !isAndroidVersion(dataBase.mTargetSdk))
                    return;
                if (!(dataBase.mTargetOSVersion == -1F) && !(isHyperOSVersion(dataBase.mTargetOSVersion) || isMiuiVersion(dataBase.mTargetOSVersion)))
                    return;
                if ((dataBase.isPad == 1 && !isPad()) || (dataBase.isPad == 2 && isPad()))
                    return;

                try {
                    Class<?> clazz = Objects.requireNonNull(getClass().getClassLoader()).loadClass(s);
                    BaseModule module = (BaseModule) clazz.getDeclaredConstructor().newInstance();
                    module.init(lpparam);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                         InstantiationException | InvocationTargetException e) {
                    logE(TAG, e);
                }
            }
        });
    }

    private void androidCrashEventHook(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            XposedBridge.log("[HyperCeiler][I]: Log level is " + logLevelDesc());
            try {
                new CrashHook(lpparam);
            } catch (Exception e) {
                logE(TAG, e);
            }
        }
    }

    private boolean isInSafeMode(String pkg) {
        switch (pkg) {
            case "com.android.systemui" -> {
                return isSystemUIModuleEnable();
            }
            case "com.miui.home" -> {
                return isHomeModuleEnable();
            }
            case "com.miui.securitycenter" -> {
                return isSecurityCenterModuleEnable();
            }
        }
        return false;
    }

    private boolean isOtherRestrictions(String pkg) {
        switch (pkg) {
            case "com.google.android.webview", "com.miui.contentcatcher",
                 "com.miui.catcherpatch" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean isSafeModeEnable(String key) {
        return mPrefsMap.getBoolean(key);
    }

    private boolean isSystemUIModuleEnable() {
        return isSafeModeEnable("system_ui_safe_mode_enable");
    }

    private boolean isHomeModuleEnable() {
        return isSafeModeEnable("home_safe_mode_enable");
    }

    private boolean isSecurityCenterModuleEnable() {
        return isSafeModeEnable("security_center_safe_mode_enable");
    }
}
