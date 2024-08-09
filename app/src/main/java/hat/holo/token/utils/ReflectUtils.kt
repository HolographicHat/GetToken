package hat.holo.token.utils

import java.lang.reflect.AccessibleObject

fun <T : AccessibleObject> T.setAccess() = apply {
    isAccessible = true
}

@Suppress("UNCHECKED_CAST")
fun <R> Any.invokeMethod(methodName: String, vararg args: Any?) : R {
    val method = this.javaClass.getDeclaredMethod(methodName)
    method.isAccessible = true
    return method.invoke(this, *args) as R
}

inline fun <reified T> Any.visitField(fieldName: String) : T {
    val field = this.javaClass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this) as T
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
