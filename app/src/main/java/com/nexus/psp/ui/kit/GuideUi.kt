package com.nexus.psp.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexus.psp.emulator.ControlLayout
import com.nexus.psp.emulator.ControlsView
import com.nexus.psp.emulator.Guide
import com.nexus.psp.emulator.GuideMode
import com.nexus.psp.emulator.GuideRow
import org.json.JSONObject

/** Маскот Сейвик: картинка из набора (mascot-<поза>.webp). В светлой теме — на тёмном круге, чтобы свечение читалось. */
@Composable
fun Mascot(pose: String, size: Dp, modifier: Modifier = Modifier) {
    // позы, у которых есть живая пара, — живой маскот (дышит, моргает, отзывается на касание)
    val live = LIVE[pose]
    val img: @Composable (Modifier, Dp) -> Unit = { m, s -> if (live != null) LiveMascot(live, s, m, glow = false) else AssetImage("mascot-$pose.webp", m.size(s)) }
    if (K.dark) img(modifier, size)
    else Box(modifier.size(size).clip(CircleShape).background(Color(0xFF14151A)), contentAlignment = Alignment.Center) { img(Modifier, size * .86f) }
}

private val LIVE = mapOf("wave" to "wave", "sad" to "sad", "joy" to "jump", "hug" to "heart", "wait" to "think")

/** Значок кнопки KAPSULA Touch (ctl_<имя>) — тот же, что на экране игры. */
@Composable
fun CtlIcon(name: String?, size: Dp, tint: Color = K.ink, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val id = remember(name) { if (name == null) 0 else ctx.resources.getIdentifier("ctl_$name", "drawable", ctx.packageName) }
    if (id != 0) Icon(painterResource(id), null, modifier.size(size), tint = tint) else Box(modifier.size(size))
}

/** Кнопка консоли: фигуры рисуются, остальное — короткой надписью на плашке. */
@Composable
fun KeyChip(key: String) {
    val shape = RoundedCornerShape(7.dp)
    val glyph = key in setOf("ft", "fr", "fb", "fl")
    Box(
        Modifier.height(24.dp).widthIn(min = 24.dp).clip(shape).background(K.s2).border(1.dp, K.line, shape).padding(horizontal = if (glyph) 0.dp else 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph) Canvas(Modifier.size(24.dp)) {
            val c = Offset(size.width / 2, size.height / 2); val r = size.minDimension * .26f; val w = 2.dp.toPx()
            when (key) {
                "ft" -> drawPath(Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r * 1.08f, c.y + r * .78f); lineTo(c.x - r * 1.08f, c.y + r * .78f); close() }, Color(ControlLayout.AMBER), style = Stroke(w))
                "fr" -> drawCircle(Color(ControlLayout.BLUE), r * .92f, c, style = Stroke(w))
                "fb" -> { val q = r * .78f; drawLine(Color(ControlLayout.GREEN), Offset(c.x - q, c.y - q), Offset(c.x + q, c.y + q), w); drawLine(Color(ControlLayout.GREEN), Offset(c.x + q, c.y - q), Offset(c.x - q, c.y + q), w) }
                else -> { val q = r * .78f; drawRect(Color(ControlLayout.ROSE), Offset(c.x - q, c.y - q), Size(q * 2, q * 2), style = Stroke(w)) }
            }
        } else Text(Guide.KEY_NAMES[key] ?: key, style = T.num.copy(fontSize = 11.5.sp, color = K.ink2), maxLines = 1)
    }
}

/** Превью раскладки как в игре: те же кнопки и места, с подписями. */
@Composable
fun GuidePreview(mode: GuideMode, ctl: JSONObject?, plat: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().aspectRatio(844f / 390f).clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF26304A), Color(0xFF4A3A4C), Color(0xFF6B4E45))))
            .border(1.dp, K.line, RoundedCornerShape(18.dp)),
    ) {
        AndroidView(
            factory = { c -> ControlsView(c, object : ControlsView.Listener { override fun key(code: Int, down: Boolean) {}; override fun stick(right: Boolean, x: Float, y: Float) {}; override fun action(act: String) {} }).apply { preview = true; captions = true } },
            update = { v -> v.layName = "${mode.spec.id}:$plat"; v.platform = plat; v.applyCtl(ctl); v.setLayout(mode.btns) },
            modifier = Modifier.matchParentSize(),
        )
        // зона камеры свайпом — мягкой подсветкой правой половины
        if (mode.btns.any { it.cam != null }) Row(
            Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .45f)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) { CtlIcon("cam", 14.dp, Color(0xFFF2F1EC)); Text("свайп — камера", style = T.small.copy(fontSize = 11.sp, color = Color(0xFFF2F1EC))) }
    }
}

/** Строка таблицы: значок · действие (+ пояснение) · кнопки консоли. */
@Composable
fun GuideRowView(r: GuideRow, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(K.s2), contentAlignment = Alignment.Center) {
            if (r.icon in setOf("ft", "fr", "fb", "fl")) KeyChip(r.icon!!) else CtlIcon(r.icon ?: "stick", 19.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(r.name, style = T.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(r.how, if (r.rare) "в ⋯" else null).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = T.small.copy(fontSize = 11.5.sp), maxLines = 1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            r.keys.forEachIndexed { i, k -> if (i > 0) Text("+", style = T.small.copy(fontSize = 11.sp)); KeyChip(k) }
        }
    }
}

/**
 * Гайд по раскладке: вкладки режимов (если их несколько), превью, 2–3 главные подсказки и таблица кнопок.
 * Один и тот же — при первом запуске игры, в паузе, в карточке игры и во вкладке «Управление».
 */
@Composable
fun GuidePanel(modes: List<GuideMode>, ctl: JSONObject?, plat: String, wide: Boolean, modifier: Modifier = Modifier) {
    var sel by remember(modes) { mutableIntStateOf(0) }
    val m = modes.getOrNull(sel) ?: return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (modes.size > 1) Seg(modes.map { it.spec.name }, sel, small = true) { sel = it }
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(12.dp)) { GuidePreview(m, ctl, plat); Tips(m) }
            Rows(m, Modifier.weight(1f))
        } else { GuidePreview(m, ctl, plat); Tips(m); Rows(m, Modifier) }
    }
}

@Composable
private fun Tips(m: GuideMode) {
    if (m.tips.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        m.tips.forEach { (ic, t) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CtlIcon(ic, 18.dp, K.acc); Text(t, style = T.body.copy(fontSize = 13.sp, lineHeight = 18.sp))
            }
        }
    }
}

@Composable
private fun Rows(m: GuideMode, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
        val (main, rare) = m.rows.partition { !it.rare }
        main.forEach { GuideRowView(it) }
        if (rare.isNotEmpty()) {
            Text("В ПАНЕЛИ ⋯", style = T.caps, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            rare.forEach { GuideRowView(it) }
        }
    }
}
