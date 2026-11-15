package com.nexus.psp

import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Cos
import com.nexus.psp.data.Lib
import com.nexus.psp.data.Tokens
import com.nexus.psp.ui.kit.Accent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Оформление и жетоны: каталог, условия, начисления с лимитами, покупка, контраст акцента. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CosmeticsTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before fun clean() {
        ctx.getSharedPreferences("kapsula_tokens", 0).edit().clear().commit()
        AppState.autoFetch = false
        AppState.init(ctx)
        AppState.lib.clear()
        Tokens.init(ctx)
    }

    @Test fun catalogFilesExist() {
        val names = ctx.assets.list("img/look")!!.toSet()
        for (it in Cos.ITEMS) if (it.id.startsWith("av_") || it.id.startsWith("fr_") || it.id.startsWith("bn_") || it.id.startsWith("wp_"))
            assertTrue("нет картинки ${it.id}", "${it.id}.webp" in names)
        // у каждого вида есть что-то бесплатное — с первого запуска есть из чего выбрать
        Cos.Kind.entries.forEach { k -> assertTrue(k.name, Cos.of(k).any { it.free }) }
        // id предметов за жетоны уникальны (покупки хранятся по id)
        val paid = Cos.ITEMS.filter { it.price > 0 }.map { it.id }
        assertEquals(paid.size, paid.toSet().size)
    }

    @Test fun welcomeThenPlayWithDailyCap() {
        val first = Tokens.earn()
        assertTrue(first.any { it.first == "Добро пожаловать" && it.second == Tokens.WELCOME })
        val b0 = Tokens.balance
        // 1 ч игры = 4 × 15 мин = 20 жетонов
        AppState.lib["g1"] = Lib(play = 60)
        val got = Tokens.earn()
        assertEquals("first=$first got=$got", b0 + 20, Tokens.balance)
        // ещё 10 ч — но в день за игру не больше 60; плюс 100 за новый 2-й уровень (опыт = минуты × 2)
        AppState.lib["g1"] = Lib(play = 660)
        Tokens.earn()
        assertEquals(b0 + 60 + Tokens.LEVEL_UP, Tokens.balance)
        // повторный вызов без новой игры ничего не даёт
        val b1 = Tokens.balance
        assertTrue(Tokens.earn().isEmpty()); assertEquals(b1, Tokens.balance)
    }

    @Test fun buyAndUnlockByPlay() {
        Tokens.earn() // приветственные 100 + 10 за вход
        AppState.lib["g1"] = Lib(play = 180); Tokens.earn() // +60 за 3 ч
        val ninja = Cos.find(Cos.Kind.AV, "av_ninja")!!
        assertFalse(Cos.has(ninja))
        val start = Tokens.balance
        assertTrue("баланс $start", Tokens.buy(ninja)); assertTrue(Cos.has(ninja)); assertEquals(start - ninja.price, Tokens.balance)
        val dragon = Cos.find(Cos.Kind.AV, "av_dragon")!!
        assertFalse("не хватает — не покупается", Tokens.buy(dragon))
        // условие «за игру»: 5 ч — Сейвик-пилот
        val pilot = Cos.find(Cos.Kind.AV, "av_s_pilot")!!
        assertFalse(Cos.has(pilot))
        AppState.lib["g1"] = Lib(play = 300)
        assertTrue(Cos.has(pilot))
        assertFalse(Cos.has(Cos.find(Cos.Kind.AV, "av_s_king")!!))
    }

    /** Тесты не ходят на настоящий сервер (раньше автотест запуска создавал аккаунт). */
    @Test fun cloudOfflineInTests() {
        com.nexus.psp.data.Cloud.init(ctx)
        assertEquals(null, com.nexus.psp.data.Cloud.sync())
        assertEquals(true, com.nexus.psp.data.Cloud.restore("KPS-AAAA-AAAA-AAAA") != null)
    }

    @Test fun tokenWord() {
        assertEquals(listOf("жетонов", "жетон", "жетона", "жетона", "жетонов", "жетонов", "жетон", "жетона"), listOf(0, 1, 2, 4, 5, 11, 21, 102).map { Tokens.word(it) })
    }

    @Test fun accentAlwaysReadable() {
        val darkSurface = Color(0xFF131419); val lightSurface = Color(0xFFF1EFEB)
        for (c in listOf(Color(0xFF202040), Color(0xFFFFFF80), Color(0xFF808080), Color(0xFF3050FF)) + Accent.PRESETS.map { it.second }) {
            assertTrue(Accent.contrast(Accent.fit(c, darkSurface, false), darkSurface) >= 4.5f)
            assertTrue(Accent.contrast(Accent.fit(c, lightSurface, true), lightSurface) >= 4.5f)
        }
    }
}
