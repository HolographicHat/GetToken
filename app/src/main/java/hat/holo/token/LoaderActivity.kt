package hat.holo.token

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.ArrayMap
import dalvik.system.DexClassLoader
import hat.holo.token.utils.setAccess
import java.lang.ref.WeakReference

class LoaderActivity : Activity() {

    private var originClassLoader: ClassLoader? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dexPath = intent.getStringExtra("dexPath")
        val loader = DexClassLoader(dexPath, cacheDir.absolutePath, null, null)
        originClassLoader = setClassLoader(loader)
        val zActivity = loader.loadClass("hat.holo.token.TokenActivity")
        val actIntent = Intent(this, zActivity)
        actIntent.putExtra("accountInfo", intent.getSerializableExtra("accountInfo"))
        startActivityForResult(actIntent, 1234)
    }

    // platform/frameworks/base/+/master/core/java/android/app/Activity.java
    // platform/frameworks/base/+/master/core/java/android/app/LoadedApk.java
    // platform/frameworks/base/+/master/core/java/android/app/ActivityThread.java
    @SuppressLint("DiscouragedPrivateApi")
    private fun setClassLoader(classLoader: ClassLoader): ClassLoader {
        val mainThread = Activity::class.java.getDeclaredField("mMainThread").setAccess().get(this)
        val apks = mainThread.javaClass.getDeclaredField("mPackages").setAccess().get(mainThread) as ArrayMap<*, *>
        val apkRef = apks[packageName] as WeakReference<*>
        val loadedApk = apkRef.get() ?: throw IllegalStateException("WeakRef<LoadedApk> is null!")
        val fClassLoader = loadedApk.javaClass.getDeclaredField("mClassLoader").setAccess()
        val oClassLoader = fClassLoader.get(loadedApk)
        fClassLoader.set(loadedApk, classLoader)
        return oClassLoader as ClassLoader
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        originClassLoader?.let { setClassLoader(it) }
        finishAfterTransition()
    }
}
