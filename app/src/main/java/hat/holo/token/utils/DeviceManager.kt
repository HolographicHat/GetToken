package hat.holo.token.utils

import android.annotation.SuppressLint
import android.content.Context
import hat.holo.token.models.DeviceInfo

@Suppress("MemberVisibilityCanBePrivate")
@SuppressLint("StaticFieldLeak")
object DeviceManager {

    private lateinit var context: Context
    private lateinit var riskManager: Any
    private lateinit var deviceUtils: Any

    fun init(cl: ClassLoader, ctx: Context) {
        context = ctx
        riskManager = cl
            .loadClass("com.mihoyo.platform.account.sdk.risk.RiskManager")
            .visitStaticField<Any>("INSTANCE")
        deviceUtils = cl
            .loadClass("com.mihoyo.platform.account.sdk.utils.DeviceUtils")
            .visitStaticField<Any>("INSTANCE")
    }

    val deviceFp get() = riskManager.invokeMethod<String>("getDeviceFp")

    val deviceId get() = deviceUtils.invokeMethod<Context, String>("getDeviceID", context)

    val deviceInfo get() = DeviceInfo(deviceId, deviceFp)

}
