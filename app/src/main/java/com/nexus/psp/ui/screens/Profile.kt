package com.nexus.psp.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Game
import com.nexus.psp.data.hrs
import com.nexus.psp.data.size
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Avatar
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Look
import com.nexus.psp.ui.kit.NetImage
import com.nexus.psp.ui.kit.Seg
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.Sub
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Цифры профиля — один расчёт на экран. */
internal class Stats {
    val play = AppState.lib.values.sumOf { it.play }
    val saves = AppState.lib.values.sumOf { it.saves }
    val xp = play * 2 + saves * 30
    val level = Look.level(xp)
    val streak = AppState.streak()
    val games = AppState.lib.size
    val favs = AppState.favs.size
    /** Любимая игра: выбранная игроком или та, где больше всего часов. */
    val fav: Game? = AppState.byId[S.favGame] ?: AppState.lib.entries.filter { it.value.play > 0 }.maxByOrNull { it.value.play }?.let { AppState.byId[it.key] }
    val recent: List<Game> = AppState.lib.entries.filter { it.value.play > 0 }.sortedByDescending { it.value.t }.mapNotNull { AppState.byId[it.key] }.take(8)
}

/** Открыт ли редактор оформления (снаружи — для снимков экрана). */
object ProfileUi { val edit = androidx.compose.runtime.mutableStateOf(false) }

/** Белый текст на фоне карточки — фон всегда тёмный, при любой теме. */
private val OnCard = Color(0xFFF2F1EC)

/** Профиль игрока: карточка с оформлением, цифры, любимая игра, недавние, активность, награды. */
@Composable
fun ProfileScreen() {
    val ctx = LocalContext.current
    val st = Stats()
    var edit by ProfileUi.edit
    val scope = rememberCoroutineScope()
    Full {
        OvHead("Профиль")
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // ---- игрок ----
            Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlayerCard(st, Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        KButton("Оформить", icon = R.drawable.sl_sliders, small = true) { edit = true }
                        KButton("Поделиться", small = true) { scope.launch { shareProfile(ctx, st) } }
                    }
                }
                val tiles = listOf(
                    Triple(R.drawable.sl_clock, if (st.play > 0) hrs(st.play) else "—", "наиграно"),
                    Triple(R.drawable.sl_bolt, if (st.streak > 0) days(st.streak) else "—", "подряд"),
                    Triple(R.drawable.sl_grid, "${st.games}", "в библиотеке"),
                    Triple(R.drawable.sl_save, "${st.saves}", "сохранений"),
                    Triple(R.drawable.sl_heart, "${st.favs}", "в избранном"),
                    Triple(R.drawable.sl_trophy, if (S.raToken.isNotEmpty()) S.raScore else "—", "очков достижений"),
                )
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (ic, n, l) ->
                            Row(Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(14.dp)).background(K.s1).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SolarIcon(ic, 18.dp, K.acc)
                                Column { Text(n, style = T.h3.copy(fontSize = 15.sp), maxLines = 1); Text(l, style = T.small.copy(fontSize = 11.sp), maxLines = 1) }
                            }
                        }
                    }
                }
            }
            // ---- игры, активность, награды ----
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
                if (st.play == 0) StateCard("ach-first.png", "Сыграй первую игру", "Здесь появятся любимая игра, недавние игры и активность по дням.") {
                    KButton(if (AppState.byId.isEmpty()) "Добавить игру" else "Играть", if (AppState.byId.isEmpty()) R.drawable.ph_plus else R.drawable.sb_play, BtnKind.Main, small = true) {
                        val first = AppState.myList().firstOrNull()
                        if (first == null) Act.addFile() else Act.play(ctx, first.id)
                    }
                }
                st.fav?.let { g -> FavGame(g) }
                if (st.recent.size > 1) {
                    Sub("Недавно")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        st.recent.forEach { g ->
                            Column(Modifier.width(84.dp).press { Nav.open(Ov.Game(g.id)) }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.size(84.dp, 112.dp).clip(RoundedCornerShape(12.dp))) { Cover(g, 300, 400, modifier = Modifier.fillMaxSize()) }
                                Text(g.t, style = T.small.copy(fontSize = 11.sp, color = K.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Week()
                Sub("Награды KAPSULA")
                Badges(st)
                Sub("Достижения в играх")
                RaCard()
                if (AppState.favs.isNotEmpty()) {
                    Sub("Избранное")
                    AppState.favs.keys.mapNotNull { AppState.byId[it] }.forEach { g -> Item(g.id, g.t, g.plat.short + " · " + size(g.size), onClick = { Nav.open(Ov.Game(g.id)) }) }
                }
            }
        }
    }
    if (edit) ProfileEditor { edit = false }
}

private fun days(n: Int) = "$n " + when { n % 10 == 1 && n % 100 != 11 -> "день"; n % 10 in 2..4 && n % 100 !in 12..14 -> "дня"; else -> "дней" }

/** Карточка игрока: фон (оформление или обложка любимой игры), аватар, ник, девиз, уровень. */
@Composable
private fun PlayerCard(st: Stats, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Box(modifier.clip(RoundedCornerShape(22.dp))) {
        val fav = st.fav
        if (S.cardBg == -1 && fav != null) {
            Box(Modifier.matchParentSize()) { Cover(fav, 400, 400, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Black.copy(alpha = .85f)))))
        } else Box(Modifier.matchParentSize().background(Look.cardBrush(S.cardBg)))
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(84.dp, ring = true)
            Text(Look.nick(), style = T.h2.copy(color = OnCard), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (S.motto.isNotBlank()) Text(S.motto, style = T.small.copy(color = OnCard.copy(alpha = .75f), textAlign = TextAlign.Center), maxLines = 2)
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Уровень ${st.level} · ${Look.title(st.level)}", style = T.small.copy(color = OnCard, fontSize = 12.sp))
            }
            Bar((st.xp % 1000) / 1000f, color = Look.avColor(S.avColor), modifier = Modifier.padding(top = 4.dp))
            Text("${st.xp % 1000} / 1000 опыта до следующего уровня", style = T.small.copy(color = OnCard.copy(alpha = .6f), fontSize = 10.5.sp))
            actions()
        }
    }
}

@Composable
private fun FavGame(g: Game) {
    val ctx = LocalContext.current
    val e = AppState.lib[g.id]
    Sub("Любимая игра")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(66.dp, 88.dp).clip(RoundedCornerShape(12.dp))) { Cover(g, 300, 400, modifier = Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(g.t, style = T.h3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(e?.play?.takeIf { it > 0 }?.let { hrs(it) + " в игре" }, e?.last).joinToString(" · "), style = T.small, maxLines = 1)
            Text(g.plat.full, style = T.small.copy(fontSize = 11.sp))
        }
        KButton("Играть", R.drawable.sb_play, BtnKind.Main, small = true) { Act.play(ctx, g.id) }
    }
}

/** Активность за 7 дней — столбики, сегодня справа. */
@Composable
private fun Week() {
    val week = AppState.week().reversed()
    Sub("Активность за неделю")
    if (week.none { it.second > 0 }) { Text("Пока нет сыгранных дней — здесь появится время в играх по дням.", style = T.small); return }
    val mx = week.maxOf { it.second }.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(14.dp)) {
        Text("Всего за неделю: ${hrs(week.sumOf { it.second })}", style = T.small.copy(color = K.ink2), modifier = Modifier.padding(bottom = 10.dp))
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            week.forEachIndexed { i, (d, m) ->
                val today = i == week.lastIndex
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (m > 0) Text(if (m >= 60) "${m / 60}ч" else "${m}м", style = T.small.copy(fontSize = 10.sp), maxLines = 1)
                    Box(Modifier.padding(top = 3.dp).fillMaxWidth(.62f).height((4f + m.toFloat() / mx * 72f).dp).clip(RoundedCornerShape(6.dp)).background(if (m == 0) K.s3 else if (today) K.acc else K.acc.copy(alpha = .55f)))
                    Text(d, style = T.small.copy(fontSize = 10.5.sp, color = if (today) K.ink else K.ink3), maxLines = 1, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun Badges(st: Stats) {
    val consoles = AppState.byId.values.filter { (AppState.lib[it.id]?.play ?: 0) > 0 }.map { it.p }.toSet().size
    val list = listOf(
        Triple("ach-first.png", "Первый запуск", minOf(1f, st.play.toFloat())),
        Triple("ach-marathon.png", "10 часов в игре", (AppState.lib.values.maxOfOrNull { it.play } ?: 0) / 600f),
        Triple("ach-saves.png", "10 сохранений", st.saves / 10f),
        Triple("ach-consoles.png", "Три консоли", consoles / 3f),
        Triple("ach-trophy.png", "100 часов", st.play / 6000f),
        Triple("ach-fav.png", "5 в избранном", st.favs / 5f),
    )
    Text("Получено ${list.count { it.third >= 1f }} из ${list.size}", style = T.small, modifier = Modifier.padding(bottom = 8.dp))
    list.chunked(3).forEach { row ->
        Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (img, t, p) ->
                val got = p >= 1f
                Row(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(14.dp)).background(K.s1).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssetImage(img, Modifier.size(40.dp), gray = !got)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(t, style = T.small.copy(color = if (got) K.ink else K.ink3, fontSize = 11.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Bar(p.coerceIn(0f, 1f), color = if (got) K.ok else K.acc, height = 3.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RaCard() {
    val logged = S.raToken.isNotEmpty()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (logged) NetImage(Look.raPic(S.raUser), Modifier.size(44.dp).clip(CircleShape).background(K.s2), androidx.compose.ui.layout.ContentScale.Crop)
        else AssetImage("ach-trophy.png", Modifier.size(44.dp))
        Column(Modifier.weight(1f)) {
            Text(if (logged) S.raUser else "Награды прямо в играх", style = T.label)
            Text(if (logged) "${S.raScore} очков · достижения всплывают во время игры" else "Подключи бесплатный аккаунт — тысячи наград для игр PSP", style = T.small, maxLines = 2)
        }
        KButton(if (logged) "Настройки" else "Подключить", kind = if (logged) BtnKind.Soft else BtnKind.Main, small = true) { Nav.settingsTab = "ach"; Nav.open(Ov.Settings) }
    }
}

// ---------- оформление профиля ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditor(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var nick by remember { mutableStateOf(S.nick) }
    var motto by remember { mutableStateOf(S.motto) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) { saveAvatar(ctx, uri) }
            if (ok) S.avMode = "photo" else AppState.say("Не удалось открыть фото")
        }
    }
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Row(
            Modifier.fillMaxWidth(.9f).fillMaxHeight(.92f).clip(RoundedCornerShape(24.dp)).background(K.bg).border(1.dp, K.line, RoundedCornerShape(24.dp)).padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // живой предпросмотр
            Column(Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Оформление", style = T.h3)
                PlayerCard(Stats(), Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                KButton("Готово", R.drawable.ph_check, BtnKind.Main) { S.nick = nick.trim(); S.motto = motto.trim(); onClose() }
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ИМЯ И ДЕВИЗ", style = T.caps)
                SearchField(nick, "Имя игрока", Modifier.fillMaxWidth(), icon = R.drawable.sl_user) { nick = it.take(20); S.nick = nick.trim() }
                SearchField(motto, "Девиз, например «Пройду всё на 100%»", Modifier.fillMaxWidth(), icon = R.drawable.sl_text) { motto = it.take(48); S.motto = motto.trim() }

                Text("АВАТАР", style = T.caps, modifier = Modifier.padding(top = 8.dp))
                val modes = buildList { add("letter" to "Буква"); add("photo" to "Фото"); if (S.raToken.isNotEmpty()) add("ra" to "Из достижений") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Seg(modes.map { it.second }, modes.indexOfFirst { it.first == S.avMode }.coerceAtLeast(0), small = true) {
                        val m = modes[it].first
                        if (m == "photo" && !Look.photoFile(ctx).exists()) pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        else S.avMode = m
                    }
                    if (S.avMode == "photo") KButton("Другое фото", small = true) { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                }
                Text("Цвет — для буквы и полоски опыта", style = T.small)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Look.AV_COLORS.forEachIndexed { i, c ->
                        val on = S.avColor == i
                        Box(Modifier.size(34.dp).clip(CircleShape).background(c).border(if (on) 3.dp else 0.dp, if (on) K.ink else Color.Transparent, CircleShape).press { S.avColor = i }, contentAlignment = Alignment.Center) {
                            if (on) SolarIcon(R.drawable.ph_check, 16.dp, Color(0xFF0E0F13))
                        }
                    }
                }

                Text("ФОН КАРТОЧКИ", style = T.caps, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Look.CARDS.forEachIndexed { i, (name, a, b) ->
                        Swatch(name, S.cardBg == i, Modifier.background(Brush.linearGradient(listOf(a, b)))) { S.cardBg = i }
                    }
                    val fav = Stats().fav
                    if (fav != null) Swatch("Игра", S.cardBg == -1) { S.cardBg = -1 }
                }

                Text("ЛЮБИМАЯ ИГРА", style = T.caps, modifier = Modifier.padding(top = 8.dp))
                val games = AppState.myList().take(16)
                if (games.isEmpty()) Text("Добавь игры — и выбери ту, что будет на карточке.", style = T.small)
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Больше всего часов", S.favGame.isEmpty()) { S.favGame = "" }
                    games.forEach { g -> Chip(g.t, S.favGame == g.id) { S.favGame = g.id } }
                }
            }
        }
    }
}

@Composable
private fun Swatch(name: String, on: Boolean, fill: Modifier = Modifier, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(64.dp, 42.dp).clip(RoundedCornerShape(12.dp)).then(fill).background(if (name == "Игра") K.s2 else Color.Transparent)
            .border(if (on) 2.5.dp else 1.dp, if (on) K.ink else K.line, RoundedCornerShape(12.dp)).press(onClick = onClick), contentAlignment = Alignment.Center) {
            if (name == "Игра") SolarIcon(R.drawable.sl_pad, 18.dp, K.ink2)
        }
        Text(name, style = T.small.copy(fontSize = 10.5.sp, color = if (on) K.ink else K.ink3))
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) K.ink else K.s2).press(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp)) {
        Text(text, style = T.small.copy(color = if (on) K.bg else K.ink2, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Фото для аватара: квадрат по центру, 320×320, JPEG. */
private fun saveAvatar(ctx: Context, uri: android.net.Uri): Boolean = try {
    val src = ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: throw IllegalStateException()
    val side = minOf(src.width, src.height)
    val sq = android.graphics.Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    val out = android.graphics.Bitmap.createScaledBitmap(sq, 320, 320, true)
    Look.photoFile(ctx).outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
    true
} catch (e: Exception) { false }

// ---------- «Поделиться» ----------

/** Карточка профиля картинкой 1200×630 — для мессенджеров и соцсетей. */
private suspend fun shareProfile(ctx: Context, st: Stats) {
    val f = withContext(Dispatchers.IO) { try { drawShareCard(ctx, st) } catch (e: Exception) { null } }
    if (f == null) { AppState.say("Не удалось подготовить карточку"); return }
    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".share", f)
    val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, "Мой профиль в KAPSULA — эмуляторе PSP").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { ctx.startActivity(Intent.createChooser(send, "Поделиться профилем").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { AppState.say("Нет приложений, чтобы поделиться") }
}

internal fun drawShareCard(ctx: Context, st: Stats): File {
    val w = 1200; val h = 630
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val (_, a, b) = Look.card(if (S.cardBg < 0) 0 else S.cardBg)
    c.drawPaint(android.graphics.Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), a.toArgb(), b.toArgb(), android.graphics.Shader.TileMode.CLAMP) })
    val font = try { androidx.core.content.res.ResourcesCompat.getFont(ctx, R.font.manrope) } catch (e: Exception) { null } ?: android.graphics.Typeface.DEFAULT
    fun paint(size: Float, color: Int, bold: Boolean = true) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color
        // шрифт переменный: вес задаётся числом (Android 9+), иначе — обычный жирный
        typeface = if (android.os.Build.VERSION.SDK_INT >= 28) android.graphics.Typeface.create(font, if (bold) 800 else 500, false)
            else android.graphics.Typeface.create(font, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }
    val white = 0xFFF2F1EC.toInt(); val dim = 0xB3F2F1EC.toInt()
    // аватар
    val cx = 150f; val cy = 200f; val r = 90f
    val acc = Look.avColor(S.avColor).toArgb()
    c.drawCircle(cx, cy, r + 6, paint(0f, white))
    val photo = Look.photoFile(ctx).takeIf { S.avMode == "photo" && it.exists() }?.let { android.graphics.BitmapFactory.decodeFile(it.path) }
    if (photo != null) {
        val sh = android.graphics.BitmapShader(photo, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        sh.setLocalMatrix(android.graphics.Matrix().apply { setScale(2 * r / photo.width, 2 * r / photo.height); postTranslate(cx - r, cy - r) })
        c.drawCircle(cx, cy, r, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { shader = sh })
    } else {
        c.drawCircle(cx, cy, r, paint(0f, acc))
        val p = paint(84f, 0xFF0E0F13.toInt()).apply { textAlign = android.graphics.Paint.Align.CENTER }
        c.drawText(Look.nick().take(1).uppercase(), cx, cy + 30f, p)
    }
    // имя, девиз, уровень
    c.drawText(Look.nick().take(20), 280f, 180f, paint(64f, white))
    if (S.motto.isNotBlank()) c.drawText(S.motto.take(48), 282f, 230f, paint(30f, dim, false))
    c.drawText("Уровень ${st.level} · ${Look.title(st.level)}", 282f, if (S.motto.isNotBlank()) 280f else 240f, paint(30f, acc))
    // цифры
    val cells = listOf(
        (if (st.play > 0) hrs(st.play) else "—") to "наиграно", "${st.games}" to "игр", (if (st.streak > 0) days(st.streak) else "—") to "подряд", "${st.saves}" to "сохранений",
    )
    cells.forEachIndexed { i, (n, l) ->
        val x = 70f + i * 270f
        c.drawRoundRect(android.graphics.RectF(x, 360f, x + 250f, 480f), 28f, 28f, paint(0f, 0x1FFFFFFF))
        c.drawText(n, x + 26f, 418f, paint(40f, white))
        c.drawText(l, x + 26f, 458f, paint(26f, dim, false))
    }
    st.fav?.let { c.drawText("Любимая игра: ${it.t}".take(60), 70f, 545f, paint(28f, dim, false)) }
    // знак приложения
    val brand = paint(30f, white).apply { letterSpacing = .25f; textAlign = android.graphics.Paint.Align.RIGHT }
    c.drawText("KAPSULA", w - 70f, 590f, brand)
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = File(dir, "kapsula-profile.png")
    f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    return f
}
