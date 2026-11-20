# Маскот KAPSULA «кукла из частей» — промпты (2026-11-20)

Идея: маскот собирается в коде из частей и анимируется на 60 кадрах/с (дыхание, моргание, ручки, хохолок, предметы).
К каждой генерации прикладывать образец: `mascot-reference.png` (нынешний маскот: «машет» + голова).
Два листа: крупные части — отдельно (им нужно разрешение), лицо и предметы — мелким листом.
Лист A — на тёмном фоне (светлое тело). Лист B — на БЕЛОМ: глаза тёмные, на тёмном фоне при вырезке становятся дырами (проверено на листе A, 2026-11-20). Свечение и искорки НЕ рисуем: их делает код.

## Лист A — тело, рюкзак, ручки (4×3, 1536×1152)
```
Use the ATTACHED IMAGE as the exact character reference: the cute glowing cream-yellow flame/ghost mascot with a dark cartridge backpack. Keep its style, colors, proportions, outline and shading EXACTLY the same.
Make a sprite sheet of SEPARATE BODY PARTS for cut-out (puppet) animation:
- canvas 1536x1152, flat solid dark background #14161B, a strict grid of 4 columns x 3 rows, equal cells, each part centered in its own cell, wide empty gaps, nothing touching;
- ALL parts at exactly the SAME SCALE as each other (as if they were cut from one 900-pixel-tall character), straight FRONT view, no perspective;
- NO glow, NO sparkles, NO aura, NO shadows around parts (the app adds glow), clean edges.
Cells, left to right, top to bottom:
1 REFERENCE: the whole mascot assembled, neutral pose, front view, arms relaxed down, eyes open, small smile, with backpack — this shows how the parts fit;
2 BODY only: the blob body WITHOUT face, WITHOUT arms, WITHOUT backpack, WITHOUT the flame curl on top (smooth rounded top);
3 the FLAME CURL / tuft from the top of the head, alone;
4 the BACKPACK (cartridge pack) alone, as seen behind the body;
5 LEFT arm (character's left, on the right side of the image) relaxed down;
6 LEFT arm raised up, waving;
7 LEFT arm bent forward, holding something in front of the belly (hand empty);
8 RIGHT arm relaxed down;
9 RIGHT arm raised up, waving;
10 RIGHT arm bent forward, holding something in front of the belly (hand empty);
11 and 12: empty cells.
Each arm has a soft rounded end where it joins the body, so it can overlap the body without a visible seam. No text, no logos.
```

## Лист B — глаза, рты, предметы (6×4, 1536×1024)
```
Use the ATTACHED IMAGE as the exact character reference (the same cute glowing cream-yellow mascot). Keep its face style EXACTLY: the same big glossy dark eyes, the same small mouth, the same line thickness.
Make a sprite sheet of SEPARATE FACE PARTS and PROPS for cut-out animation:
- canvas 1536x1024, flat solid pure WHITE background #FFFFFF (the eyes are dark — they must not merge with the background), a strict grid of 6 columns x 4 rows, equal cells, each item centered, wide gaps, nothing touching;
- all eyes are a PAIR with exactly the same distance between them and the same size in every cell; all mouths at the same scale;
- NO glow, NO sparkles around items, clean edges, front view.
Row 1 — EYES (pairs): 1 open normal; 2 closed (blink, soft curved lines); 3 happy ^^ arcs; 4 sad (with tilted eyebrows); 5 sleepy half-closed; 6 wink (left open, right closed).
Row 2 — 7 surprised wide-open eyes (pair); MOUTHS: 8 small smile; 9 open happy smile; 10 small "o"; 11 sad frown; 12 big yawn.
Row 3 — 13 flat calm mouth line; 14 pair of soft pink blush cheeks; 15 a single tear drop; 16 a small ceramic mug with steam; 17 a small golden trophy cup; 18 a golden coin.
Row 4 — 19 a small red heart; 20 three sleepy "z" shapes (stylized, not a font); 21 a small game box being carried; 22 a tiny keypad mobile phone; 23 a small cloud with an up arrow; 24 a four-point sparkle star.
Props are sized to be held in the mascot's small hands. No other text, no logos.
```

## Проверить в результатах
- Части одного масштаба; тело без лица, рук и хохолка; ручки с круглыми концами.
- Глаза — пары с одинаковым расстоянием во всех ячейках; рты одного размера.
- Без свечения и искорок; фон ровный тёмный.

## Итоги сборки листа A (2026-11-20)
- Собрал куклу: дыхание, хохолок, моргание (пока сжатием лица из эталона), взмах — работает, пропорции узнаваемые.
- Ручки на листе в ~2,3 раза крупнее, чем у эталона, — масштабирую в коде по эталону.
- У хохолка ровный срез — ставлю ЗА телом.
- Лямки рюкзака не нарисованы на теле — при желании отдельной частью.
