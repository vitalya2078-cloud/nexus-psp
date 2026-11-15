package com.nexus.psp.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Cos
import com.nexus.psp.data.Tokens
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.kit.Accent
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Avatar
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Look
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.Range
import com.nexus.psp.ui.kit.Seg
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * «Оформление» (из быстрой панели): тема и цвет, фон главного экрана, профиль (аватар, рамка, баннер, титул).
 * Предметы — бесплатные, за игру или за жетоны; фильтр «Все / Бесплатные / За игру / За жетоны / Мои».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LooksScreen(start: String) {
    var tab by rememberSaveable { mutableStateOf(start) }
    val tabs = listOf("theme" to "Тема и цвет", "wall" to "Фон", "profile" to "Профиль")
    Full {
        OvHead("Оформление", back = true, actions = { TokensChip() })
        Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Seg(tabs.map { it.second }, tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0)) { tab = tabs[it].first }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (tab) {
                "wall" -> WallTab()
                "profile" -> ProfileTab()
                else -> ThemeTab()
            }
        }
    }
}

/** Баланс жетонов: монетка и число. */
@Composable
fun TokensChip() {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(K.s2).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Coin(16.dp)
        Text("${Tokens.balance}", style = T.num.copy(fontSize = 14.sp))
        Text(Tokens.word(Tokens.balance), style = T.small.copy(fontSize = 12.sp))
        // событие «×N жетонов» — янтарная бирка
        com.nexus.psp.data.Cloud.event?.let { (m, _, _) ->
            Text("×" + (if (m % 1.0 == 0.0) m.toInt().toString() else m.toString()), style = T.num.copy(fontSize = 12.sp, color = Color(0xFF0E0F13)),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFD9B36C)).padding(horizontal = 6.dp, vertical = 1.dp))
        }
    }
}

/**
 * Короткий ID игрока (касание — скопировать). У создателя (админа) — золотой, с мягким свечением.
 */
@Composable
fun PlayerId(onDark: Boolean = true, modifier: Modifier = Modifier) {
    val id = com.nexus.psp.data.Cloud.shortId
    if (id.isEmpty()) return
    val ctx = LocalContext.current
    val gold = com.nexus.psp.data.Cloud.isAdmin
    val g1 = Color(0xFFF6D98A); val g2 = Color(0xFFD9A441)
    Row(
        modifier.clip(RoundedCornerShape(10.dp))
            .background(if (gold) androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(g2.copy(alpha = .28f), g1.copy(alpha = .18f))) else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = if (onDark) .1f else .0f)))
            .border(1.dp, if (gold) g1.copy(alpha = .85f) else Color.White.copy(alpha = if (onDark) .14f else .0f), RoundedCornerShape(10.dp))
            .press {
                ctx.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("ID KAPSULA", id))
                AppState.say("ID $id скопирован")
            }.padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("ID", style = T.small.copy(fontSize = 10.5.sp, color = if (gold) g1 else (if (onDark) Color(0xFFF2F1EC).copy(alpha = .6f) else K.ink3)))
        Text(id, style = T.num.copy(fontSize = 12.5.sp, color = if (gold) g1 else (if (onDark) Color(0xFFF2F1EC) else K.ink),
            shadow = if (gold) androidx.compose.ui.graphics.Shadow(g2.copy(alpha = .8f), blurRadius = 10f) else null))
    }
}

/** Монетка-жетон (значок из набора кнопок, янтарным цветом). */
@Composable
fun Coin(size: Dp) {
    val ctx = LocalContext.current
    val id = remember { ctx.resources.getIdentifier("ctl_coin", "drawable", ctx.packageName) }
    if (id != 0) SolarIcon(id, size, Color(0xFFD9B36C))
}

@Composable
private fun Caps(t: String, top: Dp = 10.dp) = Text(t.uppercase(), style = T.caps, modifier = Modifier.padding(top = top, bottom = 2.dp))

// ---------- тема и цвет ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeTab() {
    val th = listOf("dark" to "Тёмная", "oled" to "OLED", "light" to "Светлая")
    Caps("Тема", 2.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        th.forEach { (id, name) ->
            val on = S.theme == id
            val (bg, fg) = when (id) { "oled" -> Color(0xFF000000) to Color(0xFFF2F1EC); "light" -> Color(0xFFE7E5E0) to Color(0xFF1C1D20); else -> Color(0xFF0B0C0F) to Color(0xFFF2F1EC) }
            Column(
                Modifier.width(132.dp).clip(RoundedCornerShape(16.dp)).background(bg).border(if (on) 2.5.dp else 1.dp, if (on) K.acc else K.line2, RoundedCornerShape(16.dp))
                    .press { S.theme = id; AppState.applyLook() }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(fg.copy(alpha = .8f)))
                Box(Modifier.width(60.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(fg.copy(alpha = .35f)))
                Text(name, style = T.label.copy(color = fg, fontSize = 13.sp))
            }
        }
    }
    Caps("Цвет акцента")
    Text("Цвет кнопок, выделения и ползунков. KAPSULA сама подправит яркость, чтобы всё читалось.", style = T.small)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        AccentDot("KAPSULA", Color(0xFF7FA7D9), S.accent.isEmpty()) { S.accent = ""; AppState.applyLook() }
        Accent.PRESETS.forEach { (name, c) ->
            val hex = "#%06X".format(c.toArgb() and 0xFFFFFF)
            AccentDot(name, c, S.accent.equals(hex, true)) { S.accent = hex; AppState.applyLook() }
        }
        val wallOk = S.wall == "photo" || S.wall.startsWith("wp_")
        AccentDot("Из фона", AppState.wallColor ?: Color(0xFF8E96C4), S.accent == "wall", hint = !wallOk) {
            if (!wallOk) AppState.say("Сначала выбери картинку во вкладке «Фон»") else { S.accent = "wall"; AppState.applyLook() }
        }
        if (android.os.Build.VERSION.SDK_INT >= 31) AccentDot("Как в системе", Accent.system(LocalContext.current) ?: K.acc, S.accent == "system") { S.accent = "system"; AppState.applyLook() }
    }
    Caps("Удобство")
    Opt("Крупный текст", null) { Toggle(S.big) { S.big = it; AppState.applyLook() } }
    Opt("Высокий контраст", null) { Toggle(S.contrast) { S.contrast = it; AppState.applyLook() } }
}

@Composable
private fun AccentDot(name: String, c: Color, on: Boolean, hint: Boolean = false, onClick: () -> Unit) {
    Column(Modifier.width(76.dp).press(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.copy(alpha = if (hint) .4f else 1f)).border(if (on) 3.dp else 0.dp, if (on) K.ink else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            if (on) SolarIcon(R.drawable.ph_check, 18.dp, Color(0xFF0E0F13))
        }
        Text(name, style = T.small.copy(fontSize = 11.sp, color = if (on) K.ink else K.ink3), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------- фон главного экрана ----------

@Composable
private fun WallTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var f by rememberSaveable { mutableStateOf(Cos.Filter.ALL) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { saveWall(ctx, uri) }
            if (ok) { S.wall = "photo"; AppState.wallChanged(); AppState.say("Фон поставлен") } else AppState.say("Фото не открылось")
        }
    }
    Text("Фон главного экрана — за обложками игр. Затемнение держит текст читаемым.", style = T.small)
    Filters(f) { f = it }
    Grid(Cos.of(Cos.Kind.WP).filter { Cos.pass(it, f) }, 190.dp, selected = { it.id == S.wall }, preview = { it, m -> WallPreview(it, m) }) { item ->
        if (item.id == "photo") pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else { S.wall = item.id; AppState.wallChanged() }
    }
    if (S.wall != "none") Opt("Затемнение", "Сильнее — спокойнее фон и лучше читается текст") { Range(S.wallDim, 10, 85) { S.wallDim = it } }
}

@Composable
private fun WallPreview(it: Cos.Item, m: Modifier) {
    Box(m.aspectRatio(21f / 9f).clip(RoundedCornerShape(12.dp)).background(K.s2), contentAlignment = Alignment.Center) {
        when {
            it.id.startsWith("wp_") -> AssetImage(it.asset, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            it.id == "photo" -> {
                SolarIcon(R.drawable.sl_folder, 26.dp, K.ink2)
            }
            it.id == "game" -> SolarIcon(R.drawable.sl_pad, 26.dp, K.ink2)
            else -> SolarIcon(R.drawable.ph_x, 22.dp, K.ink3)
        }
    }
}

/** Своё фото для фона: уменьшаем до 1920 по ширине и сохраняем рядом с данными приложения. */
private fun saveWall(ctx: Context, uri: android.net.Uri): Boolean = try {
    val src = ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: throw IllegalStateException()
    val k = minOf(1f, 1920f / src.width)
    val b = if (k < 1f) android.graphics.Bitmap.createScaledBitmap(src, (src.width * k).toInt(), (src.height * k).toInt(), true) else src
    File(ctx.filesDir, "wall.jpg").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
    true
} catch (e: Exception) { false }

// ---------- профиль ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var nick by remember { mutableStateOf(S.nick) }
    var motto by remember { mutableStateOf(S.motto) }
    var part by rememberSaveable { mutableStateOf(Cos.Kind.AV) }
    var f by rememberSaveable { mutableStateOf(Cos.Filter.ALL) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { saveAvatar(ctx, uri) }
            if (ok) S.avMode = "photo" else AppState.say("Фото не открылось")
        }
    }
    // превью — тот же широкий баннер, что на «Моё»: видно всю картинку и как профиль выглядит на самом деле
    Banner(Stats(), onEdit = {}, onShare = null)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth > 600.dp
        val nickF = @Composable { m: Modifier -> SearchField(nick, "Имя игрока", m, icon = R.drawable.sl_user) { nick = it.take(20); S.nick = nick.trim() } }
        val mottoF = @Composable { m: Modifier -> SearchField(motto, "Девиз, например «Пройду всё на 100%»", m, icon = R.drawable.sl_text) { motto = it.take(48); S.motto = motto.trim() } }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caps("Имя и девиз")
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { nickF(Modifier.weight(1f)); mottoF(Modifier.weight(1.6f)) }
            else { nickF(Modifier.fillMaxWidth()); mottoF(Modifier.fillMaxWidth()) }
            Caps("Любимая игра")
            val games = AppState.myList().take(12)
            if (games.isEmpty()) Text("Добавь игры — и выбери ту, что будет на карточке.", style = T.small)
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.nexus.psp.ui.kit.Chip("Больше всего часов", S.favGame.isEmpty()) { S.favGame = "" }
                games.forEach { g -> com.nexus.psp.ui.kit.Chip(g.t, S.favGame == g.id) { S.favGame = g.id } }
            }
        }
    }
    Caps("Оформление профиля")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Cos.Kind.AV, Cos.Kind.FR, Cos.Kind.BN, Cos.Kind.TT).forEach { k -> com.nexus.psp.ui.kit.Chip(k.title, part == k) { part = k } }
    }
    Filters(f) { f = it }
    when (part) {
        Cos.Kind.AV -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Своё фото", R.drawable.sl_folder, small = true) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                KButton("Буква", small = true) { S.avMode = "letter" }
            }
            Grid(Cos.of(Cos.Kind.AV).filter { Cos.pass(it, f) }, 96.dp, selected = { S.avMode == "set" && S.avItem == it.id }, preview = { it, m ->
                Box(m.aspectRatio(1f).clip(CircleShape).background(Color(0xFF1D1F29))) { AssetImage(it.asset, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }) { S.avItem = it.id; S.avMode = "set" }
        }
        Cos.Kind.FR -> Grid(Cos.of(Cos.Kind.FR).filter { Cos.pass(it, f) }, 96.dp, selected = { S.frame == it.id }, preview = { it, m ->
            Box(m.aspectRatio(1f), contentAlignment = Alignment.Center) {
                Avatar(64.dp)
                if (it.id == "fr_creator") PixelCrownFrame(88.dp) else if (it.id.isNotEmpty()) AssetImage(it.asset, Modifier.fillMaxSize())
            }
        }) { S.frame = it.id }
        Cos.Kind.BN -> Grid(Cos.of(Cos.Kind.BN).filter { Cos.pass(it, f) }, 240.dp, selected = { Look.bannerId() == it.id }, preview = { it, m ->
            com.nexus.psp.ui.kit.BannerBg(it.id, Stats().fav, m.aspectRatio(3.6f).clip(RoundedCornerShape(12.dp)))
        }) { item ->
            S.banner = item.id
            if (item.id.startsWith("g")) S.cardBg = item.id.removePrefix("g").toIntOrNull() ?: 0
            if (item.id == "game") S.cardBg = -1
        }
        else -> Grid(Cos.of(Cos.Kind.TT).filter { Cos.pass(it, f) }, 200.dp, selected = { S.titleSel == it.id }, preview = { it, m ->
            Box(m.height(44.dp).clip(RoundedCornerShape(12.dp)).background(K.s2), contentAlignment = Alignment.Center) {
                Text(if (it.id.isEmpty()) Look.title(Stats().level) else it.name, style = T.label.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }) { S.titleSel = it.id }
    }
}

// ---------- общее: фильтр, сетка предметов, покупка ----------

@Composable
private fun Filters(f: Cos.Filter, onPick: (Cos.Filter) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Cos.Filter.entries.forEach { x -> com.nexus.psp.ui.kit.Chip(x.title, f == x) { onPick(x) } }
    }
}

/**
 * Сетка предметов: картинка, название и состояние (выбрано / есть / цена / условие с прогрессом).
 * Касание: доступно — выбрать; за жетоны — спросить и купить; за игру — подсказать, что осталось.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Grid(items: List<Cos.Item>, cell: Dp, selected: (Cos.Item) -> Boolean, preview: @Composable (Cos.Item, Modifier) -> Unit, onUse: (Cos.Item) -> Unit) {
    val scope = rememberCoroutineScope()
    if (items.isEmpty()) { Text("Здесь пока пусто", style = T.small, modifier = Modifier.padding(vertical = 10.dp)); return }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = maxOf(2, ((maxWidth + 10.dp) / (cell + 10.dp)).toInt())
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { item ->
                        val has = Cos.has(item)
                        val on = has && selected(item)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) K.acc.copy(alpha = .14f) else K.s1)
                                .border(if (on) 2.dp else 1.dp, if (on) K.acc else K.line, RoundedCornerShape(16.dp))
                                .press {
                                    when {
                                        has -> onUse(item)
                                        item.price > 0 -> scope.launch { if (buy(item)) onUse(item) }
                                        item.need != null -> {
                                            val have = Cos.progress(item.need.what)
                                            AppState.say("Откроется: ${item.need.text}. Сейчас ${needNow(item.need, have)}")
                                        }
                                    }
                                }.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(Modifier.fillMaxWidth().graphicsLayer { alpha = if (has) 1f else .45f }) { preview(item, Modifier.fillMaxWidth()) }
                                if (!has && item.need != null) SolarIcon(R.drawable.sl_keyhole, 22.dp, K.ink)
                            }
                            Text(item.name, style = T.small.copy(fontSize = 11.5.sp, color = if (has) K.ink else K.ink3, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            when {
                                on -> Text("Выбрано", style = T.small.copy(fontSize = 10.5.sp, color = K.acc))
                                has -> Text(if (item.free) "Бесплатно" else "Твоё", style = T.small.copy(fontSize = 10.5.sp, color = K.ink3))
                                item.price > 0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Coin(13.dp); Text("${item.price}", style = T.num.copy(fontSize = 12.sp, color = if (Tokens.balance >= item.price) K.ink else K.ink3))
                                }
                                item.need != null -> {
                                    val have = Cos.progress(item.need.what)
                                    Text(item.need.text, style = T.small.copy(fontSize = 10.5.sp, color = K.ink3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Bar((have.toFloat() / item.need.n).coerceIn(0f, 1f), height = 3.dp)
                                }
                            }
                        }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Сколько набрано по условию — словами. */
private fun needNow(n: Cos.Need, have: Int): String = when {
    n.what == "play" || n.what.startsWith("con:") -> "${have / 60} ч ${have % 60} мин"
    n.what == "level" -> "уровень $have"
    else -> "$have из ${n.n}"
}

/** Покупка за жетоны: спросить, списать. true — куплено. */
private suspend fun buy(item: Cos.Item): Boolean {
    if (Tokens.balance < item.price) {
        (item.price - Tokens.balance).let { n -> AppState.say("Не хватает $n ${Tokens.word(n)}. Они копятся за игру и ежедневный заход") }
        return false
    }
    val a = AppState.askUser("«${item.name}» за ${item.price} ${Tokens.word(item.price)}?", "После покупки останется ${Tokens.balance - item.price}.", "Купить", "Не сейчас")
    if (a != 0) return false
    val ok = Tokens.buy(item)
    AppState.say(if (ok) "«${item.name}» — твоё" else "Не получилось купить")
    return ok
}
