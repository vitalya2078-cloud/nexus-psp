package com.nexus.psp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import com.nexus.psp.ui.kit.tap

/** Шапка v2: бренд, вкладки; справа — поиск, загрузки (кольцо), колокольчик, быстрая панель, профиль. */
@Composable
fun Header() {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.displayCutout).height(60.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.tap { Nav.go("games") }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LogoMark(40.dp)
            Text("KAPSULA", style = T.label.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.5.sp))
        }
        Row(Modifier.padding(start = 28.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            TabLabel("Игры", Nav.tab == "games") { Nav.go("games") }
            TabLabel("Магазин", Nav.tab == "store") { Nav.go("store") }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(R.drawable.sl_search) { Nav.open(Ov.Search) }
            DownloadsBtn()
            BellBtn()
            IconBtn(R.drawable.sl_grid) { Nav.open(Ov.Quick) }
            Box(
                Modifier.padding(start = 4.dp).size(38.dp).clip(CircleShape).background(K.ink).press { Nav.open(Ov.Profile) },
                contentAlignment = Alignment.Center,
            ) { Text("N", style = T.label.copy(color = K.bg, fontWeight = FontWeight.ExtraBold)) }
        }
    }
}

@Composable
private fun TabLabel(text: String, on: Boolean, onClick: () -> Unit) {
    val line by animateFloatAsState(if (on) 1f else 0f, tween(220), label = "tab")
    Column(Modifier.tap(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = T.label.copy(fontSize = 16.sp, color = if (on) K.ink else K.ink3))
        Box(Modifier.padding(top = 5.dp).height(2.5.dp).width(28.dp).graphicsLayer { scaleX = line; alpha = line }.clip(RoundedCornerShape(2.dp)).background(K.acc))
    }
}

@Composable
private fun IconBtn(icon: Int, content: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).press(onClick = onClick), contentAlignment = Alignment.Center) {
        SolarIcon(icon, 21.dp, K.ink2)
        content()
    }
}

/** Колокольчик: синяя точка-капсула при непрочитанных; новое уведомление — один «взмах». */
@Composable
private fun BellBtn() {
    val unread = AppState.alerts.any { it.u }
    val swing = remember { Animatable(0f) }
    val n = AppState.alerts.size
    val first = remember { mutableIntStateOf(n) }
    LaunchedEffect(n) {
        if (n > first.intValue) { swing.snapTo(0f); swing.animateTo(1f, tween(800)) }
        first.intValue = n
    }
    Box(Modifier.size(42.dp).clip(CircleShape).press { Nav.open(Ov.Alerts) }, contentAlignment = Alignment.Center) {
        val t = swing.value
        val ang = if (t in 0.01f..0.99f) kotlin.math.sin(t * 4 * Math.PI).toFloat() * 16f * (1 - t) else 0f
        SolarIcon(R.drawable.sl_bell, 21.dp, K.ink2, Modifier.graphicsLayer { rotationZ = ang; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, .12f) })
        CapsuleDot(unread, K.acc)
    }
}

private val TRAY = PathParser().parsePathString("M6 13.5v1.2a3.3 3.3 0 0 0 3.3 3.3h5.4a3.3 3.3 0 0 0 3.3-3.3v-1.2").toPath()
private val ARROW = PathParser().parsePathString("M12 5.5v7M9.2 9.9 12 12.7l2.8-2.8").toPath()
private val CHECK = PathParser().parsePathString("M9.3 11.6l1.9 1.9 3.6-3.8").toPath()

/**
 * Кольцо загрузки: две половины заполняются синим снизу вверх; пауза — янтарное;
 * готово — зелёное с галочкой, пульсирует и держится до нажатия; зелёная точка, если готовые есть, пока качается другая.
 */
@Composable
private fun DownloadsBtn() {
    // пока магазина нет, кольцо показывает копирование своих файлов; готово — зелёное, держится до нажатия
    val im = AppState.importing
    var doneFlag by remember { androidx.compose.runtime.mutableStateOf(false) }
    var wasImporting by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(im != null) { if (im == null && wasImporting) doneFlag = true; wasImporting = im != null }
    val pr = (im?.p ?: 0f) * 100f
    val ready = doneFlag
    val mode = when { im != null -> "dl"; doneFlag -> "done"; else -> "idle" }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val fill by animateFloatAsState(if (mode == "done") 1f else maxOf(.03f, pr / 100f), tween(600), label = "ring")
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(mode) {
        if (mode == "done") { Act.buzzLong(); repeat(3) { pulse.animateTo(1.13f, tween(300)); pulse.animateTo(1f, tween(300)) } }
    }
    // анимация стрелки — только пока идёт копирование; значение читается при рисовании, без перекомпоновки шапки
    val dropState: androidx.compose.runtime.State<Float> = if (mode == "dl") rememberInfiniteTransition(label = "drop").animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "d")
        else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Box(
        Modifier.size(42.dp).clip(CircleShape).press {
            if (mode == "done") { doneFlag = false; Nav.go("games") } else Nav.open(Ov.Downloads)
        },
        contentAlignment = Alignment.Center,
    ) {
        if (mode == "idle") SolarIcon(R.drawable.sl_dl, 21.dp, K.ink2)
        else {
            val col = when (mode) { "pause" -> K.warn; "done" -> K.ok; else -> K.acc }
            val track = K.ink.copy(alpha = .17f)
            val ink = if (mode == "done") K.ok else K.ink
            Canvas(Modifier.size(34.dp).scale(pulse.value)) {
                val sw = 2.6.dp.toPx(); val r = size.minDimension / 2 - sw / 2
                val tl = Offset(size.width / 2 - r, size.height / 2 - r); val sz = Size(r * 2, r * 2)
                drawCircle(track, r, style = Stroke(sw))
                drawArc(col, 90f, 180f * fill, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(col, 90f, -180f * fill, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                // значок «стрелка падает в капсулу-лоток», нарисован в сетке 24
                val k = 19.dp.toPx() / 24f
                translate((size.width - 24 * k) / 2, (size.height - 24 * k) / 2) {
                    scale(k, k, pivot = Offset.Zero) {
                        val st = Stroke(1.6f, cap = StrokeCap.Round)
                        drawPath(TRAY, ink, style = st)
                        if (mode == "done") drawPath(CHECK, ink, style = Stroke(1.7f, cap = StrokeCap.Round))
                        else {
                            val drop = dropState.value
                            val y = if (mode == "dl") -2.5f + drop * 4f else 0f
                            val a = if (mode == "dl") (if (drop < .25f) .3f + drop * 2.8f else if (drop > .75f) .3f + (1 - drop) * 2.8f else 1f) else .55f
                            translate(0f, y) { drawPath(ARROW, ink.copy(alpha = a), style = st) }
                        }
                    }
                }
            }
        }
        CapsuleDot(false, K.ok)
    }
}

private fun mutableIntStateOf(v: Int) = androidx.compose.runtime.mutableIntStateOf(v)
