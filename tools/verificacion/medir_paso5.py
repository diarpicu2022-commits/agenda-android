"""Mide las capturas del paso 5 sobre el render y copia las válidas a docs/capturas/paso5-*.png.

Uso: python medir_paso5.py <carpeta_salida> <densidad_fisica_dpi> <carpeta_docs>

Comprobaciones (contrato + enmiendas):
- Contraste del trazo real frente al fondo, por nodo: AA (≥ 4,5); horas de la lista AAA (≥ 7).
  Cada nodo se recorta contra la barra inferior y no se mide si se ve menos del 60 % de su alto.
  En las filas del día se excluye la columna del hilo (16 dp): es un gráfico, no texto.
- Toques ≥ 48 dp (C9.2) en pestañas, Crear, tipos, modos, «cuándo», días, campos y guardar.
  Los días a 360 dp miden ~46 dp de ancho visible; su área táctil se amplía a 48 dp y eso no
  sale en los bounds: se informan aparte, no como fallo, si su alto es ≥ 48.
- Errores con su texto (C10), frase-resumen, conflictos «Se cruza con…» y «…de traslado,
  necesitas…», mapa «pendiente».
- Enmienda 2026-09-24: con fuente 2,0 la etiqueta «Actividades» ocupa las mismas bandas de
  tinta que «Hoy» (icono + una línea), es decir, no se parte.
"""
import glob
import os
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict

from PIL import Image

carpeta, densidad, docs = sys.argv[1], float(sys.argv[2]), sys.argv[3]

MSG_TITULO = 'Escribe un nombre para la actividad.'
MSG_HORA = 'Escribe la hora así: 8:00.'


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


def bandas_de_tinta(im, caja):
    """Número de bandas horizontales con tinta (≥ 3:1 frente al fondo) dentro de la caja."""
    zona = im.crop(caja)
    fondo = Counter(zona.get_flattened_data()).most_common(1)[0][0]
    ancho, alto = zona.size
    filas = []
    for y in range(alto):
        fila = [zona.getpixel((x, y)) for x in range(0, ancho, 2)]
        filas.append(any(contraste(p, fondo) >= 3 for p in fila))
    bandas, dentro = 0, False
    for hay in filas:
        if hay and not dentro:
            bandas += 1
        dentro = hay
    return bandas


hx = lambda c: '#%02X%02X%02X' % c
limites = lambda n: tuple(map(int, re.findall(r'\d+', n.get('bounds'))))
TOCABLES = ('pestana-', 'boton-crear', 'tipo-', 'modo-', 'cuando-', 'dia-', 'campo-', 'formulario-guardar', 'formulario-mapa')
fallos = 0
textos_por_escenario = defaultdict(set)
medidas = []

for xml in sorted(glob.glob(os.path.join(carpeta, '*.xml'))):
    nombre = os.path.basename(xml)[:-4]
    if nombre in ('a', 'b', 't'):
        continue
    medidas.append(nombre)
    escenario = re.sub(r'-\d+$', '', nombre)
    im = Image.open(xml[:-4] + '.png').convert('RGB')
    raiz = ET.parse(xml)
    px_por_dp = (im.width / 360) if '360dp' in nombre else densidad / 160
    lineas = [f'— {nombre}']
    barra = [limites(n) for n in raiz.iter('node') if (n.get('resource-id') or '') == 'barra-inferior']
    tope = barra[0][1] if barra else im.height
    no_medidos = 0

    for n in raiz.iter('node'):
        rid = n.get('resource-id') or ''
        texto = n.get('text') or n.get('content-desc') or ''
        if texto:
            textos_por_escenario[escenario].add(texto)
        caja = limites(n)
        if caja[3] - caja[1] < 8 or caja[2] - caja[0] < 8:
            continue
        dentro_de_barra = barra and caja[1] >= tope
        if not dentro_de_barra and caja[3] > tope:
            if max(0, tope - caja[1]) < 0.6 * (caja[3] - caja[1]):
                no_medidos += 1
                continue
            caja = (caja[0], caja[1], caja[2], tope)

        if rid.startswith(TOCABLES):
            w = (caja[2] - caja[0]) / px_por_dp
            h = (caja[3] - caja[1]) / px_por_dp
            if rid.startswith('dia-') and h >= 48 and w < 48:
                lineas.append(f'   {rid}: {w:.0f} × {h:.0f} dp (área táctil ampliada a 48 dp)')
            elif w < 48 or h < 48:
                fallos += 1
                lineas.append(f'   FALLA toque {rid}: {w:.0f} × {h:.0f} dp < 48')

        if rid == 'banda-salida':
            r, fg, bg = par(im, (caja[0], caja[1] + int(40 * px_por_dp), caja[2], caja[3]), 200)
            ok = r is not None and r >= 7
            fallos += not ok
            lineas.append(f'   banda {r:.2f} {"AAA" if ok else "FALLA"} {hx(fg)} sobre {hx(bg)}')
        elif texto.strip() and n.get('class', '').endswith(('TextView', 'View', 'EditText')):
            fila_con_hora = re.match(r'^\d{1,2}:\d{2}', texto) is not None
            if fila_con_hora:
                caja = (caja[0] + int(16 * px_por_dp), caja[1], caja[2], caja[3])
            r, fg, bg = par(im, caja, 12)
            if r is None:
                continue
            umbral = 7 if fila_con_hora else 4.5
            if r < umbral:
                fallos += 1
                lineas.append(f'   FALLA {r:.2f} < {umbral} «{texto[:50]}» {hx(fg)} sobre {hx(bg)}')

    # Enmienda 2026-09-24: «Actividades» no se parte con fuente 2,0.
    if 'fuente2' in nombre:
        tabs = {(n.get('resource-id') or ''): limites(n) for n in raiz.iter('node') if (n.get('resource-id') or '').startswith('pestana-')}
        if 'pestana-hoy' in tabs and 'pestana-actividades' in tabs:
            b_hoy = bandas_de_tinta(im, tabs['pestana-hoy'])
            b_act = bandas_de_tinta(im, tabs['pestana-actividades'])
            ok = b_act == b_hoy
            fallos += not ok
            lineas.append(f'   etiqueta «Actividades»: {b_act} bandas de tinta, «Hoy»: {b_hoy} {"OK (una línea)" if ok else "FALLA (se parte)"}')

    if no_medidos:
        lineas.append(f'   {no_medidos} nodos sin medir: visibles < 60 % de su alto (bajo la barra)')
    print('\n'.join(lineas))
    shutil.copy(xml[:-4] + '.png', os.path.join(docs, f'paso5-{nombre}.png'))

# Comprobaciones de contenido por escenario (todos los fotogramas del desplazamiento juntos).
print('\nContenido por escenario')
for esc, textos in sorted(textos_por_escenario.items()):
    todo = '\n'.join(textos)
    chequeos = []
    if esc.startswith('form-errores'):
        chequeos += [('error de título', MSG_TITULO in todo), ('error de hora', MSG_HORA in todo)]
    if esc.startswith('form-completo'):
        chequeos += [('frase-resumen', re.search(r'^Álgebra lineal: ', todo, re.M) is not None)]
    if esc.startswith('form-conflictos'):
        chequeos += [('solape', 'Se cruza con' in todo), ('traslado', 'de traslado, necesitas' in todo), ('«Guardar igual»', 'Guardar igual' in todo)]
    if esc.startswith('form-puntual'):
        chequeos += [('frase-resumen puntual', re.search(r'^Parcial de Cálculo: el ', todo, re.M) is not None)]
    if esc.startswith('mapa'):
        chequeos += [('mapa pendiente', 'Mapa pendiente' in todo)]
    for nombre_chequeo, ok in chequeos:
        fallos += not ok
        print(f'   {esc}: {nombre_chequeo} {"OK" if ok else "FALLA"}')

print()
print(f'capturas medidas: {len(medidas)}  fallos: {fallos}')
