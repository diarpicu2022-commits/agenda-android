"""
Ícono de Agenda · dirección 2 corregida «La línea del día» (decisión de Diego, 2026-11-10).

Un anillo casi cerrado (la línea del día enrollada, igual al anillo de CampusWatch) con las tres estaciones del sistema
sobre el riel (círculo = clase, cuadrado = turno, triángulo = entrega) y el punto de salida en `hora` al comienzo, junto
a la abertura de arriba. Una sola geometría produce:
  - el frente del ícono adaptable (colores) y el monocromo (una tinta) de Android, para el teléfono y el reloj;
  - una vista previa medible: el ícono a 48 px recortado en círculo, junto a íconos de referencia.

Uso: python tools/icono/icono_ruta.py [carpeta_de_vista_previa]
"""
import math
import os
import sys

from PIL import Image, ImageDraw

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
PAPEL, TINTA, HORA, RUTA = "#F2EFE6", "#251E2B", "#AD3C24", "#1D5A60"

C = 54.0          # centro del lienzo de 108 dp
R = 24.0          # radio del riel (todo cae dentro del círculo seguro de 33 dp)
GROSOR = 8.5      # trazo del riel: 3,8 px a 48 px
# Ángulos en grados, 0 a la derecha y creciendo en el sentido del reloj (y hacia abajo, como en Android).
SALIDA = 300      # punto de salida, arriba a la derecha
FIN = 240         # el riel termina arriba a la izquierda: la abertura queda arriba, entre 240 y 300
ESTACIONES = [(10, "triangulo"), (95, "cuadrado"), (175, "circulo")]
HUECO = 31        # grados libres: media estación (6,75) + punta redonda (4,25) + 2 dp de aire = 13 dp a radio 24


def punto(grados, r=R):
    a = math.radians(grados)
    return C + r * math.cos(a), C + r * math.sin(a)


def tramos():
    """Tramos del riel entre la salida, las estaciones y el final, dejando el hueco de cada forma."""
    cortes = [SALIDA] + [a + 360 if a < SALIDA else a for a, _ in ESTACIONES]
    cortes = sorted(cortes)
    fin = FIN + 360
    out = []
    for i, a in enumerate(cortes):
        b = cortes[i + 1] if i + 1 < len(cortes) else fin
        ini = a + HUECO
        hasta = b - HUECO if i + 1 < len(cortes) else b
        if hasta > ini:
            out.append((ini, hasta))
    return out


def forma(tipo, centro, lado=13.5):
    x, y = centro
    if tipo == "circulo":
        return ("circulo", (x, y, lado / 2 + 0.6))
    if tipo == "cuadrado":
        h = lado / 2
        return ("poli", [(x - h, y - h), (x + h, y - h), (x + h, y + h), (x - h, y + h)])
    h = lado * 0.62  # triángulo equilátero un poco más grande para igualar el peso visual
    return ("poli", [(x, y - h), (x + h * 0.9, y + h * 0.62), (x - h * 0.9, y + h * 0.62)])


# ---------- Vector de Android ----------
def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def arco_path(ini, fin):
    x1, y1 = punto(ini)
    x2, y2 = punto(fin)
    grande = 1 if (fin - ini) > 180 else 0
    return f"M{f(x1)},{f(y1)} A{f(R)},{f(R)} 0 {grande} 1 {f(x2)},{f(y2)}"


def circulo_path(x, y, r):
    return f"M{f(x - r)},{f(y)} a{f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a{f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 Z"


def poli_path(pts):
    return "M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts) + " Z"


HALO = 2.6  # borde del color del papel que recorta cada estación sobre el riel (como en un mapa de metro)


def vector(mono):
    """Riel continuo con las estaciones encima. En color llevan un halo de papel; en monocromo (solo silueta) no."""
    color = lambda c: TINTA if mono else c
    halo = "" if mono else f' android:strokeColor="{PAPEL}" android:strokeWidth="{f(HALO)}" android:strokeLineJoin="round"'
    partes = [f'    <path android:pathData="{arco_path(SALIDA, FIN + 360)}" android:strokeColor="{color(TINTA)}" '
              f'android:strokeWidth="{f(GROSOR)}" android:strokeLineCap="round" android:fillColor="#00000000" />']
    for ang, tipo in ESTACIONES:
        k, d = forma(tipo, punto(ang))
        p = circulo_path(*d) if k == "circulo" else poli_path(d)
        partes.append(f'    <path android:pathData="{p}" android:fillColor="{color(RUTA)}"{halo} />')
    x, y = punto(SALIDA)
    partes.append(f'    <path android:pathData="{circulo_path(x, y, 7.0)}" android:fillColor="{color(HORA)}"{halo} />')
    cabeza = ("<!-- Generado por tools/icono/icono_ruta.py: ícono «La línea del día» (decisión de Diego, 2026-11-10). -->\n"
              '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
              '    android:width="108dp" android:height="108dp"\n'
              '    android:viewportWidth="108" android:viewportHeight="108">\n')
    return '<?xml version="1.0" encoding="utf-8"?>\n' + cabeza + "\n".join(partes) + "\n</vector>\n"


# ---------- Vista previa (misma geometría con PIL) ----------
def dibujar(px, mono=False, fondo=PAPEL):
    s = 8  # sobremuestreo para bordes suaves
    k = px * s / 108.0
    img = Image.new("RGB", (px * s, px * s), fondo)
    d = ImageDraw.Draw(img)
    col = lambda c: TINTA if mono else c
    caja = [(C - R - GROSOR / 2) * k, (C - R - GROSOR / 2) * k, (C + R + GROSOR / 2) * k, (C + R + GROSOR / 2) * k]
    d.arc(caja, SALIDA, FIN, fill=col(TINTA), width=int(GROSOR * k))
    for ang in (SALIDA, FIN):  # puntas redondas, como strokeLineCap="round"
        x, y = punto(ang)
        r = GROSOR / 2
        d.ellipse([(x - r) * k, (y - r) * k, (x + r) * k, (y + r) * k], fill=col(TINTA))
    h = 0 if mono else HALO
    for ang, tipo in ESTACIONES:
        kind, dat = forma(tipo, punto(ang))
        if kind == "circulo":
            x, y, r = dat
            if h: d.ellipse([(x - r - h / 2) * k, (y - r - h / 2) * k, (x + r + h / 2) * k, (y + r + h / 2) * k], fill=PAPEL)
            d.ellipse([(x - r) * k, (y - r) * k, (x + r) * k, (y + r) * k], fill=col(RUTA))
        else:
            pts = [(x * k, y * k) for x, y in dat]
            if h: d.line(pts + [pts[0]], fill=PAPEL, width=int(h * k), joint="curve")
            d.polygon(pts, fill=col(RUTA))
    x, y = punto(SALIDA)
    if h: d.ellipse([(x - 7.0 - h / 2) * k, (y - 7.0 - h / 2) * k, (x + 7.0 + h / 2) * k, (y + 7.0 + h / 2) * k], fill=PAPEL)
    d.ellipse([(x - 7.0) * k, (y - 7.0) * k, (x + 7.0) * k, (y + 7.0) * k], fill=col(HORA))
    return img.resize((px, px), Image.LANCZOS)


def recorte_circulo(img):
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).ellipse([0, 0, img.size[0] - 1, img.size[1] - 1], fill=255)
    out = Image.new("RGB", img.size, "#1B2A3A")
    out.paste(img, (0, 0), m)
    return out


def vista_previa(carpeta):
    os.makedirs(carpeta, exist_ok=True)
    grande = Image.new("RGB", (3 * 220 + 40, 260), "#FFFFFF")
    for i, (mono, fondo) in enumerate([(False, PAPEL), (True, PAPEL), (True, "#DCE3EA")]):
        grande.paste(recorte_circulo(dibujar(200, mono, fondo)), (20 + i * 220, 30))
    grande.save(os.path.join(carpeta, "icono-grande.png"))
    # 48 px recortado en círculo, sobre un fondo de pantalla oscuro, en fila con tres «íconos» de referencia.
    fila = Image.new("RGB", (4 * 64 + 16, 80), "#1B2A3A")
    fila.paste(recorte_circulo(dibujar(48)), (16, 16))
    d = ImageDraw.Draw(fila)
    for i, c in enumerate(["#3B82F6", "#22C55E", "#F59E0B"]):
        x = 16 + (i + 1) * 64
        d.ellipse([x, 16, x + 47, 63], fill=c)
    fila.resize((fila.width * 3, fila.height * 3), Image.NEAREST).save(os.path.join(carpeta, "icono-48px.png"))
    return dibujar(48)


if __name__ == "__main__":
    for modulo in ("app", "wear"):
        dr = os.path.join(RAIZ, modulo, "src", "main", "res", "drawable")
        open(os.path.join(dr, "ic_launcher_foreground.xml"), "w", encoding="utf-8", newline="\n").write(vector(False))
        open(os.path.join(dr, "ic_launcher_monochrome.xml"), "w", encoding="utf-8", newline="\n").write(vector(True))
    print("tramos:", [(round(a), round(b)) for a, b in tramos()])
    if len(sys.argv) > 1:
        img48 = vista_previa(sys.argv[1])
        # Medida: el trazo más fino a 48 px y el contraste de cada color sobre el papel.
        print("trazo del riel a 48 px:", round(GROSOR * 48 / 108, 2), "px")
        def lum(h):
            r, g, b = (int(h[i:i + 2], 16) / 255 for i in (1, 3, 5))
            c = lambda v: v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
            return 0.2126 * c(r) + 0.7152 * c(g) + 0.0722 * c(b)
        for nombre, h in [("tinta", TINTA), ("hora", HORA), ("ruta", RUTA)]:
            a, b = sorted([lum(h), lum(PAPEL)], reverse=True)
            print(f"contraste {nombre} sobre papel: {(a + 0.05) / (b + 0.05):.2f}:1")
        # Todo dentro del círculo seguro de 33 dp.
        lejos = max(math.dist(punto(a), (C, C)) + 7 for a, _ in ESTACIONES)
        print("alcance máximo desde el centro:", round(max(lejos, R + GROSOR / 2, math.dist(punto(SALIDA), (C, C)) + 7.0), 1), "dp (límite 33)")
