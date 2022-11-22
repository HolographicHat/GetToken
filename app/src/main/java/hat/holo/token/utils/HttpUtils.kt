package hat.holo.token.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import hat.holo.token.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val gson by lazy { Gson() }

fun buildHttpRequest(block: Request.Builder.() -> Unit) = Request.Builder().apply(block).build()

fun <K,V> Map<K,V>.toJson() = gson.toJson(this)!!

fun JsonElement.toJson() = gson.toJson(this)!!

fun <K,V> Map<K,V>.post(builder: Request.Builder) {
    builder.post(toJson().toRequestBody("application/json".toMediaType()))
}

val defaultOkClient = OkHttpClient.Builder().apply {
    if (BuildConfig.DEBUG) {
        val i = HttpLoggingInterceptor {
            Log.d("LSPosed-Bridge", it)
        }
        i.level = HttpLoggingInterceptor.Level.BODY
        addInterceptor(i)
    }
}.build()

suspend inline fun <reified T : Any> Request.getAsJson(client: OkHttpClient = defaultOkClient): T {
    return Gson().fromJson(this@getAsJson.getAsText(client), T::class.java)!!
}

suspend fun Request.getAsText(client: OkHttpClient = defaultOkClient) = withContext(Dispatchers.IO) {
    client.newCall(this@getAsText).await().use { r->
        checkNotNull(r.body).use { b ->
            b.string()
        }
    }
}

inline fun <reified T: Any> JsonElement.convertTo(): T = Gson().fromJson(this, T::class.java)

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isCancelled) return
            continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation {
        try {
            cancel()
        } catch (_: Throwable) {
        }
    }
}
