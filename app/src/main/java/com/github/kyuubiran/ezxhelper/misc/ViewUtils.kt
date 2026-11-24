package com.github.kyuubiran.ezxhelper.misc

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.github.kyuubiran.ezxhelper.EzXHelper

object ViewUtils {
    @JvmStatic
    @JvmOverloads
    @SuppressLint("DiscouragedApi")
    fun getIdByName(name: String, context: Context = EzXHelper.appContext): Int {
        return context.resources.getIdentifier(name, "id", context.packageName)
    }

    @JvmStatic
    @SuppressLint("DiscouragedApi")
    fun View.findViewByIdName(name: String): View? {
        val ctx = context
        if (ctx != null) {
            val id = ctx.resources.getIdentifier(name, "id", ctx.packageName)
            if (id != 0) {
                findViewById<View?>(id)?.let { return it }
            }
        }
        if (EzXHelper.isAppContextInited && EzXHelper.appContext !== ctx) {
            val appCtx = EzXHelper.appContext
            val appId = appCtx.resources.getIdentifier(name, "id", appCtx.packageName)
            if (appId != 0) {
                return findViewById(appId)
            }
        }
        return null
    }
}
