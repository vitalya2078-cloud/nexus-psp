package com.nexus.psp.ui

import android.app.Activity
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.emulator.GameActivity
import org.json.JSONObject

/** Слои поверх вкладок (как hash-маршруты прототипа: #game-id, #library, #search…). */
sealed interface Ov {
    data class Game(val id: String) : Ov
    data class GSet(val id: String) : Ov
    data object Library : Ov
    data object Search : Ov
    data object Downloads : Ov
    data object Alerts : Ov
    data object Quick : Ov
    data object Settings : Ov
    data object Profile : Ov
    data object Pad : Ov
}

object Nav {
    var tab by mutableStateOf("games")
    val stack = mutableStateListOf<Ov>()
    val top get() = stack.lastOrNull()
    var settingsTab by mutableStateOf("general")

    fun open(o: Ov) {
        if (stack.lastOrNull() == o) return
        // одинаковые панели не копятся: открытие уже открытого слоя поднимает его наверх
        stack.remove(o); stack.add(o)
    }
    fun replace(o: Ov) { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex); open(o) }
    fun back(): Boolean { if (stack.isEmpty()) return false; stack.removeAt(stack.lastIndex); return true }
    fun closeAll() = stack.clear()
    fun go(t: String) { stack.clear(); tab = t }
}

/** Действия, которым нужен Android: запуск игры, вибрация. */
object Act {
    var view: View? = null
    var pickFile: (() -> Unit)? = null
    var pickFolder: (() -> Unit)? = null
    /** Скопировать найденные в папке игры по очереди. */
    var importAll: ((List<com.nexus.psp.data.Found>) -> Unit)? = null
    var pickCover: ((String) -> Unit)? = null
    var ensureNotify: (((Boolean) -> Unit) -> Unit)? = null
    var openNotifySettings: (() -> Unit)? = null

    fun buzz() { if (S.vib) view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    fun buzzLong() { if (S.vib) view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

    /** Играть: образ запускается в ядре в отдельном процессе (GameActivity). slot — сразу загрузить сохранение. */
    fun play(ctx: Context, id: String, slot: Int = -1) {
        val g = AppState.byId[id] ?: return
        buzz()
        AppState.markPlayed(id)
        // мгновенное продолжение: без выбранного слота — с места, где вышел
        val s = if (slot < 0 && S.autoResume && AppState.stateFile(g, 4).exists()) 4 else slot
        GameActivity.start(ctx, bootJson(ctx, id, s).toString())
    }

    /** Настройки для запуска: общие + свои для этой игры (если включены). */
    fun bootJson(ctx: Context, id: String, slot: Int = -1): JSONObject {
        val g = AppState.byId[id]!!
        val own = com.nexus.psp.data.GameConfig(ctx, id).read().takeIf { it.optBoolean("own") } ?: JSONObject()
        val vars = try { JSONObject(S.coreVars) } catch (e: Exception) { JSONObject() }
        own.optJSONObject("vars")?.let { o -> o.keys().forEach { k -> vars.put(k, o.get(k)) } }
        val kp = try { JSONObject(S.keyPos) } catch (e: Exception) { JSONObject() }
        own.optJSONObject("keyPos")?.let { o -> o.keys().forEach { k -> kp.put(k, o.get(k)) } }
        // значки и турбо: у игры со своими настройками — её набор целиком, иначе общий
        val ki = own.optJSONObject("keyIcon") ?: try { JSONObject(S.keyIcon) } catch (e: Exception) { JSONObject() }
        val kt = own.optJSONObject("keyTurbo") ?: try { JSONObject(S.keyTurbo) } catch (e: Exception) { JSONObject() }
        val kx = own.optJSONObject("extraBtns") ?: try { JSONObject(S.extraBtns) } catch (e: Exception) { JSONObject() }
        val s = JSONObject().put("ctlMode", own.optString("ctlMode", S.ctlMode)).put("op", own.optInt("op", S.op)).put("sc", own.optInt("sc", S.sc))
            .put("mirror", own.optBoolean("mirror", S.mirror)).put("vib", S.vib).put("hide", S.hide).put("resume", S.autoResume).put("fps", own.optBoolean("fps", S.fps)).put("keyPos", kp).put("keyIcon", ki).put("keyTurbo", kt).put("extraBtns", kx)
        val boot = JSONObject().put("id", id).put("name", g.t).put("plat", g.p).put("size", g.size).put("path", g.path).put("s", s).put("vars", vars)
            .put("own", own.optBoolean("own", false)).put("preset", if (own.optBoolean("own")) own.optString("preset", "custom") else S.corePreset)
        AppState.meta[id]?.backdrop?.let { boot.put("bg", it.path) }
        if (slot >= 0) boot.put("loadSlot", slot)
        return boot
    }

    fun addFile() { pickFile?.invoke() ?: AppState.say("Выбор файла недоступен") }
    fun findGames() { pickFolder?.invoke() ?: AppState.say("Выбор папки недоступен") }

    fun icon(name: String): Int = when (name) {
        "check" -> R.drawable.ph_check; "x" -> R.drawable.ph_x; "plus" -> R.drawable.ph_plus
        else -> R.drawable.sl_info
    }

    fun finish(a: Activity) = a.finish()
}
