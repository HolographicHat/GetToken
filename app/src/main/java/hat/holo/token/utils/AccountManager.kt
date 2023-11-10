package hat.holo.token.utils

import hat.holo.token.models.AccountInfo

@Suppress("MemberVisibilityCanBePrivate", "unused")
object AccountManager {

    private lateinit var instPorte: Any
    private lateinit var clzPorte: Class<*>
    private lateinit var clzAccount: Class<*>

    fun init(cl: ClassLoader) {
        clzPorte = cl.loadClass("com.mihoyo.platform.account.sdk.Porte")
        clzAccount = cl.loadClass("com.mihoyo.platform.account.sdk.bean.Account")
        instPorte = clzPorte.getDeclaredField("INSTANCE").get(null)!!
    }

    private fun loginCurrentAccount() = clzPorte.getDeclaredMethod("loginCurrentAccount").invoke(instPorte)

    val isLogin get() = if (loginCurrentAccount() == null) false else !uid.isNullOrEmpty() && !sToken.isNullOrEmpty()

    val mid get() = clzAccount.getDeclaredMethod("getMid").invoke(loginCurrentAccount()) as String?
    val uid get() = clzAccount.getDeclaredMethod("getAid").invoke(loginCurrentAccount()) as String?
    val lToken get() = clzPorte.getDeclaredMethod("getLToken").invoke(instPorte) as String?
    val sToken get() = clzAccount.getDeclaredMethod("getTokenStr").invoke(loginCurrentAccount()) as String?

    val accountInfo get() = AccountInfo(mid!!, uid!!, lToken!!, sToken!!)

}
