package com.dpinta.agenda.ui.privacidad

import com.dpinta.agenda.R
import androidx.compose.ui.res.stringResource
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
    /** Títulos y textos en `values/strings.xml` y `values-en/strings.xml` (politica_t1…t8, politica_c1…c8). */
    val secciones: List<Pair<Int, Int>> = listOf(
        R.string.politica_t1 to R.string.politica_c1,
        R.string.politica_t2 to R.string.politica_c2,
        R.string.politica_t3 to R.string.politica_c3,
        R.string.politica_t4 to R.string.politica_c4,
        R.string.politica_t5 to R.string.politica_c5,
        R.string.politica_t6 to R.string.politica_c6,
        R.string.politica_t7 to R.string.politica_c7,
        R.string.politica_t8 to R.string.politica_c8,
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
        Text(stringResource(titulo), style = t.cuerpoFuerte, color = c.tinta, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        Text(stringResource(texto), style = t.cuerpo, color = c.tinta)
        Spacer(Modifier.height(AgendaSpacing.s16))
    }
}

/** Ajustes › Datos › Política de privacidad. */
@Composable
fun PrivacidadPantalla(onAtras: () -> Unit) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(Modifier.fillMaxSize().background(c.papel)) {
        Cabecera(stringResource(R.string.ajustes_politica), onAtras = onAtras)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = m, vertical = AgendaSpacing.s8).navigationBarsPadding()) {
            Text(stringResource(R.string.politica_version_vigente, PoliticaPrivacidad.VERSION, stringResource(R.string.politica_vigencia)), style = AgendaTheme.tipo.apoyo, color = c.tinta2)
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
        Text(stringResource(R.string.consent_titulo), style = t.titulo, color = c.tinta, modifier = Modifier.semantics { heading() })
        NotaPrivacidad(stringResource(R.string.consent_nota_titulo), stringResource(R.string.consent_nota_cuerpo))
        Text(
            stringResource(R.string.consent_cuerpo),
            style = t.cuerpo, color = c.tinta,
        )
        if (completa) Column { Secciones() }
        else BotonSubrayado(stringResource(R.string.consent_leer), c.tinta) { completa = true }
        Text(stringResource(R.string.politica_version_corta, PoliticaPrivacidad.VERSION, stringResource(R.string.politica_vigencia)), style = t.apoyo, color = c.tinta2)
        BotonRelleno(stringResource(R.string.consent_acepto), c.tinta, c.papel, Modifier.fillMaxWidth()) { onAceptar() }
    }
}
