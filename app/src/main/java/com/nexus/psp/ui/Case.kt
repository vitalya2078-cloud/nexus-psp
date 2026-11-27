package com.nexus.psp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nexus.psp.data.Game
import com.nexus.psp.ui.kit.AssetImage

/** Футляр приставки: размер картинки и окно этикетки (доли 0..1) — из tools/art_cut.py (режим case). */
data class Case(val w: Int, val h: Int, val l: Float, val t: Float, val r: Float, val b: Float)

object Cases {
    private val all = mapOf(
        "box" to Case(295, 420, 0.1178f, 0.1356f, 0.8459f, 0.8856f),
        "cart16" to Case(338, 291, 0.2041f, 0.0997f, 0.7988f, 0.7698f),
        "cart8" to Case(358, 303, 0.1927f, 0.1617f, 0.8073f, 0.6568f),
        "cartsmall" to Case(284, 315, 0.162f, 0.2603f, 0.8345f, 0.8222f),
        "cd" to Case(347, 325, 0.1671f, 0.0677f, 0.9193f, 0.9323f),
        "dvd" to Case(301, 420, 0.0732f, 0.0548f, 0.9076f, 0.9498f),
        "phone" to Case(181, 420, 0.1768f, 0.1674f, 0.8232f, 0.4217f),
        "umd" to Case(266, 420, 0.1123f, 0.2f, 0.8804f, 0.9241f),
    )

    /** Какой футляр у приставки: UMD, диск, DVD, картриджи, кнопочный телефон, коробка. */
    fun name(platform: String) = when (platform) {
        "psp" -> "umd"
        "ps1", "saturn", "dc" -> "cd"
        "ps2" -> "dvd"
        "md", "sms" -> "cart16"
        "nes", "a26", "a78" -> "cart8"
        "gg", "ngp", "ws", "lynx", "pce" -> "cartsmall"
        "j2me" -> "phone"
        else -> "box"
    }

    fun of(platform: String): Case = all.getValue(name(platform))
}

/** Обложка игры в футляре своей приставки: обложка — в окне этикетки, сверху — сам футляр; под ним мягкая тень. */
@Composable
fun CaseCover(g: Game, modifier: Modifier = Modifier) {
    val c = Cases.of(g.p)
    BoxWithConstraints(modifier.aspectRatio(c.w.toFloat() / c.h).drawBehind {
        // тень на столе: тёмный овал под футляром
        drawOval(Brush.radialGradient(listOf(Color.Black.copy(alpha = .45f), Color.Transparent), Offset(size.width / 2, size.height * .98f), size.width * .55f),
            Offset(size.width * -.05f, size.height * .9f), Size(size.width * 1.1f, size.height * .16f))
    }) {
        // обложка чуть шире окна — края уходят под рамку, без просветов
        Box(Modifier.offset(maxWidth * (c.l - .012f), maxHeight * (c.t - .01f)).size(maxWidth * (c.r - c.l + .024f), maxHeight * (c.b - c.t + .02f))) {
            Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
        }
        AssetImage("case/${Cases.name(g.p)}.webp", Modifier.fillMaxSize())
    }
}
