package com.nexus.psp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Game
import com.nexus.psp.data.hrs
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.FileImage
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Avatar
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Look
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.launch
import java.io.File

/** Белый текст на баннере — фон баннера всегда тёмный. */
private val OnBanner = Color(0xFFF2F1EC)

/** Сохранение для «Продолжить»: игра, слот, когда, картинка момента. */
private class SaveRef(val g: Game, val slot: Int, val time: Long, val thumb: File?)

/** Скриншот из игры (копия для «Моментов»): files/moments/<id игры>__<время>.jpg. */
class Moment(val file: File, val gameId: String, val time: Long)

object Moments {
    fun dir(c: android.content.Context) = File(c.filesDir, "moments").apply { mkdirs() }
    fun list(c: android.content.Context): List<Moment> = dir(c).listFiles()?.filter { it.name.endsWith(".jpg") }?.mapNotNull { f ->
        val p = f.nameWithoutExtension.split("__"); if (p.size < 2) null else Moment(f, p[0], p[1].toLongOrNull() ?: f.lastModified())
    }?.sortedByDescending { it.time } ?: emptyList()
}

/**
 * «Моё» — всё про тебя в одном месте: профиль, цифры, где остановился, моменты, награды, игра по сети.
 * Пока игр мало — подсказка, как их добавить.
 */
@Composable
fun MyScreen() {
    val ctx = LocalContext.current
    val st = Stats()
    var view by remember { mutableStateOf<Moment?>(null) }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Banner(st, onEdit = { com.nexus.psp.ui.Nav.open(com.nexus.psp.ui.Ov.Look("profile")) }, onShare = { scope.launch { shareProfile(ctx, st) } })
        StatsStrip(st)
        if (AppState.byId.size < 3) AddGuide()
        ContinueRow()
        MomentsRow { view = it }
        Section("Награды")
        Badges(st)
        RaCard()
    }
    view?.let { m -> MomentViewer(m) { view = null } }
}

@Composable
private fun Section(t: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t.uppercase(), style = T.caps, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Баннер: фон (оформление или обложка любимой игры), аватар, имя, девиз, уровень, кнопки. */
@Composable
internal fun Banner(st: Stats, onEdit: () -> Unit, onShare: (() -> Unit)?) {
    if (com.nexus.psp.data.Cloud.isAdmin) { MyCreatorBanner(st, onEdit, onShare); return }
    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp))) {
        com.nexus.psp.ui.kit.BannerBg(Look.bannerId(), st.fav, Modifier.matchParentSize())
        if (com.nexus.psp.data.Cloud.isAdmin) Sparkles(Modifier.matchParentSize())
        Row(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Avatar(104.dp, Modifier.press(onClick = onEdit), ring = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val admin = com.nexus.psp.data.Cloud.isAdmin
                    PlayerName(Look.nick(), admin, T.h1.copy(color = OnBanner), com.nexus.psp.data.Cloud.creator.optString("name", "gold"), Modifier.weight(1f, fill = false))
                    if (admin) PixelCrown(22.dp)
                    PlayerId()
                }
                com.nexus.psp.data.Cloud.creator.optString("status").takeIf { com.nexus.psp.data.Cloud.isAdmin && it.isNotEmpty() }?.let {
                    Text(it, style = T.small.copy(color = Creator.GOLD, fontSize = 13.sp), maxLines = 1)
                }
                Text(S.motto.ifBlank { "Нажми на аватар и придумай себе девиз" }, style = T.small.copy(color = OnBanner.copy(alpha = if (S.motto.isBlank()) .45f else .75f), fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("Ур. ${st.level} · ${Look.titleText(st.level)}", style = T.small.copy(color = OnBanner, fontSize = 12.sp))
                    }
                    Column(Modifier.width(180.dp)) {
                        Bar((st.xp % 1000) / 1000f, color = Look.avColor(S.avColor))
                        Text("ещё ${1000 - st.xp % 1000} опыта до ${st.level + 1}-го", style = T.small.copy(color = OnBanner.copy(alpha = .55f), fontSize = 10.5.sp), modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            // в «Оформлении» баннер — только превью, без кнопок
            if (onShare != null) Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                Box(Modifier.press(onClick = onEdit)) { TokensChip() }
                KButton("Оформить", icon = R.drawable.sl_brush, small = true, onClick = onEdit)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    var find by remember { mutableStateOf(false) }
                    KButton(icon = R.drawable.sl_search, small = true) { find = true }
                    KButton("Поделиться", small = true, onClick = onShare)
                    if (find) FindPlayerDialog { find = false }
                }
            }
        }
    }
}

/** Свой баннер создателя + жетоны и кнопки под ним. */
@Composable
private fun MyCreatorBanner(st: Stats, onEdit: () -> Unit, onShare: (() -> Unit)?) {
    val cr = com.nexus.psp.data.Cloud.creator
    val last = AppState.lib.entries.filter { it.value.play > 0 }.maxByOrNull { it.value.t }
    val playing = last?.let { e -> AppState.byId[e.key]?.let { g ->
        org.json.JSONObject().put("t", g.t).put("p", g.p).put("live", false)
            .put("at", java.time.Instant.ofEpochMilli(e.value.t).atOffset(java.time.ZoneOffset.UTC).toString())
    } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CreatorBanner(CreatorCard(
            nick = Look.nick(), motto = S.motto, status = cr.optString("status"), nameStyle = cr.optString("name", "gold"),
            shortId = com.nexus.psp.data.Cloud.shortId, level = st.level, xp = (st.xp % 1000) / 1000f,
            title = Look.titleText(st.level), banner = Look.bannerId(),
            avatar = { s -> Avatar(s, Modifier.press(onClick = onEdit)) }, playing = playing,
        ))
        if (onShare != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.press(onClick = onEdit)) { TokensChip() }
            Box(Modifier.weight(1f))
            var find by remember { mutableStateOf(false) }
            KButton("Оформить", icon = R.drawable.sl_brush, small = true, onClick = onEdit)
            KButton(icon = R.drawable.sl_search, small = true) { find = true }
            KButton("Поделиться", small = true, onClick = onShare)
            if (find) FindPlayerDialog { find = false }
        }
    }
}

/** Цифры одной строкой. */
@Composable
private fun StatsStrip(st: Stats) {
    val items = listOf(
        Triple(R.drawable.sl_clock, if (st.play > 0) hrs(st.play) else "0 мин", "в играх"),
        Triple(R.drawable.sl_bolt, if (st.streak > 0) days(st.streak) else "0 дней", "подряд"),
        Triple(R.drawable.sl_grid, "${st.games}", "игр"),
        Triple(R.drawable.sl_save, "${st.saves}", "сохранений"),
        Triple(R.drawable.sl_heart, "${st.favs}", "в избранном"),
        Triple(R.drawable.sl_trophy, if (S.raToken.isNotEmpty()) S.raScore else "—", "очков наград"),
    )
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (ic, n, l) ->
            Row(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(14.dp)).background(K.s1).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SolarIcon(ic, 17.dp, K.acc)
                Column { Text(n, style = T.label.copy(fontSize = 14.sp), maxLines = 1); Text(l, style = T.small.copy(fontSize = 10.5.sp), maxLines = 1) }
            }
        }
    }
}

/** Пустой блок: картинка, пара слов и (необязательно) кнопка. */
@Composable
private fun Empty(img: String, title: String, text: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (img.startsWith("mascot-")) com.nexus.psp.ui.kit.Mascot(img.removePrefix("mascot-"), 64.dp) else AssetImage(img, Modifier.width(96.dp).height(64.dp))
        Column(Modifier.weight(1f)) { Text(title, style = T.label.copy(fontSize = 14.sp)); Text(text, style = T.small) }
        action?.invoke()
    }
}

/** «Добавь игры» — пока их меньше трёх. */
@Composable
private fun AddGuide() {
    Section("Добавь игры")
    Empty("mascot-wave", if (AppState.byId.isEmpty()) "Пока тут пусто — закинь первую игру" else "Ещё пару игр — и станет веселее",
        "Образы своих дисков: PSP — ISO, CSO, CHD; PS1 — CUE с BIN, CHD, PBP. Можно выбрать файл или поискать по телефону.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton("Выбрать файл", R.drawable.ph_plus, BtnKind.Main, small = true) { Act.addFile() }
            KButton("Найти", R.drawable.sl_search, small = true) { Act.findGames() }
        }
    }
}

/** «Продолжить»: последние сохранения из всех игр с картинкой момента. */
@Composable
private fun ContinueRow() {
    val ctx = LocalContext.current
    val saves = remember(AppState.byId.size, AppState.lib.values.sumOf { it.saves }) {
        AppState.byId.values.flatMap { g ->
            listOf(4, 0, 1, 2, 3).mapNotNull { s -> AppState.stateFile(g, s).takeIf { it.exists() }?.let { f -> SaveRef(g, s, f.lastModified(), File(f.path + ".jpg").takeIf { it.exists() }) } }
        }.sortedByDescending { it.time }.distinctBy { it.g.id to it.slot }.take(10)
    }
    Section("Продолжить")
    if (saves.isEmpty()) { Empty("es-saves.png", "Сохранений пока нет", "Сохранись в игре — и сможешь вернуться к этому месту прямо отсюда."); return }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        saves.forEach { r ->
            Column(Modifier.width(196.dp).clip(RoundedCornerShape(16.dp)).background(K.s1).press { Act.play(ctx, r.g.id, r.slot) }) {
                Box(Modifier.fillMaxWidth().height(110.dp).background(K.s2)) {
                    if (r.thumb != null) FileImage(r.thumb, Modifier.fillMaxSize()) else Cover(r.g, 392, 220, modifier = Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.Center).size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = .55f)), contentAlignment = Alignment.Center) {
                        SolarIcon(R.drawable.sb_play, 20.dp, OnBanner)
                    }
                }
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(r.g.t, style = T.label.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((if (r.slot == 4) "Где вышел" else if (r.slot == 0) "Быстрое" else "Слот ${r.slot}") + " · " + ago(r.time), style = T.small.copy(fontSize = 11.sp), maxLines = 1)
                }
            }
        }
    }
}

/** «5 мин назад», «вчера», «12 нояб.». */
private fun ago(t: Long): String {
    val d = System.currentTimeMillis() - t
    return when {
        d < 60_000 -> "только что"
        d < 3_600_000 -> "${d / 60_000} мин назад"
        d < 86_400_000 -> "${d / 3_600_000} ч назад"
        d < 2 * 86_400_000 -> "вчера"
        else -> java.text.SimpleDateFormat("d MMM", java.util.Locale("ru")).format(java.util.Date(t))
    }
}

/** «Моменты»: скриншоты из игр. */
@Composable
private fun MomentsRow(onOpen: (Moment) -> Unit) {
    val ctx = LocalContext.current
    val list = remember(AppState.lib.values.sumOf { it.play }) { Moments.list(ctx).take(20) }
    Section("Моменты")
    if (list.isEmpty()) { Empty("es-moments.png", "Тут будут твои скриншоты", "В игре: пауза → «Скриншот». Красивые моменты соберутся здесь."); return }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        list.forEach { m ->
            Box(Modifier.size(196.dp, 110.dp).clip(RoundedCornerShape(14.dp)).background(K.s2).press { onOpen(m) }) {
                FileImage(m.file, Modifier.fillMaxSize())
                Text(AppState.byId[m.gameId]?.t ?: "", style = T.small.copy(color = OnBanner, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .7f)))).padding(horizontal = 8.dp, vertical = 5.dp))
            }
        }
    }
}

/** Скриншот на весь экран: «Поделиться», «Удалить». */
@Composable
private fun MomentViewer(m: Moment, onClose: () -> Unit) {
    val ctx = LocalContext.current
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .94f)).press(onClick = onClose)) {
            FileImage(m.file, Modifier.fillMaxSize().padding(24.dp), maxDim = 1600)
            Row(Modifier.align(Alignment.TopEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Поделиться", small = true) {
                    try {
                        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
                        val f = File(dir, "kapsula-moment.jpg"); m.file.copyTo(f, overwrite = true)
                        val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".share", f)
                        ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("image/jpeg").putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION), "Поделиться").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) { AppState.say("Не получилось поделиться") }
                }
                KButton(icon = R.drawable.sl_trash, small = true) { m.file.delete(); AppState.say("Удалено"); onClose() }
                KButton(icon = R.drawable.ph_x, small = true) { onClose() }
            }
        }
    }
}
