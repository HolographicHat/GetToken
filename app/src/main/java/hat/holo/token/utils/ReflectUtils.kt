package hat.holo.token.utils

import java.lang.reflect.AccessibleObject

fun <T : AccessibleObject> T.setAccess() = apply {
    isAccessible = true
}
