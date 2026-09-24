"""Mide las capturas del paso 4 sobre el render y copia las válidas a docs/capturas/paso4-*.png.

Uso: python medir_paso4.py <carpeta_salida> <densidad_fisica_dpi> <carpeta_docs>

- Banda (resource-id banda-salida): contraste del trazo real frente al fondo, AAA (≥ 7) (C9.1).
- Textos y filas expuestos a accesibilidad: AA (≥ 4,5); las filas de la lista llevan horas:
  AAA (≥ 7), también las terminadas (enmienda 2026-09-23, C9.1).
- Marca «✓» (C6, enmienda 2026-09-23): en las filas terminadas, la zona del hilo a la altura
  de la hora tiene bastante más tinta que la misma zona en una fila sin terminar (solo el hilo).
- Solape (C10, enmienda 2026-09-23): en las capturas «cruce», la fila de Física anuncia
  «Conflicto: Se cruza con Cálculo diferencial 8:00» (o «8:00 a. m.» en 12 h).
- Barra: cada pestaña y Crear ≥ 48 × 48 dp (C9.2), medido con los bounds del volcado.
"""
import glob
import os
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from collections import Counter

from PIL import Image

carpeta, densidad, docs = sys.argv[1], float(sys.argv[2]), sys.argv[3]


def lum(c):
    r = []
    for x in c:
        x /= 255
        r.append(x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4)
    return .2126 * r[0] + .7152 * r[1] + .0722 * r[2]


def contraste(a, b):
    a, b = sorted([lum(a), lum(b)], reverse=True)
    return (a + .05) / (b + .05)


def par(im, caja, minimo):
    px = list(im.crop(caja).get_flattened_data())
    cnt = Counter(px)
    fondo = cnt.most_common(1)[0][0]
    cands = [(contraste(c, fondo), c) for c, k in cnt.items() if k >= minimo and c != fondo]
    if not cands:
        return None, None, fondo
    r, c = max(cands)
    return r, c, fondo


hx = lambda c: '#%02X%02X%02X' % c
limites = lambda n: tuple(map(int, re.findall(r'\d+', n.get('bounds'))))
fallos = 0
avisos = []

for xml in sorted(glob.glob(os.path.join(carpeta, '*.xml'))):
    nombre = os.path.basename(xml)[:-4]
    if nombre in ('a', 'b'):
        continue
    im = Image.open(xml[:-4] + '.png').convert('RGB')
    raiz = ET.parse(xml)
    # A 360 dp se forzó la densidad para que el ancho mida 360 dp; si no, la física del teléfono.
    px_por_dp = (im.width / 360) if '360dp' in nombre else densidad / 160
    lineas = [f'— {nombre}']
    # Todo lo que queda bajo la barra inferior no se ve: se recorta contra su borde superior.
    barra = [limites(n) for n in raiz.iter('node') if (n.get('resource-id') or '') == 'barra-inferior']
    tope = barra[0][1] if barra else im.height
    no_medidos = 0

    for n in raiz.iter('node'):
        rid = n.get('resource-id') or ''
        texto = n.get('text') or n.get('content-desc') or ''
        caja = limites(n)
        if caja[3] - caja[1] < 8 or caja[2] - caja[0] < 8:
            continue
        dentro_de_barra = barra and caja[1] >= tope
        if not dentro_de_barra and caja[3] > tope:
            visible = max(0, tope - caja[1])
            if visible < 0.6 * (caja[3] - caja[1]):
                no_medidos += 1
                continue
            caja = (caja[0], caja[1], caja[2], tope)
        if rid == 'banda-salida':
            r, fg, bg = par(im, (caja[0], caja[1] + int(40 * px_por_dp), caja[2], caja[3]), 200)
            ok = r is not None and r >= 7
            fallos += not ok
            lineas.append(f'   banda {r:.2f} {"AAA" if ok else "FALLA"} {hx(fg)} sobre {hx(bg)}')
        elif rid.startswith('pestana-') or rid == 'boton-crear':
            w = (caja[2] - caja[0]) / px_por_dp
            h = (caja[3] - caja[1]) / px_por_dp
            ok = w >= 48 and h >= 48
            fallos += not ok
            lineas.append(f'   {rid}: {w:.0f} × {h:.0f} dp {"OK" if ok else "FALLA (< 48 dp)"}')
        elif texto.strip() and n.get('class', '').endswith(('TextView', 'View')):
            fila_con_hora = re.match(r'^\d{1,2}:\d{2}', texto) is not None
            if fila_con_hora:
                # El hilo del día (columna de 16 dp) es un gráfico (3:1), no texto: se excluye.
                caja = (caja[0] + int(16 * px_por_dp), caja[1], caja[2], caja[3])
            r, fg, bg = par(im, caja, 12)
            if r is None:
                continue
            umbral = 7 if fila_con_hora else 4.5
            if r < umbral:
                fallos += 1
                lineas.append(f'   FALLA {r:.2f} < {umbral} «{texto[:50]}» {hx(fg)} sobre {hx(bg)}')
    # --- marca «✓» de sesión terminada: tinta en la zona del hilo, fila terminada frente a no terminada
    filas = [(n.get('content-desc') or '', limites(n)) for n in raiz.iter('node')
             if re.match(r'^\d{1,2}:\d{2}', n.get('content-desc') or '')]

    def tinta_en_hilo(caja):
        zona = im.crop((caja[0], caja[1] + int(16 * px_por_dp), caja[0] + int(16 * px_por_dp), caja[1] + int(32 * px_por_dp)))
        px = list(zona.get_flattened_data())
        fondo = Counter(px).most_common(1)[0][0]
        return sum(1 for c in px if contraste(c, fondo) >= 4.5)

    hechas = [f for f in filas if 'terminada' in f[0]]
    pendientes = [f for f in filas if 'terminada' not in f[0]]
    if hechas and pendientes:
        con_marca = min(tinta_en_hilo(f[1]) for f in hechas)
        sin_marca = max(tinta_en_hilo(f[1]) for f in pendientes)
        ok = con_marca >= 1.8 * max(sin_marca, 1)
        fallos += not ok
        lineas.append(f'   marca ✓: {con_marca} px de tinta en el hilo (terminada) frente a {sin_marca} (sin terminar) {"OK" if ok else "FALLA"}')

    # --- texto de solape (C10, enmienda 2026-09-23)
    if 'cruce' in nombre:
        fisica = [t for t, _ in filas if 'Física' in t]
        ok = bool(fisica) and re.search(r'Conflicto: Se cruza con Cálculo diferencial 8:00( a\. m\.)?$', fisica[0]) is not None
        fallos += not ok
        lineas.append(f'   solape: «{fisica[0] if fisica else "(sin fila de Física)"}» {"OK" if ok else "FALLA"}')

    if no_medidos:
        lineas.append(f'   {no_medidos} nodos sin medir: visibles < 60 % de su alto (bajo la barra)')
    print('\n'.join(lineas))
    shutil.copy(xml[:-4] + '.png', os.path.join(docs, f'paso4-{nombre}.png'))

medidas = [x for x in glob.glob(os.path.join(carpeta, '*.xml')) if os.path.basename(x) not in ('a.xml', 'b.xml')]
print()
print(f'capturas medidas: {len(medidas)}  fallos: {fallos}')
for a in avisos:
    print('AVISO', a)
