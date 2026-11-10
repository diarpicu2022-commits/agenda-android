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

## 3. Decisiones para Diego

1. ¿A, B o C? (A está hecha y se puede ver en la rama.)
2. En A, ¿la figura camina por dentro del riel (como está) o encima del riel?
3. En A, con 50 a 55 minutos la figura pasa cerca de «Empieza en». No se tocan, pero queda justo. Si molesta, la
   figura se puede achicar o mover hacia el borde.
4. ¿El destino cambia según el tipo? Hoy es el campus para todo. Se puede hacer una tienda para Trabajo y una casa
   para Personal.

## 4. Fallo propio

La primera versión del destino era una casa. Para una clase se leía como «a casa». Se cambió por la fachada del campus.
