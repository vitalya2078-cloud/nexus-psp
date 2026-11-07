package com.nexus.psp.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R

/**
 * Палитра KAPSULA «графит + слоновая кость» — те же токены, что в веб-прототипе.
 * Тема (dark / oled / light) и крупный текст переключаются на лету: цвета читаются из состояния.
 */
object K {
    var theme by mutableStateOf("dark")
    var contrast by mutableStateOf(false)
    private val light get() = theme == "light"

    val bg get() = when (theme) { "oled" -> Color(0xFF000000); "light" -> Color(0xFFE7E5E0); else -> Color(0xFF0B0C0F) }
    val s1 get() = when (theme) { "oled" -> Color(0xFF0B0B0D); "light" -> Color(0xFFF1EFEB); else -> Color(0xFF131419) }
    val s2 get() = when (theme) { "oled" -> Color(0xFF121215); "light" -> Color(0xFFFAF9F6); else -> Color(0xFF1A1C22) }
    val s3 get() = when (theme) { "oled" -> Color(0xFF1A1A1F); "light" -> Color(0xFFFFFFFF); else -> Color(0xFF23262E) }
    val ink get() = if (light) Color(0xFF1C1D20) else Color(0xFFF2F1EC)
    val ink2 get() = ink.copy(alpha = if (contrast) .9f else .74f)
    val ink3 get() = ink.copy(alpha = if (contrast) .75f else .52f)
    val line get() = if (light) Color(0x1A141416) else Color(0x14FFFFFF)
    val line2 get() = if (light) Color(0x33141416) else Color(0x29FFFFFF)
    val acc get() = if (light) Color(0xFF3E6699) else Color(0xFF7FA7D9)
    val ok get() = if (light) Color(0xFF2F7A4C) else Color(0xFF8FC9A3)
    val warn get() = if (light) Color(0xFF8A6418) else Color(0xFFD9B36C)
    val bad get() = if (light) Color(0xFFA34444) else Color(0xFFD98A8A)
    val psp = Color(0xFF9DB8D6)
    val ps1 = Color(0xFFB8B4AE)
    val ps2 = Color(0xFF8E96C4)
    val dark get() = !light
}

@OptIn(ExperimentalTextApi::class)
private fun manrope(w: Int) = Font(R.font.manrope, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

/** Типографика; size() учитывает «Крупный текст». */
object T {
    var big by mutableStateOf(false)
    private fun s(v: Float): TextUnit = (if (big) v * 1.12f else v).sp
    val h1 get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = s(26f), lineHeight = s(30f), color = K.ink)
    val h2 get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = s(20f), color = K.ink)
    val h3 get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = s(16f), color = K.ink)
    val body get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = s(14f), lineHeight = s(21f), color = K.ink2)
    val label get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = s(13.5f), color = K.ink)
    val small get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = s(12f), color = K.ink3)
    val caps get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = s(11f), letterSpacing = .8.sp, color = K.ink3)
    val num get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = s(13f), color = K.ink)
}

@Composable
fun KapsulaTheme(content: @Composable () -> Unit) {
    val cs = if (K.dark) darkColorScheme(primary = K.acc, background = K.bg, surface = K.s1, onPrimary = K.bg, onBackground = K.ink, onSurface = K.ink)
    else lightColorScheme(primary = K.acc, background = K.bg, surface = K.s1, onPrimary = Color.White, onBackground = K.ink, onSurface = K.ink)
    MaterialTheme(colorScheme = cs, content = content)
}

// ---------- значки и картинки ----------

/** Значок Solar: sl_ (линия) и sb_ (заливка) красятся; sd_ (duotone) — со своими цветами. */
@Composable
fun SolarIcon(res: Int, size: Dp = 22.dp, tint: Color = K.ink, modifier: Modifier = Modifier) =
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.size(size))

@Composable
fun SolarDuo(res: Int, size: Dp = 22.dp, modifier: Modifier = Modifier) =
    Icon(painterResource(res), contentDescription = null, tint = Color.Unspecified, modifier = modifier.size(size))

private val imgCache = HashMap<String, ImageBitmap?>()

/** Иллюстрации из assets/img (общие с прототипом: консоли, носители, пустые состояния, достижения). */
@Composable
fun AssetImage(name: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit, gray: Boolean = false) {
    val ctx = LocalContext.current
    val bmp = remember(name) {
        imgCache.getOrPut(name) {
            try { ctx.assets.open("img/$name").use { android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap() } } catch (e: Exception) { null }
        }
    } ?: return
    Image(
        bmp, null, modifier, contentScale = contentScale,
        colorFilter = if (gray) androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }) else null,
        alpha = if (gray) .45f else 1f,
    )
}

// ---------- взаимодействие ----------

/**
 * Нажимаемая поверхность: мягкая подсветка ровно по форме элемента (рябь внутри уже заданной обрезки).
 * Раньше здесь было уменьшение — оно сжимало только содержимое внутри фона и давало «двойную» кнопку.
 */
@Composable
fun Modifier.press(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(color = K.ink), enabled = enabled, onClick = onClick)

/** Клик без ряби (для подложек и строк). */
@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier = this.clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)

// ---------- кнопки ----------

enum class BtnKind { Main, Soft, Glass }

@Composable
fun KButton(
    text: String? = null, icon: Int? = null, kind: BtnKind = BtnKind.Soft, small: Boolean = false, enabled: Boolean = true,
    danger: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val h = if (small) 36.dp else 44.dp
    val bg = when (kind) { BtnKind.Main -> K.ink; BtnKind.Soft -> K.s2; BtnKind.Glass -> Color.Black.copy(alpha = .45f) }
    val fg = when { kind == BtnKind.Main -> K.bg; danger -> K.bad; kind == BtnKind.Glass -> Color(0xFFF2F1EC); else -> K.ink }
    val round = text == null
    Row(
        modifier.height(h).then(if (round) Modifier.width(h) else Modifier).clip(RoundedCornerShape(h / 2)).background(if (enabled) bg else bg.copy(alpha = .5f))
            .press(enabled, onClick).padding(horizontal = if (round) 0.dp else if (small) 13.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) SolarIcon(icon, if (small) 17.dp else 19.dp, if (enabled) fg else fg.copy(alpha = .5f))
        if (text != null) Text(text, style = T.label.copy(color = if (enabled) fg else fg.copy(alpha = .5f), fontSize = if (small) 13.sp else 14.sp), maxLines = 1)
    }
}

/** Фильтр-чип с точкой цвета консоли и счётчиком (как .chip). */
@Composable
fun Chip(text: String, on: Boolean = false, dot: Color? = null, count: Int? = null, icon: Int? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) K.ink else K.s2, label = "chip")
    val fg = if (on) K.bg else K.ink2
    Row(
        Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).press(onClick = onClick).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        if (icon != null) SolarIcon(icon, 15.dp, fg)
        Text(text, style = T.label.copy(color = fg, fontSize = 13.sp), maxLines = 1)
        if (count != null) Text("$count", style = T.small.copy(color = fg.copy(alpha = .6f), fontSize = 11.sp))
    }
}

@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) =
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = content)

/** Сегментный переключатель (как .seg). */
@Composable
fun Seg(options: List<String>, selected: Int, small: Boolean = false, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(K.s2).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.height(if (small) 30.dp else 34.dp).clip(RoundedCornerShape(9.dp)).background(if (on) K.ink else Color.Transparent)
                    .tap { onSelect(i) }.padding(horizontal = if (small) 10.dp else 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text(o, style = T.label.copy(color = if (on) K.bg else K.ink2, fontSize = if (small) 12.5.sp else 13.sp), maxLines = 1) }
        }
    }
}

/** Переключатель (как .tg). */
@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (on) 22.dp else 2.dp, label = "tg")
    val bg by animateColorAsState(if (on) K.acc else K.s3, label = "tgc")
    Box(Modifier.width(50.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(bg).tap { onChange(!on) }) {
        Box(Modifier.offset(x = x, y = 3.dp).size(24.dp).clip(CircleShape).background(if (on) Color.White else K.ink3))
    }
}

/**
 * Ползунок KAPSULA: толстая скруглённая дорожка, заполненная часть — стальной синий,
 * круглый бегунок слоновой кости. Тянуть и нажимать можно в любом месте дорожки.
 */
@Composable
fun Range(value: Int, min: Int, max: Int, unit: String = "%", fill: Boolean = false, onChange: (Int) -> Unit) {
    Row(if (fill) Modifier.fillMaxWidth() else Modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KSlider(value, min, max, if (fill) Modifier.weight(1f) else Modifier.width(200.dp), onChange)
        Text("$value$unit", style = T.num, modifier = Modifier.width(46.dp))
    }
}

@Composable
fun KSlider(value: Int, min: Int, max: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    val frac = ((value - min).toFloat() / (max - min).coerceAtLeast(1)).coerceIn(0f, 1f)
    val acc = K.acc; val track = K.s3; val knob = Color(0xFFF2F1EC); val shadow = Color.Black.copy(alpha = .35f)
    var dragging by remember { mutableStateOf(false) }
    val r by animateDpAsState(if (dragging) 13.dp else 11.dp, label = "knob")
    fun at(x: Float, w: Float, pad: Float) = (min + ((x - pad) / (w - 2 * pad)).coerceIn(0f, 1f) * (max - min)).roundToInt()
    androidx.compose.foundation.Canvas(
        modifier.height(32.dp)
            .pointerInput(min, max) {
                val pad = 13.dp.toPx()
                detectTapGestures { onChange(at(it.x, size.width.toFloat(), pad)) }
            }
            .pointerInput(min, max) {
                val pad = 13.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true; onChange(at(it.x, size.width.toFloat(), pad)) },
                    onDragEnd = { dragging = false }, onDragCancel = { dragging = false },
                ) { ch, _ -> ch.consume(); onChange(at(ch.position.x, size.width.toFloat(), pad)) }
            },
    ) {
        val pad = 13.dp.toPx(); val h = 6.dp.toPx(); val cy = size.height / 2
        val x0 = pad; val x1 = size.width - pad; val x = x0 + (x1 - x0) * frac
        drawRoundRect(track, androidx.compose.ui.geometry.Offset(x0 - h / 2, cy - h / 2), androidx.compose.ui.geometry.Size(x1 - x0 + h, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawRoundRect(acc, androidx.compose.ui.geometry.Offset(x0 - h / 2, cy - h / 2), androidx.compose.ui.geometry.Size(x - x0 + h, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
        drawCircle(shadow, r.toPx() + 1.5f, androidx.compose.ui.geometry.Offset(x, cy + 1.5f))
        drawCircle(knob, r.toPx(), androidx.compose.ui.geometry.Offset(x, cy))
        drawCircle(acc, r.toPx() * .32f, androidx.compose.ui.geometry.Offset(x, cy))
    }
}

/** Строка настройки: название + пояснение слева, управление справа (как .opt). */
@Composable
fun Opt(title: String, desc: String? = null, badge: (@Composable () -> Unit)? = null, control: @Composable () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title, style = T.label.copy(fontSize = 14.sp))
                    badge?.invoke()
                }
                if (desc != null) Text(desc, style = T.small, modifier = Modifier.padding(top = 2.dp))
            }
            control()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(K.line))
    }
}

@Composable
fun Sub(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), style = T.caps, modifier = modifier.padding(top = 16.dp, bottom = 6.dp))

/** Пилюля (как .pill); dot — цвет консоли. */
@Composable
fun Pill(text: String, dot: Color? = null, icon: Int? = null, color: Color = K.ink2, bg: Color = K.s2) {
    Row(
        Modifier.height(26.dp).clip(RoundedCornerShape(13.dp)).background(bg).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        if (icon != null) SolarIcon(icon, 14.dp, color)
        Text(text, style = T.small.copy(color = color, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
fun ConsolePill(text: String, color: Color) = Pill(text, dot = color)

/** Карточка-пояснение с иллюстрацией (как .state). */
@Composable
fun StateCard(img: String?, title: String?, text: String, tint: Color? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (tint != null) tint.copy(alpha = .09f) else K.s1)
            .border(1.dp, if (tint != null) tint.copy(alpha = .3f) else K.line, RoundedCornerShape(18.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (img != null) AssetImage(img, Modifier.size(64.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, style = T.label.copy(fontSize = 14.sp))
            Text(text, style = T.body.copy(fontSize = 13.sp, lineHeight = 19.sp))
            if (action != null) Box(Modifier.padding(top = 10.dp)) { action() }
        }
    }
}

/** Тонкая полоса прогресса (как .pb). */
@Composable
fun Bar(fraction: Float, color: Color = K.acc, height: Dp = 4.dp, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(K.s3)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(height / 2)).background(color))
    }
}

/** Плитка действия меню паузы (как .qb). */
@Composable
fun ActionTile(icon: Int, label: String, main: Boolean = false, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (main) Color(0xFFF2F1EC) else Color(0xFF131419)
    val fg = when { main -> Color(0xFF0B0C0F); danger -> Color(0xFFD98A8A); else -> Color(0xFFF2F1EC) }
    Column(
        Modifier.width(112.dp).clip(RoundedCornerShape(16.dp)).background(bg)
            .border(BorderStroke(1.dp, if (main) Color.Transparent else Color(0x14FFFFFF)), RoundedCornerShape(16.dp))
            .press(enabled, onClick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SolarIcon(icon, 22.dp, if (enabled) fg else fg.copy(alpha = .35f))
        Text(label, style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (enabled) fg else fg.copy(alpha = .35f)))
    }
}

@Composable
fun Ellip(text: String, style: TextStyle, modifier: Modifier = Modifier, lines: Int = 1) =
    Text(text, style = style, maxLines = lines, overflow = TextOverflow.Ellipsis, modifier = modifier)

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

val ScreenPad = PaddingValues(horizontal = 16.dp)
