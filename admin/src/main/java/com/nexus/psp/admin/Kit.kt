package com.nexus.psp.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Палитра и шрифты — как у KAPSULA (графит + слоновая кость), акцент админки — золото. */
object C {
    val bg = Color(0xFF0B0C0F); val s1 = Color(0xFF131419); val s2 = Color(0xFF1A1C22); val s3 = Color(0xFF23262E)
    val ink = Color(0xFFF2F1EC); val ink2 = ink.copy(alpha = .74f); val ink3 = ink.copy(alpha = .58f); val line = Color(0x14FFFFFF)
    val gold = Color(0xFFD9B36C); val gold2 = Color(0xFFF6D98A); val blue = Color(0xFF7FA7D9); val ok = Color(0xFF8FC9A3); val bad = Color(0xFFD98A8A)
}

object F {
    val rubik = FontFamily(Font(R.font.rubik_semibold, FontWeight.SemiBold), Font(R.font.rubik_bold, FontWeight.Bold))
    val body = FontFamily(Font(R.font.manrope))
    val h1 = TextStyle(fontFamily = rubik, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = C.ink)
    val h2 = TextStyle(fontFamily = rubik, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = C.ink)
    val label = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.ink)
    val text = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = C.ink2, lineHeight = 20.sp)
    val small = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = C.ink3)
    val caps = TextStyle(fontFamily = rubik, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = C.ink3, letterSpacing = 1.5.sp)
    val num = TextStyle(fontFamily = rubik, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = C.ink)
}

@Composable
fun Caps(t: String, m: Modifier = Modifier) = Text(t.uppercase(), style = F.caps, modifier = m.padding(top = 14.dp, bottom = 6.dp))

/** Кнопка: main — светлая (главное действие), gold — золотая, danger — красная, иначе — тёмная. */
@Composable
fun Btn(text: String, modifier: Modifier = Modifier, main: Boolean = false, gold: Boolean = false, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = when { !enabled -> C.s2; gold -> C.gold; main -> C.ink; danger -> C.bad.copy(alpha = .16f); else -> C.s2 }
    val fg = when { !enabled -> C.ink3; gold || main -> C.bg; danger -> C.bad; else -> C.ink }
    Box(
        modifier.height(46.dp).clip(RoundedCornerShape(23.dp)).background(bg).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = F.label.copy(color = fg, fontSize = 14.sp)) }
}

/** Поле ввода в стиле KAPSULA. */
@Composable
fun Field(value: String, hint: String, modifier: Modifier = Modifier, secret: Boolean = false, digits: Boolean = false, onChange: (String) -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(16.dp)).background(C.s1).border(1.dp, C.line, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(hint, style = F.text.copy(color = C.ink3))
        BasicTextField(
            value, onChange, singleLine = true, textStyle = F.label, cursorBrush = SolidColor(C.gold), modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (digits) KeyboardType.NumberPassword else KeyboardType.Text),
        )
    }
}

/** Плашка-карточка. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(C.s1).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
}

/** Цифра сводки: значение и подпись. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, gold: Boolean = false) {
    Card(modifier) {
        Text(value, style = F.num.copy(color = if (gold) C.gold2 else C.ink))
        Text(label, style = F.small)
    }
}

/** Строка «подпись — значение». */
@Composable
fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, style = F.text); Text(v, style = F.label.copy(fontSize = 14.sp))
    }
}

/** Бирка: например «Заблокирован», «Админ». */
@Composable
fun Tag(t: String, color: Color) {
    Text(t, style = F.small.copy(color = color, fontSize = 11.sp), modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = .14f)).padding(horizontal = 7.dp, vertical = 2.dp))
}
