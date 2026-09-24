# Agenda — arquitectura técnica y alcance

Fecha: 2026-09-23 · Estado: propuesta, pendiente de visto bueno

## 1. Decisiones tomadas con Diego

| Tema | Decisión |
|---|---|
| Plataforma | Android nativo, Kotlin, Jetpack Compose |
| Tiempo de viaje | Google Routes API (`computeRoutes`), con tráfico y transporte público |
| Modos de transporte | Bus/transporte público, a pie, carro o moto (se elige por actividad; hay uno por defecto) |
| Datos | Solo en el teléfono, cifrados. Sin cuenta ni servidor |
| Entorno | Android SDK por línea de comandos + JDK 21 en `%LOCALAPPDATA%` |

## 2. Stack

- **UI**: Jetpack Compose + Material 3 como base técnica, con tema propio (lo define el contrato de diseño del anexo UX).
- **Arquitectura**: una sola Activity, MVVM con flujo unidireccional (`UiState` inmutable + eventos), en capas `ui / domain / data`.
- **DI**: Hilt.
- **Persistencia**: Room sobre SQLCipher (`net.zetetic:sqlcipher-android`). La contraseña de la base de datos es aleatoria (32 bytes) y se guarda envuelta con una clave AES-GCM del Android Keystore; nunca aparece en texto plano ni en el código.
- **Preferencias**: DataStore (sin secretos dentro).
- **Tiempo**: `java.time`. Las recurrencias se guardan como `LocalTime` + `DayOfWeek` + `ZoneId`, y los instantes concretos se calculan al programar (así los cambios de zona horaria no rompen nada).
- **Programación**: `AlarmManager.setExactAndAllowWhileIdle` para avisos que tienen que llegar al minuto, y WorkManager para los recálculos de ruta. `BOOT_COMPLETED`, `TIME_SET` y `TIMEZONE_CHANGED` reprograman todo.
- **Ubicación**: Fused Location Provider. Mapa y selección de lugar con Maps Compose y Places (New) Autocomplete.
- **Red**: Retrofit/OkHttp + kotlinx.serialization, solo HTTPS (`network_security_config` sin tráfico en claro).
- **Widget**: Jetpack Glance.
- **Calidad**: ktlint + detekt, pruebas unitarias del dominio (recurrencias, conflictos, cálculo de salida), pruebas de Room, pruebas de UI en Compose y CI en GitHub Actions.

## 3. Modelo de datos (núcleo)

```
Lugar        id, nombre, dirección, lat, lng, placeId?
Actividad    id, título, tipo (CLASE | TRABAJO | PUNTUAL | EXAMEN | OTRO), color,
             lugarId, sala (aula / piso / oficina), notas,
             modoTransporte?, margenMin (colchón al llegar), avisoAntesMin
Recurrencia  actividadId, díasSemana (bitmask), horaInicio, horaFin,
             desde, hasta (semestre), zona
Ocurrencia   actividadId, inicio, fin (para las puntuales)
Excepción    actividadId, fecha, tipo (CANCELADA | MOVIDA), nuevoInicio?
Semestre     id, nombre, inicio, fin, festivos[]
Tarea        id, actividadId?, título, vence, hecha
```

## 4. La alerta de salida

1. Para cada ocurrencia de hoy con lugar, un aviso de **pre-cálculo** se programa en `inicio − 120 min`.
2. Cuando salta, se obtiene la posición (`getCurrentLocation`, precisión equilibrada) y se llama a `computeRoutes` con el modo de la actividad (`TRANSIT`, `DRIVE` con `TRAFFIC_AWARE` o `WALK`) y `arrivalTime = inicio − margen`.
3. Hora de salida = `inicio − margen − duración`. Se programan una alarma exacta para esa hora y otro recálculo a mitad de camino (el tráfico cambia). A menos de 15 minutos, la última cifra calculada queda fija.
4. Si la actividad anterior termina en otro lugar, se calcula desde ese lugar y no desde la posición actual. Si el tiempo de traslado no alcanza, se avisa del **conflicto** en cuanto se crea la actividad.
5. Si no hay red o falta el permiso, se usa la última duración conocida para ese trayecto y la notificación lo dice («estimación de ayer»). Nunca se falla en silencio.
6. Durante el trayecto, una notificación en curso (Live Update en Android 16) muestra «llegas 7:55 · Aula B-204».

Permisos, explicados en pantalla **antes** de pedirlos: `POST_NOTIFICATIONS`, `ACCESS_FINE_LOCATION` y luego `ACCESS_BACKGROUND_LOCATION` (sin este último, la app pide la ubicación al abrirse y en lo demás usa el último trayecto conocido), `USE_EXACT_ALARM` (permitido para apps de calendario) y `RECEIVE_BOOT_COMPLETED`.

## 5. Seguridad

- Base de datos cifrada (SQLCipher) con la clave en el Keystore; `allowBackup=false` y reglas de extracción que excluyen la base y las preferencias. La copia de seguridad es una exportación manual cifrada con contraseña (AES-256-GCM + Argon2id/PBKDF2).
- Bloqueo opcional con biometría o PIN del dispositivo (BiometricPrompt) al abrir la app.
- **Clave de Google**: se lee de `local.properties` (fuera de git) con el Secrets Gradle Plugin. En Google Cloud queda restringida a este paquete + huella SHA-1 y a las APIs Routes y Places. Una clave dentro de un APK siempre se puede extraer; con esas restricciones no sirve fuera de la app.
- Se piden los permisos mínimos y cada uno se justifica en la UI. La ubicación nunca se guarda en historial ni se envía a otro sitio que no sea Google Routes.
- Release con R8 (minify + shrink), sin logs con datos personales (Timber solo en debug), `FLAG_SECURE` opcional en pantallas sensibles.
- Dependencias con versiones fijadas en un catálogo (`libs.versions.toml`), verificación de dependencias de Gradle y Dependabot.

## 6. Funciones recomendadas (además de lo pedido)

> 2026-09-23: Diego aprobó **todo** el alcance de esta sección (P1, P2 y P3). Las prioridades
> solo ordenan la construcción; nada se descarta. Reloj de referencia a futuro: Galaxy Watch8.

Prioridad 1: entran en la primera versión
1. **Ahora / Siguiente**: la pantalla de inicio responde «¿dónde tengo que estar y cuándo salgo?».
2. **Semestres y festivos**: las clases se generan solo entre las fechas del semestre y se saltan los festivos y las semanas sin clase.
3. **Detección de conflictos**: solapes y traslados imposibles entre dos actividades seguidas.
4. **Cancelar o mover una sola sesión** («hoy no hay Cálculo») sin tocar la serie.
5. **Widget** con la siguiente actividad y la hora de salida.

Prioridad 2
6. **Modo clase**: silencio o No Molestar automático durante clases y trabajo.
7. **Tareas y exámenes ligados a una materia**, con aviso días antes.
8. **Resumen matutino**: una notificación a la hora que elijas con el día completo y la primera salida.
9. **Asistencia**: marcar asistí/falté y llevar la cuenta de faltas por materia (útil si hay límite de faltas).
10. **Horas trabajadas** por semana o quincena, a partir de los turnos.

Prioridad 3
11. Importar/exportar `.ics` (horario de la universidad, Google Calendar).
12. Notas rápidas por clase.
13. Reloj Wear OS con la hora de salida.

## 7. Plan de implementación (por pasos, con visto bueno en cada uno)

0. Entorno: SDK, JDK 21, emulador.
1. Esqueleto del proyecto: módulos, catálogo de versiones, Hilt, lint, CI, cifrado de la base.
2. Dominio: recurrencias, semestres, conflictos y cálculo de salida, con pruebas.
3. Tokens de diseño (contrato elegido) → componente clave (tarjeta de salida) → esqueleto de navegación → resto de pantallas → estados.
4. Notificaciones y alarmas.
5. Ubicación + Routes API + alerta de salida.
6. Widget, modo clase, tareas (P2).
7. Verificación: pruebas, capturas del emulador en teléfono pequeño/grande, temas claro y oscuro, TalkBack, contraste medido.
