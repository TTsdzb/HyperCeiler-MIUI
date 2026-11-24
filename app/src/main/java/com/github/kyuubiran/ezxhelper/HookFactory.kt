package com.github.kyuubiran.ezxhelper

import com.github.kyuubiran.ezxhelper.interfaces.IMethodHookCallback
import com.sevtinge.hyperceiler.xposedcompat.XC_MethodHook
import com.sevtinge.hyperceiler.xposedcompat.XposedBridge
import com.sevtinge.hyperceiler.xposedcompat.callbacks.XCallback
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.function.Consumer

class HookFactory private constructor(private val target: Member) {
    private var beforeHook: IMethodHookCallback? = null
    private var afterHook: IMethodHookCallback? = null

    fun before(callback: IMethodHookCallback) {
        beforeHook = callback
    }

    fun after(callback: IMethodHookCallback) {
        afterHook = callback
    }

    fun replace(callback: (XC_MethodHook.MethodHookParam) -> Any?) {
        beforeHook = IMethodHookCallback { param ->
            try {
                val res = callback(param)
                param.result = if (res === Unit && (target as? Method)?.returnType == Void.TYPE) null else res
            } catch (t: Throwable) {
                Log.e(t)
            }
        }
        afterHook = null
    }

    fun returnConstant(result: Any?) {
        beforeHook = IMethodHookCallback { param ->
            param.result = result
        }
        afterHook = null
    }

    private fun hook(priority: Int): XC_MethodHook.Unhook {
        val beforeCb = beforeHook
        val afterCb = afterHook
        return XposedBridge.hookMethod(target, object : XC_MethodHook(priority) {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (beforeCb != null) {
                    try {
                        beforeCb.onMethodHooked(param)
                    } catch (t: Throwable) {
                        Log.e(t)
                    }
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                if (afterCb != null) {
                    try {
                        afterCb.onMethodHooked(param)
                    } catch (t: Throwable) {
                        Log.e(t)
                    }
                }
            }
        })
    }

    companion object `-Static` {
        @JvmStatic
        @JvmOverloads
        fun Member.createHook(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): XC_MethodHook.Unhook {
            return HookFactory(this).apply(block).hook(priority)
        }

        @JvmStatic
        @JvmOverloads
        fun Iterable<Member>.createHooks(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): List<XC_MethodHook.Unhook> {
            return filter { !Modifier.isAbstract(it.modifiers) }
                .map { it.createHook(priority, block) }
        }

        @JvmStatic
        @JvmOverloads
        fun Array<out Member>.createHooks(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: HookFactory.() -> Unit
        ): List<XC_MethodHook.Unhook> {
            return filter { !Modifier.isAbstract(it.modifiers) }
                .map { it.createHook(priority, block) }
        }

        @JvmStatic
        @JvmOverloads
        fun Member.createBeforeHook(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: (XC_MethodHook.MethodHookParam) -> Unit
        ): XC_MethodHook.Unhook {
            return createHook(priority) { before(block) }
        }

        @JvmStatic
        @JvmOverloads
        fun Iterable<Member>.createBeforeHooks(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: (XC_MethodHook.MethodHookParam) -> Unit
        ): List<XC_MethodHook.Unhook> {
            return createHooks(priority) { before(block) }
        }

        @JvmStatic
        @JvmOverloads
        fun Array<out Member>.createBeforeHooks(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: (XC_MethodHook.MethodHookParam) -> Unit
        ): List<XC_MethodHook.Unhook> {
            return createHooks(priority) { before(block) }
        }

        @JvmStatic
        @JvmOverloads
        fun Member.createAfterHook(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: (XC_MethodHook.MethodHookParam) -> Unit
        ): XC_MethodHook.Unhook {
            return createHook(priority) { after(block) }
        }

        @JvmStatic
        @JvmOverloads
        fun Iterable<Member>.createAfterHooks(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: (XC_MethodHook.MethodHookParam) -> Unit
        ): List<XC_MethodHook.Unhook> {
            return createHooks(priority) { after(block) }
        }

        @JvmStatic
        @JvmOverloads
        fun Array<out Member>.createAfterHooks(
            priority: Int = XCallback.PRIORITY_DEFAULT,
            block: (XC_MethodHook.MethodHookParam) -> Unit
        ): List<XC_MethodHook.Unhook> {
            return createHooks(priority) { after(block) }
        }

        @JvmStatic
        @JvmOverloads
        fun createMethodHook(
            member: Member?,
            priority: Int = XCallback.PRIORITY_DEFAULT,
            consumer: Consumer<HookFactory>
        ): XC_MethodHook.Unhook? {
            if (member == null) {
                Log.e("HookFactory.createMethodHook called with null member")
                return null
            }
            val factory = HookFactory(member)
            consumer.accept(factory)
            return factory.hook(priority)
        }
    }
}
