package com.hchen.hooktool.tool;

import com.sevtinge.hyperceiler.xposedcompat.XC_MethodHook;
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

public class ParamTool {
    private final ThreadLocal<XC_MethodHook.MethodHookParam> currentParam = new ThreadLocal<>();

    public final XC_MethodHook.MethodHookParam swapParam(XC_MethodHook.MethodHookParam next) {
        XC_MethodHook.MethodHookParam prev = currentParam.get();
        if (next == null) {
            currentParam.remove();
        } else {
            currentParam.set(next);
        }
        return prev;
    }

    protected final XC_MethodHook.MethodHookParam requireParam() {
        XC_MethodHook.MethodHookParam p = currentParam.get();
        if (p == null) {
            throw new IllegalStateException("MethodHookParam is not active on the current thread");
        }
        return p;
    }

    public Object thisObject() {
        return requireParam().thisObject;
    }

    public Member getMethod() {
        return requireParam().method;
    }

    public Object[] getArgs() {
        return requireParam().args;
    }

    public Object getArgs(int index) {
        return requireParam().args[index];
    }

    public void setArgs(int index, Object value) {
        requireParam().args[index] = value;
    }

    public Object getResult() {
        return requireParam().getResult();
    }

    public void setResult(Object result) {
        requireParam().setResult(result);
    }

    public void returnNull() {
        requireParam().setResult(null);
    }

    public Throwable getThrowable() {
        return requireParam().getThrowable();
    }

    public void setThrowable(Throwable throwable) {
        requireParam().setThrowable(throwable);
    }

    public boolean hasThrowable() {
        return requireParam().hasThrowable();
    }

    public Object getThisField(String fieldName) {
        return CoreTool.getField(thisObject(), fieldName);
    }

    public boolean setThisField(String fieldName, Object value) {
        return CoreTool.setField(thisObject(), fieldName, value);
    }

    public Object callThisMethod(String methodName, Object... args) {
        return CoreTool.callMethod(thisObject(), methodName, args);
    }

    public Object getObjectExtra(String key) {
        return requireParam().getObjectExtra(key);
    }

    public void setObjectExtra(String key, Object value) {
        requireParam().setObjectExtra(key, value);
    }

    public Object invokeOriginalMethod() throws InvocationTargetException, IllegalAccessException {
        XC_MethodHook.MethodHookParam p = requireParam();
        return XposedBridge.invokeOriginalMethod(p.method, p.thisObject, p.args);
    }
}
