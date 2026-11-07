package com.nexus.psp.ui.screens

import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private fun Item(gid: String?, title: String, sub: String, unread: Boolean = false, icon: Int? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val g = gid?.let { AppState.byId[it] }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (unread) K.acc.copy(alpha = .08f) else Color.Transparent)
            .then(if (onClick != null) Modifier.tap(onClick) else Modifier).padding(8.dp),
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

/** Загрузки: активные с паузой/отменой, установленные с «Играть» (готовые — сверху), память. */
@Composable
fun DownloadsDrawer() {
    val ctx = LocalContext.current
    val act = AppState.active()
    val done = AppState.lib.filter { it.value.s == "ok" }.keys.sortedByDescending { it in AppState.readyIds }
    Drawer("Загрузки") {
        com.nexus.psp.ui.kit.Sub("Сейчас")
        if (act.isEmpty()) Text("Активных загрузок нет", style = T.small)
        act.forEach { id ->
            val g = AppState.byId[id] ?: return@forEach; val e = AppState.lib[id] ?: return@forEach
            Item(id, g.t, AppState.stat(id).lbl + " · " + size(g.size)) {
                KButton(icon = if (e.s == "dl") R.drawable.sb_pause else R.drawable.sb_play, small = true) { if (e.s == "dl") AppState.pause(id) else AppState.resume(id) }
                KButton(icon = R.drawable.ph_x, small = true) { AppState.cancel(id) }
            }
            Bar(e.pr / 100f, if (e.s == "dl") K.acc else K.warn, modifier = Modifier.padding(start = 72.dp, end = 8.dp, bottom = 6.dp))
        }
        Sub("Установлены")
        done.forEach { id ->
            val g = AppState.byId[id] ?: return@forEach
            Item(id, g.t, g.plat.short + " · " + size(g.size), onClick = { Nav.open(Ov.Game(id)) }) {
                KButton("Играть", kind = BtnKind.Main, small = true) { Act.play(ctx, id) }
            }
        }
        Sub("Память")
        Bar((AppState.TOTAL - AppState.free()).toFloat() / AppState.TOTAL, height = 8.dp)
        Text("Игры KAPSULA ${size(AppState.used())} · свободно ${size(AppState.free())} (демо)", style = T.small, modifier = Modifier.padding(top = 6.dp))
        if (AppState.free() < 3000) StateCard("es-storage.png", "Мало памяти.", "Удали с телефона игры, в которые не играешь, — они останутся в библиотеке.")
    }
}

/** Уведомления; открытие гасит точку колокольчика через секунду. */
@Composable
fun AlertsDrawer() {
    LaunchedEffect(Unit) { delay(1200); AppState.readAll() }
    Drawer("Уведомления", actions = { KButton("Прочитать все", small = true) { AppState.readAll() } }) {
        if (AppState.alerts.isEmpty()) Text("Пока ничего нового", style = T.small)
        AppState.alerts.forEach { a ->
            Item(a.g, a.t, a.d, unread = a.u, icon = R.drawable.sl_bolt, onClick = { if (a.g != null && AppState.byId[a.g] != null) Nav.open(Ov.Game(a.g)) else Nav.open(Ov.Settings) })
        }
    }
}

private fun padState(): String {
    val d = InputDevice.getDeviceIds().toList().mapNotNull { InputDevice.getDevice(it) }.firstOrNull { !it.isVirtual && (it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) }
    return d?.name?.take(22) ?: "Не подключён"
}

/** Быстрая панель (п. 16): плитки-переключатели и ползунки громкости/яркости. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPanel() {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).tap { Nav.back() })
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)).background(K.bg).tap { }
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            com.nexus.psp.ui.OvHead("Быстрая панель")
            val tiles = listOf(
                Tile(R.drawable.sd_pad, "Геймпад", padState(), false) { Nav.settingsTab = "ctl"; Nav.replace(Ov.Settings) },
                Tile(R.drawable.ph_wifi, "Только Wi-Fi", if (S.wifi) "Вкл" else "Выкл", S.wifi) { S.wifi = !S.wifi },
                Tile(R.drawable.sd_battery, "Экономия", if (S.saver) "Вкл" else "Выкл", S.saver) { S.saver = !S.saver },
                Tile(R.drawable.sd_moon, "Тема", mapOf("dark" to "Тёмная", "oled" to "OLED", "light" to "Светлая")[S.theme] ?: "", false) {
                    S.theme = mapOf("dark" to "oled", "oled" to "light", "light" to "dark")[S.theme] ?: "dark"; AppState.applyLook()
                },
                Tile(R.drawable.ph_hand, "Вибрация", if (S.vib) "Вкл" else "Выкл", S.vib) { S.vib = !S.vib },
                Tile(R.drawable.sd_music, "Звуки", if (S.sound) "Вкл" else "Выкл", S.sound) { S.sound = !S.sound },
                Tile(R.drawable.sd_device, "Экранный геймпад", mapOf("touch" to "KAPSULA Touch", "classic" to "Классика", "pad" to "Геймпад")[S.ctlMode] ?: "", false) { Nav.replace(Ov.Pad) },
                Tile(R.drawable.sd_gear, "Настройки", "Все параметры", false) { Nav.replace(Ov.Settings) },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 4) {
                tiles.forEach { t -> QTile(t, Modifier.weight(1f)) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(K.s1).padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SolarIcon(R.drawable.sl_vol, 20.dp, K.ink2); Range(S.vol, 0, 100, fill = true) { S.vol = it }
                }
                Row(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(K.s1).padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
        if (t.on) SolarIcon(t.icon, 24.dp, K.bg) else SolarDuo(t.icon, 24.dp)
        Column {
            Text(t.title, style = T.label.copy(color = if (t.on) K.bg else K.ink))
            Text(t.sub, style = T.small.copy(color = if (t.on) K.bg.copy(alpha = .65f) else K.ink3))
        }
    }
}

/** Профиль (п. 10): уровень, цифры, достижения, облако, история по дням, избранное. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDrawer() {
    val ok = AppState.lib.filter { it.value.s == "ok" }
    val play = ok.values.sumOf { it.play }
    val svs = ok.values.sumOf { it.saves }
    val xp = play * 2 + svs * 30
    Drawer("Профиль") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(K.ink), contentAlignment = Alignment.Center) { Text("N", style = T.h2.copy(color = K.bg)) }
            Column {
                Text("Игрок", style = T.h2)
                Text("Уровень ${xp / 1000 + 1} · ${xp % 1000} / 1000 XP", style = T.small)
                Bar((xp % 1000) / 1000f, modifier = Modifier.width(200.dp).padding(top = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("${play / 60} ч" to "наиграно", "${AppState.lib.size}" to "в библиотеке", "$svs" to "сохранений", "${AppState.favs.size}" to "в избранном").forEach { (n, l) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(K.s1).padding(10.dp)) { Text(n, style = T.h3); Text(l, style = T.small) }
            }
        }
        Sub("Достижения")
        val ach = listOf(
            Triple("ach-first.png", "Первый запуск", ok.isNotEmpty()),
            Triple("ach-marathon.png", "10 часов в игре", ok.values.any { it.play >= 600 }),
            Triple("ach-saves.png", "10 сохранений", svs >= 10),
            Triple("ach-consoles.png", "Три консоли", listOf("psp", "ps1", "ps2").all { p -> ok.keys.any { AppState.byId[it]?.p == p } }),
            Triple("ach-trophy.png", "100 часов", play >= 6000),
            Triple("ach-fav.png", "5 в избранном", AppState.favs.size >= 5),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
            ach.forEach { (img, t, got) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(K.s1).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AssetImage(img, Modifier.size(56.dp), gray = !got)
                    Text(t, style = T.small.copy(color = if (got) K.ink else K.ink3, fontSize = 11.sp), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Sub("Облако")
        Opt("Продолжить на другом устройстве", "Сохранения и библиотека в аккаунте (в планах)") { Toggle(S.cloud) { S.cloud = it } }
        Sub("История по дням")
        listOf("Сегодня" to 95, "Вчера" to 140, "Ср" to 40, "Вт" to 0, "Пн" to 75).forEach { (d, m) ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(d, style = T.label, modifier = Modifier.width(70.dp))
                Box(Modifier.width((maxOf(4f, m / 140f * 180f)).dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(K.acc))
                Text(if (m > 0) hrs(m) else "—", style = T.small)
            }
        }
        Sub("Избранное")
        AppState.favs.keys.mapNotNull { AppState.byId[it] }.forEach { g -> Item(g.id, g.t, g.plat.short + " · " + AppState.stat(g.id).lbl, onClick = { Nav.open(Ov.Game(g.id)) }) }
    }
}
