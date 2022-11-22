package hat.holo.token

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.getSystemService
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import hat.holo.token.models.AccountInfo
import hat.holo.token.models.BaseResponse
import hat.holo.token.models.FetchRsp
import hat.holo.token.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val textColor = Color(0xFF424242)

@Keep
class TokenActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    @SuppressLint("DiscouragedPrivateApi", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        val accountInfo = intent.getSerializableExtra("accountInfo") as AccountInfo
        setContent {
            rememberSystemUiController().setStatusBarColor(Color.White)
            MaterialTheme(
                colors = lightColors(
                    primary = Color(0xFF2196F3)
                ),
                content = {
                    Surface {
                        Content(accountInfo)
                    }
                }
            )
        }
        setResult(2333)
    }
}

private fun TokenActivity.showDialog(msg: String) = runOnUiThread {
    AlertDialog.Builder(this).run {
        setMessage(msg)
        setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
    }.create().show()
}

private suspend fun TokenActivity.genAuthCode(
    acc: AccountInfo,
    useSToken: Boolean
): String? = runCatching {
    val params = mutableMapOf<String, Any?>("app_id" to "4", "device" to "0")
    val fetchResult = buildHttpRequest {
        url("https://hk4e-sdk.mihoyo.com/hk4e_cn/combo/panda/qrcode/fetch")
        params.post(this)
    }.getAsJson<BaseResponse>()
    if (fetchResult.retcode != 0) {
        showDialog("请求失败: ${fetchResult.message}")
        return null
    }
    val ticket = fetchResult.data<FetchRsp>().getUri().getQueryParameter("ticket")
    val scanResult = buildHttpRequest {
        url("https://api-sdk.mihoyo.com/hk4e_cn/combo/panda/qrcode/scan")
        params.apply {
            put("ticket", ticket)
        }.post(this)
    }.getAsJson<BaseResponse>()
    if (scanResult.retcode != 0) {
        showDialog("请求失败: ${scanResult.message}")
        return@runCatching null
    }
    val confirmResult = buildHttpRequest {
        url("https://api-sdk.mihoyo.com/hk4e_cn/combo/panda/qrcode/confirm")
        params.apply {
            put("payload", buildMap {
                put("proto", "Account")
                put("raw", buildMap {
                    put("uid", acc.uid)
                    put("ltoken", acc.lToken)
                    if (useSToken) {
                        put("mid", acc.mid)
                        put("stoken", acc.sToken)
                    }
                }.toJson())
            })
        }.post(this)
    }.getAsJson<BaseResponse>()
    if (confirmResult.retcode != 0) {
        showDialog("请求失败: ${confirmResult.message}")
        return null
    }
    return ticket
}.onFailure {
    showDialog("网络异常，请稍后重试")
}.getOrNull()

@Composable
private fun TokenActivity.Content(accountInfo: AccountInfo) = Column(
    modifier = Modifier.fillMaxSize()
) {
    TopAppBar()
    Column(
        modifier = Modifier.padding(15.dp)
    ) {
        var isLoading by remember { mutableStateOf(false) }
        var authTicket by remember { mutableStateOf("") }
        var grantSToken by remember { mutableStateOf(false) }
        CustomCheckBox(
            checked = true,
            onCheckedChange = {},
            name = "LToken",
            permissions = buildAnnotatedString {
                appendLine("此令牌可以用于:")
                appendLine(" · 获取实时便笺与统计信息")
                appendLine(" · 其它可以通过米游社网页完成的操作")
            } // TODO: More description
        )
        CustomCheckBox(
            checked = grantSToken,
            onCheckedChange = { v -> grantSToken = v },
            name = "SToken",
            permissions = buildAnnotatedString {
                appendLine("此令牌可以用于:")
                appendLine(" · 登录原神/崩坏3等游戏", true)
                appendLine(" · 获取 LToken")
                appendLine(" · 其它可以通过米游社APP完成的操作")
            } // TODO: More description
        )
        Divider()
        AnimatedVisibility(
            visible = authTicket.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            AuthTicketView(authTicket)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (BuildConfig.DEBUG) {
                TextButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            val confirmResult = buildHttpRequest {
                                url("https://hk4e-sdk.mihoyo.com/hk4e_cn/combo/panda/qrcode/query")
                                buildMap {
                                    put("app_id", "4")
                                    put("device", "0")
                                    put("ticket", authTicket.removePrefix("ma_"))
                                }.post(this)
                            }.getAsJson<BaseResponse>()
                            if (confirmResult.retcode != 0) {
                                showDialog("请求失败: ${confirmResult.message}")
                                return@launch
                            } else {
                                showDialog(confirmResult.data?.toJson() ?: "success, but data is null")
                            }
                        }
                    }
                ) {
                    Text(text = "query")
                }
            }
            TextButton(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        authTicket = ""
                        isLoading = true
                        authTicket = genAuthCode(accountInfo, grantSToken)?.let { s -> "ma_${s}" } ?: ""
                        isLoading = false
                    }
                },
                enabled = !isLoading
            ) {
                AnimatedVisibility(visible = isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                }
                AnimatedVisibility(visible = !isLoading) {
                    // 120s available
                    Text("生成登录代码")
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TokenActivity.AuthTicketView(authTicket: String) = ConstraintLayout(
    modifier = Modifier.fillMaxWidth()
) {
    val (divider, code, copyBtn) = createRefs()
    Text(
        text = authTicket,
        modifier = Modifier.constrainAs(code) {
            top.linkTo(parent.top, 12.dp)
            start.linkTo(parent.start, 12.dp)
        },
        fontFamily = FontFamily.Monospace
    )
    var showDoneIcon by remember { mutableStateOf(false) }
    val iconColor by animateColorAsState(if (showDoneIcon) Color(0xFF4CAF50) else textColor)
    IconButton(
        modifier = Modifier.constrainAs(copyBtn) {
            top.linkTo(parent.top)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        },
        onClick = {
            runCatching {
                val clip = ClipData.newPlainText(null, authTicket)
                getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
            }.onFailure {
                showDialog("复制失败")
            }.onSuccess {
                showDoneIcon = true
            }
        }
    ) {
        AnimatedContent(targetState = if (showDoneIcon) Icons.Outlined.Done else Res.iconCopy) { targetIcon ->
            Image(
                imageVector = targetIcon,
                contentDescription = "Copy auth ticket",
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
        LaunchedEffect(showDoneIcon) {
            if (showDoneIcon) {
                delay(1500)
                showDoneIcon = false
            }
        }
    }
    Divider(
        modifier = Modifier.constrainAs(divider) {
            top.linkTo(code.bottom, 12.dp)
            bottom.linkTo(parent.bottom)
        }
    )
}

@Composable
private fun CustomCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    name: String,
    permissions: AnnotatedString
) {
    val interactionSource = remember { MutableInteractionSource() }
    ConstraintLayout(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .fillMaxWidth()
    ) {
        val (checkBox, itemTitle, itemDesc) = createRefs()
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.constrainAs(checkBox) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            },
            interactionSource = interactionSource
        )
        Text(
            text = name,
            modifier = Modifier.constrainAs(itemTitle) {
                top.linkTo(parent.top)
                start.linkTo(checkBox.end)
                bottom.linkTo(itemDesc.top)
            }
        )
        Text(
            text = permissions,
            modifier = Modifier.constrainAs(itemDesc) {
                top.linkTo(checkBox.bottom)
                start.linkTo(itemTitle.start)
                bottom.linkTo(parent.bottom, 5.dp)
            },
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TopAppBar() = TopAppBar(
    modifier = Modifier.fillMaxWidth(),
    elevation = 10.dp,
    backgroundColor = Color.White
) {
    Text(
        text = "获取登录信息",
        modifier = Modifier.padding(start = 16.dp),
        color = textColor
    )
}

private fun AnnotatedString.Builder.appendLine(str: String, emphasize: Boolean = false) = apply {
    if (emphasize) pushStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold))
    append(str)
    append('\n')
    if (emphasize) pop()
}
