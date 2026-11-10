#!/usr/bin/env bash
# Paso 3 del anexo 2026-11-09-campuswatch-claro: todas las pantallas de CampusWatch en claro y en oscuro, medidas sobre el
# render con medir_reloj.py (contraste, toques y círculo). Uso: bash tools/verificacion/recorrido_temas_reloj.sh
set -u
ADB() { MSYS_NO_PATHCONV=1 "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" "$@"; }
medir() { PYTHONIOENCODING=utf-8 python tools/verificacion/medir_reloj.py "$1" 2>/dev/null | grep -v -i deprecat | grep -v getdata; }
arriba() { ADB shell input swipe 227 380 227 80 250; sleep 3; }
for tema in claro oscuro; do
  for esc in proxima salir urgente parcial; do
    python -I tools/verificacion/reloj_dia.py $esc $tema >/dev/null; sleep 8; medir "p3-$tema-$esc"
  done
  # Horario: la primera página es la sesión del día; arriba vienen Acciones, Hoy y Mañana.
  python -I tools/verificacion/reloj_dia.py horario $tema >/dev/null; sleep 8; medir "p3-$tema-horario-0"
  for i in 1 2 3; do arriba; medir "p3-$tema-horario-$i"; done
  python -I tools/verificacion/reloj_dia.py vacio $tema >/dev/null; sleep 8; medir "p3-$tema-vacio"
  python -I tools/verificacion/reloj_dia.py sin-datos $tema >/dev/null; sleep 8; medir "p3-$tema-sin-datos"
  # Confirmación: «Ya voy» en Es hora de salir (sin teléfono emparejado sale «No llegó al teléfono»).
  python -I tools/verificacion/reloj_dia.py salir $tema >/dev/null; sleep 8
  b=$(ADB exec-out uiautomator dump /dev/tty 2>/dev/null | grep -ao 'text="Ya voy"[^>]*bounds="[^"]*"' | grep -o '\[[0-9,]*\]\[[0-9,]*\]' | grep -o '[0-9]\+' | tr '\n' ' ')
  if [ -n "$b" ]; then set -- $b; ADB shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); sleep 3; medir "p3-$tema-confirmacion"; else echo "[p3-$tema-confirmacion] no se encontró «Ya voy»"; fi
done
