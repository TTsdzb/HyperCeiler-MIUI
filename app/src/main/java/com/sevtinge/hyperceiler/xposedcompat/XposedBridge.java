package com.sevtinge.hyperceiler.xposedcompat;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

public final class XposedBridge {
    private static final String TAG = "HyperCeiler";
    private static final Object[] EMPTY_ARGS = new Object[0];

    public static final ClassLoader BOOTCLASSLOADER = resolveBootClassLoader();

    private XposedBridge() {
    }

    private static ClassLoader resolveBootClassLoader() {
        try {
            ClassLoader cl = Class.forName("android.app.Activity").getClassLoader();
            if (cl != null) {
                return cl;
            }
        } catch (Throwable ignored) {
        }
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            if (cl != null) {
                return cl;
            }
        } catch (Throwable ignored) {
        }
        return XposedBridge.class.getClassLoader();
    }

    public static int getXposedVersion() {
        XposedInterface xi = XposedCompatRuntime.getInterfaceOrNull();
        return xi != null ? xi.getApiVersion() : 102;
    }

    public static synchronized void log(String text) {
        String safeText = text != null ? text : "null";
        XposedInterface xi = XposedCompatRuntime.getInterfaceOrNull();
        if (xi != null) {
            xi.log(Log.INFO, TAG, safeText);
        } else {
            Log.i(TAG, safeText);
        }
    }

    public static synchronized void log(Throwable t) {
        if (t == null) {
            log("null");
            return;
        }
        String msg = t.toString();
        XposedInterface xi = XposedCompatRuntime.getInterfaceOrNull();
        if (xi != null) {
            xi.log(Log.ERROR, TAG, msg, t);
        } else {
            Log.e(TAG, msg, t);
        }
    }

    public static boolean deoptimizeMethod(Member method) {
        if (!(method instanceof Executable)) {
            throw new IllegalArgumentException("Only methods and constructors can be deoptimized: " + method);
        }
        return XposedCompatRuntime.requireInterface().deoptimize((Executable) method);
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        XposedInterface xi = XposedCompatRuntime.requireInterface();
        Object[] safeArgs = args != null ? args : EMPTY_ARGS;
        if (method instanceof Method) {
            return xi.getInvoker((Method) method)
                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                    .invoke(thisObject, safeArgs);
        }
        if (method instanceof Constructor<?>) {
            XposedInterface.CtorInvoker<?> invoker = xi.getInvoker((Constructor<?>) method)
                    .setType(XposedInterface.Invoker.Type.ORIGIN);
            if (thisObject != null) {
                return invoker.invoke(thisObject, safeArgs);
            }
            try {
                return invoker.newInstance(safeArgs);
            } catch (InstantiationException e) {
                throw new InvocationTargetException(e);
            }
        }
        throw new IllegalArgumentException("method must be of type Method or Constructor: " + method);
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        if (hookMethod == null) {
            throw new IllegalArgumentException("hookMethod must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        if (!(hookMethod instanceof Executable)) {
            throw new IllegalArgumentException("Only methods and constructors can be hooked: " + hookMethod);
        }
        if (Modifier.isAbstract(hookMethod.getModifiers())) {
            throw new IllegalArgumentException("Cannot hook abstract methods: " + hookMethod);
        }

        XposedInterface xi = XposedCompatRuntime.requireInterface();
        XposedInterface.HookHandle handle = xi.hook((Executable) hookMethod)
                .setPriority(callback.priority)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
                    param.method = hookMethod;
                    Object origThis = chain.getThisObject();
                    param.thisObject = origThis;
                    Object[] origArgs = chain.getArgs().toArray(EMPTY_ARGS);
                    param.args = origArgs.clone();

                    try {
                        callback.beforeHookedMethod(param);
                    } catch (Throwable t) {
                        log(t);
                        param.resetResultAndReturnEarly();
                    }

                    if (!param.returnEarly) {
                        try {
                            boolean thisChanged = param.thisObject != origThis;
                            Object[] currentArgs = param.args;
                            boolean argsChanged = currentArgs == null || currentArgs.length != origArgs.length;
                            if (!argsChanged) {
                                for (int i = 0; i < origArgs.length; i++) {
                                    if (currentArgs[i] != origArgs[i]) {
                                        argsChanged = true;
                                        break;
                                    }
                                }
                            }

                            Object[] proceedArgs = currentArgs != null ? currentArgs : EMPTY_ARGS;
                            Object result;
                            if (thisChanged && argsChanged) {
                                result = chain.proceedWith(param.thisObject, proceedArgs);
                            } else if (thisChanged) {
                                result = chain.proceedWith(param.thisObject);
                            } else if (argsChanged) {
                                result = chain.proceed(proceedArgs);
                            } else {
                                result = chain.proceed();
                            }
                            param.setResult(result);
                        } catch (Throwable t) {
                            param.setThrowable(t);
                        }
                    }

                    Object lastResult = param.getResult();
                    Throwable lastThrowable = param.getThrowable();
                    boolean lastReturnEarly = param.returnEarly;
                    try {
                        callback.afterHookedMethod(param);
                    } catch (Throwable t) {
                        log(t);
                        param.restoreState(lastResult, lastThrowable, lastReturnEarly);
                    }

                    if (param.hasThrowable()) {
                        throw param.getThrowable();
                    }
                    return param.getResult();
                });

        return callback.new Unhook(hookMethod, callback, handle);
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        if (hookClass == null) {
            throw new IllegalArgumentException("hookClass must not be null");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("methodName must not be null");
        }
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Method method : hookClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && !Modifier.isAbstract(method.getModifiers())) {
                unhooks.add(hookMethod(method, callback));
            }
        }
        return unhooks;
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        if (hookClass == null) {
            throw new IllegalArgumentException("hookClass must not be null");
        }
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Constructor<?> constructor : hookClass.getDeclaredConstructors()) {
            if (!Modifier.isAbstract(constructor.getModifiers())) {
                unhooks.add(hookMethod(constructor, callback));
            }
        }
        return unhooks;
    }
}
