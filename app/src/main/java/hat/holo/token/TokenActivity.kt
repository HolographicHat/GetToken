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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.getSystemService
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import hat.holo.token.models.AccountInfo
import kotlinx.coroutines.delay

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

@Composable
private fun TokenActivity.Content(accountInfo: AccountInfo) = Column(
    modifier = Modifier.fillMaxSize()
) {
    TopAppBar()
    Column(
        modifier = Modifier.padding(15.dp)
    ) {
        var grantSToken by remember { mutableStateOf(false) }
        var showDoneIcon by remember { mutableStateOf(false) }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    runCatching {
                        val authStr = buildMap {
                            put("ltuid", accountInfo.uid)
                            put("ltoken", accountInfo.lToken)
                            if (grantSToken) {
                                put("stuid", accountInfo.uid)
                                put("mid", accountInfo.mid)
                                put("stoken", accountInfo.sToken)
                            }
                        }.map { (k, v) -> "$k=$v" }.joinToString(";")
                        val clip = ClipData.newPlainText(null, authStr)
                        getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
                    }.onFailure {
                        showDialog("复制失败")
                    }.onSuccess {
                        showDoneIcon = true
                    }
                }
            ) {
                AnimatedVisibility(visible = showDoneIcon) {
                    Image(
                        imageVector = Icons.Outlined.Done,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color(0xFF4CAF50))
                    )
                }
                Text("复制登录信息")
            }
        }
        LaunchedEffect(showDoneIcon) {
            if (showDoneIcon) {
                delay(1500)
                showDoneIcon = false
            }
        }
    }
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
