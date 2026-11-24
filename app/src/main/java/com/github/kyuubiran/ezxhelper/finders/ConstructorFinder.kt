package com.github.kyuubiran.ezxhelper.finders

import com.github.kyuubiran.ezxhelper.ClassUtils
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

class ConstructorFinder private constructor(
    private var seq: Sequence<Constructor<*>>
) {
    inline fun filter(crossinline predicate: Constructor<*>.() -> Boolean): ConstructorFinder = apply {
        filterSeq { predicate(it) }
    }

    @PublishedApi
    internal fun filterSeq(predicate: (Constructor<*>) -> Boolean) {
        seq = seq.filter(predicate)
    }

    fun filterByParamCount(count: Int): ConstructorFinder = apply {
        seq = seq.filter { it.parameterCount == count }
    }

    fun filterByParamCount(range: IntRange): ConstructorFinder = apply {
        seq = seq.filter { it.parameterCount in range }
    }

    inline fun filterByParamCount(crossinline predicate: (Int) -> Boolean): ConstructorFinder = apply {
        filterSeq { predicate(it.parameterCount) }
    }

    fun filterByParamTypes(vararg paramTypes: Class<*>?): ConstructorFinder = apply {
        seq = seq.filter { ctor ->
            val actual = ctor.parameterTypes
            if (actual.size != paramTypes.size) return@filter false
            for (i in actual.indices) {
                val expected = paramTypes[i]
                if (expected != null && actual[i] != expected) return@filter false
            }
            true
        }
    }

    inline fun filterByParamTypes(crossinline predicate: (Array<Class<*>>) -> Boolean): ConstructorFinder = apply {
        filterSeq { ctor ->
            try {
                predicate(ctor.parameterTypes)
            } catch (_: IndexOutOfBoundsException) {
                false
            }
        }
    }

    fun filterPublic(): ConstructorFinder = apply {
        seq = seq.filter { Modifier.isPublic(it.modifiers) }
    }

    fun filterNonPublic(): ConstructorFinder = apply {
        seq = seq.filter { !Modifier.isPublic(it.modifiers) }
    }

    fun filterPrivate(): ConstructorFinder = apply {
        seq = seq.filter { Modifier.isPrivate(it.modifiers) }
    }

    fun filterProtected(): ConstructorFinder = apply {
        seq = seq.filter { Modifier.isProtected(it.modifiers) }
    }

    fun first(): Constructor<*> = seq.first().apply { isAccessible = true }

    inline fun first(crossinline predicate: Constructor<*>.() -> Boolean): Constructor<*> =
        filter(predicate).first()

    fun firstOrNull(): Constructor<*>? = seq.firstOrNull()?.apply { isAccessible = true }

    inline fun firstOrNull(crossinline predicate: Constructor<*>.() -> Boolean): Constructor<*>? =
        filter(predicate).firstOrNull()

    fun single(): Constructor<*> = seq.single().apply { isAccessible = true }

    inline fun single(crossinline predicate: Constructor<*>.() -> Boolean): Constructor<*> =
        filter(predicate).single()

    fun singleOrNull(): Constructor<*>? = seq.singleOrNull()?.apply { isAccessible = true }

    inline fun singleOrNull(crossinline predicate: Constructor<*>.() -> Boolean): Constructor<*>? =
        filter(predicate).singleOrNull()

    fun last(): Constructor<*> = seq.last().apply { isAccessible = true }

    fun lastOrNull(): Constructor<*>? = seq.lastOrNull()?.apply { isAccessible = true }

    fun toList(): List<Constructor<*>> = seq.onEach { it.isAccessible = true }.toList()

    fun toSet(): Set<Constructor<*>> = seq.onEach { it.isAccessible = true }.toSet()

    fun find(): List<Constructor<*>> = toList()

    companion object `-Static` {
        @JvmStatic
        fun fromClass(clazz: Class<*>): ConstructorFinder =
            ConstructorFinder(clazz.declaredConstructors.asSequence())

        @JvmStatic
        @JvmOverloads
        fun fromClass(className: String, cl: ClassLoader? = null): ConstructorFinder =
            fromClass(ClassUtils.loadClass(className, cl))

        @JvmStatic
        fun fromArray(ctors: Array<Constructor<*>>): ConstructorFinder =
            ConstructorFinder(ctors.asSequence())

        @JvmStatic
        fun fromSequence(seq: Sequence<Constructor<*>>): ConstructorFinder =
            ConstructorFinder(seq)

        @JvmStatic
        fun fromIterable(iter: Iterable<Constructor<*>>): ConstructorFinder =
            ConstructorFinder(iter.asSequence())

        fun Class<*>.constructorFinder(): ConstructorFinder = fromClass(this)

        fun Array<Constructor<*>>.constructorFinder(): ConstructorFinder = fromArray(this)

        fun Iterable<Constructor<*>>.constructorFinder(): ConstructorFinder = fromIterable(this)

        fun Sequence<Constructor<*>>.constructorFinder(): ConstructorFinder = fromSequence(this)
    }
}
