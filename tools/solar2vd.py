"""Значки Solar (CC BY 4.0) из ui/kapsula.js → векторные значки Android (res/drawable).

sl_* — linear (обводка, красится tint), sb_* — bold (заливка, tint),
sd_* — bold duotone: основная часть цветом слоновой кости, полупрозрачная — стальным синим (цвета зашиты).
Запуск: python3 tools/solar2vd.py
"""
import json, os, re
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'ui', 'kapsula.js')
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable')
INK, ACC = '#F2F1EC', '#7FA7D9'


def objects():
    js = open(SRC, encoding='utf-8').read()
    res = {}
    for name in ('SL', 'SD', 'SB'):
        m = re.search(r'^var ' + name + r' = (\{.*?\});$', js, re.M)
        res[name] = json.loads(m.group(1))
    return res


def snake(n):
    return re.sub(r'([A-Z])', lambda m: '_' + m.group(1).lower(), n)


def circle_d(cx, cy, r):
    cx, cy, r = float(cx), float(cy), float(r)
    return f'M{cx - r:g},{cy}a{r:g},{r:g} 0 1,0 {2 * r:g},0a{r:g},{r:g} 0 1,0 {-2 * r:g},0'


def walk(el, inh, out):
    st = dict(inh)
    for a in ('fill', 'stroke', 'stroke-width', 'stroke-linecap', 'stroke-linejoin', 'opacity', 'fill-rule'):
        if a in el.attrib:
            st[a] = el.attrib[a]
    tag = el.tag
    if tag == 'path':
        out.append((el.attrib['d'], st))
    elif tag == 'circle':
        out.append((circle_d(el.attrib['cx'], el.attrib['cy'], el.attrib['r']), st))
    for ch in el:
        walk(ch, st, out)


def vd(body, mode):
    root = ET.fromstring('<svg>' + body + '</svg>')
    parts = []
    walk(root, {}, parts)
    paths = []
    for d, st in parts:
        alpha = st.get('opacity')
        color = INK if mode == 'duo' else '#FFFFFFFF'
        if mode == 'duo' and alpha is not None:
            color, alpha = ACC, None
        a = [f'android:pathData="{d}"']
        fill = st.get('fill', 'currentColor')
        if fill != 'none':
            a.append(f'android:fillColor="{color}"')
            if alpha: a.append(f'android:fillAlpha="{alpha}"')
            if st.get('fill-rule') == 'evenodd': a.append('android:fillType="evenOdd"')
        stroke = st.get('stroke', 'none')
        if stroke != 'none':
            a.append(f'android:strokeColor="{color}"')
            a.append(f'android:strokeWidth="{st.get("stroke-width", "1")}"')
            if alpha: a.append(f'android:strokeAlpha="{alpha}"')
            cap = st.get('stroke-linecap')
            if cap: a.append(f'android:strokeLineCap="{cap}"')
            join = st.get('stroke-linejoin')
            if join: a.append(f'android:strokeLineJoin="{join}"')
        paths.append('    <path ' + ' '.join(a) + ' />')
    return ('<?xml version="1.0" encoding="utf-8"?>\n<!-- Solar by 480 Design, CC BY 4.0. Сгенерировано tools/solar2vd.py -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" '
            'android:viewportWidth="24" android:viewportHeight="24">\n' + '\n'.join(paths) + '\n</vector>\n')


def main():
    o = objects()
    os.makedirs(OUT, exist_ok=True)
    n = 0
    for key, prefix, mode in (('SL', 'sl_', 'line'), ('SB', 'sb_', 'line'), ('SD', 'sd_', 'duo')):
        for name, body in o[key].items():
            open(os.path.join(OUT, prefix + snake(name) + '.xml'), 'w', encoding='utf-8').write(vd(body, mode))
            n += 1
    print('icons:', n)


if __name__ == '__main__':
    main()
