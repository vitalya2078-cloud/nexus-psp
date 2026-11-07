package com.nexus.psp.ui.screens

import android.graphics.PointF
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Game
import com.nexus.psp.emulator.ControlLayout
import com.nexus.psp.emulator.ControlsView
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.kit.Chip
import com.nexus.psp.ui.kit.ChipRow
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.Range
import com.nexus.psp.ui.kit.Seg
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.R as Res
import org.json.JSONObject

/** Положения кнопок из настроек → смещения для ControlsView. */
fun loadOffsets(v: ControlsView, json: String) {
    v.offsets.clear()
    try { val o = JSONObject(json); o.keys().forEach { k -> val a = o.getJSONArray(k); v.offsets[k] = PointF(a.getDouble(0).toFloat(), a.getDouble(1).toFloat()) } } catch (e: Exception) { }
}

/** Экранный геймпад: режим, живое превью с перетаскиванием кнопок, прозрачность, размер, левша (п. 20, 56–58). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PadScreen() {
    val modes = listOf("touch", "classic", "pad")
    var prev by rememberSaveable { mutableStateOf("touch") }
    val chips = when (S.ctlMode) {
        "classic" -> listOf("classic:psp" to "PSP", "classic:ps1" to "PS1", "classic:ps2" to "PS2")
        "touch" -> listOf("touch:psp" to "PSP", "touch:ps2" to "PS1 / PS2", "foot" to "Пример пресета: пешком", "drive" to "Пример: за рулём")
        else -> emptyList()
    }
    val pv = chips.firstOrNull { it.first == prev }?.first ?: chips.firstOrNull()?.first ?: ""
    val bg: Game = com.nexus.psp.data.PREVIEW_GAME
    Full {
        OvHead("Экранный геймпад", actions = {
            KButton("Сбросить", small = true) { S.op = 55; S.sc = 100; S.ctlMode = "touch"; S.keyPos = "{}"; S.keyIcon = "{}"; S.keyTurbo = "{}"; AppState.say("Раскладка сброшена") }
        })
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1.6f)) {
                Box(Modifier.fillMaxWidth().aspectRatio(844f / 390f).clip(RoundedCornerShape(18.dp)).border(1.dp, K.line, RoundedCornerShape(18.dp))) {
                    Cover(bg, 1600, 740, 11, Modifier.fillMaxSize())
                    if (S.ctlMode == "pad") Text("Экранные кнопки скрыты — играй на геймпаде", style = T.label.copy(color = androidx.compose.ui.graphics.Color(0xFFF2F1EC)), modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(16.dp)).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .6f)).padding(12.dp))
                    else {
                        val lay = when { pv.startsWith("classic") -> "classic"; pv == "foot" || pv == "drive" -> pv; else -> "touch" }
                        val plat = if (pv.contains(':')) pv.substringAfter(':') else "psp"
                        val op = S.op; val sc = S.sc; val mir = S.mirror; val kp = S.keyPos
                        AndroidView(
                            factory = { c -> ControlsView(c, object : ControlsView.Listener { override fun key(code: Int, down: Boolean) {}; override fun stick(right: Boolean, x: Float, y: Float) {}; override fun action(act: String) {} }) },
                            update = { v ->
                                v.editMode = true; v.layName = "$lay:$plat"; v.style = if (lay == "classic") "classic" else "touch"; v.platform = plat
                                v.customIcons.clear(); try { JSONObject(S.keyIcon).let { o -> o.keys().forEach { k -> v.customIcons[k] = o.getString(k) } } } catch (e: Exception) { }
                                v.turbo.clear(); try { JSONObject(S.keyTurbo).let { o -> o.keys().forEach { k -> v.turbo.add(k) } } } catch (e: Exception) { }
                                v.mirror = mir; v.scaleK = sc / 100f; v.alpha = (op / 100f).coerceAtLeast(.15f)
                                loadOffsets(v, kp)
                                v.onMoved = { key, x, y -> val o = try { JSONObject(S.keyPos) } catch (e: Exception) { JSONObject() }; o.put(key, org.json.JSONArray(listOf(x.toDouble(), y.toDouble()))); S.keyPos = o.toString() }
                                v.setLayout(if (lay == "foot" || lay == "drive") ControlLayout.preset(lay, true) else ControlLayout.layout(lay, plat))
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                if (chips.isNotEmpty()) ChipRow(Modifier.padding(top = 10.dp)) { chips.forEach { (id, t) -> Chip(t, pv == id) { prev = id } } }
                Text("Перетаскивай кнопки на превью — положение запомнится для этой раскладки.", style = T.small, modifier = Modifier.padding(top = 8.dp))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Opt("Режим", null) { Seg(listOf("Touch", "Классика", "Геймпад"), modes.indexOf(S.ctlMode).coerceAtLeast(0), small = true) { S.ctlMode = modes[it] } }
                Text(
                    when (S.ctlMode) {
                        "touch" -> "Стик появляется там, где коснулся палец. Кнопки действий — дугой под большой палец. В игре: пауза → Настройки → Управление → «Переставить», нажми на кнопку — выберешь значок действия и турбо."
                        "classic" -> "Свой джойстик для каждой консоли — выбирается сам по игре: у PSP — L/R, крестовина и «таблетка», у PS1 — L1/L2/R1/R2, у PS2 — два стика. Сверху — ускорение, сохранить, загрузить, пауза."
                        else -> "Экранные кнопки скрыты. Подключи геймпад по Bluetooth или USB."
                    }, style = T.small, modifier = Modifier.padding(vertical = 8.dp),
                )
                Text("Прозрачность", style = T.label.copy(fontSize = 14.sp), modifier = Modifier.padding(top = 8.dp))
                Range(S.op, 15, 100, fill = true) { S.op = it }
                Text("Размер кнопок", style = T.label.copy(fontSize = 14.sp), modifier = Modifier.padding(top = 4.dp))
                Range(S.sc, 70, 130, fill = true) { S.sc = it }
                Opt("Для левшей", "Зеркальная раскладка") { Toggle(S.mirror) { S.mirror = it } }
                Opt("Скрывать с геймпадом", null) { Toggle(S.hide) { S.hide = it } }
            }
        }
    }
}
