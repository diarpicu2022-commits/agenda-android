# Agenda en inglés

Hecho el 10 de noviembre de 2026 en la rama `feature/agenda-ingles`.

El profesor pidió que todos los proyectos se puedan usar en español y en inglés. Agenda tenía casi todo el texto
escrito dentro del código, unos 300 textos en 34 archivos, y las fechas fijadas en español.

## Quién lo usa y qué cambia

Agenda la usa un estudiante universitario en Colombia. El inglés sirve para un compañero de intercambio o para quien
tenga el teléfono en inglés. Por eso:

- El idioma por defecto sigue al del sistema. Si el teléfono está en inglés, Agenda sale en inglés, y con cualquier otro
  idioma sale en español.
- En Ajustes › Apariencia, debajo del tema, se elige Español, English o Como el sistema. Desde Android 13 es el mismo
  idioma por app que se ve en los ajustes del teléfono.
- Las fechas se siguen escribiendo día/mes/año también en inglés, porque la app es para la universidad en Colombia.
  El mensaje de error lo aclara («day/month/year»). Decisión de Diego del 10 de noviembre.
- Los festivos colombianos conservan su nombre oficial y llevan la traducción entre paréntesis, por ejemplo
  «Batalla de Boyacá (Battle of Boyacá)».
- Lo que escribe la persona (nombres de actividades, lugares, motivos) no se traduce.

## Cómo está hecho

Hay dos maneras, según el tipo de texto:

1. **Rótulos fijos de pantalla** (Ajustes, política, consentimiento, barra, permisos) van en `res/values/strings.xml`
   y `res/values-en/strings.xml`. Los patrones de fecha también están ahí (`fmt_dia_largo`, `fmt_dia_mes`), porque el
   orden cambia con el idioma.
2. **Frases armadas en código** (boleto, banda, avisos, widget, formulario, TalkBack) llevan el par español e inglés
   junto, con `Lengua` (`l("Sal ya", "Leave now")`). Esas frases tienen horas, plurales y un orden de palabras distinto
   en cada idioma, y se prueban en la JVM sin Android.

Los errores y textos que guarda el ViewModel o el dominio siguen en español y se traducen al mostrarse
(`Validador.mensaje`, `Mensajes.de`). Así las pruebas que ya existían no cambian.

En la app, `lengua()` y `formatoFecha()` salen del idioma de la configuración. Los avisos y el widget lo leen con
`Idioma.lengua(contexto)`. Al cambiar de idioma se reprograman los avisos y se vuelven a registrar los canales de
notificación con su nombre traducido.

## Términos

| Español | Inglés |
|---|---|
| Voy saliendo (aviso) | Leaving now |
| Hoy no voy (aviso) | Not going today |
| Ya voy (boleto) | On my way |
| Aplazar 5 min | Snooze 5 min |
| Sal ya · Vas tarde | Leave now · Running late |
| Boleto de salida | Departure ticket |
| Margen | Buffer |
| Salón | Room |
| Tu ritmo | Your rhythm |

## Verificación

- `tools/verificacion/restos_espanol.py` recorre con la app en inglés Hoy, Semana, Actividades, los cinco escenarios
  del formulario, Ajustes, la política, Semestre y Lugar. Lee el árbol de accesibilidad, así que revisa también lo que
  lee TalkBack. La primera pasada encontró 9 restos, todos el botón «Elegir en el mapa». Después del arreglo, 0 restos.
- Hay pruebas JVM nuevas. `BandaSalidaInglesTest` (5) recorre todos los estados del boleto con y sin dato viejo y busca
  español. `MensajesTest` (4) comprueba que los 18 festivos de 2026 a 2028 y todos los errores del formulario tienen
  traducción. Las pruebas en español siguen pasando y el lint no da errores.
- Capturas en inglés de Hoy, el formulario, Semana, Actividades, Ajustes y el consentimiento en el emulador
  (1080 × 2400, 420 dpi).
- Barra inferior a 360 dp con la letra al doble, en la app instalada: «Today», «Week» y «Activities» caben enteros.

## Pendiente

- `BarraInferiorTest` falla en inglés. Dentro de la prueba «Today» se mide como si cupiera a 14 sp y luego desborda
  el ancho. En la app real, con el mismo ancho y la misma letra, cabe. La prueba cambia la escala de letra por código,
  y algo de esa forma de medir no coincide con el dibujo real. Falta averiguar por qué antes de tocar la etiqueta.
- Android 10 a 12 no se probó (no hay emulador con esas versiones). Ahí el idioma se aplica envolviendo el contexto de
  la actividad, los avisos y el widget.
- El widget y las notificaciones en inglés solo se probaron con pruebas unitarias de texto, no en pantalla.
- La pantalla de demostración de la banda (solo depuración) sigue en español.
- CampusWatch sigue en español porque es otro módulo y lleva su propio paso.
