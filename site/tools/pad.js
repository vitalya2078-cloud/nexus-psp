/* Тестер геймпада: Gamepad API, стандартная раскладка (A B X Y, плечи, крестовина, стики). Ничего не отправляет. */
(function () {
  "use strict";
  var status = document.querySelector(".js-status"), name = document.querySelector(".js-name"), tested = document.querySelector(".js-tested");
  var btns = Array.prototype.slice.call(document.querySelectorAll("[data-b]")), seen = {}, rumble = document.querySelector(".js-rumble");
  var ls = document.querySelector(".js-ls"), rs = document.querySelector(".js-rs");
  var bars = [".js-a0", ".js-a1", ".js-a2", ".js-a3"].map(function (s) { return document.querySelector(s); });
  var l2 = document.querySelector(".js-l2"), r2 = document.querySelector(".js-r2");
  if (!navigator.getGamepads) { name.textContent = "Этот браузер не умеет работать с геймпадами — попробуй Chrome"; return; }
  function pad() { var g = navigator.getGamepads(); for (var i = 0; i < g.length; i++) if (g[i]) return g[i]; return null; }
  function loop() {
    var g = pad();
    status.classList.toggle("on", !!g);
    if (g) {
      name.textContent = "Подключён: " + (g.id.replace(/\(.*?\)/g, "").trim() || "геймпад") + (g.mapping === "standard" ? "" : " · своя раскладка, кнопки могут не совпасть");
      btns.forEach(function (el) {
        var i = +el.getAttribute("data-b"), b = g.buttons[i], on = !!(b && (b.pressed || b.value > .5));
        el.classList.toggle("on", on); if (on) seen[i] = true;
      });
      var ax = g.axes;
      ls.setAttribute("cx", 230 + (ax[0] || 0) * 24); ls.setAttribute("cy", 245 + (ax[1] || 0) * 24);
      rs.setAttribute("cx", 390 + (ax[2] || 0) * 24); rs.setAttribute("cy", 245 + (ax[3] || 0) * 24);
      bars.forEach(function (b, i) { b.style.width = (((ax[i] || 0) + 1) * 50) + "%"; });
      l2.style.width = ((g.buttons[6] && g.buttons[6].value) || 0) * 100 + "%";
      r2.style.width = ((g.buttons[7] && g.buttons[7].value) || 0) * 100 + "%";
      var n = Object.keys(seen).length;
      tested.textContent = n ? "Проверено кнопок: " + n + " из " + btns.length + (n >= btns.length ? " — всё работает!" : "") : "";
      rumble.hidden = !g.vibrationActuator;
    }
    requestAnimationFrame(loop);
  }
  rumble.addEventListener("click", function () {
    var g = pad(); if (g && g.vibrationActuator) g.vibrationActuator.playEffect("dual-rumble", { duration: 600, strongMagnitude: .8, weakMagnitude: .6 }).catch(function () {});
  });
  addEventListener("gamepadconnected", function () { if (navigator.vibrate) navigator.vibrate(30); });
  requestAnimationFrame(loop);
})();
