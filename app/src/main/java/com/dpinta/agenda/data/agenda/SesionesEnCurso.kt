package com.dpinta.agenda.data.agenda

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lo que Diego dijo de la sesión en curso («Voy saliendo», «Ya llegué», «+5 min»). Lo comparten
 * la banda de Hoy y los botones de la notificación; vive en memoria, como hasta ahora en Hoy.
 */
@Singleton
class SesionesEnCurso @Inject constructor() {

    /** Sesión identificada por actividad + inicio. */
    data class Sesion(val actividadId: Long, val inicio: LocalDateTime)

    val salida = MutableStateFlow<Pair<Sesion, Instant>?>(null)
    val llegadas = MutableStateFlow<Set<Sesion>>(emptySet())
    val pospuesto = MutableStateFlow<Instant?>(null)
}
