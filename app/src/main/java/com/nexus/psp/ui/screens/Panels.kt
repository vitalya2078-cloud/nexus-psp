package com.nexus.psp.ui.screens

import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.hrs
import com.nexus.psp.data.size
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.Drawer
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.Ellip
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.Range
import com.nexus.psp.ui.kit.SolarDuo
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.Sub
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.ui.kit.press
import com.nexus.psp.ui.kit.tap
import kotlinx.coroutines.delay

@Composable
internal fun Item(gid: String?, title: String, sub: String, unread: Boolean = false, icon: Int? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val g = gid?.let { AppState.byId[it] }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (unread) K.acc.copy(alpha = .08f) else Color.Transparent)
            .then(if (onClick != null) Modifier.press(onClick = onClick) else Modifier).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(K.s2), contentAlignment = Alignment.Center) {
            if (g != null) Cover(g, 300, 300, modifier = Modifier.fillMaxSize()) else if (icon != null) SolarIcon(icon, 20.dp, K.ink2)
        }
        Column(Modifier.weight(1f)) {
            Ellip(title, T.label)
            Text(sub, style = T.small)
        }
        trailing()
    }
}

/** Пустая панель: большой значок, заголовок, пояснение. */
@Composable
private fun Empty(icon: Int, title: String, text: String, extra: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(K.s1), contentAlignment = Alignment.Center) { SolarDuo(icon, 34.dp) }
        Text(title, style = T.h3)
        Text(text, style = T.body.copy(fontSize = 13.sp), modifier = Modifier.padding(horizontal = 12.dp))
        extra()
    }
}

/** Загрузки: пока магазина нет — копирование своих файлов и память телефона. */
@Composable
fun DownloadsDrawer() {
    val im = AppState.importing
    Drawer("Загрузки") {
        if (im != null) {
            Sub("Добавляется")
            Item(null, im.name, "Копирование в KAPSULA · ${(im.p * 100).toInt()}%" + if (im.total > 1) " · ${im.n} из ${im.total}" else "", icon = R.drawable.sl_folder)
            Bar(im.p, modifier = Modifier.padding(start = 72.dp, end = 8.dp, bottom = 6.dp))
        } else Empty(R.drawable.sd_dl, "Сейчас ничего не загружается", "Здесь появятся загрузки из магазина и копирование твоих файлов.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                KButton("Найти на телефоне", R.drawable.sl_search, small = true) { Act.findGames() }
                KButton("Добавить игру", R.drawable.ph_plus, BtnKind.Main, small = true) { Act.addFile() }
            }
        }
        Sub("Память телефона")
        val free = AppState.freeMb(); val total = AppState.totalMb(); val used = AppState.usedMb()
        if (total > 0) Bar(1f - free.toFloat() / total, height = 8.dp)
        Text("Игры KAPSULA ${size(used)} · свободно ${size(free)}" + (if (total > 0) " из ${size(total)}" else ""), style = T.small, modifier = Modifier.padding(top = 6.dp))
        if (free in 1..2999) StateCard("es-storage.png", "Мало памяти.", "Игры PSP весят 0,2–2 ГБ. Удали ненужные игры или освободи место на телефоне.")
    }
}

/** Уведомления; открытие гасит точку колокольчика через секунду. */
@Composable
fun AlertsDrawer() {
    LaunchedEffect(Unit) { delay(1200); AppState.readAll() }
    Drawer("Уведомления", actions = { if (AppState.alerts.any { it.u }) KButton("Прочитать все", small = true) { AppState.readAll() } }) {
        if (AppState.alerts.isEmpty()) Empty(R.drawable.sd_bell, "Пока тихо", "Сюда придут новости: игра добавлена в библиотеку, магазин открылся, вышло обновление KAPSULA.")
        AppState.alerts.forEach { a ->
            Item(a.g, a.t, a.d, unread = a.u, icon = R.drawable.sl_bolt, onClick = { if (a.g != null && AppState.byId[a.g] != null) Nav.open(Ov.Game(a.g)) })
        }
    }
}

private fun padState(): String {
    val d = InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }.firstOrNull { !it.isVirtual && (it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) }
    return d?.name?.take(22) ?: "Не подключён"
}

/** Быстрая панель (п. 16): плитки и ползунки громкости/яркости. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanel() {
    Box(Modifier.fillMaxSize()) {
        val p = com.nexus.psp.ui.rememberEnter(220)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = p.value }.background(Color.Black.copy(alpha = .55f)).tap { Nav.back() })
        // шторка сверху: выезжает вниз
        Column(
            Modifier.fillMaxWidth().graphicsLayer { translationY = -(1f - p.value) * 48.dp.toPx(); alpha = p.value }
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(K.bg).tap { }
                .windowInsetsPadding(WindowInsets.displayCutout).padding(bottom = 16.dp),
        ) {
            // заголовок и плитки — на одной вертикали (поля 16 dp у обоих)
            com.nexus.psp.ui.OvHead("Быстрая панель")
            val tiles = listOf(
                Tile(R.drawable.sd_pad, "Геймпад", padState(), false) { Nav.settingsTab = "ctl"; Nav.replace(Ov.Settings) },
                Tile(if (S.theme == "light") R.drawable.sd_sun else R.drawable.sd_moon, "Тема", mapOf("dark" to "Тёмная", "oled" to "OLED", "light" to "Светлая")[S.theme] ?: "", false) {
                    S.theme = mapOf("dark" to "oled", "oled" to "light", "light" to "dark")[S.theme] ?: "dark"; AppState.applyLook()
                },
                Tile(R.drawable.ph_hand, "Вибрация", if (S.vib) "Вкл" else "Выкл", S.vib) { S.vib = !S.vib },
                Tile(R.drawable.sd_music, "Звуки", if (S.sound) "Вкл" else "Выкл", S.sound) { S.sound = !S.sound },
                Tile(R.drawable.sd_grid, "Экранный геймпад", mapOf("touch" to "KAPSULA Touch", "classic" to "Классика", "pad" to "Геймпад")[S.ctlMode] ?: "", false) { Nav.replace(Ov.Pad) },
                Tile(R.drawable.sd_folder, "Добавить игру", "ISO, CSO, CHD, PBP", false) { Nav.back(); Act.addFile() },
                Tile(R.drawable.sd_search, "Найти игры", "в папке телефона", false) { Nav.back(); Act.findGames() },
                Tile(R.drawable.sd_gear, "Настройки", "Все параметры", false) { Nav.replace(Ov.Settings) },
            )
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 4) {
                tiles.forEach { t -> QTile(t, Modifier.weight(1f)) }
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(K.s1).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SolarIcon(R.drawable.sl_vol, 20.dp, K.ink2); Range(S.vol, 0, 100, fill = true) { S.vol = it }
                }
                Row(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(K.s1).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SolarIcon(R.drawable.sl_sun, 20.dp, K.ink2); Range(S.bright, 40, 100, fill = true) { S.bright = it }
                }
            }
        }
    }
}

private data class Tile(val icon: Int, val title: String, val sub: String, val on: Boolean, val onClick: () -> Unit)

@Composable
private fun QTile(t: Tile, modifier: Modifier) {
    Column(
        modifier.height(96.dp).clip(RoundedCornerShape(18.dp)).background(if (t.on) K.ink else K.s1).press(onClick = t.onClick).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (t.on) SolarDuo(t.icon, 26.dp, main = K.bg, accent = K.bg.copy(alpha = .55f)) else SolarDuo(t.icon, 26.dp)
        Column {
            Text(t.title, style = T.label.copy(color = if (t.on) K.bg else K.ink), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(t.sub, style = T.small.copy(color = if (t.on) K.bg.copy(alpha = .7f) else K.ink2), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}


