package ru.kapsula.j2me;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * KAPSULA: IMA ADPCM (WAV, формат 0x11) → PCM 16 бит. Заменяет ffmpeg-kit в J2ME Loader:
 * Android сам не играет ADPCM-WAV, а в Java-играх (Nokia, Samsung) такой звук встречается.
 * Остальные форматы не трогаем (false) — их играет Android.
 */
public final class Adpcm {
	private static final int[] STEP = {
			7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
			130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060,
			1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484,
			7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767};
	private static final int[] INDEX = {-1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8};

	private Adpcm() {}

	private static int u16(byte[] b, int o) { return (b[o] & 0xFF) | (b[o + 1] & 0xFF) << 8; }
	private static int u32(byte[] b, int o) { return u16(b, o) | u16(b, o + 2) << 16; }

	/** Если in — WAV с IMA ADPCM: пишет PCM-WAV в out и отвечает true. */
	public static boolean convertIfAdpcm(File in, File out) throws IOException {
		if (in.length() < 44 || in.length() > 16 * 1024 * 1024) return false;
		byte[] b = new byte[(int) in.length()];
		try (RandomAccessFile f = new RandomAccessFile(in, "r")) { f.readFully(b); }
		byte[] pcm = decodeWav(b);
		if (pcm == null) return false;
		try (OutputStream os = new FileOutputStream(out)) { os.write(pcm); }
		return true;
	}

	/** WAV с IMA ADPCM → готовый PCM-WAV (с заголовком); null — не ADPCM или файл повреждён. */
	public static byte[] decodeWav(byte[] b) {
		if (b.length < 12 || b[0] != 'R' || b[1] != 'I' || b[2] != 'F' || b[3] != 'F' || b[8] != 'W' || b[9] != 'A') return null;
		int pos = 12, fmt = -1, ch = 0, rate = 0, block = 0, dataOff = -1, dataLen = 0;
		while (pos + 8 <= b.length) {
			int len = u32(b, pos + 4);
			if (len < 0 || pos + 8 + len > b.length) len = b.length - pos - 8;
			if (b[pos] == 'f' && b[pos + 1] == 'm' && b[pos + 2] == 't' && len >= 16) {
				fmt = u16(b, pos + 8); ch = u16(b, pos + 10); rate = u32(b, pos + 12); block = u16(b, pos + 20);
			} else if (b[pos] == 'd' && b[pos + 1] == 'a' && b[pos + 2] == 't' && b[pos + 3] == 'a') {
				dataOff = pos + 8; dataLen = len;
			}
			pos += 8 + len + (len & 1);
		}
		if (fmt != 0x11 || ch < 1 || ch > 2 || dataOff < 0 || block < 4 * ch || rate <= 0) return null;
		ByteArrayOutputStream pcm = new ByteArrayOutputStream(dataLen * 4 + 44);
		for (int o = dataOff; o + 4 * ch <= dataOff + dataLen; o += block) decodeBlock(b, o, Math.min(block, dataOff + dataLen - o), ch, pcm);
		byte[] body = pcm.toByteArray();
		byte[] wav = new byte[44 + body.length];
		header(wav, ch, rate, body.length);
		System.arraycopy(body, 0, wav, 44, body.length);
		return wav;
	}

	private static void decodeBlock(byte[] b, int o, int len, int ch, ByteArrayOutputStream out) {
		int[] pred = new int[ch], idx = new int[ch];
		for (int c = 0; c < ch; c++) {
			pred[c] = (short) u16(b, o + c * 4);
			idx[c] = Math.max(0, Math.min(88, b[o + c * 4 + 2]));
			put(out, pred[c]);
		}
		int p = o + 4 * ch, end = o + len;
		// данные: по 4 байта (8 отсчётов) на канал по очереди
		short[][] buf = new short[ch][8];
		while (p + 4 * ch <= end) {
			for (int c = 0; c < ch; c++) {
				for (int i = 0; i < 4; i++) {
					int v = b[p + c * 4 + i] & 0xFF;
					buf[c][i * 2] = nibble(v & 0x0F, pred, idx, c);
					buf[c][i * 2 + 1] = nibble(v >> 4, pred, idx, c);
				}
			}
			for (int i = 0; i < 8; i++) for (int c = 0; c < ch; c++) put(out, buf[c][i]);
			p += 4 * ch;
		}
	}

	private static short nibble(int n, int[] pred, int[] idx, int c) {
		int step = STEP[idx[c]];
		int diff = step >> 3;
		if ((n & 4) != 0) diff += step;
		if ((n & 2) != 0) diff += step >> 1;
		if ((n & 1) != 0) diff += step >> 2;
		pred[c] = Math.max(-32768, Math.min(32767, (n & 8) != 0 ? pred[c] - diff : pred[c] + diff));
		idx[c] = Math.max(0, Math.min(88, idx[c] + INDEX[n]));
		return (short) pred[c];
	}

	private static void put(ByteArrayOutputStream out, int s) { out.write(s & 0xFF); out.write((s >> 8) & 0xFF); }

	private static void header(byte[] w, int ch, int rate, int len) {
		w[0] = 'R'; w[1] = 'I'; w[2] = 'F'; w[3] = 'F'; le32(w, 4, 36 + len);
		w[8] = 'W'; w[9] = 'A'; w[10] = 'V'; w[11] = 'E';
		w[12] = 'f'; w[13] = 'm'; w[14] = 't'; w[15] = ' '; le32(w, 16, 16);
		le16(w, 20, 1); le16(w, 22, ch); le32(w, 24, rate); le32(w, 28, rate * ch * 2); le16(w, 32, ch * 2); le16(w, 34, 16);
		w[36] = 'd'; w[37] = 'a'; w[38] = 't'; w[39] = 'a'; le32(w, 40, len);
	}

	private static void le16(byte[] w, int o, int v) { w[o] = (byte) v; w[o + 1] = (byte) (v >> 8); }
	private static void le32(byte[] w, int o, int v) { le16(w, o, v); le16(w, o + 2, v >> 16); }
}
