# Anexo UX · Agenda personal (Android) · Investigación y direcciones · 2026-09-23

Estado: **dirección A elegida por Diego el 2026-09-23. Contrato firmado (abajo). Implementación paso a paso aún no iniciada.**

---

## Contrato de diseño · Dirección A «Tablero de salidas» (elegida 2026-09-23)

Desde esta fecha, nada de color, tipografía, espaciado, forma, icono o movimiento que no salga de este contrato. Si una cláusula no funciona al implementarla, se para, se explica con una alternativa concreta y solo se cambia con permiso explícito de Diego, anotado con fecha en el «Registro de enmiendas».

Tesis: la pantalla Hoy es el panel de andén del día. La hora a la que Diego tiene que moverse es el dato más grande de todo el sistema.
Referentes: `ref/2026-09-23-pinterest-tableros-transporte.jpg`, `ref/2026-09-23-android16-progress.png`, `ref/2026-09-23-timepage-semana-timeline.png`, `ref/2026-09-23-refero-scheduling-editorial.png`.

### C1 · Respuestas de Diego (entradas fijas del contrato)

- **C1.1 Reloj**: se sigue el ajuste del teléfono (`DateFormat.is24HourFormat`). En 24 h la hora se escribe `7:32` / `19:32`. En 12 h la cifra va sola (`7:32`) y el sufijo `a. m.` / `p. m.` va **en una línea aparte, debajo**, en el rol `sufijo` (C2.3). Motivo medido en §5: `7:32 p. m.` a 96 sp ocupa ≈400 dp y no cabe en 320 dp útiles.
- **C1.2 Mano**: derecha. Acciones principales abajo a la derecha: botón Crear en la esquina inferior derecha; en la banda de salida, la acción primaria («Voy saliendo») va a la **derecha** y la secundaria («+5 min») a la izquierda. Esto invierte los bocetos de §6 A, que quedan corregidos por esta cláusula.
- **C1.3 Transporte**: cuatro modos, y solo cuatro: **Bus / transporte público**, **A pie**, **Carro**, **Moto**. No hay bici. Etiqueta en la banda en minúscula y en lenguaje de Diego: «en bus», «a pie», «en carro», «en moto». Modo por defecto en Ajustes > Avisos; se cambia por actividad tocando la etiqueta en la banda.

### C2 · Tipografía

- **C2.1 Familias** (OFL, empaquetadas en `res/font`, sin descarga en tiempo de ejecución; existencia y licencia verificadas el 2026-09-23, ver §8):
  - **Archivo** (Omnibus-Type, archivo variable `wght`+`wdth`), siempre a **ancho 62**: solo cifras de hora, códigos de salón, rótulos, sufijo y horas de fila. Pesos **800** (hora, salón) y **700** (rótulos, sufijo, fila-hora). *Enmendado 2026-09-23: sustituye a Big Shoulders Display, que no tiene `tnum` (ver Registro de enmiendas y `ref/2026-09-23-comparativa-cifras-tnum.png`).*
  - **Atkinson Hyperlegible Next**: todo lo demás. Pesos **400** (cuerpo), **600** (nombre de actividad, botones), **700** (títulos de sección).
  - Ninguna otra familia. `FontFamily.Default` no aparece en ningún `TextStyle`.
- **C2.2 Cifras tabulares**: toda hora, duración y cuenta atrás lleva `fontFeatureSettings = "tnum"` para que no bailen al cambiar. Verificado con fontTools el 2026-09-23: Archivo y Atkinson Hyperlegible Next traen `tnum`. Atkinson no tiene el glifo «→»; por eso el rango `8:00 → 10:00` va siempre en Archivo.
- **C2.3 Escala** (sp, altura de línea en sp, familia, peso). Única escala permitida:

| Rol | Tamaño | Línea | Familia | Peso | Uso |
|---|---|---|---|---|---|
| `hora-salida` | 96 | 88 | Archivo 62 | 800 | La hora de salida en la banda. Solo ahí |
| `salon` | 40 | 44 | Archivo 62 | 800 | Código de salón en la banda y en Detalle |
| `rotulo` | 20 | 24 | Archivo 62 | 700, mayúsculas, +0,04 em | «SAL A LAS», «SAL YA», «VAS TARDE», «LLEGAS» |
| `sufijo` | 24 | 28 | Archivo 62 | 700 | `a. m.` / `p. m.` bajo la hora (solo en 12 h) |
| `actividad` | 28 | 32 | Atkinson | 600 | Nombre de actividad en la banda; máximo 2 líneas y luego elipsis |
| `seccion` | 20 | 26 | Atkinson | 700 | Títulos de pantalla y sección |
| `fila-hora` | 18 | 24 | Archivo 62 | 700 | Rango `8:00 → 10:00` en la lista del día |
| `cuerpo` | 16 | 24 | Atkinson | 400 | Texto de lista, formularios. Mínimo absoluto de texto que se lee |
| `meta` | 14 | 20 | Atkinson | 400 | Modo, margen, frescura del dato, lugar secundario |
| `boton` | 16 | 20 | Atkinson | 600 | Etiquetas de botón |

  Razón hora/cuerpo: 96 / 16 = **6,0×**. No existe texto por debajo de 14 sp, ni siquiera en la celda de Semana (se abrevia el contenido; no se reduce el cuerpo).
- **C2.4 Escala del sistema**: se respeta `fontScale`. Con escala ≥ 1,3 la `hora-salida` se limita a 112 sp y el nombre de actividad pasa a 3 líneas máximo; nada se recorta.
  - *Enmienda 2026-09-24*: la etiqueta de una pestaña de la barra que no quepa en una línea se reduce **solo ella**, lo justo (pasos de 0,5 sp), con un mínimo de **14 sp efectivos**, y **nunca parte una palabra** (sin guiones ni corte). El resto de la barra no cambia.

### C3 · Color

Tema propio. `dynamicColor` **desactivado**. Los hex son la fuente de verdad; los roles M3 (`primary`, `surface`…) se mapean a estos tokens y no al revés.

| Token | Claro | Oscuro | Único uso |
|---|---|---|---|
| `papel` | `#F2EFE6` | `#0E0F0C` | Fondo de toda superficie |
| `papel-2` | `#E8E4D8` | `#1A1B17` | Barra de navegación inferior y hojas inferiores (segundo neutro) |
| `tinta` | `#14130F` | `#F2EFE6` | Texto principal, iconos activos, botón Crear |
| `tinta-2` | `#5A574E` | `#A29E92` | Meta, dato viejo, iconos inactivos. **No** en horas (enmienda 2026-09-23: las horas pasadas van en `tinta`, C9.1) |
| `filete` | `#14130F` al 18 % | `#F2EFE6` al 18 % | Filetes de separación. Nada más |
| `senal` | `#FFB000` | `#FFB547` | **Solo** el campo de la banda de salida (estados «espera», «prepárate», «sal ya», «en camino»). Prohibido en cualquier otro sitio: listas, iconos, botones fuera de la banda, widget fuera de su banda, selección, foco |
| `sobre-senal` | `#14130F` | `#0E0F0C` | Texto e iconos sobre `senal` |
| `tarde` | `#8E2A17` | `#8E2A17` | **Solo** el campo de la banda en el estado «vas tarde» |
| `sobre-tarde` | `#F3EEE2` | `#F3EEE2` | Texto sobre `tarde` |
| `alerta-texto` | `#8E2A17` | `#FF8A6B` | Texto de conflicto o de error inline (con icono, nunca solo color) |
| `clase` | `#1F4FB8` | `#8FB0FF` | Marcador de tipo Clase |
| `trabajo` | `#1E6B45` | `#6FCB98` | Marcador de tipo Trabajo |
| `puntual` | `tinta` en contorno | `tinta` en contorno | Marcador de tipo Puntual |
| `foco` | `#14130F` | `#F2EFE6` | Anillo de foco (C9.4) |

- **C3.1** El ámbar es **solo** para la salida. Si alguna vez aparece fuera de la banda, es un fallo.
- **C3.2** Los colores de tipo solo pintan el marcador cuadrado (C6). No tiñen filas, fondos ni texto.
- **C3.3** El estado nunca depende solo del color: la banda escribe siempre «SAL A LAS», «SAL YA», «VAS TARDE» o «LLEGAS».
- **C3.4** Nunca `#000000` ni `#FFFFFF`.
- **C3.5** En notificaciones y Live Update no hay color propio (Android no permite `setColorized` en Live Update). La identidad va en la redacción (C8.3).

### C4 · Espaciado y retícula

- **C4.1 Escala** (dp), única permitida: **4 · 8 · 12 · 16 · 20 · 24 · 32 · 48 · 64**. Cualquier otro valor exige justificación escrita junto al código.
- **C4.2 Retícula**:

| Ancho | Margen lateral | Columnas | Medianil | Columna de horas (Hoy) |
|---|---|---|---|---|
| 360 dp | 20 | 4 | 12 | 64 |
| 412 dp | 24 | 4 | 16 | 64 |
| ≥ 600 dp (plegable/tablet) | 32 | 8 | 16 | 64; la banda ocupa 5 columnas y se alinea a la izquierda |

- **C4.3 Banda de salida**: sangra por la izquierda, la derecha y por arriba (se dibuja bajo la barra de estado con `WindowInsets`; el texto respeta el inset). Relleno interno: 20 dp lateral (24 dp a 412), 24 dp arriba bajo el inset, 20 dp abajo.
- **C4.4 Filas del día**: altura mínima de 64 dp, relleno vertical de 12 dp y un filete debajo. Entre la banda y la primera fila, 24 dp.
- **C4.5 Asimetría**: la columna de horas y el hilo del día quedan fijos a la izquierda; el contenido nunca se centra.
- **C4.6 Densidad**: en Hoy a 360×800 dp, entre 5 y 7 filas visibles bajo la banda. En Semana, franjas de 48 dp por hora.

### C5 · Formas y radios

- **C5.1** Radio **0 dp** en todo: banda, filas, botones, campos de texto, hojas inferiores, snackbar, menús, widget, celdas de Semana. Se sobrescriben las `Shapes` de M3 (`extraSmall` … `extraLarge` = 0).
- **C5.2** Sin sombras ni elevación tonal de M3. La separación se hace con filete, con `papel-2` o con la banda. Nada más.
- **C5.3** Botones: rectángulos de 48 dp de alto. La acción primaria de la banda va en `tinta` sobre `senal` (relleno `sobre-senal`, texto `senal`); la secundaria es solo texto con subrayado de 1,5 dp. Botón Crear: cuadrado de 56 dp en `tinta` con icono `+` en `papel`, abajo a la derecha (C1.2).
- **C5.4** Excepción única: el marcador del estado «ahora» es un punto de 8 dp (círculo), porque marca un instante y no un bloque.

### C6 · Repertorio cerrado (cada forma y su único trabajo)

| Forma | Único trabajo | Dónde |
|---|---|---|
| Banda `senal`/`tarde` a sangre | Decir cuándo moverse | Una por pantalla: Hoy y widget. Nunca en listas |
| Filete de 1,5 dp | Separar filas | Listas, formularios, Semana |
| Cuadrado de 10 dp relleno (clase/trabajo) o en contorno (puntual) | Tipo de actividad | Junto al nombre, en filas y celdas |
| Hilo vertical de 2 dp en la columna de horas | Progreso del día: relleno `tinta` hasta ahora y filete punteado `tinta-2` después | Hoy |
| Marca «✓» dibujada sobre el hilo (dos trazos rectos de 2 dp, `tinta`, sin glifo ni emoji) | Sesión terminada (enmienda 2026-09-23) | Filas del día ya pasadas |
| Línea de «ahora» (punto de 8 dp + filete + hora escrita) | Hora actual | Hoy y la columna del día actual en Semana |
| Inversión papel/tinta | Día actual en la tira de Semana y pestaña activa | Semana, navegación |
| Triángulo de aviso (icono) + texto `alerta-texto` | Conflicto o traslado insuficiente | Inline bajo la fila afectada |
| Campo de texto: rectángulo con borde de 1,5 dp en `tinta-2`, `tinta` con foco, `alerta-texto` con error; etiqueta encima y error debajo con icono | Escribir un dato (enmienda 2026-09-24) | Formularios |
| Selector segmentado: opciones con filete de 1,5 dp, la elegida invertida (papel/tinta) | Elegir una opción entre pocas (enmienda 2026-09-24) | Formularios |
| Casillas de días `L M X J V S D` con la construcción del selector; área táctil ampliada a 48 dp | Elegir días de la serie (enmienda 2026-09-24) | Formulario de actividad |
| Alfiler del mapa: filete vertical + cuadrado de 10 dp | Marcar el lugar elegido (enmienda 2026-09-24) | Mapa del formulario |

Nada más. Una forma nueva exige enmienda.

### C7 · Iconografía

- **C7.1 Familia**: **Material Symbols Sharp** (Apache 2.0), variable, con **peso 500, grado 0, relleno 0 y tamaño óptico 24**. Motivo: las esquinas rectas encajan con el radio 0 (C5.1) y el peso 500 iguala el trazo de Atkinson 400–600 al tamaño de uso.
- **C7.2 Tamaños**: 24 dp en navegación y acciones y 20 dp en línea con texto `meta`, siempre dentro de un área táctil ≥ 48 dp.
- **C7.3 Lista cerrada inicial**: hoy, semana, lista (actividades), añadir, ajustes, ubicación, bus, a pie, carro, moto, aviso (triángulo), deshacer, cerrar, flecha atrás. Un icono nuevo exige que exista en la misma familia y variante; no se mezclan otras familias ni ningún emoji.
- **C7.4 Marcadores de tipo**: no son iconos, son las formas de C6.

### C8 · Movimiento

- **C8.1 Solo `transform` y `opacity`** (`graphicsLayer`: `translationX/Y`, `scaleX/Y`, `alpha`). Nunca se anima tamaño, relleno ni posición de layout.
- **C8.2 Inventario cerrado**:

| Qué | Cómo | Duración | Curva |
|---|---|---|---|
| Cambio de estado de la banda (espera → prepárate → sal ya → tarde → en camino) | Fundido cruzado de `alpha` entre dos capas | 200 ms | `CubicBezier(0.22, 1, 0.36, 1)` (ease-out-quint) |
| Cambio de dígito de la hora (tablero) | `translationY` de −8 dp a 0 más `alpha` 0 → 1, **solo en los dígitos que cambian** | 180 ms | ease-out-quint |
| Hilo del día rellenándose | `scaleY` del relleno, origen arriba, al ritmo del reloj (una actualización por minuto, sin interpolación continua) | 240 ms | ease-out-quint |
| Hoja inferior (crear, editar) | `translationY` con `spring(dampingRatio = 0.9f, stiffness = 400f)`, interrumpible por arrastre | — | spring sin rebote visible |
| Pulsación de botón | `scale` a 0,97 y vuelta | 120 ms | ease-out-quint |

- **C8.3** No se anima: cambio de pestaña (corte directo), aparición de listas, scroll, entrada de pantallas y carga. Ninguna secuencia de entrada.
- **C8.4 Movimiento reducido**: si `ANIMATOR_DURATION_SCALE` es 0 o está activo «quitar animaciones» de accesibilidad, todo pasa a corte instantáneo; la hoja inferior aparece sin desplazamiento.
- **C8.5** Toda animación es interrumpible: un nuevo estado la reemplaza desde el valor actual, sin cola.

### C9 · Accesibilidad

- **C9.1 Contraste**: AA (4,5:1) como piso en todo texto; **AAA (≥ 7:1) en la hora de salida, el salón y las horas de la lista, también las ya pasadas** (se distinguen por la marca «✓» y el hilo, C6; enmienda 2026-09-23). Los textos de ejemplo de los campos vacíos («8:00», «10:00») son ejemplos, no datos: van en `tinta-2` con AA (enmienda 2026-09-24). Medido sobre tokens en §8 y se vuelve a medir **sobre captura del dispositivo** en cada paso.
- **C9.2 Toque**: ≥ **48 dp** en todo lo tocable, incluidos los chips de días `L M X J V S D`, la etiqueta de modo de transporte y las celdas de Semana (si la celda visual es menor, el área táctil se amplía).
- **C9.3 TalkBack**: la banda se anuncia como una frase: «Sal a las 7 y 32 para Cálculo diferencial, salón B-204, 23 minutos en bus más 7 de margen, calculado hace 3 minutos». Los marcadores de tipo tienen etiqueta («Clase»). La línea de ahora es decorativa para TalkBack; la hora actual la da el sistema.
- **C9.4 Foco** (teclado físico, switch access): anillo de 2 dp en `foco` con separación de 2 dp y radio 0. El orden es banda (acción primaria, luego secundaria, luego modo), filas de arriba abajo, navegación y Crear.
- **C9.5** `fontScale` hasta 2,0 sin recortes (C2.4). Nada depende solo del color (C3.3).

### C10 · Estados (todos con vista propia; copia literal)

| Estado | Banda | Lista |
|---|---|---|
| Espera (> 15 min para salir) | `senal`, «SAL A LAS» + hora | Normal |
| Prepárate (≤ 15 min) | `senal`, «SAL A LAS» + cuenta atrás en `meta` («en 12 min») | Normal |
| Sal ya | `senal`, «SAL YA» + «Cálculo empieza 8:00» | Normal |
| Vas tarde | `tarde`, «VAS TARDE» + «llegas 8:04 a B-204» | Normal |
| En camino (tras «Voy saliendo») | `senal`, «LLEGAS» + hora de llegada | Normal |
| Sin traslado (misma sede que la actividad anterior) | Sin banda; la fila siguiente se muestra con `hora-salida` reducida a `salon` | Normal |
| Cargando | Última hora conocida en `tinta-2` + «Recalculando trayecto…» | Esqueleto de filas en `papel-2`, sin spinner |
| Vacío, primer uso | Sin banda. «Empieza por tu horario de clases» + botones «Añadir clase» y «Añadir turno» | — |
| Vacío, día sin nada | Sin banda. «Hoy no tienes nada fijo.» + «Siguiente: lunes 7:00 · Cálculo · B-204» | — |
| Festivo o semana sin clase | Sin banda si no hay turnos; «Festivo: sin clases hoy» | Turnos del día |
| Error de ruta | `senal` con margen fijo: «No pude calcular el trayecto. Te aviso 30 min antes.» + «Reintentar» | Normal |
| Sin permiso de ubicación | `senal`; la etiqueta del modo dice «desde Casa» | Normal |
| Sin permiso de notificaciones | Franja fija sobre la banda en `papel-2`: «Sin avisos: no te llegará nada» + «Activar» | Normal |
| Dato viejo (> 15 min) | «estimado hace 25 min» en `meta`; la hora lleva «aprox.» en `sufijo` | Normal |
| Conflicto | — | Inline bajo la fila: icono de aviso + «25 min de traslado, necesitas 35» (traslado) o «Se cruza con {actividad} {hora}», p. ej. «Se cruza con Física 9:00» (solape; enmienda 2026-09-23) |

Las notificaciones siguen el orden **tiempo → qué → dónde** («Sal a las 7:32 · Cálculo» / «B-204 · 23 min en bus + 7 de margen»). El Live Update solo arranca tras «Voy saliendo», con la acción «Quitar», y el chip dice `7:32` o `12min`.

### C11 · Prohibido

- Fuentes del sistema o de inercia (Roboto, Inter, Poppins, Montserrat, Open Sans, Lato, Nunito, Segoe, Arial, Helvetica…) y `FontFamily.Default`.
- `dynamicColor` / Material You, el violeta por defecto de M3 y cualquier degradado.
- Ámbar fuera de la banda de salida.
- Tarjetas con sombra, elevación tonal, radios mayores que 0 (salvo el punto de «ahora»), glassmorphism y blur.
- Filetes laterales de color en filas o tarjetas (`border-left` de acento).
- Tarjetas anidadas y rejillas de tarjetas iguales.
- Emoji como icono; iconos de otra familia o de otra variante (Rounded, Outlined).
- Texto por debajo de 14 sp; estado comunicado solo con color.
- Diálogos modales para conflictos, errores o confirmaciones que caben en línea (se permiten solo para borrar una actividad recurrente entera).
- Animar layout, animar el cambio de pestaña, secuencias de entrada, rebotes o elásticos, pulsos o parpadeos para urgencia.
- Saludos, frases motivacionales, rachas, estadísticas o clima decorativo en Hoy.
- Colorear notificaciones o convertir en Live Update un evento que todavía no ha empezado sin que Diego lo inicie.
- Valores de espaciado fuera de C4.1 sin justificación escrita.

### Fuera de alcance de este contrato

Vista de mes, tareas y exámenes (se diseñarán con enmienda cuando se aborden), modo tablet más allá de C4.2, tile de ajustes rápidos e icono de la app.

### Registro de enmiendas

| Fecha | Cláusula | Cambio | Motivo | Aprobado por |
|---|---|---|---|---|
| 2026-09-23 | Fuera de alcance | Entra en el producto todo el alcance de `docs/arquitectura.md` §6 (P1–P3): tareas y exámenes, modo clase, resumen matutino, asistencia, horas trabajadas, `.ics`, notas por clase, Wear OS. Su diseño visual se propondrá como enmienda de cláusulas cuando se aborde cada uno, dentro de C2–C8. | Diego pidió «en ambas app añade todo lo que me recomendaste» | Diego |
| 2026-09-23 | C2.1 (y C2.2, C2.3) | Big Shoulders Display se sustituye por **Archivo** (archivo variable, ancho 62; 800 para hora y salón; 700 para rótulos, sufijo y fila-hora). | Big Shoulders no tiene `tnum`: «11:11» mide 121 dp y «10:48» 184 dp a 96 sp, y la hora cambiaría de ancho cada minuto (incumple C2.2 y C8.2). Archivo con `tnum`: 186 y 186 dp. Evidencia: `ref/2026-09-23-comparativa-cifras-tnum.png`. Es la misma familia que usará la app de gastos. | Diego |
| 2026-09-23 | C3, C6, C9.1 | Las horas ya pasadas de la lista van en `tinta` (AAA), no en `tinta-2`. Lo pasado se distingue con una marca «✓» dibujada sobre el hilo del día (nueva forma en C6) y con el propio hilo. | Contradicción entre C3 (tinta-2 para horas pasadas, 6,28 en claro) y C9.1 (AAA en las horas de la lista) detectada en el paso 4. | Diego |
| 2026-09-23 | C10 | Texto de solape: «Se cruza con {actividad} {hora}», p. ej. «Se cruza con Física 9:00». | C10 solo tenía el texto de traslado insuficiente. | Diego |
| 2026-09-23 | (aclaración, sin cambio de cláusula) | El dato viejo es a partir de 15 min, como dice C10. El dominio usaba 30 min por error; se corrigió en `DepartureCalculator.STALE_AFTER` con su prueba. | Discrepancia detectada en el paso 4. | Diego |
| 2026-09-24 | C6 | Se añaden al repertorio cuatro formas del formulario: campo de texto, selector segmentado, casillas de días y alfiler del mapa (tal como se propusieron en el paso 5). | El formulario (paso 5) las necesitaba y el repertorio cerrado no las tenía. | Diego («acepto todas») |
| 2026-09-24 | C9.1 | Los textos de ejemplo de los campos vacíos van en `tinta-2` (6,28:1, AA), no AAA. | Son ejemplos, no datos que se leen para decidir. | Diego («acepto todas») |
| 2026-09-24 | (decisiones del paso 5, sin cambio de cláusula) | Formulario a pantalla completa; horas escritas como texto («8», «830», «8 p. m.», «24/9»); margen 10 min y aviso 15 min por defecto; serie de 17 semanas si no hay semestre; «Deshacer» solo en actividades nuevas. | Confirmación pendiente del paso 5. | Diego («acepto todas») |
| 2026-09-24 | C2.4 | Etiqueta de pestaña que no cabe: se reduce solo ella, lo justo, mínimo 14 sp efectivos, sin partir palabras; el resto de la barra no cambia. | Con fuente 2,0, «Actividades» se partía en «Activida / des» (medido en el emulador, paso 4). | Diego |

---

Skills cargadas: `direccion-de-diseno` (puerta de entrada), `impeccable` (registro producto).
`imagegen-frontend-mobile` no se usó: los bocetos ASCII y las capturas de referentes bastan para decidir; generar imágenes ahora añadiría una estética que nadie ha elegido.
`impeccable` pide `PRODUCT.md`; el proyecto no existe aún, así que su contenido equivalente está en el Encuadre de este anexo.

---

## 1. Encuadre (etapa 1: identificación del usuario)

### Ficha de usuario

| Campo | Dato | Origen |
|---|---|---|
| Quién | Diego, único usuario. Estudiante universitario que además trabaja por turnos. | Encargo |
| Necesidad central | No llegar tarde a la siguiente cosa y saber **dónde** es (lugar + salón) sin pensar. | Encargo |
| Experiencia técnica | Alta: desarrolla la app él mismo (Kotlin + Compose). Tolera ajustes finos; no tolera fricción repetida. | Encargo |
| Edad / etapa | Universitario (se asume 18–30). Horario que cambia cada semestre. | Supuesto razonable, no verificado |
| Género | No relevante para ninguna decisión de esta pieza; no se usa. | — |
| Cultura / idioma | Interfaz en español. La sesión de Pinterest del usuario redirige a `co.pinterest.com`, lo que sugiere Colombia: convención horaria habitual de 12 h con «a. m./p. m.». **Pendiente de confirmar** (ver Preguntas). | Observado, no confirmado |
| Nivel de habilidad con la app | Experto desde la semana 2: abre la app decenas de veces, casi siempre para una sola consulta. | Deducido del uso |

### Condiciones reales de uso

- **Con prisa y en movimiento**, a una mano (la otra lleva mochila, café, pasamanos del bus).
- **En exterior con sol**: pantalla con brillo alto y reflejo; el contraste bajo desaparece.
- **Temprano por la mañana**: primera consulta del día medio dormido; el resumen matutino llega antes que la voluntad de abrir la app.
- **Conectividad irregular** entre casa, bus y campus: la estimación de trayecto puede estar vieja.
- **La mayoría de consultas no abren la app**: ocurren en la notificación, el chip de la barra de estado o el widget.

### Restricción dominante

Leer **una hora y un sitio** en menos de 2 segundos, al sol, con el pulgar. Todo lo demás cede ante eso.

## 2. Análisis de la comunicación (etapa 2)

**Qué tiene que entender en 2 segundos** (notificación, widget o pantalla Hoy), en este orden:

1. **Cuándo tiene que moverse**: «Sal a las 7:32» (o «Sal ya», o «Vas 4 min tarde»).
2. **A qué**: «Cálculo».
3. **Adónde exactamente**: «B-204», y solo después «Bloque B · Campus».
4. **Por qué esa hora** (confianza en el cálculo): «23 min en bus + 7 de margen · estimado hace 3 min».

**Tarea dominante**: decidir *¿me muevo ya o todavía no?*. La segunda tarea es *¿qué me queda hoy?*; la tercera, *¿cómo es mi semana?* (planificación, con calma, sentado).

**Qué NO va en Hoy**: mes completo, estadísticas, rachas, frases motivacionales, clima decorativo (solo si cambia la hora de salida, p. ej. lluvia añade margen), avatares, saludo «¡Buenos días!» que empuje la hora hacia abajo.

**Registro**: Producto (lectura obligada, §3.2 de la skill). El punto de entrada **es la tarea**: la hora de salida. Cualquier gesto expresivo se concentra en ese único momento.

---

## 3. Investigación de referentes

Evidencia guardada en `docs/ux/anexos/ref/`. Formato: observación → decisión aplicable → límite que no copiaré.

### 3.1 styles.refero.design (consultada, cuenta)

- **Cron Calendar**, `https://styles.refero.design/style/476184db-a4e6-440b-aa53-27294668361c` · `ref/2026-09-23-refero-cron-ember.png`
  Observación: casi todo vive en una banda estrecha de negro cálido, gris y blanco; un único naranja `#ff4700` reservado para acciones primarias y momentos de marca, sin colores secundarios en estados ni insignias.
  Decisión: **racionar el color de acción a un solo uso** (la hora de salida). Base para la dirección C.
  Límite: no copio su naranja exacto ni su tipografía (Helvetica Neue, prohibida aquí), ni el titular de 140 px de landing.
- **GlossGenius «Scheduling»**, `https://styles.refero.design/style/7ad5549e-9baa-4fda-ac43-79d568a86b98` · `ref/2026-09-23-refero-scheduling-editorial.png`
  Observación: producto de citas tratado como pliego impreso: tinta casi negra sobre crema, filetes de 1,5 px en vez de 1 px, sin sombras, separación por alternancia de tinte.
  Decisión: **filetes de 1,5 dp y cero sombras** para separar filas de horario (direcciones A y B).
  Límite: no copio su amarillo ni su serif Basel Classic (comercial).
- Búsquedas de soporte: `ref/2026-09-23-refero-calendar.png` (resultados de «calendar»: Cal.com, Calendly, Cron, Savvycal, Amie…), `ref/2026-09-23-refero-blueprint.png` (familia «blueprint & hairlines», descartada: ver Descartes).

### 3.2 mobbin.com (consultada en páginas públicas; la biblioteca completa pide login)

- **Patrón «Calendar» móvil**, `https://mobbin.com/explore/mobile/screens/calendar` · `ref/2026-09-23-mobbin-todoist-recurrencia-y-linea-ahora.jpg`
  Observación 1: en el selector de fecha de Todoist la recurrencia se resume en **una frase** arriba («Every weekday starting Feb 12 at 2:00 PM…») antes de los controles.
  Decisión: el formulario de actividad recurrente muestra siempre la **frase de resumen** («Cada lunes y miércoles, 7:00–9:00, B-204, del 3 ago al 28 nov») y es lo que Diego verifica antes de guardar.
  Observación 2: en la vista día de Todoist, una línea roja con la hora actual (`02:19`) cruza la rejilla.
  Decisión: **línea de «ahora»** en Hoy y en Semana, pero como filete de tinta con la hora escrita, no como punto rojo.
  Límite: no copio el rojo Todoist ni su lista con círculos de prioridad.
- **Patrón «Schedule» móvil**, `https://mobbin.com/explore/mobile/screens/schedule` · `ref/2026-09-23-mobbin-schedule-pattern.jpg`
  Observación: los selectores de franja resaltan el día actual como bloque sólido invertido («Today / Jul 15») y el resto como texto plano.
  Decisión: en la tira de días de Semana, **hoy va invertido** (tinta sobre papel → papel sobre tinta); los demás días sin caja.
  Límite: no copio chips de franja horaria de delivery.
- No accesible: la búsqueda de apps (`/search/apps/android?q=calendar`) redirige a login. No se introdujeron credenciales. Registrado 2026-09-23.

### 3.3 Pinterest (consultada con la sesión del usuario en Chrome)

- **Búsqueda «transit timetable typography swiss»**, `https://co.pinterest.com/search/pins/?q=transit%20timetable%20typography%20swiss` · `ref/2026-09-23-pinterest-tableros-transporte.jpg`
  Observación: el tablero de London Overground pone **la hora (22:52) y el destino (Chingford) a un tamaño que se lee desde lejos**, y el origen en cuerpo pequeño encima; los carteles del metro de NY usan bandas negras a sangre con texto blanco.
  Decisión: la tarjeta de salida de la dirección A es literalmente un **panel de andén**: hora enorme, destino grande, contexto pequeño. Referente que intimida (1/3).
  Límite: no copio iconografía de líneas de metro ni los colores MTA; nada de mapas de metro decorativos.
- **Búsqueda «weekly timetable poster grid editorial»**, `https://co.pinterest.com/search/pins/?q=weekly%20timetable%20poster%20grid%20editorial` · `ref/2026-09-23-pinterest-calendarios-editoriales.jpg`
  Observación 1: un calendario semanal naranja con **numerales de día gigantes recortados** por el borde derecho (8, 9, 10… June).
  Observación 2: una rejilla de mes impresa **a una sola tinta verde** (riso) con el número en la esquina de cada celda y filetes finos.
  Decisión: base de la dirección B (numeral del día recortado + una tinta sobre papel). Referente que intimida (2/3).
  Límite: no copio el papel arrugado ni el gato ilustrado; el grano solo si cumple una función.

### 3.4 ui.aceternity.com (consultada, cuenta)

- **Timeline**, `https://ui.aceternity.com/components/timeline` · `ref/2026-09-23-aceternity-timeline.png`, `ref/2026-09-23-aceternity-timeline-beam.png`
  Observación: un hilo vertical se va **rellenando** a medida que avanzas; el año queda fijo a la izquierda en gris grande.
  Decisión: en Hoy, el hilo de la izquierda se rellena **hasta la hora actual** (ligado al reloj, no al scroll): lo hecho queda relleno, lo pendiente en filete. Se anima solo con `scaleY` (transform).
  Límite: no copio el degradado azul‑violeta del hilo (prohibido) ni el comportamiento ligado al scroll.

### 3.5 Emil Kowalski, emilkowal.ski (consultada, cuenta)

- **Great Animations**, `https://emilkowal.ski/ui/great-animations` · `ref/2026-09-23-emil-great-animations.png`
  Observación: la demo de la Dynamic Island muestra **un solo objeto que cambia de forma** entre estados (Idle, Ring, Timer) en vez de aparecer uno nuevo; duración recomendada < 300 ms, ease‑out, solo `transform`/`opacity`, interrumpible, no animar acciones repetidas de teclado.
  Decisión: la tarjeta de salida es **un único objeto que muta de estado** (falta rato → prepárate → sal ya → vas tarde → en camino), nunca se apila una segunda alerta. Transiciones de 180–240 ms, `spring(dampingRatio = 0.9, stiffness = 400)` en Compose, interrumpibles. Nada animado en el cambio de pestaña ni en listas.
  Límite: no copio el rebote juguetón de la isla: aquí amortiguación alta, sin overshoot visible.

### 3.6 tasteskill.dev (consultada, cuenta con reservas)

- `https://tasteskill.dev`
  Observación: su v2 exige inferir el brief (industria, audiencia, ánimo) y **mapear a un sistema de diseño real** (Material, Fluent…) antes de estilizar, con paridad de contraste y jerarquía entre claro y oscuro.
  Decisión: Material 3 queda como **esqueleto de comportamiento** (navegación inferior, hojas, estados, accesibilidad); la identidad sale de tokens propios (tipo, color, filetes, forma), no de `dynamicColor`. Cada dirección se especifica en claro **y** oscuro con el mismo contraste medido.
  Límite: la página no da reglas visuales concretas; no la cito para ninguna decisión de estética.

### 3.7 motionsites.ai (consultada, **no cuenta** como una de las seis)

- `https://motionsites.ai` · `ref/2026-09-23-motionsites-home.png`
  Observación: catálogo de plantillas de landing con fondos animados, vídeo y titulares con brillo.
  No aporta nada a una agenda nativa de lectura obligada; citarla para llegar al número disfrazaría una decisión por defecto. Registrada como consultada sin uso.

### 3.8 Fuentes adicionales (no están en la lista, pero sostienen decisiones)

- **Android 16, notificaciones de progreso**, `https://developer.android.com/about/versions/16/features/progress-centric-notifications` · `ref/2026-09-23-android16-progress.png`
  Observación: la notificación de navegación arranca con el **estado en tiempo** («Arrive in 1 min · 100 m») y debajo una barra con marcador de posición.
  Decisión: el título de toda notificación es el tiempo accionable («Sal a las 7:32»), no el nombre del evento.
- **Criterios de Live Update**, `https://developer.android.com/develop/ui/views/notifications/live-update` · `ref/2026-09-23-android-live-update-criterios.png`
  Observación: los Live Updates **no admiten «upcoming calendar events»**; deben ser en curso, iniciados por el usuario y sensibles al tiempo; no pueden ir coloreados (`setColorized` prohibido); el chip de estado mide 96 dp y muestra entero un texto de menos de 7 caracteres.
  Decisión: el Live Update **solo empieza cuando Diego toca «Voy saliendo»** (o si lo activa explícitamente por actividad), con acción «Quitar». Antes de eso, notificaciones normales. Chip: `7:32` o cuenta atrás `12min`. La identidad de la app **no** puede depender del color en la notificación: depende de la redacción.
- **Timepage (Moleskine Studio / Bonobo)**, `https://moleskinestudio.com/timepage` · `ref/2026-09-23-timepage-hoy-campo-color.png`, `ref/2026-09-23-timepage-semana-timeline.png`, `ref/2026-09-23-timepage-mes-monocromo.png`
  Observación: el móvil entero se tiñe de **un solo color** (naranja) y todo es tono sobre tono; los rangos se escriben «11:00 → 12:00» y el lugar va en tercera línea; en semana, el día va apilado a la izquierda («MON / 9») y el día actual enmarcado. Referente que intimida (3/3).
  Decisión: rango con flecha «7:00 → 9:00» (A y C); día apilado en el margen (A y B); campo de color entero (C).
  Límite: no copio su tipografía, sus tarjetas redondeadas con filete lateral de color (prohibido) ni el tiempo decorativo.
- **Apple Calendar, Time to Leave**, `https://support.apple.com/guide/calendar/icl43600/mac` y `https://discussions.apple.com/thread/253937225`
  Observación: avisa antes de salir, al salir y si vas tarde; el fallo más citado es que **calcula a pie cuando el usuario va en coche** porque el modo de transporte está escondido en Ajustes.
  Decisión: la tarjeta de salida **siempre muestra el modo** («en bus») y tocarlo lo cambia para esa actividad. Tres avisos: prepárate, sal ya, vas tarde.
- **Notion Calendar**, `https://www.notion.com/product/calendar` · `ref/2026-09-23-notion-calendar.png`
  Observación: en la barra de menús aparece «All Hands · 50m left»: el siguiente evento vive en la superficie del sistema, no dentro de la app.
  Decisión: el widget y el chip son superficies de primera clase, diseñadas al mismo nivel que Hoy.
- **Structured**, `https://structured.app` · `ref/2026-09-23-structured-home.png`: la portada es un vídeo de ambiente; no muestra la app. `/features` devuelve 404. **Sin observación utilizable**; no sostiene ninguna decisión.

### Recuento

Cuentan como fuentes de pantalla: refero, Mobbin, Pinterest, Aceternity, Emil Kowalski, tasteskill → **6 de 7**. motionsites.ai consultada sin aporte. Imágenes en disco: 23 (de 8 orígenes). Referentes que intimidan: tablero de andén (Pinterest), calendario con numerales recortados (Pinterest), Timepage en campo de color.

---

## 4. Design Read

```text
Registro y audiencia: Producto, lectura obligada; Diego, estudiante que trabaja, único usuario, experto tras una semana.
Escena de uso: 6:50 a. m., acera o bus, sol de frente, una mano, brillo alto, sin ganas de pensar.
Tesis visual: la app es un tablero de salidas personal: la hora a la que te mueves es el dato más grande de todo el sistema.
Jerarquía y decisión dominante: Sal a las 7:32 > Cálculo > B-204 > por qué (modo, margen, frescura del dato) > resto del día.
Materiales: dos familias OFL empaquetadas en res/font, tokens propios sobre M3, filetes 1,5 dp sin sombras, movimiento 180-240 ms solo transform/opacity.
Repertorio: ver cada dirección (lista cerrada, un trabajo por forma).
Puntos de entrada: la tarea misma (hora de salida); nada decorativo compite con ella.
Riesgo que se evita: la agenda de fábrica (tarjetas redondeadas con sombra y filete lateral de color, violeta M3 por defecto, saludo con emoji, FAB morado).
```

## 5. Medición previa de la copia (§3.3)

Ancho de referencia: 360 dp (peor caso común) con márgenes de 20 dp → 320 dp útiles. 412 dp como caso holgado.

| Texto real | Caracteres | Cuerpo | Ancho estimado | Veredicto |
|---|---|---|---|---|
| `7:32` hora de salida | 4 | 96 sp condensada (≈0,42 em/car) | ≈160 dp | Cabe con aire; permite 112 sp |
| `7:32 p. m.` (si se confirma 12 h) | 10 | 96 sp | ≈400 dp | **No cabe**: «p. m.» baja a 24 sp al lado. Decisión pendiente de la pregunta 1 |
| `Cálculo diferencial` | 19 | 28 sp | ≈280 dp (sans ≈0,52 em) | Cabe en 1 línea a 360 dp |
| `Laboratorio de Física Mecánica` | 30 | 28 sp | ≈440 dp | 2 líneas; se permite, máximo 2, luego elipsis |
| `B-204` salón | 5 | 40 sp | ≈105 dp | Cabe; se promueve a segundo dato |
| `Bloque B · Campus central` | 25 | 16 sp | ≈210 dp | Cabe |
| `23 min en bus + 7 de margen · hace 3 min` | 41 | 14 sp | ≈300 dp | Justo; en 360 dp se parte en 2 líneas por el «·» |
| Notificación título `Sal a las 7:32 · Cálculo` | 24 | sistema | < 1 línea colapsada | Cabe |
| Notificación texto `B-204 · 23 min en bus + 7 de margen` | 35 | sistema | 1 línea | Cabe |
| Chip de estado `7:32` / `12min` | 4 / 5 | sistema | < 7 car. | Se muestra entero |

Razón de escala: 96 / 16 = **6,0×** (cumple el piso ≥ 6×). La lista del día usa 16 sp de cuerpo mínimo; nada de 12 sp en exterior.

---

## 6. Direcciones

Las tres respetan: toque ≥ 48 dp (Android) sobre el mínimo de 44, contraste AA de piso y AAA en cifras (medido, ver §8), foco visible con navegación por teclado/switch, `reduced motion` leído de `Settings.Global.ANIMATOR_DURATION_SCALE` / accesibilidad, y los cinco estados.
Colores por tipo de actividad: **Clase**, **Trabajo**, **Puntual** (fijo en las tres).

### A · «Tablero de salidas» (RECOMENDADA)

**Tesis**: la pantalla Hoy es el panel de andén de tu día: la hora de salida en condensada negra enorme sobre una banda ámbar a sangre; debajo, las siguientes «salidas» como líneas de tablero.

**Referentes**: `ref/2026-09-23-pinterest-tableros-transporte.jpg` (22:52 Chingford), `ref/2026-09-23-android16-progress.png` (estado en tiempo como titular), `ref/2026-09-23-timepage-semana-timeline.png` (día apilado, rango con flecha), `ref/2026-09-23-refero-scheduling-editorial.png` (filetes 1,5 dp, sin sombras).

**Tipografía** (ambas OFL, Google Fonts, verificadas 2026-09-23: CSS API HTTP 200 y `ofl/<familia>/OFL.txt` HTTP 200 en `github.com/google/fonts`; se empaquetan en `res/font` para funcionar sin red):
- **Big Shoulders Display** (Patric King, diseñada para la señalética de Chicago): horas y salones. Pesos 800/900. Cifras tabulares.
- **Atkinson Hyperlegible Next** (Braille Institute): todo lo demás. Diseñada para baja visión: distingue `1/l/I`, `0/O`, `B/8`, justo lo que se confunde al sol en «B-204».
- Escala (sp): 96 hora · 40 salón · 28 actividad · 20 sección · 16 cuerpo · 14 meta. Ratio 6×.

**Paleta**

| Token | Claro | Oscuro | Uso único |
|---|---|---|---|
| `papel` (fondo) | `#F2EFE6` | `#0E0F0C` | superficie |
| `tinta` (texto) | `#14130F` | `#F2EFE6` | texto principal |
| `tinta-2` | `#5A574E` | `#A29E92` | meta, horas pasadas |
| `filete` | `#14130F` @ 18 % | `#F2EFE6` @ 18 % | separar filas |
| `señal` (salida) | `#FFB000` campo, texto `#14130F` | `#FFB547` campo, texto `#0E0F0C` | **solo** la banda de salida |
| `tarde` | `#8E2A17` campo, texto `#F3EEE2` | `#FF8A6B` texto sobre `#0E0F0C` | solo «vas tarde» |
| `clase` | `#1F4FB8` | `#8FB0FF` | marcador de tipo |
| `trabajo` | `#1E6B45` | `#6FCB98` | marcador de tipo |
| `puntual` | `tinta` contorno | `tinta` contorno | marcador de tipo |

**Retícula y densidad**: 4 columnas en 360 dp, margen 20, medianil 12. Columna izquierda fija de 64 dp para horas (asimetría: el peso cae a la izquierda). Espaciado base 4: 4·8·12·20·32·48. Densidad media: 5 a 7 filas visibles bajo la banda.

**Repertorio (cerrado)**
- Banda ámbar a sangre: **solo** dice «cuándo moverte». Aparece una vez por pantalla, nunca en listas.
- Filete de 1,5 dp: separa filas del tablero. Nada más separa (sin tarjetas, sin sombras).
- Cuadrado de 10 dp relleno/contorno: tipo de actividad. No es botón ni estado.
- Hilo vertical de la columna de horas: progreso del día (relleno hasta ahora).
- Texto invertido (papel sobre tinta): el día de hoy en la tira de semana.

**Riesgos que gasta**: (1) a sangre por tres bordes (banda ámbar: izquierda, derecha, arriba bajo la barra de estado); (3) contraste de escala 6×; (8) asimetría estructural (columna de horas); (9) color como campo (la banda).

**Movimiento**: la banda muta de estado (espera → prepárate → sal ya → tarde) con cruce de `opacity` 200 ms y desplazamiento de cifras tipo tablero (`translationY` de −8 dp a 0, 180 ms, por dígito que cambia, no por los que siguen igual). Reducido: cambio instantáneo.

**Boceto Hoy** (360 dp)

```
┌────────────────────────────────────┐
│ 06:51          ▲▼ ◧ 83%            │ barra de estado
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│ ← banda ámbar a sangre
│▓ SAL A LAS                        ▓│
│▓ ███▀ ▀██  ██▀██ ▀▀██             ▓│
│▓   █▀  ▀▀  ▄▄▀██  ▄█▀   7:32      ▓│  96 sp Big Shoulders
│▓  █▀   ██  ▀▀▀▀  ██▄▄             ▓│
│▓                                  ▓│
│▓ ■ Cálculo diferencial            ▓│  28 sp
│▓   B-204        Bloque B · Campus ▓│  40 sp / 16 sp
│▓ ─────────────────────────────────▓│
│▓ en bus 23 min + 7 margen · hace 3▓│  14 sp · toca para cambiar modo
│▓ [ Voy saliendo ]      [ +5 min ] ▓│  48 dp
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│ MIÉ                                │
│ 23   ● ahora 06:51 ────────────────│ línea de ahora
│  ┃  8:00 → 10:00  ■ Cálculo  B-204 │
│  ┃ ────────────────────────────────│
│  ┆ 10:30 → 12:00  ■ Física   L-3   │
│  ┆ ────────────────────────────────│
│  ┆ 14:00 → 20:00  ■ Turno    Tienda│
│  ┆           ⚠ 25 min de traslado, │
│  ┆             necesitas 35        │ conflicto inline
│  ┆ ────────────────────────────────│
│  ┆ 21:00          □ Entrega informe│
├────────────────────────────────────┤
│  Hoy      Semana     Actividades  ＋│ navegación inferior + crear
└────────────────────────────────────┘
```

**Boceto Semana** (horario universitario)

```
┌────────────────────────────────────┐
│ Semana 8 · 21–27 sep     ‹  ›      │
│  LUN  MAR ▐MIÉ▌ JUE  VIE  SÁB  DOM │ hoy invertido
│   21   22 ▐23 ▌  24   25   26   27 │
│────────────────────────────────────│
│ 7  ┊■B204┊    ┊■B204┊    ┊    ┊    │ celdas: código de salón,
│ 8  ┊Cálc ┊    ┊Cálc ┊    ┊    ┊    │ no el nombre largo
│ 9  ┊─────┊■L-3┊─────┊■L-3┊    ┊    │
│10  ┊     ┊Fís ┊     ┊Fís ┊    ┊    │
│11 ─┼─────┼────┼─────┼────┼────┼─── │ ← línea de ahora (solo en MIÉ)
│14  ┊■■■■■┊    ┊■■■■■┊    ┊■■■■┊    │ turnos: relleno sólido
│    ┊Tnda ┊    ┊Tnda ┊    ┊Tnda┊    │
│21  ┊     ┊    ┊ □Inf┊    ┊    ┊    │ puntual: contorno
│────────────────────────────────────│
│ Festivo lun 13 oct · sin clase     │ modo semestre
└────────────────────────────────────┘
```

### B · «Horario impreso a una tinta»

**Tesis**: la semana es una hoja de horario universitaria impresa en risografía verde sobre papel; lo fijo (clases, turnos) va impreso en tinta sólida y lo puntual va «escrito encima» a trazo.

**Referentes**: `ref/2026-09-23-pinterest-calendarios-editoriales.jpg` (rejilla a una tinta verde; numerales recortados), `ref/2026-09-23-refero-scheduling-editorial.png` (ink on cream, 1,5 px), `ref/2026-09-23-timepage-mes-monocromo.png` (todo tono sobre tono de un color).

**Tipografía** (OFL, verificadas igual que A):
- **Newsreader** (Production Type, eje óptico): el numeral del día (recortado, 200 sp) y el nombre de la actividad. Pesos 500/600.
- **IBM Plex Sans** + **IBM Plex Mono** (una superfamilia): Sans para UI y cuerpo; Mono para horas y códigos de salón, que así alinean en columna.
- Escala (sp): 200 numeral · 72 hora de salida (Plex Mono 600) · 28 actividad · 16 cuerpo · 14 meta.

**Paleta**

| Token | Claro | Oscuro | Uso único |
|---|---|---|---|
| `papel` | `#F3EEE2` | `#0F1F19` | superficie |
| `tinta` | `#0F4A38` | `#E9E4D6` | todo el texto y lo fijo |
| `tinta-60` | `#3F6155` | `#86D4AE` | meta y lo puntual |
| `corrector` | `#8E2A17` campo, texto `#F3EEE2` | `#FF8A6B` | **solo** el sello de salida |
| tipos | Clase = tinta sólida · Trabajo = trama de líneas 45° · Puntual = contorno | igual | el tipo se distingue por **trama**, no por tono |

Nota: los «colores por tipo» se resuelven con tramas de la misma tinta; si Diego exige tres tonos, esta dirección pierde su tesis.

**Retícula y densidad**: 7 columnas en Semana (la hoja manda), margen 16. Hoy: el numeral ocupa el tercio superior y se corta por el borde derecho. Grano riso (ruido estático 3 % de opacidad) **solo sobre lo impreso fijo**: función, distinguir lo recurrente de lo puntual.

**Repertorio (cerrado)**
- Numeral del día recortado: **solo** dice qué día es. Uno por pantalla.
- Sello rojo girado −2°: **solo** la hora de salida. Encima de la primera fila (superposición).
- Trama: tipo de actividad.
- Filete 1,5 dp de tinta: separar filas.
- Grano: marca «impreso = recurrente».

**Riesgos que gasta**: (2) superposición (sello sobre la lista); (5) tipografía recortada (numeral); (7) grano con función; (10) rotación con motivo (sello).

**Movimiento**: el sello «se estampa»: `scale` 1,06 → 1 y `opacity` 0 → 1 en 160 ms, una sola vez por cambio de estado. Nada más se mueve. Reducido: aparece sin escala.

**Riesgo propio**: el serif grande y la rotación cuestan legibilidad al sol; el grano añade ruido en pantallas OLED con brillo alto. Es la dirección más distintiva y la más frágil en la escena de uso.

**Boceto Hoy**

```
┌────────────────────────────────────┐
│ 06:51                     ◧ 83%    │
│ miércoles                        ██│
│ septiembre                    ██▀▀ │ numeral 23 recortado
│                             ▄██▀   │ por el borde derecho
│                            ██▄▄▄▄▄ │
│   ╔═══════════════════════╗        │
│   ║ SAL 7:32              ║ ↻ -2°  │ sello rojo corrector
│   ║ Cálculo · B-204       ║        │ encima de la primera fila
│ ──╚═══════════════════════╝─────── │
│ 08:00 ▓ Cálculo diferencial        │ tinta sólida + grano
│       B-204 · Bloque B · bus 23'   │
│ ────────────────────────────────── │
│ 10:30 ▓ Física mecánica      L-3   │
│ ────────────────────────────────── │
│ 14:00 ▨ Turno · Tienda centro      │ trama = trabajo
│       ⚠ traslado 25' < 35' necesarios│
│ ────────────────────────────────── │
│ 21:00 ☐ Entrega informe (a trazo)  │ contorno = puntual
├────────────────────────────────────┤
│  Hoy      Semana     Actividades  ＋│
└────────────────────────────────────┘
```

**Boceto Semana**

```
┌────────────────────────────────────┐
│ SEMANA 8          21 → 27 SEP 2026 │
│ ┌────┬────┬════┬────┬────┬────┬───┐│
│ │L 21│M 22║X 23║J 24│V 25│S 26│D27││ hoy con doble filete
│ ├────┼────┼════┼────┼────┼────┼───┤│
│07│▓▓▓▓│    ║▓▓▓▓║    │    │    │   ││
│  │B204│    ║B204║    │    │    │   ││
│09│    │▓▓▓▓║    ║▓▓▓▓│    │    │   ││
│  │    │L-3 ║    ║L-3 │    │    │   ││
│11├────┼────╫────╫────┼────┼────┼───┤│ ← ahora
│14│▨▨▨▨│    ║▨▨▨▨║    │▨▨▨▨│    │   ││
│21│    │    ║ ☐  ║    │    │    │   ││
│ └────┴────┴════┴────┴────┴────┴───┘│
│ lun 13 oct festivo · sin clase     │
└────────────────────────────────────┘
```

### C · «Campo de estado»

**Tesis**: la pantalla Hoy entera es del color de tu margen: hueso cuando vas sobrado, ámbar cuando toca prepararse, ascua cuando hay que salir, rojo cuando vas tarde; lo lees con el rabillo del ojo antes de leer una cifra.

**Referentes**: `ref/2026-09-23-timepage-hoy-campo-color.png` (móvil entero teñido), `ref/2026-09-23-refero-cron-ember.png` (una sola chispa naranja racionada), `ref/2026-09-23-emil-great-animations.png` (un objeto que muta de estado).

**Tipografía** (OFL, verificadas igual que A):
- **Schibsted Grotesk** (diseñada para el grupo de prensa Schibsted): hora gigante (800), actividad, UI.
- **Spline Sans Mono**: horas de la lista y códigos de salón.
- Escala (sp): 112 hora · 32 actividad · 40 salón · 16 cuerpo · 14 meta. Ratio 7×.

**Paleta**

| Token | Claro | Oscuro | Uso único |
|---|---|---|---|
| `hueso` (campo tranquilo) | `#EDE8DD` | `#0F0D0A` | vas sobrado |
| `ámbar` (campo) | `#FFC53D` + texto `#16130F` | borde/cifra `#FFC53D` sobre `#0F0D0A` | faltan ≤ 15 min para salir |
| `ascua` (campo) | `#FF7A3D` + texto `#16130F` | `#FF7F45` + texto `#0F0D0A` | sal ya |
| `tarde` (campo) | `#A3121A` + texto `#FFF6EE` | `#A3121A` + texto `#FFF6EE` | vas tarde |
| `tinta` | `#16130F` | `#EDE8DD` | texto |
| `tinta-2` | `#5C564C` | `#9C958A` | meta |
| tipos | Clase/Trabajo/Puntual solo como **letra** C · T · P en caja de filete | igual | el tono queda reservado al estado |

**Retícula y densidad**: Hoy a pantalla completa en dos zonas: campo superior (60 % de alto) y lista inferior sobre `hueso`. Baja densidad: 3 filas visibles. Semana neutra, sin campo.

**Repertorio (cerrado)**
- Campo de color a sangre por cuatro bordes: **solo** el estado de margen.
- Cifra gigante: la hora de salida (o la cuenta atrás en «sal ya»).
- Letra en caja de filete: tipo de actividad.
- Filete: separar filas.

**Riesgos que gasta**: (1) a sangre por cuatro bordes; (3) contraste de escala 7×; (9) color como campo; (6) texto sobre campo como composición principal.

**Movimiento**: el campo cambia de color con cruce de `opacity` entre dos capas (240 ms); la cifra no se mueve. Reducido: corte instantáneo. El campo **nunca parpadea** ni pulsa.

**Riesgo propio**: el estado se codifica en tono; hay que duplicarlo siempre en texto («Sal ya») para daltonismo. Choca con «colores por tipo»: el tipo pierde el color. En la notificación (Live Update no admite `setColorized`) el campo desaparece y la dirección pierde su firma justo donde más se usa.

**Boceto Hoy**

```
┌────────────────────────────────────┐
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│ campo ascua a sangre (4 bordes)
│░ SAL YA                           ░│
│░                                  ░│
│░  ██  ██▀▀█  █▀▀█                 ░│  112 sp «7:32»
│░  ██    ▄▀   ▀▀▄▄                 ░│
│░  ██  █▄▄▄  █▄▄█                  ░│
│░                                  ░│
│░ Cálculo diferencial         [C]  ░│
│░ B-204                            ░│  40 sp
│░ Bloque B · bus 23 min + 7 margen ░│
│░ [ Voy saliendo ]      [ +5 min ] ░│
│░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│
│ 10:30  Física mecánica   L-3   [C] │ lista sobre hueso
│ ────────────────────────────────── │
│ 14:00  Turno · Tienda          [T] │
│ ────────────────────────────────── │
│ 21:00  Entrega informe         [P] │
├────────────────────────────────────┤
│  Hoy      Semana     Actividades  ＋│
└────────────────────────────────────┘
```

**Boceto Semana** (neutra; el color es solo de Hoy)

```
┌────────────────────────────────────┐
│ 21 – 27 sep                 ‹  ›   │
│ L    M    X•   J    V    S    D    │ hoy con punto
│────────────────────────────────────│
│ 07 C B204      C B204              │ lista por franjas,
│ 09      C L-3       C L-3          │ letra de tipo + salón
│ 14 T Tnda      T Tnda    T Tnda    │
│ 21             P Inf               │
│────────────────────────────────────│
│ ⚠ Jue: 25 min entre Física y Turno │ conflictos al pie
└────────────────────────────────────┘
```

### Recomendación: **A · Tablero de salidas**

1. **Sirve a la restricción dominante mejor que ninguna**: la cifra más grande del sistema es la que Diego necesita, en una condensada de señalética pensada para leerse lejos y con prisa, y el cuerpo en una familia diseñada para baja visión, que en exterior con sol es la condición de todos.
2. **Es la única que no se contradice con lo pedido**: conserva colores por tipo (B los convierte en tramas; C se los quita para dárselos al estado).
3. **Sobrevive fuera de la app**: su firma es de jerarquía y redacción («SAL A LAS 7:32» / «B-204»), que se trasladan tal cual a notificación, chip de 96 dp y widget, donde Android no deja colorear. C pierde su firma justo ahí.
4. **Gasta cuatro riesgos sin tocar la legibilidad**. B gasta cuatro también, pero tres de ellos (serif grande, rotación, grano) cobran legibilidad al sol.

Si Diego quiere más carácter del que da A, la variante honesta es tomar de C **solo** el cambio de color de la banda por estado (ámbar → ascua → rojo) dentro de la banda de A. Eso sería una enmienda y se decide explícitamente, no se mezcla por defecto.

---

## 7. Arquitectura de información

### Navegación

Barra inferior de 3 destinos (M3 `NavigationBar`, alcanzable con el pulgar) + acción de crear:

```
Hoy  ·  Semana  ·  Actividades                       [＋ Crear]
 │        │            │
 │        │            ├─ Clases (por materia)  ─ Materia ─ Tareas / Entregas / Exámenes (futuro)
 │        │            ├─ Trabajo (turnos)
 │        │            ├─ Puntuales
 │        │            └─ Lugares guardados (Casa, Campus, Tienda…) ─ Lugar ─ salones usados
 │        └─ Semana tipo horario ─ toque en celda ─ Detalle de actividad
 └─ Ahora/Siguiente + lista del día ─ Detalle de actividad
Ajustes (icono en barra superior de Hoy): Semestre (inicio/fin, festivos, semanas sin clase) ·
  Avisos (margen por defecto, modo de transporte, resumen matutino, silenciar en clase) ·
  Permisos · Copia de seguridad cifrada · Apariencia (claro/oscuro/sistema)
Superficies de sistema: notificaciones (3 canales), Live Update, widget «Siguiente», tile de ajustes rápidos (opcional)
```

«Hoy» es el inicio. «Ahora / Siguiente» no es una pestaña aparte: es la banda superior de Hoy.

### Modelo mental (lo que ve Diego, no el esquema de datos)

- **Actividad**: algo a lo que vas. Tipo (Clase / Trabajo / Puntual), nombre, cuándo, **dónde** (Lugar + Salón).
- **Lugar**: un sitio del mapa que reutilizas (Campus, Tienda). **Salón**: el texto concreto de esa actividad (B-204, piso 3, oficina 12). Un lugar tiene muchos salones; el salón no se busca en mapa.
- **Semestre**: el marco de fechas de las clases; los turnos pueden tener su propio rango.

### Flujo 1 · Crear actividad recurrente

1. `＋` → hoja inferior: **¿Qué es?** Clase · Trabajo · Puntual (tres botones grandes; la elección cambia los valores por defecto: Clase hereda fechas del semestre).
2. **Nombre** («Cálculo diferencial»). Sugerencias de materias ya creadas.
3. **Cuándo**: chips de días `L M X J V S D` + hora inicio/fin (selector de reloj M3 con teclado disponible). Se permite distinto horario por día («L 7–9, X 8–10»).
4. **Dónde**: lista de Lugares guardados arriba; «Otro lugar» abre el mapa (Flujo 2). Campo **Salón** libre, con los salones usados en ese lugar como sugerencia.
5. **Durante**: «Todo el semestre (3 ago → 28 nov)» por defecto; editable.
6. **Resumen en frase** fijo al pie (ref. Todoist en Mobbin): «Cada lunes y miércoles, 7:00 → 9:00, B-204 · Campus, del 3 ago al 28 nov».
7. **Comprobación inline antes de guardar**: solape con otra actividad o traslado insuficiente («Entre Física (L-3) y Turno hay 25 min; en bus necesitas 35»). No bloquea; ofrece «Guardar igual».
8. Guardar → vuelve a donde estaba, con confirmación en snackbar y «Deshacer».

Primera actividad creada = momento de pedir notificaciones (Flujo 3).

### Flujo 2 · Elegir lugar en mapa

1. Buscador arriba (dirección o nombre) + mapa; alfiler fijo en el centro, el mapa se mueve debajo (preciso con una mano).
2. «Usar mi ubicación» solo si ya hay permiso; si no, el botón explica y lleva al Flujo 3.
3. Nombrar el lugar («Campus», «Tienda centro») → se guarda y se reutiliza.
4. Sin red: se puede escribir la dirección y guardar sin coordenadas; el lugar queda marcado «sin ubicar» y la alerta de salida usa margen fijo hasta ubicarlo.

(Proveedor de mapa y de rutas: decisión técnica pendiente; criterio de producto: funcionar con dato viejo sin mentir y no enviar la ubicación de Diego a más servicios de los necesarios.)

### Flujo 3 · Permisos explicados antes de pedirlos

Cada permiso se pide **en el momento en que hace falta**, con una pantalla previa que muestra el resultado concreto que habilita, y siempre con alternativa si dice que no.

| Permiso | Cuándo se pide | Pantalla previa (copia) | Si lo niega |
|---|---|---|---|
| Notificaciones (`POST_NOTIFICATIONS`) | Al guardar la primera actividad | «Para avisarte "Empieza Cálculo en B-204" necesito enviarte notificaciones.» + ejemplo dibujado | Hoy muestra un aviso fijo «Sin avisos: no te llegará nada» con botón «Activar» |
| Alarmas exactas (`USE_EXACT_ALARM`, válido para apps de calendario) | Implícito; se verifica | Ninguna si se concede por manifiesto; si no, «Sin esto, el aviso puede llegar hasta 10 min tarde» | Avisos inexactos + etiqueta «aprox.» en la hora |
| Ubicación mientras se usa | Al activar la primera alerta de salida | «Para decirte "sal a las 7:32" necesito saber desde dónde sales. Solo se usa para calcular el trayecto.» | Alerta con margen fijo desde «Casa» (lugar elegido) y texto «desde Casa, no desde tu ubicación» |
| Ubicación en segundo plano | Solo si activa «calcular desde donde esté» | Explicación + paso a Ajustes del sistema (Android lo exige así) | Se calcula al abrir la app o desde el último lugar conocido, con antigüedad visible |
| No molestar (`ACCESS_NOTIFICATION_POLICY`) | Al activar «silenciar en clase» | «Silencio el móvil al empezar cada clase y lo devuelvo al terminar.» | La función queda apagada, sin insistir |
| Notificaciones promovidas (Live Update) | Al tocar «Voy saliendo» la primera vez | «Te dejo el trayecto fijo en la pantalla de bloqueo hasta que llegues.» | Notificación en curso normal |

### Notificaciones (canales)

| Canal | Importancia | Título / texto | Acciones |
|---|---|---|---|
| Salida | Alta (heads‑up) | «Sal a las 7:32 · Cálculo» / «B-204 · 23 min en bus + 7 de margen» | Voy saliendo · +5 min · Hoy no voy |
| Sal ya / Vas tarde | Alta | «Sal ya · Cálculo empieza 8:00» / «Vas 4 min tarde · llegas 8:04 a B-204» | Voy saliendo · Avisar que llego tarde (futuro) |
| Empieza | Normal | «Empieza Cálculo · B-204» | Silenciar 1 h |
| Resumen matutino | Baja | «Hoy: 3 cosas · primera salida 7:32» | Ver día |
| Live Update (tras «Voy saliendo») | Promovida | «Llegas 7:58 · B-204» + barra `ProgressStyle` (salida → llegada, punto de margen) · chip `12min` | Ya llegué · Quitar |

Nada coloreado en notificaciones (Android no lo permite en Live Update); la identidad vive en la redacción: primero el tiempo, luego el qué, luego el dónde.

### Widget «Siguiente» (2×2 y 4×2)

2×2: hora de salida (o de inicio si no hay traslado) + salón. 4×2: añade actividad, lugar y la siguiente fila. Dato viejo: «hace 40 min» en `tinta-2`. Sin permiso de ubicación: «desde Casa».

### Estados de Hoy (todos con vista propia)

| Estado | Qué se ve |
|---|---|
| Cargando | Banda con la última hora conocida en `tinta-2` y «Recalculando trayecto…»; lista con esqueleto de filas (sin spinner central) |
| Vacío (primer uso) | «Empieza por tu horario de clases» + botón «Añadir clase» + «Añadir turno»; enseña el modelo Lugar/Salón con un ejemplo |
| Vacío (día sin nada) | «Hoy no tienes nada fijo.» + «Siguiente: lunes 7:00 · Cálculo · B-204» |
| Semana sin clase / festivo | «Festivo: sin clases hoy» + turnos del día si los hay |
| Error de ruta | «No pude calcular el trayecto. Te aviso 30 min antes (margen fijo).» + «Reintentar» |
| Sin permiso | Ubicación: la banda dice «desde Casa»; notificaciones: aviso fijo arriba con «Activar» |
| Dato viejo | «estimado hace 25 min» junto al trayecto; pasado el umbral (15 min) la hora de salida se marca «aprox.» |
| Conflicto | Inline bajo la fila afectada (no modal): «25 min de traslado, necesitas 35» |
| En camino (Live Update activo) | Banda: «Llegas 7:58 · B-204», hilo de progreso del trayecto |

---

## 8. Verificación de esta fase

- **Contraste** medido con script (fórmula WCAG 2.x de luminancia relativa) sobre los tokens de las tres direcciones, en claro y oscuro. Resultados clave:
  - A: tinta/papel 16,17 · tinta/ámbar 10,15 · papel oscuro/tablero 16,72 · ámbar/tablero 10,94 · clase/papel 6,37 · trabajo/papel 5,63 · papel/tarde `#8E2A17` 7,27.
  - B: tinta/papel 8,82 · `tinta-60 #3F6155`/papel 5,94 · papel/corrector `#8E2A17` 7,27 · oscuro texto/fondo 13,44.
  - C: tinta/ámbar 11,73 · tinta/ascua `#FF7A3D` 7,14 · hueso/tarde 7,40 · oscuro fondo/ascua `#FF7F45` 7,74.
  - **Fallos propios encontrados y corregidos** en la primera pasada: rojo `#B8321C` con papel daba 5,20 (no AAA para cifra) → `#8E2A17`; ascua `#FF5A1F` con tinta daba 5,94 → `#FF7A3D`; `tinta-60 #4D6E62` en B daba 4,87 → `#3F6155`.
  - Pendiente: el contraste se midió sobre tokens, **no sobre render**. Al implementar se muestrea sobre capturas reales del dispositivo.
- **Tipografías**: comprobada existencia (Google Fonts CSS API → HTTP 200) y licencia OFL (`google/fonts/ofl/<familia>/OFL.txt` → HTTP 200) para Big Shoulders Display, Atkinson Hyperlegible Next, Newsreader, IBM Plex Sans/Mono, Schibsted Grotesk, Spline Sans Mono. Ninguna es fuente del sistema ni de la lista de inercia.
- **Medida de copia**: §5 (estimación por ancho medio de carácter; se confirma con render al implementar).
- **No aplica aún**: capturas en dispositivo, prueba comparativa, recorrido por teclado/TalkBack. Se harán por paso cuando haya contrato.

### Verificación paso 2 · Tokens (2026-09-23)

- Emulador Agenda_Pixel, 1080×2400 px a 420 dpi (411 dp de ancho, retícula holgada: margen 24). Capturas: `docs/capturas/paso2-tokens-claro.png` y `docs/capturas/paso2-tokens-oscuro.png` (5 fotogramas de desplazamiento cada una, lado a lado).
- Protocolo de captura: volcado de uiautomator, captura y segundo volcado; el fotograma solo se acepta si los dos volcados son idénticos, pertenecen a `com.dpinta.agenda` y el modo nocturno es el pedido. Motivo: la app de gastos comparte el emulador y en el primer intento pasó a primer plano durante la captura oscura; esas mediciones se descartaron.
- **Contraste medido sobre el render** (color real del trazo frente al fondo, 147 textos): 0 fallos. Mínimo en claro 6,28 (`tinta-2` sobre `papel`, AA); mínimo en oscuro 7,18. Las 18 cifras (hora, salón, rótulos, rangos) dan AAA: 10,15 y 10,94 sobre ámbar, 7,27 sobre `tarde`, 16,17 y 16,72 sobre papel.
- **Cifras tabulares medidas en el render**: `10:48`, `11:11` y `12:58` ocupan exactamente 203 px cada una.
- Pendiente: retícula compacta a 360 dp, `fontScale` 2,0 y los iconos de C7 (Material Symbols Sharp), que no hicieron falta en el espécimen.

### Verificación paso 3 · Banda de salida (2026-09-23)

Código: `ui/components/banda/` (modelo inmutable y mapeo puro en `BandaSalidaModelo.kt`; composable sin estado en `BandaSalida.kt`), `ui/components/Foco.kt`, `ui/theme/Motion.kt` (C8) e `ui/theme/Iconos.kt` (C7: Material Symbols Sharp recortado a los 15 glifos de C7.3, estático wght 500, 3 KB).

Decisiones de interpretación del contrato (sin cambiar cláusulas):
- **Vas tarde**: la cifra grande es la hora de INICIO (8:00) y la línea dice «llegas 8:04 a B-204». Así no se repite la misma hora dos veces.
- **Sal ya**: la cifra sigue siendo la hora de salida; la línea dice «Cálculo diferencial empieza 8:00».
- **En camino**: única acción «Ya llegué». **Vas tarde**: única acción «Voy saliendo». **Sin traslado**: sin acciones.
- **C1.2 frente a C9.4**: la posición visual (secundaria izquierda, primaria derecha, modo arriba) y el orden de foco (primaria, secundaria, modo) no coinciden. Se componen y colocan en orden de foco y se dibujan en posición visual; la banda es un grupo de foco que entra siempre por la primaria. TalkBack usa `traversalIndex`.

Verificación medida:
- Build, lint (solo `OldTargetApi` y `MissingApplicationIcon`, previos) y 15 pruebas unitarias del mapeo estado → texto, acciones y colores.
- 7 pruebas instrumentadas en el emulador: mismo ancho de hora con tnum; primaria a la derecha de la secundaria; toques ≥ 48 dp; frase de TalkBack literal; orden de foco con teclado; tablero que anima solo el dígito que cambia y termina antes de 290 ms; fundido de estado con dos capas a los 100 ms y una a los 350 ms; corte instantáneo con movimiento reducido en ambos casos.
- 34 capturas (7 estados en claro y oscuro a 411 dp; 24 h; 360 dp; escala de fuente 2,0), cada una aceptada solo si dos volcados de uiautomator seguidos eran idénticos. Contraste de la banda medido sobre el render: 10,15 (claro) y 10,94 (oscuro) sobre ámbar; 7,27 sobre `tarde`; 16,17 y 16,72 sin traslado. 0 fallos.
- Anillo de foco muestreado en los cuatro lados de cada control, en la tinta de la banda.
- Fallos propios encontrados y corregidos: orden de foco (primero seguía el orden visual, después se saltaba el modo); el selector de demostración no tenía anillo de foco; una prueba de movimiento mal escrita (dos `setContent`).
- Pendiente: recorrido real con TalkBack encendido (solo se verificó la frase expuesta a accesibilidad); con escala de fuente 2,0 el selector de demostración (solo en depuración) parte palabras.

### Paso 4 · Esqueleto de navegación (2026-09-23), verificación en dispositivo PENDIENTE

Hecho: barra inferior Hoy · Semana · Actividades + Crear de 56 dp (C1.2, C5.3) con rutas tipadas y sin transiciones (C8.3); Ajustes desde el icono de la cabecera de Hoy; demostración de la banda solo en depuración (Ajustes). Hoy = banda + cabecera del día + lista con hilo del día y línea de «ahora» (C6) + estados cargando, primer uso y día sin nada (C10). Semana y Actividades: esqueleto con el estado vacío de C10. `HoyViewModel` (Hilt, `Clock` inyectable, pulso por minuto) sobre un repositorio en memoria; tiempos solo del dominio (se añadieron `DayPlanner` y `Trip` a core/domain con pruebas). La banda ya no calcula tiempos: recibe llegada y minutos del dominio.

Verificado sin dispositivo: `assembleDebug`, `lintDebug` (solo los dos avisos previos), 45 pruebas unitarias en verde (app 30, dominio 15) y `assembleDebugAndroidTest` compila (4 pruebas de navegación nuevas, sin ejecutar).

Contradicciones que Diego tiene que decidir (no se cambió nada por cuenta propia):
1. ~~Dato viejo~~ Resuelto 2026-09-23: 15 min; corregido en el dominio (ver Registro de enmiendas).
2. ~~Horas pasadas~~ Resuelto 2026-09-23: `tinta` + marca «✓» (ver Registro de enmiendas).
3. ~~Texto de solape~~ Resuelto 2026-09-23: «Se cruza con {actividad} {hora}» (ver Registro de enmiendas).
4. Semana y Actividades muestran «Empieza por tu horario de clases» mientras Hoy tiene datos de ejemplo: es el esqueleto pedido; se resuelve al conectarlas al repositorio.

### Paso 5 · Crear / editar actividad (2026-09-24), verificación en dispositivo PENDIENTE

Hecho, en el orden del flujo 1 (§7): qué es (clase · trabajo · puntual · examen · otro), nombre; cuándo (cada semana con días + horas + desde/hasta, o una vez con fecha + horas); dónde (lugar y salón por separado; «Elegir en el mapa» abre el esqueleto «Mapa pendiente», sin clave de Google); cómo llegas (bus · a pie · carro · moto), margen y aviso; conflictos en la misma pantalla con `ConflictDetector` («lunes: Se cruza con Física mecánica 10:30», «lunes, con Cálculo diferencial: 15 min de traslado, necesitas 35»), que avisan y no bloquean («Guardar igual»); frase-resumen al pie; validación con mensajes literales tras el primer intento de guardar; estados cargando, no encontrada, guardando, error al guardar (conserva los datos) y guardado (snackbar «Guardado: …» con «Deshacer» solo para las nuevas). Tocar una fila de Hoy abre la edición. Guardar a la derecha y cancelar a la izquierda, fijos abajo (C1.2). Todo se guarda en el repositorio en memoria.

Decisiones tomadas en el paso (a confirmar por Diego):
- Formulario a pantalla completa, no hoja inferior: con teclado y a una mano es más estable; el anexo decía «hoja inferior».
- Horas y fechas como texto, sin reloj ni calendario modal (C11 prohíbe modales). Se acepta «8», «830», «8:00», «8 p. m.», «24/9», «24/9/2026».
- Valores por defecto: margen 10 min, aviso 15 min; sin semestre definido, la serie dura 17 semanas desde hoy.
- Un lugar escrito que coincide con uno guardado (sin distinguir mayúsculas) se reutiliza; uno nuevo no tiene tiempos de traslado y no se afirma ningún conflicto de traslado con él.
- «Deshacer» solo en actividades nuevas: una edición no guarda la versión anterior.

**Enmienda a C6 (repertorio cerrado), aprobada por Diego el 2026-09-24**: el formulario usa cuatro formas que el contrato no tenía: (1) campo de texto: rectángulo con borde de 1,5 dp en `tinta-2`, `tinta` con foco y `alerta-texto` con error; etiqueta encima y error debajo con icono; (2) selector segmentado: opciones con filete de 1,5 dp y la elegida invertida; (3) casillas de días L M X J V S D con la misma construcción (a 360 dp miden ~46 dp de ancho y su área táctil se amplía a 48); (4) alfiler del mapa: filete vertical + cuadrado de 10 dp. Si Diego no las aprueba, se cambian antes de seguir.

Verificado sin dispositivo: build, lint (solo los dos avisos previos), 73 pruebas unitarias en verde y `assembleDebugAndroidTest` compila (flujo de creación, mapa pendiente y etiqueta de la barra con fuente 2,0, sin ejecutar).

## 9. Descartes

- **Fantastical** (`ref/2026-09-23-fantastical-home.png`): bloques 3D brillantes con degradado y difuminado; es exactamente la estética de fábrica que se prohíbe.
- **Amie** (`https://amie.so`): ha girado a notas de reunión con IA; la portada ya no muestra calendario. Sin captura conservada.
- **Familia «blueprint & hairlines»** de refero: estética de herramienta de desarrollador (cuadrícula técnica azul); comunica «infraestructura», no «tu día».
- **Material You `dynamicColor`**: haría que la app tomara el color del fondo de pantalla; se pierde la identidad y el contraste medido. Se desactiva.
- **Mes como vista principal**: no responde a ninguna de las tres tareas; queda como selector dentro de Semana.
- **Tarjetas con filete lateral de color** (Timepage, Notion): prohibidas por el sistema; el tipo se marca con cuadrado, trama o letra.
- **motionsites.ai**: movimiento de landing (fondos animados, titulares brillantes) sin aplicación en producto de lectura obligada.

## 10. Preguntas abiertas para Diego (respondidas el 2026-09-23; ver C1 del contrato)

1. **¿Reloj de 12 h o 24 h?** Con 12 h, «a. m./p. m.» no cabe al lado de la cifra a 96 sp en 360 dp; se resuelve con sufijo pequeño, pero conviene saberlo antes del contrato.
2. **¿Cómo te mueves normalmente?** (bus, a pie, moto, bici). Define el modo por defecto del cálculo de salida.
3. **¿Mano dominante?** Decide si el botón de crear y las acciones de la banda van a la derecha o a la izquierda.
4. **Dirección**: A (recomendada), B o C.
