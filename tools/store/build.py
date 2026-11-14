"""Сборка магазина KAPSULA (запускает CI): скачивает архивы игр из src, достаёт значок и картинку из EBOOT.PBP,
считает размер и пишет итоговый каталог. python3 tools/store/build.py <папка_выхода> <базовый_url_файлов>"""
import json, os, struct, sys, urllib.request, zipfile, io, hashlib

out, base = sys.argv[1], sys.argv[2].rstrip('/') + '/'
os.makedirs(out, exist_ok=True)
cat = json.load(open('store/catalog.json', encoding='utf-8'))

def pbp_parts(b):
    if b[1:4] != b'PBP': return {}
    off = struct.unpack_from('<8I', b, 8) + (len(b),)
    names = ['sfo', 'icon', 'icon1', 'pic0', 'pic1', 'snd', 'psp', 'psar']
    return {n: b[off[i]:off[i + 1]] for i, n in enumerate(names) if off[i + 1] > off[i]}

def sfo_title(b):
    if len(b) < 20 or b[:4] != b'\0PSF': return None
    kt, dt, n = struct.unpack_from('<III', b, 8)
    for i in range(n):
        ko, fmt, ln, mx, do = struct.unpack_from('<HHIII', b, 20 + i * 16)
        key = b[kt + ko:b.index(b'\0', kt + ko)].decode()
        if key == 'TITLE': return b[dt + do:dt + do + ln].rstrip(b'\0').decode('utf-8', 'replace')
    return None

games = []
for g in cat['games']:
    path = os.path.join(out, g['id'] + '.zip')
    if not os.path.exists(path):
        req = urllib.request.Request(g['src'], headers={'User-Agent': 'KAPSULA-store-builder'})
        data = urllib.request.urlopen(req, timeout=120).read()
        open(path, 'wb').write(data)
    data = open(path, 'rb').read()
    z = zipfile.ZipFile(io.BytesIO(data))
    eboots = sorted((n for n in z.namelist() if n.upper().endswith('EBOOT.PBP') and '%' not in n), key=lambda n: n.count('/'))
    eboot = eboots[0] if eboots else None
    if not eboot: print('нет EBOOT.PBP:', g['id']); continue
    parts = pbp_parts(z.read(eboot))
    e = {k: v for k, v in g.items() if k != 'src'}
    if parts.get('icon'):
        open(os.path.join(out, g['id'] + '.png'), 'wb').write(parts['icon']); e['icon'] = base + g['id'] + '.png'
    if parts.get('pic1') and parts['pic1'][:4] == b'\x89PNG':
        open(os.path.join(out, g['id'] + '_bg.png'), 'wb').write(parts['pic1']); e['bg'] = base + g['id'] + '_bg.png'
    e['url'] = base + g['id'] + '.zip'
    e['size'] = len(data)
    e['unpacked'] = sum(i.file_size for i in z.infolist())
    e['sha256'] = hashlib.sha256(data).hexdigest()
    e['eboot'] = eboot
    e['title'] = (sfo_title(parts.get('sfo', b'')) or g['name'])
    games.append(e)
    print('ok', g['id'], len(data), eboot)

json.dump({'version': 1, 'categories': cat['categories'], 'games': games}, open(os.path.join(out, 'catalog.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print('игр в каталоге:', len(games))
