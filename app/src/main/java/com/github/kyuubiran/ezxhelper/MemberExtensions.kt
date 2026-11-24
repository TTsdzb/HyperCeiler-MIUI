package com.github.kyuubiran.ezxhelper

import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Modifier

object MemberExtensions {
    inline val Member.isStatic: Boolean
        get() = Modifier.isStatic(modifiers)

    inline val Member.isNotStatic: Boolean
        get() = !isStatic

    inline val Member.isFinal: Boolean
        get() = Modifier.isFinal(modifiers)

    inline val Member.isNotFinal: Boolean
        get() = !isFinal

    inline val Member.isPublic: Boolean
        get() = Modifier.isPublic(modifiers)

    inline val Member.isPrivate: Boolean
        get() = Modifier.isPrivate(modifiers)

    inline val Member.isProtected: Boolean
        get() = Modifier.isProtected(modifiers)

    inline val Member.isAbstract: Boolean
        get() = Modifier.isAbstract(modifiers)

    inline val Member.isNative: Boolean
        get() = Modifier.isNative(modifiers)

    inline val Member.paramCount: Int
        get() = if (this is Executable) parameterCount else 0
}
