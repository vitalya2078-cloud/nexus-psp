package javax.microedition.lcdui.keyboard;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.microedition.lcdui.graphics.CanvasWrapper;
import javax.microedition.util.ContextHolder;

import ru.playsoftware.j2meloader.config.ProfileModel;

/**
 * KAPSULA: клавиатура кнопочного телефона под Java-игрой (по макету): сверху левая софт-клавиша, крестовина с OK,
 * правая софт-клавиша, под софт-клавишами — зелёная и красная трубки; ниже 12 кнопок 1–9, *, 0, # с буквами.
 * Подменяет экранную клавиатуру J2ME Loader целиком (раскладка, рисовка, касания); для движка — «телефонная» (isPhone).
 * Нажатия уходят в игру как обычные клавиши: postKeyPressed / postKeyReleased.
 */
public class KapsulaKeypad extends VirtualKeyboard {
	private static final int BG = 0xFF0E1015, KEY = 0xFF2A2E37, KEY_TOP = 0xFF353A45, KEY_LINE = 0xFF3A3F4B;
	private static final int INK = 0xFFF2F1EC, INK2 = 0xFF9AA0AD, ACC = 0xFF7FA7D9, ACC_DARK = 0xFF5B82B5;
	private static final int GREEN = 0xFF5BC46A, RED = 0xFFE5533D;
	private static final String[] DIGITS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
	private static final String[] LETTERS = {"", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz", "", "", ""};
	private static final int[] CODES = {
			javax.microedition.lcdui.Canvas.KEY_NUM1, javax.microedition.lcdui.Canvas.KEY_NUM2, javax.microedition.lcdui.Canvas.KEY_NUM3,
			javax.microedition.lcdui.Canvas.KEY_NUM4, javax.microedition.lcdui.Canvas.KEY_NUM5, javax.microedition.lcdui.Canvas.KEY_NUM6,
			javax.microedition.lcdui.Canvas.KEY_NUM7, javax.microedition.lcdui.Canvas.KEY_NUM8, javax.microedition.lcdui.Canvas.KEY_NUM9,
			javax.microedition.lcdui.Canvas.KEY_STAR, javax.microedition.lcdui.Canvas.KEY_NUM0, javax.microedition.lcdui.Canvas.KEY_POUND};

	/** Кнопка: прямоугольник, код, вид (digit / soft / call / end). */
	private static final class Key {
		final RectF r = new RectF(); final int code; final String kind; final String label; final String sub;
		Key(int code, String kind, String label, String sub) { this.code = code; this.kind = kind; this.label = label; this.sub = sub; }
	}

	private final ArrayList<Key> keys = new ArrayList<>();
	private final RectF pad = new RectF();
	private final RectF panel = new RectF();
	private final Set<Integer> down = new HashSet<>();
	@SuppressWarnings("unchecked")
	private final Set<Integer>[] byPointer = new Set[10];
	private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Path path = new Path();
	private final boolean feedback;
	private javax.microedition.lcdui.Canvas target;
	private View view;

	public KapsulaKeypad(ProfileModel settings) {
		super(settings);
		feedback = settings.vkFeedback;
		for (int i = 0; i < byPointer.length; i++) byPointer[i] = new HashSet<>();
		stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeCap(Paint.Cap.ROUND); stroke.setStrokeJoin(Paint.Join.ROUND);
		text.setTextAlign(Paint.Align.CENTER); text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		keys.add(new Key(javax.microedition.lcdui.Canvas.KEY_SOFT_LEFT, "soft", null, null));
		keys.add(new Key(javax.microedition.lcdui.Canvas.KEY_SOFT_RIGHT, "soft", null, null));
		keys.add(new Key(javax.microedition.lcdui.Canvas.KEY_SEND, "call", null, null));
		keys.add(new Key(javax.microedition.lcdui.Canvas.KEY_END, "end", null, null));
		for (int i = 0; i < 12; i++) keys.add(new Key(CODES[i], "digit", DIGITS[i], LETTERS[i]));
	}

	@Override public boolean isPhone() { return true; }

	/** Высота клавиатуры: игра 3:4 занимает ширину экрана, остальное — кнопки (но не меньше 38% и не больше 52% высоты). */
	@Override public float getPhoneKeyboardHeight(float w, float h) {
		float min = Math.min(w, h), max = Math.max(w, h);
		float rest = max - min * 4f / 3f;
		return Math.max(max * 0.38f, Math.min(max * 0.52f, rest));
	}

	@Override public void setTarget(javax.microedition.lcdui.Canvas canvas) { target = canvas; }
	@Override public void setView(View v) { view = v; super.setView(v); }

	@Override public void resize(RectF screen, float left, float top, float right, float bottom) {
		float w = screen.width(), h = screen.height();
		float kh = getPhoneKeyboardHeight(w, h);
		panel.set(screen.left, screen.bottom - kh, screen.right, screen.bottom);
		float p = w * 0.04f, gap = w * 0.03f;
		float x0 = panel.left + p, x1 = panel.right - p, y = panel.top + p;
		float avail = kh - 2 * p;
		float d = Math.min(w * 0.36f, avail * 0.36f); // крестовина
		pad.set(panel.centerX() - d / 2, y, panel.centerX() + d / 2, y + d);
		float sideW = (pad.left - gap) - x0, rowH = (d - gap) / 2;
		keys.get(0).r.set(x0, y, x0 + sideW, y + rowH * 0.8f);
		keys.get(1).r.set(pad.right + gap, y, x1, y + rowH * 0.8f);
		keys.get(2).r.set(x0, y + rowH + gap, x0 + sideW, y + d);
		keys.get(3).r.set(pad.right + gap, y + rowH + gap, x1, y + d);
		float gy = y + d + gap, gh = (panel.bottom - p - gy - 3 * gap) / 4, gw = (x1 - x0 - 2 * gap) / 3;
		for (int i = 0; i < 12; i++) {
			int row = i / 3, col = i % 3;
			float kx = x0 + col * (gw + gap), ky = gy + row * (gh + gap);
			keys.get(4 + i).r.set(kx, ky, kx + gw, ky + gh);
		}
		if (view != null) view.postInvalidate();
	}

	// ---------- касания ----------
	/** Какие клавиши под пальцем: кнопка или направление крестовины (по диагонали — две). */
	private Set<Integer> at(float x, float y) {
		Set<Integer> s = new HashSet<>();
		float cx = pad.centerX(), cy = pad.centerY(), r = pad.width() / 2, dx = x - cx, dy = y - cy;
		double dist = Math.hypot(dx, dy);
		if (dist <= r * 1.08f) {
			if (dist < r * 0.36f) { s.add(javax.microedition.lcdui.Canvas.KEY_FIRE); return s; }
			double a = Math.toDegrees(Math.atan2(-dy, dx)); if (a < 0) a += 360;
			int sector = (int) Math.round(a / 45.0) % 8; // 0 — вправо, 2 — вверх…
			if (sector == 7 || sector == 0 || sector == 1) s.add(javax.microedition.lcdui.Canvas.KEY_RIGHT);
			if (sector >= 1 && sector <= 3) s.add(javax.microedition.lcdui.Canvas.KEY_UP);
			if (sector >= 3 && sector <= 5) s.add(javax.microedition.lcdui.Canvas.KEY_LEFT);
			if (sector >= 5 && sector <= 7) s.add(javax.microedition.lcdui.Canvas.KEY_DOWN);
			return s;
		}
		for (Key k : keys) if (k.r.contains(x, y)) { s.add(k.code); break; }
		return s;
	}

	private void set(int pointer, Set<Integer> now) {
		if (pointer < 0 || pointer >= byPointer.length) return;
		Set<Integer> was = byPointer[pointer];
		boolean changed = false;
		for (Integer c : new ArrayList<>(was)) if (!now.contains(c)) { was.remove(c); release(c); changed = true; }
		for (Integer c : now) if (was.add(c)) { press(c); changed = true; }
		if (changed && view != null) view.postInvalidate();
	}

	private boolean heldByOther(int code) { int n = 0; for (Set<Integer> s : byPointer) if (s.contains(code)) n++; return n > 0; }

	private void press(int code) {
		if (down.add(code)) {
			if (feedback) ContextHolder.vibrateKey(25);
			if (target != null) target.postKeyPressed(code);
		}
	}

	private void release(int code) {
		if (!heldByOther(code) && down.remove(code) && target != null) target.postKeyReleased(code);
	}

	@Override public boolean pointerPressed(int pointer, float x, float y) {
		if (!panel.contains(x, y)) return false;
		set(pointer, at(x, y));
		return true;
	}

	@Override public boolean pointerDragged(int pointer, float x, float y) {
		if (pointer < 0 || pointer >= byPointer.length) return false;
		if (byPointer[pointer].isEmpty() && !panel.contains(x, y)) return false;
		set(pointer, panel.contains(x, y) ? at(x, y) : new HashSet<>());
		return true;
	}

	@Override public boolean pointerReleased(int pointer, float x, float y) {
		if (pointer < 0 || pointer >= byPointer.length) return false;
		boolean had = !byPointer[pointer].isEmpty();
		set(pointer, new HashSet<>());
		return had;
	}

	@Override public void cancel() {
		for (Set<Integer> s : byPointer) s.clear();
		for (Integer c : new ArrayList<>(down)) { down.remove(c); if (target != null) target.postKeyReleased(c); }
		if (view != null) view.postInvalidate();
	}

	@Override public void show() { }
	@Override public void hide() { }
	@Override public boolean keyPressed(int keyCode) { return false; }
	@Override public boolean keyRepeated(int keyCode) { return false; }
	@Override public boolean keyReleased(int keyCode) { return false; }
	@Override public int getLayoutEditMode() { return LAYOUT_EOF; }

	/** Состояние клавиш для Vodafone API: как у оригинала — только направления и OK. */
	@Override public int getKeyStatesVodafone() { return 0; }

	// ---------- рисовка ----------
	@Override public void paint(CanvasWrapper g) {
		Canvas c = g.raw();
		if (c == null || panel.isEmpty()) return;
		fill.setShader(null); fill.setColor(BG); c.drawRect(panel, fill);
		for (Key k : keys) key(c, k);
		dpad(c);
	}

	private void key(Canvas c, Key k) {
		boolean on = down.contains(k.code);
		RectF r = k.r; float rad = Math.min(r.width(), r.height()) * 0.42f;
		if (k.kind.equals("soft")) {
			fill.setShader(new LinearGradient(0, r.top, 0, r.bottom, on ? ACC_DARK : 0xFF9CBDE4, on ? 0xFF4A6D9C : ACC, Shader.TileMode.CLAMP));
			c.drawRoundRect(r, r.height() / 2, r.height() / 2, fill); fill.setShader(null);
			return;
		}
		fill.setShader(new LinearGradient(0, r.top, 0, r.bottom, on ? ACC_DARK : KEY_TOP, on ? 0xFF3D5A80 : KEY, Shader.TileMode.CLAMP));
		c.drawRoundRect(r, rad, rad, fill); fill.setShader(null);
		stroke.setColor(KEY_LINE); stroke.setStrokeWidth(Math.max(1f, r.height() * 0.02f));
		c.drawRoundRect(r, rad, rad, stroke);
		if (k.kind.equals("call") || k.kind.equals("end")) { handset(c, r, k.kind.equals("call")); return; }
		boolean hasSub = k.sub != null && !k.sub.isEmpty();
		float big = r.height() * (k.label.equals("*") ? 0.55f : 0.44f);
		text.setColor(INK); text.setTextSize(big);
		float cy = hasSub ? r.top + r.height() * 0.52f : r.centerY() - (text.ascent() + text.descent()) / 2;
		c.drawText(k.label, r.centerX(), cy, text);
		if (hasSub) { text.setColor(INK2); text.setTextSize(r.height() * 0.2f); c.drawText(k.sub, r.centerX(), r.top + r.height() * 0.82f, text); }
	}

	/** Трубка: дуга с утолщениями на концах; зелёная — под наклоном, красная — «положена». */
	private void handset(Canvas c, RectF r, boolean call) {
		float s = Math.min(r.width(), r.height()) * 0.55f, cx = r.centerX(), cy = r.centerY();
		c.save(); c.rotate(call ? -35 : 180, cx, cy + (call ? 0 : s * 0.05f));
		stroke.setColor(call ? GREEN : RED); stroke.setStrokeWidth(s * 0.2f);
		RectF arc = new RectF(cx - s / 2, cy - s * 0.15f, cx + s / 2, cy + s * 0.85f);
		c.drawArc(arc, 200, 140, false, stroke);
		fill.setColor(call ? GREEN : RED);
		float e = s * 0.17f;
		for (int i = 0; i < 2; i++) {
			double a = Math.toRadians(i == 0 ? 200 : 340);
			float ex = (float) (arc.centerX() + arc.width() / 2 * Math.cos(a)), ey = (float) (arc.centerY() + arc.height() / 2 * Math.sin(a));
			c.drawRoundRect(new RectF(ex - e, ey - e * 0.6f, ex + e, ey + e * 1.1f), e * 0.5f, e * 0.5f, fill);
		}
		c.restore();
	}

	private void dpad(Canvas c) {
		float cx = pad.centerX(), cy = pad.centerY(), r = pad.width() / 2;
		fill.setShader(new LinearGradient(0, pad.top, 0, pad.bottom, KEY_TOP, KEY, Shader.TileMode.CLAMP));
		c.drawCircle(cx, cy, r, fill); fill.setShader(null);
		stroke.setColor(KEY_LINE); stroke.setStrokeWidth(Math.max(1f, r * 0.03f)); c.drawCircle(cx, cy, r, stroke);
		int[][] dirs = {{0, -1, javax.microedition.lcdui.Canvas.KEY_UP}, {0, 1, javax.microedition.lcdui.Canvas.KEY_DOWN},
				{-1, 0, javax.microedition.lcdui.Canvas.KEY_LEFT}, {1, 0, javax.microedition.lcdui.Canvas.KEY_RIGHT}};
		for (int[] d : dirs) {
			float tx = cx + d[0] * r * 0.68f, ty = cy + d[1] * r * 0.68f, t = r * 0.15f;
			path.reset();
			path.moveTo(tx + d[0] * t, ty + d[1] * t);
			path.lineTo(tx - d[0] * t + d[1] * t * 1.1f, ty - d[1] * t + d[0] * t * 1.1f);
			path.lineTo(tx - d[0] * t - d[1] * t * 1.1f, ty - d[1] * t - d[0] * t * 1.1f);
			path.close();
			fill.setColor(down.contains(d[2]) ? ACC : 0xFFC8CCD6); c.drawPath(path, fill);
		}
		boolean ok = down.contains(javax.microedition.lcdui.Canvas.KEY_FIRE);
		float rr = r * 0.36f;
		fill.setColor(0xFF1C1F26); c.drawCircle(cx, cy, rr * 1.12f, fill);
		fill.setShader(new LinearGradient(0, cy - rr, 0, cy + rr, ok ? ACC_DARK : 0xFF9CBDE4, ok ? 0xFF4A6D9C : ACC, Shader.TileMode.CLAMP));
		c.drawCircle(cx, cy, rr, fill); fill.setShader(null);
	}
}
