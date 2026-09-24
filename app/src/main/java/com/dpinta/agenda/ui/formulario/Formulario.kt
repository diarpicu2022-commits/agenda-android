package com.dpinta.agenda.ui.formulario

import androidx.compose.runtime.Immutable
import com.dpinta.agenda.data.agenda.ActividadAGuardar
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.Cuando
import com.dpinta.agenda.data.agenda.lugarLlamado
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.Conflict
import com.dpinta.agenda.domain.ConflictDetector
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.ScheduleExpander
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.WeeklyRule
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.hoy.ConflictoFila
import com.dpinta.agenda.ui.hoy.textoConflicto
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Formulario de actividad (anexo §7, flujo 1). Todo es puro: se prueba en la JVM.
 * Los campos guardan el TEXTO que escribe Diego; la validación lo interpreta.
 */

enum class Campo { TITULO, DIAS, INICIO, FIN, DESDE, HASTA, FECHA, MARGEN, AVISO }

/** Valores por defecto (decisión del paso 5, sujeta a Diego): margen 10 min, aviso 15 min. */
const val MARGEN_POR_DEFECTO = 10
const val AVISO_POR_DEFECTO = 15

/** Semestre por defecto si no hay uno definido: 17 semanas desde hoy (decisión del paso 5). */
const val SEMANAS_POR_DEFECTO = 17L

const val TITULO_MAXIMO = 60

@Immutable
data class Formulario(
    val tipo: ActivityKind = ActivityKind.CLASE,
    val titulo: String = "",
    val semanal: Boolean = true,
    val dias: Set<DayOfWeek> = emptySet(),
    val inicio: String = "",
    val fin: String = "",
    val desde: String = "",
    val hasta: String = "",
    val fecha: String = "",
    val lugar: String = "",
    val salon: String = "",
    val modo: TransportMode = TransportMode.TRANSPORTE_PUBLICO,
    val margen: String = MARGEN_POR_DEFECTO.toString(),
    val aviso: String = AVISO_POR_DEFECTO.toString(),
) {
    companion object {
        /** Nueva actividad: fechas del semestre (o 17 semanas) y la fecha de hoy ya escritas. */
        fun nuevo(hoy: LocalDate, finSemestre: LocalDate? = null) = Formulario(
            desde = escribirFecha(hoy),
            hasta = escribirFecha(finSemestre ?: hoy.plusWeeks(SEMANAS_POR_DEFECTO)),
            fecha = escribirFecha(hoy),
        )
    }
}

private val ESPANOL: Locale = Locale.forLanguageTag("es")
private val FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM", ESPANOL)
private val FECHA_LARGA_ANIO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ESPANOL)
private val DIA_Y_FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ESPANOL)

/** Así se escriben las fechas en los campos: 24/9/2026. */
fun escribirFecha(fecha: LocalDate): String = "${fecha.dayOfMonth}/${fecha.monthValue}/${fecha.year}"

/** Así se escriben las horas en los campos: 8:00, 19:30 (24 h; también se aceptan a. m./p. m.). */
fun escribirHora(hora: LocalTime): String = "${hora.hour}:${hora.minute.toString().padStart(2, '0')}"

object Interprete {

    private val SUFIJO_12H = Regex("""\s*([ap])\.?\s*m\.?\s*$""", RegexOption.IGNORE_CASE)

    /** «8», «8:00», «08:00», «800», «0800», «20.30», «8:00 p. m.», «8pm». */
    fun hora(texto: String): LocalTime? {
        var t = texto.trim()
        if (t.isEmpty()) return null
        val sufijo = SUFIJO_12H.find(t)?.groupValues?.get(1)?.lowercase()
        if (sufijo != null) t = t.substring(0, SUFIJO_12H.find(t)!!.range.first).trim()
        val (h, m) = when {
            Regex("""^\d{1,2}$""").matches(t) -> t.toInt() to 0
            Regex("""^\d{1,2}[:.h]\d{2}$""").matches(t) -> t.split(':', '.', 'h').let { it[0].toInt() to it[1].toInt() }
            Regex("""^\d{3,4}$""").matches(t) -> t.dropLast(2).toInt() to t.takeLast(2).toInt()
            else -> return null
        }
        if (m !in 0..59) return null
        val hora24 = when (sufijo) {
            null -> h
            "a" -> if (h !in 1..12) return null else h % 12
            else -> if (h !in 1..12) return null else h % 12 + 12
        }
        if (hora24 !in 0..23) return null
        return LocalTime.of(hora24, m)
    }

    /** «24/9/2026», «24-9-2026», «24/9» (este año), «24/9/26». */
    fun fecha(texto: String, hoy: LocalDate): LocalDate? {
        val partes = texto.trim().split('/', '-', '.').filter { it.isNotBlank() }
        if (partes.size !in 2..3 || partes.any { p -> p.any { !it.isDigit() } }) return null
        val d = partes[0].toInt()
        val m = partes[1].toInt()
        val a = partes.getOrNull(2)?.toInt()?.let { if (it < 100) 2000 + it else it } ?: hoy.year
        return runCatching { LocalDate.of(a, m, d) }.getOrNull()
    }

    fun minutos(texto: String, rango: IntRange): Int? =
        texto.trim().takeIf { t -> t.isNotEmpty() && t.all { it.isDigit() } && t.length <= 4 }?.toInt()?.takeIf { it in rango }
}

/** Resultado de validar: errores por campo (texto literal) o lo que se guardaría. */
data class Validacion(val errores: Map<Campo, String>, val aGuardar: ActividadAGuardar?) {
    val valida: Boolean get() = errores.isEmpty()
}

object Validador {

    const val MSG_TITULO = "Escribe un nombre para la actividad."
    const val MSG_TITULO_LARGO = "Máximo $TITULO_MAXIMO caracteres."
    const val MSG_DIAS = "Elige al menos un día."
    const val MSG_HORA = "Escribe la hora así: 8:00."
    const val MSG_FIN_ANTES = "Tiene que terminar después de empezar."
    const val MSG_FECHA = "Escribe la fecha así: 24/9/2026."
    const val MSG_HASTA_ANTES = "La serie tiene que terminar después de empezar."
    const val MSG_MARGEN = "El margen va de 0 a 120 minutos."
    const val MSG_AVISO = "El aviso va de 0 a 240 minutos."

    fun validar(f: Formulario, hoy: LocalDate, id: Long? = null): Validacion {
        val e = linkedMapOf<Campo, String>()
        val titulo = f.titulo.trim()
        when {
            titulo.isEmpty() -> e[Campo.TITULO] = MSG_TITULO
            titulo.length > TITULO_MAXIMO -> e[Campo.TITULO] = MSG_TITULO_LARGO
        }
        val inicio = Interprete.hora(f.inicio)
        val fin = Interprete.hora(f.fin)
        if (inicio == null) e[Campo.INICIO] = MSG_HORA
        if (fin == null) e[Campo.FIN] = MSG_HORA
        if (inicio != null && fin != null && !fin.isAfter(inicio)) e[Campo.FIN] = MSG_FIN_ANTES

        var cuando: Cuando? = null
        if (f.semanal) {
            if (f.dias.isEmpty()) e[Campo.DIAS] = MSG_DIAS
            val desde = Interprete.fecha(f.desde, hoy)
            val hasta = Interprete.fecha(f.hasta, hoy)
            if (desde == null) e[Campo.DESDE] = MSG_FECHA
            if (hasta == null) e[Campo.HASTA] = MSG_FECHA
            if (desde != null && hasta != null && hasta.isBefore(desde)) e[Campo.HASTA] = MSG_HASTA_ANTES
            if (e.isEmpty()) cuando = Cuando.Semanal(f.dias, inicio!!, fin!!, desde!!, hasta!!)
        } else {
            val fecha = Interprete.fecha(f.fecha, hoy)
            if (fecha == null) e[Campo.FECHA] = MSG_FECHA
            if (e.isEmpty()) cuando = Cuando.Puntual(fecha!!, inicio!!, fin!!)
        }
        val margen = Interprete.minutos(f.margen, 0..120)
        val aviso = Interprete.minutos(f.aviso, 0..240)
        if (margen == null) e[Campo.MARGEN] = MSG_MARGEN
        if (aviso == null) e[Campo.AVISO] = MSG_AVISO

        val aGuardar = if (e.isEmpty() && cuando != null) {
            ActividadAGuardar(
                id = id,
                titulo = titulo,
                tipo = f.tipo,
                cuando = cuando,
                lugar = f.lugar.trim(),
                salon = f.salon.trim(),
                modo = f.modo,
                margen = Duration.ofMinutes(margen!!.toLong()),
                aviso = Duration.ofMinutes(aviso!!.toLong()),
            )
        } else {
            null
        }
        return Validacion(e, aGuardar)
    }
}

private val NOMBRE_DIA = mapOf(
    DayOfWeek.MONDAY to "lunes", DayOfWeek.TUESDAY to "martes", DayOfWeek.WEDNESDAY to "miércoles",
    DayOfWeek.THURSDAY to "jueves", DayOfWeek.FRIDAY to "viernes", DayOfWeek.SATURDAY to "sábado",
    DayOfWeek.SUNDAY to "domingo",
)

fun nombreDia(d: DayOfWeek): String = NOMBRE_DIA.getValue(d)

private fun enumerar(partes: List<String>): String = when (partes.size) {
    0 -> ""
    1 -> partes[0]
    else -> partes.dropLast(1).joinToString(", ") + " y " + partes.last()
}

private fun dias(d: Set<DayOfWeek>): String {
    val laborables = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    return when (d) {
        DayOfWeek.entries.toSet() -> "todos los días"
        laborables -> "de lunes a viernes"
        else -> "cada " + enumerar(d.sorted().map { nombreDia(it) })
    }
}

/**
 * Frase-resumen al pie (anexo §7, paso 6 del flujo): lo que Diego comprueba antes de guardar.
 * «Cálculo diferencial: cada lunes y miércoles, de 8:00 a 10:00, en B-204 · Campus, del 24 de
 * septiembre al 20 de enero de 2027.» Solo con un borrador ya válido.
 */
fun resumen(a: ActividadAGuardar, es24h: Boolean): String {
    val donde = listOf(a.salon, a.lugar).filter { it.isNotBlank() }.joinToString(" · ").let { if (it.isEmpty()) "" else ", en $it" }
    val frase = when (val c = a.cuando) {
        is Cuando.Semanal -> {
            val hasta = if (c.hasta.year != c.desde.year) FECHA_LARGA_ANIO else FECHA_LARGA
            "${a.titulo}: ${dias(c.dias)}, de ${formatearHora(c.inicio, es24h).enLinea} a ${formatearHora(c.fin, es24h).enLinea}$donde, " +
                "del ${FECHA_LARGA.format(c.desde)} al ${hasta.format(c.hasta)}"
        }
        is Cuando.Puntual ->
            "${a.titulo}: el ${DIA_Y_FECHA.format(c.fecha)}, de ${formatearHora(c.inicio, es24h).enLinea} a ${formatearHora(c.fin, es24h).enLinea}$donde"
    }
    // Si ya acaba en punto (p. ej. «a. m.»), no se añade otro.
    return if (frase.endsWith(".")) frase else "$frase."
}

/** Un conflicto del borrador con otra actividad, un día concreto. */
data class AvisoConflicto(val fecha: LocalDate, val con: String, val conflicto: ConflictoFila)

/** Texto de C10 con el día delante: «lunes: Se cruza con Física 9:00». */
fun textoAviso(a: AvisoConflicto, es24h: Boolean): String = when (a.conflicto) {
    is ConflictoFila.Cruce -> "${nombreDia(a.fecha.dayOfWeek)}: ${textoConflicto(a.conflicto, es24h)}"
    is ConflictoFila.Traslado -> "${nombreDia(a.fecha.dayOfWeek)}, con ${a.con}: ${textoConflicto(a.conflicto, es24h)}"
}

/** Id provisional del borrador mientras no existe. */
private const val BORRADOR = -1L

/** Id provisional de un lugar nuevo: no hay tiempos de traslado, no se afirma ningún conflicto de traslado. */
private const val LUGAR_NUEVO = -2L

/**
 * Conflictos del borrador con lo ya guardado (C10), con el ConflictDetector del dominio.
 * Se mira la primera semana de la serie (el patrón se repite) o el día de la puntual.
 */
fun conflictosDelBorrador(agenda: Agenda, a: ActividadAGuardar, minutosEntre: (Long, Long) -> Long?): List<AvisoConflicto> {
    val id = a.id ?: BORRADOR
    val lugarId = agenda.lugarLlamado(a.lugar)?.id ?: (if (a.lugar.isNotBlank()) LUGAR_NUEVO else null)
    val (desde, hasta, propia) = when (val c = a.cuando) {
        is Cuando.Semanal -> Triple(c.desde, minOf(c.hasta, c.desde.plusDays(6)), WeeklyRule(id, c.dias, c.inicio, c.fin, c.desde, c.hasta))
        is Cuando.Puntual -> Triple(c.fecha, c.fecha, null)
    }
    val puntualPropia = (a.cuando as? Cuando.Puntual)?.let { Occurrence(id, lugarId, it.fecha.atTime(it.inicio), it.fecha.atTime(it.fin)) }
    val sesiones = ScheduleExpander.expand(
        rules = agenda.reglas.filterNot { it.activityId == id } + listOfNotNull(propia),
        oneOff = agenda.puntuales.filterNot { it.activityId == id } + listOfNotNull(puntualPropia),
        placeOf = { if (it == id) lugarId else agenda.actividades[it]?.lugarId },
        from = desde,
        to = hasta,
    )
    fun titulo(o: Occurrence) = agenda.actividades[o.activityId]?.titulo.orEmpty()
    return ConflictDetector.detect(sesiones) { x, y -> if (x == LUGAR_NUEVO || y == LUGAR_NUEVO) null else minutosEntre(x, y) }
        .mapNotNull { c ->
            when (c) {
                is Conflict.Overlap -> {
                    val otra = if (c.first.activityId == id) c.second else if (c.second.activityId == id) c.first else return@mapNotNull null
                    AvisoConflicto(otra.start.toLocalDate(), titulo(otra), ConflictoFila.Cruce(titulo(otra), otra.start.toLocalTime()))
                }
                is Conflict.NotEnoughTravel -> {
                    val otra = if (c.from.activityId == id) c.to else if (c.to.activityId == id) c.from else return@mapNotNull null
                    AvisoConflicto(otra.start.toLocalDate(), titulo(otra), ConflictoFila.Traslado(c.availableMinutes, c.neededMinutes))
                }
            }
        }
}

/** Formulario a partir de una actividad guardada (editar). */
fun formularioDe(agenda: Agenda, id: Long, hoy: LocalDate): Formulario? {
    val act = agenda.actividades[id] ?: return null
    val regla = agenda.reglas.firstOrNull { it.activityId == id }
    val puntual = agenda.puntuales.firstOrNull { it.activityId == id }
    val base = Formulario.nuevo(hoy, agenda.semestre?.end).copy(
        tipo = act.tipo,
        titulo = act.titulo,
        lugar = act.lugarId?.let { agenda.lugares[it]?.nombre }.orEmpty(),
        salon = act.salon,
        modo = act.modo,
        margen = act.margen.toMinutes().toString(),
        aviso = act.aviso.toMinutes().toString(),
    )
    return when {
        regla != null -> base.copy(
            semanal = true,
            dias = regla.days,
            inicio = escribirHora(regla.start),
            fin = escribirHora(regla.end),
            desde = escribirFecha(regla.from),
            hasta = escribirFecha(regla.until),
        )
        puntual != null -> base.copy(
            semanal = false,
            fecha = escribirFecha(puntual.start.toLocalDate()),
            inicio = escribirHora(puntual.start.toLocalTime()),
            fin = escribirHora(puntual.end.toLocalTime()),
        )
        else -> base
    }
}
