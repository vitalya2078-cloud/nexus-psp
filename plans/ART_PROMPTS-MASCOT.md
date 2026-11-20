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

---
# ❌ «Кукла из частей» отклонена пользователем (не понравилась сборка). Новый план — целые позы (2026-11-20)
24 позы, два листа по 12 (4×3, 1536×1152 → около 380 точек на маскота, как нынешние 360).
Фон — хромакей #00FF00 (в маскоте зелёного нет: вырежется и светлое тело, и тёмные глаза). К каждой генерации — образец `plans/mascot-reference.png`.
Между позами код делает плавный переход (подскок с растяжкой + смена картинки) и «дыхание» целиком.

## Лист 1 — основа, эмоции, время суток (позы 1–12)
```
Use the ATTACHED IMAGE as the exact character reference: the cute glowing cream-yellow flame/ghost mascot with a dark cartridge backpack. Keep its design EXACTLY the same in every drawing: same proportions, same big glossy dark eyes, same flame curl, same backpack, same colors, line style and shading. Only the pose, face and props change.
Make a character pose sheet of 12 SEPARATE drawings of this mascot:
- canvas 1536x1152, flat solid pure green chroma-key background #00FF00 (no gradients, no shadows on the background), a strict grid of 4 columns x 3 rows, equal cells, wide empty gaps, nothing touching;
- the mascot is the SAME SIZE in every cell, full body, centered, feet on the same baseline near the bottom of each cell;
- soft warm inner glow on the body is fine, but NO outer glow, NO sparkles or particles around (the app adds them);
- no text, no logos.
Poses, left to right, top to bottom:
Row 1: 1 standing calm, small smile, arms relaxed (base pose); 2 EXACTLY the same pose as 1 but with eyes closed (blink); 3 looking to the left; 4 looking to the right.
Row 2: 5 waving hello; 6 jumping with joy, both arms up; 7 laughing, eyes ^^; 8 surprised, mouth "o", arms out.
Row 3: 9 sad, eyes down; 10 stretching after waking up (morning); 11 holding a warm mug with steam, cozy (evening); 12 sitting and sleeping with three small stylized "z" above.
```

## Лист 2 — события (позы 13–24)
```
Use the ATTACHED IMAGE as the exact character reference: the cute glowing cream-yellow flame/ghost mascot with a dark cartridge backpack. Keep its design EXACTLY the same in every drawing: same proportions, same big glossy dark eyes, same flame curl, same backpack, same colors, line style and shading. Only the pose, face and props change.
Make a character pose sheet of 12 SEPARATE drawings of this mascot:
- canvas 1536x1152, flat solid pure green chroma-key background #00FF00 (no gradients, no shadows on the background), a strict grid of 4 columns x 3 rows, equal cells, wide empty gaps, nothing touching;
- the mascot is the SAME SIZE in every cell, full body, centered, feet on the same baseline near the bottom of each cell;
- soft warm inner glow on the body is fine, but NO outer glow, NO sparkles or particles around (the app adds them);
- no text, no logos.
Poses, left to right, top to bottom:
Row 1: 13 winking with a thumbs-up; 14 thinking, hand at chin, eyes up; 15 crying with one tear; 16 big yawn, sleepy.
Row 2: 17 carrying a game box in both hands; 18 hugging a pile of golden coins; 19 holding a golden trophy above the head; 20 holding a red heart in both hands.
Row 3: 21 a small cloud with an up arrow above the head, looking at it; 22 playing a tiny keypad mobile phone; 23 playing with a gamepad, excited; 24 scared, hiding behind its backpack, only eyes visible.
```
Для второго листа лучше приложить ещё и готовый лист 1 — так позы точно выйдут одного размера и стиля.
