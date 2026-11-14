package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.FNode
import com.nexus.psp.data.PL
import com.nexus.psp.data.Place
import com.nexus.psp.data.Storage
import com.nexus.psp.data.prettyName
import com.nexus.psp.data.realGame
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Pill
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun mb(b: Long) = when { b >= 1L shl 30 -> String.format("%.1f ГБ", b / 1073741824.0).replace('.', ','); b >= 1L shl 20 -> "${b shr 20} МБ"; b >= 1024 -> "${b shr 10} КБ"; else -> "$b Б" }

/** Нет доступа к файлам: объясняем зачем и даём два способа. */
@Composable
fun AccessAsk(title: String) {
    StateCard("es-addgames.png", title,
        "Чтобы показать твои игры, KAPSULA нужен доступ к файлам. Можно открыть весь телефон — тогда поиск найдёт всё сам. Или выбрать одну папку, где лежат игры. Мы только читаем файлы и ничего никуда не отправляем.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton("Весь телефон", R.drawable.sl_device, BtnKind.Main, small = true) { Act.requestAllFiles?.invoke() }
            KButton("Выбрать папку", R.drawable.sl_folder, small = true) { Act.requestFolder?.invoke() }
        }
    }
}

/**
 * Проводник KAPSULA: места слева, папки и файлы справа. mode = "games" — выбираем игры (несколько разом),
 * "bios:<консоль>" — один файл BIOS. Подходящие файлы подсвечены, к CUE сами добавляются его BIN.
 */
@Composable
fun FilesScreen(mode: String) {
    val ctx = LocalContext.current
    val tick = AppState.storageTick
    val places = remember(tick) { Storage.places(ctx) }
    var place by remember(tick) { mutableStateOf(places.firstOrNull()) }
    val path = remember(place) { mutableStateListOf<FNode>().also { l -> place?.let { l.add(it.node) } } }
    val sel = remember { mutableStateListOf<FNode>() }
    var q by remember { mutableStateOf("") }
    val bios = mode.startsWith("bios:")
    val biosPlat = mode.removePrefix("bios:")
    val cur = path.lastOrNull()
    val items by produceState<List<FNode>?>(null, cur?.key, tick) { value = null; value = cur?.let { withContext(Dispatchers.IO) { Storage.list(ctx, it) } } ?: emptyList() }
    val scope = rememberCoroutineScope()
    Full {
        OvHead(if (bios) "BIOS для ${PL[biosPlat]?.short ?: ""}" else "Выбор игр", back = true) {
            KButton("Системное окно", small = true) { if (bios) AppState.say("Выбери файл BIOS в «Настройки → BIOS»") else Act.pickFile?.invoke() }
        }
        if (places.isEmpty()) { Box(Modifier.padding(16.dp)) { AccessAsk("Покажем твои файлы") }; return@Full }
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // места
            Column(Modifier.width(190.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                places.forEach { pl ->
                    val on = place?.node?.key == pl.node.key
                    Row(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.s2 else androidx.compose.ui.graphics.Color.Transparent)
                        .press { place = pl; path.clear(); path.add(pl.node); q = "" }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SolarIcon(when (pl.icon) { "phone" -> R.drawable.sl_device; "dl" -> R.drawable.sl_dl; "sd" -> R.drawable.sl_save; else -> R.drawable.sl_folder }, 18.dp, if (on) K.acc else K.ink2)
                        Text(pl.title, style = T.label.copy(fontSize = 13.5.sp, color = if (on) K.ink else K.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text("+ Ещё папка", style = T.small.copy(color = K.acc), modifier = Modifier.padding(start = 12.dp, top = 6.dp).press { Act.requestFolder?.invoke() }.padding(vertical = 6.dp))
                if (!Storage.allFiles(ctx)) Text("Открыть весь телефон", style = T.small.copy(color = K.acc), modifier = Modifier.padding(start = 12.dp).press { Act.requestAllFiles?.invoke() }.padding(vertical = 6.dp))
            }
            // папка
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        path.forEachIndexed { i, n ->
                            if (i > 0) Text("›", style = T.small, modifier = Modifier.padding(horizontal = 6.dp))
                            Text(if (i == 0) place?.title ?: "" else n.name, style = T.label.copy(fontSize = 13.sp, color = if (i == path.lastIndex) K.ink else K.ink3),
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).press { while (path.size > i + 1) path.removeAt(path.lastIndex) }.padding(4.dp))
                        }
                    }
                    SearchField(q, "Найти в папке", Modifier.width(220.dp)) { q = it }
                }
                val list = items
                when {
                    list == null -> Text("Открываю…", style = T.small, modifier = Modifier.padding(top = 16.dp))
                    else -> {
                        val shown = list.filter { q.isBlank() || it.name.contains(q.trim(), true) }
                        if (shown.isEmpty()) Text(if (q.isBlank()) "Тут пусто" else "Ничего не нашлось", style = T.small, modifier = Modifier.padding(top = 16.dp))
                        LazyColumn(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(shown, key = { it.key }) { n ->
                                // консоль файла читается в фоне: у обычных файлов — по содержимому
                                val plat by produceState<String?>(null, n.key) { if (!bios && !n.isDir) value = withContext(Dispatchers.IO) { Storage.platformOf(n) } }
                                val fits = n.isDir || (if (bios) true else n.ext in com.nexus.psp.emulator.GameLibrary.EXT)
                                val on = sel.any { it.key == n.key }
                                Row(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.acc.copy(alpha = .14f) else androidx.compose.ui.graphics.Color.Transparent)
                                    .press {
                                        when {
                                            n.isDir -> { path.add(n); q = "" }
                                            !fits -> AppState.say("Это не игра — такой формат KAPSULA не открывает")
                                            bios -> { sel.clear(); sel.add(n) }
                                            on -> sel.removeAll { it.key == n.key }
                                            else -> sel.add(n)
                                        }
                                    }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SolarIcon(if (n.isDir) R.drawable.sl_folder else if (fits) R.drawable.ph_disc else R.drawable.sl_text, 20.dp, if (n.isDir) K.ink2 else if (fits) K.acc else K.ink3)
                                    Text(n.name, style = T.label.copy(fontSize = 13.5.sp, color = if (fits) K.ink else K.ink3), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    if (!n.isDir && fits && !bios) plat?.let { p -> Pill(PL[p]?.short ?: p, dot = PL[p]?.color) }
                                    if (!n.isDir) Text(mb(n.size), style = T.small, modifier = Modifier.width(70.dp))
                                    if (!n.isDir && fits) Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) K.acc else K.line2, CircleShape).background(if (on) K.acc else androidx.compose.ui.graphics.Color.Transparent), contentAlignment = Alignment.Center) {
                                        if (on) SolarIcon(R.drawable.ph_check, 14.dp, K.bg)
                                    }
                                }
                            }
                        }
                    }
                }
                // выбранное
                if (sel.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).clip(RoundedCornerShape(16.dp)).background(K.s1).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (bios) sel.first().name else "Выбрано: ${sel.size} · ${mb(sel.sumOf { it.size })}", style = T.label.copy(fontSize = 13.5.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    KButton("Сбросить", small = true) { sel.clear() }
                    KButton(if (bios) "Подключить" else "Добавить", R.drawable.ph_check, BtnKind.Main, small = true) {
                        val picked = sel.toList(); val siblings = items ?: emptyList()
                        scope.launch {
                            if (bios) {
                                val r = withContext(Dispatchers.IO) { com.nexus.psp.emulator.BiosFiles.add(ctx, biosPlat, picked.first().uri) }
                                if (r.startsWith("ok:")) { AppState.say("BIOS на месте: " + r.removePrefix("ok:"), R.drawable.ph_check); AppState.storageTick++; Nav.back() } else AppState.say(r.removePrefix("bad:"), R.drawable.sl_alert)
                            } else {
                                // к CUE — его дорожки из той же папки
                                val extra = withContext(Dispatchers.IO) { picked.filter { it.ext == "cue" }.flatMap { c -> Storage.cueParts(ctx, c) }.mapNotNull { nm -> siblings.firstOrNull { it.name.equals(nm, true) } } }
                                val all = (picked + extra).distinctBy { it.key }
                                Act.importUris?.invoke(all.map { it.uri }); sel.clear(); Nav.back()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- поиск игр по телефону ----------

private class Hit(val node: FNode, val plat: String, val inLib: Boolean)
private class BiosHit(val node: FNode, val plat: String)

/** «Найти игры»: по кнопке — обходим доступные места, показываем найденное по консолям, добавляем выбранное. */
@Composable
fun ScanScreen() {
    val ctx = LocalContext.current
    val tick = AppState.storageTick
    var state by remember { mutableStateOf("idle") } // idle | run | done
    var checked by remember { mutableStateOf(0) }
    val hits = remember { mutableStateListOf<Hit>() }
    val bioses = remember { mutableStateListOf<BiosHit>() }
    val pick = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val siblings = remember { HashMap<String, List<FNode>>() }
    fun run() {
        state = "run"; checked = 0; hits.clear(); bioses.clear(); pick.clear(); siblings.clear()
        scope.launch {
            val known = AppState.byId.values.map { it.file.lowercase() }.toSet()
            withContext(Dispatchers.IO) {
                val seen = HashSet<String>()
                fun walk(n: FNode, depth: Int, parentKey: String) {
                    if (depth > 8 || !seen.add(n.key)) return
                    val kids = Storage.list(ctx, n)
                    siblings[n.key] = kids
                    val parts = kids.filter { it.ext == "cue" }.flatMap { Storage.cueParts(ctx, it) }.map { it.lowercase() }.toSet()
                    for (k in kids) {
                        if (k.isDir) { walk(k, depth + 1, n.key); continue }
                        checked++
                        if (k.name.lowercase() in parts) continue
                        Storage.maybeBios(k)?.let { p -> bioses.add(BiosHit(k, p)) }
                        val plat = Storage.platformOf(k) ?: continue
                        // мелкие BIN без CUE — скорее всего не игры (кроме картриджей)
                        if (k.ext == "bin" && k.size < 8 * 1024 && plat == "a26") continue
                        val inLib = k.name.lowercase() in known
                        hits.add(Hit(k, plat, inLib))
                        if (!inLib) pick.add(k.key)
                    }
                }
                Storage.places(ctx).forEach { pl -> walk(pl.node, 0, "") }
            }
            state = "done"
        }
    }
    Full {
        OvHead("Найти игры на телефоне", back = true) {
            if (state == "done") KButton("Ещё раз", small = true) { run() }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when {
                !remember(tick) { Storage.hasAny(ctx) } -> AccessAsk("Где искать игры?")
                state == "idle" -> Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(K.s1).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    AssetImage("es-addgames.png", Modifier.width(150.dp).height(80.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Пробегусь по файлам и соберу всё, во что можно поиграть", style = T.h3)
                        Text("Где ищем: " + Storage.places(ctx).joinToString(", ") { it.title } + ". Игры добавятся, только когда ты их выберешь.", style = T.small)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                        KButton("Искать", R.drawable.sl_search, BtnKind.Main) { run() }
                        Text("+ Ещё папка", style = T.small.copy(color = K.acc), modifier = Modifier.press { Act.requestFolder?.invoke() }.padding(4.dp))
                    }
                }
                state == "run" -> Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssetImage("es-addgames.png", Modifier.width(200.dp).height(100.dp))
                    Text("Ищу игры…", style = T.h2)
                    Text("Проверено файлов: $checked · нашёл: ${hits.size}", style = T.body)
                }
                else -> ScanResults(hits, bioses, pick, siblings)
            }
        }
    }
}

@Composable
private fun ScanResults(hits: List<Hit>, bioses: List<BiosHit>, pick: MutableList<String>, siblings: Map<String, List<FNode>>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    if (hits.isEmpty() && bioses.isEmpty()) {
        StateCard("es-nothing.png", "Игр не нашлось", "Проверь, что образы лежат в папке, к которой есть доступ. Можно добавить ещё папку или открыть весь телефон.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Ещё папка", R.drawable.sl_folder, small = true) { Act.requestFolder?.invoke() }
                if (!Storage.allFiles(ctx)) KButton("Весь телефон", R.drawable.sl_device, small = true) { Act.requestAllFiles?.invoke() }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            hits.groupBy { it.plat }.toSortedMap(compareBy { p -> com.nexus.psp.data.PLATFORMS.indexOfFirst { it.id == p } }).forEach { (plat, list) ->
                item(key = "h_$plat") {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AssetImage(PL[plat]?.conImg ?: "con-psp.png", Modifier.width(44.dp).height(30.dp))
                        Text("${PL[plat]?.short ?: plat} · ${list.size}", style = T.h3, modifier = Modifier.weight(1f))
                        val fresh = list.filter { !it.inLib }.map { it.node.key }
                        if (fresh.isNotEmpty()) Text(if (fresh.all { it in pick }) "Снять все" else "Выбрать все", style = T.small.copy(color = K.acc),
                            modifier = Modifier.press { if (fresh.all { it in pick }) pick.removeAll(fresh) else pick.addAll(fresh.filter { it !in pick }) }.padding(6.dp))
                    }
                }
                items(list, key = { it.node.key }) { h ->
                    val on = h.node.key in pick
                    Row(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).background(if (on) K.acc.copy(alpha = .12f) else K.s1)
                        .press { if (h.inLib) AppState.say("Эта игра уже в библиотеке") else if (on) pick.remove(h.node.key) else pick.add(h.node.key) }.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp, 48.dp).clip(RoundedCornerShape(8.dp))) { Cover(realGame("scan_" + h.node.name, h.node.name.substringBeforeLast('.'), h.node.name, "", 0.0, h.plat), 120, 160, modifier = Modifier.fillMaxSize()) }
                        Column(Modifier.weight(1f)) {
                            Text(prettyName(h.node.name.substringBeforeLast('.')), style = T.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${h.node.ext.uppercase()} · ${mb(h.node.size)}", style = T.small, maxLines = 1)
                        }
                        if (h.inLib) Pill("в библиотеке", icon = R.drawable.ph_check, color = K.ok)
                        else Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) K.acc else K.line2, CircleShape).background(if (on) K.acc else androidx.compose.ui.graphics.Color.Transparent), contentAlignment = Alignment.Center) {
                            if (on) SolarIcon(R.drawable.ph_check, 14.dp, K.bg)
                        }
                    }
                }
            }
            if (bioses.isNotEmpty()) {
                item(key = "bios") { Text("ПОХОЖЕ НА BIOS", style = T.caps, modifier = Modifier.padding(top = 14.dp)) }
                items(bioses, key = { "b_" + it.node.key }) { b ->
                    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(K.s1).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SolarIcon(R.drawable.sl_chip, 20.dp, K.acc)
                        Column(Modifier.weight(1f)) {
                            Text(b.node.name, style = T.label.copy(fontSize = 13.5.sp), maxLines = 1)
                            Text("Похоже на BIOS ${PL[b.plat]?.short ?: b.plat} · ${mb(b.node.size)}", style = T.small)
                        }
                        KButton("Подключить", small = true) {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { com.nexus.psp.emulator.BiosFiles.add(ctx, b.plat, b.node.uri) }
                                if (r.startsWith("ok:")) AppState.say("BIOS ${PL[b.plat]?.short} подключён", R.drawable.ph_check) else AppState.say(r.removePrefix("bad:"), R.drawable.sl_alert)
                            }
                        }
                    }
                }
            }
        }
        if (pick.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).clip(RoundedCornerShape(16.dp)).background(K.s1).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val chosen = hits.filter { it.node.key in pick }
            Text("Выбрано: ${chosen.size} · ${mb(chosen.sumOf { it.node.size })}", style = T.label.copy(fontSize = 13.5.sp), modifier = Modifier.weight(1f))
            KButton("Добавить", R.drawable.ph_check, BtnKind.Main, small = true) {
                scope.launch {
                    val uris = withContext(Dispatchers.IO) {
                        chosen.flatMap { h ->
                            val parent = siblings.values.firstOrNull { l -> l.any { it.key == h.node.key } } ?: emptyList()
                            listOf(h.node) + if (h.node.ext == "cue") Storage.cueParts(ctx, h.node).mapNotNull { nm -> parent.firstOrNull { it.name.equals(nm, true) } } else emptyList()
                        }.distinctBy { it.key }.map { it.uri }
                    }
                    Act.importUris?.invoke(uris); Nav.back()
                }
            }
        }
    }
}
