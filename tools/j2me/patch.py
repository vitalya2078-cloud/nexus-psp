#!/usr/bin/env python3
"""Патч KAPSULA для J2ME Loader: движок как библиотека внутри KAPSULA.

1. ContextHolder: контекст приложения задаёт KAPSULA (своего Application у движка в KAPSULA нет).
2. MicroActivity: задаёт контекст первым делом — процесс :j2me начинается с неё.
3. InternalDataSource: без ffmpeg-kit (очень тяжёлый, нужен был только для ADPCM-звука) —
   ADPCM в WAV перекодируется маленьким встроенным декодером (tools/j2me/Adpcm.java).
Каждая замена проверяется: если оригинал изменился — патч падает, а не молча пропускает.
"""
import shutil
import sys
from pathlib import Path

root = Path(sys.argv[1])
java = root / "app/src/main/java"


def edit(rel, pairs):
    p = java / rel
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if s.count(old) != 1:
            sys.exit(f"patch: {rel}: не найдено ровно одно вхождение: {old[:60]!r}")
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")


edit("javax/microedition/util/ContextHolder.java", [
    ("""	public static Context getAppContext() {
		return EmulatorApplication.getInstance();
	}""",
     """	private static Context appContext;

	/** KAPSULA: контекст приложения (в KAPSULA нет EmulatorApplication). */
	public static void setAppContext(Context c) {
		if (c != null) appContext = c.getApplicationContext();
	}

	public static Context getAppContext() {
		return appContext != null ? appContext : EmulatorApplication.getInstance();
	}"""),
])

edit("javax/microedition/shell/MicroActivity.java", [
    ("""	public void onCreate(Bundle savedInstanceState) {
""",
     """	public void onCreate(Bundle savedInstanceState) {
		ContextHolder.setAppContext(this);
"""),
])

edit("javax/microedition/media/InternalDataSource.java", [
    ("""import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.StreamInformation;
""", ""),
])
# convert(): вместо ffprobe/ffmpeg — свой декодер IMA ADPCM WAV → PCM 16 бит
p = java / "javax/microedition/media/InternalDataSource.java"
s = p.read_text(encoding="utf-8")
a = s.index("	private void convert() {")
b = s.index("\n	}\n", s.index("catch (Throwable t)", a)) + len("\n	}\n")
s = s[:a] + """	private void convert() {
		// KAPSULA: ADPCM-звук (в WAV) — в обычный PCM, без ffmpeg
		try {
			File out = new File(mediaFile.getPath() + "_pcm.wav");
			if (ru.kapsula.j2me.Adpcm.convertIfAdpcm(mediaFile, out)) {
				if (!mediaFile.delete()) Log.w(TAG, "convert: error delete file=" + mediaFile);
				mediaFile = out;
				mediaFile.deleteOnExit();
			}
		} catch (Throwable t) {
			Log.e(TAG, "ADPCM error", t);
		}
	}
""" + s[b:]
p.write_text(s, encoding="utf-8")

dst = java / "ru/kapsula/j2me"
dst.mkdir(parents=True, exist_ok=True)
shutil.copy(Path(__file__).parent / "Adpcm.java", dst / "Adpcm.java")
print("patched")
