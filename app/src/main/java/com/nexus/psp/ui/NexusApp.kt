package com.nexus.psp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF08090F)
private val Rail = Color(0xFF10111B)
private val Panel = Color(0xFF151724)
private val PanelLight = Color(0xFF1B1E30)
private val Neon = Color(0xFF9B5CFF)
private val NeonBlue = Color(0xFF35D7FF)
private val Muted = Color(0xFF85899F)
private val White = Color(0xFFF4F2FF)

private enum class Tab { HOME, LIBRARY, SETTINGS }

@Composable
fun NexusApp() {
    var selected by remember { mutableStateOf(Tab.HOME) }

    Row(Modifier.fillMaxSize().background(Bg)) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight().width(76.dp),
            containerColor = Rail,
            contentColor = White,
            header = {
                Box(
                    Modifier.padding(top = 18.dp, bottom = 28.dp)
                        .size(42.dp)
                        .background(
                            Brush.linearGradient(listOf(Neon, Color(0xFF5424B8))),
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }
        ) {
            RailItem(selected == Tab.HOME, { selected = Tab.HOME }, Icons.Rounded.Home, "Домой")
            RailItem(selected == Tab.LIBRARY, { selected = Tab.LIBRARY }, Icons.Rounded.SportsEsports, "Игры")
            RailItem(selected == Tab.SETTINGS, { selected = Tab.SETTINGS }, Icons.Rounded.Settings, "Опции")
        }

        Box(Modifier.fillMaxSize()) {
            when (selected) {
                Tab.HOME -> HomeScreen(onLibrary = { selected = Tab.LIBRARY })
                Tab.LIBRARY -> LibraryScreen()
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun RailItem(selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null, modifier = Modifier.size(22.dp)) },
        label = { Text(label, fontSize = 9.sp, letterSpacing = 0.2.sp) },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = Neon,
            selectedTextColor = White,
            indicatorColor = Neon.copy(alpha = 0.17f),
            unselectedIconColor = Muted,
            unselectedTextColor = Muted
        )
    )
}

@Composable
private fun HomeScreen(onLibrary: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(start = 26.dp, end = 28.dp, top = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(Modifier.weight(1.35f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NEXUS", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("PSP  //  GAME SYSTEM", color = NeonBlue, fontSize = 9.sp, letterSpacing = 1.8.sp)
                }
                Row(
                    Modifier.background(Panel, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).background(NeonBlue, RoundedCornerShape(50)))
                    Spacer(Modifier.width(7.dp))
                    Text("SYSTEM ONLINE", color = NeonBlue, fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF241344), Color(0xFF171A32), Color(0xFF101827))
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .border(1.dp, Neon.copy(alpha = 0.32f), RoundedCornerShape(22.dp))
            ) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(18.dp)
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(listOf(Neon.copy(alpha = 0.30f), Color.Transparent)),
                            RoundedCornerShape(100.dp)
                        )
                )
                Column(
                    Modifier.align(Alignment.CenterStart).padding(horizontal = 25.dp, vertical = 20.dp)
                        .fillMaxWidth(0.84f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("YOUR NEXT LEVEL STARTS HERE", color = NeonBlue, fontSize = 9.sp, letterSpacing = 1.8.sp)
                    Spacer(Modifier.height(13.dp))
                    Text("ТВОЯ ИГРОВАЯ\nВСЕЛЕННАЯ.", color = White, fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Собери коллекцию. Выбери игру. Погрузись в приключение.",
                        color = Color(0xFFC3C5D8), fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(17.dp))
                    Button(
                        onClick = onLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = Neon),
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ОТКРЫТЬ БИБЛИОТЕКУ", fontSize = 10.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("N / 01", color = Color.White.copy(alpha = 0.18f), fontSize = 42.sp,
                    fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp))
            }
        }

        Column(Modifier.weight(0.9f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("КОЛЛЕКЦИЯ", color = Muted, fontSize = 10.sp, letterSpacing = 1.8.sp, modifier = Modifier.weight(1f))
                Text("0 GAMES", color = NeonBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                Modifier.fillMaxWidth().weight(1f)
                    .background(Panel, RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF292B40), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(54.dp).background(Neon.copy(alpha = 0.13f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SportsEsports, null, tint = Neon, modifier = Modifier.size(29.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("БИБЛИОТЕКА ПУСТА", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                Spacer(Modifier.height(5.dp))
                Text("Добавь первую игру,\nчтобы начать.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(13.dp))
                TextButton(onClick = onLibrary) {
                    Text("ДОБАВИТЬ ИГРЫ  +", color = NeonBlue, fontSize = 9.sp, letterSpacing = 0.8.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemTile("BUILD", "0.2.0", Modifier.weight(1f))
                SystemTile("PLATFORM", "ANDROID", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SystemTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Panel, RoundedCornerShape(13.dp))
            .border(1.dp, Color(0xFF292B40), RoundedCornerShape(13.dp))
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LibraryScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp)) {
        Text("GAME LIBRARY", color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
        Text("ТВОИ ИГРЫ", color = White, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.background(Panel, RoundedCornerShape(18.dp)).padding(horizontal = 28.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.LibraryAdd, null, tint = Neon, modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(18.dp))
                Column {
                    Text("ПОКА НЕТ ИГР", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Импорт ISO / CSO появится на следующем этапе.", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp)) {
        Text("SYSTEM CONFIGURATION", color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
        Text("НАСТРОЙКИ", color = White, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                SettingCard("Графика", "Разрешение и качество изображения", Icons.Rounded.Tune)
                SettingCard("Звук", "Аудио и задержка", Icons.Rounded.VolumeUp)
            }
            Column(Modifier.weight(1f)) {
                SettingCard("Управление", "Кнопки и подключённые геймпады", Icons.Rounded.Gamepad)
                SettingCard("О приложении", "NEXUS PSP · версия 0.2.0", Icons.Rounded.Info)
            }
        }
    }
}

@Composable
private fun SettingCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .background(Panel, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF292B40), RoundedCornerShape(14.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}
