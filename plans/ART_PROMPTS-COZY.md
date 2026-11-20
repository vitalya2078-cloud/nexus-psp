# KAPSULA «Уют» — промпты графики (отдельная версия, основная не меняется)

Порядок: в ОДНОМ чате сначала «0. Стиль», потом листы 1–4 по очереди (стиль сохранится).
Нельзя: логотипы и названия брендов (PlayStation, Nintendo, Sega, Sony…), копии реальных коробок и картриджей, надписи.
Места под обложки и подписи — ровная серая заливка #808080: туда код вставит обложку игры или название.

## 0. Стиль (отправить первым)
```
We are making a consistent set of game-art assets for "KAPSULA Cozy", a retro game emulator app. Mood: a cozy room in the EVENING lit by a warm desk lamp.
Style rules for every image in this chat:
- Soft painterly 2D illustration with clean shapes, gentle cel shading, subtle hand-made texture (cardboard fibers, wood grain), no photorealism, no 3D render look.
- Lighting: warm lamp light from the upper left (color ~#FFC27A), soft long shadows to the lower right, deep warm darkness in the corners. No neon, no cold blue light.
- Palette: warm browns, honey wood, kraft cardboard beige, cream, muted terracotta, a little olive green; dark areas are warm dark brown (~#1E140D), never pure black.
- Straight front view (orthographic), no tilt, unless a prompt says otherwise.
- Absolutely NO brand logos, NO real product replicas, NO text or letters (except where a prompt asks for an empty label area).
- Areas reserved for game covers or labels are filled with a flat neutral grey #808080 with crisp edges.
Reply "ready" and wait.
```

## 1. Комната (одна картинка, 1536×1024)
```
Background for the main screen, landscape 1536x1024: the back wall of a cozy evening room, nothing in the foreground. Warm wallpaper with a very subtle vertical pattern, a wooden wainscot at the bottom, a warm pool of light from a desk lamp that is OUTSIDE the frame at the upper left, the light fades into warm darkness toward the right and the corners. Quiet and calm, low detail in the middle so shelves and UI can be placed on top. No shelves, no furniture, no objects, no text.
```

## 2. Картонная коробка консоли — 4 состояния (2×2, 1536×1536)
```
A 2x2 sprite sheet on a flat pure white background (#FFFFFF), four separate drawings of the SAME kraft cardboard storage box (like a box for keeping game collections), same size and position in every cell, front view slightly from above (about 15 degrees), warm lamp light from the upper left, soft contact shadow under the box only, wide empty gaps between cells:
cell 1 (top left): box CLOSED, lid on, a blank rectangular paper label on the front side filled with flat grey #808080;
cell 2 (top right): lid slightly lifted on the back edge, a thin warm glow coming out of the gap;
cell 3 (bottom left): lid fully opened and leaning back, the inside is empty, cardboard dividers visible;
cell 4 (bottom right): the EMPTY OPEN box seen from the front-top so the inside is visible, room for game cases standing in a row like records in a crate.
Same label position and size in all cells. No text, no logos.
```

## 3. Оболочки игр — 12 штук (4×3, 1536×1152)
```
A 4x3 sprite sheet on a flat pure white background (#FFFFFF), 12 separate game containers, each centered in its own equal cell with wide gaps, same scale family, straight front view, warm lamp light from the upper left, soft contact shadow only under each object. Generic original designs, no brand logos, no real product replicas. Every cover area / label area is a flat grey #808080 rectangle with crisp edges (the app will put the game cover there).
Row 1 (FRONT views):
1 a slim portable-disc case: tall rounded plastic case, dark translucent, cover window #808080 taking most of the front;
2 a CD jewel case: square clear plastic case with a dark hinge spine on the left, cover area #808080;
3 a DVD keep case: tall dark plastic case with a clear sleeve, cover area #808080;
4 a classic 8-bit console cartridge: grey plastic, ridged grip at the bottom, label area #808080 on the front.
Row 2 (FRONT views):
5 a 16-bit console cartridge: dark plastic, taller label area #808080, grip lines on the sides;
6 a credit-card-sized game card: thin, rounded corners, label area #808080;
7 a small handheld-console cartridge: compact, label area #808080;
8 a generic mid-2000s keypad mobile phone lying flat, screen area #808080.
Row 3 (SPINE views, the same containers standing edge-on, as seen in a crate):
9 spine of the portable-disc case (narrow, label strip #808080);
10 spine of the CD jewel case;
11 spine of the DVD case (label strip #808080);
12 top edge of the 8-bit cartridge (label strip #808080).
No text, no letters.
```

## 4. Полка и мелочи для уюта (3×2, 1536×1024)
```
A 3x2 sprite sheet on a flat pure white background (#FFFFFF), six separate objects, each centered in its own cell, wide gaps, straight front view, warm lamp light from the upper left:
1 a long horizontal wooden wall shelf plank (honey wood, visible grain, small metal brackets underneath), very wide and thin so it can be stretched;
2 a small brass label plate for the shelf edge, rounded corners, two tiny screws, blank engraving area filled with flat grey #808080;
3 a vintage desk lamp with a warm glowing shade;
4 a small potted plant in a terracotta pot;
5 a ceramic mug with a little steam;
6 a small wooden stand / tray for 2-3 games lying on a desk (empty).
No text, no logos.
```

## После генерации
Пришли листы как есть — вырежу, уберу белый фон, проверю на тёмном фоне. Места #808080 станут окнами для обложек и подписей.
