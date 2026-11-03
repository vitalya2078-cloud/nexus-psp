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
    HOME("Главная"), LIBRARY("Библиотека"), CATALOG("Каталог"),
    FAVORITES("Избранное"), DOWNLOADS("Загрузки"), SETTINGS("Настройки"),
    CONTROLLER("Геймпад"), NOTIFICATIONS("Уведомления"), PROFILE("Профиль")
}

@Composable
fun NexusApp() {
    var selected by remember { mutableStateOf(Section.HOME) }
    var search by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(true) }

    Row(Modifier.fillMaxSize().background(Bg)) {
        SideNavigation(
            expanded = expanded,
            selected = selected,
            onToggle = { expanded = !expanded },
            onSelect = { selected = it }
        )
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp)) {
            TopBar(
                search = search,
                onSearch = { search = it },
                onNotifications = { selected = Section.NOTIFICATIONS },
                onProfile = { selected = Section.PROFILE },
                onController = { selected = Section.CONTROLLER }
            )
            Spacer(Modifier.height(18.dp))
            when (selected) {
                Section.HOME -> HomeScreen()
                Section.LIBRARY -> LibraryScreen()
                Section.CATALOG -> ContentScreen("КАТАЛОГ ИГР", "ИГРЫ ДЛЯ СКАЧИВАНИЯ", "Каталог будет наполняться играми, доступными для законного распространения.", Icons.Rounded.Storefront)
                Section.FAVORITES -> ContentScreen("ИЗБРАННОЕ", "ТВОИ СОХРАНЁННЫЕ ИГРЫ", "Здесь будут отображаться игры, которые ты добавишь в избранное.", Icons.Rounded.Favorite)
                Section.DOWNLOADS -> ContentScreen("ЗАГРУЗКИ", "УПРАВЛЕНИЕ ЗАГРУЗКАМИ", "Здесь будут отображаться активные и завершённые загрузки.", Icons.Rounded.Download)
                Section.SETTINGS -> SettingsScreen { selected = Section.CONTROLLER }
                Section.CONTROLLER -> ContentScreen("УПРАВЛЕНИЕ", "НАСТРОЙКА ГЕЙМПАДА", "Настройка расположения, размера и прозрачности кнопок.", Icons.Rounded.Gamepad)
                Section.NOTIFICATIONS -> ContentScreen("ЦЕНТР СОБЫТИЙ", "УВЕДОМЛЕНИЯ", "Здесь будут обновления приложения и статусы загрузок.", Icons.Rounded.Notifications)
                Section.PROFILE -> ContentScreen("NEXUS ID", "ПРОФИЛЬ", "Настройки профиля и дополнительные возможности приложения.", Icons.Rounded.AccountCircle)
            }
        }
    }
}

@Composable
private fun SideNavigation(
    expanded: Boolean,
    selected: Section,
    onToggle: () -> Unit,
    onSelect: (Section) -> Unit
) {
    val navWidth = if (expanded) 184.dp else 64.dp
    Column(
        Modifier.width(navWidth).fillMaxHeight()
            .background(Brush.verticalGradient(listOf(Color(0xFF151322), Rail, Color(0xFF0D0E16))))
            .border(width = 1.dp, color = Neon.copy(alpha = 0.18f))
            .padding(horizontal = if (expanded) 9.dp else 7.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.SpaceBetween else Arrangement.Center) {
            if (expanded) {
                Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Color(0xFFB36BFF), Color(0xFF5424B8))), RoundedCornerShape(13.dp))
                    .border(1.dp, Color(0xFFD5A5FF).copy(alpha = 0.65f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center) {
                    Text("N", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(34.dp)) {
                Icon(if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                    contentDescription = if (expanded) "Свернуть меню" else "Развернуть меню",
                    tint = NeonBlue, modifier = Modifier.size(22.dp))
            }
        }
        if (!expanded) {
            Spacer(Modifier.height(13.dp))
            Box(Modifier.size(38.dp).background(Brush.linearGradient(listOf(Color(0xFFB36BFF), Color(0xFF5424B8))), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Text("N", color = White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(22.dp))
        SideNavItem(Section.HOME, selected, Icons.Rounded.Home, expanded, onSelect)
        SideNavItem(Section.LIBRARY, selected, Icons.Rounded.SportsEsports, expanded, onSelect)
        SideNavItem(Section.CATALOG, selected, Icons.Rounded.Storefront, expanded, onSelect)
        SideNavItem(Section.FAVORITES, selected, Icons.Rounded.Favorite, expanded, onSelect)
        SideNavItem(Section.DOWNLOADS, selected, Icons.Rounded.Download, expanded, onSelect)
        Spacer(Modifier.weight(1f))
        SideNavItem(Section.SETTINGS, selected, Icons.Rounded.Settings, expanded, onSelect)
    }
}

@Composable
private fun SideNavItem(
    section: Section,
    selected: Section,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onSelect: (Section) -> Unit
) {
    val active = section == selected
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(vertical = 2.dp)
            .background(
                if (active) Brush.horizontalGradient(listOf(Color(0xFF43256F), Color(0xFF211936)))
                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                RoundedCornerShape(11.dp)
            )
            .then(if (active) Modifier.border(1.dp, Neon.copy(alpha = 0.8f), RoundedCornerShape(11.dp)) else Modifier)
            .clickable { onSelect(section) }
            .padding(horizontal = if (expanded) 12.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
    ) {
        Icon(icon, contentDescription = section.title,
            tint = if (active) Neon else Muted, modifier = Modifier.size(21.dp))
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(section.title, color = if (active) White else Muted, fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun TopBar(
    search: String,
    onSearch: (String) -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onController: () -> Unit
) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).fillMaxHeight()
                .background(Panel, RoundedCornerShape(13.dp))
                .border(1.2.dp, NeonBlue.copy(alpha = 0.42f), RoundedCornerShape(13.dp))
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = NeonBlue, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            BasicTextField(
                value = search,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 13.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (search.isEmpty()) Text("Поиск игр и приложений...", color = Muted, fontSize = 13.sp)
                    inner()
                }
            )
        }
        Spacer(Modifier.width(10.dp))
        HeaderAction(Icons.Rounded.Gamepad, "Геймпад", onController, NeonBlue)
        HeaderAction(Icons.Rounded.Notifications, "Уведомления", onNotifications, Muted)
        HeaderAction(Icons.Rounded.AccountCircle, "Профиль", onProfile, Muted)
    }
}

@Composable
private fun HeaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color
) {
    Column(
        Modifier.padding(start = 5.dp).width(52.dp).fillMaxHeight()
            .background(Brush.verticalGradient(listOf(Color(0xFF20243A), Panel)), RoundedCornerShape(11.dp))
            .border(1.dp, tint.copy(alpha = 0.32f), RoundedCornerShape(11.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        Text(label, color = Muted, fontSize = 7.sp, maxLines = 1)
    }
}

@Composable
private fun HomeScreen() {
    Column(Modifier.fillMaxSize()) {
        Text("NEXUS  /  PSP", color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Text("ГЛАВНАЯ", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        // Главный экран остаётся свободным, пока в базу не добавлены реальные игры.
    }
}

@Composable
private fun LibraryScreen() {
    var filter by remember { mutableStateOf("Все игры") }

    Column(Modifier.fillMaxSize()) {
        Text("ТВОЯ КОЛЛЕКЦИЯ  /  NEXUS", color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("БИБЛИОТЕКА", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("0 ИГР", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Все игры", "Установленные", "Избранное").forEach { item ->
                LibraryFilter(item, selected = filter == item) { filter = item }
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(width = 66.dp, height = 3.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Neon, NeonBlue, Color.Transparent)))
                )
                Spacer(Modifier.height(22.dp))
                Text("ПОКА ПУСТО", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    "Добавленные игры появятся здесь",
                    color = Muted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun LibraryFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF43256F), Color(0xFF211936))) else Brush.verticalGradient(listOf(Panel, Panel)), RoundedCornerShape(10.dp))
            .then(
                if (selected) Modifier.border(1.dp, Neon.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                else Modifier.border(1.dp, Color(0xFF292B40), RoundedCornerShape(10.dp))
            )
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (selected) White else Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ContentScreen(
    kicker: String,
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(Modifier.fillMaxSize()) {
        Text(kicker, color = NeonBlue, fontSize = 9.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(5.dp))
        Text(title, color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.background(Panel, RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFF292B40), RoundedCornerShape(18.dp))
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
        Spacer(Modifier.height(5.dp))
        Text("НАСТРОЙКИ", color = White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                SettingCard("Графика", "Разрешение и качество изображения", Icons.Rounded.Tune)
                SettingCard("Звук", "Аудио и задержка", Icons.Rounded.VolumeUp)
            }
            Column(Modifier.weight(1f)) {
                SettingCard("Управление", "Кнопки и подключённые геймпады", Icons.Rounded.Gamepad, onController)
                SettingCard("О приложении", "NEXUS PSP · Cyber", Icons.Rounded.Info)
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .background(Panel, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF292B40), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
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
