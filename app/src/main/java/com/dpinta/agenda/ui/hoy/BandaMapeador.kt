package com.dpinta.agenda.ui.hoy

import com.dpinta.agenda.domain.DeparturePlan
import com.dpinta.agenda.domain.DepartureState
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.domain.Trip
import com.dpinta.agenda.ui.components.banda.BandaSalidaModelo
import com.dpinta.agenda.ui.components.banda.EstadoBanda
import com.dpinta.agenda.ui.components.banda.ModoTransporte
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** La sesión a la que apunta la banda, ya resuelta a textos. */
data class SesionBanda(
    val actividad: String,
    val salon: String,
    val lugar: String,
    val inicio: Instant,
    val margen: Duration,
    val modo: TransportMode,
)

/**
 * Traduce el dominio a la banda. No calcula tiempos: los toma de [DeparturePlan] y de [Trip].
 *
 * Unifica los estados: el dominio conoce ESPERA, PREPARATE, SAL_YA y VAS_TARDE; la UI añade
 * «en camino» (Diego tocó «Voy saliendo») y «sin traslado» (no hay lugar, o es el mismo que la
 * sesión anterior, o ya llegó).
 */
object BandaMapeador {

    fun modelo(
        sesion: SesionBanda,
        plan: DeparturePlan?,
        estimacion: TravelEstimate?,
        salioEn: Instant?,
        ahora: Instant,
        zona: ZoneId,
    ): BandaSalidaModelo {
        val inicio = hora(sesion.inicio, zona)
        val modo = aModoUi(sesion.modo)
        if (plan == null || estimacion == null) {
            return BandaSalidaModelo(
                estado = EstadoBanda.SinTraslado,
                horaSalida = inicio,
                horaInicio = inicio,
                horaLlegada = inicio,
                minutosParaSalir = 0,
                actividad = sesion.actividad,
                salon = sesion.salon,
                lugar = sesion.lugar,
                modo = modo,
                duracion = Duration.ZERO,
                margen = sesion.margen,
                calculadoHace = Duration.ZERO,
                datoViejo = false,
            )
        }
        val calculadoHace = Duration.between(estimacion.computedAt, ahora).let { if (it.isNegative) Duration.ZERO else it }
        val base = BandaSalidaModelo(
            estado = aEstadoUi(plan.state),
            horaSalida = hora(plan.leaveAt, zona),
            horaInicio = inicio,
            horaLlegada = hora(plan.arriveIfLeavingNow, zona),
            minutosParaSalir = plan.minutesToLeave,
            actividad = sesion.actividad,
            salon = sesion.salon,
            lugar = sesion.lugar,
            modo = aModoUi(estimacion.mode),
            duracion = estimacion.duration,
            margen = sesion.margen,
            calculadoHace = calculadoHace,
            datoViejo = plan.stale,
            manual = estimacion.manual,
        )
        if (salioEn == null) return base
        val trayecto = Trip.progress(salioEn, estimacion, ahora)
        return base.copy(
            estado = EstadoBanda.EnCamino,
            horaLlegada = hora(trayecto.arriveAt, zona),
            duracion = Duration.ofMinutes(trayecto.minutesRemaining),
        )
    }

    fun aEstadoUi(estado: DepartureState): EstadoBanda = when (estado) {
        DepartureState.ESPERA -> EstadoBanda.Espera
        DepartureState.PREPARATE -> EstadoBanda.Preparate
        DepartureState.SAL_YA -> EstadoBanda.SalYa
        DepartureState.VAS_TARDE -> EstadoBanda.VasTarde
    }

    fun aModoUi(modo: TransportMode): ModoTransporte = when (modo) {
        TransportMode.TRANSPORTE_PUBLICO -> ModoTransporte.Bus
        TransportMode.A_PIE -> ModoTransporte.APie
        TransportMode.CARRO -> ModoTransporte.Carro
        TransportMode.MOTO -> ModoTransporte.Moto
    }

    fun aModoDominio(modo: ModoTransporte): TransportMode = when (modo) {
        ModoTransporte.Bus -> TransportMode.TRANSPORTE_PUBLICO
        ModoTransporte.APie -> TransportMode.A_PIE
        ModoTransporte.Carro -> TransportMode.CARRO
        ModoTransporte.Moto -> TransportMode.MOTO
    }

    private fun hora(instante: Instant, zona: ZoneId): LocalTime =
        instante.atZone(zona).toLocalTime().withSecond(0).withNano(0)
}
