package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.data.Secrets
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.Balsamiq
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.LiveMascot
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press

/** «Секреты»: найденные — с позой Сейвика и его фразой, ненайденные — загадка-подсказка. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecretsScreen() {
    Full {
        OvHead("Секреты · ${Secrets.found.size} из ${Secrets.ALL.size}")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("По KAPSULA спрятаны пасхалки. Подсказки — под замками. Сейвик сам скажет, когда найдёшь.", style = T.small)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val order = Secrets.ALL.sortedBy { if (Secrets.has(it.id)) 0 else 1 }
                order.forEach { e -> EggCard(e) }
            }
        }
    }
}

@Composable
private fun EggCard(e: Secrets.Egg) {
    val got = Secrets.has(e.id)
    Column(
        Modifier.width(150.dp).height(188.dp).clip(RoundedCornerShape(18.dp)).background(if (got) K.s2 else K.s1)
            .border(1.dp, if (got) K.acc.copy(alpha = .45f) else K.line, RoundedCornerShape(18.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
            if (got) LiveMascot(e.pose, 82.dp, glow = false)
            else {
                AssetImage("mascot-l-${e.pose}.webp", Modifier.size(82.dp).alpha(.08f))
                Text("?", style = T.h1.copy(fontFamily = Balsamiq, fontSize = 40.sp, color = K.ink2))
            }
        }
        Text(if (got) e.title else "???", style = T.label.copy(fontFamily = Balsamiq, fontSize = 14.sp), maxLines = 1, textAlign = TextAlign.Center)
        Text(if (got) e.text else e.hint, style = T.small.copy(fontSize = 11.sp, lineHeight = 14.sp), maxLines = 4, textAlign = TextAlign.Center)
    }
}

/** Карточка в «Моё»: прогресс и вход в «Секреты». */
@Composable
fun SecretsCard() {
    val n = Secrets.found.size; val all = Secrets.ALL.size
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).press { Nav.open(Ov.Secrets) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LiveMascot(Secrets.found.lastOrNull()?.let { id -> Secrets.ALL.firstOrNull { it.id == id }?.pose } ?: "detective", 64.dp, glow = false)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (n == 0) "Тут спрятаны секреты" else "Найдено $n из $all", style = T.label.copy(fontSize = 14.sp))
            Text(if (n == 0) "Пасхалки по всему приложению. Попробуй пощекотать Сейвика…" else "Открой список — там подсказки к остальным", style = T.small)
            Bar(n / all.toFloat(), color = K.acc)
        }
    }
}
