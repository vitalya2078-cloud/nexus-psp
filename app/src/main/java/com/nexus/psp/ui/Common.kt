package com.nexus.psp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Game
import com.nexus.psp.data.PL
import com.nexus.psp.data.PLATFORMS
import com.nexus.psp.data.hrs
import com.nexus.psp.data.size
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.Chip
import com.nexus.psp.ui.kit.ChipRow
import com.nexus.psp.ui.kit.Ellip
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.tap

/** Главное действие: все игры библиотеки — свои файлы, поэтому всегда «Играть». */
@Composable
fun ActButton(g: Game, small: Boolean = false, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    KButton("Играть", R.drawable.sb_play, BtnKind.Main, small, modifier = modifier) { Act.play(ctx, g.id) }
}

/** Плашка формата файла на обложке (ISO, CSO…). */
@Composable
fun FormatBadge(g: Game, modifier: Modifier = Modifier) {
    if (g.format.isEmpty()) return
    Text(
        g.format, style = T.small.copy(color = Color(0xFFF2F1EC), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
        modifier = modifier.clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Метка консоли на обложке (.pl). */
@Composable
fun BoxScope.PlatformTag(g: Game) {
    Text(
        g.plat.short, style = T.small.copy(color = Color(0xFFF2F1EC), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold),
        modifier = Modifier.align(Alignment.TopStart).padding(7.dp).clip(RoundedCornerShape(7.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Карточка игры: обложка 3:4, название, подпись, действие. Долгое нажатие — контекстное меню. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCard(g: Game, width: Dp? = 132.dp, withAct: Boolean = true, mine: Boolean = false, modifier: Modifier = Modifier) {
    val e = AppState.lib[g.id]
    val info = if (mine && e != null && e.play > 0) "Наиграно ${hrs(e.play)}" else "${size(g.size)} · ${if (e?.play == 0) "не запускалась" else g.format}"
    Column(modifier.then(if (width != null) Modifier.width(width) else Modifier), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(14.dp))
                .border(1.dp, K.line, RoundedCornerShape(14.dp))
                .combinedClickable(remember { MutableInteractionSource() }, null, onLongClick = { Act.buzzLong(); Ctx.open(g.id) }) { Nav.open(Ov.Game(g.id)) },
        ) {
            Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
            PlatformTag(g)
            FormatBadge(g, Modifier.align(Alignment.BottomStart).padding(7.dp))
        }
        Ellip(g.t, T.label.copy(fontSize = 14.sp), Modifier.padding(top = 2.dp))
        Ellip(info, T.small)
        if (withAct) ActButton(g, small = true, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Чипы консолей со счётчиком (conChips()). */
@Composable
fun ConChips(src: List<Game>, modifier: Modifier = Modifier) {
    ChipRow(modifier) {
        Chip("Все", S.con == "all", count = src.size) { S.con = "all" }
        PLATFORMS.forEach { p -> Chip(p.short, S.con == p.id, dot = p.color, count = src.count { it.p == p.id }) { S.con = p.id } }
    }
}

// ---------- каркасы слоёв ----------

@Composable
private fun Scrim(onClose: () -> Unit) {
    val p = rememberEnter(180)
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = p.value }.background(Color.Black.copy(alpha = .55f)).tap(onClose))
}

/**
 * Появление слоя: 0 → 1 за ms (быстро, без пружин). «Уменьшить движение» — сразу.
 * Значение читается в graphicsLayer — кадры анимации не пересобирают содержимое слоя.
 */
@Composable
fun rememberEnter(ms: Int = 220): androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val a = remember { androidx.compose.animation.core.Animatable(if (com.nexus.psp.data.AppState.S.calm) 1f else 0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(ms, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    return a
}

/** Шапка слоя: заголовок + действия + закрыть. */
@Composable
fun OvHead(title: String? = null, back: Boolean = false, actions: @Composable () -> Unit = {}, leading: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (back) KButton(icon = R.drawable.sl_left) { Nav.back() }
        if (leading != null) leading()
        if (title != null) Text(title, style = T.h2, modifier = Modifier.weight(1f)) else Box(Modifier.weight(1f))
        actions()
        KButton(icon = R.drawable.ph_x) { Nav.back() }
    }
}

/** Боковая панель справа (загрузки, уведомления, профиль). */
@Composable
fun Drawer(title: String, actions: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Scrim { Nav.back() }
        val p = rememberEnter(240)
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(440.dp).graphicsLayer { translationX = (1f - p.value) * 72.dp.toPx(); alpha = .4f + .6f * p.value }
                .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)).background(K.bg).tap { }
                .windowInsetsPadding(WindowInsets.displayCutout),
        ) {
            OvHead(title, actions = actions)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 20.dp), content = content)
        }
    }
}

/** Полноэкранный слой (библиотека, поиск, настройки). */
@Composable
fun Full(content: @Composable ColumnScope.() -> Unit) {
    val p = rememberEnter(200)
    Column(
        Modifier.fillMaxSize().graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 14.dp.toPx() }
            .background(K.bg).tap { }.windowInsetsPadding(WindowInsets.displayCutout),
        content = content,
    )
}

// ---------- контекстное меню по долгому нажатию ----------

object Ctx {
    var id by mutableStateOf<String?>(null)
    fun open(i: String) { id = i }
}

@Composable
fun CtxMenu() {
    val id = Ctx.id ?: return
    val g = AppState.byId[id] ?: run { Ctx.id = null; return }
    val e = AppState.lib[id]
    BackHandler { Ctx.id = null }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .5f)).tap { Ctx.id = null })
        Column(
            Modifier.align(Alignment.Center).width(320.dp).clip(RoundedCornerShape(20.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(20.dp)).tap { }.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) { Cover(g, 300, 300, modifier = Modifier.fillMaxSize()) }
                Ellip(g.t, T.h3, Modifier.weight(1f))
            }
            ActButton(g, small = true, modifier = Modifier.padding(bottom = 6.dp))
            CtxItem(R.drawable.sl_info, "Об игре") { Ctx.id = null; Nav.open(Ov.Game(id)) }
            if (e != null) {
                CtxItem(if (AppState.favs[id] == true) R.drawable.sb_heart else R.drawable.sl_heart, if (AppState.favs[id] == true) "Убрать из избранного" else "В избранное") { AppState.toggleFav(id) }
                AppState.cols.forEach { (c, ids) -> CtxItem(if (id in ids) R.drawable.ph_check else R.drawable.sl_folder, if (id in ids) "В коллекции «$c»" else "Добавить в «$c»") { AppState.toggleCol(c, id) } }
                CtxItem(R.drawable.sl_trash, "Удалить с телефона", danger = true) { Ctx.id = null; AppState.uninstall(id) }
            }
        }
    }
}

@Composable
private fun CtxItem(icon: Int, text: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).tap(onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SolarIcon(icon, 18.dp, if (danger) K.bad else K.ink2)
        Text(text, style = T.label.copy(color = if (danger) K.bad else K.ink))
    }
}

// ---------- логотип ----------

/** Знак KAPSULA: капсула, перегородка посередине, синий треугольник «play» в левой половине. */
@Composable
fun LogoMark(w: Dp = 40.dp, modifier: Modifier = Modifier) {
    val ink = K.ink; val acc = K.acc
    androidx.compose.foundation.Canvas(modifier.width(w).height(w * .55f)) {
        val sw = size.height * .11f
        drawRoundRect(ink, Offset(sw / 2, sw / 2), Size(size.width - sw, size.height - sw), CornerRadius(size.height / 2), style = Stroke(sw))
        drawLine(ink, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), sw)
        val h = size.height * .42f; val x0 = size.width * .16f; val cy = size.height / 2
        drawPath(Path().apply { moveTo(x0, cy - h / 2); lineTo(x0 + h * .9f, cy); lineTo(x0, cy + h / 2); close() }, acc)
    }
}

/** Точка-капсула за верхним правым углом значка (бейджи шапки v2). */
@Composable
fun BoxScope.CapsuleDot(on: Boolean, color: Color) {
    val a by animateFloatAsState(if (on) 1f else 0f, tween(250), label = "dot")
    Box(
        Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 5.dp).graphicsLayer { alpha = a; scaleX = .3f + .7f * a; scaleY = .3f + .7f * a }
            .size(10.dp, 6.dp).clip(RoundedCornerShape(3.dp)).background(K.bg).padding(0.dp),
    ) { Box(Modifier.fillMaxSize().padding(0.dp).clip(RoundedCornerShape(3.dp)).background(color)) }
}

/** Отложенный старт эффекта (для анимаций появления). */
@Composable
fun rememberAppeared(): Boolean {
    var v by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { v = true }
    return v
}

fun consoleColor(p: String) = PL[p]?.color ?: K.psp
