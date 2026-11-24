package com.sevtinge.hyperceiler.xposedcompat;

public interface IXposedHookZygoteInit {
    void initZygote(StartupParam startupParam) throws Throwable;

    final class StartupParam {
        public String modulePath;
        public boolean startsSystemServer;

        public StartupParam() {
        }
    }
}
