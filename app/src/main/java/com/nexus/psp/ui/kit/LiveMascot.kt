package com.nexus.psp.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.nexus.psp.data.AppState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Живой маскот: 24 целые позы (ui/img/mascot-l-*.webp, одинаковый холст, ноги на одной линии),
 * движение — в коде: дыхание, моргание, взгляды по сторонам, подскок при смене позы, свечение и искорки, реакция на касание.
 */
object Buddy {
    val POSES = listOf(
        "idle", "blink", "lookl", "lookr", "wave", "jump", "laugh", "wow", "sad", "stretch", "mug", "sleep",
        "wink", "think", "cry", "yawn", "box", "coins", "trophy", "heart", "cloud", "phone", "pad", "hide",
    )

    /** Поза приветствия по времени суток: утро — потягивается, день — машет, вечер — кружка, ночь — спит. */
    fun greetPose(hour: Int) = when (hour) { in 6..10 -> "stretch"; in 11..17 -> "wave"; in 18..22 -> "mug"; else -> "sleep" }

    fun greetText(hour: Int, nick: String): String {
        val n = nick.trim().takeIf { it.isNotEmpty() }?.let { ", $it" } ?: ""
        return when (hour) {
            in 6..10 -> "Доброе утро$n!"
            in 11..17 -> "Привет$n! Во что сыграем?"
            in 18..22 -> "Добрый вечер$n. Самое время для игры"
            else -> "Не спится$n? Я тоже"
        }
    }

    /** Реакция на сообщение: по смыслу текста и значку. */
    fun moodFor(text: String, alert: Boolean): String {
        val t = text.lowercase()
        return when {
            alert || listOf("ошиб", "не удалось", "не найден", "нет доступа", "не получилось").any { it in t } -> "sad"
            "жетон" in t -> "coins"
            listOf("испытан", "достижен", "награ", "уровень").any { it in t } -> "trophy"
            listOf("друг", "заявк").any { it in t } -> "heart"
            listOf("облак", "сохранен").any { it in t } -> "cloud"
            listOf("добавлен", "добавил", "скопир", "игра в библиотеке").any { it in t } -> "box"
            listOf("удален", "убрал").any { it in t } -> "wow"
            else -> "wink"
        }
    }

    /** Заход на главный экран за этот запуск уже поприветствовали. */
    var greeted = false
}

/**
 * pose — поза, которую маскот держит (меняется с подскоком). Касание — смех и прыжок.
 * live = false или маскот выключен в настройках — просто картинка позы, без движения.
 */
@Composable
fun LiveMascot(pose: String, size: Dp, modifier: Modifier = Modifier, live: Boolean = true, glow: Boolean = true, onTap: (() -> Unit)? = null) {
    val on = live && runCatching { AppState.S.mascot && !AppState.S.calm }.getOrDefault(true)
    if (!on) { AssetImage("mascot-l-$pose.webp", modifier.size(size)); return }
    var shown by remember { mutableStateOf(pose) }
    var extra by remember { mutableStateOf<String?>(null) } // моргание, взгляд, реакция на касание
    val hop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // смена позы: присел → прыжок → картинка меняется в верхней точке → приземлился с растяжкой
    LaunchedEffect(pose) {
        if (pose == shown) return@LaunchedEffect
        hop.animateTo(.5f, tween(170, easing = FastOutSlowInEasing))
        shown = pose
        hop.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        hop.snapTo(0f)
    }
    // жизнь в покое: моргнуть раз в 3–6 с, иногда посмотреть по сторонам
    LaunchedEffect(shown) {
        if (shown !in setOf("idle", "wave", "think", "box", "heart", "pad", "phone")) return@LaunchedEffect
        val rnd = java.util.Random()
        while (true) {
            delay(3000L + rnd.nextInt(3000))
            if (extra != null) continue
            if (shown == "idle" && rnd.nextInt(4) == 0) {
                extra = if (rnd.nextBoolean()) "lookl" else "lookr"; delay(1300); extra = null
            } else if (shown == "idle") { extra = "blink"; delay(130); extra = null }
        }
    }
    val inf = rememberInfiniteTransition(label = "buddy")
    val breath by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart), label = "br")
    val tw by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart), label = "tw")

    Box(
        modifier.size(size).tap {
            onTap?.invoke()
            if (extra == null) scope.launch {
                extra = "laugh"
                hop.animateTo(.5f, tween(150)); hop.animateTo(1f, tween(240)); hop.snapTo(0f)
                delay(700); extra = null
            }
        },
    ) {
        if (glow) Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val c = Offset(w / 2, w * .56f)
            val pulse = .5f + .5f * kotlin.math.sin(breath * 2 * Math.PI).toFloat()
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD27A).copy(alpha = .22f + .06f * pulse), Color.Transparent), c, w * .5f), w * .5f, c)
            // три искорки: всплывают и гаснут по очереди
            for (i in 0 until 3) {
                val p = (tw + i / 3f) % 1f
                val a = kotlin.math.sin(p * Math.PI).toFloat()
                val x = w * (.18f + .32f * i)
                val y = w * (.42f - .3f * p)
                drawCircle(Color(0xFFFFE3A3).copy(alpha = .75f * a), w * .012f * (1 + a), Offset(x, y))
            }
        }
        // h: 0 → .5 — присед и взлёт, .5 → 1 — падение и растяжка при приземлении
        val h = hop.value
        val up = if (h <= .5f) kotlin.math.sin(h * 2 * Math.PI / 2).toFloat() else kotlin.math.sin((1 - h) * 2 * Math.PI / 2).toFloat()
        val squash = if (h > .8f) (1 - h) * 5 * .08f else 0f
        val b = kotlin.math.sin(breath * 2 * Math.PI).toFloat()
        AssetImage(
            "mascot-l-${extra ?: shown}.webp",
            Modifier.fillMaxSize().graphicsLayer {
                transformOrigin = TransformOrigin(.5f, 1f)
                translationY = -up * this.size.height * .1f
                scaleY = 1f + .018f * b - squash
                scaleX = 1f - .01f * b + squash
            },
        )
    }
}
