package com.dpinta.agenda.ui.hoy

import androidx.compose.runtime.Immutable
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.ui.components.banda.BandaSalidaModelo
import java.time.LocalDate
import java.time.LocalTime

/** Tipos que se distinguen a la vista (C3: clase, trabajo, puntual). */
enum class TipoFila { Clase, Trabajo, Puntual }

/** Examen cuenta como clase y «otro» como puntual: el marcador solo distingue tres (C6). */
fun ActivityKind.aTipoFila(): TipoFila = when (this) {
    ActivityKind.CLASE, ActivityKind.EXAMEN -> TipoFila.Clase
    ActivityKind.TRABAJO -> TipoFila.Trabajo
    ActivityKind.PUNTUAL, ActivityKind.OTRO -> TipoFila.Puntual
}

@Immutable
data class FilaHoy(
    val clave: String,
    /** Para abrir la edición al tocar la fila. */
    val actividadId: Long,
    val inicio: LocalTime,
    val fin: LocalTime,
    val titulo: String,
    val tipo: TipoFila,
    val salon: String,
    val lugar: String,
    /** Ya terminó: se marca con «✓» sobre el hilo (C6). Horas en tinta, AAA (C9.1). */
    val pasada: Boolean,
    /** Conflicto inline bajo la fila (C10). */
    val conflicto: ConflictoFila?,
)

/** Conflictos de C10. La hora se formatea en la UI (12/24 h, C1.1). */
sealed interface ConflictoFila {
    data class Traslado(val disponibles: Long, val necesarios: Long) : ConflictoFila
    data class Cruce(val actividad: String, val hora: LocalTime) : ConflictoFila
}

/** Texto literal de C10: «25 min de traslado, necesitas 35» / «Se cruza con Física 9:00». */
fun textoConflicto(c: ConflictoFila, es24h: Boolean): String = when (c) {
    is ConflictoFila.Traslado -> "${c.disponibles} min de traslado, necesitas ${c.necesarios}"
    is ConflictoFila.Cruce ->
        "Se cruza con ${c.actividad} ${com.dpinta.agenda.ui.components.banda.formatearHora(c.hora, es24h).enLinea}"
}

/** Primera sesión de los próximos días cuando hoy no hay nada (C10 «día sin nada»). */
data class SiguienteDia(val fecha: LocalDate, val hora: LocalTime, val titulo: String, val salon: String)

sealed interface HoyUiState {
    /** C10 «cargando»: esqueleto de filas, sin spinner. */
    data object Cargando : HoyUiState

    /** C10 «vacío, primer uso»: no hay ninguna actividad todavía. */
    data object PrimerUso : HoyUiState

    @Immutable
    data class Dia(
        val fecha: LocalDate,
        val ahora: LocalTime,
        /** Null si hoy no queda nada por delante. */
        val banda: BandaSalidaModelo?,
        val filas: List<FilaHoy>,
        /** Posición de la línea de «ahora» entre las filas (C6): antes de la fila con este índice. */
        val indiceAhora: Int,
        /** Solo si hoy no hay filas. */
        val siguiente: SiguienteDia?,
        /**
         * Sesión en curso mientras dura (DayFocus, decisión de Diego 2026-10-07): mientras no sea nula, Hoy la muestra en la
         * tarjeta AHORA en vez del boleto; el boleto vuelve cuando ya toca prepararse para la siguiente.
         */
        val enCurso: EnCurso? = null,
        /** Primer nombre del perfil local para el saludo; null si no lo ha escrito. */
        val nombre: String? = null,
    ) : HoyUiState
}

/** La sesión que está pasando ahora, con lo siguiente para la línea «Después: …». */
@Immutable
data class EnCurso(
    val actividadId: Long,
    val titulo: String,
    val tipo: TipoFila,
    val salon: String,
    val lugar: String,
    val inicio: LocalTime,
    val fin: LocalTime,
    val despues: SiguienteDia?,
)
