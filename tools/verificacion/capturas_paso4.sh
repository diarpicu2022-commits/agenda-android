#!/usr/bin/env bash
# Capturas y mediciones del paso 4 (esqueleto de navegación) en un dispositivo real.
# Uso:  bash capturas_paso4.sh -s <serial-adb>
#       (el serial sale de `adb devices`; con depuración inalámbrica es del tipo 192.168.x.x:port)
#
# Qué hace, en tandas cortas y restaurando SIEMPRE el estado del teléfono (trap):
#   1. Toma el cerrojo dispositivo.lock ("agenda"); si lo tiene otro, espera cada 30 s (máx. 20 min).
#   2. Guarda modo nocturno, font_scale, wm size/density y time_12_24 originales.
#   3. Instala app-debug.apk y captura (hora fija con el extra de depuración «hora»):
#      Hoy 06:51 / 07:31 / 10:05 / 21:40, Semana y Actividades en claro y oscuro;
#      Hoy a 360 dp (wm density) y con escala de fuente 2,0.
#      Cada captura solo se acepta si dos volcados de uiautomator seguidos son idénticos y son
#      de com.dpinta.agenda.
#   4. Mide sobre el render (medir_paso4.py): contraste (horas de la lista AAA, también las
#      pasadas), toques de la barra ≥ 48 dp, marca «✓» de sesión terminada dibujada sobre el hilo
#      y texto de solape «Se cruza con {actividad} {hora}» (enmiendas 2026-09-23).
#   5. Restaura el teléfono y suelta el cerrojo.
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
OUT="$SP/p4/salida"
PROY="${PROY:-$(cd "$AQUI/../.." && pwd)}"
APK="$PROY/app/build/outputs/apk/debug/app-debug.apk"
DOCS="$PROY/docs/capturas"
LOCK="$SP/dispositivo.lock"
PKG="com.dpinta.agenda"
mkdir -p "$OUT" "$DOCS"

# ---------- cerrojo ----------
tomar() {
  for _ in $(seq 1 40); do
    if [ ! -f "$LOCK" ]; then printf "agenda" > "$LOCK"; sleep 1; [ "$(cat "$LOCK")" = "agenda" ] && return 0; fi
    [ "$(cat "$LOCK" 2>/dev/null)" = "agenda" ] && return 0
    echo "$(date +%H:%M:%S) dispositivo ocupado por: $(cat "$LOCK" 2>/dev/null); espero 30 s"; sleep 30
  done
  echo "No se pudo tomar el cerrojo en 20 min"; return 1
}
soltar() { [ "$(cat "$LOCK" 2>/dev/null)" = "agenda" ] && rm -f "$LOCK"; }

# ---------- estado original y restauración ----------
ORIG_NOCHE=""; ORIG_FS=""; ORIG_TIME=""; ORIG_SIZE=""; ORIG_DENS=""
guardar() {
  ORIG_NOCHE=$(ADB shell cmd uimode night | tr -d '\r' | awk '{print $3}')      # yes | no | auto
  ORIG_FS=$(ADB shell settings get system font_scale | tr -d '\r')
  ORIG_TIME=$(ADB shell settings get system time_12_24 | tr -d '\r')
  ORIG_SIZE=$(ADB shell wm size | tr -d '\r' | awk -F': ' '/Override/{print $2}')
  ORIG_DENS=$(ADB shell wm density | tr -d '\r' | awk -F': ' '/Override/{print $2}')
  echo "original: noche=$ORIG_NOCHE font_scale=$ORIG_FS time_12_24=$ORIG_TIME size_override=${ORIG_SIZE:-no} density_override=${ORIG_DENS:-no}"
}
restaurar() {
  echo "restaurando el teléfono…"
  [ -n "$ORIG_NOCHE" ] && ADB shell cmd uimode night "$ORIG_NOCHE" >/dev/null
  [ -n "$ORIG_FS" ] && [ "$ORIG_FS" != "null" ] && ADB shell settings put system font_scale "$ORIG_FS"
  [ "$ORIG_FS" = "null" ] && ADB shell settings delete system font_scale >/dev/null
  if [ -n "$ORIG_DENS" ]; then ADB shell wm density "$ORIG_DENS"; else ADB shell wm density reset; fi
  if [ -n "$ORIG_SIZE" ]; then ADB shell wm size "$ORIG_SIZE"; else ADB shell wm size reset; fi
  if [ "$ORIG_TIME" = "null" ] || [ -z "$ORIG_TIME" ]; then ADB shell settings delete system time_12_24 >/dev/null; else ADB shell settings put system time_12_24 "$ORIG_TIME"; fi
  ADB shell am force-stop "$PKG"
  soltar
  echo "estado final: $(ADB shell cmd uimode night | tr -d '\r'), font_scale=$(ADB shell settings get system font_scale | tr -d '\r'), $(ADB shell wm density | tr -d '\r' | tr '\n' ' ')"
}

# ---------- captura verificada ----------
volcar() { ADB shell uiautomator dump /sdcard/agenda-ui.xml >/dev/null 2>&1; ADB pull /sdcard/agenda-ui.xml "$1" >/dev/null 2>&1; }
pantalla_bloqueada() { ADB shell dumpsys window | tr -d '\r' | grep -qE "mDreamingLockscreen=true|isKeyguardShowing=true|mShowingLockscreen=true"; }

# capturar <nombre> <pestana hoy|semana|actividades> <hora HH:mm> <noche yes|no> [ejemplo]
capturar() {
  local nombre=$1 pestana=$2 hora=$3 noche=$4 ejemplo=${5:-normal}
  [ "$(ADB shell cmd uimode night | tr -d '\r')" = "Night mode: $noche" ] || { ADB shell cmd uimode night "$noche" >/dev/null; sleep 2; }
  for intento in 1 2 3 4; do
    ADB shell input keyevent KEYCODE_WAKEUP
    if pantalla_bloqueada; then echo "La pantalla está bloqueada: desbloquéala en el teléfono. Reintento en 15 s"; sleep 15; continue; fi
    ADB shell am force-stop "$PKG"
    ADB shell am start -W -n "$PKG/.MainActivity" --es pestana "$pestana" --es hora "$hora" --es ejemplo "$ejemplo" >/dev/null
    sleep 3
    volcar "$OUT/a.xml"; ADB exec-out screencap -p > "$OUT/$nombre.png"; volcar "$OUT/b.xml"
    if cmp -s "$OUT/a.xml" "$OUT/b.xml" && grep -q "package=\"$PKG\"" "$OUT/a.xml" \
       && [ "$(ADB shell cmd uimode night | tr -d '\r')" = "Night mode: $noche" ]; then
      cp "$OUT/a.xml" "$OUT/$nombre.xml"; echo "  $nombre ok"; return 0
    fi
    echo "  $nombre inestable (intento $intento)"; sleep 1
  done
  echo "  $nombre FALLÓ"; return 1
}

# ---------- programa ----------
tomar || exit 1
trap restaurar EXIT
guardar
echo "modelo: $(ADB shell getprop ro.product.model | tr -d '\r') · Android $(ADB shell getprop ro.build.version.release | tr -d '\r') · $(ADB shell wm size | tr -d '\r' | head -1) · $(ADB shell wm density | tr -d '\r' | head -1)"
ADB install -r "$APK" | tail -1 || exit 1
rm -f "$OUT"/*.png "$OUT"/*.xml

echo "Tanda 1: tres destinos, claro y oscuro (Hoy a las 06:51)"
for tema in claro oscuro; do n=no; [ $tema = oscuro ] && n=yes
  capturar "hoy-$tema"          hoy         06:51 $n
  capturar "semana-$tema"       semana      06:51 $n
  capturar "actividades-$tema"  actividades 06:51 $n
done

echo "Tanda 2: Hoy a otras horas (sal ya, sin traslado, sin banda)"
for tema in claro oscuro; do n=no; [ $tema = oscuro ] && n=yes
  capturar "hoy-0731-$tema" hoy 07:31 $n
  capturar "hoy-1005-$tema" hoy 10:05 $n
  capturar "hoy-2140-$tema" hoy 21:40 $n
done

echo "Tanda 3: Hoy a 360 dp"
FIS_W=$(ADB shell wm size | tr -d '\r' | awk -F'[: x]+' '/Physical/{print $3}')
ADB shell wm density $(( FIS_W * 160 / 360 )); sleep 2
capturar "hoy-360dp-claro"  hoy 06:51 no
capturar "hoy-360dp-oscuro" hoy 06:51 yes
if [ -n "$ORIG_DENS" ]; then ADB shell wm density "$ORIG_DENS"; else ADB shell wm density reset; fi; sleep 2

echo "Tanda 4: Hoy con escala de fuente 2,0"
ADB shell settings put system font_scale 2.0; sleep 2
capturar "hoy-fuente2-claro"  hoy 06:51 no
capturar "hoy-fuente2-oscuro" hoy 06:51 yes
[ "$ORIG_FS" = "null" ] && ADB shell settings delete system font_scale >/dev/null || ADB shell settings put system font_scale "${ORIG_FS:-1.0}"

echo "Tanda 5: enmiendas 2026-09-23 (marca ✓ a las 10:05 ya capturada; solape con el ejemplo «cruce»)"
capturar "hoy-cruce-claro"  hoy 07:00 no  cruce
capturar "hoy-cruce-oscuro" hoy 07:00 yes cruce

DENS_ACTUAL=$(ADB shell wm density | tr -d '\r' | awk -F': ' '/Physical/{print $2}')
echo "Medición sobre el render"
python "$AQUI/medir_paso4.py" "$OUT" "$DENS_ACTUAL" "$DOCS"
