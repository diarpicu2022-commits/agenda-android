package com.dpinta.agenda.ui.permisos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dpinta.agenda.ui.components.BotonRelleno
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme

/**
 * Permiso de notificaciones (Flujo 3 del anexo): se explica antes de pedirlo, una sola vez, cuando
 * ya hay algo que avisar; si lo niega, Hoy muestra la franja «Sin avisos» (C10).
 */
class PermisoAvisos internal constructor(
    val concedido: Boolean,
    /** Mostrar la pantalla previa: hay actividades, falta el permiso y aún no se explicó. */
    val explicar: Boolean,
    val activar: () -> Unit,
    val ahoraNo: () -> Unit,
)

private const val PREFERENCIAS = "permisos"
private const val EXPLICADO = "avisos_explicado"

@Composable
fun rememberPermisoAvisos(hayActividades: Boolean): PermisoAvisos {
    val contexto = LocalContext.current
    val preferencias = remember { contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE) }
    var concedido by remember { mutableStateOf(avisosActivos(contexto)) }
    var explicado by remember { mutableStateOf(preferencias.getBoolean(EXPLICADO, false)) }
    // Al volver de Ajustes del sistema, el permiso puede haber cambiado.
    LifecycleResumeEffect(Unit) {
        concedido = avisosActivos(contexto)
        onPauseOrDispose { }
    }
    fun marcarExplicado() {
        explicado = true
        preferencias.edit { putBoolean(EXPLICADO, true) }
    }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { si ->
        concedido = si && avisosActivos(contexto)
        // Denegado dos veces, Android ya no muestra el diálogo: el camino es Ajustes.
        if (!si && explicado) abrirAjustes(contexto)
        marcarExplicado()
    }
    return PermisoAvisos(
        concedido = concedido,
        explicar = hayActividades && !concedido && !explicado,
        activar = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationManagerCompat.from(contexto).areNotificationsEnabled()) {
                lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                marcarExplicado()
                abrirAjustes(contexto)
            }
        },
        ahoraNo = ::marcarExplicado,
    )
}

private fun avisosActivos(contexto: Context) = NotificationManagerCompat.from(contexto).areNotificationsEnabled()

private fun abrirAjustes(contexto: Context) {
    contexto.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Pantalla previa del Flujo 3: el resultado concreto que habilita, con un ejemplo dibujado. */
@Composable
fun PantallaPermisoAvisos(onActivar: () -> Unit, onAhoraNo: () -> Unit, modifier: Modifier = Modifier) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(
        modifier
            .fillMaxSize()
            .background(c.papel)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = m, vertical = AgendaSpacing.s32),
    ) {
        Text(
            "Para avisarte «Empieza Cálculo en B-204» necesito enviarte notificaciones.",
            style = AgendaTheme.tipo.seccion,
            color = c.tinta,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(AgendaSpacing.s24))
        EjemploAviso()
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BotonSubrayado("Ahora no", tinta = c.tinta, onClick = onAhoraNo)
            BotonRelleno("Activar", relleno = c.tinta, tinta = c.papel, onClick = onActivar)
        }
    }
}

/** Una notificación dibujada con los tokens de la app: sin radio ni sombra (C11). */
@Composable
private fun EjemploAviso() {
    val c = AgendaTheme.colores
    // Sin icono: el repertorio no tiene uno de notificación y el de aviso significa conflicto.
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.papel2)
            .border(AgendaMedidas.filete, c.filete)
            .padding(AgendaSpacing.s16)
            .clearAndSetSemantics { contentDescription = "Ejemplo de aviso: Empieza Cálculo, B-204" },
    ) {
        Text("Agenda · en 15 min", style = AgendaTheme.tipo.meta, color = c.tinta2)
        Spacer(Modifier.height(AgendaSpacing.s4))
        Text("Empieza Cálculo · B-204", style = AgendaTheme.tipo.cuerpo, color = c.tinta)
    }
}

/** C10 «sin permiso de notificaciones»: franja fija sobre la banda, en `papel-2`, con «Activar». */
@Composable
fun FranjaSinAvisos(onActivar: () -> Unit, modifier: Modifier = Modifier) {
    val c = AgendaTheme.colores
    Row(
        modifier
            .fillMaxWidth()
            .background(c.papel2)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = AgendaTheme.reticula.margen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Sin avisos: no te llegará nada", style = AgendaTheme.tipo.meta, color = c.tinta, modifier = Modifier.weight(1f))
        BotonSubrayado("Activar", tinta = c.tinta, onClick = onActivar)
    }
}
