# Anexo · CampusWatch con los colores de la app y modo claro — 2026-11-09

El profesor pidió (`PENDIENTES-PROFE.md`, punto 3) llevar el diseño y los colores de Agenda al reloj, porque hoy se ve
muy oscuro, y darle **modo claro**. El oscuro actual queda como opción y para «siempre activo».

## 1. Para quién y qué decide

- **Persona**: Diego, estudiante que también trabaja, mira el reloj caminando entre clases, de día y a pleno sol en el
  campus, y de noche al volver del turno. Necesita leer de un vistazo cuánto falta para salir y a qué salón va.
- **Decisión dominante**: «¿ya tengo que salir?». La cifra del anillo y el aula se leen en menos de dos segundos.
- **Registro**: producto, lectura obligada. El punto de entrada es la tarea, o sea la cifra y el color de urgencia, y nada más.

## 2. El contrato vigente y por qué esto es una enmienda

`03-campuswatch.md` del sistema de diseño dice que el reloj va «siempre en el tema `reloj`» (fondo `#050407`, casi negro
con tinte ciruela, pensado para OLED). El modo claro **cambia el contrato**. Necesita el visto bueno de Diego y queda
fechado en este anexo. Los tokens ya traen el tema `claro` de la app (`fondo #f4eee3`, `tinta #251e2b`, líneas y
urgencias en sus versiones claras) y el tema `noche` (`fondo #15121a`), así que **ningún color nuevo** hace falta.

## 3. Referentes (observación → decisión → lo que no se copia)

1. **Wear OS, guía de color** (https://developer.android.com/design/ui/wear/guides/styles/color). Los relojes «se
   diseñan desde el negro» porque deben funcionar de día y de noche. → El modo oscuro no desaparece y «siempre activo»
   sigue en negro en cualquier dirección. → No se copia la paleta Material.
2. **Suunto, tema de pantalla** (https://www.suunto.com/Support/Product-support/suunto_spartan_sport/suunto_spartan_sport/features/display-theme/).
   Ofrece un tema claro para leer mejor durante el ejercicio y la navegación al aire libre. → El claro es una opción
   que elige la persona, no algo impuesto. → No se copia su tipografía ni su disposición.
3. **Garmin, foro de desarrolladores de Connect IQ** (https://forums.garmin.com/developer/connect-iq/f/discussion/252520/white-or-black-colour-and-burn-display).
   La discusión sobre blanco o negro en pantallas que se queman apunta a fondos negros en bajo consumo y a dejar lo claro
   para la pantalla activa. Es un foro, no una pauta oficial. →
   «Siempre activo» queda en el tema `reloj` aunque se elija claro.
4. **Samsung, colores de One UI Watch** (https://developer.samsung.com/one-ui-watch-tizen/visual/colors.html). Pide que
   las pantallas se vean a pleno sol y recomienda fondos oscuros por batería. → El costo de batería del claro se dice en
   el selector y el claro no es el modo de bajo consumo.
5. **El sistema de diseño de Agenda** (`agenda-design-system/tokens.json`). Los tres temas comparten nombres de token, así
   que el reloj puede cambiar de tema sin tocar las pantallas. → Se generan `claro` y `noche` igual que hoy se genera
   `reloj` (`tools/cw_recursos.py`).
6. **El reloj de SISCAN** (`siscan-movil`, anexo 2026-10-07). Ya resolvió lo mismo con «Claro / Oscuro / Como el reloj»,
   el ambiente siempre oscuro y el contraste medido en los dos temas. → Mismo patrón de selector, medido igual. → No se
   copia su escena ni su paleta.

## 4. Direcciones

### A · «La hoja del día en la muñeca» (recomendada)

El reloj usa el tema `claro` de la app, con papel `#f4eee3`, tinta `#251e2b` y el anillo y las urgencias en sus colores
claros (`hora #ad3c24`, `ruta #1d5a60`, `ciruela #7a3a6c`). En el reloj hay un selector con tres opciones, **Claro ·
Oscuro · Como el teléfono**. Por defecto va «Como el teléfono», que sigue el tema que se elija en la app (fase 2). «Siempre activo», la
Tarjeta y la Complicación siguen en `reloj` (negro), por batería y porque así lo piden las guías. El QR sigue blanco,
como dice el sistema.

- A favor: es exactamente lo que pidió el profesor, el reloj y la app se ven como una sola cosa, y se lee mejor al sol.
- En contra: el claro gasta más batería en OLED. Se compensa porque el ambiente sigue negro y la persona puede elegir.

### B · «La noche de la app»

El reloj pasa del `reloj` OLED al tema `noche` de la app (`fondo #15121a`, `superficie #1f1b25`), que es menos negro y
tiene la misma identidad que Agenda de noche, y el `claro` queda como opción en el selector.

- A favor: gasta menos que el claro y ya no se ve «tan oscuro».
- En contra: por defecto sigue siendo oscuro, así que responde a medias al pedido.

### C · «Según la hora»

Claro de 6:00 a 19:00 y `reloj` de noche, automático, con el selector para fijar uno.

- A favor: legible de día y ahorra batería de noche sin que la persona haga nada.
- En contra: un cambio de colores sin aviso puede confundir, y de día gasta igual que A.

## 5. Contrato de diseño (dirección A, elegida por Diego el 2026-11-09)

Enmienda al sistema de diseño: `03-campuswatch.md` decía «siempre en el tema `reloj`». Desde el 2026-11-09 el reloj tiene
dos temas en pantalla activa.

- **W1 · Temas.** `claro` (los tokens `claro` de la app, sin cambiar ningún valor) y `oscuro` (el tema `reloj` de
  siempre). Ningún color fuera de `tokens.json` y `tokens.css`.
- **W2 · Selector.** En el reloj, Claro · Oscuro · Como el teléfono. Por defecto, Como el teléfono. Mientras la app no
  tenga su propio selector, «Como el teléfono» sigue el modo oscuro del sistema del teléfono.
- **W3 · Lo que no cambia.** «Siempre activo», la Tarjeta y la Complicación quedan en `reloj` (negro), por batería y porque
  Tiles y la esfera no pueden seguir el tema de la app. El QR, cuando exista, sigue blanco.
- **W4 · Mismas pantallas.** Composición, tipografía (Sofia Sans Condensed, `reloj-*`), anillo, tamaños y movimiento no
  cambian. Solo cambia la paleta.
- **W5 · Contraste.** Medido sobre el render en los dos temas. La cifra, el aula y la hora de salida a nivel AAA (7:1) y
  el resto a AA (4,5:1). El anillo y las marcas, que no son texto, a 3:1 contra el fondo.
- **W6 · Asistencia local (elegida por Diego el 2026-11-09).** Pantalla de asistencia con las faltas por materia que ya
  calcula `Attendance` en el dominio, con el repertorio de Horario. El QR queda pendiente hasta que exista un servidor.

Pasos: 1 tokens · 2 componente clave (Próxima con el anillo) · 3 el resto de pantallas · 4 selector · 5 asistencia ·
6 estados y verificación. Cada paso se muestra y espera visto bueno.

### Paso 1 · Tokens (hecho 2026-11-09, aprobado)

- `tools/cw_recursos.py` genera también `CWClaro` (tema `claro` de la app, 35 colores con los mismos nombres que `CW`).
  `Reloj` (Tema.kt) lee de la paleta activa, que es un estado de Compose.
- `tools/verificacion/contraste_tokens_reloj.py` calcula 21 pares por tema. En oscuro pasan todos (el más bajo, 6,13:1).
  En claro pasan 20.
- **Decisiones de Diego (2026-11-09)**:
  - «ES HORA DE SALIR», «SAL YA» y «AHORA» quedan en su color de urgencia en claro (5,27 a 6,37:1, AA). Son rótulos y la
    cifra que se lee para decidir va en tinta. Enmienda a W5, que pedía AAA para la hora de salida.
  - La hora de la fila actual del horario va en tinta en claro (13,24:1 sobre `horaSuave`), porque en `hora` daba 4,99:1.
- Fallo propio: la primera corrida dio 32 fallas falsas porque el script leía el canal alfa (`0xFF…`) como parte del color.

### Paso 2 · Componente clave, Próxima con el anillo (hecho 2026-11-09)

- `PreferenciaTema` (Datos.kt) guarda Claro · Oscuro · Como el teléfono en su propio archivo (`campuswatch_tema`). Por
  defecto, Como el teléfono. Mientras el teléfono no publique su modo, se toma claro, que es el de la app por defecto.
- `CampusWatchActivity` aplica el tema elegido en pantalla activa y el oscuro en «siempre activo» (W3).
- `reloj_dia.py` acepta el tema como segundo argumento, para las pruebas.
- **Medido sobre el render** (`medir_reloj.py`, captura estable). En claro la cuenta «17 min» y el nombre dan 14,00:1 y el
  anillo de «Pronto» 5,42:1 como marca (`docs/capturas/cw-medida-p2-proxima-claro.png`). En oscuro no cambió nada (19,01:1
  y 11,16:1, `cw-medida-p2-proxima-oscuro.png`).
- **Hallazgo para Diego.** En claro la línea del aula y la hora («B-204 · 03:44», en `tinta-suave`) da 5,63:1. W5 pide AAA
  para el aula.

- **Decisión de Diego (2026-11-09):** en claro la línea del aula y la hora va en tinta (`Reloj.aula`).

### Paso 3 · El resto de las pantallas (hecho 2026-11-09)

- `Reloj.aula` (tinta en claro, tinta suave en oscuro) en las líneas de aula de Próxima y En curso, y `Reloj.horaFila`
  (tinta en claro, `hora` en oscuro) en la hora de la fila actual del horario.
- `tools/verificacion/recorrido_temas_reloj.sh` (nuevo) recorre en los dos temas Próxima, Salir, Urgente, Parcial, la
  sesión del día con Acciones, Hoy y Mañana, Vacío, Sin datos y la Confirmación (tocando «Ya voy»). Son 22 capturas
  estables (`docs/capturas/cw-medida-p3-*`) y **0 fallos** de contraste AA, toques o círculo.
- **Hallazgos para Diego**, del mismo tipo que el aula. En claro, las horas de la lista del horario que no son la actual
  (00:45, 06:05, 13:35) dan 5,63:1, y «comienza en 12 min» en Es hora de salir también. `CLAUDE.md` pide AAA para las horas
  de la lista. En oscuro dan 11,16:1.
- **Decisión de Diego (2026-11-09):** en claro, las horas de la lista y «comienza en» van en tinta (`Reloj.cifraApoyo`).
  Medido después en claro: 14,00:1.

### Paso 4 · Selector de tema (hecho 2026-11-09)

- Página **Tema** al final del recorrido (después de Mañana) con Claro · Oscuro · Como el teléfono, con el repertorio de
  Acciones. La opción elegida se distingue por el color (`ruta`) y no cambia de tamaño.
- El teléfono publica su modo oscuro en `/campuswatch/tema` junto con el día (`PublicadorReloj`). El reloj lo recibe en
  `ReceptorDia` y al abrir (`AlmacenDia.sincronizar`). El cambio se ve al momento (`PreferenciaTema.cambio`).
- **Medido** en el emulador. Al tocar Oscuro todo pasa a oscuro y al tocar Como el teléfono vuelve a claro, y la
  preferencia queda guardada. Con `telefono_oscuro = true` simulado, el reloj abre en oscuro. Contraste de la página
  Tema en claro de 5,63 a 15,55:1 y en oscuro de 11,16 a 17,38:1, sin fallos.
- **Fallo encontrado al mirar la captura.** «Como el teléfono» se cortaba en «Como el» en el botón elegido, que medía el
  62 % del ancho. `medir_reloj.py` no lo ve porque el volcado trae el texto completo. Ahora las tres opciones miden el
  90 % (`BotonReloj(ancho = …)`).
- **Pendiente**: el emparejamiento real teléfono ↔ reloj (los emuladores no se emparejan sin Google Play). El teléfono
  publica su modo cuando reprograma los avisos, no en el instante en que se cambia el modo del sistema.

### Paso 5 · Asistencia local (hecho la noche del 2026-11-09, con autorización de Diego para seguir sin parar)

Diego pidió seguir toda la noche y dejar las decisiones juntas para el día siguiente. Las que tomé están marcadas con
**[por confirmar]**.

- **De dónde salen las marcas [por confirmar].** «Hoy no voy» solo cancelaba la sesión y no distinguía «no fui» de «no hubo
  clase». Ahora hay una tabla `asistencia` (Room v3, migración 2 → 3 probada con Robolectric y esquema `3.json`). En una
  clase o examen, «Voy saliendo» o «Ya voy» marcan **asistí** y «Hoy no voy» o «Cancelar» marcan **falté**. Al calcular,
  la sesión cancelada con marca «falté» sí cuenta como sesión que hubo, porque si no la falta no contaría nunca.
- **Dominio.** `WatchAttendance` y formato `cw2` con líneas de asistencia. El reloj sigue leyendo `cw1` (prueba de ida y
  vuelta).
- **Teléfono.** `PublicadorReloj` calcula con `Attendance.summary` desde el inicio del semestre (o 120 días) y publica la
  asistencia junto con el día. `ServicioReloj` recibe «asisti» y «falte» y los pasa al mismo receptor, que guarda y vuelve
  a publicar.
- **Reloj.** Dos páginas con el repertorio de Acciones y Horario (W4):
  - **¿Fuiste?** aparece después de la principal cuando una clase de hoy ya terminó y no tiene marca. Tiene «Sí, fui» y «No
    fui», y se oculta al responder.
  - **Asistencia**, antes de Tema, con un bloque centrado por materia: el nombre y debajo «2 faltas de 4».
- **Límite de faltas [por confirmar].** Las materias todavía no tienen un campo de faltas permitidas, así que hoy se
  muestra solo la cuenta («0 faltas»). Cuando exista el límite, cerca de él aparece un punto `aviso` y pasado de él un
  punto `critico`. En claro la cifra sigue en tinta (misma regla que el aula), porque en `aviso` daba 5,42:1.
- **Medido** en el emulador con el escenario nuevo `asistencia` de `reloj_dia.py`. ¿Fuiste? da de 5,63 a 15,55:1 en claro
  y de 11,16 a 19,01:1 en oscuro. En Asistencia todas las cifras dan 14,00:1 en claro y 12,40 a 19,01:1 en oscuro. Sin
  fallos (`docs/capturas/cw-medida-p5-*`).
- **Fallos propios encontrados al mirar la captura.** La primera versión ponía nombre y faltas en una fila, cortaba los
  nombres («Cálculo diferenc…») y los dejaba centrados dentro de su columna. Ahora es un bloque centrado por materia.
- **Pendiente.** El emparejamiento real (los mensajes del reloj al teléfono no se pudieron probar entre emuladores) y una
  pantalla de asistencia en el teléfono.

## 6. Verificación prevista

- Contraste medido sobre el render en cada tema que entre. La cifra, el aula y la hora de salida a nivel AAA, y el resto a AA.
- Toques de 48 dp o más.
- Capturas en el emulador Wear OS a 454 px de Próxima, Salir, Urgente, Parcial, En curso, Acciones, Hoy, Mañana, Sin
  datos, Sin conexión, Confirmación y «siempre activo».

## Privacidad

El tema elegido se guarda solo en el reloj. No es un dato personal. La política de Agenda lo nombrará junto a la línea
del horario que guarda el reloj.
