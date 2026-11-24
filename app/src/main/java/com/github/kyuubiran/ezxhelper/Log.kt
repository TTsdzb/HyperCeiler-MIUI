package com.github.kyuubiran.ezxhelper

import com.sevtinge.hyperceiler.xposedcompat.XposedBridge

object Log {
    @JvmStatic
    var currentLogTag: String = "EzXHelper"

    @JvmStatic
    @JvmOverloads
    fun d(msg: String, thr: Throwable? = null) {
        XposedBridge.log("[$currentLogTag][D]: $msg")
        if (thr != null) XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun d(thr: Throwable, msg: String = "") {
        if (msg.isNotEmpty()) XposedBridge.log("[$currentLogTag][D]: $msg")
        XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun i(msg: String, thr: Throwable? = null) {
        XposedBridge.log("[$currentLogTag][I]: $msg")
        if (thr != null) XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun i(thr: Throwable, msg: String = "") {
        if (msg.isNotEmpty()) XposedBridge.log("[$currentLogTag][I]: $msg")
        XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun w(msg: String, thr: Throwable? = null) {
        XposedBridge.log("[$currentLogTag][W]: $msg")
        if (thr != null) XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun w(thr: Throwable, msg: String = "") {
        if (msg.isNotEmpty()) XposedBridge.log("[$currentLogTag][W]: $msg")
        XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun e(msg: String, thr: Throwable? = null) {
        XposedBridge.log("[$currentLogTag][E]: $msg")
        if (thr != null) XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun e(thr: Throwable, msg: String = "") {
        if (msg.isNotEmpty()) XposedBridge.log("[$currentLogTag][E]: $msg")
        XposedBridge.log(thr)
    }

    @JvmStatic
    @JvmOverloads
    fun ex(msg: String, thr: Throwable? = null) {
        e(msg, thr)
    }

    @JvmStatic
    @JvmOverloads
    fun ex(thr: Throwable, msg: String = "") {
        e(thr, msg)
    }
}
