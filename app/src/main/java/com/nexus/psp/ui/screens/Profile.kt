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

/** Белый текст на фоне карточки — фон всегда тёмный, при любой теме. */
private val OnCard = Color(0xFFF2F1EC)

internal fun days(n: Int) = "$n " + when { n % 10 == 1 && n % 100 != 11 -> "день"; n % 10 in 2..4 && n % 100 !in 12..14 -> "дня"; else -> "дней" }

/** Карточка игрока: фон (оформление или обложка любимой игры), аватар, ник, девиз, уровень. */
@Composable
internal fun PlayerCard(st: Stats, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Box(modifier.clip(RoundedCornerShape(22.dp))) {
        com.nexus.psp.ui.kit.BannerBg(Look.bannerId(), st.fav, Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Avatar(84.dp, ring = true)
            Text(Look.nick(), style = T.h2.copy(color = OnCard), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (S.motto.isNotBlank()) Text(S.motto, style = T.small.copy(color = OnCard.copy(alpha = .75f), textAlign = TextAlign.Center), maxLines = 2)
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Уровень ${st.level} · ${Look.titleText(st.level)}", style = T.small.copy(color = OnCard, fontSize = 12.sp))
            }
            Bar((st.xp % 1000) / 1000f, color = Look.avColor(S.avColor), modifier = Modifier.padding(top = 4.dp))
            Text("${st.xp % 1000} / 1000 опыта до следующего уровня", style = T.small.copy(color = OnCard.copy(alpha = .6f), fontSize = 10.5.sp))
            actions()
        }
    }
}



/** Награды профиля: картинка, название, прогресс 0…1 (≥ 1 — получена). */
internal fun badgeList(st: Stats): List<Triple<String, String, Float>> {
    val consoles = AppState.byId.values.filter { (AppState.lib[it.id]?.play ?: 0) > 0 }.map { it.p }.toSet().size
    return listOf(
        Triple("ach-first.png", "Первый запуск", minOf(1f, st.play.toFloat())),
        Triple("ach-marathon.png", "10 часов в игре", (AppState.lib.values.maxOfOrNull { it.play } ?: 0) / 600f),
        Triple("ach-saves.png", "10 сохранений", st.saves / 10f),
        Triple("ach-consoles.png", "Три консоли", consoles / 3f),
        Triple("ach-trophy.png", "100 часов", st.play / 6000f),
        Triple("ach-fav.png", "5 в избранном", st.favs / 5f),
    )
}

@Composable
internal fun Badges(st: Stats) {
    val list = badgeList(st)
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
internal fun RaCard() {
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

internal fun saveAvatar(ctx: Context, uri: android.net.Uri): Boolean = try {
    val src = ctx.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: throw IllegalStateException()
    val side = minOf(src.width, src.height)
    val sq = android.graphics.Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
    val out = android.graphics.Bitmap.createScaledBitmap(sq, 320, 320, true)
    Look.photoFile(ctx).outputStream().use { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
    true
} catch (e: Exception) { false }

// ---------- «Поделиться» ----------

/** Карточка профиля картинкой 1200×630 — для мессенджеров и соцсетей. */
internal suspend fun shareProfile(ctx: Context, st: Stats) {
    val f = withContext(Dispatchers.IO) { try { drawShareCard(ctx, st) } catch (e: Exception) { null } }
    if (f == null) { AppState.say("Карточка не собралась, попробуй ещё раз"); return }
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
    c.drawText("Уровень ${st.level} · ${Look.titleText(st.level)}", 282f, if (S.motto.isNotBlank()) 280f else 240f, paint(30f, acc))
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
