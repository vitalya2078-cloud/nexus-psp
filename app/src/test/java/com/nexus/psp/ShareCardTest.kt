package com.nexus.psp

import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.data.AppState
import com.nexus.psp.ui.screens.Stats
import com.nexus.psp.ui.screens.drawShareCard
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Карточка профиля для «Поделиться» рисуется без ошибок и сохраняется картинкой. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ShareCardTest {
    @Test fun drawsCard() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        AppState.autoFetch = false
        AppState.init(ctx)
        AppState.S.nick = "Виталий"; AppState.S.motto = "Пройду всё на 100%"
        val f = drawShareCard(ctx, Stats())
        assertTrue(f.exists() && f.length() > 10_000)
        f.copyTo(java.io.File("build/shots/k01-share-card.png"), overwrite = true)
    }
}
