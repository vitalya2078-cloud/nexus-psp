package com.nexus.psp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.tap
import com.nexus.psp.ui.screens.AlertsDrawer
import com.nexus.psp.ui.screens.DownloadsDrawer
import com.nexus.psp.ui.screens.GameSettingsScreen
import com.nexus.psp.ui.screens.GameSheet
import com.nexus.psp.ui.screens.GamesScreen
import com.nexus.psp.ui.screens.LibraryScreen
import com.nexus.psp.ui.screens.Onboarding
import com.nexus.psp.ui.screens.PadScreen
import com.nexus.psp.ui.screens.ProfileScreen
import com.nexus.psp.ui.screens.QuickPanel
import com.nexus.psp.ui.screens.SearchScreen
import com.nexus.psp.ui.screens.SettingsScreen
import com.nexus.psp.ui.screens.StoreScreen
import kotlinx.coroutines.delay

/** Фоновый оттенок под выбранную игру (п. 25). */
object Ambient {
    var color by mutableStateOf(Color.Transparent)
    /** Игра в фокусе карусели: её фон рисуется на весь экран, под шапкой тоже. */
    var game by mutableStateOf<com.nexus.psp.data.Game?>(null)
}

@Composable
fun KapsulaApp() {
    var splash by remember { mutableStateOf(true) }
    var onb by remember { mutableStateOf(!S.onboarded) }
    val tint by androidx.compose.animation.animateColorAsState(Ambient.color, tween(900), label = "amb")
    val bright = S.bright / 100f
    Box(
        Modifier.fillMaxSize().background(K.bg)
            .drawBehind { if (tint.alpha > 0f) drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = if (K.dark) .22f else .1f), Color.Transparent), Offset(size.width * .7f, size.height * .2f), size.maxDimension * .7f), size.maxDimension, Offset(size.width * .7f, size.height * .2f)) }
            .graphicsLayer { if (bright < 1f) alpha = 1f },
    ) {
        // фон игры — до краёв экрана (без отступа под вырез камеры) и под шапкой: нет «полки» и тёмной полосы слева
        if (Nav.tab == "games" && Ambient.game != null) {
            GameBackdrop(Ambient.game, Modifier.fillMaxSize(), strength = .55f)
            // мягкое затемнение сверху: шапка читается на любом фоне и получает глубину
            Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(K.bg.copy(alpha = .82f), K.bg.copy(alpha = .35f), Color.Transparent))))
        }
        Column(Modifier.fillMaxSize()) {
            Header()
            AnimatedContent(Nav.tab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "tab", modifier = Modifier.weight(1f)) { t ->
                if (t == "store") StoreScreen() else GamesScreen()
            }
        }
        Overlays()
        CtxMenu()
        ToastHost(Modifier.align(Alignment.BottomCenter))
        Importing(Modifier.align(Alignment.BottomCenter))
        Scanning(Modifier.align(Alignment.BottomCenter))
        FoundDialog()
        AskDialog()
        if (onb && !splash) Onboarding { onb = false }
        if (splash) Splash(full = !S.launched) { splash = false; S.launched = true }
        // яркость интерфейса (быстрая панель)
        if (bright < 1f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 1f - bright)))
    }
    BackHandler(enabled = Nav.stack.isNotEmpty() && Ctx.id == null) { Nav.back() }
    BackHandler(enabled = Nav.stack.isEmpty() && Nav.tab == "store" && Ctx.id == null) { Nav.go("games") }
}

@Composable
private fun Overlays() {
    // все открытые слои; видимый — последний (под ним остаются предыдущие для «Назад»)
    val top = Nav.top ?: return
    Box(Modifier.fillMaxSize()) {
        when (top) {
            is Ov.Game -> GameSheet(top.id)
            is Ov.GSet -> GameSettingsScreen(top.id)
            Ov.Library -> LibraryScreen()
            Ov.Search -> SearchScreen()
            Ov.Downloads -> DownloadsDrawer()
            Ov.Alerts -> AlertsDrawer()
            Ov.Quick -> QuickPanel()
            Ov.Settings -> SettingsScreen()
            Ov.Profile -> ProfileScreen()
            Ov.Pad -> PadScreen()
        }
    }
}

@Composable
private fun ToastHost(modifier: Modifier) {
    val t = AppState.toast
    LaunchedEffect(t?.id) { if (t != null) { delay(if (t.action != null) 5000 else 2600); if (AppState.toast?.id == t.id) AppState.toast = null } }
    AnimatedVisibility(t != null, modifier.padding(bottom = 16.dp), enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        val v = t ?: return@AnimatedVisibility
        Row(
            Modifier.clip(RoundedCornerShape(24.dp)).background(K.ink).padding(start = 18.dp, end = if (v.action != null) 6.dp else 18.dp, top = 6.dp, bottom = 6.dp).height(32.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (v.icon != null) SolarIcon(v.icon, 18.dp, K.bg)
            Text(v.text, style = T.label.copy(color = K.bg), maxLines = 1)
            if (v.action != null) Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(K.bg).tap { AppState.toast = null; v.onAction?.invoke() }.padding(horizontal = 13.dp, vertical = 6.dp),
            ) { Text(v.action, style = T.label.copy(fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)) }
        }
    }
}

/** Копирование своих файлов: имя, прогресс, «2 из 5» — внизу экрана. */
@Composable
private fun Importing(modifier: Modifier) {
    val im = AppState.importing ?: return
    Column(
        modifier.padding(bottom = 64.dp).width(380.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Копирую «${im.name}» · ${(im.p * 100).toInt()}%" + (if (im.total > 1) " · ${im.n} из ${im.total}" else ""), style = T.label, maxLines = 1)
        com.nexus.psp.ui.kit.Bar(im.p)
    }
}

/** «Найти игры на телефоне»: что нашлось и подтверждение копирования. */
@Composable
private fun FoundDialog() {
    val f = AppState.found ?: return
    androidx.compose.ui.window.Dialog({ AppState.found = null }) {
        Column(Modifier.width(520.dp).clip(RoundedCornerShape(22.dp)).background(K.s1).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (f.isEmpty()) {
                Text("Игр не нашлось", style = T.h3)
                Text("В этой папке и вложенных папках нет файлов ISO, CSO, CHD или PBP. Попробуй другую папку, например «Загрузки».", style = T.body)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    com.nexus.psp.ui.kit.KButton("Закрыть", small = true) { AppState.found = null }
                    com.nexus.psp.ui.kit.KButton("Другая папка", small = true, kind = com.nexus.psp.ui.kit.BtnKind.Main) { AppState.found = null; Act.findGames() }
                }
            } else {
                val new = f.filter { !it.exists }
                val total = new.sumOf { it.sizeMb }
                Text(if (new.isEmpty()) "Все найденные игры уже в библиотеке" else "Новых игр: ${new.size} · ${com.nexus.psp.data.size(total)}" + if (new.size < f.size) " (ещё ${f.size - new.size} уже есть)" else "", style = T.h3)
                Text("Файлы скопируются в папку KAPSULA. Свободно ${com.nexus.psp.data.size(AppState.freeMb())}.", style = T.body)
                Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    f.forEach { Text("• ${it.name} — " + if (it.exists) "уже есть" else com.nexus.psp.data.size(it.sizeMb), style = T.small.copy(color = if (it.exists) K.ink3 else K.ink2), maxLines = 1) }
                }
                if (total > AppState.freeMb()) Text("Места не хватит на все игры — скопируются, пока есть место.", style = T.small.copy(color = K.warn))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    com.nexus.psp.ui.kit.KButton("Отмена", small = true) { AppState.found = null }
                    if (new.isNotEmpty()) com.nexus.psp.ui.kit.KButton(if (new.size == 1) "Добавить" else "Добавить все", com.nexus.psp.R.drawable.ph_plus, com.nexus.psp.ui.kit.BtnKind.Main, small = true) { val l = f; AppState.found = null; Act.importAll?.invoke(l) }
                }
            }
        }
    }
}

/** Вопрос с вариантами (например, «Заменить / Оставить обе»). */
@Composable
private fun AskDialog() {
    val a = AppState.ask ?: return
    androidx.compose.ui.window.Dialog({ a.answer.complete(-1) }) {
        Column(Modifier.width(460.dp).clip(RoundedCornerShape(22.dp)).background(K.s1).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(a.title, style = T.h3)
            Text(a.text, style = T.body)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                a.options.forEachIndexed { i, o -> com.nexus.psp.ui.kit.KButton(o, small = true, kind = if (i == 0) com.nexus.psp.ui.kit.BtnKind.Main else com.nexus.psp.ui.kit.BtnKind.Soft) { a.answer.complete(i) } }
            }
        }
    }
}

/** Идёт сканирование папки. */
@Composable
private fun Scanning(modifier: Modifier) {
    if (!AppState.scanning) return
    Text("Ищу игры в папке…", style = T.label, modifier = modifier.padding(bottom = 64.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).padding(horizontal = 16.dp, vertical = 12.dp))
}

/** Фирменная заставка: капсула собирается, знак уезжает в шапку (п. 34). Первый запуск — полная, дальше — короткая. */
@Composable
private fun Splash(full: Boolean, onDone: () -> Unit) {
    val a = remember { Animatable(0f) }
    val out = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        val calm = S.calm
        a.animateTo(1f, tween(if (calm) 150 else if (full) 900 else 420, easing = FastOutSlowInEasing))
        if (full && !calm) { Act.buzz(); delay(450) }
        out.animateTo(0f, tween(if (calm) 120 else 380))
        onDone()
    }
    Box(Modifier.fillMaxSize().alpha(out.value).background(K.bg).tap { }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(96.dp, Modifier.graphicsLayer { scaleX = .6f + .4f * a.value; scaleY = .6f + .4f * a.value; alpha = a.value })
            Spacer(Modifier.height(16.dp))
            Text("KAPSULA", style = T.h2.copy(letterSpacing = (2 + 4 * a.value).sp), modifier = Modifier.alpha(a.value))
            Spacer(Modifier.height(4.dp).width(1.dp))
        }
    }
}
