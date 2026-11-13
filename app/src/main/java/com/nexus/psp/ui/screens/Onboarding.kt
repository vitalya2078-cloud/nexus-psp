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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.PLATFORMS
import com.nexus.psp.data.plural
import com.nexus.psp.data.size
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.ui.Act
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

/** Знакомство при первом запуске (п. 6): консоли → как играть → первая игра. */
@Composable
fun Onboarding(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val cons = remember { mutableStateListOf("psp", "ps1", "ps2") }
    var how by remember { mutableStateOf("touch") }
    fun finish() {
        S.con = if (cons.size == 1) cons[0] else "all"
        S.hide = how == "pad"; if (how == "pad") S.ctlMode = "pad"
        S.onboarded = true; Nav.go("games"); onDone()
    }
    Box(Modifier.fillMaxSize().background(K.bg).tap { }.windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal = 32.dp, vertical = 20.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { i -> Box(Modifier.size(28.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= step) K.acc else K.s3)) } }
            AnimatedContent(step, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "onb", modifier = Modifier.weight(1f)) { st ->
                // на невысоких экранах шаг прокручивается — карточки не обрезаются
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 18.dp)) {
                    Text(listOf("Во что играешь?", "Как будешь играть?", "Добавь первую игру")[st], style = T.h1)
                    Text(
                        listOf("Выбери консоли — KAPSULA настроит под них библиотеку.", "Можно поменять в любой момент в настройках.", "Образ своего диска или бесплатная homebrew-игра: ISO, CSO, CHD или PBP.")[st],
                        style = T.body, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
                    )
                    Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (st) {
                            0 -> PLATFORMS.forEach { p ->
                                val on = p.id in cons
                                Opt(on, Modifier.weight(1f), { if (on) cons.remove(p.id) else cons.add(p.id) }) {
                                    AssetImage(p.conImg, Modifier.fillMaxWidth().height(if (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 380) 52.dp else 78.dp))
                                    ConsolePill(p.short, p.color)
                                    Text(p.full, style = T.h3)
                                    Text(if (p.ready) "Работает" else "Скоро подключим", style = T.small.copy(color = if (p.ready) K.ok else K.ink3))
                                }
                            }
                            1 -> listOf(Triple("touch", "Сенсорный экран", "Кнопки KAPSULA Touch поверх игры"), Triple("pad", "Геймпад", "Bluetooth или USB — экранные кнопки скроются")).forEach { (id, t, d) ->
                                Opt(how == id, Modifier.weight(1f), { how = id }) {
                                    SolarIcon(if (id == "pad") R.drawable.sl_pad else R.drawable.ph_hand, 24.dp, K.acc)
                                    Text(t, style = T.h3)
                                    Text(d, style = T.small)
                                }
                            }
                            else -> {
                                Opt(false, Modifier.weight(1f), { finish(); Act.addFile() }) {
                                    SolarIcon(R.drawable.sl_folder, 24.dp, K.acc)
                                    Text("Выбрать файл", style = T.h3)
                                    Text("Одна игра — скопируется в KAPSULA", style = T.small)
                                }
                                Opt(false, Modifier.weight(1f), { finish(); Act.findGames() }) {
                                    SolarIcon(R.drawable.sl_search, 24.dp, K.acc)
                                    Text("Найти на телефоне", style = T.h3)
                                    Text("Выбери папку, например «Загрузки» — KAPSULA найдёт все игры", style = T.small)
                                }
                            }
                        }
                    }
                }
            }
            // кнопки шагов — отдельной полосой с воздухом над ней, не впритык к карточкам
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                KButton(if (step == 2) "Позже" else "Пропустить") { finish() }
                Spacer(Modifier.weight(1f))
                if (step > 0) { KButton("Назад") { step-- }; Spacer(Modifier.width(8.dp)) }
                if (step < 2) KButton("Далее", R.drawable.sl_right, BtnKind.Main) { step++ }
            }
        }
    }
}

@Composable
private fun Opt(on: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(if (on) K.acc.copy(alpha = .1f) else K.s1)
            .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(18.dp)).press(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}
