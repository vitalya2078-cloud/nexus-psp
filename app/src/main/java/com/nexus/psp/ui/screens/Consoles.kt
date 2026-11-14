package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.PL
import com.nexus.psp.data.PLATFORMS
import com.nexus.psp.data.Platform
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.GameCard
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Pill
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press

private fun games(p: String) = AppState.byId.values.count { it.p == p }

private fun gamesText(n: Int) = if (n == 0) "Пока нет игр" else "$n " + when { n % 10 == 1 && n % 100 != 11 -> "игра"; n % 10 in 2..4 && n % 100 !in 12..14 -> "игры"; else -> "игр" }

/** Все консоли: по группам, карточка — картинка, название, сколько игр. Нажал — её полка с играми. */
@Composable
fun ConsolesScreen() {
    Full {
        OvHead("Консоли")
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PLATFORMS.groupBy { it.family }.forEach { (fam, list) ->
                item(span = { GridItemSpan(maxLineSpan) }) { Text(fam.uppercase(), style = T.caps, modifier = Modifier.padding(top = 6.dp)) }
                items(list, key = { it.id }) { p -> ConsoleCard(p) }
            }
        }
    }
}

@Composable
private fun ConsoleCard(p: Platform) {
    val n = games(p.id)
    Row(
        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(p.color.copy(alpha = .16f), K.s1)))
            .border(1.dp, if (n > 0) p.color.copy(alpha = .45f) else K.line, RoundedCornerShape(16.dp)).press { Nav.open(Ov.Console(p.id)) }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AssetImage(p.conImg, Modifier.width(64.dp).height(44.dp))
        Column(Modifier.weight(1f)) {
            Text(p.short, style = T.label.copy(fontSize = 14.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(gamesText(n), style = T.small, maxLines = 1)
        }
        if (p.exp) Pill("тест", color = K.warn, bg = K.warn.copy(alpha = .14f))
        else if (p.biosLevel == 2) Pill("BIOS", color = K.ink3)
    }
}

/** Полка консоли: картинка, что нужно для запуска, форматы, игры и кнопки добавления. */
@Composable
fun ConsoleScreen(id: String) {
    val p = PL[id] ?: run { Nav.back(); return }
    val ctx = LocalContext.current
    val list = AppState.byId.values.filter { it.p == id }.sortedByDescending { AppState.lib[it.id]?.t ?: 0 }
    val bios = remember(id) { com.nexus.psp.emulator.BiosFiles.found(ctx, id) }
    Full {
        OvHead(p.full, back = true)
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.horizontalGradient(listOf(p.color.copy(alpha = .2f), K.s1))).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    AssetImage(p.conImg, Modifier.width(150.dp).height(104.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(gamesText(list.size), style = T.h3)
                        Text("Форматы: " + if (id == "ps1") "CUE с BIN, CHD, PBP, M3U, ISO, IMG" else p.exts.joinToString(", ") { it.uppercase() }, style = T.small)
                        Text(when {
                            p.biosLevel == 0 -> "BIOS не нужен — просто добавь игру"
                            bios.isNotEmpty() -> "BIOS на месте: " + bios.joinToString(", ")
                            else -> p.biosHint
                        }, style = T.small.copy(color = if (p.biosLevel == 2 && bios.isEmpty()) K.warn else K.ink2))
                        if (p.exp) Text("Тестовый режим: нужен мощный телефон, не все игры пойдут", style = T.small.copy(color = K.warn))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                        KButton("Добавить игру", R.drawable.ph_plus, BtnKind.Main, small = true) { Act.addFile() }
                        KButton("Найти на телефоне", R.drawable.sl_search, small = true) { Act.findGames() }
                        if (p.bios) KButton("BIOS", R.drawable.sl_chip, small = true) { Nav.settingsTab = "bios"; Nav.open(Ov.Settings) }
                    }
                }
            }
            if (list.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                StateCard("es-addgames.png", "Тут пока пусто", "Добавь образ своей игры для ${p.short} — он появится на этой полке.")
            } else items(list, key = { it.id }) { GameCard(it, null, mine = true) }
        }
    }
}
