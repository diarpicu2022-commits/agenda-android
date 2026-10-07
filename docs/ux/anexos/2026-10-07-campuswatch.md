# Anexo · CampusWatch (Wear OS) — 2026-10-07

Trabajo nocturno autorizado por Diego («si acabas [SISCAN] continúas con el de CampusWatch, siguiendo el sistema de
diseño de él, te doy autorización de todo»). Rama `campuswatch`. Sin visto bueno intermedio esa noche: las decisiones
que son de Diego quedan abajo en «Para decidir».

## Contrato
El **sistema de diseño Agenda + CampusWatch** (`Proyectos Finales/App Smartwacht/agenda-design-system.zip`) es el contrato:
`03-campuswatch.md` (esfera, pantallas, vibración, sin conexión), `02-urgencia.md` (niveles, umbrales, acciones),
`tokens.json` tema **reloj** y la familia **reloj** (Sofia Sans Condensed: `reloj-hora` 44/44·700, `reloj-titulo` 20/22·650,
`reloj-apoyo` 15/18·500). Referencias dibujadas: `WatchNextActivity`, `WatchLeaveNow`, `WatchActions`, `WatchSchedule`,
`WatchConfirmation`, `WatchTile`, `WatchComplications`, `WatchAmbient`.
No se usó nada fuera del sistema: los 35 colores se generan de `tokens.json`/`tokens.css` (`tools/cw_recursos.py` →
`res/values/cw_colores.xml` y `ColoresReloj.kt`, un solo origen para Compose, Tiles y XML).

## Qué se hizo
- **Dominio** (`core/domain/Watch.kt`, con pruebas): `Urgency` y `UrgencyRules` con los umbrales del sistema (Calma > 60 min,
  Pronto ≤ 30, Salir ahora a la hora de salida, Urgente 5 min después sin «Ya voy», Perdida al empezar sin salir);
  `WatchDay`/`WatchSession` y su formato de intercambio teléfono → reloj (ida y vuelta probada con tabuladores, saltos y barras).
  «Siguiente» = la primera que no ha empezado, igual que `DayPlanner.next` en el teléfono.
- **Teléfono** (`app/reloj/Reloj.kt`): `MapeadorReloj` (hoy y mañana con la hora de salida si hay trayecto) y
  `PublicadorReloj`, llamado en `ProgramadorAvisos.reprogramar()` junto al widget. `ServicioReloj` recibe las acciones del
  reloj y las pasa **al mismo `AccionAvisoReceiver` de la notificación** (Voy saliendo · +5 min · Hoy no voy): el reloj nunca
  hace algo distinto del teléfono.
- **Reloj** (módulo `:wear`, mismo `applicationId` que el teléfono porque la capa de datos lo exige):
  1. Próxima: anillo de cuenta (riel `superficie-fuerte`, 10 px `stroke-anillo`, se vacía en la última hora, muesca `hora`
     en la salida, color por urgencia), «Empieza en», cifra, nombre en una línea, «aula · hora». Parcial: rótulo PARCIAL y
     anillo `ciruela` hasta «Salir ahora».
  2. Es hora de salir / SAL YA (urgente) con «Ya voy».
  4. Acciones: Ya voy, Aplazar 5 min, Cancelar sesión de hoy (48 dp, uno por fila).
  5. Horario de hoy y de mañana: hora, marca de tipo (● clase, ■ trabajo, ▲ entrega, ◆ parcial, ✓ hecha), fila actual en
     `hora-suave`; pie «Actualizado 07:12» o «Datos locales · 07:12».
  7. Confirmación con el check dibujado (480 ms, `ease-llegada`; sin animación si el sistema la quitó).
  Navegación vertical con la corona (un gesto = una página). Siempre activo: solo contornos, sin color de urgencia.
  Tarjeta (Tile) «Próxima» con arco, y Complicación (RANGED_VALUE 0–60 min para salir, SHORT_TEXT).

## Desviaciones conscientes (y por qué)
- **Examen y urgencia**: el sistema dice que el examen «sube un nivel». Subir Pronto → Salir ahora mostraría «ES HORA DE
  SALIR» media hora antes; se aplicó solo Salir ahora → Urgente, y antes se distingue por el color ciruela. Con prueba.
- **Tile y Complicación** usan la fuente condensada **del sistema**: Tiles/ProtoLayout no cargan fuentes de la app.
- **Vibración**: los avisos llegan al reloj como la notificación del teléfono (puente de notificaciones de Wear OS), que vibra
  una vez; los patrones propios por nivel quedan pendientes (necesitan que el reloj programe sus propias alarmas).
- **Asistencia y QR** (pantallas 3 y 6) **no se hicieron**: el QR del sistema exige un servidor que valide `sessionId`,
  `studentId`, `timestamp` y `nonce`, y en el teléfono aún no hay datos de asistencia conectados. Ver «Para decidir».

## Verificación (medida en el emulador Wear OS, 454×454, densidad 2,0)
- `tools/verificacion/reloj_dia.py <escenario>` carga un día de prueba; `medir_reloj.py` mide sobre la captura (dos volcados
  iguales seguidos = UI estable).
- **Contraste** sobre el render: mínimo **7,57:1** (hora resaltada del horario); el resto 9,2–19,0:1. Todo ≥ AAA.
- **Toques**: «Ya voy» 108×48 dp, Aplazar y Cancelar 158×48 dp (mínimo del sistema: 48).
- `:core:domain:test` + `:app:testDebugUnitTest`: **128 pruebas, 0 fallos**. `:wear:lintDebug` sin errores.
- Capturas: `docs/capturas/cw-paso1-*.png` (próxima, salir, urgente, parcial, en curso, acciones, hoy, mañana, sin datos,
  sin conexión, tarjeta).
- **Fallos propios encontrados por la verificación**: (1) con la clase en curso el reloj la mostraba tachada como «perdida»
  → «siguiente» alineada con el teléfono; (2) sin conexión con el teléfono la confirmación mostraba el **check de éxito** →
  ahora «!» en `aviso` y «No llegó al teléfono»; (3) la Tarjeta repetía la hora sin trayecto; (4) lint: API restringida y
  peso experimental en la Tarjeta.
- **Pendiente de medir**: emparejado real teléfono ↔ reloj (los emuladores no se emparejan sin Google Play; se probó
  cargando el día en el almacenamiento del reloj), Complicación sobre una esfera, modo siempre activo en pantalla, TalkBack.

## Privacidad
El reloj guarda solo el horario de hoy y mañana que le pasa el teléfono (títulos, aulas, lugares y horas), en el
almacenamiento privado de la app; no habla con ningún servidor. La política de Agenda (`docs/legal/`) debe añadir una
línea sobre esto antes de publicarse.
