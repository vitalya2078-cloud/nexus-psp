package com.nexus.psp.ui.kit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.data.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Оформление профиля: цвета аватара и фоны карточки игрока. */
object Look {
    val AV_COLORS = listOf(
        Color(0xFF8FB3E8), Color(0xFFE8A27C), Color(0xFF9BD3A8), Color(0xFFD9B36C),
        Color(0xFFC79BE0), Color(0xFFE07C8F), Color(0xFF7CC8D9), Color(0xFFF0EEE8),
    )
    /** Фоны карточки: название и два цвета градиента. */
    val CARDS = listOf(
        Triple("Ночь", Color(0xFF233054), Color(0xFF10131C)),
        Triple("Закат", Color(0xFF6A3044), Color(0xFF1B1020)),
        Triple("Океан", Color(0xFF124556), Color(0xFF0B1720)),
        Triple("Лес", Color(0xFF24472F), Color(0xFF0D1510)),
        Triple("Аврора", Color(0xFF34265C), Color(0xFF0F2C33)),
        Triple("Графит", Color(0xFF34363E), Color(0xFF121317)),
    )
    fun avColor(i: Int) = AV_COLORS[i.coerceIn(0, AV_COLORS.lastIndex)]
    fun card(i: Int) = CARDS[i.coerceIn(0, CARDS.lastIndex)]
    fun cardBrush(i: Int) = card(i).let { Brush.linearGradient(listOf(it.second, it.third)) }

    fun nick(): String = AppState.S.nick.trim().ifEmpty { "Игрок" }
    fun photoFile(c: android.content.Context) = File(c.filesDir, "avatar.jpg")
    fun raPic(user: String) = "https://media.retroachievements.org/UserPic/$user.png"

    /** Уровень по опыту и звание. */
    fun level(xp: Int) = xp / 1000 + 1
    fun title(level: Int) = when { level >= 11 -> "Легенда"; level >= 7 -> "Мастер"; level >= 4 -> "Знаток"; level >= 2 -> "Игрок"; else -> "Новичок" }
    /** Титул на профиле: выбранный (если открыт) или по уровню. */
    fun titleText(level: Int): String = AppState.S.titleSel.takeIf { it.isNotEmpty() }
        ?.let { com.nexus.psp.data.Cos.find(com.nexus.psp.data.Cos.Kind.TT, it) }?.takeIf { com.nexus.psp.data.Cos.has(it) }?.name ?: title(level)
    /** Баннер профиля: выбранный (если открыт) или прежний фон карточки. */
    fun bannerId(): String = AppState.S.banner.takeIf { it.isNotEmpty() }
        ?.takeIf { id -> com.nexus.psp.data.Cos.find(com.nexus.psp.data.Cos.Kind.BN, id)?.let { com.nexus.psp.data.Cos.has(it) } == true }
        ?: if (AppState.S.cardBg == -1) "game" else "g${AppState.S.cardBg.coerceIn(0, CARDS.lastIndex)}"
    /** Рамка аватара (только открытая). */
    fun frameId(): String = AppState.S.frame.takeIf { id -> id.isNotEmpty() && com.nexus.psp.data.Cos.find(com.nexus.psp.data.Cos.Kind.FR, id)?.let { com.nexus.psp.data.Cos.has(it) } == true } ?: ""
}

/**
 * Фон баннера профиля: картинка (bn_*), градиент (g0…g5) или обложка любимой игры (game).
 * dark — затемнение слева направо, чтобы текст на баннере читался.
 */
@Composable
fun BannerBg(id: String, fav: com.nexus.psp.data.Game?, modifier: Modifier = Modifier) {
    Box(modifier) {
        when {
            id.startsWith("bn_") -> {
                AssetImage("look/$id.webp", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .55f), Color.Black.copy(alpha = .25f), Color.Black.copy(alpha = .45f)))))
            }
            id == "game" && fav != null -> {
                com.nexus.psp.ui.Cover(fav, 800, 300, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .85f), Color.Black.copy(alpha = .45f)))))
            }
            else -> Box(Modifier.fillMaxSize().background(Look.cardBrush(id.removePrefix("g").toIntOrNull() ?: 0)))
        }
    }
}

/** Аватар игрока: буква на цвете, своё фото или аватар RetroAchievements. */
@Composable
fun Avatar(size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val S = AppState.S
    val ctx = LocalContext.current
    val col = Look.avColor(S.avColor)
    val frame = if (ring) Look.frameId() else ""
    if (frame.isNotEmpty()) {
        // рамка рисуется поверх, чуть шире аватара; сам аватар — без белого кольца
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Avatar(size * .8f, ring = false)
            if (frame == "fr_creator") com.nexus.psp.ui.screens.PixelCrownFrame(size) else AssetImage("look/$frame.webp", Modifier.size(size))
        }
        return
    }
    val base = modifier.size(size).clip(CircleShape).let { if (ring) it.border(2.dp, K.ink.copy(alpha = .9f), CircleShape) else it }
    if (S.avMode == "set") {
        Box(base.background(Color(0xFF1D1F29))) { AssetImage("look/${S.avItem}.webp", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        return
    }
    val photo = S.avMode == "photo" && Look.photoFile(ctx).exists()
    val ra = S.avMode == "ra" && S.raUser.isNotEmpty() && S.raToken.isNotEmpty()
    when {
        photo -> {
            val stamp = Look.photoFile(ctx).lastModified()
            val img by produceState<ImageBitmap?>(null, stamp) {
                value = withContext(Dispatchers.IO) { try { android.graphics.BitmapFactory.decodeFile(Look.photoFile(ctx).path)?.asImageBitmap() } catch (e: Exception) { null } }
            }
            Box(base.background(col)) { img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
        }
        ra -> NetImage(Look.raPic(S.raUser), base.background(col), ContentScale.Crop)
        else -> Box(base.background(col), contentAlignment = Alignment.Center) {
            Text(Look.nick().take(1).uppercase(), style = T.label.copy(color = Color(0xFF0E0F13), fontWeight = FontWeight.ExtraBold, fontSize = (size.value * .42f).sp))
        }
    }
}

/** Картинка из интернета с кэшем в памяти и на диске (аватары). */
@Composable
fun NetImage(url: String, modifier: Modifier, scale: ContentScale = ContentScale.Fit) {
    val ctx = LocalContext.current
    val img by produceState(netCache.get(url), url) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            try {
                val f = com.nexus.psp.data.Net.cached(ctx, url, "net2") ?: return@withContext null
                android.graphics.BitmapFactory.decodeFile(f.path)?.asImageBitmap()?.also { netCache.put(url, it) } ?: run { f.delete(); null }
            } catch (e: Exception) { null }
        }
    }
    Box(modifier) { img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = scale) } }
}
private val netCache = android.util.LruCache<String, ImageBitmap>(40)
