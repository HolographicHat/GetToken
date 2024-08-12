package hat.holo.token.utils

object AppUtils {

    private lateinit var inst: Any

    fun init(cl: ClassLoader) {
        inst = cl.loadClass("com.mihoyo.hyperion.utils.AppUtils").visitStaticField<Any>("INSTANCE")
    }

    fun showToast(str: String) = inst.invokeMethod<String, Unit?>("showToast", str)

}
