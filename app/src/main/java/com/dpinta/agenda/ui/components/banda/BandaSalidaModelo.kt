package com.dpinta.agenda.ui.components.banda

import com.dpinta.agenda.ui.components.Lengua
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.dpinta.agenda.ui.theme.AgendaColors
import com.dpinta.agenda.ui.theme.Icono
import java.time.Duration
import java.time.LocalTime

/*
 * Banda de salida: modelo inmutable y mapeo puro a textos, acciones y colores.
 * Sin dependencias de Android: se prueba en la JVM (app/src/test).
 * Contrato de diseño: C1 (reloj, mano, transporte), C3 (color), C9.3 (TalkBack), C10 (estados).
 */

/** Estados de C10 que se dibujan en la banda. El dato viejo es un atributo, no un estado. */
enum class EstadoBanda { Espera, Preparate, SalYa, VasTarde, EnCamino, SinTraslado }

/** Los cuatro modos de C1.3, y solo cuatro. */
enum class ModoTransporte(val etiqueta: String, val etiquetaEn: String, val icono: Icono) {
    Bus("en bus", "by bus", Icono.Bus),
    APie("a pie", "on foot", Icono.APie),
    Carro("en carro", "by car", Icono.Carro),
    Moto("en moto", "by motorbike", Icono.Moto),
    ;

    fun etiqueta(l: Lengua): String = l(etiqueta, etiquetaEn)

    /** Orden al tocar la etiqueta (C1.3). */
    fun siguiente(): ModoTransporte = entries[(ordinal + 1) % entries.size]
}

/**
 * Lo que la banda necesita saber. Lo calcula quien la usa (state hoisting): los tiempos
 * (salida, llegada, minutos) vienen YA calculados del dominio (DepartureCalculator, Trip);
 * la banda no hace aritmética de tiempos.
 *
 * @param horaSalida hora a la que hay que moverse.
 * @param horaInicio hora a la que empieza la actividad.
 * @param horaLlegada llegada estimada (si sales ahora o, en camino, la del trayecto iniciado).
 * @param minutosParaSalir cuenta atrás para «prepárate».
 * @param duracion trayecto estimado.
 * @param calculadoHace antigüedad de la estimación del trayecto.
 * @param datoViejo la estimación se marcó vieja en el dominio; la hora se marca «aprox.».
 */
@Immutable
data class BandaSalidaModelo(
    val estado: EstadoBanda,
    val horaSalida: LocalTime,
    val horaInicio: LocalTime,
    val horaLlegada: LocalTime,
    val minutosParaSalir: Long,
    val actividad: String,
    val salon: String,
    val lugar: String,
    val modo: ModoTransporte,
    val duracion: Duration,
    val margen: Duration,
    val calculadoHace: Duration,
    val datoViejo: Boolean,
    /** Tiempo escrito por Diego: no es un cálculo, así que no lleva «hace…» (ni en pantalla ni en TalkBack). */
    val manual: Boolean = false,
) {
    /** Estados en los que hay trayecto (todos menos «sin traslado»). */
    val hayTrayecto: Boolean get() = estado != EstadoBanda.SinTraslado
}

/** Acciones de la banda. La primaria va a la derecha, la secundaria a la izquierda (C1.2). */
enum class AccionBanda(val etiqueta: String, val etiquetaEn: String) {
    VoySaliendo("Voy saliendo", "Leaving now"),
    MasCinco("+5 min", "+5 min"),
    YaLlegue("Ya llegué", "I'm here"),
    ;

    fun etiqueta(l: Lengua): String = l(etiqueta, etiquetaEn)
}

/** Hora ya escrita: la cifra y, aparte, el sufijo de 12 h (C1.1). */
data class HoraFormateada(val cifra: String, val sufijo12h: String?) {
    /** Forma de una sola línea, para texto corrido («Cálculo empieza 8:00 a. m.»). */
    val enLinea: String get() = if (sufijo12h == null) cifra else "$cifra $sufijo12h"
}

/** C1.1: `7:32` / `19:32` en 24 h; `7:32` + `a. m.` / `p. m.` en 12 h (`AM` / `PM` en inglés). */
fun formatearHora(hora: LocalTime, es24h: Boolean, l: Lengua = Lengua.ES): HoraFormateada {
    val minutos = hora.minute.toString().padStart(2, '0')
    if (es24h) return HoraFormateada("${hora.hour}:$minutos", null)
    val hora12 = if (hora.hour % 12 == 0) 12 else hora.hour % 12
    val sufijo = if (hora.hour < 12) l("a. m.", "AM") else l("p. m.", "PM")
    return HoraFormateada("$hora12:$minutos", sufijo)
}

/** Todo el texto visible y hablado de la banda para un modelo. */
data class TextosBanda(
    /** «SAL A LAS», «SAL YA», «VAS TARDE», «LLEGAS». Nulo sin traslado (C10). */
    val rotulo: String?,
    /** Cuenta atrás en el renglón del rótulo; solo en «prepárate» (C10). */
    val cuentaAtras: String?,
    /** La cifra grande. */
    val cifra: String,
    /** Línea bajo la cifra: sufijo de 12 h y/o «aprox.» (C1.1, C10 dato viejo). */
    val sufijo: String?,
    /** Frase de estado bajo la cifra: «Cálculo empieza 8:00», «llegas 8:04 a B-204». */
    val lineaEstado: String?,
    val actividad: String,
    val salon: String,
    val lugar: String,
    /** Etiqueta tocable del modo (C1.3). Nula sin traslado. */
    val modo: String?,
    /** «23 min + 7 de margen». */
    val trayecto: String?,
    /** «hace 3 min» o «estimado hace 25 min» (C10). */
    val frescura: String?,
    val primaria: AccionBanda?,
    val secundaria: AccionBanda?,
    /** Frase única de TalkBack (C9.3). */
    val fraseTalkBack: String,
)

private fun minutos(d: Duration): Long = d.toMinutes()

fun minutosHablados(n: Long, l: Lengua = Lengua.ES): String =
    if (n == 1L) l("1 minuto", "1 minute") else l("$n minutos", "$n minutes")

/** «25 min» · «27 h» · «3 días»: un dato de ayer no se cuenta en minutos. */
fun antiguedad(d: Duration, l: Lengua = Lengua.ES): String = when {
    d < Duration.ofHours(1) -> "${d.toMinutes()} min"
    d < Duration.ofDays(2) -> "${d.toHours()} h"
    else -> l("${d.toDays()} días", "${d.toDays()} days")
}

private fun antiguedadHablada(d: Duration, l: Lengua): String = when {
    d < Duration.ofHours(1) -> minutosHablados(d.toMinutes(), l)
    d < Duration.ofDays(2) -> if (d.toHours() == 1L) l("1 hora", "1 hour") else l("${d.toHours()} horas", "${d.toHours()} hours")
    else -> l("${d.toDays()} días", "${d.toDays()} days")
}

/**
 * Hora para TalkBack: «7 y 32», «8 en punto», con «de la mañana/tarde/noche» en 12 h. En inglés, «7:32 PM» y
 * «8 o'clock», que TalkBack ya lee bien.
 */
fun horaHablada(hora: LocalTime, es24h: Boolean, l: Lengua = Lengua.ES): String {
    if (l == Lengua.EN) {
        if (es24h) return if (hora.minute == 0) "${hora.hour} o'clock" else "${hora.hour}:${hora.minute.toString().padStart(2, '0')}"
        return formatearHora(hora, false, l).enLinea
    }
    val h = if (es24h) hora.hour else (if (hora.hour % 12 == 0) 12 else hora.hour % 12)
    val base = if (hora.minute == 0) "$h en punto" else "$h y ${hora.minute}"
    if (es24h) return base
    val franja = when (hora.hour) {
        in 0..11 -> "de la mañana"
        in 12..18 -> "de la tarde"
        else -> "de la noche"
    }
    return "$base $franja"
}

/** Mapeo puro modelo → textos (C10, texto literal del contrato). */
fun textosBanda(m: BandaSalidaModelo, es24h: Boolean, l: Lengua = Lengua.ES): TextosBanda {
    val salida = formatearHora(m.horaSalida, es24h, l)
    val inicio = formatearHora(m.horaInicio, es24h, l)
    val llegada = formatearHora(m.horaLlegada, es24h, l)

    val cifra = when (m.estado) {
        EstadoBanda.Espera, EstadoBanda.Preparate, EstadoBanda.SalYa -> salida
        EstadoBanda.VasTarde, EstadoBanda.SinTraslado -> inicio
        EstadoBanda.EnCamino -> llegada
    }
    val aprox = if (m.datoViejo && m.hayTrayecto) l("aprox.", "approx.") else null
    val sufijo = listOfNotNull(cifra.sufijo12h, aprox).joinToString(" ").ifEmpty { null }

    val rotulo = when (m.estado) {
        EstadoBanda.Espera, EstadoBanda.Preparate -> l("SAL A LAS", "LEAVE AT")
        EstadoBanda.SalYa -> l("SAL YA", "LEAVE NOW")
        EstadoBanda.VasTarde -> l("VAS TARDE", "RUNNING LATE")
        EstadoBanda.EnCamino -> l("LLEGAS", "ARRIVING")
        EstadoBanda.SinTraslado -> null
    }
    val cuentaAtras = if (m.estado == EstadoBanda.Preparate) l("en ${m.minutosParaSalir} min", "in ${m.minutosParaSalir} min") else null
    val lineaEstado = when (m.estado) {
        EstadoBanda.SalYa -> l("${m.actividad} empieza ${inicio.enLinea}", "${m.actividad} starts ${inicio.enLinea}")
        EstadoBanda.VasTarde -> l("llegas ${llegada.enLinea} a ${m.salon}", "you arrive ${llegada.enLinea} at ${m.salon}")
        else -> null
    }

    val trayecto = when {
        !m.hayTrayecto -> null
        m.estado == EstadoBanda.EnCamino -> l("${minutos(m.duracion)} min de trayecto", "${minutos(m.duracion)} min of travel")
        else -> l("${minutos(m.duracion)} min + ${minutos(m.margen)} de margen", "${minutos(m.duracion)} min + ${minutos(m.margen)} min buffer")
    }
    val frescura = when {
        !m.hayTrayecto || m.manual -> null
        m.datoViejo -> l("estimado hace ${antiguedad(m.calculadoHace, l)}", "estimated ${antiguedad(m.calculadoHace, l)} ago")
        else -> l("hace ${antiguedad(m.calculadoHace, l)}", "${antiguedad(m.calculadoHace, l)} ago")
    }

    val (primaria, secundaria) = when (m.estado) {
        EstadoBanda.Espera, EstadoBanda.Preparate, EstadoBanda.SalYa ->
            AccionBanda.VoySaliendo to AccionBanda.MasCinco
        EstadoBanda.VasTarde -> AccionBanda.VoySaliendo to null
        EstadoBanda.EnCamino -> AccionBanda.YaLlegue to null
        EstadoBanda.SinTraslado -> null to null
    }

    return TextosBanda(
        rotulo = rotulo,
        cuentaAtras = cuentaAtras,
        cifra = cifra.cifra,
        sufijo = sufijo,
        lineaEstado = lineaEstado,
        actividad = m.actividad,
        salon = m.salon,
        lugar = m.lugar,
        modo = if (m.hayTrayecto) m.modo.etiqueta(l) else null,
        trayecto = trayecto,
        frescura = frescura,
        primaria = primaria,
        secundaria = secundaria,
        fraseTalkBack = fraseTalkBack(m, es24h, l),
    )
}

/** C9.3: la banda se anuncia como una sola frase. */
fun fraseTalkBack(m: BandaSalidaModelo, es24h: Boolean, l: Lengua = Lengua.ES): String {
    if (l == Lengua.EN) return fraseTalkBackEn(m, es24h)
    val lugarHablado = m.lugar.replace(" · ", ", ")
    val trayecto = "${minutosHablados(minutos(m.duracion))} ${m.modo.etiqueta} " +
        "más ${minutos(m.margen)} de margen"
    val calculo = when {
        m.manual -> ""
        m.datoViejo -> ", estimado hace ${antiguedadHablada(m.calculadoHace, l)}, hora aproximada"
        else -> ", calculado hace ${antiguedadHablada(m.calculadoHace, l)}"
    }
    val salida = horaHablada(m.horaSalida, es24h)
    val inicio = horaHablada(m.horaInicio, es24h)
    val llegada = horaHablada(m.horaLlegada, es24h)
    return when (m.estado) {
        EstadoBanda.Espera ->
            "Sal a las $salida para ${m.actividad}, salón ${m.salon}, $trayecto$calculo"
        EstadoBanda.Preparate ->
            "Sal a las $salida, en ${minutosHablados(m.minutosParaSalir)}, para ${m.actividad}, " +
                "salón ${m.salon}, $trayecto$calculo"
        EstadoBanda.SalYa ->
            "Sal ya. ${m.actividad} empieza a las $inicio, salón ${m.salon}, $trayecto$calculo"
        EstadoBanda.VasTarde ->
            "Vas tarde. ${m.actividad} empieza a las $inicio, llegas a las $llegada " +
                "al salón ${m.salon}$calculo"
        EstadoBanda.EnCamino ->
            "Llegas a las $llegada al salón ${m.salon}, ${m.actividad}, " +
                "${minutosHablados(minutos(m.duracion))} de trayecto ${m.modo.etiqueta}$calculo"
        EstadoBanda.SinTraslado ->
            "${m.actividad} a las $inicio, salón ${m.salon}, $lugarHablado"
    }
}

/** La misma frase en inglés (el orden de las partes cambia, por eso va aparte y no pieza por pieza). */
private fun fraseTalkBackEn(m: BandaSalidaModelo, es24h: Boolean): String {
    val l = Lengua.EN
    val lugarHablado = m.lugar.replace(" · ", ", ")
    val trayecto = "${minutosHablados(minutos(m.duracion), l)} ${m.modo.etiquetaEn} " +
        "plus ${minutosHablados(minutos(m.margen), l)} buffer"
    val calculo = when {
        m.manual -> ""
        m.datoViejo -> ", estimated ${antiguedadHablada(m.calculadoHace, l)} ago, approximate time"
        else -> ", calculated ${antiguedadHablada(m.calculadoHace, l)} ago"
    }
    val salida = horaHablada(m.horaSalida, es24h, l)
    val inicio = horaHablada(m.horaInicio, es24h, l)
    val llegada = horaHablada(m.horaLlegada, es24h, l)
    return when (m.estado) {
        EstadoBanda.Espera -> "Leave at $salida for ${m.actividad}, room ${m.salon}, $trayecto$calculo"
        EstadoBanda.Preparate ->
            "Leave at $salida, in ${minutosHablados(m.minutosParaSalir, l)}, for ${m.actividad}, room ${m.salon}, $trayecto$calculo"
        EstadoBanda.SalYa -> "Leave now. ${m.actividad} starts at $inicio, room ${m.salon}, $trayecto$calculo"
        EstadoBanda.VasTarde ->
            "Running late. ${m.actividad} starts at $inicio, you arrive at $llegada at room ${m.salon}$calculo"
        EstadoBanda.EnCamino ->
            "Arriving at $llegada at room ${m.salon}, ${m.actividad}, " +
                "${minutosHablados(minutos(m.duracion), l)} of travel ${m.modo.etiquetaEn}$calculo"
        EstadoBanda.SinTraslado -> "${m.actividad} at $inicio, room ${m.salon}, $lugarHablado"
    }
}

/** Fondo y tinta de la banda por estado (C3, C3.1: el ámbar solo aquí). */
data class ColoresBanda(val fondo: Color, val frente: Color)

fun AgendaColors.coloresBanda(estado: EstadoBanda): ColoresBanda = when (estado) {
    EstadoBanda.VasTarde -> ColoresBanda(fondo = tarde, frente = sobreTarde)
    EstadoBanda.SinTraslado -> ColoresBanda(fondo = papel, frente = tinta)
    else -> ColoresBanda(fondo = senal, frente = sobreSenal)
}
