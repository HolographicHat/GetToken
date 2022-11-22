package hat.holo.token.utils

import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Method
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class ConstraintSetWrapper(
    clazz: Class<*>,
    private val clone: Method,
    private val apply: Method,
    private val connect: Method,
    private val connectWithMargin: Method
) {

    private val instance = clazz.newInstance()

    fun clone(constraintLayout: Any) {
        clone.invoke(instance, constraintLayout)
    }

    fun connect(startID: Int, startSide: Int, endID: Int, endSide: Int) {
        connect.invoke(instance, startID, startSide, endID, endSide)
    }

    fun connect(startID: Int, startSide: Int, endID: Int, endSide: Int, margin: Int) {
        connectWithMargin.invoke(instance, startID, startSide, endID, endSide, margin)
    }

    fun applyTo(constraintLayout: Any) {
        apply.invoke(instance, constraintLayout)
    }
}

@OptIn(ExperimentalTime::class)
fun ClassLoader.createConstraintSet(ctx: Context) = measureTimedValue {
    runCatching {
        val calledMethods = arrayListOf<Method>()
        val zRadialViewGroup = loadClass("com.google.android.material.timepicker.RadialViewGroup")
        val zConstraints = loadClass("androidx.constraintlayout.widget.Constraints")
        val zConstraintLayout = loadClass("androidx.constraintlayout.widget.ConstraintLayout")
        val zConstraintSet = zConstraints.getDeclaredMethod("getConstraintSet").returnType
        val hooks = zConstraintSet.declaredMethods.filter { method ->
            method.parameterCount == 1 && method.parameterTypes[0] == zConstraintLayout
        }.map { method ->
            XposedHelpers.findAndHookMethod(
                zConstraintSet,
                method.name,
                zConstraintLayout,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        calledMethods += param.method as Method
                    }
                })
        }
        val oRadialViewGroup = zRadialViewGroup.getDeclaredConstructor(Context::class.java).apply {
            isAccessible = true
        }.newInstance(ctx)
        zRadialViewGroup.getDeclaredMethod("updateLayoutParams").invoke(oRadialViewGroup)
        hooks.forEach { hook -> hook.unhook() }
        if (calledMethods.size == 2) {
            ConstraintSetWrapper(
                clazz = zConstraintSet,
                clone = calledMethods[0],
                apply = calledMethods[1],
                connect = zConstraintSet.declaredMethods.first { method ->
                    method.parameterCount == 4 && method.parameterTypes.all { p -> p == Int::class.java }
                },
                connectWithMargin = zConstraintSet.declaredMethods.first { method ->
                    method.parameterCount == 5 && method.parameterTypes.all { p -> p == Int::class.java }
                }
            )
        } else null
    }.onFailure { ex ->
        XposedBridge.log(ex)
    }.getOrNull()
}.let { (result, duration) ->
    XposedBridge.log("createConstraintSet cost ${duration.toString(DurationUnit.MILLISECONDS, 2)}")
    result
}
