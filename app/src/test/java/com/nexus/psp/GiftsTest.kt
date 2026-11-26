package com.nexus.psp

import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Cos
import com.nexus.psp.data.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 0.50: рамки-подарки открываются только подарком; «Что нового» — из общего с сайтом changes.json; статистика — по согласию. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GiftsTest {
    private fun init() { AppState.autoFetch = false; AppState.init(ApplicationProvider.getApplicationContext()) }

    @Test fun giftFramesOnlyByGift() {
        init()
        val ids = listOf("fr_pioneer", "fr_winter", "fr_pumpkin", "fr_birthday")
        ids.forEach { id ->
            val it = Cos.find(Cos.Kind.FR, id)!!
            assertEquals("gift", it.need?.what); assertEquals(0, it.price)
            assertFalse("без подарка $id закрыта", Cos.has(it))
            assertTrue(Cos.visible(it))
        }
        Tokens.fromServer(0, setOf("fr_pioneer"), 0)
        assertTrue(Cos.has(Cos.find(Cos.Kind.FR, "fr_pioneer")!!))
        assertFalse(Cos.has(Cos.find(Cos.Kind.FR, "fr_winter")!!))
        Tokens.fromServer(0, emptySet(), 0)
    }

    @Test fun giftAssetsExist() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf("look/fr_pioneer.webp", "look/fr_winter.webp", "look/fr_pumpkin.webp", "look/fr_birthday.webp",
            "mascot-l-gift.webp", "mascot-l-gift-open.webp", "mascot-l-bye.webp", "mascot-l-checklist.webp", "mascot-l-letter.webp",
            "mascot-l-scroll.webp", "mascot-l-welcome.webp", "mascot-l-cartbox.webp", "mascot-l-chip.webp", "mascot-l-gamepad.webp").forEach {
            ctx.assets.open("img/$it").use { s -> assertTrue("$it пустой", s.read() >= 0) }
        }
    }

    @Test fun whatsNewForThisVersion() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val items = com.nexus.psp.ui.screens.changesFor(ctx, BuildConfig.VERSION_NAME)
        assertNotNull("в site/changes.json нет пунктов для ${BuildConfig.VERSION_NAME}", items)
        assertTrue(items!!.isNotEmpty())
    }

    @Test fun statsOffUntilAsked() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("kapsula", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        init()
        assertFalse(AppState.S.stats); assertFalse(AppState.S.statsAsked)
    }
}
