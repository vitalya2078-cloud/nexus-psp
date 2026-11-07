package com.nexus.psp.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R

/** Палитра KAPSULA «графит + слоновая кость» — те же токены, что в веб-прототипе (--bg, --s1…, --acc). */
object K {
    val bg = Color(0xFF0B0C0F)
    val s1 = Color(0xFF131419)
    val s2 = Color(0xFF1A1C22)
    val s3 = Color(0xFF23262E)
    val ink = Color(0xFFF2F1EC)
    val ink2 = Color(0xFFF2F1EC).copy(alpha = .72f)
    val ink3 = Color(0xFFF2F1EC).copy(alpha = .5f)
    val line = Color(0xFFFFFFFF).copy(alpha = .07f)
    val acc = Color(0xFF7FA7D9)
    val ok = Color(0xFF8FC9A3)
    val warn = Color(0xFFD9B36C)
    val bad = Color(0xFFD98A8A)
    val psp = Color(0xFF9DB8D6)
    val ps1 = Color(0xFFB8B4AE)
    val ps2 = Color(0xFF8E96C4)
}

@OptIn(ExperimentalTextApi::class)
private fun manrope(w: Int) = Font(R.font.manrope, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

object T {
    val h1 = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = K.ink)
    val h2 = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = K.ink)
    val body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = K.ink)
    val label = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = K.ink)
    val small = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = K.ink3)
    val caps = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp, color = K.ink3)
}

@Composable
fun KapsulaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = K.acc, background = K.bg, surface = K.s1, onPrimary = K.bg, onBackground = K.ink, onSurface = K.ink),
        content = content,
    )
}

/** Значок Solar: линейный (sl_), с заливкой (sb_) — красятся; duotone (sd_) — со своими цветами. */
@Composable
fun SolarIcon(res: Int, size: Dp = 22.dp, tint: Color = K.ink, modifier: Modifier = Modifier) =
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.size(size))

@Composable
fun SolarDuo(res: Int, size: Dp = 22.dp, modifier: Modifier = Modifier) =
    Icon(painterResource(res), contentDescription = null, tint = Color.Unspecified, modifier = modifier.size(size))

/** Нажимаемая поверхность с лёгким «проседанием» при нажатии, как в вебе (.b:active). */
@Composable
fun Modifier.press(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val down by src.collectIsPressedAsState()
    return this.scale(if (down) .96f else 1f).clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Плитка действия меню паузы (как .qb в вебе). */
@Composable
fun ActionTile(icon: Int, label: String, main: Boolean = false, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (main) K.ink else K.s1
    val fg = when { main -> K.bg; danger -> K.bad; else -> K.ink }
    Column(
        Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, if (main) Color.Transparent else K.line, RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.press(onClick) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SolarIcon(icon, 22.dp, if (enabled) fg else fg.copy(alpha = .35f))
        Text(label, style = T.label.copy(color = if (enabled) fg else fg.copy(alpha = .35f)))
    }
}

/** Пилюля консоли (как .pill.c). */
@Composable
fun ConsolePill(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(13.dp)).background(K.s2).padding(horizontal = 10.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Text(text, style = T.small.copy(color = K.ink2, fontWeight = FontWeight.Bold))
        }
    }
}
