package com.nexus.psp.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.rotate
import com.nexus.psp.ui.kit.press
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Cloud
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.BannerBg
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Look
import com.nexus.psp.ui.kit.Mascot
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Особое оформление создателя KAPSULA (только у админа; видят все, кто открывает его профиль):
 * рамка «Пиксельная корона», имя переливающимся цветом, пиксельная корона у имени, статус, золотые искры на баннере.
 * Всё рисуется кодом; при «Уменьшить движение» — без анимации.
 */
object Creator {
    /** Цвета имени: стиль → (светлый, тёмный). */
    val NAME = mapOf(
        "gold" to (Color(0xFFF6D98A) to Color(0xFFD9A441)), "rose" to (Color(0xFFF7C6C7) to Color(0xFFD98A8A)),
        "emerald" to (Color(0xFFA8E6C1) to Color(0xFF4FA36F)), "neon" to (Color(0xFF8FF0FF) to Color(0xFFB98CFF)),
        "ice" to (Color(0xFFE6F4FF) to Color(0xFF7FA7D9)),
    )
    val GOLD = Color(0xFFF6D98A); val GOLD2 = Color(0xFFD9A441); val GOLD3 = Color(0xFFA9772A)
}

/** Время для анимаций создателя (0…1 по кругу) — общее; при «Уменьшить движение» стоит на месте. */
@Composable
private fun loop(ms: Int): Float {
    if (S.calm) return .3f
    val t = rememberInfiniteTransition(label = "creator")
    val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Restart), label = "t")
    return v
}

/** Имя: у создателя — переливающийся градиент (блик проходит по буквам), у остальных — обычный цвет. */
@Composable
fun PlayerName(nick: String, creator: Boolean, style: TextStyle, nameStyle: String = "gold", modifier: Modifier = Modifier) {
    if (!creator) { Text(nick, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier); return }
    val (a, b) = Creator.NAME[nameStyle] ?: Creator.NAME.getValue("gold")
    val t = loop(3600)
    val shift = t * 900f
    val brush = Brush.linearGradient(listOf(b, a, Color.White.copy(alpha = .95f), a, b), start = Offset(shift - 600f, 0f), end = Offset(shift, 80f))
    Text(nick, style = style.copy(brush = brush, shadow = androidx.compose.ui.graphics.Shadow(b.copy(alpha = .55f), blurRadius = 14f)), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** Пиксельная корона (7×5 «пикселей»). */
@Composable
fun PixelCrown(size: Dp, modifier: Modifier = Modifier) {
    val rows = listOf("X..X..X", "XX.X.XX", "XXXXXXX", "XGXGXGX", "XXXXXXX")
    Canvas(modifier.size(size, size * 5f / 7f)) {
        val px = this.size.width / 7f
        rows.forEachIndexed { y, row -> row.forEachIndexed { x, c ->
            if (c != '.') drawRect(if (c == 'G') Color(0xFFE05A5A) else if (y < 2) Creator.GOLD else Creator.GOLD2, Offset(x * px, y * px), Size(px * .96f, px * .96f))
        } }
    }
}

/**
 * Рамка «Пиксельная корона»: кольцо из золотых квадратиков вокруг аватара, сверху — пиксельная корона,
 * отдельные пиксели изредка вспыхивают.
 */
@Composable
fun PixelCrownFrame(size: Dp, modifier: Modifier = Modifier) {
    val t = loop(2600)
    Box(modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val r = this.size.minDimension * .43f
            val px = this.size.minDimension * .052f
            val n = ((2 * PI * r) / (px * 1.18f)).toInt()
            for (i in 0 until n) {
                val ang = 2 * PI * i / n - PI / 2
                // наверху — место для короны
                if (kotlin.math.abs(ang + PI / 2) < .32) continue
                val x = c.x + (r * cos(ang)).toFloat(); val y = c.y + (r * sin(ang)).toFloat()
                // мерцание: у каждого пикселя своя фаза, вспыхивают по очереди
                val ph = ((i * 0.618f) % 1f)
                val glow = (1f - kotlin.math.abs(((t - ph + 1f) % 1f) - .5f) * 8f).coerceIn(0f, 1f)
                val col = if (i % 2 == 0) Creator.GOLD2 else Creator.GOLD3
                drawRect(lerp(col, Color.White, glow * .8f), Offset(x - px / 2, y - px / 2), Size(px, px))
            }
        }
        PixelCrown(size * .34f, Modifier.align(Alignment.TopCenter).padding(top = 0.dp))
    }
}

private fun lerp(a: Color, b: Color, f: Float) = Color(a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f)

/** Редкие золотые искры поверх баннера создателя. */
@Composable
fun Sparkles(modifier: Modifier = Modifier) {
    val t = loop(4200)
    Canvas(modifier) {
        val pts = listOf(.06f to .22f, .14f to .7f, .3f to .14f, .44f to .82f, .58f to .2f, .66f to .66f, .78f to .3f, .88f to .78f, .95f to .16f, .5f to .48f)
        pts.forEachIndexed { i, (fx, fy) ->
            val a = (1f - kotlin.math.abs(((t + i * .37f) % 1f) - .5f) * 3f).coerceIn(0f, 1f)
            if (a <= 0f) return@forEachIndexed
            val p = Offset(size.width * fx, size.height * fy); val s = 5.dp.toPx() * (.6f + a * .6f)
            val path = Path().apply { moveTo(p.x, p.y - s); lineTo(p.x + s * .3f, p.y - s * .3f); lineTo(p.x + s, p.y); lineTo(p.x + s * .3f, p.y + s * .3f)
                lineTo(p.x, p.y + s); lineTo(p.x - s * .3f, p.y + s * .3f); lineTo(p.x - s, p.y); lineTo(p.x - s * .3f, p.y - s * .3f); close() }
            drawPath(path, Creator.GOLD.copy(alpha = a * .85f))
        }
    }
}

// ---------- чужой профиль ----------

/** Окно «Найти игрока»: ввести ID — откроется его карточка. */
@Composable
fun FindPlayerDialog(onClose: () -> Unit) {
    var id by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onClose) {
        Column(Modifier.widthIn(max = 440.dp).clip(RoundedCornerShape(22.dp)).background(K.bg).border(1.dp, K.line, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Найти игрока", style = T.h3)
            Text("Попроси у друга ID — он на его карточке в «Моё».", style = T.small)
            SearchField(id, "ID, например 7Q4M2K", Modifier.fillMaxWidth(), icon = R.drawable.sl_user) { id = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(10) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Открыть", kind = BtnKind.Main, enabled = id.length >= 6) { onClose(); Nav.open(Ov.Player(id)) }
                KButton("Отмена") { onClose() }
            }
        }
    }
}

/** Экран профиля другого игрока по ID: баннер, аватар, рамка, имя, титул, цифры, витрина. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(id: String) {
    val res by produceState<Pair<JSONObject?, String?>?>(null, id) { value = withContext(Dispatchers.IO) { Cloud.publicProfile(id) } }
    Full {
        OvHead("Игрок", back = true)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val r = res
            when {
                r == null -> Text("Загружаю…", style = T.body)
                r.first == null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Mascot("search", 90.dp); Text(r.second ?: "Не нашли", style = T.body)
                }
                r.first!!.optBoolean("banned") -> Text("Аккаунт ${r.first!!.optString("short_id")} заблокирован", style = T.body)
                else -> PublicCard(r.first!!)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PublicCard(o: JSONObject) {
    val p = o.optJSONObject("profile") ?: JSONObject()
    val creator = o.optBoolean("is_admin")
    val cr = o.optJSONObject("creator") ?: JSONObject()
    val nick = p.optString("nick").ifBlank { "Игрок" }
    val level = o.optInt("level", 1)
    val banner = p.optString("banner").let { b -> if (b.startsWith("bn_") || b.startsWith("g")) b else "g0" }.let { if (it == "bn_creator" && !creator) "g0" else it }
    if (creator) {
        val title = p.optString("titleSel").takeIf { it.isNotEmpty() }?.let { com.nexus.psp.data.Cos.find(com.nexus.psp.data.Cos.Kind.TT, it)?.name } ?: Look.title(level)
        val bare = JSONObject(p.toString()).apply { remove("frame") }
        CreatorBanner(CreatorCard(nick, p.optString("motto"), cr.optString("status"), cr.optString("name", "gold"), o.optString("short_id"),
            level, null, title, banner, { s -> AvatarOf(bare, nick, s, true) }, o.optJSONObject("playing"),
            o.optInt("cheers"), own = o.optString("short_id") == Cloud.shortId))
    } else Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp))) {
        BannerBg(banner, null, Modifier.fillMaxSize())
        if (creator) Sparkles(Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            AvatarOf(p, nick, 104.dp, creator)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerName(nick, creator, T.h1.copy(color = Color(0xFFF2F1EC)), cr.optString("name", "gold"), Modifier.weight(1f, fill = false))
                    if (creator) PixelCrown(22.dp)
                    IdChip(o.optString("short_id"), creator)
                }
                cr.optString("status").takeIf { creator && it.isNotEmpty() }?.let { Text(it, style = T.small.copy(color = Creator.GOLD, fontSize = 13.sp)) }
                p.optString("motto").takeIf { it.isNotEmpty() }?.let { Text(it, style = T.small.copy(color = Color(0xFFF2F1EC).copy(alpha = .75f), fontSize = 13.sp), maxLines = 1) }
                val title = p.optString("titleSel").takeIf { it.isNotEmpty() }?.let { com.nexus.psp.data.Cos.find(com.nexus.psp.data.Cos.Kind.TT, it)?.name } ?: Look.title(level)
                Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Ур. $level · $title", style = T.small.copy(color = Color(0xFFF2F1EC), fontSize = 12.sp))
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            "${o.optInt("play") / 60} ч" to "в играх", "${o.optInt("games")}" to "игр", "${o.optInt("consoles")}" to "консолей",
            o.optString("since").take(7).split('-').let { if (it.size == 2) "${it[1]}.${it[0]}" else "—" } to "в KAPSULA с",
        ).forEach { (n, l) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(K.s1).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(n, style = T.label.copy(fontSize = 15.sp)); Text(l, style = T.small.copy(fontSize = 11.sp))
            }
        }
    }
    val show = p.optJSONArray("show")
    if (show != null && show.length() > 0) {
        Text("ВИТРИНА", style = T.caps, modifier = Modifier.padding(top = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until show.length()) show.getJSONObject(i).let { g ->
                val pl = com.nexus.psp.data.PL[g.optString("p")]
                Row(Modifier.clip(RoundedCornerShape(14.dp)).background(K.s1).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(pl?.color ?: K.acc))
                    Text(g.optString("t"), style = T.label.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    pl?.let { Text(it.short, style = T.small.copy(fontSize = 11.sp)) }
                }
            }
        }
    }
    val badges = p.optJSONArray("badges")
    if (badges != null && badges.length() > 0) {
        Text("НАГРАДЫ", style = T.caps, modifier = Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until badges.length()) AssetImage(badges.getString(i) + ".png", Modifier.size(44.dp))
        }
    }
}

/** Аватар чужого игрока: из набора или буква (своё фото других не загружается). Рамка — его выбор. */
@Composable
private fun AvatarOf(p: JSONObject, nick: String, size: Dp, creator: Boolean) {
    val frame = p.optString("frame").let { if (it == "fr_creator" && !creator) "" else it }
    val inner = if (frame.isNotEmpty()) size * .8f else size
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        val m = Modifier.size(inner).clip(CircleShape)
        if (p.optString("avMode") == "set" && p.optString("avItem").startsWith("av_"))
            Box(m.background(Color(0xFF1D1F29))) { AssetImage("look/${p.optString("avItem")}.webp", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
        else Box(m.background(Look.avColor(p.optInt("avColor", 7))), contentAlignment = Alignment.Center) {
            Text(nick.take(1).uppercase(), style = T.label.copy(color = Color(0xFF0E0F13), fontSize = (inner.value * .42f).sp))
        }
        when {
            frame == "fr_creator" -> PixelCrownFrame(size)
            frame.startsWith("fr_") -> AssetImage("look/$frame.webp", Modifier.size(size))
            frame.isEmpty() -> Box(Modifier.size(size).border(2.dp, Color(0xFFF2F1EC).copy(alpha = .9f), CircleShape))
        }
    }
}

/** ID на чужой карточке (у создателя — золотой). */
@Composable
private fun IdChip(id: String, gold: Boolean) {
    Text("ID $id", style = T.num.copy(fontSize = 12.sp, color = if (gold) Creator.GOLD else Color(0xFFF2F1EC).copy(alpha = .8f)),
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (gold) Creator.GOLD2.copy(alpha = .22f) else Color.White.copy(alpha = .1f))
            .border(1.dp, if (gold) Creator.GOLD.copy(alpha = .8f) else Color.White.copy(alpha = .14f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
}

// ---------- баннер создателя ----------

/** Что показывает баннер создателя (свой — из телефона, чужой просмотр — с сервера). */
class CreatorCard(
    val nick: String, val motto: String, val status: String, val nameStyle: String, val shortId: String,
    val level: Int, val xp: Float?, val title: String, val banner: String,
    val avatar: @Composable (Dp) -> Unit, val playing: JSONObject?,
    /** Сколько раз создателя поприветствовали; own — свой баннер (кнопки нет, только счётчик). */
    val cheers: Int = 0, val own: Boolean = true,
)

private val BADGES = listOf("cr_b1" to "Создатель KAPSULA", "cr_b2" to "Разработчик", "cr_b3" to "Друг Сейвика")

/**
 * Баннер создателя: рамка-картинка с бегущим бликом и пульсом, светящийся ID с искрами, титул на плашке,
 * уровень на эмблеме, три особых значка и «Сейчас играет». Только у аккаунта создателя.
 */
@Composable
fun CreatorBanner(c: CreatorCard, modifier: Modifier = Modifier) {
    var burst by remember { mutableStateOf(0) }
    Box(modifier.fillMaxWidth().height(184.dp).clip(RoundedCornerShape(24.dp))) {
        BannerBg(c.banner, null, Modifier.fillMaxSize())
        Sparkles(Modifier.fillMaxSize())
        if (burst > 0) Salute(burst, Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().padding(start = 18.dp, end = 14.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CreatorAvatar(c.avatar, 136.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlayerName(c.nick, true, TextStyle(fontFamily = com.nexus.psp.ui.kit.Russo, fontSize = 30.sp), c.nameStyle, Modifier.weight(1f, fill = false))
                        LevelEmblem(c.level, 44.dp)
                    }
                    GlowId(c.shortId, Modifier.padding(start = 2.dp))
                }
                (c.status.ifEmpty { c.motto }).takeIf { it.isNotEmpty() }?.let { Text(it, style = T.small.copy(color = Creator.GOLD, fontSize = 12.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                c.xp?.let { GoldBar(it, Modifier.widthIn(max = 260.dp).fillMaxWidth()) }
                CheerButton(c) { burst++ }
            }
            SaveikMascot(Modifier.align(Alignment.Bottom))
            Column(Modifier.align(Alignment.Top), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BADGES.forEach { (img, name) ->
                        AssetImage("look/$img.webp", Modifier.size(46.dp).press { AppState.say(name) })
                    }
                }
                Playing(c.playing)
            }
        }
    }
}

/** Аватар в рамке создателя: блик бежит по золоту, свечение пульсирует, иногда отлетают искры. */
@Composable
fun CreatorAvatar(avatar: @Composable (Dp) -> Unit, size: Dp) {
    val t = loop(3400)
    val p = loop(2200)
    Box(Modifier.size(size)) {
        // свечение за рамкой
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width * .5f, this.size.height * .517f)
            val a = .22f + .16f * kotlin.math.sin(p * 2 * PI).toFloat()
            drawCircle(Brush.radialGradient(listOf(Creator.GOLD.copy(alpha = a), Color.Transparent), c, this.size.minDimension * .56f), this.size.minDimension * .56f, c)
        }
        // сам аватар — в круглом окне рамки (центр окна чуть ниже центра картинки из-за короны)
        Box(Modifier.align(Alignment.TopStart).padding(start = size * .147f, top = size * .167f).size(size * .70f).clip(CircleShape)) { avatar(size * .70f) }
        // рамка + бегущий блик только по золоту (SrcAtop)
        AssetImage("look/cr_frame.webp", Modifier.fillMaxSize()
            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                rotate(t * 360f) {
                    drawRect(Brush.sweepGradient(listOf(Color.Transparent, Color.Transparent, Color.White.copy(alpha = .75f), Color.Transparent, Color.Transparent),
                        Offset(this.size.width / 2, this.size.height / 2)), blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop)
                }
            })
        // искры с кольца наружу
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width * .5f, this.size.height * .517f); val r0 = this.size.minDimension * .4f
            for (i in 0 until 5) {
                val ph = ((t * 1.7f + i * .2f) % 1f)
                if (ph > .55f) continue
                val ang = (i * 1.3f + 0.6f) * 2.0
                val d = r0 + this.size.minDimension * .18f * ph / .55f
                val pt = Offset(c.x + (d * cos(ang)).toFloat(), c.y + (d * sin(ang)).toFloat())
                drawCircle(Creator.GOLD.copy(alpha = (1f - ph / .55f) * .9f), 2.2.dp.toPx(), pt)
            }
        }
    }
}

/** ID без плашки: буквы светятся, по ним переливается золото, иногда поднимаются искры. */
@Composable
fun GlowId(id: String, modifier: Modifier = Modifier) {
    val t = loop(4000)
    val shift = t * 700f
    val brush = Brush.linearGradient(listOf(Creator.GOLD2, Creator.GOLD, Color(0xFFFFF4D2), Creator.GOLD, Creator.GOLD2), start = Offset(shift - 500f, 0f), end = Offset(shift, 40f))
    Box(modifier) {
        Text(id, style = TextStyle(fontFamily = com.nexus.psp.ui.kit.Russo, fontSize = 12.sp, brush = brush, letterSpacing = 1.sp,
            shadow = androidx.compose.ui.graphics.Shadow(Creator.GOLD.copy(alpha = .8f), blurRadius = 10f)))
        Canvas(Modifier.matchParentSize()) {
            for (i in 0 until 4) {
                val ph = ((t * 2f + i * .27f) % 1f)
                if (ph > .6f) continue
                val x = this.size.width * ((i * .29f + .08f) % 1f)
                val y = this.size.height * (1f - ph / .6f * 1.3f)
                drawCircle(Creator.GOLD.copy(alpha = (1f - ph / .6f) * .85f), 1.2.dp.toPx(), Offset(x, y))
            }
        }
    }
}

/** Уровень на эмблеме: крупное золотое число. */
@Composable
fun LevelEmblem(level: Int, size: Dp) {
    Box(Modifier.size(size)) {
        AssetImage("look/cr_level.webp", Modifier.fillMaxSize())
        Text("$level", style = TextStyle(fontFamily = com.nexus.psp.ui.kit.Russo, fontSize = (size.value * (if (level >= 100) .26f else .34f)).sp,
            brush = Brush.verticalGradient(listOf(Color(0xFFFFF4D2), Creator.GOLD, Creator.GOLD2)),
            shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .6f), Offset(0f, 2f), 4f)),
            modifier = Modifier.align(Alignment.Center).padding(bottom = size * .08f))
    }
}

/** Полоса опыта: золото с бегущим бликом. */
@Composable
fun GoldBar(frac: Float, modifier: Modifier = Modifier) {
    val t = loop(2600)
    Canvas(modifier.height(5.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(this.size.height / 2)
        drawRoundRect(Color.White.copy(alpha = .14f), cornerRadius = r)
        val w = this.size.width * frac.coerceIn(0.02f, 1f)
        drawRoundRect(Brush.horizontalGradient(listOf(Creator.GOLD3, Creator.GOLD, Creator.GOLD2)), size = Size(w, this.size.height), cornerRadius = r)
        val x = w * t
        drawRoundRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .7f), Color.Transparent), x - 30f, x + 30f),
            topLeft = Offset((x - 30f).coerceAtLeast(0f), 0f), size = Size(60f.coerceAtMost(w), this.size.height), cornerRadius = r)
    }
}

/** «Сейчас играет» / «Последняя игра». */
@Composable
private fun Playing(p: JSONObject?) {
    val t = p?.optString("t").orEmpty()
    if (t.isEmpty()) return
    val live = p!!.optBoolean("live")
    val pulse = loop(1400)
    val pl = com.nexus.psp.data.PL[p.optString("p")]
    Column(Modifier.widthIn(max = 220.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .45f))
        .border(1.dp, (if (live) Color(0xFF8FC9A3) else Creator.GOLD).copy(alpha = .4f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (live) Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8FC9A3).copy(alpha = .5f + .5f * kotlin.math.abs(kotlin.math.sin(pulse * PI)).toFloat())))
            Text(if (live) "ИГРАЕТ СЕЙЧАС" else "ПОСЛЕДНЯЯ ИГРА", style = TextStyle(fontFamily = com.nexus.psp.ui.kit.Russo, fontSize = 9.5.sp, letterSpacing = .8.sp, color = if (live) Color(0xFF8FC9A3) else Creator.GOLD))
        }
        Text(t, style = TextStyle(fontFamily = com.nexus.psp.ui.kit.Russo, fontSize = 13.sp, color = Color(0xFFF2F1EC), textAlign = androidx.compose.ui.text.style.TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
        val since = try { java.time.OffsetDateTime.parse(p.optString(if (live) "since" else "at")).toInstant().toEpochMilli() } catch (e: Exception) { 0L }
        val mins = if (since > 0) ((System.currentTimeMillis() - since) / 60000).toInt().coerceAtLeast(0) else -1
        Text(listOfNotNull(pl?.short, when {
            mins < 0 -> null
            live -> if (mins < 1) "только начал" else if (mins < 60) "$mins мин" else "${mins / 60} ч ${mins % 60} мин"
            mins < 1 -> "только что"; mins < 60 -> "$mins мин назад"; mins < 1440 -> "${mins / 60} ч назад"; else -> "${mins / 1440} дн назад"
        }).joinToString(" · "), style = T.small.copy(fontSize = 11.sp, color = Color(0xFFF2F1EC).copy(alpha = .7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center))
    }
}


internal fun cheersWord(n: Int) = "$n " + when { n % 10 == 1 && n % 100 != 11 -> "приветствие"; n % 10 in 2..4 && n % 100 !in 12..14 -> "приветствия"; else -> "приветствий" }

/** «Поприветствовать создателя»: у гостя — кнопка (раз в сутки), у себя — только счётчик. */
@Composable
private fun CheerButton(c: CreatorCard, onCheer: () -> Unit) {
    var n by remember(c.cheers) { mutableStateOf(c.cheers) }
    var sent by remember { mutableStateOf(Cloud.cheeredToday) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val beat = loop(1600)
    val active = !c.own && !sent && !busy
    val txt = when {
        c.own -> cheersWord(n)
        sent -> "Привет отправлен · $n"
        else -> if (n > 0) "Поприветствовать · $n" else "Поприветствовать"
    }
    Row(Modifier.clip(RoundedCornerShape(50)).background(if (active) Creator.GOLD2.copy(alpha = .9f) else Color.Black.copy(alpha = .45f))
        .border(1.dp, Creator.GOLD.copy(alpha = .55f), RoundedCornerShape(50))
        .press(enabled = active) {
            busy = true
            scope.launch {
                val (cnt, already, err) = withContext(Dispatchers.IO) { Cloud.cheer(c.shortId) }
                busy = false
                if (err != null) { AppState.say(err); return@launch }
                n = cnt; sent = true
                if (already) AppState.say("Сегодня привет уже отправлен — заходи завтра") else onCheer()
            }
        }.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val sc = if (active) 1f + .12f * kotlin.math.abs(kotlin.math.sin(beat * PI)).toFloat() else 1f
        SolarIcon(R.drawable.sl_heart, 15.dp, if (active) Color(0xFF2A1A06) else Creator.GOLD, Modifier.graphicsLayer { scaleX = sc; scaleY = sc })
        Text(txt, style = TextStyle(fontFamily = com.nexus.psp.ui.kit.Russo, fontSize = 12.sp, color = if (active) Color(0xFF2A1A06) else Creator.GOLD))
    }
}

/** Салют золотых искр по всему баннеру — после приветствия. */
@Composable
private fun Salute(key: Int, modifier: Modifier) {
    val a = remember(key) { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(key) { a.snapTo(0f); a.animateTo(1f, tween(1600, easing = LinearEasing)) }
    val parts = remember(key) { val r = java.util.Random(key.toLong()); List(3) { Offset(.2f + .6f * r.nextFloat(), .25f + .4f * r.nextFloat()) } to List(54) { r.nextFloat() } }
    Canvas(modifier) {
        val t = a.value
        if (t >= 1f) return@Canvas
        parts.first.forEachIndexed { k, o ->
            val tt = ((t - k * .15f) / .7f).coerceIn(0f, 1f)
            if (tt <= 0f || tt >= 1f) return@forEachIndexed
            val c0 = Offset(size.width * o.x, size.height * o.y)
            for (i in 0 until 18) {
                val ang = i / 18.0 * 2 * PI + parts.second[k * 18 + i]
                val d = size.minDimension * .32f * (1 - (1 - tt) * (1 - tt)) * (.7f + .3f * parts.second[(k * 18 + i + 7) % 54])
                val pt = Offset(c0.x + (d * cos(ang)).toFloat(), c0.y + (d * sin(ang)).toFloat() + 30f * tt * tt)
                drawCircle((if (i % 3 == 0) Color(0xFFFFF4D2) else Creator.GOLD).copy(alpha = 1f - tt), (2.6f - 1.4f * tt).dp.toPx(), pt)
            }
        }
    }
}

private val SAVEIK = listOf(
    "Привет! Я Сейвик", "Сохраняйся почаще!", "Создатель опять что-то пилит…", "Тут живёт KAPSULA",
    "Нажми ещё — не жалко", "Скоро будет ещё круче", "Я храню твои сохранения", "Ура, гости!",
)

/** Живой Сейвик: тап — подпрыгивает, радуется и говорит фразу. */
@Composable
private fun SaveikMascot(modifier: Modifier = Modifier) {
    val jump = remember { androidx.compose.animation.core.Animatable(0f) }
    var say by remember { mutableStateOf<String?>(null) }
    var k by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val idle = loop(2600)
    Column(modifier.widthIn(max = 150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(28.dp), contentAlignment = Alignment.BottomCenter) {
            say?.let {
                Text(it, style = TextStyle(fontFamily = com.nexus.psp.ui.kit.Russo, fontSize = 10.5.sp, color = Color(0xFF2A1A06), textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    maxLines = 2, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Creator.GOLD).padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
        val bob = if (S.calm) 0f else 2.5f * sin(idle * 2 * PI).toFloat()
        Box(Modifier.graphicsLayer { translationY = (bob - jump.value * 26f) * density }.press {
            k++
            say = SAVEIK[(k - 1) % SAVEIK.size]
            scope.launch {
                jump.animateTo(1f, tween(180)); jump.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = .35f, stiffness = 500f))
                kotlinx.coroutines.delay(2200); if (say == SAVEIK[(k - 1) % SAVEIK.size]) say = null
            }
        }) { Mascot(if (say != null) "joy" else "wave", 66.dp) }
    }
}
