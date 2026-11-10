package com.dpinta.agenda.wear

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpinta.agenda.domain.Urgency
import com.dpinta.agenda.domain.WatchSession
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle as EstiloFecha
import java.util.Locale

/** Tema del reloj en pantalla activa (anexo 2026-11-09-campuswatch-claro, W1): OSCURO es el tema «reloj» de siempre
 *  (03-campuswatch.md · «Esfera», casi negro con tinte ciruela); CLARO, el tema «claro» de la app. «Siempre activo», la
 *  Tarjeta y la Complicación no pasan por aquí: usan CW (W3). */
enum class TemaReloj { CLARO, OSCURO }

object Reloj {
    /** Tema que se está mostrando. Es un estado de Compose: al cambiarlo se redibuja todo lo que lee estos colores. */
    var tema by mutableStateOf(TemaReloj.OSCURO)
    private fun c(oscuro: Long, claro: Long) = Color(if (tema == TemaReloj.CLARO) claro else oscuro)
    val fondo: Color get() = c(CW.fondo, CWClaro.fondo)
    val superficie: Color get() = c(CW.superficie, CWClaro.superficie)
    val superficieFuerte: Color get() = c(CW.superficieFuerte, CWClaro.superficieFuerte)
    val linea: Color get() = c(CW.linea, CWClaro.linea)
    val tinta: Color get() = c(CW.tinta, CWClaro.tinta)
    val tintaSuave: Color get() = c(CW.tintaSuave, CWClaro.tintaSuave)
    val ruta: Color get() = c(CW.ruta, CWClaro.ruta)
    val hora: Color get() = c(CW.hora, CWClaro.hora)
    val horaSuave: Color get() = c(CW.horaSuave, CWClaro.horaSuave)
    val sobreHora: Color get() = c(CW.sobreHora, CWClaro.sobreHora)
    val ciruela: Color get() = c(CW.ciruela, CWClaro.ciruela)
    val exito: Color get() = c(CW.exito, CWClaro.exito)
    val critico: Color get() = c(CW.critico, CWClaro.critico)
    val aviso: Color get() = c(CW.aviso, CWClaro.aviso)
    val trabajo: Color get() = c(CW.lineaTrabajo, CWClaro.lineaTrabajo)
    val personal: Color get() = c(CW.lineaPersonal, CWClaro.lineaPersonal)
    val entrega: Color get() = c(CW.lineaEntrega, CWClaro.lineaEntrega)
    /** Aula y hora bajo el nombre: en claro va en tinta para llegar a AAA (decisión de Diego, paso 2 del anexo 2026-11-09). */
    val aula: Color get() = if (tema == TemaReloj.CLARO) tinta else tintaSuave
    /** Hora de la fila actual del horario: en claro va en tinta (13,24:1); `hora` daba 4,99:1 (decisión del paso 1). */
    val horaFila: Color get() = if (tema == TemaReloj.CLARO) tinta else hora

    /** El color sigue el nivel de urgencia; un parcial usa ciruela hasta llegar a «Salir ahora». */
    fun urgencia(u: Urgency, examen: Boolean = false): Color = when {
        examen && u < Urgency.LEAVE_NOW && u != Urgency.MISSED -> ciruela
        else -> when (u) {
            Urgency.CALM -> c(CW.urgCalma, CWClaro.urgCalma)
            Urgency.UPCOMING -> c(CW.urgProxima, CWClaro.urgProxima)
            Urgency.SOON -> c(CW.urgPronto, CWClaro.urgPronto)
            Urgency.LEAVE_NOW -> c(CW.urgSalir, CWClaro.urgSalir)
            Urgency.URGENT -> c(CW.urgUrgente, CWClaro.urgUrgente)
            Urgency.MISSED -> c(CW.urgPerdida, CWClaro.urgPerdida)
        }
    }

    fun tipo(k: WatchSession.Kind): Color = when (k) {
        WatchSession.Kind.CLASE -> ruta
        WatchSession.Kind.TRABAJO -> trabajo
        WatchSession.Kind.PERSONAL -> personal
        WatchSession.Kind.ENTREGA -> entrega
    }

    /** Alto de toque del reloj (size-toque-reloj). */
    val toque = 48.dp
}

/** Familia «reloj»: Sofia Sans Condensed variable, con los pesos exactos del sistema (650 no existe como estático). */
private fun sofia(peso: Int) = Font(R.font.sofia_sans_condensed, FontWeight(peso), variationSettings = FontVariation.Settings(FontVariation.weight(peso)))

val FamiliaReloj = FontFamily(sofia(500), sofia(650), sofia(700))

object Tipo {
    /** reloj-hora 44/44 · 700: la cuenta regresiva, lo más grande de la esfera. */
    val hora = TextStyle(fontFamily = FamiliaReloj, fontWeight = FontWeight(700), fontSize = 44.sp, lineHeight = 44.sp, textAlign = TextAlign.Center,
        fontFeatureSettings = "tnum")
    /** reloj-titulo 20/22 · 650: nombre de la actividad, una línea. */
    val titulo = TextStyle(fontFamily = FamiliaReloj, fontWeight = FontWeight(650), fontSize = 20.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
    /** reloj-apoyo 15/18 · 500: aula, lugar y rótulos; nunca menos de 15 px. */
    val apoyo = TextStyle(fontFamily = FamiliaReloj, fontWeight = FontWeight(500), fontSize = 15.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
        fontFeatureSettings = "tnum")
    /** Rótulo en mayúsculas (ES HORA DE SALIR, PARCIAL): apoyo en 700 con tracking. */
    val rotulo = apoyo.copy(fontWeight = FontWeight(700), letterSpacing = 0.06.sp)
}

object Formato {
    private val ES = Locale.forLanguageTag("es-CO")

    fun hora(t: LocalTime): String = "%02d:%02d".format(t.hour, t.minute)

    /** «18 min», «1 h 20 min»; nunca negativo. */
    fun cuenta(d: Duration): String {
        val m = d.toMinutes().coerceAtLeast(0)
        return if (m < 60) "$m min" else if (m % 60 == 0L) "${m / 60} h" else "${m / 60} h ${m % 60} min"
    }

    /** «Hoy · mié 7», «Mañana · jue 8». */
    fun dia(fecha: LocalDate, hoy: LocalDate): String {
        val nombre = fecha.dayOfWeek.getDisplayName(EstiloFecha.SHORT, ES).trimEnd('.')
        val cual = if (fecha == hoy) "Hoy" else if (fecha == hoy.plusDays(1)) "Mañana" else nombre.replaceFirstChar { it.uppercase() }
        return "$cual · $nombre ${fecha.dayOfMonth}"
    }
}

/** Movimiento reducido: si el sistema quitó las animaciones, todo corta sin transición (06-movimiento.md). */
fun sinMovimiento(contexto: Context): Boolean =
    Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
