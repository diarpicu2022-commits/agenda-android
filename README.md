# Agenda + CampusWatch

Agenda es una app Android para quien estudia y trabaja a la vez. Junta el horario de la universidad y los turnos del trabajo, y responde la pregunta de cada día, que es dónde tengo que estar y a qué hora tengo que salir. CampusWatch es su app para el reloj (Wear OS) y muestra lo mismo en la muñeca.

Es el proyecto final de Diseño de Interfaces de Software (5.º semestre, Universidad Cooperativa de Colombia). La hice pensando en mi propia semana, entre clases, monitoría y trabajo.

## Para qué sirve

Cuando las clases y el trabajo quedan en lugares distintos, el problema no es acordarse de la clase sino salir a tiempo. Agenda calcula la hora de salida con el tiempo de traslado entre tus lugares guardados y el modo en que te mueves (a pie, bus, carro o moto), y avisa por etapas. Primero «prepárate», después «sal ya» y, si se pasó la hora, «vas tarde».

| Pantalla | Qué hace |
|---|---|
| Hoy | Lo que sigue y cuándo salir, con la banda de salida y la línea del día |
| Semana | El horario completo de clases y turnos |
| Actividades | Clases, turnos, exámenes y entregas, con sus lugares |
| Crear | Una actividad nueva, que se repite por semana dentro del semestre |

Las clases se generan solo entre las fechas del semestre y se saltan los festivos de Colombia. También avisa cuando dos actividades se cruzan o cuando no alcanzas a llegar de una a otra. Deja cancelar una sola sesión («hoy no hay Cálculo») sin tocar la serie, lleva la asistencia con el límite de faltas por materia y suma las horas trabajadas. Tiene widget de inicio, modo claro y oscuro, y está en español e inglés.

**CampusWatch** muestra la próxima actividad con su anillo de cuenta regresiva, el aviso «Es hora de salir», el horario de hoy y mañana y la asistencia. Tiene tarjeta, complicación y modo claro y oscuro. Funciona con el último día que le mandó el teléfono, así que sigue sirviendo sin conexión.

## Cómo probarla

Agenda **no necesita cuenta**. Todo se guarda en el teléfono, cifrado, y nada se envía a un servidor.

1. Instala el APK del teléfono (y el del reloj, si tienes un reloj Wear OS). Los APK los entrego aparte mientras se define el canal de publicación (ver «Lo que falta»).
2. Crea el semestre, tus lugares con el tiempo de traslado entre ellos y unas cuantas actividades para ver la hora de salida y los avisos.
3. En el reloj sin teléfono emparejado, entra a «Sin horario» y toca **«Ver demostración»**. Carga un día de ejemplo relativo a la hora actual (una clase en 18 minutos, un turno, una entrega) y muestra «Demostración» en el pie. Se sale desde la misma página y no se mezcla con datos reales.

El reloj y el teléfono tienen que estar firmados con la misma clave para hablar entre sí. Por eso los dos APK se instalan de la misma versión.

## Cómo correrla desde el código

Necesitas JDK 21 (con JDK 25 no compila) y el SDK de Android por línea de comandos. En `local.properties` va la ruta del SDK (`sdk.dir=...`), y ese archivo no se sube a git.

```bash
./gradlew assembleDebug                    # APK de teléfono y reloj
./gradlew testDebugUnitTest :core:domain:test
./gradlew lintDebug
./gradlew :app:installDebug :wear:installDebug
```

Para el APK de release se pone un `key.properties` con la firma. Sin él, la compilación de release usa la clave de depuración, igual para el teléfono y el reloj.

## Cómo está hecho

- **Kotlin + Jetpack Compose**, una sola Activity, MVVM en capas `ui / domain / data` y Hilt.
- **Room sobre SQLCipher.** La clave de la base de datos es aleatoria y se guarda envuelta con el Android Keystore.
- **Avisos** con `AlarmManager` exacto y WorkManager, que se reprograman al reiniciar o al cambiar la hora del teléfono.
- **El dominio** (`core/domain`) tiene la lógica sin Android, con sus pruebas (`ScheduleExpander`, `ConflictDetector`, `DepartureCalculator`, festivos colombianos).
- **El reloj** (`wear/`) usa el mismo `applicationId` que el teléfono, porque la capa de datos de Wear OS solo une apps con el mismo paquete y la misma firma.
- **Diseño:** el sistema de diseño de Agenda (tipografías Archivo y Atkinson Hyperlegible). Las decisiones y su verificación están en `docs/ux/` y las capturas de cada paso en `docs/capturas/`.

| Carpeta | Qué hay |
|---|---|
| `app/` | App del teléfono |
| `wear/` | CampusWatch, la app del reloj |
| `core/` | Dominio y lógica compartida |
| `docs/arquitectura.md` | Alcance y decisiones técnicas |
| `docs/legal/` | Política de privacidad (borrador) |
| `CONTINUAR.md` | Estado del proyecto y lo que sigue |

## Datos personales

Agenda guarda en el teléfono el horario, los lugares y la asistencia, cifrados. No hay cuenta, ni servidor, ni rastreo, y la app no pide permiso de internet ni de ubicación. Solo pide notificaciones, alarmas exactas y arrancar al encender el teléfono, para que los avisos lleguen al minuto. La política está en `docs/legal/politica-privacidad.md` y es un borrador técnico que debe revisar un abogado antes de publicar en Google Play.

## Lo que falta

- Probar la demostración en un reloj real con la versión release.
- No molestar automático durante las clases y el centro de avisos.
- Importar y exportar el horario en `.ics`.
- Definir el canal de actualizaciones (Google Play o GitHub Releases).

## Autor

Diego Armando Pinta Cuasquen · [GitHub](https://github.com/diarpicu2022-commits) · [Portafolio](https://mi-portafolio-beta-kohl.vercel.app)
