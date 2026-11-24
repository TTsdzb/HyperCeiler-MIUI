package com.sevtinge.hyperceiler.xposedcompat;

import com.sevtinge.hyperceiler.xposedcompat.callbacks.XCallback;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;

public abstract class XC_MethodHook extends XCallback {
    public XC_MethodHook() {
        super();
    }

    public XC_MethodHook(int priority) {
        super(priority);
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;

        private Object result = null;
        private Throwable throwable = null;
        public boolean returnEarly = false;
        private Map<String, Object> extra = null;

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
            this.returnEarly = true;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean hasThrowable() {
            return throwable != null;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            this.result = null;
            this.returnEarly = true;
        }

        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null) {
                throw throwable;
            }
            return result;
        }

        public synchronized Object getObjectExtra(String key) {
            if (extra == null) {
                return null;
            }
            return extra.get(key);
        }

        public synchronized void setObjectExtra(String key, Object o) {
            if (extra == null) {
                extra = new HashMap<>();
            }
            extra.put(key, o);
        }

        void resetResultAndReturnEarly() {
            this.result = null;
            this.throwable = null;
            this.returnEarly = false;
        }

        void restoreState(Object result, Throwable throwable, boolean returnEarly) {
            this.result = result;
            this.throwable = throwable;
            this.returnEarly = returnEarly;
        }
    }

    public class Unhook {
        private final Member hookMethod;
        private final XC_MethodHook callback;
        private final XposedInterface.HookHandle handle;
        private final AtomicBoolean unhooked = new AtomicBoolean(false);

        public Unhook(Member hookMethod, XC_MethodHook callback, XposedInterface.HookHandle handle) {
            this.hookMethod = hookMethod;
            this.callback = callback;
            this.handle = handle;
        }

        public Member getHookedMethod() {
            return hookMethod;
        }

        public XC_MethodHook getCallback() {
            return callback;
        }

        public void unhook() {
            if (handle != null && unhooked.compareAndSet(false, true)) {
                handle.unhook();
            }
        }
    }
}
