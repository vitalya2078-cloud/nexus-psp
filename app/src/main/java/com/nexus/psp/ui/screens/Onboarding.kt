package com.nexus.psp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.GAMES
import com.nexus.psp.data.PLATFORMS
import com.nexus.psp.data.plural
import com.nexus.psp.data.size
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.ConsolePill
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import com.nexus.psp.ui.kit.tap

/** Знакомство при первом запуске (п. 6): консоли → где хранить → как играть. */
@Composable
fun Onboarding(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val cons = remember { mutableStateListOf("psp", "ps1", "ps2") }
    var how by remember { mutableStateOf("touch") }
    val ctx = LocalContext.current
    fun finish() {
        S.con = if (cons.size == 1) cons[0] else "all"
        S.hide = how == "pad"; if (how == "pad") S.ctlMode = "pad"
        S.onboarded = true; Nav.go("store"); onDone()
    }
    Box(Modifier.fillMaxSize().background(K.bg).tap { }.windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal = 32.dp, vertical = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { i -> Box(Modifier.size(28.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= step) K.acc else K.s3)) } }
            AnimatedContent(step, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "onb", modifier = Modifier.weight(1f)) { st ->
                Column(Modifier.fillMaxSize().padding(top = 18.dp)) {
                    Text(listOf("Во что играешь?", "Где хранить игры?", "Как будешь играть?")[st], style = T.h1)
                    Text(
                        listOf("Выбери консоли — KAPSULA покажет подходящие игры.", "Игры весят 0,2–2 ГБ. Свободно ${size(AppState.free())} (демо).", "Можно поменять в любой момент.")[st],
                        style = T.body, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (st) {
                            0 -> PLATFORMS.forEach { p ->
                                val on = p.id in cons
                                Opt(on, Modifier.weight(1f), { if (on) cons.remove(p.id) else cons.add(p.id) }) {
                                    AssetImage(p.conImg, Modifier.fillMaxWidth().height(90.dp))
                                    ConsolePill(p.short, p.color)
                                    Text(p.full, style = T.h3)
                                    Text(plural(GAMES.count { it.p == p.id }, "игра", "игры", "игр") + " в каталоге", style = T.small)
                                }
                            }
                            1 -> Opt(true, Modifier.weight(1f), {}) {
                                SolarIcon(R.drawable.sl_folder, 24.dp, K.acc)
                                Text("Память телефона", style = T.h3)
                                Text(CoreFiles.gamesDir(ctx).absolutePath.replace("/storage/emulated/0/", ""), style = T.small)
                                Text("Выбор своей папки и SD-карты появится позже.", style = T.small)
                            }
                            else -> listOf(Triple("touch", "Сенсорный экран", "Кнопки KAPSULA Touch поверх игры"), Triple("pad", "Геймпад", "Bluetooth или USB — экранные кнопки скроются")).forEach { (id, t, d) ->
                                Opt(how == id, Modifier.weight(1f), { how = id }) {
                                    SolarIcon(if (id == "pad") R.drawable.sl_pad else R.drawable.ph_hand, 24.dp, K.acc)
                                    Text(t, style = T.h3)
                                    Text(d, style = T.small)
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                KButton("Пропустить") { finish() }
                Spacer(Modifier.weight(1f))
                if (step > 0) { KButton("Назад") { step-- }; Spacer(Modifier.width(8.dp)) }
                KButton(if (step == 2) "Готово" else "Далее", R.drawable.sl_right, BtnKind.Main) { if (step < 2) step++ else finish() }
            }
        }
    }
}

@Composable
private fun Opt(on: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (on) K.acc.copy(alpha = .1f) else K.s1)
            .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(18.dp)).press(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}
