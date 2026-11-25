# Сайт KAPSULA — промпты картинок (2026-11-25)

Нужны только два листа: всё остальное (фон, звёзды, город, рамка телефона, пылинки, инструкции) рисует код.
Фон обоих листов — хромакей #00FF00, как у листов поз маскота: вырезается чисто, а зелёного в маскоте и предметах нет.
Мелкие иконки интерфейса не генерируем — их рисует код вектором.

## Лист 1 — новые позы маскота для сайта (4×3, 1536×1152)
Приложить: `plans/mascot/poses-1.png` (лучше и `poses-2.png`) — чтобы маскот вышел тем же и того же размера.
Бег влево не рисуем — код отражает бег вправо.
```
Use the ATTACHED IMAGES as the exact character reference: the cute glowing cream-yellow flame/ghost mascot with a dark cartridge backpack. Keep its design EXACTLY the same in every drawing: same proportions, same big glossy dark eyes, same flame curl, same backpack, same colors, line style and shading, and the SAME SIZE as on the attached sheets. Only the pose, face and props change.
Make a character pose sheet of 12 SEPARATE drawings of this mascot:
- canvas 1536x1152, flat solid pure green chroma-key background #00FF00 (no gradients, no shadows on the background), a strict grid of 4 columns x 3 rows, equal cells, wide empty gaps, nothing touching;
- the mascot is the SAME SIZE in every cell, full body, centered, feet on the same baseline near the bottom of each cell;
- soft warm inner glow on the body is fine, but NO outer glow, NO sparkles or particles around (the app adds them);
- no green colors anywhere on the character or props; no text, no logos.
Poses, left to right, top to bottom:
Row 1: 1 pointing to the right side with one arm, friendly "look here" face; 2 pointing straight up with one arm, excited; 3 holding a small smartphone with both hands and showing its screen to the viewer (the screen is a plain dark rectangle), proud smile; 4 waving goodbye while holding a small glowing lantern in the other hand (night, cozy).
Row 2: 5 dancing: arms up to the left, leaning left, happy eyes ^^; 6 dancing: arms up to the right, leaning right, happy eyes ^^; 7 dancing: spinning on one foot, arms out, mouth open laughing; 8 running fast to the right, body leaning forward, determined happy face.
Row 3: 9 standing with the open backpack held above the head with both hands to catch something falling, looking up; 10 dizzy after being bumped: swirly eyes, wobbly pose (no stars drawn around); 11 lost and confused, holding an unfolded paper map upside down, question face; 12 sitting cross-legged and holding a tiny game console, very focused.
```

## Лист 2 — предметы для мини-игры «Поймай картриджи» (6×4, 1536×1024)
Приложить тот же лист поз: стиль и свет — как у маскота.
```
Use the ATTACHED IMAGE only as a STYLE reference (cozy, soft cel shading, clean dark outline, warm lamp light from the upper left), not as content.
A sprite sheet of 24 SEPARATE small game items for a cozy retro catching mini-game:
- canvas 1536x1024, flat solid pure green chroma-key background #00FF00, a strict grid of 6 columns x 4 rows, equal cells, each item centered, wide gaps, nothing touching;
- all items about the same size (fill about 70% of the cell), straight front view, slight tilt allowed, NO glow and NO sparkles around them;
- no green colors on the items; generic original designs, NO brand logos, NO real product replicas, NO text or letters.
Row 1 (good items): 1 red game cartridge; 2 blue game cartridge; 3 yellow game cartridge; 4 purple game cartridge; 5 grey cartridge with a blank amber label; 6 small handheld-console game card.
Row 2 (good items): 7 shiny game disc (rainbow reflection); 8 game disc in a clear jewel case; 9 small portable-disc case; 10 golden coin; 11 small golden star; 12 small red heart.
Row 3 (bad items): 13 cracked broken game disc; 14 cute but grumpy pixel glitch bug (round, spiky, purple); 15 tangled cable knot; 16 small grey rain cloud with a drop; 17 black bomb with a short burning fuse (cartoony); 18 rotten apple core.
Row 4 (power-ups and bits): 19 horseshoe magnet (red and silver); 20 hourglass with amber sand; 21 tiny desk lamp glowing warm; 22 small cartoon clock; 23 four-point sparkle star in warm amber; 24 small soft puff of dust cloud (for landing effect).
```

## Проверить в результатах
- Лист 1: маскот того же размера и вида, что на `poses-1.png`; ноги на одной линии; без свечения вокруг.
- Лист 2: предметы одного размера, без зелёного, без логотипов и надписей.
Пришли листы как есть — вырежу, выровняю и встрою.
