package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexus.psp.BuildConfig
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.LiveMascot
import com.nexus.psp.ui.kit.T
import org.json.JSONArray

/**
 * Окна, которые показываются по одному поверх главного экрана после знакомства:
 * «Что нового» (после обновления) → вопрос про анонимную статистику (один раз) → подарок по ссылке.
 */
@Composable
fun Notices() {
    val ctx = LocalContext.current
    // «Что нового»: после обновления (не при первой установке). Текст — assets/changes.json, тот же, что на сайте.
    var news by remember {
        mutableStateOf(
            if (S.seenVer == BuildConfig.VERSION_NAME) null
            else if (S.seenVer.isEmpty() && !S.statsAsked && !wasHere()) { S.seenVer = BuildConfig.VERSION_NAME; null }
            else changesFor(ctx, BuildConfig.VERSION_NAME)
        )
    }
    when {
        news != null -> WhatsNew(news!!) { S.seenVer = BuildConfig.VERSION_NAME; news = null }
        !S.statsAsked -> StatsAsk()
        else -> GiftDialog()
    }
}

/** Приложение уже стояло до 0.50 (есть библиотека или аккаунт) — значит, это обновление, а не новая установка. */
private fun wasHere() = AppState.lib.isNotEmpty() || com.nexus.psp.data.Cloud.code.isNotEmpty()

/** Пункты «Что нового» для версии (пусто — нечего показать). */
fun changesFor(ctx: android.content.Context, ver: String): List<String>? = try {
    val a = JSONArray(ctx.assets.open("changes.json").bufferedReader().readText())
    (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.optString("ver") == ver }
        ?.optJSONArray("items")?.let { i -> (0 until i.length()).map { i.getString(it) } }?.takeIf { it.isNotEmpty() }
} catch (e: Exception) { null }

@Composable
private fun Sheet(content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog({ }, androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false)) {
        Column(
            Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(24.dp)).background(K.s1).border(1.dp, K.acc.copy(alpha = .28f), RoundedCornerShape(24.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
private fun WhatsNew(items: List<String>, onClose: () -> Unit) = Sheet {
    LiveMascot("scroll", 104.dp)
    Text("Что нового в ${BuildConfig.VERSION_NAME}", style = T.h3)
    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { t ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = 8.dp).size(7.dp).clip(CircleShape).background(K.acc))
                Text(t, style = T.body)
            }
        }
    }
    KButton("Отлично!", kind = BtnKind.Main) { onClose() }
}

/** Анонимная статистика — только с согласия: один раз спросить, ответ можно поменять в «Настройки → Общие». */
@Composable
private fun StatsAsk() = Sheet {
    LiveMascot("checklist", 104.dp)
    Text("Поможешь KAPSULA стать лучше?", style = T.h3, textAlign = TextAlign.Center)
    Text(
        "Можно присылать нам анонимные счётчики: сколько раз запускались игры, на каких консолях, были ли вылеты. " +
            "Без названий игр, без файлов и без твоего ID — только общие суммы. Поменять ответ можно в «Настройки → Общие».",
        style = T.body, textAlign = TextAlign.Center,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KButton("Помогать", kind = BtnKind.Main) { S.stats = true; S.statsAsked = true }
        KButton("Не сейчас") { S.stats = false; S.statsAsked = true; com.nexus.psp.data.Telemetry.clear() }
    }
}
