package com.github.kyuubiran.ezxhelper

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object ObjectUtils {
    private val PRIMITIVE_TO_WRAPPER = mapOf(
        Boolean::class.javaPrimitiveType!! to Boolean::class.javaObjectType,
        Byte::class.javaPrimitiveType!! to Byte::class.javaObjectType,
        Char::class.javaPrimitiveType!! to Char::class.javaObjectType,
        Short::class.javaPrimitiveType!! to Short::class.javaObjectType,
        Int::class.javaPrimitiveType!! to Integer::class.java,
        Long::class.javaPrimitiveType!! to Long::class.javaObjectType,
        Float::class.javaPrimitiveType!! to Float::class.javaObjectType,
        Double::class.javaPrimitiveType!! to Double::class.javaObjectType,
        Void.TYPE to Void::class.java
    )

    private val WRAPPER_TO_PRIMITIVE = PRIMITIVE_TO_WRAPPER.entries.associate { (k, v) -> v to k }

    @JvmStatic
    @JvmOverloads
    fun getObjectOrNull(obj: Any?, fieldName: String, fieldType: Class<*>? = null): Any? {
        return getObjectOrNullUntilSuperclass(obj, fieldName, fieldType)
    }

    @JvmStatic
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun <T> getObjectOrNullAs(obj: Any?, fieldName: String, fieldType: Class<*>? = null): T? {
        return getObjectOrNull(obj, fieldName, fieldType) as? T
    }

    @JvmStatic
    @JvmOverloads
    fun getObjectOrNullUntilSuperclass(obj: Any?, fieldName: String, fieldType: Class<*>? = null): Any? {
        if (obj == null) return null
        return try {
            val field = findInstanceFieldOrNull(obj.javaClass, fieldName, fieldType, walkSuper = true) ?: return null
            field.get(obj)
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun <T> getObjectOrNullUntilSuperclassAs(obj: Any?, fieldName: String, fieldType: Class<*>? = null): T? {
        return getObjectOrNullUntilSuperclass(obj, fieldName, fieldType) as? T
    }

    @JvmStatic
    @JvmOverloads
    fun setObject(obj: Any?, fieldName: String, value: Any?, fieldType: Class<*>? = null) {
        setObjectUntilSuperclass(obj, fieldName, value, fieldType)
    }

    @JvmStatic
    @JvmOverloads
    fun setObjectUntilSuperclass(obj: Any?, fieldName: String, value: Any?, fieldType: Class<*>? = null) {
        if (obj == null) throw IllegalArgumentException("obj must not be null")
        val field = findInstanceFieldOrNull(obj.javaClass, fieldName, fieldType, walkSuper = true)
            ?: throw NoSuchFieldException("${obj.javaClass.name}#$fieldName")
        field.set(obj, value)
    }

    @JvmStatic
    @JvmOverloads
    fun invokeMethodBestMatch(
        obj: Any?,
        methodName: String,
        returnType: Class<*>? = null,
        vararg args: Any?
    ): Any? {
        if (obj == null) throw IllegalArgumentException("obj must not be null")
        val method = findBestMatchMethod(obj.javaClass, methodName, returnType, isStatic = false, args = args)
            ?: throw NoSuchMethodException("${obj.javaClass.name}#$methodName(${args.joinToString { it?.javaClass?.name ?: "null" }})")
        return try {
            method.invoke(obj, *args)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    private fun findInstanceFieldOrNull(
        clazz: Class<*>,
        fieldName: String,
        fieldType: Class<*>?,
        walkSuper: Boolean
    ): Field? {
        var cur: Class<*>? = clazz
        while (cur != null) {
            for (f in cur.declaredFields) {
                if (!Modifier.isStatic(f.modifiers) && f.name == fieldName && (fieldType == null || f.type == fieldType)) {
                    f.isAccessible = true
                    return f
                }
            }
            if (!walkSuper) break
            cur = cur.superclass
        }
        return null
    }

    internal fun findBestMatchMethod(
        clazz: Class<*>,
        methodName: String,
        returnType: Class<*>?,
        isStatic: Boolean,
        args: Array<out Any?>
    ): Method? {
        var best: Method? = null
        var bestCost = Float.MAX_VALUE
        var cur: Class<*>? = clazz
        while (cur != null) {
            for (method in cur.declaredMethods) {
                if (method.name != methodName) continue
                if (Modifier.isStatic(method.modifiers) != isStatic) continue
                if (returnType != null && method.returnType != returnType) continue
                val paramTypes = method.parameterTypes
                if (paramTypes.size != args.size) continue
                val cost = computeMatchCost(args, paramTypes)
                if (cost < 0f) continue
                val finalCost = cost + if (method.isBridge) 0.05f else 0f
                if (best == null || finalCost < bestCost) {
                    best = method
                    bestCost = finalCost
                }
            }
            cur = cur.superclass
        }
        return best?.apply { isAccessible = true }
    }

    private fun computeMatchCost(args: Array<out Any?>, paramTypes: Array<Class<*>>): Float {
        var total = 0f
        for (i in paramTypes.indices) {
            val cost = getTypeCost(args[i]?.javaClass, paramTypes[i])
            if (cost < 0f) return -1f
            total += cost
        }
        return total
    }

    private fun getTypeCost(src: Class<*>?, dst: Class<*>): Float {
        if (src == dst) return 0f
        if (src == null) {
            if (dst.isPrimitive) return -1f
            return if (dst == Any::class.java) 0.9f else 0.5f
        }
        if (dst.isPrimitive) {
            val srcPrim = if (src.isPrimitive) src else WRAPPER_TO_PRIMITIVE[src] ?: return -1f
            if (srcPrim == dst) return 0.1f
            val widen = getPrimitiveWideningCost(srcPrim, dst)
            if (widen < 0f) return -1f
            return (if (src.isPrimitive) 0f else 0.1f) + widen
        }
        val effectiveSrc = if (src.isPrimitive) PRIMITIVE_TO_WRAPPER[src]!! else src
        if (effectiveSrc == dst) return 0.1f
        if (!dst.isAssignableFrom(effectiveSrc)) return -1f
        val base = if (src.isPrimitive) 0.2f else 0f
        if (dst.isInterface) return base + 1.5f
        var depth = 0
        var cur: Class<*>? = effectiveSrc
        while (cur != null && cur != dst) {
            depth++
            cur = cur.superclass
        }
        return base + if (cur == dst) depth.toFloat() else 10f
    }

    private fun getPrimitiveWideningCost(src: Class<*>, dst: Class<*>): Float {
        if (src == Boolean::class.javaPrimitiveType || dst == Boolean::class.javaPrimitiveType ||
            src == Void.TYPE || dst == Void.TYPE
        ) return -1f
        if (src == dst) return 0f
        var steps = 0
        var cur: Class<*>? = src
        while (cur != null && cur != dst) {
            steps++
            cur = when (cur) {
                Byte::class.javaPrimitiveType -> Short::class.javaPrimitiveType
                Short::class.javaPrimitiveType, Char::class.javaPrimitiveType -> Int::class.javaPrimitiveType
                Int::class.javaPrimitiveType -> Long::class.javaPrimitiveType
                Long::class.javaPrimitiveType -> Float::class.javaPrimitiveType
                Float::class.javaPrimitiveType -> Double::class.javaPrimitiveType
                else -> null
            }
        }
        return if (cur == dst) steps * 0.2f else -1f
    }
}
