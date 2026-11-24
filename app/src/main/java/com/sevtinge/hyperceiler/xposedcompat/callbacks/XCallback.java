package com.sevtinge.hyperceiler.xposedcompat.callbacks;

import io.github.libxposed.api.XposedInterface;

public abstract class XCallback {
    public static final int PRIORITY_DEFAULT = XposedInterface.PRIORITY_DEFAULT;
    public static final int PRIORITY_LOWEST = XposedInterface.PRIORITY_LOWEST;
    public static final int PRIORITY_HIGHEST = XposedInterface.PRIORITY_HIGHEST;

    public final int priority;

    public XCallback() {
        this(PRIORITY_DEFAULT);
    }

    public XCallback(int priority) {
        this.priority = priority;
    }
}
