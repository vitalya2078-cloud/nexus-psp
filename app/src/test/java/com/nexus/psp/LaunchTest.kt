package com.nexus.psp

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Запуск настоящего MainActivity: ловит падения при старте приложения. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class LaunchTest {
    @Test fun mainActivityStarts() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup()
        c.pause().resume()
        c.get().window.decorView.rootView.draw(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(1688, 780, android.graphics.Bitmap.Config.ARGB_8888)))
        c.pause().stop().destroy()
    }
}
