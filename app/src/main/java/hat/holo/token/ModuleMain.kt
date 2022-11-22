package hat.holo.token

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.XResources
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.Keep
import dalvik.system.BaseDexClassLoader
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import hat.holo.token.utils.*
import java.io.File
import kotlin.math.roundToInt

@Keep
class ModuleMain : IXposedHookLoadPackage, IXposedHookZygoteInit {

    private var isPatch = false
    private var modulePath = ""
    private val targetPackageName = "com.mihoyo.hyperion"

    // platform/libcore/+/refs/heads/master/dalvik/src/main/java/dalvik/system/DexPathList.java
    // platform/libcore/+/refs/heads/master/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java
    @SuppressLint("DiscouragedPrivateApi")
    private fun ClassLoader.appendToClassPath(ctx: Context) {
        val zBaseDexClassLoader = BaseDexClassLoader::class.java
        val fPathList = zBaseDexClassLoader.getDeclaredField("pathList").setAccess()
        val oPathList = fPathList.get(this) // DexPathList
        val zDexPathList = oPathList.javaClass
        val mAddDexPath = zDexPathList.getDeclaredMethod("addDexPath", String::class.java, File::class.java)
        mAddDexPath.setAccess().invoke(oPathList, modulePath, ctx.cacheDir)
    }

    @SuppressLint("DiscouragedApi")
    override fun handleLoadPackage(lpparam: LoadPackageParam) = with(lpparam) pms@{
        if (packageName != targetPackageName) return
        with(classLoader) {
            val c0 = loadClass("com.mihoyo.hyperion.app.HyperionApplicationHelper")
            findAndHookMethod(c0, "initOnMainProcess", Application::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    AppUtils.init(classLoader)
                    AccountManager.init(classLoader)
                    val app = p.args[0] as Application
                    appendToClassPath(app.applicationContext)
                }
            })
            val c1 = loadClass("com.mihoyo.hyperion.main.user.MainUserInfoPage")
            XposedBridge.hookAllConstructors(c1, object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    if (p.args.size != 3 || p.args.getOrNull(0) !is Context) return
                    val root1 = p.thisObject as FrameLayout
                    val root2 = root1.getChildAt(0) as FrameLayout
                    val root3 = root2.getChildAt(0) as ViewGroup
                    val ctx = root1.context
                    val scanId = ctx.resources.getIdentifier("scanIv", "id", targetPackageName)
                    val scanBtn = root3.findViewById<ImageView>(scanId)
                    val tokenBtn = ImageView(ctx)
                    tokenBtn.id = XResources.getFakeResId("getTokenIv")
                    tokenBtn.setImageDrawable(Res.iconToken)
                    val size = Dimension.convertDpToPixel(32f, ctx).roundToInt()
                    tokenBtn.layoutParams = ViewGroup.LayoutParams(size, size)
                    tokenBtn.setOnClickListener {
                        if (AccountManager.isLogin) {
                            if (isPatch) {
                                val intent = Intent(ctx, LoaderActivity::class.java)
                                intent.putExtra("accountInfo", AccountManager.accountInfo)
                                intent.putExtra("dexPath", modulePath)
                                ctx.startActivity(intent)
                            } else {
                                val intent = Intent()
                                intent.setClassName("hat.holo.token", "hat.holo.token.TokenActivity")
                                intent.putExtra("accountInfo", AccountManager.accountInfo)
                                ctx.startActivity(intent)
                            }
                        } else {
                            AppUtils.showToast("未登录")
                        }
                    }
                    root3.addView(tokenBtn)
                    for (i in 0 until root3.childCount) {
                        val view = root3.getChildAt(i)
                        if (view.id == -1) view.id = XResources.getFakeResId("b5AaLhI6WDlkTMIrRA$i")
                    }
                    val set = createConstraintSet(ctx)
                    if (set == null) {
                        AlertDialog.Builder(ctx).run {
                            setTitle("Error")
                            setMessage("Create ConstraintSetWrapper fail.")
                        }.create().show()
                        return
                    }
                    set.clone(root3)
                    set.connect(tokenBtn.id, 2, scanBtn.id, 1, Dimension.convertDpToPixel(9f, ctx).roundToInt())
                    set.connect(tokenBtn.id, 3, scanBtn.id, 3)
                    set.applyTo(root3)
                }
            })
        }
        XposedBridge.log("Module initialized!")
    }

    override fun initZygote(params: IXposedHookZygoteInit.StartupParam) {
        modulePath = params.modulePath
        isPatch = modulePath.contains("lspatch")
    }
}
