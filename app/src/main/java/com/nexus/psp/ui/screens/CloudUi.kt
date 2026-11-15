package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Cloud
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * «Аккаунт KAPSULA» в настройках: код восстановления (показать, скопировать) и восстановление по коду на новом телефоне.
 */
@Composable
fun CloudAccount() {
    val ctx = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var restore by remember { mutableStateOf(false) }
    val code = Cloud.code
    Opt(
        "Аккаунт KAPSULA",
        if (code.isEmpty()) "Создаётся при первой связи с интернетом. По коду восстановления вернутся жетоны, оформление и открытое за игру"
        else "Жетоны, оформление и открытое за игру хранятся на сервере. Код восстановления вернёт их на новом телефоне",
        sub = if (code.isNotEmpty()) (if (show) code else "Код: KPS-••••-••••-••••") else null,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (code.isNotEmpty()) {
                KButton(if (show) "Скрыть" else "Показать код", small = true) { show = !show }
                KButton("Скопировать", small = true) {
                    val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("Код KAPSULA", code)); AppState.say("Код скопирован. Сохрани его в заметках")
                }
            }
            KButton("Восстановить", small = true) { restore = true }
        }
    }
    if (restore) RestoreDialog { restore = false }
}

@Composable
private fun RestoreDialog(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    androidx.compose.ui.window.Dialog(onClose) {
        Column(
            Modifier.widthIn(max = 460.dp).clip(RoundedCornerShape(22.dp)).background(K.bg).border(1.dp, K.line, RoundedCornerShape(22.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Восстановить по коду", style = T.h3)
            Text("Жетоны, купленное оформление и открытое за игру вернутся. Жетоны и покупки этого телефона заменятся теми, что в аккаунте.", style = T.small)
            SearchField(input, "KPS-XXXX-XXXX-XXXX", Modifier.fillMaxWidth(), icon = R.drawable.sl_keyhole) { input = it.take(24); err = null }
            err?.let { Text(it, style = T.small.copy(color = K.bad, fontSize = 13.sp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton(if (busy) "Проверяю…" else "Восстановить", kind = BtnKind.Main, enabled = !busy && input.length >= 12) {
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { Cloud.restore(input) }
                        busy = false
                        if (r == null) { AppState.say("Аккаунт восстановлен"); onClose() } else err = r
                    }
                }
                KButton("Отмена") { onClose() }
            }
        }
    }
}
