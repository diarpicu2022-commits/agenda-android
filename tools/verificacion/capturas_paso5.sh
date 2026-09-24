#!/usr/bin/env bash
# Capturas y mediciones del paso 5 (crear / editar actividad) en un dispositivo real.
# Uso:  bash capturas_paso5.sh -s <serial-adb>
#
# Igual que el del paso 4: cerrojo dispositivo.lock, estado del teléfono guardado y restaurado
# con trap, cada fotograma aceptado solo si dos volcados de uiautomator seguidos coinciden.
# Novedades: el formulario es largo, así que cada escenario se captura en varios fotogramas
# desplazando hasta el final; el mapa pendiente se abre tocando «Elegir en el mapa».
# Escenarios (extra de depuración «formulario»): nuevo, errores, completo, conflictos, puntual.
set -u

SERIAL=""
while getopts "s:" o; do case $o in s) SERIAL=$OPTARG ;; *) echo "uso: $0 -s <serial>"; exit 2 ;; esac; done
[ -z "$SERIAL" ] && { echo "uso: $0 -s <serial>"; exit 2; }

export MSYS_NO_PATHCONV=1
export PYTHONIOENCODING=utf-8
ADB_BIN="${LOCALAPPDATA}/Android/Sdk/platform-tools/adb.exe"
ADB() { "$ADB_BIN" -s "$SERIAL" "$@"; }
AQUI="$(cd "$(dirname "$0")" && pwd)"
SP="${SP:-$AQUI/../../build/verificacion}"; mkdir -p "$SP/p4" "$SP/p5"
OUT="$SP/p5/salida"
PROY="${PROY:-$(cd "$AQUI/../.." && pwd)}"
APK="$PROY/app/build/outputs/apk/debug/app-debug.apk"
DOCS="$PROY/docs/capturas"
LOCK="$SP/dispositivo.lock"
PKG="com.dpinta.agenda"
mkdir -p "$OUT" "$DOCS"

tomar() {
  for _ in $(seq 1 40); do
    if [ ! -f "$LOCK" ]; then printf "agenda" > "$LOCK"; sleep 1; [ "$(cat "$LOCK")" = "agenda" ] && return 0; fi
    [ "$(cat "$LOCK" 2>/dev/null)" = "agenda" ] && return 0
    echo "$(date +%H:%M:%S) dispositivo ocupado por: $(cat "$LOCK" 2>/dev/null); espero 30 s"; sleep 30
  done
  echo "No se pudo tomar el cerrojo en 20 min"; return 1
}
soltar() { [ "$(cat "$LOCK" 2>/dev/null)" = "agenda" ] && rm -f "$LOCK"; }

ORIG_NOCHE=""; ORIG_FS=""; ORIG_SIZE=""; ORIG_DENS=""
guardar() {
  ORIG_NOCHE=$(ADB shell cmd uimode night | tr -d '\r' | awk '{print $3}')
  ORIG_FS=$(ADB shell settings get system font_scale | tr -d '\r')
  ORIG_SIZE=$(ADB shell wm size | tr -d '\r' | awk -F': ' '/Override/{print $2}')
  ORIG_DENS=$(ADB shell wm density | tr -d '\r' | awk -F': ' '/Override/{print $2}')
  echo "original: noche=$ORIG_NOCHE font_scale=$ORIG_FS size_override=${ORIG_SIZE:-no} density_override=${ORIG_DENS:-no}"
}
restaurar() {
  echo "restaurando el teléfono…"
  [ -n "$ORIG_NOCHE" ] && ADB shell cmd uimode night "$ORIG_NOCHE" >/dev/null
  if [ "$ORIG_FS" = "null" ] || [ -z "$ORIG_FS" ]; then ADB shell settings delete system font_scale >/dev/null; else ADB shell settings put system font_scale "$ORIG_FS"; fi
  if [ -n "$ORIG_DENS" ]; then ADB shell wm density "$ORIG_DENS"; else ADB shell wm density reset; fi
  if [ -n "$ORIG_SIZE" ]; then ADB shell wm size "$ORIG_SIZE"; else ADB shell wm size reset; fi
  ADB shell am force-stop "$PKG"
  soltar
  echo "estado final: $(ADB shell cmd uimode night | tr -d '\r'), font_scale=$(ADB shell settings get system font_scale | tr -d '\r'), $(ADB shell wm density | tr -d '\r' | tr '\n' ' ')"
}

volcar() { ADB shell uiautomator dump /sdcard/agenda-ui.xml >/dev/null 2>&1; ADB pull /sdcard/agenda-ui.xml "$1" >/dev/null 2>&1; }
pantalla_bloqueada() { ADB shell dumpsys window | tr -d '\r' | grep -qE "mDreamingLockscreen=true|isKeyguardShowing=true|mShowingLockscreen=true"; }

# fotograma <nombre>: captura el estado actual si es estable y es de la agenda.
fotograma() {
  local nombre=$1
  for intento in 1 2 3 4; do
    volcar "$OUT/a.xml"; ADB exec-out screencap -p > "$OUT/$nombre.png"; volcar "$OUT/b.xml"
    if cmp -s "$OUT/a.xml" "$OUT/b.xml" && grep -q "package=\"$PKG\"" "$OUT/a.xml"; then
      cp "$OUT/a.xml" "$OUT/$nombre.xml"; echo "  $nombre ok"; return 0
    fi
    echo "  $nombre inestable (intento $intento)"; sleep 1
  done
  echo "  $nombre FALLÓ"; return 1
}

# abrir <extra-clave> <valor> <noche>: arranca la app limpia con un extra de depuración.
abrir() {
  local clave=$1 valor=$2 noche=$3
  [ "$(ADB shell cmd uimode night | tr -d '\r')" = "Night mode: $noche" ] || { ADB shell cmd uimode night "$noche" >/dev/null; sleep 2; }
  ADB shell input keyevent KEYCODE_WAKEUP
  while pantalla_bloqueada; do echo "La pantalla está bloqueada: desbloquéala en el teléfono (reintento en 15 s)"; sleep 15; done
  ADB shell am force-stop "$PKG"
  ADB shell am start -W -n "$PKG/.MainActivity" --es "$clave" "$valor" --es hora 07:00 >/dev/null
  sleep 3
}

# recorrer <prefijo>: fotogramas desplazando el formulario hasta que dos seguidos coinciden
# (el final). Máximo 10: con 360 dp y fuente 2,0 el formulario necesita más de 5 (antes el tope
# de 5 dejaba la frase-resumen sin capturar). Si se llega al tope sin final, se avisa.
recorrer() {
  local prefijo=$1 i=1
  local alto; alto=$(ADB shell wm size | tr -d '\r' | awk -F'[: x]+' '/Physical/{print $4}')
  fotograma "$prefijo-$i" || return 1
  local final=no
  while [ $i -lt 10 ]; do
    ADB shell input swipe 540 $((alto * 70 / 100)) 540 $((alto * 30 / 100)) 600; sleep 1.5
    i=$((i + 1)); fotograma "$prefijo-$i" || return 1
    if cmp -s "$OUT/$prefijo-$i.xml" "$OUT/$prefijo-$((i - 1)).xml"; then rm -f "$OUT/$prefijo-$i".*; final=si; break; fi
  done
  [ $final = si ] || echo "  AVISO $prefijo: 10 fotogramas sin llegar al final del formulario"
  [ $final = si ] || touch "$OUT/$prefijo.sin-final"
}

# tocar_etiqueta <resource-id>: toca el centro de un nodo del último volcado estable.
tocar_etiqueta() {
  volcar "$OUT/t.xml"
  local centro; centro=$(python - "$OUT/t.xml" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).iter('node'):
    if n.get('resource-id') == sys.argv[2]:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
)
  [ -z "$centro" ] && { echo "  no encontré $1"; return 1; }
  ADB shell input tap $centro; sleep 2
}

tomar || exit 1
trap restaurar EXIT
guardar
echo "modelo: $(ADB shell getprop ro.product.model | tr -d '\r') · Android $(ADB shell getprop ro.build.version.release | tr -d '\r') · $(ADB shell wm size | tr -d '\r' | head -1)"
ADB install -r "$APK" | tail -1 || exit 1
rm -f "$OUT"/*.png "$OUT"/*.xml

echo "Tanda 1: escenarios del formulario, claro y oscuro"
for tema in claro oscuro; do n=no; [ $tema = oscuro ] && n=yes
  for esc in nuevo errores completo conflictos puntual; do
    abrir formulario "$esc" $n; recorrer "form-$esc-$tema"
  done
done

echo "Tanda 2: mapa pendiente"
for tema in claro oscuro; do n=no; [ $tema = oscuro ] && n=yes
  abrir formulario nuevo $n
  for _ in 1 2 3; do ADB shell input swipe 540 1600 540 900 400; sleep 0.8; done
  tocar_etiqueta formulario-mapa && fotograma "mapa-$tema"
done

echo "Tanda 3: formulario a 360 dp y con fuente 2,0; barra con fuente 2,0"
FIS_W=$(ADB shell wm size | tr -d '\r' | awk -F'[: x]+' '/Physical/{print $3}')
ADB shell wm density $(( FIS_W * 160 / 360 )); sleep 2
abrir formulario completo no; recorrer "form-completo-360dp-claro"
ADB shell settings put system font_scale 2.0; sleep 2
abrir formulario completo no; recorrer "form-completo-360dp-fuente2-claro"
abrir pestana hoy no; fotograma "barra-360dp-fuente2-claro"
if [ -n "$ORIG_DENS" ]; then ADB shell wm density "$ORIG_DENS"; else ADB shell wm density reset; fi; sleep 2
abrir pestana hoy no; fotograma "barra-fuente2-claro"
if [ "$ORIG_FS" = "null" ] || [ -z "$ORIG_FS" ]; then ADB shell settings delete system font_scale >/dev/null; else ADB shell settings put system font_scale "$ORIG_FS"; fi

DENS_ACTUAL=$(ADB shell wm density | tr -d '\r' | awk -F': ' '/Physical/{print $2}')
echo "Medición sobre el render"
python "$AQUI/medir_paso5.py" "$OUT" "$DENS_ACTUAL" "$DOCS"
