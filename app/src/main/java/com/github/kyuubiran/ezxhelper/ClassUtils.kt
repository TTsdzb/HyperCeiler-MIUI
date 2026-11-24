package com.github.kyuubiran.ezxhelper

import com.sevtinge.hyperceiler.xposedcompat.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object ClassUtils {
    @JvmStatic
    @JvmOverloads
    @Throws(ClassNotFoundException::class)
    fun loadClass(className: String, cl: ClassLoader? = null): Class<*> {
        val targetLoader = cl ?: EzXHelper.classLoader
        return try {
            XposedHelpers.findClass(className, targetLoader)
        } catch (e: XposedHelpers.ClassNotFoundError) {
            val cause = e.cause
            if (cause is ClassNotFoundException) throw cause
            throw ClassNotFoundException(className, e)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun loadClassOrNull(className: String, cl: ClassLoader? = null): Class<*>? {
        return try {
            loadClass(className, cl)
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    @Throws(ClassNotFoundException::class)
    fun loadFirstClass(vararg classNames: String): Class<*> = loadFirstClass(null, *classNames)

    @JvmStatic
    @Throws(ClassNotFoundException::class)
    fun loadFirstClass(cl: ClassLoader?, vararg classNames: String): Class<*> {
        for (name in classNames) {
            val cls = loadClassOrNull(name, cl)
            if (cls != null) return cls
        }
        throw ClassNotFoundException("None of classes found: ${classNames.joinToString()}")
    }

    @JvmStatic
    fun loadFirstClassOrNull(vararg classNames: String): Class<*>? = loadFirstClassOrNull(null, *classNames)

    @JvmStatic
    fun loadFirstClassOrNull(cl: ClassLoader?, vararg classNames: String): Class<*>? {
        for (name in classNames) {
            val cls = loadClassOrNull(name, cl)
            if (cls != null) return cls
        }
        return null
    }

    @JvmStatic
    @JvmOverloads
    fun getStaticObjectOrNull(clazz: Class<*>?, fieldName: String, fieldType: Class<*>? = null): Any? {
        if (clazz == null) return null
        return try {
            val field = findStaticFieldOrNull(clazz, fieldName, fieldType) ?: return null
            field.get(null)
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun <T> getStaticObjectOrNullAs(clazz: Class<*>?, fieldName: String, fieldType: Class<*>? = null): T? {
        return getStaticObjectOrNull(clazz, fieldName, fieldType) as? T
    }

    @JvmStatic
    @JvmOverloads
    fun setStaticObject(clazz: Class<*>?, fieldName: String, value: Any?, fieldType: Class<*>? = null) {
        if (clazz == null) throw IllegalArgumentException("clazz must not be null")
        val field = findStaticFieldOrNull(clazz, fieldName, fieldType)
            ?: throw NoSuchFieldException("${clazz.name}#$fieldName")
        field.set(null, value)
    }

    @JvmStatic
    @JvmOverloads
    fun invokeStaticMethodBestMatch(
        clazz: Class<*>?,
        methodName: String,
        returnType: Class<*>? = null,
        vararg args: Any?
    ): Any? {
        if (clazz == null) throw IllegalArgumentException("clazz must not be null")
        val method = ObjectUtils.findBestMatchMethod(clazz, methodName, returnType, isStatic = true, args = args)
            ?: throw NoSuchMethodException("${clazz.name}#$methodName(${args.joinToString { it?.javaClass?.name ?: "null" }})")
        return try {
            method.invoke(null, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    private fun findStaticFieldOrNull(clazz: Class<*>, fieldName: String, fieldType: Class<*>?): Field? {
        var cur: Class<*>? = clazz
        while (cur != null) {
            for (f in cur.declaredFields) {
                if (Modifier.isStatic(f.modifiers) && f.name == fieldName && (fieldType == null || f.type == fieldType)) {
                    f.isAccessible = true
                    return f
                }
            }
            cur = cur.superclass
        }
        return null
    }
}
