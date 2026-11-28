package com.nexus.psp.admin

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Снимки вертикальной админки: вход и сводка (данные подменные, сервер не нужен). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-port-xhdpi")
class AdminShotsTest {
    init { System.setProperty("roborazzi.test.record", "true") }
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, logged: Boolean, tab: String = "sum") {
        St.startTab = tab
        Api.init(ApplicationProvider.getApplicationContext())
        Api.fake = { a ->
            when (a) {
                "summary" -> Api.Res(JSONObject("""{"players":128,"active_day":31,"active_week":77,"new_week":19,"banned":1,"tokens_given_week":5320,"tokens_spent_week":2100,
                    "versions":{"0.39.0":60,"0.40.0":68},"consoles":{"psp":42000,"ps1":9100,"nes":3300},"top_buys":[{"item":"av_dragon","n":12},{"item":"fr_crown","n":4}],
                    "daily":[{"day":"2026-11-10","players":20},{"day":"2026-11-11","players":26},{"day":"2026-11-12","players":22},{"day":"2026-11-13","players":31},{"day":"2026-11-14","players":28},{"day":"2026-11-15","players":31}]}"""), null)
                "push_list" -> Api.Res(JSONObject("""{"phones":3,"players_with_push":2,"msgs":[{"id":2,"title":"Тук-тук","tip":6,"at":"2026-11-28T10:00:00"},{"id":1,"title":"Выходные ×2","tip":null,"at":"2026-11-27T18:00:00"}],
                    "personal":[{"title":"Спасибо за отзыв!","to":"K7Q2","at":"2026-11-28T09:12:00"}]}"""), null)
                "tips_list" -> Api.Res(JSONObject("""{"cfg":{"enabled":true,"hours":[10,16,21],"last":"2026-11-28 10"},"tips":[{"id":1,"title":"Сейвик проголодался","body":"Миска пустая, а я смотрю на неё очень выразительно","active":true,"sent":1,"last":"2026-11-28T07:00:00"},
                    {"id":2,"title":"Время для RPG","body":"Чай, плед и сорок часов приключений впереди","active":false,"sent":0}]}"""), null)
                "gifts" -> Api.Res(JSONArray("""[{"code":"SEEKER","item":"tt_seeker","title":"Искатель секретов","active":true,"claims":4},{"code":"PIONEER","item":"fr_pioneer","title":"Первопроходец","active":false,"claims":0}]"""), null)
                "creator_set" -> Api.Res(JSONObject("""{"status":"Делаю KAPSULA","name":"gold","banner":"bn_creator"}"""), null)
                else -> Api.Res(JSONArray(), null)
            }
        }
        St.logged = logged; St.me = "KAPSULA"
        rule.setContent { AdminApp() }
        rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/admin-$name.png")
    }

    @Test fun login() = shot("login", false)
    @Test fun summary() = shot("summary", true)
    @Test fun myProfile() = shot("my-profile", true, "more")
    @Test fun pushes() = shot("pushes", true, "push")
    @Test fun news() = shot("news", true, "news")
}
