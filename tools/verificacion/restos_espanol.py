"""
Red de restos en español: recorre las pantallas de Agenda con la app en inglés y falla si queda texto en español.

Lee el árbol de accesibilidad (uiautomator), así revisa a la vez lo que se ve y lo que lee TalkBack. Los datos de
ejemplo de la app de depuración (nombres de actividades, lugares, salones) están en español a propósito y se quitan
antes de buscar; los nombres oficiales de los festivos también (en inglés van con su traducción entre paréntesis).

Uso: python tools/verificacion/restos_espanol.py   (emulador encendido, APK de depuración instalada)
Sale con código 1 si encuentra restos.
"""
import os
import re
import subprocess
import sys
import time

ADB = os.path.expanduser(r"~\AppData\Local\Android\Sdk\platform-tools\adb.exe")
PAQ = "com.dpinta.agenda"

# Datos de ejemplo y nombres propios: se quitan antes de buscar.
DATOS = [
    "Cálculo diferencial", "Física mecánica", "Entrega informe", "Álgebra lineal", "Parcial de Cálculo", "Inventario",
    "Turno", "Tienda centro", "Campus", "Caja 2", "Bodega", "Auditorio 2", "Agenda", "Español", "Diego",
    "Diego Armando Pinta Cuasquen", "Ley 1581", "Decreto 1377", "Decreto 1074", "Calculus", "Batalla de Boyacá",
]
# Señales de español: tildes, ñ, signos de apertura y palabras que no existen en inglés.
ESPANOL = re.compile(
    r"[áéíóúñ¿¡]|\b(de|del|la|las|el|los|y|en|con|para|sin|hoy|sal|salir|llegas|empieza|termina|guardar|"
    r"cancelar|volver|ahora|semana|actividades|ajustes|clase|trabajo|lugar|salón|margen|aviso|minutos|días)\b",
    re.IGNORECASE,
)


def adb(*a):
    return subprocess.run([ADB, *a], capture_output=True)


def textos():
    adb("shell", "uiautomator", "dump", "/sdcard/v.xml")
    x = adb("exec-out", "cat", "/sdcard/v.xml").stdout.decode("utf-8", "replace")
    out = []
    for t, d in re.findall(r'text="([^"]*)"[^>]*content-desc="([^"]*)"', x):
        out += [v for v in (t, d) if v]
    return [v.replace("&apos;", "'").replace("&quot;", '"').replace("&amp;", "&") for v in out]


def tocar(objetivo):
    adb("shell", "uiautomator", "dump", "/sdcard/v.xml")
    x = adb("exec-out", "cat", "/sdcard/v.xml").stdout.decode("utf-8", "replace")
    m = re.search(r'<node [^>]*?(?:text|content-desc)="%s"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"' % re.escape(objetivo), x)
    if not m:
        return False
    a, b, c, d = map(int, m.groups())
    adb("shell", "input", "tap", str((a + c) // 2), str((b + d) // 2))
    time.sleep(1.5)
    return True


def abrir(*extras):
    adb("shell", "am", "force-stop", PAQ)
    adb("shell", "am", "start", "-n", PAQ + "/.MainActivity", *extras)
    time.sleep(9)


def bajar():
    adb("shell", "input", "swipe", "540", "1800", "540", "600", "400")
    time.sleep(1)


def revisar(nombre, encontrados):
    malos = []
    for v in textos():
        limpio = v
        for dato in sorted(DATOS, key=len, reverse=True):
            limpio = limpio.replace(dato, "")
        limpio = re.sub(r"\([^)]*\)", "", limpio)  # «Batalla de Boyacá (Battle of Boyacá)»: el paréntesis es la traducción
        limpio = re.sub(r"\b[A-ZÁÉÍÓÚ][a-záéíóúñ]+(?: [a-záéíóúñ]+)* \(", "", limpio)
        if ESPANOL.search(limpio):
            malos.append(v)
    for v in dict.fromkeys(malos):
        encontrados.append((nombre, v))
    print(f"{nombre}: {'ok' if not malos else str(len(set(malos))) + ' restos'}")


def main():
    adb("shell", "cmd", "locale", "set-app-locales", PAQ, "--locales", "en")
    encontrados = []
    abrir("--es", "hora", "11:50")
    for i in range(3):
        revisar(f"Hoy {i + 1}", encontrados); bajar()
    abrir("--es", "pestana", "semana"); revisar("Semana", encontrados)
    abrir("--es", "pestana", "actividades"); revisar("Actividades", encontrados); bajar(); revisar("Actividades 2", encontrados)
    for esc in ["nuevo", "errores", "completo", "conflictos", "puntual"]:
        abrir("--es", "formulario", esc)
        if esc == "errores":
            tocar("Save")
        for i in range(3):
            revisar(f"Formulario {esc} {i + 1}", encontrados); bajar()
    abrir()
    tocar("Settings")
    for i in range(3):
        revisar(f"Ajustes {i + 1}", encontrados); bajar()
    if tocar("Privacy policy"):
        for i in range(3):
            revisar(f"Política {i + 1}", encontrados); bajar()
    abrir(); tocar("Settings"); bajar()
    if tocar("Add semester"):
        revisar("Semestre", encontrados)
    abrir("--es", "pestana", "actividades"); bajar()
    if tocar("Campus"):
        revisar("Lugar", encontrados)
    print()
    for pantalla, v in encontrados:
        print(f"RESTO  {pantalla}: {v}")
    print(f"\n{len(encontrados)} restos en español")
    sys.exit(1 if encontrados else 0)


if __name__ == "__main__":
    main()
