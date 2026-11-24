package com.github.kyuubiran.ezxhelper.finders

import com.github.kyuubiran.ezxhelper.ClassUtils
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class MethodFinder private constructor(
    private var seq: Sequence<Method>,
    private val sourceClass: Class<*>? = null
) {
    fun findSuper(isOnlySuper: Boolean = false): MethodFinder = apply {
        val cls = sourceClass ?: return@apply
        seq = sequence {
            var cur: Class<*>? = if (isOnlySuper) cls.superclass else cls
            while (cur != null) {
                yieldAll(cur.declaredMethods.iterator())
                cur = cur.superclass
            }
        }
    }

    inline fun filter(crossinline predicate: Method.() -> Boolean): MethodFinder = apply {
        filterSeq { predicate(it) }
    }

    @PublishedApi
    internal fun filterSeq(predicate: (Method) -> Boolean) {
        seq = seq.filter(predicate)
    }

    fun filterByName(name: String): MethodFinder = apply {
        seq = seq.filter { it.name == name }
    }

    fun filterByParamCount(count: Int): MethodFinder = apply {
        seq = seq.filter { it.parameterCount == count }
    }

    fun filterByParamCount(range: IntRange): MethodFinder = apply {
        seq = seq.filter { it.parameterCount in range }
    }

    inline fun filterByParamCount(crossinline predicate: (Int) -> Boolean): MethodFinder = apply {
        filterSeq { predicate(it.parameterCount) }
    }

    fun filterByParamTypes(vararg paramTypes: Class<*>?): MethodFinder = apply {
        seq = seq.filter { method ->
            val actual = method.parameterTypes
            if (actual.size != paramTypes.size) return@filter false
            for (i in actual.indices) {
                val expected = paramTypes[i]
                if (expected != null && actual[i] != expected) return@filter false
            }
            true
        }
    }

    inline fun filterByParamTypes(crossinline predicate: (Array<Class<*>>) -> Boolean): MethodFinder = apply {
        filterSeq { method ->
            try {
                predicate(method.parameterTypes)
            } catch (_: IndexOutOfBoundsException) {
                false
            }
        }
    }

    fun filterByReturnType(returnType: Class<*>): MethodFinder = apply {
        seq = seq.filter { it.returnType == returnType }
    }

    fun filterByAssignableReturnType(returnType: Class<*>): MethodFinder = apply {
        seq = seq.filter { returnType.isAssignableFrom(it.returnType) }
    }

    fun filterStatic(): MethodFinder = apply {
        seq = seq.filter { Modifier.isStatic(it.modifiers) }
    }

    fun filterNonStatic(): MethodFinder = apply {
        seq = seq.filter { !Modifier.isStatic(it.modifiers) }
    }

    fun filterFinal(): MethodFinder = apply {
        seq = seq.filter { Modifier.isFinal(it.modifiers) }
    }

    fun filterNonFinal(): MethodFinder = apply {
        seq = seq.filter { !Modifier.isFinal(it.modifiers) }
    }

    fun filterAbstract(): MethodFinder = apply {
        seq = seq.filter { Modifier.isAbstract(it.modifiers) }
    }

    fun filterNonAbstract(): MethodFinder = apply {
        seq = seq.filter { !Modifier.isAbstract(it.modifiers) }
    }

    fun filterPublic(): MethodFinder = apply {
        seq = seq.filter { Modifier.isPublic(it.modifiers) }
    }

    fun filterNonPublic(): MethodFinder = apply {
        seq = seq.filter { !Modifier.isPublic(it.modifiers) }
    }

    fun filterPrivate(): MethodFinder = apply {
        seq = seq.filter { Modifier.isPrivate(it.modifiers) }
    }

    fun filterProtected(): MethodFinder = apply {
        seq = seq.filter { Modifier.isProtected(it.modifiers) }
    }

    fun first(): Method = seq.first().apply { isAccessible = true }

    inline fun first(crossinline predicate: Method.() -> Boolean): Method =
        filter(predicate).first()

    fun firstOrNull(): Method? = seq.firstOrNull()?.apply { isAccessible = true }

    inline fun firstOrNull(crossinline predicate: Method.() -> Boolean): Method? =
        filter(predicate).firstOrNull()

    fun single(): Method = seq.single().apply { isAccessible = true }

    inline fun single(crossinline predicate: Method.() -> Boolean): Method =
        filter(predicate).single()

    fun singleOrNull(): Method? = seq.singleOrNull()?.apply { isAccessible = true }

    inline fun singleOrNull(crossinline predicate: Method.() -> Boolean): Method? =
        filter(predicate).singleOrNull()

    fun last(): Method = seq.last().apply { isAccessible = true }

    fun lastOrNull(): Method? = seq.lastOrNull()?.apply { isAccessible = true }

    fun toList(): List<Method> = seq.onEach { it.isAccessible = true }.toList()

    fun toSet(): Set<Method> = seq.onEach { it.isAccessible = true }.toSet()

    fun find(): List<Method> = toList()

    companion object `-Static` {
        @JvmStatic
        fun fromClass(clazz: Class<*>): MethodFinder =
            MethodFinder(clazz.declaredMethods.asSequence(), clazz)

        @JvmStatic
        @JvmOverloads
        fun fromClass(className: String, cl: ClassLoader? = null): MethodFinder =
            fromClass(ClassUtils.loadClass(className, cl))

        @JvmStatic
        fun fromArray(methods: Array<Method>): MethodFinder =
            MethodFinder(methods.asSequence())

        @JvmStatic
        fun fromSequence(seq: Sequence<Method>): MethodFinder =
            MethodFinder(seq)

        @JvmStatic
        fun fromIterable(iter: Iterable<Method>): MethodFinder =
            MethodFinder(iter.asSequence())

        fun Class<*>.methodFinder(): MethodFinder = fromClass(this)

        fun Array<Method>.methodFinder(): MethodFinder = fromArray(this)

        fun Iterable<Method>.methodFinder(): MethodFinder = fromIterable(this)

        fun Sequence<Method>.methodFinder(): MethodFinder = fromSequence(this)
    }
}
