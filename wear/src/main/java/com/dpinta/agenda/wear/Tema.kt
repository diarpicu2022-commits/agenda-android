package com.dpinta.agenda.wear

import android.content.Context
import android.provider.Settings
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

/** Tema «reloj» (03-campuswatch.md · «Esfera»): fondo casi negro con tinte ciruela, superficie solo detrás de botones. */
object Reloj {
    val fondo = Color(CW.fondo)
    val superficie = Color(CW.superficie)
    val superficieFuerte = Color(CW.superficieFuerte)
    val linea = Color(CW.linea)
    val tinta = Color(CW.tinta)
    val tintaSuave = Color(CW.tintaSuave)
    val ruta = Color(CW.ruta)
    val hora = Color(CW.hora)
    val horaSuave = Color(CW.horaSuave)
    val sobreHora = Color(CW.sobreHora)
    val ciruela = Color(CW.ciruela)
    val exito = Color(CW.exito)
    val critico = Color(CW.critico)
    val trabajo = Color(CW.lineaTrabajo)
    val personal = Color(CW.lineaPersonal)
    val entrega = Color(CW.lineaEntrega)

    /** El color sigue el nivel de urgencia; un parcial usa ciruela hasta llegar a «Salir ahora». */
    fun urgencia(u: Urgency, examen: Boolean = false): Color = when {
        examen && u < Urgency.LEAVE_NOW && u != Urgency.MISSED -> ciruela
        else -> when (u) {
            Urgency.CALM -> Color(CW.urgCalma)
            Urgency.UPCOMING -> Color(CW.urgProxima)
            Urgency.SOON -> Color(CW.urgPronto)
            Urgency.LEAVE_NOW -> Color(CW.urgSalir)
            Urgency.URGENT -> Color(CW.urgUrgente)
            Urgency.MISSED -> Color(CW.urgPerdida)
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
