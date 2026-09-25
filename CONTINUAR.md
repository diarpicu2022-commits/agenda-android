# Continuar el trabajo — Agenda

Última actualización: 2026-09-24. Leer antes [`CLAUDE.md`](CLAUDE.md) (reglas) y
[`docs/arquitectura.md`](docs/arquitectura.md) (alcance completo aprobado: P1, P2 y P3).

## 1. Preparar el entorno (Linux / nube)

```bash
# JDK 21 (el proyecto compila con toolchain 21; JDK 25 no sirve para AGP)
sudo apt-get update && sudo apt-get install -y openjdk-21-jdk unzip
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Android SDK por línea de comandos
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools && cd $ANDROID_HOME/cmdline-tools
curl -sSLo clt.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
unzip -q clt.zip && mv cmdline-tools latest && rm clt.zip
yes | latest/bin/sdkmanager --licenses >/dev/null
latest/bin/sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
cd -
echo "sdk.dir=$ANDROID_HOME" > local.properties   # local.properties NO se sube a git

./gradlew assembleDebug lintDebug testDebugUnitTest :core:domain:test
```

- Versiones fijadas en `gradle/libs.versions.toml`: AGP 9.4.1 (Kotlin integrado), Kotlin 2.4.20, Compose BOM 2026.09.00,
  compileSdk 37, targetSdk 36, minSdk 29, Gradle 9.7.1.
- **Memoria**: `org.gradle.jvmargs=-Xmx2g`. No lanzar dos Gradle a la vez.
- **En la nube no suele haber emulador** (sin KVM). Ahí se compila, se pasa lint y se prueba en JVM.
  Las capturas y las pruebas instrumentadas se ejecutan en local (emulador `Agenda_Pixel`, API 36, o el Galaxy S25 FE de Diego)
  con `tools/verificacion/capturas_paso*.sh -s <serial>`, y se declaran **pendientes** si no hay dispositivo.

## 2. Estado actual

| Paso de diseño | Estado | Evidencia |
|---|---|---|
| 1 · Esqueleto (Hilt, Room+SQLCipher, CI) | Hecho | `docs/capturas/paso1-*` |
| 2 · Tokens (dirección A, Archivo 62 + Atkinson) | Hecho, 0 fallos de contraste sobre render | `docs/capturas/paso2-*` |
| 3 · Componente clave: banda de salida (7 estados) | Hecho, 34 capturas, 0 fallos | `docs/capturas/paso3-*` |
| 4 · Esqueleto de navegación (Hoy · Semana · Actividades + Crear) | Hecho, 79 mediciones, 0 fallos reales | `docs/capturas/paso4-*` |
| 5 · Crear / editar actividad | Hecho; verificado en emulador salvo lo pendiente abajo | `build/verificacion/p5` (local) |

Dominio (`core/domain`), con pruebas: `ScheduleExpander` (series semanales, semestre, festivos, excepciones),
`ConflictDetector` (solapes y traslado insuficiente), `DepartureCalculator` (hora de salida; estados espera / prepárate ≤15 min /
sal ya (2 min de gracia) / vas tarde; dato viejo > 15 min; recálculo a mitad de camino), `DayPlanner`/`Trip`.
73 pruebas unitarias en verde. Los datos viven **en memoria** (`AgendaEnMemoria`) detrás de `AgendaRepository`.

**2026-09-24 (sesión en la nube, solo dominio):** se añadieron, con pruebas, `ColombianHolidays` (Ley Emiliani + Pascua;
`between()` llena `Semester.daysOff`), `AlarmPlanner` (precálculo a −120 min y salida solo si hay traslado; aviso propio
de la actividad; nada en el pasado; idempotente para reprogramar tras `BOOT_COMPLETED`/`TIME_SET`) y `WorkedHours` +
`PayPeriods` (semana lunes–domingo y quincena 1–15 / 16–fin). Después, en la misma sesión: `ics/IcsReader` + `IcsWriter`
(P3.11: VEVENT, RRULE semanal con BYDAY/UNTIL/COUNT, EXDATE, RECURRENCE-ID, TZID/UTC/flotante, líneas plegadas;
las reglas no semanales se conservan en `unsupportedRule` para avisar, no se inventan) y `Attendance` (P2.9: faltas por
materia contra un límite opcional; solo cuentan las sesiones que hubo), `QuietMode` (P2.6: tramos de No Molestar,
uniendo sesiones seguidas), `TaskReminders` (P2.7: vencida / hoy / aviso N días antes, exámenes primero) y
`MorningBriefing` (P2.8: sesiones del día, primera salida con traslado, tareas y horas de trabajo).
`:core:domain`: 37 pruebas, 0 fallos. El modelo de datos aún no tiene tabla de tareas (arquitectura §3: `Tarea`). **Nada de esto está
conectado a la UI todavía** y la parte Android no se compiló: en esa sesión `dl.google.com` (SDK y Google Maven) estaba
bloqueado por la red del entorno. Para probar el dominio sin SDK: `bash tools/verificacion/dominio_sin_sdk.sh`.

Decisiones de Diego ya aplicadas (Registro de enmiendas del anexo): Big Shoulders → Archivo 62; horas pasadas en tinta con «✓»;
solape «Se cruza con {actividad} {hora}»; etiqueta de pestaña que no cabe se reduce sola (mín. 14 sp, nunca parte palabras);
alcance completo P1–P3; reloj según el ajuste del teléfono; mano derecha; transporte bus / a pie / carro / moto.

## 3. Pendiente inmediato (antes del siguiente paso)

1. Con 360 dp + fuente 2,0, el script no detecta la frase-resumen del formulario: revisar si se sale de la vista o si es el detector.
2. ~~Placeholders de hora~~, ~~decisiones del paso 5~~ y ~~enmienda C6~~: **aprobados por Diego el 2026-09-24**
   (anotado en el Registro de enmiendas del anexo).
5. Pruebas instrumentadas (`connectedDebugAndroidTest`) y recorrido real de TalkBack: sin ejecutar.
6. ~~Compilar la app completa con el dominio nuevo~~: hecho en local el 2026-09-24 — build, lint y 95 pruebas en verde.

## 4. Siguientes pasos (cada uno se muestra y espera visto bueno)

1. ~~**Persistencia real**~~ Hecho 2026-09-24 (rama `persistencia`): Room + SQLCipher v1 (actividad, lugar, serie, puntual, excepción, semestre + días sin clase con `festivosPropuestos`, trayecto, traslado), `AgendaRoom` en Hilt, `AgendaEnMemoria` solo en pruebas, semilla en `src/debug`; 99 pruebas (4 de Room con Robolectric). Pendiente: prueba de migración al crear v2, y verificar en emulador que la semilla aparece y que release arranca en «primer uso».
2. **Notificaciones y alarmas** — parte lógica hecha 2026-09-24 (rama `avisos`): paquete `avisos/` con 4 canales del anexo,
   `ProgramadorAvisos` (AlarmPlanner → `setExactAndAllowWhileIdle`, 7 días, idempotente, se rehace al cambiar la agenda),
   receptores de alarma y de `BOOT_COMPLETED`/`TIME_SET`/`TIMEZONE_CHANGED`/`MY_PACKAGE_REPLACED`, textos «Sal a las…» y
   «Empieza…» con prueba. Verificado en emulador: 58 alarmas exactas (`policy_permission`), aviso de salida y «Empieza»
   llegan tras cambiar la hora (`docs/capturas/paso6-avisos-notificaciones.png`).
   Hecho después (misma rama): pantalla previa del permiso (Flujo 3, una vez, al haber actividades), franja «Sin avisos»
   (C10) que abre el diálogo o Ajustes, y acciones Voy saliendo · +5 min · Hoy no voy (estado compartido en
   `SesionesEnCurso`). Verificado en emulador: contraste sobre render ≥ 5,68:1 (textos nuevos 14,6–16,2:1), toques ≥ 48 dp,
   +5 min reprograma a +5, Hoy no voy cancela la sesión y rehace las alarmas (`docs/capturas/paso6-*`).
   Copia sin cláusula, pendiente de visto bueno: «Ahora no» y «Agenda · en 15 min» del ejemplo.
   Sin medir: oscuro, 360 dp y fuente 2,0 de estas pantallas; «Voy saliendo» desde la notificación; TalkBack.
   Hecho después: «Sal a las» 15 min antes de salir (ventana «prepárate»), «Sal ya» a la hora de salida y «Vas N min tarde»
   2 min después si no tocó «Voy saliendo» (dominio: `AlarmKind.SAL_YA`/`VAS_TARDE`); resumen matutino a las 6:00 fijas
   (`HORA_RESUMEN`, falta el ajuste para elegirla) que no se pierde si coincide con un reprogramado; las alarmas atrasadas
   no avisan de sesiones ya empezadas. Icono de la app (enmienda en el anexo). Verificado en emulador cambiando la hora.
   Pendiente de visto bueno: adelantar «Sal a las» a 15 min antes (antes sonaba a la hora de salida).
   Falta: reprogramar al guardar una estimación (hoy solo lo hace el precálculo);
   la semilla no guarda trayecto hacia «Tienda centro» (lugar 2), por eso el Turno no tiene alarma de salida.
3. **Ubicación + Google Routes API** (TRANSIT, DRIVE con tráfico, TWO_WHEELER, WALK) y Places (New) para el mapa.
   Diego tiene que crear la API key en Google Cloud (facturación activa; restringida a paquete + SHA-1 y a Routes/Places).
   La clave va en `local.properties` con el Secrets Gradle Plugin. Live Update solo cuando Diego toca «Voy saliendo».
4. Semana y Actividades con datos reales. **Actividades hecha 2026-09-25** (rama `avisos`): Clases · Trabajo · Puntuales
   (examen cuenta como clase, «otro» como puntual) y Lugares guardados con su número de actividades; tocar una fila
   abre la edición; «lun, mié y vie · 8:00 → 10:00 a. m. · Campus». Medido en emulador claro, oscuro y 360 dp + fuente
   2,0 (`docs/capturas/paso7-*`): contraste mínimo 5,68:1 (barra inferior, previa), toques ≥ 48 dp.
   **Semana hecha 2026-09-25**: rejilla de 7 días con franjas de 48 dp, hoy invertido, línea de «ahora» solo en hoy,
   celdas en `papel-2` con cuadrado de tipo + salón (rol nuevo `celda`) + nombre abreviado, carriles si dos sesiones se
   cruzan, festivos del semestre, número de semana, ‹ › y «Volver a esta semana». Con letra grande se desliza en
   horizontal (enmiendas del 2026-09-25 en el anexo). `docs/capturas/paso8-*`.
   Copia sin cláusula, pendiente de visto bueno: «Esta semana no tienes nada fijo.» y «Volver a esta semana».
5. Prioridad 1 de la arquitectura: widget Glance (siguiente actividad + hora de salida), semestres/festivos en la UI.
6. Prioridad 2: modo clase (No Molestar), tareas y exámenes por materia, resumen matutino, asistencia, horas trabajadas.
7. Prioridad 3: importar/exportar `.ics`, notas por clase, **Wear OS** (Diego piensa comprar un Galaxy Watch8; mientras, emulador Wear OS 6).
8. Versión final: APK/AAB de release firmado (keystore fuera de git), R8, sin logs de datos personales.

## 5. Herramientas de verificación

`tools/verificacion/capturas_paso4.sh` y `capturas_paso5.sh` (+ `medir_*.py`): instalan el APK, capturan en claro/oscuro,
360 dp y fuente 2,0, restauran el dispositivo al terminar y miden contraste/toques/textos.
Uso: `bash tools/verificacion/capturas_paso5.sh -s <serial-adb>`. En Git Bash (Windows) exportar `MSYS_NO_PATHCONV=1`.
En el emulador `Agenda_Pixel` usar `-memory 2048` y **no** compilar a la vez (el PC de Diego tiene 12 GB).

## 6. Activar la compilación automática en GitHub (CI)

El flujo de CI está en `docs/ci/ci.yml` y no en `.github/workflows/`, porque el token con el que se subió el repo no tenía
el permiso `workflow`. Para activarlo:

```bash
gh auth refresh -h github.com -s workflow     # una vez, autoriza en el navegador
mkdir -p .github/workflows && git mv docs/ci/ci.yml .github/workflows/ci.yml
git commit -m "Activar CI" && git push
```

Ojo: el runner necesita `platforms;android-37.0`. Si falla por la plataforma, añadir antes del build un paso con
`$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-37.0"`.
