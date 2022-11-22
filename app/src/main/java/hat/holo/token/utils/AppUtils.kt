package hat.holo.token.utils

import java.lang.reflect.Method

object AppUtils {

    private lateinit var instance: Any
    private val methodTable = arrayListOf<Method>()

    fun init(cl: ClassLoader) {
        val clz = cl.loadClass("com.mihoyo.hyperion.utils.AppUtils")
        instance = clz.getDeclaredField("INSTANCE").get(null)!!
        methodTable.add(clz.getDeclaredMethod("showToast", String::class.java))
    }

    fun showToast(str: String) {
        methodTable[0].invoke(instance, str)
    }
}
