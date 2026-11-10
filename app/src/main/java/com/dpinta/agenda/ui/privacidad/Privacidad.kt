package com.dpinta.agenda.ui.privacidad

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.dpinta.agenda.ui.components.BotonRelleno
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.NotaPrivacidad
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import java.time.LocalDate

/**
 * Política de tratamiento de datos dentro de la app (regla de privacidad de Diego, punto 1). Es el mismo texto que
 * `docs/legal/politica-privacidad.md`: borrador técnico que revisa un abogado antes de publicarse para terceros.
 */
object PoliticaPrivacidad {
    const val VERSION = "0.2"
    const val VIGENCIA = "10 de noviembre de 2026"
    val secciones: List<Pair<String, String>> = listOf(
        "Quién es el responsable" to "Diego Armando Pinta Cuasquen, desarrollador de Agenda. Contacto: diarpicu2022@gmail.com.",
        "Qué se guarda" to "Tu perfil, tus actividades, lugares, traslados y semestres, la asistencia a tus clases y la hora real de tus " +
            "salidas. Todo queda solo en este teléfono, en una base de datos cifrada. Agenda no tiene cuenta, ni servidor, ni permiso de Internet.",
        "Tu ritmo" to "Con la hora en que tocas «Voy saliendo» la app aprende, en el teléfono, cuántos minutos sueles salir tarde y te lo " +
            "aconseja. Solo aconseja: no cambia tus avisos.",
        "El reloj" to "CampusWatch recibe del teléfono el horario de hoy y mañana, tu asistencia por materia y el consejo de Tu ritmo. " +
            "No habla con ningún servidor y no recibe el historial de salidas.",
        "Qué no hace" to "No hay publicidad, analítica, rastreo ni cookies, y no hay copia en la nube.",
        "Cuánto tiempo" to "Hasta que borres los datos o desinstales la app.",
        "Tus derechos" to "Conocer, actualizar, rectificar y suprimir tus datos y revocar la autorización (Ley 1581 de 2012 y Decreto 1377 " +
            "de 2013, compilado en el Decreto 1074 de 2015). Corriges tu perfil en Ajustes. En Ajustes, Datos exportas tu horario a un archivo .ics y borras todo.",
        "Cambios" to "Si esta política cambia, la app te muestra la nueva versión con su fecha y te pide aceptarla otra vez.",
    )
}

/** Consentimiento registrado con versión y fecha (regla de privacidad, punto 2). Se vuelve a pedir si cambia la versión. */
object Consentimiento {
    private const val PREFS = "consentimiento"
    fun aceptado(c: Context): Boolean =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("version", null) == PoliticaPrivacidad.VERSION
    fun aceptar(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
        putString("version", PoliticaPrivacidad.VERSION)
        putString("fecha", LocalDate.now().toString())
    }
}

@Composable
private fun Secciones() {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    for ((titulo, texto) in PoliticaPrivacidad.secciones) {
        Text(titulo, style = t.cuerpoFuerte, color = c.tinta, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        Text(texto, style = t.cuerpo, color = c.tinta)
        Spacer(Modifier.height(AgendaSpacing.s16))
    }
}

/** Ajustes › Datos › Política de privacidad. */
@Composable
fun PrivacidadPantalla(onAtras: () -> Unit) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(Modifier.fillMaxSize().background(c.papel)) {
        Cabecera("Política de privacidad", onAtras = onAtras)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = m, vertical = AgendaSpacing.s8).navigationBarsPadding()) {
            Text("Versión ${PoliticaPrivacidad.VERSION} · vigente desde el ${PoliticaPrivacidad.VIGENCIA}", style = AgendaTheme.tipo.apoyo, color = c.tinta2)
            Spacer(Modifier.height(AgendaSpacing.s16))
            Secciones()
        }
    }
}

/**
 * Primer uso (y cada versión nueva): la nota de privacidad del sistema, la política a un toque y «Acepto». Sin aceptar
 * no se entra, porque la app guarda datos personales desde la primera actividad (Ley 1581: autorización previa).
 */
@Composable
fun ConsentimientoPantalla(onAceptar: () -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    var completa by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(c.papel).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = m, vertical = AgendaSpacing.s24),
        verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s16),
    ) {
        Text("Antes de empezar", style = t.titulo, color = c.tinta, modifier = Modifier.semantics { heading() })
        NotaPrivacidad("Tus datos permanecen en este dispositivo.", "Agenda no tiene cuenta ni servidor. Puedes borrar todo cuando quieras desde Ajustes.")
        Text(
            "Para avisarte cuándo salir, Agenda guarda tu horario, tus lugares, tu asistencia y la hora real de tus salidas, " +
                "cifrados en este teléfono. Lee la política y acepta para continuar.",
            style = t.cuerpo, color = c.tinta,
        )
        if (completa) Column { Secciones() }
        else BotonSubrayado("Leer la política completa", c.tinta) { completa = true }
        Text("Versión ${PoliticaPrivacidad.VERSION} · ${PoliticaPrivacidad.VIGENCIA}", style = t.apoyo, color = c.tinta2)
        BotonRelleno("Acepto", c.tinta, c.papel, Modifier.fillMaxWidth()) { onAceptar() }
    }
}
