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

## 6. Verificación prevista

- Contraste medido sobre el render en cada tema que entre. La cifra, el aula y la hora de salida a nivel AAA, y el resto a AA.
- Toques de 48 dp o más.
- Capturas en el emulador Wear OS a 454 px de Próxima, Salir, Urgente, Parcial, En curso, Acciones, Hoy, Mañana, Sin
  datos, Sin conexión, Confirmación y «siempre activo».

## Privacidad

El tema elegido se guarda solo en el reloj. No es un dato personal. La política de Agenda lo nombrará junto a la línea
del horario que guarda el reloj.
