package com.nexus.psp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.Pet
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Balsamiq
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.LiveMascot
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.delay

/**
 * Уголок Сейвика: та же комната с лампой, коврик, корзинка, миска и игрушки. Сверху — четыре потребности,
 * снизу — четыре действия. Сейвик показывает, чего ему не хватает, и радуется заботе.
 */
@Composable
fun PetScreen() {
    LaunchedEffect(Unit) { Pet.update() }
    @Suppress("UNUSED_VARIABLE") val t = Pet.tick // перерисовка после действий
    var act by remember { mutableStateOf<String?>(null) }   // поза действия на пару секунд
    var say by remember { mutableStateOf<String?>(Pet.need() ?: "Привет! Я тут, в своём уголке") }
    LaunchedEffect(act) { if (act != null) { delay(1800); act = null } }
    LaunchedEffect(say) { if (say != null) { delay(4000); say = null } }
    fun go(pose: String, text: String) { act = pose; say = text; com.nexus.psp.ui.Act.buzz() }

    Box(Modifier.fillMaxSize().background(K.bg).press { }) {
        AssetImage("look/wp_room.webp", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .18f)))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val big = minOf(maxHeight * .46f, 260.dp)
            // пол: коврик, корзинка слева, миска справа, растение у стены
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 74.dp)) {
                AssetImage("pet/rug.webp", Modifier.width(big * 1.5f).offset(y = big * .08f))
            }
            AssetImage("pet/bed.webp", Modifier.align(Alignment.BottomCenter).offset(x = -big * 1.15f, y = -82.dp).width(big * .62f))
            AssetImage(if (Pet.food > 60) "pet/bowlfull.webp" else "pet/bowl.webp", Modifier.align(Alignment.BottomCenter).offset(x = big * 1.05f, y = -84.dp).width(big * .42f))
            AssetImage("pet/carttoy.webp", Modifier.align(Alignment.BottomCenter).offset(x = big * .7f, y = -76.dp).width(big * .26f))
            AssetImage("pet/plant.webp", Modifier.align(Alignment.BottomEnd).offset(x = -24.dp, y = -96.dp).width(big * .48f))
            // Сейвик и облачко
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(say != null, enter = fadeIn(tween(250)), exit = fadeOut(tween(300))) {
                    Text(say ?: "", maxLines = 2, style = T.label.copy(fontFamily = Balsamiq, fontSize = 15.sp, color = Color(0xFF3A2A20)),
                        modifier = Modifier.widthIn(max = 260.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFBF0E4)).padding(horizontal = 14.dp, vertical = 8.dp))
                }
                LiveMascot(act ?: Pet.pose(), big) { if (!Pet.asleep) say = listOf("Хи-хи!", "Щекотно!", "Я тебя тоже люблю", "Поиграем?").random() }
            }
        }
        // потребности
        Column(Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 18.dp).clip(RoundedCornerShape(18.dp)).background(K.s1.copy(alpha = .86f))
            .border(1.dp, K.acc.copy(alpha = .2f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Сейвик", style = T.h3.copy(fontFamily = Balsamiq))
            NeedBar("pet/bowlfull.webp", "Сытость", Pet.food)
            NeedBar("pet/bed.webp", "Бодрость", Pet.energy)
            NeedBar("pet/tub.webp", "Чистота", Pet.clean)
            NeedBar("pet/carttoy.webp", "Настроение", Pet.fun_)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(18.dp).size(42.dp).clip(CircleShape).background(K.s2.copy(alpha = .9f)).press { Nav.back() }, contentAlignment = Alignment.Center) {
            SolarIcon(R.drawable.ph_x, 18.dp)
        }
        // действия
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PetAction("pet/bowlfull.webp", "Покормить") { go("eat", Pet.feed()) }
            PetAction("pet/bed.webp", if (Pet.asleep) "Разбудить" else "Уложить") { val s = Pet.sleep(); go(if (Pet.asleep) "bed" else "stretch", s) }
            PetAction("pet/tub.webp", "Помыть") { go("bath", Pet.wash()) }
            PetAction("pet/carttoy.webp", "Поиграть") { val s = Pet.play(); go(if (Pet.energy < 20f) "yawn" else listOf("toy", "tickle", "joy").random(), s) }
        }
    }
}

@Composable
private fun NeedBar(icon: String, title: String, v: Float) {
    val p by animateFloatAsState(v / 100f, tween(500), label = "need")
    val c = when { v < 30 -> Color(0xFFE88A7A); v < 60 -> Color(0xFFF4B26B); else -> Color(0xFF9FD6B4) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssetImage(icon, Modifier.size(24.dp))
        Column {
            Text(title, style = T.small.copy(fontSize = 11.sp))
            Box(Modifier.width(120.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(K.s3)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(4.dp)).background(c))
            }
        }
    }
}

@Composable
private fun PetAction(icon: String, title: String, onClick: () -> Unit) {
    Column(
        Modifier.width(96.dp).clip(RoundedCornerShape(18.dp)).background(K.s1.copy(alpha = .9f)).border(1.dp, K.acc.copy(alpha = .25f), RoundedCornerShape(18.dp))
            .press(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AssetImage(icon, Modifier.size(34.dp))
        Text(title, style = T.label.copy(fontFamily = Balsamiq, fontSize = 13.sp))
    }
}

@Suppress("unused") private fun Dp.half() = this / 2
