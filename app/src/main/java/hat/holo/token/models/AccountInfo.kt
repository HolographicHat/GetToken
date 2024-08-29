package hat.holo.token.models

data class AccountInfo(
    val mid: String,
    val uid: String,
    val lToken: String?,
    val sToken: String
) : java.io.Serializable
