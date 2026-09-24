package com.dpinta.agenda.depuracion

import android.content.Context
import androidx.core.content.edit
import com.dpinta.agenda.data.agenda.ActividadAGuardar
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.Cuando
import com.dpinta.agenda.data.agenda.HerramientasDepuracion
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Solo debug: la primera vez que se abre con la base vacía, guarda en Room el día de ejemplo de
 * los bocetos (Cálculo, Física, Turno con traslado insuficiente, Entrega) y tiempos de trayecto
 * de prueba. El extra «ejemplo=cruce» mueve Física a las 9:00 para ver un solape.
 */
class SemillaDepuracion @Inject constructor(
    @ApplicationContext private val contexto: Context,
    private val repositorio: AgendaRepository,
    private val reloj: Clock,
) : HerramientasDepuracion {

    override suspend fun alArrancar(ejemplo: String?) {
        val preferencias = contexto.getSharedPreferences("depuracion", Context.MODE_PRIVATE)
        val hoy = LocalDate.now(reloj)
        if (!preferencias.getBoolean("sembrado", false) && repositorio.agenda().first().vacia) {
            sembrar(hoy)
            preferencias.edit { putBoolean("sembrado", true) }
        }
        if (ejemplo == "cruce") {
            val agenda = repositorio.agenda().first()
            val fisica = agenda.actividades.values.firstOrNull { it.titulo == "Física mecánica" } ?: return
            repositorio.guardar(serie(fisica.id, "Física mecánica", ActivityKind.CLASE, "L-3", "Campus", 7, hoy, LocalTime.of(9, 0), LocalTime.of(10, 30)))
        }
    }

    private suspend fun sembrar(hoy: LocalDate) {
        repositorio.guardar(serie(null, "Cálculo diferencial", ActivityKind.CLASE, "B-204", "Campus", 7, hoy, LocalTime.of(8, 0), LocalTime.of(10, 0)))
        repositorio.guardar(serie(null, "Física mecánica", ActivityKind.CLASE, "L-3", "Campus", 7, hoy, LocalTime.of(10, 30), LocalTime.of(12, 0)))
        repositorio.guardar(serie(null, "Turno", ActivityKind.TRABAJO, "Caja 2", "Tienda centro", 10, hoy, LocalTime.of(12, 25), LocalTime.of(18, 0)))
        repositorio.guardar(
            ActividadAGuardar(
                null, "Entrega informe", ActivityKind.PUNTUAL,
                Cuando.Puntual(hoy, LocalTime.of(21, 0), LocalTime.of(21, 30)),
                lugar = "", salon = "", modo = TransportMode.TRANSPORTE_PUBLICO, margen = Duration.ZERO, aviso = Duration.ofMinutes(15),
            ),
        )
        val lugares = repositorio.agenda().first().lugares.values.associateBy { it.nombre }
        val campus = lugares.getValue("Campus").id
        val tienda = lugares.getValue("Tienda centro").id
        val minutos = mapOf(
            campus to mapOf(TransportMode.TRANSPORTE_PUBLICO to 23L, TransportMode.A_PIE to 48L, TransportMode.CARRO to 14L, TransportMode.MOTO to 11L),
            tienda to mapOf(TransportMode.TRANSPORTE_PUBLICO to 31L, TransportMode.A_PIE to 62L, TransportMode.CARRO to 18L, TransportMode.MOTO to 15L),
        )
        minutos.forEach { (lugar, porModo) ->
            porModo.forEach { (modo, m) -> repositorio.guardarEstimacion(lugar, TravelEstimate(Duration.ofMinutes(m), modo, reloj.instant(), false)) }
        }
        repositorio.guardarTraslado(campus, tienda, 35)
        repositorio.guardarTraslado(tienda, campus, 35)
    }

    private fun serie(
        id: Long?, titulo: String, tipo: ActivityKind, salon: String, lugar: String, margen: Long,
        hoy: LocalDate, inicio: LocalTime, fin: LocalTime,
    ) = ActividadAGuardar(
        id, titulo, tipo,
        Cuando.Semanal(DayOfWeek.entries.toSet(), inicio, fin, hoy.minusDays(120), hoy.plusDays(120)),
        lugar = lugar, salon = salon, modo = TransportMode.TRANSPORTE_PUBLICO,
        margen = Duration.ofMinutes(margen), aviso = Duration.ofMinutes(15),
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HerramientasModule {
    @Binds abstract fun herramientas(impl: SemillaDepuracion): HerramientasDepuracion
}
