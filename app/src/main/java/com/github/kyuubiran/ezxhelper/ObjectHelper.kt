package com.github.kyuubiran.ezxhelper

class ObjectHelper(val obj: Any?) {
    fun getObjectOrNull(fieldName: String, fieldType: Class<*>? = null): Any? =
        ObjectUtils.getObjectOrNull(obj, fieldName, fieldType)

    fun <T> getObjectOrNullAs(fieldName: String, fieldType: Class<*>? = null): T? =
        ObjectUtils.getObjectOrNullAs(obj, fieldName, fieldType)

    fun getObjectOrNullUntilSuperclass(fieldName: String, fieldType: Class<*>? = null): Any? =
        ObjectUtils.getObjectOrNullUntilSuperclass(obj, fieldName, fieldType)

    fun <T> getObjectOrNullUntilSuperclassAs(fieldName: String, fieldType: Class<*>? = null): T? =
        ObjectUtils.getObjectOrNullUntilSuperclassAs(obj, fieldName, fieldType)

    fun setObject(fieldName: String, value: Any?, fieldType: Class<*>? = null) =
        ObjectUtils.setObject(obj, fieldName, value, fieldType)

    fun setObjectUntilSuperclass(fieldName: String, value: Any?, fieldType: Class<*>? = null) =
        ObjectUtils.setObjectUntilSuperclass(obj, fieldName, value, fieldType)

    fun invokeMethodBestMatch(methodName: String, returnType: Class<*>? = null, vararg args: Any?): Any? =
        ObjectUtils.invokeMethodBestMatch(obj, methodName, returnType, *args)

    companion object {
        @JvmStatic
        fun Any?.objectHelper(): ObjectHelper = ObjectHelper(this)

        inline fun Any?.objectHelper(block: ObjectHelper.() -> Unit): ObjectHelper =
            ObjectHelper(this).apply(block)
    }
}
