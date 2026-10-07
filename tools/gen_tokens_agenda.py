"""Genera ui/theme/Tokens.kt desde el sistema de diseño Agenda + CampusWatch (tokens.json + alias de tokens.css).
Uso: python tools/gen_tokens_agenda.py <carpeta agenda-design-system>"""
import json, re, sys

ds = sys.argv[1]
t = json.load(open(f'{ds}/tokens.json', encoding='utf8'))
css = open(f'{ds}/tokens.css', encoding='utf8').read()

def bloque(selector):
    i = css.index(selector)
    return css[i:css.index('}', i)]

temas = {'claro': ':root', 'noche': '[data-theme="noche"]'}
valores = {}
for tema in temas:
    v = {c['name']: c['value'][tema] for c in t['color']['tokens'] if isinstance(c.get('value'), dict) and tema in c['value']}
    for nombre, valor in re.findall(r'--([a-z-]+):\s*([^;]+);', bloque(temas[tema])):
        if nombre in v or not (nombre.startswith('urg-') or nombre.startswith('linea-') or nombre in ('info',)):
            continue
        m = re.match(r'var\(--([a-z-]+)\)', valor.strip())
        v[nombre] = v[m.group(1)] if m else valor.strip()
    valores[tema] = v

nombres = [n for n in valores['claro'] if n in valores['noche']]
def camel(n):
    p = n.split('-')
    return p[0] + ''.join(x.capitalize() for x in p[1:])
def hexa(v):
    v = v.strip().lstrip('#')
    if len(v) == 3:
        v = ''.join(c * 2 for c in v)
    return '0xFF' + v.upper()

out = ['package com.dpinta.agenda.ui.theme', '',
       'import androidx.compose.runtime.Immutable', 'import androidx.compose.ui.graphics.Color', '',
       '// GENERADO por tools/gen_tokens_agenda.py desde el sistema de diseño Agenda + CampusWatch',
       '// (tokens.json y alias de tokens.css). No editar a mano: se regenera.', '',
       '/** Colores del sistema de diseño, tema «claro» y «noche» (README · Color). */',
       '@Immutable', 'data class AgendaDs(']
for n in nombres:
    out.append(f'    val {camel(n)}: Color,')
out.append('    val esNoche: Boolean,')
out.append(')')
for tema, nombre_val in (('claro', 'AgendaDsClaro'), ('noche', 'AgendaDsNoche')):
    out.append('')
    out.append(f'val {nombre_val} = AgendaDs(')
    for n in nombres:
        out.append(f'    {camel(n)} = Color({hexa(valores[tema][n])}),')
    out.append(f'    esNoche = {"true" if tema == "noche" else "false"},')
    out.append(')')
open('app/src/main/java/com/dpinta/agenda/ui/theme/Tokens.kt', 'w', encoding='utf8').write('\n'.join(out) + '\n')
print(len(nombres), 'colores ·', ', '.join(nombres))
