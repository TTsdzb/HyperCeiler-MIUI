package com.github.kyuubiran.ezxhelper.finders

import com.github.kyuubiran.ezxhelper.ClassUtils
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class FieldFinder private constructor(
    private var seq: Sequence<Field>,
    private val sourceClass: Class<*>? = null
) {
    fun findSuper(isOnlySuper: Boolean = false): FieldFinder = apply {
        val cls = sourceClass ?: return@apply
        seq = sequence {
            var cur: Class<*>? = if (isOnlySuper) cls.superclass else cls
            while (cur != null) {
                yieldAll(cur.declaredFields.iterator())
                cur = cur.superclass
            }
        }
    }

    inline fun filter(crossinline predicate: Field.() -> Boolean): FieldFinder = apply {
        filterSeq { predicate(it) }
    }

    @PublishedApi
    internal fun filterSeq(predicate: (Field) -> Boolean) {
        seq = seq.filter(predicate)
    }

    fun filterByName(name: String): FieldFinder = apply {
        seq = seq.filter { it.name == name }
    }

    fun filterByType(type: Class<*>): FieldFinder = apply {
        seq = seq.filter { it.type == type }
    }

    fun filterByAssignableType(type: Class<*>): FieldFinder = apply {
        seq = seq.filter { type.isAssignableFrom(it.type) }
    }

    fun filterStatic(): FieldFinder = apply {
        seq = seq.filter { Modifier.isStatic(it.modifiers) }
    }

    fun filterNonStatic(): FieldFinder = apply {
        seq = seq.filter { !Modifier.isStatic(it.modifiers) }
    }

    fun filterFinal(): FieldFinder = apply {
        seq = seq.filter { Modifier.isFinal(it.modifiers) }
    }

    fun filterNonFinal(): FieldFinder = apply {
        seq = seq.filter { !Modifier.isFinal(it.modifiers) }
    }

    fun filterPublic(): FieldFinder = apply {
        seq = seq.filter { Modifier.isPublic(it.modifiers) }
    }

    fun filterNonPublic(): FieldFinder = apply {
        seq = seq.filter { !Modifier.isPublic(it.modifiers) }
    }

    fun first(): Field = seq.first().apply { isAccessible = true }

    inline fun first(crossinline predicate: Field.() -> Boolean): Field =
        filter(predicate).first()

    fun firstOrNull(): Field? = seq.firstOrNull()?.apply { isAccessible = true }

    inline fun firstOrNull(crossinline predicate: Field.() -> Boolean): Field? =
        filter(predicate).firstOrNull()

    fun single(): Field = seq.single().apply { isAccessible = true }

    inline fun single(crossinline predicate: Field.() -> Boolean): Field =
        filter(predicate).single()

    fun singleOrNull(): Field? = seq.singleOrNull()?.apply { isAccessible = true }

    inline fun singleOrNull(crossinline predicate: Field.() -> Boolean): Field? =
        filter(predicate).singleOrNull()

    fun last(): Field = seq.last().apply { isAccessible = true }

    fun lastOrNull(): Field? = seq.lastOrNull()?.apply { isAccessible = true }

    fun toList(): List<Field> = seq.onEach { it.isAccessible = true }.toList()

    fun toSet(): Set<Field> = seq.onEach { it.isAccessible = true }.toSet()

    fun find(): List<Field> = toList()

    companion object `-Static` {
        @JvmStatic
        fun fromClass(clazz: Class<*>): FieldFinder =
            FieldFinder(clazz.declaredFields.asSequence(), clazz)

        @JvmStatic
        @JvmOverloads
        fun fromClass(className: String, cl: ClassLoader? = null): FieldFinder =
            fromClass(ClassUtils.loadClass(className, cl))

        @JvmStatic
        fun fromArray(fields: Array<Field>): FieldFinder =
            FieldFinder(fields.asSequence())

        @JvmStatic
        fun fromSequence(seq: Sequence<Field>): FieldFinder =
            FieldFinder(seq)

        @JvmStatic
        fun fromIterable(iter: Iterable<Field>): FieldFinder =
            FieldFinder(iter.asSequence())

        fun Class<*>.fieldFinder(): FieldFinder = fromClass(this)

        fun Array<Field>.fieldFinder(): FieldFinder = fromArray(this)

        fun Iterable<Field>.fieldFinder(): FieldFinder = fromIterable(this)

        fun Sequence<Field>.fieldFinder(): FieldFinder = fromSequence(this)
    }
}
