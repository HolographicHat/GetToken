package hat.holo.token.models

import androidx.annotation.Keep
import com.google.gson.JsonElement
import hat.holo.token.utils.convertTo

@Keep
data class BaseResponse(
    val retcode: Int = 0,
    val message: String = "ok",
    val data: JsonElement? = null
) {
    inline fun <reified T : Any> data(): T {
        return data!!.convertTo()
    }
}
