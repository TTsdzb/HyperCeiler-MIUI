package com.sevtinge.hyperceiler.xposedcompat;

import java.util.Objects;

import io.github.libxposed.api.XposedInterface;

public final class XposedCompatRuntime {
    private static volatile XposedInterface sInterface;

    private XposedCompatRuntime() {
    }

    public static void init(XposedInterface xposedInterface) {
        sInterface = Objects.requireNonNull(xposedInterface, "xposedInterface must not be null");
    }

    public static XposedInterface requireInterface() {
        XposedInterface xi = sInterface;
        if (xi == null) {
            throw new IllegalStateException("XposedCompatRuntime is not initialized");
        }
        return xi;
    }

    public static XposedInterface getInterfaceOrNull() {
        return sInterface;
    }
}
