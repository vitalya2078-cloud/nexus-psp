"""Листы-образцы пиктограмм (plans/icons/touch-sheet-N.png, 6x4) и старые PNG (ui/img/ctl) -> VectorDrawable res/drawable/ctl_<имя>.xml.
Обводка potrace (pure-python potracer), заливка белым, evenOdd; размер вписан в поле 100x100 с полями."""
import sys, os, numpy as np, potrace
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'app/src/main/res/drawable')
SHEETS = {
 1: 'tap back menu jump attack sprint crouch - block roll camreset map kick throw special lock cover heal bag skill talk skip climb door',
 2: 'aim shoot reload - grenade gas brake hbrake nitro horn wheel car view pass - tackle players combo turbo both tilt layout rewind shot',
 3: 'grab weapon goal sword bow magic potion dash sneak light knife lookback swim lever jab heavy charge key zoom point moto slowmo lights drop',
}
# старые PNG, которых нет в новых листах
OLD = 'dpad l1 l2 r1 r2 select start pause ff save load swap edit mirror left right up down leftd rightd cam ok rew stick fb fl fr ft'.split()

def trace(mask):
    ys, xs = np.nonzero(mask)
    y0, y1, x0, x1 = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    m = mask[y0:y1, x0:x1]
    h, w = m.shape; s = max(h, w)
    pad = np.zeros((s, s), bool); pad[(s - h) // 2:(s - h) // 2 + h, (s - w) // 2:(s - w) // 2 + w] = m
    k = 84.0 / s; off = 8.0  # поле 100, значок 84
    bm = potrace.Bitmap(~pad)  # potracer заливает пиксели со значением False
    pl = bm.trace(turdsize=max(4, int(s * s / 4000)), alphamax=1.0, opttolerance=0.3)
    f = lambda p: f"{p.x * k + off:.2f},{p.y * k + off:.2f}"
    d = []
    for c in pl:
        d.append('M' + f(c.start_point))
        for seg in c.segments:
            if seg.is_corner: d.append('L' + f(seg.c) + 'L' + f(seg.end_point))
            else: d.append('C' + f(seg.c1) + ' ' + f(seg.c2) + ' ' + f(seg.end_point))
        d.append('Z')
    return ''.join(d)

def write(name, d):
    open(os.path.join(OUT, f'ctl_{name}.xml'), 'w').write(
        '<?xml version="1.0" encoding="utf-8"?>\n<!-- KAPSULA Touch: значок кнопки (tools/ctl_icons.py) -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="48dp" android:height="48dp" '
        'android:viewportWidth="100" android:viewportHeight="100">\n'
        f'  <path android:fillColor="#FFFFFFFF" android:fillType="evenOdd" android:pathData="{d}"/>\n</vector>\n')

def sheets():
    for n, names in SHEETS.items():
        im = np.asarray(Image.open(os.path.join(ROOT, f'plans/icons/touch-sheet-{n}.png')).convert('L'))
        H, W = im.shape; cw, ch = W // 6, H // 4
        for i, name in enumerate(names.split()):
            if name == '-': continue
            r, c = divmod(i, 6)
            cell = im[r * ch + 6:(r + 1) * ch - 6, c * cw + 6:(c + 1) * cw - 6]
            write(name, trace(cell > 128))

def old():
    for name in OLD:
        p = os.path.join(ROOT, f'ui/img/ctl/{name}.png')
        a = np.asarray(Image.open(p).convert('RGBA'))
        write(name, trace((a[..., 3] > 128) & (a[..., :3].mean(-1) > 100)))

if __name__ == '__main__':
    old(); sheets()
    print('ok')
