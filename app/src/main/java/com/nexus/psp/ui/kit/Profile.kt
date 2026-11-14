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
}

/** Аватар игрока: буква на цвете, своё фото или аватар RetroAchievements. */
@Composable
fun Avatar(size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val S = AppState.S
    val ctx = LocalContext.current
    val col = Look.avColor(S.avColor)
    val base = modifier.size(size).clip(CircleShape).let { if (ring) it.border(2.dp, K.ink.copy(alpha = .9f), CircleShape) else it }
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
                val f = File(ctx.cacheDir, "net/" + url.hashCode().toUInt().toString(16) + ".img")
                if (!f.exists()) { f.parentFile?.mkdirs(); java.net.URL(url).openStream().use { i -> f.outputStream().use { i.copyTo(it) } } }
                android.graphics.BitmapFactory.decodeFile(f.path)?.asImageBitmap()?.also { netCache.put(url, it) }
            } catch (e: Exception) { null }
        }
    }
    Box(modifier) { img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = scale) } }
}
private val netCache = android.util.LruCache<String, ImageBitmap>(40)
