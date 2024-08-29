package hat.holo.token.utils

import android.annotation.SuppressLint
import android.content.Context
import hat.holo.token.models.DeviceInfo

@Suppress("MemberVisibilityCanBePrivate")
@SuppressLint("StaticFieldLeak")
object DeviceManager {

    private lateinit var context: Context
    private lateinit var deviceId: String
    private lateinit var riskManager: Any

    fun init(cl: ClassLoader, ctx: Context) {
        context = ctx
        riskManager = cl
            .loadClass("com.mihoyo.platform.account.sdk.risk.RiskManager")
            .visitStaticField<Any>("INSTANCE")
        val preDevice = ctx.getSharedPreferences("pre_device.xml", Context.MODE_PRIVATE)
        deviceId = preDevice.getString("device_id", "").toString()
    }

    val deviceFp get() = riskManager.invokeMethod<String>("getDeviceFp")

    val deviceInfo get() = DeviceInfo(deviceId, deviceFp)

}
