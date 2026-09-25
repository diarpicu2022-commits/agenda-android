package com.dpinta.agenda.ui.components.banda

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
enum class ModoTransporte(val etiqueta: String, val icono: Icono) {
    Bus("en bus", Icono.Bus),
    APie("a pie", Icono.APie),
    Carro("en carro", Icono.Carro),
    Moto("en moto", Icono.Moto),
    ;

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
enum class AccionBanda(val etiqueta: String) {
    VoySaliendo("Voy saliendo"),
    MasCinco("+5 min"),
    YaLlegue("Ya llegué"),
}

/** Hora ya escrita: la cifra y, aparte, el sufijo de 12 h (C1.1). */
data class HoraFormateada(val cifra: String, val sufijo12h: String?) {
    /** Forma de una sola línea, para texto corrido («Cálculo empieza 8:00 a. m.»). */
    val enLinea: String get() = if (sufijo12h == null) cifra else "$cifra $sufijo12h"
}

/** C1.1: `7:32` / `19:32` en 24 h; `7:32` + `a. m.` / `p. m.` en 12 h. */
fun formatearHora(hora: LocalTime, es24h: Boolean): HoraFormateada {
    val minutos = hora.minute.toString().padStart(2, '0')
    if (es24h) return HoraFormateada("${hora.hour}:$minutos", null)
    val hora12 = if (hora.hour % 12 == 0) 12 else hora.hour % 12
    val sufijo = if (hora.hour < 12) "a. m." else "p. m."
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

private fun minutosHablados(n: Long): String = if (n == 1L) "1 minuto" else "$n minutos"

/** «25 min» · «27 h» · «3 días»: un dato de ayer no se cuenta en minutos. */
fun antiguedad(d: Duration): String = when {
    d < Duration.ofHours(1) -> "${d.toMinutes()} min"
    d < Duration.ofDays(2) -> "${d.toHours()} h"
    else -> "${d.toDays()} días"
}

private fun antiguedadHablada(d: Duration): String = when {
    d < Duration.ofHours(1) -> minutosHablados(d.toMinutes())
    d < Duration.ofDays(2) -> if (d.toHours() == 1L) "1 hora" else "${d.toHours()} horas"
    else -> "${d.toDays()} días"
}

/** Hora para TalkBack: «7 y 32», «8 en punto», con «de la mañana/tarde/noche» en 12 h. */
fun horaHablada(hora: LocalTime, es24h: Boolean): String {
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
fun textosBanda(m: BandaSalidaModelo, es24h: Boolean): TextosBanda {
    val salida = formatearHora(m.horaSalida, es24h)
    val inicio = formatearHora(m.horaInicio, es24h)
    val llegada = formatearHora(m.horaLlegada, es24h)

    val cifra = when (m.estado) {
        EstadoBanda.Espera, EstadoBanda.Preparate, EstadoBanda.SalYa -> salida
        EstadoBanda.VasTarde, EstadoBanda.SinTraslado -> inicio
        EstadoBanda.EnCamino -> llegada
    }
    val aprox = if (m.datoViejo && m.hayTrayecto) "aprox." else null
    val sufijo = listOfNotNull(cifra.sufijo12h, aprox).joinToString(" ").ifEmpty { null }

    val rotulo = when (m.estado) {
        EstadoBanda.Espera, EstadoBanda.Preparate -> "SAL A LAS"
        EstadoBanda.SalYa -> "SAL YA"
        EstadoBanda.VasTarde -> "VAS TARDE"
        EstadoBanda.EnCamino -> "LLEGAS"
        EstadoBanda.SinTraslado -> null
    }
    val cuentaAtras = if (m.estado == EstadoBanda.Preparate) "en ${m.minutosParaSalir} min" else null
    val lineaEstado = when (m.estado) {
        EstadoBanda.SalYa -> "${m.actividad} empieza ${inicio.enLinea}"
        EstadoBanda.VasTarde -> "llegas ${llegada.enLinea} a ${m.salon}"
        else -> null
    }

    val trayecto = when {
        !m.hayTrayecto -> null
        m.estado == EstadoBanda.EnCamino -> "${minutos(m.duracion)} min de trayecto"
        else -> "${minutos(m.duracion)} min + ${minutos(m.margen)} de margen"
    }
    val frescura = when {
        !m.hayTrayecto || m.manual -> null
        m.datoViejo -> "estimado hace ${antiguedad(m.calculadoHace)}"
        else -> "hace ${antiguedad(m.calculadoHace)}"
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
        modo = if (m.hayTrayecto) m.modo.etiqueta else null,
        trayecto = trayecto,
        frescura = frescura,
        primaria = primaria,
        secundaria = secundaria,
        fraseTalkBack = fraseTalkBack(m, es24h),
    )
}

/** C9.3: la banda se anuncia como una sola frase. */
fun fraseTalkBack(m: BandaSalidaModelo, es24h: Boolean): String {
    val lugarHablado = m.lugar.replace(" · ", ", ")
    val trayecto = "${minutosHablados(minutos(m.duracion))} ${m.modo.etiqueta} " +
        "más ${minutos(m.margen)} de margen"
    val calculo = when {
        m.manual -> ""
        m.datoViejo -> ", estimado hace ${antiguedadHablada(m.calculadoHace)}, hora aproximada"
        else -> ", calculado hace ${antiguedadHablada(m.calculadoHace)}"
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

/** Fondo y tinta de la banda por estado (C3, C3.1: el ámbar solo aquí). */
data class ColoresBanda(val fondo: Color, val frente: Color)

fun AgendaColors.coloresBanda(estado: EstadoBanda): ColoresBanda = when (estado) {
    EstadoBanda.VasTarde -> ColoresBanda(fondo = tarde, frente = sobreTarde)
    EstadoBanda.SinTraslado -> ColoresBanda(fondo = papel, frente = tinta)
    else -> ColoresBanda(fondo = senal, frente = sobreSenal)
}
