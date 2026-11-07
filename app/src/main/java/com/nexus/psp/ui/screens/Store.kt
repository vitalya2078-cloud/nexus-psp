package com.nexus.psp.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.SolarDuo
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle

/**
 * Магазин: каталога пока нет (п. 23 — панель автора на Supabase), поэтому — честный экран «скоро»:
 * что здесь будет, «сообщить, когда откроется» и путь к своей игре.
 */
@Composable
fun StoreScreen() {
    Row(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        AssetImage("con-all.png", Modifier.weight(1f).height(240.dp))
        Column(Modifier.weight(1.2f).widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("МАГАЗИН KAPSULA", style = T.caps.copy(color = K.acc))
            Text("Скоро откроется", style = T.h1.copy(fontSize = 30.sp))
            Text("Здесь появятся бесплатные homebrew-игры и игры от авторов, которые разрешили их распространять. Скачал — и играешь, без поиска файлов.", style = T.body)
            if (!AppState.online) StateCard("es-offline.png", "Нет интернета", "Когда магазин откроется, для него понадобится сеть. Свои игры работают без интернета.")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SolarDuo(R.drawable.sd_bell, 24.dp)
                Column(Modifier.weight(1f)) {
                    Text("Сообщить, когда откроется", style = T.label.copy(fontSize = 14.sp))
                    Text("Придёт уведомление в KAPSULA", style = T.small)
                }
                Toggle(S.storeNotify) { on ->
                    if (!on) { S.storeNotify = false; return@Toggle }
                    val ask = com.nexus.psp.ui.Act.ensureNotify
                    if (ask == null) { S.storeNotify = true; return@Toggle }
                    ask { ok ->
                        S.storeNotify = ok
                        if (ok) AppState.say("Сообщим, когда магазин откроется", R.drawable.ph_check)
                        else AppState.say("Уведомления запрещены", R.drawable.sl_alert, "Разрешить") { com.nexus.psp.ui.Act.openNotifySettings?.invoke() }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Добавить свою игру", R.drawable.ph_plus, BtnKind.Main) { Act.addFile() }
                KButton("Найти на телефоне", R.drawable.sl_search) { Act.findGames() }
            }
        }
    }
}
