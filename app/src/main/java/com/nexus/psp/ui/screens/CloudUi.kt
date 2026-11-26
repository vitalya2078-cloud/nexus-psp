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
    if (code.isNotEmpty()) Opt("ID игрока", "Его можно называть в поддержке и (скоро) друзьям. Код восстановления — секрет, его не показывай") { PlayerId(onDark = false) }
    Opt("Код подарка", "Подарки — рамки и другое оформление — выдаются на сайте KAPSULA и в наших новостях. Кнопка «Забрать» на сайте откроет KAPSULA сама, а код можно ввести и здесь") {
        KButton("Ввести", small = true) { GiftLink.pending = "" }
    }
    val scope = rememberCoroutineScope()
    var del by remember { mutableStateOf(false) }
    if (Cloud.noAccount) Opt("Профиль удалён", "Прогресса, жетонов и друзей на сервере больше нет. Игры и сохранения на телефоне остались") {
        KButton("Создать заново", small = true) { Cloud.allowAccount(); scope.launch { withContext(Dispatchers.IO) { Cloud.sync() }; AppState.say(if (Cloud.code.isNotEmpty()) "Новый профиль создан" else "Нет связи с сервером — профиль создастся сам, когда появится интернет") } }
    } else if (code.isNotEmpty()) Opt("Удалить профиль", "Стереть с сервера всё, что там о тебе есть: прогресс, жетоны, оформление, друзей и облачные сейвы. Игры и сохранения на телефоне не тронем") {
        KButton("Удалить", small = true, danger = true) { del = true }
    }
    if (restore) RestoreDialog { restore = false }
    if (del) DeleteDialog { del = false }
}

/** Подарок: ссылка с сайта (kapsula://gift/КОД) или код, введённый вручную. pending != null — окно открыто. */
object GiftLink {
    var pending by mutableStateOf<String?>(null)
}

/** Окно подарка: с кодом из ссылки забирает сразу, без кода — поле ввода. Поверх всего (KapsulaApp). */
@Composable
fun GiftDialog() {
    val start = GiftLink.pending ?: return
    val scope = rememberCoroutineScope()
    var input by remember(start) { mutableStateOf(start) }
    var busy by remember(start) { mutableStateOf(false) }
    var err by remember(start) { mutableStateOf<String?>(null) }
    var got by remember(start) { mutableStateOf<String?>(null) }
    fun claim() {
        busy = true; err = null
        scope.launch {
            val (title, e) = withContext(Dispatchers.IO) { Cloud.gift(input) }
            busy = false; if (title != null) got = title else err = e
        }
    }
    androidx.compose.runtime.LaunchedEffect(start) { if (start.isNotEmpty()) claim() }
    val close = { GiftLink.pending = null }
    androidx.compose.ui.window.Dialog(close) {
        Column(
            Modifier.widthIn(max = 460.dp).clip(RoundedCornerShape(22.dp)).background(K.s1).border(1.dp, K.acc.copy(alpha = .3f), RoundedCornerShape(22.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            com.nexus.psp.ui.kit.LiveMascot(if (got != null) "gift-open" else if (err != null) "sad" else "gift", 120.dp, glow = got != null)
            when {
                got != null -> {
                    Text("Подарок: $got", style = T.h3)
                    Text("Уже лежит в «Оформлении» — надень, когда захочешь.", style = T.small)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KButton("Открыть оформление", kind = BtnKind.Main) { close(); com.nexus.psp.ui.Nav.open(com.nexus.psp.ui.Ov.Look("profile")) }
                        KButton("Позже") { close() }
                    }
                }
                else -> {
                    Text(if (busy) "Забираю подарок…" else "Код подарка", style = T.h3)
                    SearchField(input, "Например, PIONEER", Modifier.fillMaxWidth(), icon = R.drawable.sl_keyhole) { input = it.take(32); err = null }
                    err?.let { Text(it, style = T.small.copy(color = K.bad, fontSize = 13.sp)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KButton(if (busy) "Проверяю…" else "Забрать", kind = BtnKind.Main, enabled = !busy && input.isNotBlank()) { claim() }
                        KButton("Отмена") { close() }
                    }
                }
            }
        }
    }
}

/** Удаление профиля: два подтверждения, грустный Сейвик с чемоданчиком. */
@Composable
private fun DeleteDialog(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var sure by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    androidx.compose.ui.window.Dialog(onClose) {
        Column(
            Modifier.widthIn(max = 480.dp).clip(RoundedCornerShape(22.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(22.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            com.nexus.psp.ui.kit.LiveMascot("bye", 110.dp, glow = false)
            Text(if (sure) "Точно удалить?" else "Удалить профиль?", style = T.h3)
            Text(
                if (sure) "Это не отменить. Код восстановления перестанет работать."
                else "С сервера сотрутся прогресс и уровень, жетоны и купленное оформление, друзья, подарки и облачные сейвы. Игры, сохранения и настройки на телефоне останутся.",
                style = T.small.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            )
            err?.let { Text(it, style = T.small.copy(color = K.bad, fontSize = 13.sp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton(if (busy) "Удаляю…" else if (sure) "Удалить навсегда" else "Удалить", enabled = !busy, danger = true) {
                    if (!sure) { sure = true; return@KButton }
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { Cloud.deleteProfile() }
                        busy = false
                        if (r == null) { AppState.sayAs("bye", "Профиль удалён. Пока, и спасибо, что играл"); onClose() } else err = r
                    }
                }
                KButton("Оставить", kind = BtnKind.Main) { onClose() }
            }
        }
    }
}

@Composable
private fun RestoreDialog(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctxApp = androidx.compose.ui.platform.LocalContext.current.applicationContext
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
                        if (r == null) {
                            AppState.say("Аккаунт восстановлен"); onClose()
                            // сохранения игр из облака — на этот телефон (что есть и тут, и там — спросим)
                            if (AppState.S.cloud) AppState.scope.launch {
                                val rep = com.nexus.psp.data.CloudSaves.sync(ctxApp)
                                if (rep.down > 0) AppState.say("Сохранения из облака: ${rep.down}")
                            }
                        } else err = r
                    }
                }
                KButton("Отмена") { onClose() }
            }
        }
    }
}
