"""Deja en el reloj (build de depuración) un día de prueba relativo a la hora del emulador, y abre CampusWatch.
Uso: python tools/verificacion/reloj_dia.py <escenario>   escenarios: proxima · salir · urgente · parcial · horario · vacio"""
import datetime as dt, os, subprocess, sys

ADB = os.path.expandvars(r'%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe')
PKG = 'com.dpinta.agenda'
def sh(*a): return subprocess.run([ADB, 'shell', *a], capture_output=True, text=True, encoding='utf8').stdout.strip()

ahora = dt.datetime.strptime(sh('date', '+%Y-%m-%dT%H:%M'), '%Y-%m-%dT%H:%M')
def t(m): return (ahora + dt.timedelta(minutes=m)).strftime('%Y-%m-%dT%H:%M')
def fila(id_, ini, fin, titulo, aula, lugar, salida, tipo, examen=False):
    return '\t'.join([str(id_), t(ini), t(fin), titulo, aula, lugar, t(salida) if salida is not None else '', tipo, '1' if examen else '0'])

esc = sys.argv[1] if len(sys.argv) > 1 else 'proxima'
base = [
    fila(1, -170, -120, 'Desayuno', '', 'Casa', None, 'PERSONAL'),
    fila(4, 150, 270, 'Turno', '', 'Tienda centro', 120, 'TRABAJO'),
    fila(5, 600, 600, 'Informe Física', '', '', None, 'ENTREGA'),
    fila(6, 24 * 60 - 60, 24 * 60 + 60, 'Programación', 'Lab 3', 'Campus', 24 * 60 - 95, 'CLASE'),
]
siguiente = {
    'proxima': fila(3, 18, 108, 'Cálculo diferencial', 'B-204', 'Campus', 50, 'CLASE'),   # sin trayecto pendiente: cuenta al inicio
    'salir': fila(3, 12, 102, 'Cálculo diferencial', 'B-204', 'Campus', 0, 'CLASE'),
    'urgente': fila(3, 6, 96, 'Cálculo diferencial', 'B-204', 'Campus', -6, 'CLASE'),
    'parcial': fila(3, 42, 132, 'Parcial de Física', 'Aula 105', 'Campus', 20, 'CLASE', True),
    'horario': fila(3, -20, 70, 'Cálculo diferencial', 'B-204', 'Campus', -50, 'CLASE'),
}
if esc == 'proxima':  # sin trayecto: la siguiente empieza donde ya está
    siguiente['proxima'] = fila(3, 18, 108, 'Cálculo diferencial', 'B-204', 'Campus', None, 'CLASE')
filas = [] if esc == 'vacio' else base + [siguiente.get(esc, siguiente['proxima'])]
texto = 'cw1\t' + t(-3) + '\n' + '\n'.join(filas) + '\n'
xml_txt = texto.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('\t', '&#9;').replace('\n', '&#10;')
xml = f"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    <string name=\"dia\">{xml_txt}</string>\n</map>\n"
sh('am', 'force-stop', PKG)
if esc == 'sin-datos':
    sh('run-as', PKG, 'rm', '-f', 'shared_prefs/campuswatch.xml')
else:
    p = subprocess.run([ADB, 'shell', 'run-as', PKG, 'sh', '-c', '"mkdir -p shared_prefs && cat > shared_prefs/campuswatch.xml"'], input=xml.encode('utf8'), capture_output=True)
    if p.returncode: print(p.stderr.decode())
sh('am', 'start', '-n', f'{PKG}/com.dpinta.agenda.wear.CampusWatchActivity')
print(esc, ahora.strftime('%H:%M'))
