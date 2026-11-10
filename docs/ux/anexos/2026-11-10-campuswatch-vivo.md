# Anexo · CampusWatch con más vida — 2026-11-10

Pedido de Diego (noche del 2026-11-09): «veo muy simple el diseño del smartwatch de Agenda, ¿no se le puede añadir
figuras o personajes, algo que le dé más vida?, como me conoces mi gusto de diseño». Lo hice en la rama
`feature/campuswatch-vivo`, aparte, para que elija con la pieza en la mano. Todo lo de este anexo está **[por confirmar]**.

## 1. Lo que ya sé de su gusto

- En SISCAN eligió la **escena ilustrada del secador**: cielo según la hora, paisaje de Nariño y la cifra encima
  (`siscan-movil`, anexo 2026-10-12, «escena del secador y tema por hora»).
- En NotaScan, la **hoja de examen dibujada** del login y el escudo del colegio: ilustración que cuenta qué hace la app.
- Rechaza el diseño de fábrica y el simplón. Quiere un momento memorable con sentido, no adorno suelto.

## 2. Direcciones

### A · «El camino a clase» (recomendada, implementada)

El anillo de cuenta ya era «la línea del día enrollada». Ahora se lee como un camino:

- **El destino** a las 12: la fachada del campus (frontón, columnas, escalinata) en el color de la urgencia.
- **Una figura que camina** por dentro del riel, en la punta de la cuenta. Avanza un paso por minuto y llega al
  edificio cuando la cuenta se vacía. Es tinta sobre el fondo, como marca (14:1 en claro, 19:1 en oscuro).
- **El Galeras al fondo**, una silueta con su cráter en `superficieFuerte`, apenas distinta del fondo. Es el paisaje de
  Pasto, como el paisaje de la escena de SISCAN.
- Sin animación continua (batería del reloj). Nada de esto aparece en siempre activo ni en la Tarjeta.

Medido en el emulador, en Próxima y Es hora de salir, claro y oscuro. Sin fallos de contraste, toques ni círculo
(`docs/capturas/cw-medida-vivo-*`).

### B · «Un compañero» (no implementada)

Un personaje fijo (por ejemplo, un búho estudiante) que cambia de pose según la urgencia: lee tranquilo en Calma, se
pone la mochila en Pronto, corre en Salir ahora y Urgente. Más simpático, pero el personaje compite con la cifra y repite
lo que ya dice el color.

### C · «El cielo de la hora» (no implementada)

Como la escena de SISCAN: el fondo es un cielo que cambia con la hora (amanecer, día, tarde, noche) con el Galeras
abajo. Muy atmosférico, pero en claro el texto pierde contraste sobre los degradados del cielo y gasta más batería.

## 3. Decisiones de Diego (2026-11-10)

Eligió la dirección **A «El camino a clase»** con mis recomendaciones: la figura dentro del riel y el campus como destino
para todos los tipos por ahora. PR #3 mergeado.

## 3.1 Lo que se le preguntó

1. ¿A, B o C? (A está hecha y se puede ver en la rama.)
2. En A, ¿la figura camina por dentro del riel (como está) o encima del riel?
3. En A, con 50 a 55 minutos la figura pasa cerca de «Empieza en». No se tocan, pero queda justo. Si molesta, la
   figura se puede achicar o mover hacia el borde.
4. ¿El destino cambia según el tipo? Hoy es el campus para todo. Se puede hacer una tienda para Trabajo y una casa
   para Personal.

## 4. Fallo propio

La primera versión del destino era una casa. Para una clase se leía como «a casa». Se cambió por la fachada del campus.

## 5. Dirección A+ «Ruta y compañero» (decisión de Diego, 2026-11-10)

Diego vio la dirección A todavía muy sencilla y eligió A+, que es la ruta de A con más vida en la pantalla principal y un
compañero en las demás.

### Contrato

- **Pantalla principal.** El destino a las 12 cambia con el tipo: campus para clases, tienda para turnos, casa para lo
  personal y las entregas. El viajero cambia con el modo (persona con mochila, bus, carro o moto). A pie corre cuando
  la salida es urgente. Ciclo de cuatro fotogramas que se anima unos dos segundos al encender la pantalla y luego se
  queda quieto, por la batería. Nada se mueve en siempre activo ni con las animaciones del sistema apagadas.
- **Cielo.** Dos nubes y el sol de día o la luna de noche, en `superficieFuerte`, como el Galeras. Van en la franja de
  arriba, lejos de la cifra.
- **Compañero (paso 2).** Un cuy estudiante de Nariño en «¿Fuiste?», asistencia, confirmaciones y «nada por ahora». No
  aparece en la pantalla principal, para no competir con la cifra.
- **Familia de formas.** Formas llenas en tinta o en el color de la urgencia, trazo redondo de 2,6 dp, sin degradados.
- El modo viaja del teléfono al reloj en el formato `cw4` (el reloj sigue leyendo cw1 a cw3).

### Paso 1 · Ruta (hecho y aprobado 2026-11-10)

`wear/.../Sprites.kt` tiene los destinos, el viajero, las ruedas que giran y el cielo. Se probó en el emulador del reloj
con `CW_MODO` y `CW_TIPO` en `reloj_dia.py`.

Fallos propios encontrados en las capturas, antes de mostrarlo:

1. La moto salía deformada. Se redibujó de lado con el conductor sentado.
2. El sol quedaba pegado a la cifra. Se subió a la izquierda, junto al destino.
3. Dentro del riel, como estaba aprobado en A, el bus y el carro tocaban el título en «Es hora de salir», porque a esa
   hora el viajero cae a la altura del nombre. Encima del riel los cortaba el borde redondo de la pantalla. Quedó
   encima del riel, 5 dp hacia adentro y con un halo del fondo, y así no toca el texto ni se corta. **Esto cambia la
   respuesta de Diego a la pregunta 2 de la sección 3.1 y Diego lo aprobó el mismo día**
   (`VIAJERO_SOBRE_RIEL` en `Anillo.kt`).

### Paso 2 · El cuy estudiante (hecho 2026-11-10)

`wear/.../Cuy.kt` dibuja al compañero en cuatro poses y aparece en cuatro lugares:

| Pantalla | Pose |
|---|---|
| «¿Fuiste?» | Pregunta, junto al título, para que los dos botones conserven su espacio |
| Asistencia | Contento, o preocupado si alguna materia llegó al límite, junto al encabezado |
| Confirmación | Contento con la insignia de check, o preocupado con «!» si no llegó al teléfono |
| «Nada fijo por ahora» y «Sin horario» | Dormido |

El cuy es compañía, no información. TalkBack no lo lee, porque el texto de al lado ya dice lo mismo.

Fallo propio. La primera versión era de frente con la silueta en tinta y se leía como un oso o un hámster (en oscuro,
como un oso polar). Se redibujó de lado, con el cuerpo alargado en `aviso` (ámbar) y manchas del fondo, como el cuy
pinto de Nariño, y así se ve igual en claro y en oscuro. La gota de «preocupado» pasó a `ruta` para no confundirse con
el cuerpo.

Verificación con `medir_reloj.py` en «¿Fuiste?» (claro) y en asistencia (oscuro, inglés), sin fallos de contraste,
toques ni círculo. Los textos van de 6,30:1 («Sí, fui», blanco sobre `exito`) a 19,01:1.

