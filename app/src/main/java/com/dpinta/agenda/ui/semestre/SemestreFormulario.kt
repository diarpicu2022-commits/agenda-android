package com.dpinta.agenda.ui.semestre

import com.dpinta.agenda.data.agenda.SemestreGuardado
import com.dpinta.agenda.data.agenda.festivosPropuestos
import com.dpinta.agenda.ui.formulario.Interprete
import java.time.LocalDate

/** Lo que se escribe en el formulario de semestre, tal cual (texto), más los días sin clase ya decididos. */
data class SemestreFormulario(
    val nombre: String = "",
    val inicio: String = "",
    val fin: String = "",
    val dias: Map<LocalDate, String> = emptyMap(),
    /** Mientras Diego no toque la lista, los festivos se recalculan al cambiar las fechas. */
    val diasTocados: Boolean = false,
    val nuevoDia: String = "",
    val nuevoMotivo: String = "",
) {
    companion object {
        fun de(s: SemestreGuardado) = SemestreFormulario(
            nombre = s.nombre,
            inicio = "${s.inicio.dayOfMonth}/${s.inicio.monthValue}/${s.inicio.year}",
            fin = "${s.fin.dayOfMonth}/${s.fin.monthValue}/${s.fin.year}",
            dias = s.diasSinClase,
            diasTocados = true,
        )
    }
}

data class ErroresSemestre(val nombre: String? = null, val inicio: String? = null, val fin: String? = null, val nuevoDia: String? = null) {
    val ninguno: Boolean get() = nombre == null && inicio == null && fin == null
}

object SemestreLogica {

    fun inicio(f: SemestreFormulario, hoy: LocalDate): LocalDate? = Interprete.fecha(f.inicio, hoy)

    /** Sin año, el fin es la primera fecha así que cae después del inicio (un semestre de agosto a enero). */
    fun fin(f: SemestreFormulario, hoy: LocalDate): LocalDate? {
        val fin = Interprete.fecha(f.fin, hoy) ?: return null
        val inicio = inicio(f, hoy) ?: return fin
        val conAnio = f.fin.trim().split('/', '-', '.').filter { it.isNotBlank() }.size == 3
        return if (!conAnio && fin.isBefore(inicio)) fin.plusYears(1) else fin
    }

    /** Recalcula los festivos propuestos si la lista no se ha tocado y las dos fechas son válidas. */
    fun conFechas(f: SemestreFormulario, hoy: LocalDate): SemestreFormulario {
        if (f.diasTocados) return f
        val i = inicio(f, hoy) ?: return f.copy(dias = emptyMap())
        val n = fin(f, hoy)?.takeIf { !it.isBefore(i) } ?: return f.copy(dias = emptyMap())
        return f.copy(dias = festivosPropuestos(i, n))
    }

    fun quitar(f: SemestreFormulario, fecha: LocalDate) = f.copy(dias = f.dias - fecha, diasTocados = true)

    /** Añade el día escrito; devuelve el error si la fecha no se entiende o cae fuera del semestre. */
    fun anadir(f: SemestreFormulario, hoy: LocalDate): Pair<SemestreFormulario, String?> {
        val fecha = Interprete.fecha(f.nuevoDia, hoy) ?: return f to "Escribe la fecha como 12/10"
        val i = inicio(f, hoy)
        val n = fin(f, hoy)
        if (i != null && n != null && (fecha.isBefore(i) || fecha.isAfter(n))) return f to "Ese día no está dentro del semestre"
        val motivo = f.nuevoMotivo.trim().ifEmpty { "Sin clase" }
        return f.copy(dias = f.dias + (fecha to motivo), diasTocados = true, nuevoDia = "", nuevoMotivo = "") to null
    }

    fun validar(f: SemestreFormulario, hoy: LocalDate): ErroresSemestre {
        val i = inicio(f, hoy)
        val n = fin(f, hoy)
        return ErroresSemestre(
            nombre = if (f.nombre.isBlank()) "Ponle un nombre, por ejemplo 2026-2" else null,
            inicio = if (i == null) "Escribe la fecha como 3/8" else null,
            fin = when {
                n == null -> "Escribe la fecha como 28/11"
                i != null && !n.isAfter(i) -> "Termina antes de empezar"
                else -> null
            },
        )
    }
}
