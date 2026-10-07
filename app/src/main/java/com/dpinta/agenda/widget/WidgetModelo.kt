package com.dpinta.agenda.widget

import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.domain.DayPlanner
import com.dpinta.agenda.domain.DepartureState
import com.dpinta.agenda.domain.DayFocus
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
        /** Estado de la salida (null sin trayecto): pasada la hora ya no dice «sal a las», dice «sal ya» o «vas tarde». */
        val estado: DepartureState? = null,
    ) : WidgetModelo

    /** La sesión en curso se queda en el widget mientras dura (DayFocus): «AHORA · HASTA 10:00». */
    data class EnCurso(
        val actividad: String,
        val salon: String,
        val lugar: String,
        val fin: LocalTime,
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
        val actual = DayFocus.current(validas, ahora, { it.start }, { it.end })
        val siguiente = DayPlanner.next(validas, ahora)
        if (siguiente == null && actual == null) return WidgetModelo.SinNada
        val instante = ahora.atZone(zona).toInstant()
        val planDe = { o: Occurrence ->
            val a = agenda.actividades.getValue(o.activityId)
            val e = if (DayPlanner.needsTravel(DayPlanner.previous(validas, o), o)) trayecto(o) else null
            e?.let { it to DepartureCalculator.plan(o.start.atZone(zona).toInstant(), a.margen, it, instante) }
        }
        val planSiguiente = siguiente?.let(planDe)
        val salidaSiguiente = planSiguiente?.second?.leaveAt?.atZone(zona)?.toLocalDateTime()
        if (actual != null && DayFocus.showCurrent(ahora, actual.end, siguiente?.start, salidaSiguiente)) {
            val a = agenda.actividades.getValue(actual.activityId)
            return WidgetModelo.EnCurso(
                actividad = a.titulo,
                salon = a.salon,
                lugar = a.lugarId?.let { agenda.lugares[it]?.nombre }.orEmpty(),
                fin = actual.end.toLocalTime(),
                despues = siguiente?.let { n -> agenda.actividades.getValue(n.activityId).let { WidgetModelo.Fila(n.start.toLocalTime(), it.titulo, it.salon) } },
            )
        }
        siguiente ?: return WidgetModelo.SinNada
        val act = agenda.actividades.getValue(siguiente.activityId)
        val estimacion = planSiguiente?.first
        val plan = planSiguiente?.second
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
            hace = if (plan?.stale == true && estimacion != null) Duration.between(estimacion.computedAt, instante) else null,
            despues = despues,
            estado = plan?.state,
        )
    }
}
