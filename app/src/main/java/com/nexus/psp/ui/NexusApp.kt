package com.nexus.psp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                NavigationBarItem(
                    selected = selected == Tab.HOME,
                    onClick = { selected = Tab.HOME },
                    icon = { Icon(Icons.Rounded.Home, null) },
                    label = { Text("Главная") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selected == Tab.LIBRARY,
                    onClick = { selected = Tab.LIBRARY },
                    icon = { Icon(Icons.Rounded.SportsEsports, null) },
                    label = { Text("Игры") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selected == Tab.SETTINGS,
                    onClick = { selected = Tab.SETTINGS },
                    icon = { Icon(Icons.Rounded.Settings, null) },
                    label = { Text("Настройки") },
                    colors = navColors()
                )
            }
        }
    ) { padding ->
        when (selected) {
            Tab.HOME -> HomeScreen(Modifier.padding(padding), onLibrary = { selected = Tab.LIBRARY })
            Tab.LIBRARY -> LibraryScreen(Modifier.padding(padding))
            Tab.SETTINGS -> SettingsScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Purple,
    selectedTextColor = Color.White,
    indicatorColor = Purple.copy(alpha = 0.16f),
    unselectedIconColor = Muted,
    unselectedTextColor = Muted
)

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onLibrary: () -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NEXUS", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Text("PLAYSTATION PORTABLE EMULATOR", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = Panel) {
                    Icon(Icons.Rounded.AccountCircle, null, tint = Cyan, modifier = Modifier.padding(11.dp).size(24.dp))
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF30245C), Color(0xFF182C42), Color(0xFF171923))),
                        RoundedCornerShape(26.dp)
                    )
                    .padding(22.dp)
            ) {
                Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                    Text("  ВАША ИГРОВАЯ СИСТЕМА  ", color = Color.White, fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(20.dp))
                Text("Игры начинаются\nздесь.", color = Color.White, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Добавьте свою коллекцию PSP и продолжайте приключения.", color = Color(0xFFD0CBE5), fontSize = 13.sp)
                Spacer(Modifier.height(22.dp))
                Button(onClick = onLibrary, colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Открыть библиотеку")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("БЫСТРЫЙ ДОСТУП", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f))
                Text("0 ИГР", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.SportsEsports, null, tint = Purple, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(10.dp))
                Text("Пока здесь тихо", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(5.dp))
                Text("Ваша библиотека появится здесь после добавления игр.", color = Muted, fontSize = 12.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoTile("ВЕРСИЯ", "0.1.0", Modifier.weight(1f))
                InfoTile("ПЛАТФОРМА", "ANDROID", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoTile(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(title, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LibraryScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Библиотека", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("ВАША КОЛЛЕКЦИЯ PSP", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.LibraryAdd, null, tint = Purple, modifier = Modifier.size(54.dp))
                Spacer(Modifier.height(12.dp))
                Text("Игры ещё не добавлены", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Импорт игр добавим на следующем этапе.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Настройки", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("НАСТРОЙКА NEXUS PSP", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(20.dp))
        SettingRow("Графика", "Разрешение и качество изображения", Icons.Rounded.Tune)
        SettingRow("Управление", "Кнопки и подключённые геймпады", Icons.Rounded.Gamepad)
        SettingRow("Звук", "Аудио и задержка", Icons.Rounded.VolumeUp)
        SettingRow("О приложении", "NEXUS PSP · версия 0.1.0", Icons.Rounded.Info)
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp).background(Panel, RoundedCornerShape(16.dp)).padding(16.dp),
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
