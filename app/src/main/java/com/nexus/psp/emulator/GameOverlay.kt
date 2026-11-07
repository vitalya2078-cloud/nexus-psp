package com.nexus.psp.emulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.ui.kit.ActionTile
import com.nexus.psp.ui.kit.ConsolePill
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press

private val CONSOLE = mapOf("psp" to ("PlayStation Portable" to K.psp), "ps1" to ("PlayStation" to K.ps1), "ps2" to ("PlayStation 2" to K.ps2))

/** Всё, что поверх картинки игры: заставка загрузки, меню паузы, ошибка, всплывающая подсказка. */
@Composable
fun GameOverlay(a: GameActivity, ui: GameActivity.UiState) {
    val (conName, conColor) = CONSOLE[a.platform] ?: CONSOLE.getValue("psp")
    Box(Modifier.fillMaxSize()) {
        // заставка, пока ядро не нарисовало первый кадр
        AnimatedVisibility(ui.phase == "loading", enter = fadeIn(), exit = fadeOut(tween(500))) {
            Boot(a.title, conName, conColor)
        }
        AnimatedVisibility(ui.phase == "ready" && ui.paused, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
            PauseMenu(a, ui, conName, conColor)
        }
        if (ui.phase == "error") ErrorScreen(ui.error) { a.exitGame() }
        AnimatedVisibility(ui.toast.isNotEmpty(), Modifier.align(Alignment.TopCenter).padding(top = 14.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(
                ui.toast,
                style = T.label.copy(color = K.bg),
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(K.ink).padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun Boot(title: String, con: String, color: Color) {
    val pulse by rememberInfiniteTransition(label = "boot").animateFloat(.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
    Column(
        Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // капсула с «экраном» — тот же знак, что в веб-заставке PSP
        Box(Modifier.width(150.dp).height(62.dp).border(2.dp, color.copy(alpha = pulse), RoundedCornerShape(31.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.width(54.dp).height(38.dp).border(2.dp, color.copy(alpha = pulse), RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = T.h2.copy(fontSize = 20.sp))
        Spacer(Modifier.height(6.dp))
        Text(con.uppercase(), style = T.caps.copy(color = K.ink3))
    }
}

@Composable
private fun PauseMenu(a: GameActivity, ui: GameActivity.UiState, con: String, color: Color) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .35f), .55f to Color.Black.copy(alpha = .8f), 1f to Color.Black.copy(alpha = .95f)))
            // касание пустого места не уходит в игру
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().windowInsetsPadding(WindowInsets.displayCutout).padding(start = 22.dp, end = 22.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ConsolePill(con, color)
            Text(a.title, style = T.h1)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile(R.drawable.sb_play, "Продолжить", main = true) { a.setPaused(false) }
                ActionTile(R.drawable.sl_save, "Сохранить") { a.saveQuick() }
                ActionTile(R.drawable.sl_load, "Загрузить", enabled = ui.hasState) { a.loadQuick() }
                ActionTile(R.drawable.sl_bolt, "Ускорение", on = ui.fast, sub = if (ui.fast) "×2 · вкл" else "Выкл") { a.toggleFast() }
                ActionTile(R.drawable.sl_exit, "Выйти", danger = true) { a.exitGame() }
            }
            Text(
                if (ui.hasState) "Быстрое сохранение есть — «Загрузить» вернёт к нему" else "Быстрых сохранений пока нет",
                style = T.small,
            )
        }
    }
}

@Composable
private fun ErrorScreen(msg: String, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SolarIcon(R.drawable.sl_alert, 40.dp, K.warn)
        Spacer(Modifier.height(14.dp))
        Text("Игра не запустилась", style = T.h2)
        Spacer(Modifier.height(6.dp))
        Text(msg, style = T.body.copy(color = K.ink2, fontWeight = FontWeight.Medium), textAlign = TextAlign.Center, modifier = Modifier.width(420.dp))
        Spacer(Modifier.height(18.dp))
        Box(Modifier.clip(RoundedCornerShape(22.dp)).background(K.ink).press(onClick = onExit).padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text("Выйти", style = T.label.copy(color = K.bg))
        }
    }
}
