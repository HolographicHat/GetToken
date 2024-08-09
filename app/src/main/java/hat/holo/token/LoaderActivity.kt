package hat.holo.token

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.ArrayMap
import dalvik.system.DexClassLoader
import hat.holo.token.utils.visitAndSetField
import hat.holo.token.utils.visitField
import hat.holo.token.utils.visitParentField
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
        val actThread = this.visitParentField<Activity, Any>("mMainThread")
        val pkgs = actThread.visitField<ArrayMap<String, WeakReference<*>>>("mPackages")
        val pkg = pkgs[packageName]?.get() ?: throw IllegalStateException("WeakRef<LoadedApk> is null!")
        return pkg.visitAndSetField<ClassLoader>("mClassLoader", classLoader)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        originClassLoader?.let { setClassLoader(it) }
        finishAfterTransition()
    }
}
