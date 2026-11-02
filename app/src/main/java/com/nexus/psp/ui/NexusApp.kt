package com.nexus.psp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
private val Neon = Color(0xFF9B5CFF)
private val NeonBlue = Color(0xFF35D7FF)
private val Muted = Color(0xFF85899F)
private val White = Color(0xFFF4F2FF)

private enum class Section(val title: String) {
    HOME("Главная"), LIBRARY("Библиотека"), CATALOG("Каталог игр"),
    FAVORITES("Избранное"), DOWNLOADS("Загрузки"), SETTINGS("Настройки"),
    CONTROLLER("Геймпад"), NOTIFICATIONS("Уведомления"), PROFILE("Профиль")
}

@Composable
fun NexusApp() {
    var selected by remember { mutableStateOf(Section.HOME) }
    var search by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.width(104.dp).fillMaxHeight().background(Rail).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.padding(top = 13.dp, bottom = 15.dp).size(42.dp)
                    .background(Brush.linearGradient(listOf(Neon, Color(0xFF5424B8))), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) { Text("N", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black) }

            SidebarItem(selected == Section.HOME, "Главная", Icons.Rounded.Home) { selected = Section.HOME }
            SidebarItem(selected == Section.LIBRARY, "Библиотека", Icons.Rounded.SportsEsports) { selected = Section.LIBRARY }
            SidebarItem(selected == Section.CATALOG, "Каталог", Icons.Rounded.Storefront) { selected = Section.CATALOG }
            SidebarItem(selected == Section.FAVORITES, "Избранное", Icons.Rounded.Favorite) { selected = Section.FAVORITES }
            SidebarItem(selected == Section.DOWNLOADS, "Загрузки", Icons.Rounded.Download) { selected = Section.DOWNLOADS }
            Spacer(Modifier.weight(1f))
            SidebarItem(selected == Section.SETTINGS, "Настройки", Icons.Rounded.Settings) { selected = Section.SETTINGS }
            SidebarItem(selected == Section.CONTROLLER, "Геймпад", Icons.Rounded.Gamepad) { selected = Section.CONTROLLER }
            Spacer(Modifier.height(8.dp))
        }

        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
            TopBar(search, { search = it }, { selected = Section.NOTIFICATIONS }, { selected = Section.PROFILE }, { selected = Section.CONTROLLER })
            Spacer(Modifier.height(14.dp))
            when (selected) {
                Section.HOME -> HomeScreen(search, { selected = Section.CATALOG }, { selected = Section.LIBRARY })
                Section.LIBRARY -> ContentScreen("БИБЛИОТЕКА", "ТВОЯ КОЛЛЕКЦИЯ", "Здесь будут игры, которые ты добавишь в свою библиотеку.", Icons.Rounded.SportsEsports)
                Section.CATALOG -> ContentScreen("КАТАЛОГ ИГР", "ИГРЫ ДЛЯ СКАЧИВАНИЯ", "Здесь появится каталог игр, опубликованных с разрешения правообладателей, homebrew и свободно распространяемых проектов.", Icons.Rounded.Storefront)
                Section.FAVORITES -> ContentScreen("ИЗБРАННОЕ", "ТВОИ СОХРАНЁННЫЕ ИГРЫ", "Добавляй игры в избранное, чтобы быстро находить их.", Icons.Rounded.Favorite)
                Section.DOWNLOADS -> ContentScreen("ЗАГРУЗКИ", "УПРАВЛЕНИЕ ЗАГРУЗКАМИ", "Здесь будут отображаться активные и завершённые загрузки.", Icons.Rounded.Download)
                Section.SETTINGS -> SettingsScreen { selected = Section.CONTROLLER }
                Section.CONTROLLER -> ContentScreen("НАСТРОЙКА ГЕЙМПАДА", "УПРАВЛЕНИЕ", "Настройка расположения, размера и прозрачности кнопок появится здесь.", Icons.Rounded.Gamepad)
                Section.NOTIFICATIONS -> ContentScreen("УВЕДОМЛЕНИЯ", "ЦЕНТР СОБЫТИЙ", "Здесь будут обновления приложения, новости каталога и статусы загрузок.", Icons.Rounded.Notifications)
                Section.PROFILE -> ContentScreen("ПРОФИЛЬ", "АККАУНТ NEXUS", "Здесь появятся настройки профиля и платных возможностей.", Icons.Rounded.AccountCircle)
            }
        }
    }
}

@Composable
private fun SidebarItem(selected: Boolean, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .background(if (selected) Neon.copy(alpha = 0.16f) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (selected) Neon else Muted, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (selected) White else Muted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun TopBar(
    search: String, onSearch: (String) -> Unit, onNotifications: () -> Unit,
    onProfile: () -> Unit, onController: () -> Unit
) {
    Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).fillMaxHeight().background(Panel, RoundedCornerShape(13.dp))
                .border(1.dp, Color(0xFF292B40), RoundedCornerShape(13.dp))
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = NeonBlue, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            BasicTextField(
                value = search, onValueChange = onSearch, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 13.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (search.isEmpty()) Text("Поиск игр и приложений...", color = Muted, fontSize = 13.sp)
                    inner()
                }
            )
        }
        Spacer(Modifier.width(10.dp))
        HeaderAction(Icons.Rounded.Gamepad, "Геймпад", onController)
        HeaderAction(Icons.Rounded.Notifications, "Уведомления", onNotifications)
        HeaderAction(Icons.Rounded.AccountCircle, "Профиль", onProfile)
    }
}

@Composable
private fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.padding(start = 5.dp).width(48.dp).fillMaxHeight()
            .background(Panel, RoundedCornerShape(11.dp)).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (label == "Геймпад") NeonBlue else Muted, modifier = Modifier.size(19.dp))
        Text(label, color = Muted, fontSize = 7.sp, maxLines = 1)
    }
}

@Composable
private fun HomeScreen(search: String, onCatalog: () -> Unit, onLibrary: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NEXUS // GAME HUB", color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
                Text("ИГРОВАЯ ВСЕЛЕННАЯ", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = onCatalog) { Text("ВЕСЬ КАТАЛОГ  →", color = NeonBlue, fontSize = 10.sp) }
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1.25f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.fillMaxWidth().weight(1f)
                        .background(Brush.linearGradient(listOf(Color(0xFF32145B), Color(0xFF171A32), Color(0xFF101827))), RoundedCornerShape(20.dp))
                        .border(1.dp, Neon.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(22.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(Modifier.fillMaxWidth(0.82f)) {
                        Text("DISCOVER • PLAY • REPEAT", color = NeonBlue, fontSize = 9.sp, letterSpacing = 1.8.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("ТВОИ ЛЮБИМЫЕ\nИГРЫ — В ОДНОМ МЕСТЕ", color = White, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("Открывай каталог, собирай коллекцию и возвращайся к приключениям.", color = Color(0xFFC3C5D8), fontSize = 12.sp)
                        Spacer(Modifier.height(13.dp))
                        Button(onClick = onCatalog, colors = ButtonDefaults.buttonColors(containerColor = Neon), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Rounded.Explore, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("ОТКРЫТЬ КАТАЛОГ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MiniAction("МОЯ БИБЛИОТЕКА", Icons.Rounded.SportsEsports, Modifier.weight(1f), onLibrary)
                    MiniAction("КАТАЛОГ ИГР", Icons.Rounded.Storefront, Modifier.weight(1f), onCatalog)
                }
            }
            Column(Modifier.weight(0.8f).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (search.isBlank()) "ПОПУЛЯРНОЕ" else "РЕЗУЛЬТАТЫ ПОИСКА", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                    Spacer(Modifier.weight(1f))
                    Text("0 ИГР", color = NeonBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().weight(1f).background(Panel, RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF292B40), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = Neon, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(9.dp))
                        Text("КАТАЛОГ ГОТОВИТСЯ", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Игры появятся здесь после\nдобавления в базу NEXUS.", color = Muted, fontSize = 11.sp)
                        TextButton(onClick = onCatalog) { Text("ПЕРЕЙТИ В КАТАЛОГ", color = NeonBlue, fontSize = 9.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(44.dp).background(Panel, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF292B40), RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ContentScreen(kicker: String, title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxSize()) {
        Text(kicker, color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
        Text(title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.background(Panel, RoundedCornerShape(18.dp)).border(1.dp, Color(0xFF292B40), RoundedCornerShape(18.dp))
                    .padding(horizontal = 25.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Neon, modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("РАЗДЕЛ В РАЗРАБОТКЕ", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(message, color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onController: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("SYSTEM CONFIGURATION", color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
        Text("НАСТРОЙКИ", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                SettingCard("Графика", "Разрешение и качество изображения", Icons.Rounded.Tune)
                SettingCard("Звук", "Аудио и задержка", Icons.Rounded.VolumeUp)
            }
            Column(Modifier.weight(1f)) {
                SettingCard("Управление", "Кнопки и подключённые геймпады", Icons.Rounded.Gamepad, onController)
                SettingCard("О приложении", "NEXUS PSP · Cyber Neon", Icons.Rounded.Info)
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Panel, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF292B40), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}
