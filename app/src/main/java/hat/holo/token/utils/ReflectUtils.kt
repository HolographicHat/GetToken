package hat.holo.token.utils

import java.lang.reflect.AccessibleObject

fun <T : AccessibleObject> T.setAccess() = apply {
    isAccessible = true
}

inline fun <reified R> Any.invokeMethod(methodName: String) : R {
    val method = this.javaClass.getDeclaredMethod(methodName)
    method.isAccessible = true
    return method.invoke(this) as R
}

inline fun <reified T, reified R> Any.invokeMethod(methodName: String, a1: T) : R {
    val method = this.javaClass.getDeclaredMethod(methodName, T::class.java)
    method.isAccessible = true
    return method.invoke(this, a1) as R
}

inline fun <reified T1, reified T2, reified R> Any.invokeMethod(methodName: String, a1: T1, a2: T2) : R {
    val method = this.javaClass.getDeclaredMethod(methodName, T1::class.java, T2::class.java)
    method.isAccessible = true
    return method.invoke(this, a1, a2) as R
}

inline fun <reified T> Any.visitField(fieldName: String) : T {
    val field = this.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this) as T
}

inline fun <reified T> Class<*>.visitStaticField(fieldName: String) : T {
    val field = getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(null) as T
}

inline fun <reified T, reified R> T.visitParentField(fieldName: String) : R {
    val field = T::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this) as R
}

inline fun <reified T> Any.visitAndSetField(fieldName: String, value: Any) : T {
    val field = this.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    val old = field.get(this) as T
    field.set(this, value)
    return old
}
