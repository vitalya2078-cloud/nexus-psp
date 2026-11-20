package ru.kapsula.j2me;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * KAPSULA: время в Java-игре. Процесс игры при паузе добавляет минуты в last_session.json —
 * тот же файл, что пишет GameActivity; главный экран забирает его при возвращении (AppState.consumeSession).
 */
public final class Session {
	private static long carry; // доли минуты между паузами

	private Session() {}

	public static synchronized void add(Context c, String id, long startMs) {
		if (id == null || startMs <= 0) return;
		long ms = SystemClock.elapsedRealtime() - startMs + carry;
		if (ms <= 0) return;
		int min = (int) (ms / 60_000L);
		carry = ms % 60_000L;
		File f = new File(c.getFilesDir(), "last_session.json");
		try {
			int before = 0;
			if (f.exists()) {
				JSONObject old = new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
				if (id.equals(old.optString("id"))) before = old.optInt("minutes");
			}
			JSONObject o = new JSONObject().put("id", id).put("minutes", before + min).put("saves", 0);
			try (FileOutputStream os = new FileOutputStream(f)) { os.write(o.toString().getBytes(StandardCharsets.UTF_8)); }
		} catch (Exception e) {
			Log.w("KapsulaJ2me", "session", e);
		}
	}
}
