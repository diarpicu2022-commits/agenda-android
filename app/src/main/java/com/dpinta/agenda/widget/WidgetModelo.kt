package com.dpinta.agenda.widget

import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.domain.DayPlanner
import com.dpinta.agenda.domain.DepartureCalculator
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.TravelEstimate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Lo que dibuja el widget «Siguiente» (anexo, «Widget Siguiente»). */
sealed interface WidgetModelo {
    data object PrimerUso : WidgetModelo
    data object SinNada : WidgetModelo

    data class Siguiente(
        /** «SAL A LAS» si hay trayecto; «EMPIEZA» si no (misma sede o sin lugar). */
        val salir: Boolean,
        val hora: LocalTime,
        /** Null si es hoy; si no, el día de la sesión. */
        val dia: LocalDate?,
        val salon: String,
        val actividad: String,
        val lugar: String,
        /** Minutos desde el último cálculo, solo si es dato viejo (C10). */
        val hace: Duration?,
        /** La siguiente fila, para 4×2. */
        val despues: Fila?,
    ) : WidgetModelo

    data class Fila(val hora: LocalTime, val actividad: String, val salon: String)
}

object WidgetMapeador {

    /**
     * @param sesiones las sesiones desde hoy en adelante (unos días), ya expandidas.
     * @param trayecto estimación hacia una sesión que necesita trayecto, o null si no se sabe.
     */
    fun modelo(
        agenda: Agenda,
        sesiones: List<Occurrence>,
        ahora: LocalDateTime,
        zona: ZoneId,
        trayecto: (Occurrence) -> TravelEstimate?,
    ): WidgetModelo {
        if (agenda.vacia) return WidgetModelo.PrimerUso
        val validas = sesiones.filter { it.activityId in agenda.actividades }
        val siguiente = DayPlanner.next(validas, ahora) ?: return WidgetModelo.SinNada
        val act = agenda.actividades.getValue(siguiente.activityId)
        val necesita = DayPlanner.needsTravel(DayPlanner.previous(validas, siguiente), siguiente)
        val estimacion = if (necesita) trayecto(siguiente) else null
        val instante = ahora.atZone(zona).toInstant()
        val plan = estimacion?.let { DepartureCalculator.plan(siguiente.start.atZone(zona).toInstant(), act.margen, it, instante) }
        val despues = validas.sortedBy { it.start }.firstOrNull { it.start.isAfter(siguiente.start) }?.let { s ->
            val a = agenda.actividades.getValue(s.activityId)
            WidgetModelo.Fila(s.start.toLocalTime(), a.titulo, a.salon)
        }
        return WidgetModelo.Siguiente(
            salir = plan != null,
            hora = plan?.leaveAt?.atZone(zona)?.toLocalTime() ?: siguiente.start.toLocalTime(),
            dia = siguiente.start.toLocalDate().takeIf { it != ahora.toLocalDate() },
            salon = act.salon,
            actividad = act.titulo,
            lugar = act.lugarId?.let { agenda.lugares[it]?.nombre }.orEmpty(),
            hace = if (plan?.stale == true) Duration.between(estimacion.computedAt, instante) else null,
            despues = despues,
        )
    }
}
