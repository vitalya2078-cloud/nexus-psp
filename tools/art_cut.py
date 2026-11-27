#!/usr/bin/env python3
"""
Нарезка листов из нейросети (зелёный фон #00FF00) на отдельные картинки для приложения.
  python3 tools/art_cut.py <лист.png> <ряды> <колонки> <режим> <папка> имя1 имя2 …  («-» — пропустить ячейку)
Режимы: mascot — холст 321×305 как у ui/img/mascot-l-* (по центру, ноги на одной линии);
        item — обрезка по содержимому, до 256 px; case — футляр: розовое окно этикетки → прозрачное,
        рядом <имя>.json с прямоугольником окна (доли 0..1) для подстановки обложки; big — до 640 px.
"""
import json, sys
import numpy as np
from PIL import Image
from scipy import ndimage

def key(im):
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    green = g - np.maximum(r, b)
    alpha = np.clip(1 - (green - 25) / 70, 0, 1)
    # убрать зелёный ореол по краям
    spill = green > 0
    g2 = np.where(spill, np.maximum(r, b) + (g - np.maximum(r, b)) * 0.15, g)
    out = np.dstack([r, g2, b, alpha * 255]).clip(0, 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")

def cells(im, rows, cols):
    al = np.asarray(im)[..., 3] > 40
    lab, n = ndimage.label(ndimage.binary_dilation(al, iterations=3))
    H, W = al.shape; boxes = {}
    for i, sl in enumerate(ndimage.find_objects(lab), 1):
        if sl is None: continue
        ys, xs = sl; area = (lab[sl] == i).sum()
        if area < 150: continue
        cy, cx = (ys.start + ys.stop) / 2, (xs.start + xs.stop) / 2
        cell = (min(rows - 1, int(cy / H * rows)), min(cols - 1, int(cx / W * cols)))
        b = boxes.get(cell, [xs.start, ys.start, xs.stop, ys.stop])
        boxes[cell] = [min(b[0], xs.start), min(b[1], ys.start), max(b[2], xs.stop), max(b[3], ys.stop)]
    return boxes

def main():
    src, rows, cols, mode, out = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4], sys.argv[5]
    names = sys.argv[6:]
    im = key(Image.open(src))
    bx = cells(im, rows, cols)
    import os; os.makedirs(out, exist_ok=True)
    for k, name in enumerate(names):
        if name == "-": continue
        cell = (k // cols, k % cols)
        if cell not in bx: print("нет ячейки", cell, name); continue
        x0, y0, x1, y1 = bx[cell]
        c = im.crop((max(0, x0 - 2), max(0, y0 - 2), x1 + 2, y1 + 2))
        if mode == "mascot":
            W, H = 321, 305
            s = min((W - 6) / c.width, (H - 4) / c.height)
            c = c.resize((max(1, round(c.width * s)), max(1, round(c.height * s))), Image.LANCZOS)
            cv = Image.new("RGBA", (W, H), (0, 0, 0, 0)); cv.alpha_composite(c, ((W - c.width) // 2, H - c.height - 2))
            cv.save(f"{out}/{name}.webp", "WEBP", quality=90, method=6)
        elif mode == "case":
            a = np.asarray(c).copy()
            mag = (a[..., 0] > 190) & (a[..., 2] > 190) & (a[..., 1] < 90)
            ys, xs = np.where(mag)
            win = [xs.min() / c.width, ys.min() / c.height, (xs.max() + 1) / c.width, (ys.max() + 1) / c.height] if len(xs) else [0, 0, 1, 1]
            a[mag] = [0, 0, 0, 0]
            # тонкая розовая кромка вокруг окна — тоже прочь
            # розовая кромка вокруг окна (смешанные пиксели) — прочь: всё розоватое рядом с окном
            near = ndimage.binary_dilation(mag, iterations=8)
            r_, g_, b_ = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
            pink = near & (r_ > g_ + 25) & (b_ > g_ + 25)
            a[pink] = [0, 0, 0, 0]
            c = Image.fromarray(a, "RGBA")
            s = min(1.0, 420 / max(c.size)); c = c.resize((round(c.width * s), round(c.height * s)), Image.LANCZOS)
            c.save(f"{out}/{name}.webp", "WEBP", quality=92, method=6)
            json.dump({"w": c.width, "h": c.height, "win": [round(v, 4) for v in win]}, open(f"{out}/{name}.json", "w"))
        else:
            lim = 256 if mode == "item" else 640
            s = min(1.0, lim / max(c.size)); c = c.resize((round(c.width * s), round(c.height * s)), Image.LANCZOS)
            c.save(f"{out}/{name}.webp", "WEBP", quality=90, method=6)
        print("готово", name)

if __name__ == "__main__":
    main()
