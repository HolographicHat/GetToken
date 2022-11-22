package hat.holo.token.models

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class FetchRsp(
    val url: String
) {
    fun getUri(): Uri = Uri.parse(url)
}
