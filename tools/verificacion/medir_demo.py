"""Mide la pantalla actual de CampusWatch en el reloj conectado: que cada texto quepa dentro de la esfera y que cada
elemento tocable mida al menos 48 dp de alto. Uso: python tools/verificacion/medir_demo.py <nombre>"""
import math, os, re, subprocess, sys

ADB = os.path.expandvars(r'%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe')
def sh(*a): return subprocess.run([ADB, *a], capture_output=True, text=True, encoding='utf8').stdout

ancho = int(re.search(r'(\d+)x\d+', sh('shell', 'wm', 'size')).group(1))
dpi = int(re.search(r'(\d+)\s*$', sh('shell', 'wm', 'density').strip()).group(1))
dp = dpi / 160
r = c = ancho / 2
xml = sh('exec-out', 'uiautomator', 'dump', '/dev/tty')
fallas = 0
for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?clickable="(true|false)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    texto, toca, x1, y1, x2, y2 = m.group(1), m.group(2) == 'true', *map(int, m.groups()[2:])
    if not texto and not toca:
        continue
    # Texto: sus cuatro esquinas dentro del círculo. Tocable (pastilla): los extremos de su eje y el alto mínimo.
    puntos = [((x1 + x2) / 2, y1), ((x1 + x2) / 2, y2), (x1 + (y2 - y1) / 2, (y1 + y2) / 2), (x2 - (y2 - y1) / 2, (y1 + y2) / 2)] if toca \
        else [(x1, y1), (x2, y1), (x1, y2), (x2, y2)]
    fuera = max(math.hypot(px - c, py - c) - r for px, py in puntos)
    alto = (y2 - y1) / dp
    mal = fuera > 0 or (toca and alto < 47.5)
    fallas += mal
    print(f"{'FALLA' if mal else 'ok   '} {'toque' if toca else 'texto'} {texto or '(botón)'!r:44} alto {alto:5.1f} dp  margen {-fuera:6.1f} px")
print(f"{sys.argv[1] if len(sys.argv) > 1 else 'pantalla'}: {fallas} fallas")
sys.exit(1 if fallas else 0)
