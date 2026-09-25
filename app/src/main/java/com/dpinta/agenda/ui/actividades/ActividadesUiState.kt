package com.dpinta.agenda.ui.actividades

import androidx.compose.runtime.Immutable
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.ui.hoy.TipoFila
import com.dpinta.agenda.ui.hoy.aTipoFila
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface ActividadesUiState {
    data object Cargando : ActividadesUiState
    data object Vacia : ActividadesUiState

    @Immutable
    data class Lista(val secciones: List<Seccion>, val lugares: List<LugarFila>) : ActividadesUiState
}

/** Arquitectura de información del anexo: Clases · Trabajo · Puntuales; después, Lugares guardados. */
enum class Grupo(val titulo: String) { Clases("Clases"), Trabajo("Trabajo"), Puntuales("Puntuales") }

@Immutable
data class Seccion(val grupo: Grupo, val filas: List<ActividadFila>)

/** Cuándo se repite o cuándo es; el texto final lo arma la pantalla según el reloj del teléfono. */
sealed interface Cuando {
    val inicio: LocalTime
    val fin: LocalTime

    data class Semanal(val dias: List<DayOfWeek>, override val inicio: LocalTime, override val fin: LocalTime) : Cuando
    data class Fecha(val fecha: LocalDate, override val inicio: LocalTime, override val fin: LocalTime) : Cuando
}

@Immutable
data class ActividadFila(
    val id: Long,
    val titulo: String,
    val tipo: TipoFila,
    val salon: String,
    val lugar: String,
    val cuando: List<Cuando>,
    /** Puntual que ya pasó: va al final de su sección; no cambia de color. */
    val pasada: Boolean,
)

@Immutable
data class LugarFila(val nombre: String, val actividades: Int)

object ActividadesMapeador {

    fun estado(agenda: Agenda, hoy: LocalDate): ActividadesUiState {
        if (agenda.vacia) return ActividadesUiState.Vacia
        val reglas = agenda.reglas.groupBy { it.activityId }
        val puntuales = agenda.puntuales.groupBy { it.activityId }
        val filas = agenda.actividades.values.map { act ->
            val series = reglas[act.id].orEmpty().map { r -> Cuando.Semanal(r.days.sorted(), r.start, r.end) }
            val fechas = puntuales[act.id].orEmpty().sortedBy { it.start }
                .map { o -> Cuando.Fecha(o.start.toLocalDate(), o.start.toLocalTime(), o.end.toLocalTime()) }
            val cuando = series + fechas
            val pasada = series.isEmpty() && fechas.isNotEmpty() && fechas.all { it.fecha.isBefore(hoy) }
            act.tipo to ActividadFila(
                id = act.id,
                titulo = act.titulo,
                tipo = act.tipo.aTipoFila(),
                salon = act.salon,
                lugar = act.lugarId?.let { agenda.lugares[it]?.nombre }.orEmpty(),
                cuando = cuando,
                pasada = pasada,
            )
        }
        val secciones = filas.groupBy({ grupo(it.first) }, { it.second })
            .map { (g, f) -> Seccion(g, f.sortedWith(compareBy<ActividadFila>({ it.pasada }, { orden(it) }, { it.titulo }))) }
            .sortedBy { it.grupo.ordinal }
        val usos = agenda.actividades.values.groupingBy { it.lugarId }.eachCount()
        val lugares = agenda.lugares.values
            .map { LugarFila(it.nombre, usos[it.id] ?: 0) }
            .sortedBy { it.nombre.lowercase() }
        return ActividadesUiState.Lista(secciones, lugares)
    }

    private fun grupo(tipo: ActivityKind) = when (tipo.aTipoFila()) {
        TipoFila.Clase -> Grupo.Clases
        TipoFila.Trabajo -> Grupo.Trabajo
        TipoFila.Puntual -> Grupo.Puntuales
    }

    /** Primero lo que ocurre antes en la semana (series) o antes en el calendario (puntuales). */
    private fun orden(f: ActividadFila): String = when (val c = f.cuando.firstOrNull()) {
        is Cuando.Semanal -> "0${c.dias.first().value}${c.inicio}"
        is Cuando.Fecha -> "1${c.fecha}${c.inicio}"
        null -> "2"
    }
}

private val DIAS = mapOf(
    DayOfWeek.MONDAY to "lun", DayOfWeek.TUESDAY to "mar", DayOfWeek.WEDNESDAY to "mié", DayOfWeek.THURSDAY to "jue",
    DayOfWeek.FRIDAY to "vie", DayOfWeek.SATURDAY to "sáb", DayOfWeek.SUNDAY to "dom",
)

/** «lun, mié y vie» · «lun a vie» · «todos los días». */
fun textoDias(dias: List<DayOfWeek>): String {
    val orden = dias.distinct().sorted()
    if (orden.size == 7) return "todos los días"
    if (orden == DayOfWeek.entries.take(5)) return "lun a vie"
    val nombres = orden.map { DIAS.getValue(it) }
    return if (nombres.size == 1) nombres[0] else nombres.dropLast(1).joinToString(", ") + " y " + nombres.last()
}
