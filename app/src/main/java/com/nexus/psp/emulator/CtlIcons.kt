package com.nexus.psp.emulator

/** Набор значков KAPSULA Touch для выбора в редакторе: группа → (значок, подпись). Файлы — res/drawable/ctl_<значок>.xml. */
object CtlIcons {
    val GROUPS: List<Pair<String, List<Pair<String, String>>>> = listOf(
        "Основное" to listOf(
            "tap" to "Действие", "ok" to "Да", "back" to "Отмена", "menu" to "Меню", "talk" to "Поговорить", "skip" to "Пропустить",
            "grab" to "Взять", "drop" to "Бросить", "door" to "Войти", "climb" to "Лезть", "lever" to "Рычаг", "key" to "Открыть",
            "map" to "Карта", "bag" to "Вещи", "heal" to "Лечение", "potion" to "Зелье", "camreset" to "Камера", "lookback" to "Оглянуться",
        ),
        "Движение" to listOf(
            "jump" to "Прыжок", "sprint" to "Бег", "dash" to "Рывок", "crouch" to "Присесть", "sneak" to "Красться", "roll" to "Перекат",
            "cover" to "Укрытие", "swim" to "Плыть", "zoom" to "Бинокль", "light" to "Фонарь", "point" to "Указать", "slowmo" to "Замедление",
        ),
        "Бой" to listOf(
            "attack" to "Удар", "jab" to "Лёгкий", "heavy" to "Сильный", "kick" to "Нога", "throw" to "Бросок", "block" to "Блок",
            "special" to "Суперудар", "charge" to "Сила", "skill" to "Умение", "magic" to "Магия", "sword" to "Меч", "knife" to "Нож",
            "bow" to "Лук", "lock" to "Цель", "combo" to "Комбо",
        ),
        "Стрельба" to listOf(
            "aim" to "Прицел", "shoot" to "Огонь", "reload" to "Перезарядка", "weapon" to "Оружие", "grenade" to "Граната", "swap" to "Смена",
        ),
        "Транспорт" to listOf(
            "gas" to "Газ", "brake" to "Тормоз", "hbrake" to "Ручник", "nitro" to "Нитро", "horn" to "Сигнал", "wheel" to "Руль",
            "car" to "Машина", "moto" to "Мотоцикл", "view" to "Вид", "lights" to "Фары", "mirror" to "Зеркало", "cam" to "Камера",
        ),
        "Полёты" to listOf(
            "jet" to "Самолёт", "heli" to "Вертолёт", "takeoff" to "Взлёт", "thr_up" to "Газ +", "thr_down" to "Газ −", "climb" to "Вверх",
            "dive" to "Вниз", "roll_l" to "Крен ←", "roll_r" to "Крен →", "yaw_l" to "Поворот ←", "yaw_r" to "Поворот →", "missile" to "Ракета",
            "mgun" to "Пулемёт", "bombs" to "Бомбы", "flares" to "Ловушки", "tgt_switch" to "Цель", "afterburner" to "Форсаж", "airbrake" to "Тормоз",
            "gear" to "Шасси", "cockpit" to "Кабина", "parachute" to "Парашют", "eject" to "Катапульта", "winch" to "Крюк", "radar" to "Радар",
        ),
        "Спорт" to listOf(
            "pass" to "Пас", "goal" to "Удар", "tackle" to "Подкат", "players" to "Игрок",
        ),
        "Кнопки" to listOf(
            "ft" to "Верх", "fr" to "Право", "fb" to "Низ", "fl" to "Лево", "l1" to "L1", "r1" to "R1", "l2" to "L2", "r2" to "R2",
            "select" to "Select", "start" to "Start", "arr_up" to "Вверх", "arr_down" to "Вниз", "arr_left" to "Влево", "arr_right" to "Вправо",
            "layout" to "Раскладка", "both" to "Две разом", "turbo" to "Турбо",
        ),
    )

    /** Группы значков для игры этой консоли: общие + «Классика» — родные кнопки её джойстика. */
    fun groups(plat: String): List<Pair<String, List<Pair<String, String>>>> = GROUPS + ("Классика" to ControlLayout.classicIcons(plat))

    /** Подпись под кнопкой по её значку (кнопки консоли и служебные — без подписи). */
    val CAPS: Map<String, String> by lazy { GROUPS.filter { it.first != "Кнопки" }.flatMap { it.second }.toMap() + mapOf("more" to "Ещё", "left" to "Влево", "right" to "Вправо", "camreset" to "Камера") }
}
