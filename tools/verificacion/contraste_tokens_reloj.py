"""Contraste de los pares de color del reloj en los dos temas (anexo 2026-11-09-campuswatch-claro, W5), calculado desde
ColoresReloj.kt (CW = oscuro, CWClaro = claro). Es la comprobación del paso 1 (tokens); la del render va en el paso 6.
Uso: python tools/verificacion/contraste_tokens_reloj.py"""
import re, sys
src = open('wear/src/main/java/com/dpinta/agenda/wear/ColoresReloj.kt', encoding='utf8').read()
def paleta(obj):
    b = src[src.index(f'object {obj} {{'):]; b = b[:b.index('}')]
    return {n: v[4:] for n, v in re.findall(r'val (\w+): Long = (0x\w+)', b)}
def lum(h):
    r, g, b = (int(h[i:i + 2], 16) / 255 for i in (0, 2, 4))
    f = lambda c: c / 12.92 if c <= .04045 else ((c + .055) / 1.055) ** 2.4
    return .2126 * f(r) + .7152 * f(g) + .0722 * f(b)
def cr(a, b):
    x, y = sorted((lum(a), lum(b)), reverse=True); return (x + .05) / (y + .05)
# (texto o marca, fondo, mínimo, qué es). Los valores son 0xAARRGGBB: se toma RRGGBB.
PARES = [('tinta', 'fondo', 7, 'cifra del anillo, nombre y aula (AAA)'), ('tintaSuave', 'fondo', 4.5, 'rótulos y pie'),
         ('hora', 'fondo', 7, 'ES HORA DE SALIR y hora de salida (AAA)'), ('sobreHora', 'hora', 4.5, 'texto del botón Ya voy'),
         ('tinta', 'superficie', 4.5, 'texto de Acciones'), ('tinta', 'horaSuave', 7, 'fila actual del horario (AAA)'),
         ('ciruela', 'fondo', 4.5, 'rótulo PARCIAL'), ('exito', 'fondo', 3, 'check de confirmación'), ('aviso', 'fondo', 4.5, 'No llegó al teléfono'),
         ('critico', 'fondo', 4.5, 'SAL YA'), ('superficieFuerte', 'fondo', 1.0, 'riel del anillo (solo se informa)')]
PARES += [(u, 'fondo', 3, 'anillo en ' + u) for u in ('urgCalma', 'urgProxima', 'urgPronto', 'urgSalir', 'urgUrgente', 'urgPerdida', 'lineaTrabajo', 'lineaPersonal', 'lineaEntrega', 'ruta')]
fallas = 0
for tema, obj in (('oscuro', 'CW'), ('claro', 'CWClaro')):
    p = paleta(obj)
    for a, b, m, que in PARES:
        r = cr(p[a], p[b]); mal = r < m; fallas += mal
        print(f"{tema:6} {a:16} sobre {b:11} {r:5.2f}:1  mín {m:>3}  {'FALLA' if mal else 'ok   '}  {que}")
print('fallas:', fallas); sys.exit(1 if fallas else 0)
