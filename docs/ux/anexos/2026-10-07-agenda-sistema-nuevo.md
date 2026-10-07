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

## Siguiente
Semana, Actividades, Formulario, Ajustes y Semestre todavía usan los alias (se ven con la paleta y la tipografía nuevas,
pero con la composición vieja). Widget «Siguiente»: pendiente de pasar al `HomeWidget` del sistema.
