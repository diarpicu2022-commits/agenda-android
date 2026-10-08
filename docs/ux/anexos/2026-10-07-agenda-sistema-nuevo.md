# Anexo · Agenda con el sistema de diseño Agenda + CampusWatch — 2026-10-07

Decisión de Diego (2026-10-06): «botar a la basura ese UI tan feo y colocarle el nuevo del sistema de diseño».
Trabajo nocturno autorizado («te doy autorización de todo»), rama `sistema-nuevo` (sobre `campuswatch`).
Enmienda registrada en `2026-09-23-agenda-direcciones.md` · Registro de enmiendas.

## Contrato
Sistema **Agenda + CampusWatch** (`README.md`, `01-linea-del-dia.md`, `02-urgencia.md`, `04-patrones.md`, `tokens.json`),
referencias `PantallaHoy`, `DepartureCard`, `TodayTimeline`, `AppNav`. Temas «claro» y «noche».

## Paso 1 · Tokens (hecho)
- `ui/theme/Tokens.kt` **generado** desde `tokens.json`/`tokens.css` (`tools/gen_tokens_agenda.py`): 38 colores × 2 temas.
- Tipografía: **Bricolage Grotesque** (display: horas, cuentas, títulos) y **Figtree** (ui), variables y empaquetadas
  (OFL), con la escala exacta: hora-hero 56/56·700, hora-xl 36/40·650, hora-lg 22/26·600, hora-sm 14/18·600,
  titulo 28/32·700, encabezado 19/24·650, cuerpo-fuerte 16/22·600, cuerpo 16/24, apoyo 14/20·450, etiqueta 12/16·700.
- Formas: ficha 6, bloque 14, lámina 24, pastilla; Material 3 hereda (campos = ficha, hojas = lámina).
- Los nombres del contrato anterior (`papel`, `senal`, `clase`…) quedan como **alias** de los tokens nuevos
  (`fondo`, `hora`, `linea-universidad`…) para que toda la app cambie de una vez sin romperse; cada pantalla pasa a
  `AgendaTheme.ds` al migrarla.
- Medido: con Bricolage (más ancha que Archivo 62) la hora de la lista vieja se partía «10:3 / 0» → resuelto en el paso 2.

## Paso 2 · Componente clave + Hoy (hecho)
- **Boleto de salida** (`ui/components/boleto/BoletoSalida.kt`): la única forma con muescas y perforación. Arriba, en el
  `-suave` del nivel: «BOLETO DE SALIDA», insignia de urgencia (ícono de forma + palabra), «Debes salir en», la cuenta en
  hora-hero del color del nivel, «Hora de salida 07:25», tramo «Salida ···· 23 min en bus ···· Campus» (el centro cambia
  el modo). Abajo: «Cálculo diferencial comienza a las 8:00 · B-204 · Viaje 23 min · Margen 7 min», aviso de dato viejo
  y «Ya voy» (pastilla `hora`) + «Aplazar 5 min». Estados: espera/pronto (cuenta), sal ya, vas tarde (urgente),
  en camino («Llegada estimada», «Ya llegué»), sin traslado («PRÓXIMA · Empieza a las»).
  Las acciones son las mismas de antes (`AccionBanda`): mismo ViewModel, mismas alarmas.
- **Línea del día** (`ui/hoy/LineaDelDia.kt`): hora, riel (4 dp) con estaciones de 14 dp — círculo universidad,
  cuadrado trabajo, triángulo entrega, ✓ completada —, tramo punteado hacia lo que viene, sesión en curso en bloque
  `hora-suave` con «AHORA», marcador «● Ahora 7:41 a. m.» en `hora`, conflicto con ícono y texto `critico`.
- **Hoy** (`PantallaHoy`): fecha + «Buenos días» (sin nombre: la app no tiene cuentas), boleto, «Tu día», en un solo
  desplazamiento; la barra de estado queda fija con el fondo.
- **Barra inferior** (`AppNav`): superficie con línea; activa en pastilla `ruta-suave` y etiqueta `ruta`; Crear = círculo `ruta`.

### Verificación del paso 2 (emulador Agenda_Pixel, API 36, 411 dp)
- Claro, noche y fuente 2,0: `docs/capturas/sn-paso2-*`. `:app:testDebugUnitTest` 84/84, `lintDebug` sin errores.
- Fallos propios encontrados midiendo: contenido bajo la barra de estado al desplazar; «estimado 12 días» (faltaba «hace»);
  con fuente 2,0, «8:07 a. / m.» partido y el tramo sin puntos con el destino cortado → hora con espacios no separables
  (solo en el boleto: notificaciones y widget conservan su texto y sus pruebas) y tramo apilado con letra grande.
- Pendiente de medir con los scripts de `tools/verificacion/`: contraste sobre el render de cada estado del boleto y 360 dp.

## Paso 3 · Componentes compartidos (hecho)
Botón principal en pastilla `hora` (uno por pantalla: «Guardar», «Ya voy»); «quiet» en texto `ruta` sin subrayado;
selectores en fichas de 6 dp (elegida `ruta-suave` + borde `ruta`); días L–D en pastillas (elegido `ruta`); campos con
etiqueta visible 14·600, borde `linea-fuerte` 1,5 dp, `superficie`, radio ficha; encabezado sin filete.
Los colores del contrato anterior que llegan como parámetro se traducen dentro del componente, así Formulario, Ajustes,
Semestre y Lugar cambian sin tocarlos. Capturas `sn-paso3-*`.

## Paso 4 · Semana y Actividades (hecho)
- **Semana** (PantallaSemana): rótulo «Semana 10 · 5–11 oct» + título; selector de días en pastillas con las estaciones
  de cada día (hoy con anillo `hora`, elegido en `ruta`, festivo con anillo `ciruela`); debajo, la línea del día elegido
  con las mismas estaciones de Hoy, y los cruces como aviso «X y Y se cruzan de 9:00 a 10:00» (◐ `aviso`).
  Reemplaza la rejilla horaria (se eliminó el código de la rejilla).
- **Actividades**: estaciones del sistema (● ■ ▲) en lugar de cuadrados, rangos en `hora-sm`, encabezados `encabezado`.
- `:app:testDebugUnitTest` 84/84, `:core:domain:test` en verde, `lintDebug` sin errores. Capturas `sn-paso4-*`.

## Paso 5 · Widget «Siguiente» (hecho)
HomeWidget del sistema: lámina 24 en `superficie` con borde `linea` (antes, banda amarilla a sangre), rótulo en
`etiqueta` `tinta-suave`, la hora en Bricolage `hora-xl` con el acento `hora`, salón en Bricolage 600, textos en Figtree.
Colores generados de `tokens.json` para claro y noche (`res/values*/widget.xml`). Mismos textos y pruebas.
**Desviación consciente**: el sistema muestra una cuenta («Salir en 18 min»); un widget de Android no se puede redibujar
cada minuto y una cuenta congelada mentiría, así que se mantiene la hora absoluta («Sal a las 7:30»), que siempre es cierta.
**Pendiente encontrado al medir** (ya existía): pasada la hora de salida el widget sigue diciendo «Sal a las 7:30» hasta
el siguiente reprogramado; debería pasar a «Vas tarde» o a la siguiente sesión con una alarma a la hora de salida.

## Siguiente
Formulario, Ajustes y Semestre ya usan las piezas nuevas pero conservan su composición; pendiente pasarlos a `SettingsGroup`
y `Switch` del sistema. Widget «Siguiente»: pendiente de pasar al `HomeWidget` del sistema (hoy sigue el amarillo anterior).

## Paso 6 · Ajustes, formulario, Semestre y Lugar (2026-10-08)

- **Ajustes**: nota de privacidad («Tus datos permanecen en este dispositivo»), Tu perfil, grupos del sistema (Agenda ·
  Avisos · Datos) con `GrupoAjustes`/`FilaAjuste`/`FilaInterruptor` (`ui/components/Ajustes.kt`). «Lugares y traslados»
  ahora se abre desde aquí. **«Borrar todos mis datos»** (derecho del titular, CLAUDE.md §Privacidad 5) con diálogo de
  confirmación y `clearApplicationUserData()`. «Demostración de la banda» solo en la compilación de prueba.
- **Formulario**: el tipo se elige con **fichas** con la forma de estación de cada tipo (● Clase, ■ Trabajo, ▲ Puntual,
  ◆ Examen, ○ Otro), en lugar del segmentado; choques en caja `avisoSuave`; resumen en caja `rutaSuave`; pie sobre
  `superficie`; títulos de sección en `encabezado`.
- **Semestre y Lugar**: «Días sin clase» y los títulos de sección pasaron de `seccion` a `encabezado` (mismo nivel que
  el formulario).

Verificación: `assembleDebug`, `testDebugUnitTest` (**86 pruebas, 0 fallos**) y `lintDebug` sin errores; capturas en el
emulador del teléfono: `docs/capturas/sn-paso6-{ajustes,ajustes-2,formulario,formulario-2,semestre}.png`.
Pendiente declarado: el diálogo de «Borrar todos mis datos» no se pulsó en la verificación (borra el emulador de
demostración); queda probado por compilación, no por recorrido.
