# Política de tratamiento de datos — Agenda y CampusWatch

> **Borrador técnico** · versión 0.2 · vigente desde el 10 de noviembre de 2026 (reemplaza la 0.1 del 7 de octubre).
> Antes de publicarlo para terceros debe revisarlo un abogado. El mismo texto se muestra en la app en Ajustes › Datos ›
> Política de privacidad (`ui/privacidad/Privacidad.kt`, `PoliticaPrivacidad`), y la app pide aceptarlo en el primer uso
> y cada vez que cambie la versión.

- **Responsable:** Diego Armando Pinta Cuasquen (desarrollador). Contacto: diarpicu2022@gmail.com.
- **Sin cuenta y sin servidor.** Agenda no tiene cuentas en internet ni inicio de sesión, y no tiene permiso de Internet.
  Todo se guarda **solo en el teléfono** y, para CampusWatch, en el reloj emparejado:
  1. **Perfil**: tu nombre (para el saludo) y, si quieres, carrera y universidad.
  2. **Agenda**: actividades, horarios, lugares guardados, tiempos de traslado, semestres y excepciones, en una base de
     datos cifrada (SQLCipher, clave en el Android Keystore).
  3. **Asistencia**: si fuiste o faltaste a cada clase. Se marca sola al tocar «Voy saliendo» o «Hoy no voy», o cuando
     respondes «¿Fuiste?» en el reloj. Va en la misma base cifrada.
  4. **Salidas reales («Tu ritmo»)**: la hora en que tocas «Voy saliendo» frente a la hora planeada. La app aprende de
     ellas, en el teléfono, cuántos minutos sueles salir tarde, y te lo aconseja. Va en la misma base cifrada. No se usa
     para nada más y no sale del teléfono.
  5. **Ajustes**: tema, resumen matutino y su hora (preferencias del teléfono, no son datos personales).
  6. **CampusWatch**: el horario de hoy y mañana, la asistencia por materia y el consejo de «Tu ritmo» ya calculado, que
     el teléfono le pasa al reloj por la conexión entre los dos (capa de datos de Wear OS). El reloj guarda además el tema
     que elijas en él. El reloj no habla con ningún servidor y no recibe el historial de salidas.
- **Qué no hace:** no usa publicidad, analítica ni rastreo, no hay cookies y no envía tus datos a nadie. No hay copia en
  la nube (la copia de seguridad de Android está desactivada).
- **Permisos:** notificaciones y alarmas exactas, para avisarte cuándo salir. Se explican antes de pedirlos.
- **Conservación:** hasta que los borres o desinstales la app.
- **Tus derechos** (Ley 1581 de 2012 y Decreto 1377 de 2013, compilado en el Decreto 1074 de 2015): conocer,
  actualizar, rectificar y suprimir tus datos y revocar la autorización. En la app: Ajustes › Tu perfil (corregir o
  borrar), borrar una actividad la elimina, y Ajustes › Datos › «Borrar todos mis datos» borra todo de una vez. Revocar
  la autorización es dejar de usar la app y borrar los datos. Pendiente: «Exportar mis datos».
- **Menores:** la app está pensada para estudiantes universitarios. Si la usa un menor de edad, con autorización de sus
  padres o representantes.
- **Cambios:** una nueva versión de esta política se muestra en la app con su fecha y se vuelve a pedir que la aceptes.
