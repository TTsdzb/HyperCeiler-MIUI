package com.hchen.hooktool.tool;

import com.hchen.hooktool.HCInit;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.log.XposedLog;
import com.sevtinge.hyperceiler.xposedcompat.XC_MethodHook;
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge;
import com.sevtinge.hyperceiler.xposedcompat.XposedHelpers;
import com.sevtinge.hyperceiler.xposedcompat.callbacks.XC_LoadPackage;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CoreTool {
    private static final String LOG_TAG = "CoreTool";

    public static final class FindClassResult {
        private final Class<?> clazz;

        private FindClassResult(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Class<?> get() {
            return clazz;
        }

        public boolean exists() {
            return clazz != null;
        }
    }

    public static final class ChainTool {
        private static final class Entry {
            final String methodName;
            final boolean anyMethod;
            final Object[] parameterTypes;
            IHook hook;

            Entry(String methodName, boolean anyMethod, Object[] parameterTypes) {
                this.methodName = methodName;
                this.anyMethod = anyMethod;
                this.parameterTypes = parameterTypes;
            }
        }

        private final List<Entry> entries = new ArrayList<>();
        private Entry current;

        private ChainTool(String methodName, boolean anyMethod, Object[] parameterTypes) {
            addEntry(methodName, anyMethod, parameterTypes);
        }

        private void addEntry(String methodName, boolean anyMethod, Object[] parameterTypes) {
            current = new Entry(methodName, anyMethod, parameterTypes);
            entries.add(current);
        }

        public ChainTool method(String methodName, Object... parameterTypes) {
            addEntry(methodName, false, parameterTypes);
            return this;
        }

        public ChainTool anyMethod(String methodName) {
            addEntry(methodName, true, null);
            return this;
        }

        public ChainTool hook(IHook callback) {
            if (current != null) {
                current.hook = callback;
            }
            return this;
        }
    }

    private static ClassLoader defaultClassLoader() {
        XC_LoadPackage.LoadPackageParam lpp = HCInit.getLoadPackageParam();
        if (lpp != null && lpp.classLoader != null) {
            return lpp.classLoader;
        }
        return XposedBridge.BOOTCLASSLOADER;
    }

    public static FindClassResult findClass(String className) {
        return findClass(className, defaultClassLoader());
    }

    public static FindClassResult findClass(String className, ClassLoader classLoader) {
        if (className == null) {
            return new FindClassResult(null);
        }
        Class<?> cls = XposedHelpers.findClassIfExists(className, classLoader);
        return new FindClassResult(cls);
    }

    public static boolean existsClass(String className) {
        return findClass(className).exists();
    }

    public static boolean existsClass(String className, ClassLoader classLoader) {
        return findClass(className, classLoader).exists();
    }

    public static ChainTool method(String methodName, Object... parameterTypes) {
        return new ChainTool(methodName, false, parameterTypes);
    }

    public static ChainTool anyMethod(String methodName) {
        return new ChainTool(methodName, true, null);
    }

    public static void chain(String className, ChainTool chainTool) {
        chain(className, defaultClassLoader(), chainTool);
    }

    public static void chain(String className, ClassLoader classLoader, ChainTool chainTool) {
        Class<?> clazz = findClass(className, classLoader).get();
        if (clazz == null) {
            XposedLog.logE(LOG_TAG, "chain: class not found: " + className);
            return;
        }
        chain(clazz, chainTool);
    }

    public static void chain(Class<?> clazz, ChainTool chainTool) {
        if (clazz == null || chainTool == null) {
            return;
        }
        for (ChainTool.Entry entry : chainTool.entries) {
            if (entry.hook == null) {
                continue;
            }
            try {
                if (entry.anyMethod) {
                    hookAllMethod(clazz, entry.methodName, entry.hook);
                } else {
                    Object[] params = entry.parameterTypes;
                    Object[] paramsAndCb = new Object[(params != null ? params.length : 0) + 1];
                    if (params != null && params.length > 0) {
                        System.arraycopy(params, 0, paramsAndCb, 0, params.length);
                    }
                    paramsAndCb[paramsAndCb.length - 1] = entry.hook;
                    hookMethod(clazz, entry.methodName, paramsAndCb);
                }
            } catch (Throwable t) {
                XposedLog.logE(LOG_TAG, "chain hook failed on " + clazz.getName() + "#" + entry.methodName, t);
            }
        }
    }

    public static XC_MethodHook.Unhook hook(Member member, IHook callback) {
        if (member == null || callback == null) {
            XposedLog.logE(LOG_TAG, "hook: null member or callback");
            return null;
        }
        return XposedBridge.hookMethod(member, callback.toMethodHook());
    }

    public static XC_MethodHook.Unhook hook(Member member, XC_MethodHook callback) {
        if (member == null || callback == null) {
            XposedLog.logE(LOG_TAG, "hook: null member or callback");
            return null;
        }
        return XposedBridge.hookMethod(member, callback);
    }

    public static XC_MethodHook.Unhook hookMethod(Class<?> clazz, String methodName, Object... paramsAndCallback) {
        if (clazz == null) {
            XposedLog.logE(LOG_TAG, "hookMethod: clazz is null for " + methodName);
            return null;
        }
        try {
            Object[] converted = convertTrailingIHook(paramsAndCallback);
            return XposedHelpers.findAndHookMethod(clazz, methodName, converted);
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "hookMethod failed: " + clazz.getName() + "#" + methodName, t);
            return null;
        }
    }

    public static XC_MethodHook.Unhook hookMethod(String className, String methodName, Object... paramsAndCallback) {
        return hookMethod(className, defaultClassLoader(), methodName, paramsAndCallback);
    }

    public static XC_MethodHook.Unhook hookMethod(String className, ClassLoader classLoader, String methodName, Object... paramsAndCallback) {
        Class<?> clazz = findClass(className, classLoader).get();
        if (clazz == null) {
            XposedLog.logE(LOG_TAG, "hookMethod: class not found: " + className);
            return null;
        }
        return hookMethod(clazz, methodName, paramsAndCallback);
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethod(Class<?> clazz, String methodName, IHook callback) {
        if (clazz == null || callback == null) {
            XposedLog.logE(LOG_TAG, "hookAllMethod: null clazz or callback for " + methodName);
            return Collections.emptySet();
        }
        try {
            return XposedBridge.hookAllMethods(clazz, methodName, callback.toMethodHook());
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "hookAllMethod failed: " + clazz.getName() + "#" + methodName, t);
            return Collections.emptySet();
        }
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethod(String className, String methodName, IHook callback) {
        return hookAllMethod(className, defaultClassLoader(), methodName, callback);
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethod(String className, ClassLoader classLoader, String methodName, IHook callback) {
        Class<?> clazz = findClass(className, classLoader).get();
        if (clazz == null) {
            XposedLog.logE(LOG_TAG, "hookAllMethod: class not found: " + className);
            return Collections.emptySet();
        }
        return hookAllMethod(clazz, methodName, callback);
    }

    private static Object[] convertTrailingIHook(Object[] paramsAndCallback) {
        if (paramsAndCallback == null || paramsAndCallback.length == 0) {
            return paramsAndCallback;
        }
        Object last = paramsAndCallback[paramsAndCallback.length - 1];
        if (last instanceof IHook iHook) {
            Object[] copy = paramsAndCallback.clone();
            copy[copy.length - 1] = iHook.toMethodHook();
            return copy;
        }
        return paramsAndCallback;
    }

    public static IHook returnResult(final Object value) {
        return new IHook() {
            @Override
            public void before() {
                setResult(value);
            }
        };
    }

    public static IHook doNothing() {
        return new IHook() {
            @Override
            public void before() {
                returnNull();
            }
        };
    }

    public static Object getField(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        try {
            return XposedHelpers.getObjectField(obj, fieldName);
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "getField failed: " + obj.getClass().getName() + "#" + fieldName, t);
            return null;
        }
    }

    public static Object getField(Object obj, Field field) {
        if (obj == null || field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "getField(Field) failed: " + field.getName(), t);
            return null;
        }
    }

    public static boolean setField(Object obj, String fieldName, Object value) {
        if (obj == null) {
            return false;
        }
        try {
            XposedHelpers.setObjectField(obj, fieldName, value);
            return true;
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "setField failed: " + obj.getClass().getName() + "#" + fieldName, t);
            return false;
        }
    }

    public static boolean setField(Object obj, Field field, Object value) {
        if (obj == null || field == null) {
            return false;
        }
        try {
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "setField(Field) failed: " + field.getName(), t);
            return false;
        }
    }

    public static Object getStaticField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            return null;
        }
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "getStaticField failed: " + clazz.getName() + "#" + fieldName, t);
            return null;
        }
    }

    public static Object getStaticField(String className, String fieldName) {
        return getStaticField(findClass(className).get(), fieldName);
    }

    public static Object getStaticField(String className, ClassLoader classLoader, String fieldName) {
        return getStaticField(findClass(className, classLoader).get(), fieldName);
    }

    public static boolean setStaticField(Class<?> clazz, String fieldName, Object value) {
        if (clazz == null) {
            return false;
        }
        try {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
            return true;
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "setStaticField failed: " + clazz.getName() + "#" + fieldName, t);
            return false;
        }
    }

    public static boolean setStaticField(String className, String fieldName, Object value) {
        return setStaticField(findClass(className).get(), fieldName, value);
    }

    public static boolean setStaticField(String className, ClassLoader classLoader, String fieldName, Object value) {
        return setStaticField(findClass(className, classLoader).get(), fieldName, value);
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        if (obj == null) {
            return null;
        }
        try {
            return XposedHelpers.callMethod(obj, methodName, args);
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "callMethod failed: " + obj.getClass().getName() + "#" + methodName, t);
            return null;
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null) {
            return null;
        }
        try {
            return XposedHelpers.callStaticMethod(clazz, methodName, args);
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "callStaticMethod failed: " + clazz.getName() + "#" + methodName, t);
            return null;
        }
    }

    public static Object callStaticMethod(String className, String methodName, Object... args) {
        return callStaticMethod(findClass(className).get(), methodName, args);
    }

    public static Object callStaticMethod(String className, ClassLoader classLoader, String methodName, Object... args) {
        return callStaticMethod(findClass(className, classLoader).get(), methodName, args);
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        if (clazz == null) {
            return null;
        }
        try {
            return XposedHelpers.newInstance(clazz, args);
        } catch (Throwable t) {
            XposedLog.logE(LOG_TAG, "newInstance failed: " + clazz.getName(), t);
            return null;
        }
    }

    public static Object newInstance(String className, Object... args) {
        return newInstance(findClass(className).get(), args);
    }

    public static Object newInstance(String className, ClassLoader classLoader, Object... args) {
        return newInstance(findClass(className, classLoader).get(), args);
    }
}
