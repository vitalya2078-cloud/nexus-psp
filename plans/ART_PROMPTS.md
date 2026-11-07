# KAPSULA — промпты для генерации графики (ChatGPT / GPT-image)

Как пользоваться:
- Генерировать **в одном чате**, сначала отправить «Базовый стиль», потом промпты по очереди — так стиль будет одинаковым.
- Промпты на английском: модели изображений понимают их точнее.
- Просить **прозрачный фон (PNG)**, кроме баннера Google Play.
- Если модель добавила логотипы, надписи или символы PlayStation — перегенерировать: их использовать нельзя.

---

## 0. Базовый стиль (отправить первым)

```
You are creating a consistent asset set for KAPSULA, a minimalist retro-console emulator app for Android.
Style rules for every image in this chat:
- Flat vector illustration, clean geometric line art, 2.5px uniform stroke, rounded line caps.
- Palette ONLY: ivory #F2F1EC (main lines), steel blue #7FA7D9 (single accent), graphite #23262E (subtle fills). No other colors, no gradients except a very soft graphite shadow.
- Calm, premium, strict look. No neon, no glow, no sparkles, no 3D, no photorealism.
- Transparent background, centered subject, generous padding (subject fills ~70% of the canvas).
- Absolutely NO brand logos, NO text, NO letters, NO PlayStation symbols (no triangle/circle/cross/square buttons), NO real product replicas — generic, original, stylized designs only.
Reply "ready" and wait for my next prompts.
```

---

## 1. Иллюстрации консолей (1024×1024, прозрачный фон)

**1.1 Портативная консоль (PSP)**
```
Illustration: a generic handheld game console seen from the front, wide rounded-pill body, large horizontal screen in the center, a small d-pad on the left and four plain round buttons on the right (no symbols), two shoulder bumps on top. The screen is a soft graphite fill with one steel-blue accent line. Follow the base style. 1024x1024, transparent background.
```

**1.2 Приставка с CD (PS1)**
```
Illustration: a generic retro home console seen at a slight 3/4 angle from above, compact low rectangular body with a round top lid for a CD, a CD disc partially visible above it with a steel-blue reflection arc, one wired gamepad with plain round buttons (no symbols) lying in front. Follow the base style. 1024x1024, transparent background.
```

**1.3 Приставка-«башня» с DVD (PS2)**
```
Illustration: a generic slim home console standing vertically like a tower, with horizontal grooves, a thin DVD tray slot with a steel-blue accent line, and a DVD disc leaning against it. Original design, not a replica of any real product. Follow the base style. 1024x1024, transparent background.
```

**1.4 «Все консоли»**
```
Illustration: three small generic console silhouettes (a handheld, a compact disc console, a vertical tower) arranged in a gentle arc, overlapping slightly, the middle one with a steel-blue accent. Follow the base style. 1024x1024, transparent background.
```

---

## 2. Значки носителей и функций (512×512, прозрачный фон, единый набор)

```
Create a set of 8 matching line icons on a 4x2 grid, each icon in its own equal cell, 512x512 per icon, follow the base style, icons must look like one family (same stroke, same corner radius, same visual weight):
1) small optical disc in a protective shell with a window (generic mini-disc, no logo)
2) CD disc with a center hole
3) DVD disc with a small steel-blue arc
4) BIOS chip: square microchip with pins and a tiny key symbol
5) save slot: a floppy-like save card with a check mark
6) cloud with an up-and-down arrow (sync)
7) download into a capsule-shaped container
8) gamepad, generic, plain round buttons (no symbols)
Transparent background.
```
(Если модель не делит на сетку — просить по одному значку с тем же текстом стиля.)

---

## 3. Заглушка «нет обложки» (600×800, вертикальная обложка)

```
A placeholder game cover, 600x800 portrait, graphite #1A1C22 background with a very subtle diagonal line texture, centered outline of a capsule (pill shape split into two halves: left half contains a steel-blue play triangle, right half contains three short horizontal ivory lines). Minimal, elegant, no text. Follow the base style but use the solid graphite background instead of transparent.
```

---

## 4. Пустые и ошибочные состояния (800×600, прозрачный фон)

**4.1 Нет интернета**
```
Empty-state illustration 800x600: a capsule-shaped device sitting calmly with a broken wifi arc above it, one wifi arc highlighted in steel blue, a small plug lying unplugged nearby. Friendly, calm, minimal. Follow the base style.
```

**4.2 Мало памяти**
```
Empty-state illustration 800x600: a storage box overflowing with stacked discs and game cases, the lid cannot close, a small steel-blue gauge showing almost full. Minimal, calm. Follow the base style.
```

**4.3 Библиотека пуста**
```
Empty-state illustration 800x600: an empty open shelf with soft graphite shadow, one capsule-shaped game case being placed onto it by a simple line-art hand, steel-blue accent on the case. Inviting, minimal. Follow the base style.
```

**4.4 Нужен BIOS**
```
Empty-state illustration 800x600: a generic console with an open service panel, a microchip floating next to it with a small key, a steel-blue dotted line showing where the chip goes. Minimal, instructional. Follow the base style.
```

**4.5 Ничего не найдено**
```
Empty-state illustration 800x600: a magnifying glass over an empty grid of rounded card outlines, one card outline highlighted in steel blue with a question mark made of a simple curve (no letters). Minimal. Follow the base style.
```

---

## 5. Значки достижений (512×512, прозрачный фон, набор из 6)

```
Create 6 achievement badges as one matching set, 3x2 grid, each 512x512, follow the base style. Each badge is a rounded hexagon outline in ivory with an inner symbol; unlocked look uses one steel-blue detail:
1) First launch — a play triangle with a small spark line
2) Marathon 10 hours — a clock with a full ring
3) 10 saves — stacked save cards
4) Three consoles — three small dots in a triangle connected by lines
5) 100 hours — a trophy cup, simple
6) Five favourites — a heart with five small ticks around it
No text, no numbers rendered as letters (show quantities with simple marks). Transparent background.
```
Дополнительно попросить: «Make a locked version of the same set: all lines in graphite #5A5E68, no blue».

---

## 6. Значок уведомлений Android (96×96)

```
A single-color white silhouette icon, 96x96, transparent background: a capsule (pill) shape split vertically into two halves, left half has a cut-out play triangle, right half has three short horizontal cut-out lines. Pure white #FFFFFF only, no shading, no outline stroke variations, must read clearly at 24x24.
```
(Этот лучше потом перерисовать векторно — Android требует строгий одноцветный значок.)

---

## 7. Google Play — баннер (1024×500, без прозрачности)

```
Google Play feature graphic 1024x500 for an app called KAPSULA. Dark graphite background #0B0C0F with a very subtle diagonal texture. On the left: the KAPSULA capsule logo mark (pill split in two halves: left half has a steel-blue play triangle, right half has three ivory lines) and the word "KAPSULA" in a bold geometric sans-serif, wide letter spacing, ivory color. On the right: three generic stylized console line illustrations (handheld, compact disc console, vertical tower) in ivory lines with one steel-blue accent, arranged in a gentle arc. Premium, calm, strict. No other text, no brand logos, no PlayStation symbols.
```
(Текст в генерации часто искажается — надпись KAPSULA можно потом наложить в Canva.)

---

## После генерации
Присылай картинки — я обрежу, подгоню размеры, при необходимости переведу в вектор (SVG) и встрою в приложение.

---

## Экономный вариант: 3 генерации вместо 20
Отправить «Базовый стиль», затем три листа. Нарезку на отдельные картинки делает разработчик.

- **Лист 1 (1536×1024, сетка 6×4):** значки носителей и функций (1–8), достижения открытые (9–14) и закрытые (15–20), значок уведомлений (21).
- **Лист 2 (1536×1024, сетка 2×2):** PSP, PS1, PS2, «все консоли».
- **Лист 3 (1536×1024, сетка 3×2):** нет интернета, мало памяти, библиотека пуста, нужен BIOS, ничего не найдено.

Полные тексты листов — в переписке от 06.11.2026 (ответ «Собрал всё мелкое на один холст»). Приоритет при нехватке лимита: лист 1 → лист 2 → лист 3. Заглушку «нет обложки» рисуем в коде.
