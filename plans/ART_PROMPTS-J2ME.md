# KAPSULA — графика для Java-игр (J2ME)

Две генерации. Цифры 0–9, `*`, `#` и подписи на кнопках НЕ генерируем — их рисует код (чётко на любом размере).
Нельзя: логотип Java (чашка кофе), Nokia/Sony Ericsson/Motorola и любые бренды, буквы и надписи.

## Лист 1 — значки кнопок телефона (6×4, как touch-sheet-1…4)

```
A 6x4 grid of 24 game-control icons on a pure black background, each icon a solid white flat glyph, centered in its cell, same visual weight and size as a modern mobile UI icon set (thick rounded shapes, no thin hairlines, no outlines-only, no gradients, no shadows, no text, no letters, no digits, no brand logos). Even spacing, generous padding, consistent stroke thickness across all icons. Icons, left to right, top to bottom:
1 left soft key: a small rounded rectangle tab in the bottom-left corner of a phone screen frame
2 right soft key: the same tab in the bottom-right corner
3 OK / select: a filled circle inside a ring
4 five-way navigation pad: a round pad with four arrow notches and a center dot
5 four arrows pointing out in a cross
6 phone handset (call / green key)
7 phone handset tilted down with a short line under it (hang up / red key)
8 3x4 grid of rounded dots (number keypad)
9 backspace / clear: a left-pointing tag with an x inside
10 3x3 grid of rounded squares (phone menu)
11 pause: two vertical rounded bars
12 speaker with sound waves
13 speaker with a slash (mute)
14 stylus pen tapping a small screen with ripple
15 classic candybar keypad phone silhouette (generic, no brand)
16 flip phone, opened, silhouette
17 slider phone silhouette
18 envelope (message)
19 signal bars, four ascending
20 battery, three quarters full
21 floppy disk with a small down arrow (save)
22 two arrows rotating around a phone (screen rotate)
23 joystick lever with a fire dot on top
24 star burst with an exclamation (special / bonus)
Output 1536x1024.
```

## Лист 2 — картинка консоли и значок носителя (2 на листе)

```
Two separate stickers side by side on a transparent background, cartoon sticker style with a thick dark outline, soft cel shading, rounded friendly shapes, graphite and charcoal body colors with ONE accent color steel blue #7FA7D9 and small warm orange highlights, matching a set of retro console illustrations. No text, no letters, no digits on keys (keys are plain rounded bumps), no brand logos, no coffee cup, no real product replica.
LEFT (large, about 60% width): a generic mid-2000s candybar mobile phone, slight 3/4 front view, tall rounded body, small color screen showing a simple pixel-art platformer scene (a tiny hero, blocks, a coin), two soft keys under the screen, a round five-way navigation pad in the middle, call and end keys at its sides, a 3x4 keypad of plain rounded keys below, a short stub antenna on top.
RIGHT (small, about 30% width, square, for a 160px icon): a microSD-style memory card with a steel-blue label stripe and a tiny game-controller pictogram on the label, same sticker style.
Output 1536x1024, transparent PNG.
```

## После генерации
- Лист 1: я вырежу и перерисую нужные значки вектором (как с прошлыми листами), в набор ролей кнопок Java.
- Лист 2: телефон → `ui/img/con-j2me.png` (≈520 px по ширине), карта → `ui/img/ic-jar.png` (160 px).
