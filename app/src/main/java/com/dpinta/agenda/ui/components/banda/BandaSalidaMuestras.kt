package com.dpinta.agenda.ui.components.banda

import java.time.Duration
import java.time.LocalTime

/**
 * Datos de ejemplo coherentes entre sí para previews y pruebas de la banda.
 * Cálculo diferencial a las 8:00 en B-204; 23 min en bus + 7 de margen.
 * Los tiempos están escritos a mano como los devolvería el dominio.
 */
object BandaSalidaMuestras {
    private val base = BandaSalidaModelo(
        estado = EstadoBanda.Espera,
        horaSalida = LocalTime.of(7, 32),
        horaInicio = LocalTime.of(8, 0),
        horaLlegada = LocalTime.of(7, 14),
        minutosParaSalir = 41,
        actividad = "Cálculo diferencial",
        salon = "B-204",
        lugar = "Bloque B · Campus",
        modo = ModoTransporte.Bus,
        duracion = Duration.ofMinutes(23),
        margen = Duration.ofMinutes(7),
        calculadoHace = Duration.ofMinutes(3),
        datoViejo = false,
    )

    fun para(estado: EstadoBanda, datoViejo: Boolean = false): BandaSalidaModelo {
        val m = when (estado) {
            EstadoBanda.Espera -> base
            EstadoBanda.Preparate -> base.copy(estado = estado, minutosParaSalir = 12, horaLlegada = LocalTime.of(7, 43))
            EstadoBanda.SalYa -> base.copy(estado = estado, minutosParaSalir = 0, horaLlegada = LocalTime.of(7, 55))
            // Son las 7:41: si sale ahora llega a las 8:04.
            EstadoBanda.VasTarde -> base.copy(estado = estado, minutosParaSalir = -9, horaLlegada = LocalTime.of(8, 4))
            // Salió 7:35 con 23 min de trayecto: llega 7:58.
            EstadoBanda.EnCamino -> base.copy(estado = estado, minutosParaSalir = 0, horaLlegada = LocalTime.of(7, 58))
            EstadoBanda.SinTraslado -> base.copy(
                estado = estado,
                horaInicio = LocalTime.of(10, 30),
                actividad = "Física mecánica",
                salon = "L-3",
                lugar = "Bloque L · Campus",
            )
        }
        return if (datoViejo) m.copy(datoViejo = true, calculadoHace = Duration.ofMinutes(25)) else m
    }
}
