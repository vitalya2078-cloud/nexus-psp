package com.nexus.psp.ui

import androidx.compose.foundation.background
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

private val Bg = Color(0xFF101116)
private val Panel = Color(0xFF1B1D27)
private val Purple = Color(0xFF865CFF)
private val Cyan = Color(0xFF43C8FF)
private val Muted = Color(0xFF9295A5)

private enum class Tab(val label: String) {
    HOME("Главная"), LIBRARY("Игры"), SETTINGS("Настройки")
}

@Composable
fun NexusApp() {
    var selected by remember { mutableStateOf(Tab.HOME) }

    Row(Modifier.fillMaxSize().background(Bg)) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight().width(88.dp),
            containerColor = Panel,
            contentColor = Color.White,
            header = {
                Text("N", color = Purple, fontSize = 25.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 18.dp, bottom = 22.dp))
            }
        ) {
            NavigationRailItem(
                selected = selected == Tab.HOME,
                onClick = { selected = Tab.HOME },
                icon = { Icon(Icons.Rounded.Home, null) },
                label = { Text("Главная", fontSize = 10.sp) },
                colors = railColors()
            )
            NavigationRailItem(
                selected = selected == Tab.LIBRARY,
                onClick = { selected = Tab.LIBRARY },
                icon = { Icon(Icons.Rounded.SportsEsports, null) },
                label = { Text("Игры", fontSize = 10.sp) },
                colors = railColors()
            )
            NavigationRailItem(
                selected = selected == Tab.SETTINGS,
                onClick = { selected = Tab.SETTINGS },
                icon = { Icon(Icons.Rounded.Settings, null) },
                label = { Text("Настройки", fontSize = 10.sp) },
                colors = railColors()
            )
        }

        Box(Modifier.weight(1f).fillMaxHeight()) {
            when (selected) {
                Tab.HOME -> HomeScreen(onLibrary = { selected = Tab.LIBRARY })
                Tab.LIBRARY -> LibraryScreen()
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun railColors() = NavigationRailItemDefaults.colors(
    selectedIconColor = Purple,
    selectedTextColor = Color.White,
    indicatorColor = Purple.copy(alpha = 0.16f),
    unselectedIconColor = Muted,
    unselectedTextColor = Muted
)

@Composable
private fun HomeScreen(onLibrary: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column(Modifier.weight(1.15f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NEXUS", color = Color.White, fontSize = 27.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("PLAYSTATION PORTABLE EMULATOR", color = Muted,
                        fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = Panel) {
                    Icon(Icons.Rounded.AccountCircle, null, tint = Cyan,
                        modifier = Modifier.padding(10.dp).size(25.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF30245C), Color(0xFF182C42), Color(0xFF171923))),
                        RoundedCornerShape(26.dp)
                    ).padding(22.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                    Text("ВАША ИГРОВАЯ СИСТЕМА", color = Color.White, fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("Игры начинаются здесь.", color = Color.White, fontSize = 27.sp,
                    lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Добавьте свою коллекцию PSP и продолжайте приключения.",
                    color = Color(0xFFD0CBE5), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onLibrary, colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Открыть библиотеку")
                }
            }
        }

        Column(Modifier.weight(0.85f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("БЫСТРЫЙ ДОСТУП", color = Muted, fontSize = 11.sp,
                letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 5.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).background(Panel, RoundedCornerShape(20.dp)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.SportsEsports, null, tint = Purple, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(9.dp))
                Text("Пока здесь тихо", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(5.dp))
                Text("Ваша библиотека появится здесь после добавления игр.",
                    color = Muted, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoTile("ВЕРСИЯ", "0.1.0", Modifier.weight(1f))
                InfoTile("ПЛАТФОРМА", "ANDROID", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoTile(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(5.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LibraryScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp)) {
        Text("Библиотека", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("ВАША КОЛЛЕКЦИЯ PSP", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Icon(Icons.Rounded.LibraryAdd, null, tint = Purple, modifier = Modifier.size(58.dp))
                Column {
                    Text("Игры ещё не добавлены", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                    Text("Импорт игр добавим на следующем этапе.", color = Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 24.dp)) {
        Text("Настройки", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("НАСТРОЙКА NEXUS PSP", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f)) {
                SettingRow("Графика", "Разрешение и качество изображения", Icons.Rounded.Tune)
                SettingRow("Звук", "Аудио и задержка", Icons.Rounded.VolumeUp)
            }
            Column(Modifier.weight(1f)) {
                SettingRow("Управление", "Кнопки и подключённые геймпады", Icons.Rounded.Gamepad)
                SettingRow("О приложении", "NEXUS PSP · версия 0.1.0", Icons.Rounded.Info)
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .background(Panel, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}
