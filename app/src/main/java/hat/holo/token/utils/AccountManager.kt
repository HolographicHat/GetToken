package hat.holo.token.utils

import hat.holo.token.models.AccountInfo
import java.lang.reflect.Method

@Suppress("MemberVisibilityCanBePrivate", "unused")
object AccountManager {

    private lateinit var instance: Any
    private val methodTable = arrayListOf<Method>()

    fun init(cl: ClassLoader) {
        val clz = cl.loadClass("com.mihoyo.hyperion.user.account.AccountManager")
        instance = clz.getDeclaredField("INSTANCE").get(null)!!
        methodTable.add(clz.getDeclaredMethod("getMid"))
        methodTable.add(clz.getDeclaredMethod("getUserId"))
        methodTable.add(clz.getDeclaredMethod("getLToken"))
        methodTable.add(clz.getDeclaredMethod("getSToken"))
        methodTable.add(clz.getDeclaredMethod("getSTokenV1"))
        methodTable.add(clz.getDeclaredMethod("userIsLogin"))
        methodTable.add(clz.getDeclaredMethod("getLastUserId"))
        methodTable.add(clz.getDeclaredMethod("getLoginTicket"))
    }

    val isLogin get() = methodTable[5].invoke(instance) as Boolean

    val mid get() = methodTable[0].invoke(instance) as String
    val uid get() = methodTable[1].invoke(instance) as String
    val lToken get() = methodTable[2].invoke(instance) as String
    val sToken get() = methodTable[3].invoke(instance) as String
    val sTokenV1 get() = methodTable[4].invoke(instance) as String
    val lastUserId get() = methodTable[6].invoke(instance) as String
    val loginTicket get() = methodTable[7].invoke(instance) as String

    val accountInfo get() = AccountInfo(mid, uid, lToken, sToken)

}
