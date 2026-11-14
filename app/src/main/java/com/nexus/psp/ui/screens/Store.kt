package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.StoreCatalog
import com.nexus.psp.data.StoreGame
import com.nexus.psp.data.installToLibrary
import com.nexus.psp.data.realGame
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.NetImage
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.launch

/** Открытая карточка игры магазина (снаружи — для снимков экрана). */
object StoreUi { val open = mutableStateOf<String?>(null) }

/** Магазин: бесплатные игры, которые разрешено распространять. Категории, карточки, установка в один тап. */
@Composable
fun StoreScreen() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { StoreCatalog.load(ctx) }
    var cat by remember { mutableStateOf("all") }
    var open by StoreUi.open
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Бесплатные игры для PSP", style = T.h3)
                Text("Только то, что авторы разрешили распространять: открытые лицензии, homebrew и демо-сцена", style = T.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (StoreCatalog.games.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("all" to "Все") + StoreCatalog.cats.toList().filter { (k, _) -> StoreCatalog.games.any { it.category == k } }).forEach { (k, n) ->
                    val on = cat == k
                    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) K.ink else K.s1).press { cat = k }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(n, style = T.label.copy(fontSize = 13.sp, color = if (on) K.bg else K.ink2))
                    }
                }
            }
        }
        when {
            StoreCatalog.games.isEmpty() && StoreCatalog.state == "offline" -> StateCard("es-offline.png", "Нет интернета", "Магазину нужна сеть, чтобы показать игры. Свои игры работают без интернета.") {
                KButton("Повторить", small = true) { AppState.scope.launch { StoreCatalog.load(ctx, force = true) } }
            }
            StoreCatalog.games.isEmpty() && StoreCatalog.state == "fail" -> StateCard("es-nothing.png", "Магазин не открылся", "Попробуй чуть позже.") {
                KButton("Повторить", small = true) { AppState.scope.launch { StoreCatalog.load(ctx, force = true) } }
            }
            StoreCatalog.games.isEmpty() -> Text("Загружаю каталог…", style = T.small, modifier = Modifier.padding(top = 20.dp))
            else -> LazyVerticalGrid(GridCells.Adaptive(220.dp), Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
                items(StoreCatalog.games.filter { cat == "all" || it.category == cat }, key = { it.id }) { g -> StoreCard(g) { open = g.id } }
            }
        }
    }
    StoreCatalog.games.firstOrNull { it.id == open }?.let { g -> StoreDetail(g) { open = null } }
}

/** Картинка игры: значок из EBOOT (144×80), без него — обложка KAPSULA по названию. */
@Composable
private fun StoreArt(g: StoreGame, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(K.s2)) {
        Cover(realGame("store_" + g.id, g.name, g.id + ".pbp", "", 0.0, g.plat), 400, 220, modifier = Modifier.fillMaxSize())
        if (g.icon != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)))
            NetImage(g.icon, Modifier.fillMaxSize().padding(10.dp), ContentScale.Fit)
        }
    }
}

@Composable
private fun StoreCard(g: StoreGame, onOpen: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(18.dp)).background(K.s1).press(onClick = onOpen).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StoreArt(g, Modifier.fillMaxWidth().aspectRatio(144f / 80f))
        Text(g.name, style = T.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${StoreCatalog.cats[g.category] ?: ""} · ${mb(g.unpacked.takeIf { it > 0 } ?: g.size)}", style = T.small, maxLines = 1)
        InstallButton(g, Modifier.fillMaxWidth())
    }
}

private fun mb(b: Long) = if (b >= 1048576) "${(b + 524288) / 1048576} МБ" else "${maxOf(1, b / 1024)} КБ"

/** Кнопка: «Установить» → прогресс → «Играть». */
@Composable
private fun InstallButton(g: StoreGame, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val p = StoreCatalog.progress[g.id]
    val lib = AppState.byId[StoreCatalog.libId(g)]
    when {
        p != null -> Column(modifier.height(40.dp), verticalArrangement = Arrangement.Center) {
            Text(if (p < .96f) "Загрузка ${(p * 100).toInt()}%" else "Распаковка…", style = T.small.copy(fontSize = 11.sp))
            Bar(p, modifier = Modifier.padding(top = 4.dp))
        }
        lib != null -> KButton("Играть", R.drawable.sb_play, BtnKind.Main, small = true) { Act.play(ctx, lib.id) }
        else -> KButton("Установить", R.drawable.sl_dl, small = true) {
            if (StoreCatalog.progress.isNotEmpty()) { AppState.say("Подожди — уже ставлю другую игру"); return@KButton }
            // установка не прерывается, если уйти с экрана магазина
            AppState.scope.launch {
                try {
                    val added = StoreCatalog.installToLibrary(ctx.applicationContext, g)
                    if (added != null) AppState.say("«${g.name}» установлена", R.drawable.ph_check, "Играть") { Act.play(ctx, added.id) }
                } catch (e: Exception) { AppState.say(e.message ?: "Не удалось установить", R.drawable.sl_alert) }
            }
        }
    }
}

/** Карточка игры: описание, автор, лицензия, исходники, размер, установка и удаление. */
@Composable
private fun StoreDetail(g: StoreGame, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val lib = AppState.byId[StoreCatalog.libId(g)]
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth(.9f).fillMaxHeight(.9f).clip(RoundedCornerShape(24.dp)).background(K.bg).border(1.dp, K.line, RoundedCornerShape(24.dp))) {
            g.bg?.let { NetImage(it, Modifier.fillMaxSize(), ContentScale.Crop) }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(K.bg, K.bg.copy(alpha = .92f), K.bg.copy(alpha = .55f)))))
            Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StoreArt(g, Modifier.fillMaxWidth().aspectRatio(144f / 80f))
                    InstallButton(g, Modifier.fillMaxWidth())
                    if (lib != null) KButton("Удалить", R.drawable.sl_trash, small = true) {
                        if (com.nexus.psp.emulator.GameLibrary.delete(ctx, lib.path)) { AppState.loadReal(); AppState.say("«${g.name}» удалена") }
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.name, style = T.h1, modifier = Modifier.weight(1f), maxLines = 2)
                        KButton(icon = R.drawable.ph_x, small = true) { onClose() }
                    }
                    Text(listOfNotNull(StoreCatalog.cats[g.category], g.plat.uppercase(), mb(g.unpacked.takeIf { it > 0 } ?: g.size)).joinToString(" · "), style = T.small)
                    Text(g.desc, style = T.body)
                    Info(R.drawable.sl_user, "Автор", g.author)
                    Info(R.drawable.sl_book, "Лицензия", g.license)
                    g.source?.let { Link(R.drawable.sl_globe, "Исходный код", it) }
                    g.site?.takeIf { it != g.source }?.let { Link(R.drawable.sl_globe, "Страница игры", it) }
                    Text("Игра распространяется с разрешения автора или по открытой лицензии. KAPSULA не меняет её файлы.", style = T.small.copy(fontSize = 11.sp), modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun Info(icon: Int, t: String, v: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SolarIcon(icon, 18.dp, K.ink3)
        Text(t, style = T.small, modifier = Modifier.width(110.dp))
        Text(v, style = T.label.copy(fontSize = 13.5.sp), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Link(icon: Int, t: String, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.clip(RoundedCornerShape(8.dp)).press { openWeb(ctx, url) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SolarIcon(icon, 18.dp, K.ink3)
        Text(t, style = T.small, modifier = Modifier.width(110.dp))
        Text(url.removePrefix("https://").removePrefix("http://").removePrefix("www."), style = T.label.copy(fontSize = 13.sp, color = K.acc), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
