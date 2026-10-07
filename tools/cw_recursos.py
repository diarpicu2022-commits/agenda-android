"""Recursos de CampusWatch generados desde el sistema de diseño (tema «reloj»). Idempotente.
Uso: python tools/cw_recursos.py <carpeta agenda-design-system> <ttf Sofia Sans Condensed>"""
import json, os, re, shutil, sys

ds, fuente = sys.argv[1], sys.argv[2]
R = 'wear/src/main/res'
for d in ('values', 'font', 'drawable', 'mipmap-anydpi'):
    os.makedirs(f'{R}/{d}', exist_ok=True)

# Colores: tema «reloj» de tokens.json + alias de urgencia de tokens.css (var(--x) → el color del tema reloj).
t = json.load(open(f'{ds}/tokens.json', encoding='utf8'))
cols = {c['name']: c['value']['reloj'] for c in t['color']['tokens'] if isinstance(c.get('value'), dict) and 'reloj' in c['value']}
css = open(f'{ds}/tokens.css', encoding='utf8').read()
bloque = css[css.index('[data-theme="reloj"]'):]
bloque = bloque[:bloque.index('}')]
for nombre, valor in re.findall(r'--(urg-[a-z]+):\s*([^;]+);', bloque):
    m = re.match(r'var\(--([a-z-]+)\)', valor.strip())
    cols[nombre] = cols[m.group(1)] if m else valor.strip()
lineas = ['<?xml version="1.0" encoding="utf-8"?>', '<!-- GENERADO desde tokens.json / tokens.css (tema «reloj»). No editar a mano. -->', '<resources>']
for n, v in cols.items():
    lineas.append(f'    <color name="cw_{n.replace("-", "_")}">{v}</color>')
lineas.append('</resources>')
open(f'{R}/values/cw_colores.xml', 'w', encoding='utf8').write('\n'.join(lineas) + '\n')

# Kotlin: los mismos colores para Compose y Tiles (un solo origen).
kt = ['package com.dpinta.agenda.wear', '', '// GENERADO por tools/cw_recursos.py desde tokens.json (tema «reloj»). No editar a mano.',
      '', 'object CW {']
for n, v in cols.items():
    partes = n.split('-')
    nombre = partes[0] + ''.join(p.capitalize() for p in partes[1:])
    kt.append(f'    const val {nombre}: Long = 0xFF{v.lstrip("#").upper()}')
kt.append('}')
os.makedirs('wear/src/main/java/com/dpinta/agenda/wear', exist_ok=True)
open('wear/src/main/java/com/dpinta/agenda/wear/ColoresReloj.kt', 'w', encoding='utf8').write('\n'.join(kt) + '\n')

shutil.copy(fuente, f'{R}/font/sofia_sans_condensed.ttf')

open(f'{R}/values/strings.xml', 'w', encoding='utf8').write('''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">CampusWatch</string>
    <string name="tile_label">Próxima salida</string>
    <string name="complication_label">Minutos para salir</string>
</resources>
''')

# Ícono de salida (⟶, «Salir ahora» en 02-urgencia.md): trazo redondeado, la misma familia que los íconos de Agenda.
open(f'{R}/drawable/ic_salida.xml', 'w', encoding='utf8').write('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FFFFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round" android:fillColor="#00000000"
        android:pathData="M4,12 H19 M14,7 L19,12 L14,17" />
</vector>
''')

# Ícono de la app: el de Agenda (misma marca), con su fondo.
app = 'app/src/main/res'
for f in ('drawable/ic_launcher_foreground.xml', 'drawable/ic_launcher_monochrome.xml', 'mipmap-anydpi/ic_launcher.xml'):
    shutil.copy(f'{app}/{f}', f'{R}/{f}')
fondo = re.search(r'<color name="ic_launcher_background">([^<]+)</color>', ''.join(open(os.path.join(app, 'values', x), encoding='utf8').read() for x in os.listdir(f'{app}/values') if x.endswith('.xml')))
open(f'{R}/values/ic_launcher.xml', 'w', encoding='utf8').write(
    f'<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">{fondo.group(1) if fondo else cols["fondo"]}</color>\n</resources>\n')
print(len(cols), 'colores')
