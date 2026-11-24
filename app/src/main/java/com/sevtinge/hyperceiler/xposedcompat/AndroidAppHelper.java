package com.sevtinge.hyperceiler.xposedcompat;

import android.annotation.SuppressLint;
import android.app.Application;

import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public final class AndroidAppHelper {
    private AndroidAppHelper() {
    }

    public static Application currentApplication() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            try {
                Method currentApplication = activityThreadClass.getDeclaredMethod("currentApplication");
                currentApplication.setAccessible(true);
                Object app = currentApplication.invoke(null);
                if (app instanceof Application) {
                    return (Application) app;
                }
            } catch (Throwable ignored) {
            }
            Method currentActivityThread = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object at = currentActivityThread.invoke(null);
            if (at != null) {
                Method getApplication = activityThreadClass.getDeclaredMethod("getApplication");
                getApplication.setAccessible(true);
                Object app = getApplication.invoke(at);
                if (app instanceof Application) {
                    return (Application) app;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String currentPackageName() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentPackageName = activityThreadClass.getDeclaredMethod("currentPackageName");
            currentPackageName.setAccessible(true);
            Object pkg = currentPackageName.invoke(null);
            if (pkg instanceof String) {
                return (String) pkg;
            }
        } catch (Throwable ignored) {
        }
        Application app = currentApplication();
        return app != null ? app.getPackageName() : null;
    }

    public static String currentProcessName() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentProcessName = activityThreadClass.getDeclaredMethod("currentProcessName");
            currentProcessName.setAccessible(true);
            Object proc = currentProcessName.invoke(null);
            if (proc instanceof String) {
                return (String) proc;
            }
        } catch (Throwable ignored) {
        }
        return currentPackageName();
    }
}
