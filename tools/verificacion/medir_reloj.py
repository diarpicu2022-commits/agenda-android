"""Mide CampusWatch sobre el render del emulador (no sobre los tokens):
- contraste de cada texto: color de tinta (el más alejado del fondo dentro de su caja) frente al fondo (el más frecuente);
- toques: todo nodo clicable ≥ 48 dp por lado (size-toque-reloj);
- textos: que ninguno se salga del círculo de la esfera.
Uso: python tools/verificacion/medir_reloj.py <nombre> (deja la captura en docs/capturas/cw-medida-<nombre>.png)"""
import io, math, os, re, subprocess, sys
from collections import Counter
from PIL import Image

ADB = os.path.expandvars(r'%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe')
def adb(*a, binario=False):
    r = subprocess.run([ADB, *a], capture_output=True)
    return r.stdout if binario else r.stdout.decode('utf8', 'replace')

nombre = sys.argv[1]
# Dos volcados seguidos deben coincidir (la UI ya terminó de dibujarse).
def volcado():
    adb('shell', 'uiautomator', 'dump', '/sdcard/u.xml')
    return adb('shell', 'cat', '/sdcard/u.xml')
a, b = volcado(), volcado()
estable = re.sub(r'bounds="[^"]*"', '', a) == re.sub(r'bounds="[^"]*"', '', b)
img = Image.open(io.BytesIO(adb('exec-out', 'screencap', '-p', binario=True))).convert('RGB')
img.save(f'docs/capturas/cw-medida-{nombre}.png')
densidad = int(re.search(r'(\d+)', adb('shell', 'wm', 'density')).group(1)) / 160
W, H = img.size

def lum(c):
    def ch(v):
        v /= 255
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    r, g, b = c
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)
def ratio(c1, c2):
    l1, l2 = sorted((lum(c1), lum(c2)), reverse=True)
    return (l1 + 0.05) / (l2 + 0.05)

fallos, filas = [], []
for n in re.findall(r'<node [^>]*>', b):
    texto = re.search(r' text="([^"]*)"', n).group(1)
    clic = 'clickable="true"' in n
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', re.search(r'bounds="([^"]*)"', n).group(1)))
    if clic and 'android.view.View' in n or clic and texto == '':
        wdp, hdp = (x2 - x1) / densidad, (y2 - y1) / densidad
        if (wdp < 47.5 or hdp < 47.5) and (x2 - x1) < W:
            fallos.append(f'toque {wdp:.0f}×{hdp:.0f} dp < 48')
    if not texto:
        continue
    zona = img.crop((x1, y1, x2, y2))
    px = list(zona.getdata())
    fondo = Counter(px).most_common(1)[0][0]
    tinta = max(px, key=lambda c: abs(lum(c) - lum(fondo)))
    r = ratio(tinta, fondo)
    # Fuera de la esfera: alguna esquina interior del texto más allá del radio.
    cx, cy, R = W / 2, H / 2, W / 2
    fuera = any(math.hypot(px_ - cx, py_ - cy) > R for px_, py_ in ((x1 + 4, (y1 + y2) / 2), (x2 - 4, (y1 + y2) / 2)))
    filas.append(f'{r:5.2f}:1  {texto[:34]}' + ('  ← FUERA DE LA ESFERA' if fuera else ''))
    if r < 4.5:
        fallos.append(f'contraste {r:.2f}:1 en «{texto}»')
    if fuera:
        fallos.append(f'«{texto}» se sale de la esfera')
print(f'[{nombre}] estable={estable} · {len(filas)} textos · densidad {densidad:.2f}')
for f in filas:
    print('   ', f)
print('    FALLOS:', fallos or 'ninguno')
