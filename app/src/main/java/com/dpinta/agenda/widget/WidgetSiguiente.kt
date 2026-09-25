package com.dpinta.agenda.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.dpinta.agenda.MainActivity
import com.dpinta.agenda.R
import com.dpinta.agenda.avisos.ProgramadorAvisos
import com.dpinta.agenda.avisos.enSegundoPlano
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.trayectoHacia
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.TravelEstimate
import com.dpinta.agenda.ui.components.banda.antiguedad
import com.dpinta.agenda.ui.components.banda.formatearHora
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** Widget «Siguiente» (anexo): la hora de salida (o de inicio) y el salón; en 4×2, qué, dónde y lo siguiente. */
@AndroidEntryPoint
class WidgetSiguiente : AppWidgetProvider() {

    @Inject lateinit var repositorio: AgendaRepository
    @Inject lateinit var programador: ProgramadorAvisos
    @Inject lateinit var reloj: Clock

    override fun onUpdate(contexto: Context, gestor: AppWidgetManager, ids: IntArray) {
        enSegundoPlano {
            val modelo = cargar()
            val es24h = DateFormat.is24HourFormat(contexto)
            for (id in ids) gestor.updateAppWidget(id, vistas(contexto, gestor, id, modelo, es24h))
        }
    }

    private suspend fun cargar(): WidgetModelo {
        val agenda = repositorio.agenda().first()
        val ahora = LocalDateTime.now(reloj)
        val sesiones = programador.sesiones(agenda, ahora.toLocalDate(), ahora.toLocalDate().plusDays(ProgramadorAvisos.DIAS))
        val trayectos = HashMap<Occurrence, TravelEstimate?>()
        for (s in sesiones) {
            val modo = agenda.actividades[s.activityId]?.modo ?: continue
            trayectos[s] = repositorio.trayectoHacia(agenda, sesiones, s, modo, reloj.instant())
        }
        return WidgetMapeador.modelo(agenda, sesiones, ahora, reloj.zone) { trayectos[it] }
    }

    private fun vistas(contexto: Context, gestor: AppWidgetManager, id: Int, m: WidgetModelo, es24h: Boolean): RemoteViews {
        val dibujo = WidgetDibujo(contexto)
        val minimo = gestor.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).coerceAtLeast(110)
        // Ancho útil de la columna de la hora: el del widget menos 16 dp por lado; en 4×2, la mitad.
        fun util(anchoDp: Int, columnas: Int) = px(contexto, ((anchoDp - 32) - 16 * (columnas - 1)) / columnas)
        val pequeno = rellenar(RemoteViews(contexto.packageName, R.layout.widget_siguiente), contexto, m, es24h, dibujo, util(minimo, 1))
        val ancho = rellenar(RemoteViews(contexto.packageName, R.layout.widget_siguiente_ancho), contexto, m, es24h, dibujo, util(maxOf(minimo, 250), 2))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return RemoteViews(mapOf(SizeF(110f, 110f) to pequeno, SizeF(250f, 110f) to ancho))
        }
        return if (minimo >= 250) ancho else pequeno
    }

    private fun px(contexto: Context, dp: Int) = (dp * contexto.resources.displayMetrics.density).toInt()

    private fun rellenar(v: RemoteViews, contexto: Context, m: WidgetModelo, es24h: Boolean, dibujo: WidgetDibujo, anchoMax: Int): RemoteViews {
        val abrir = PendingIntent.getActivity(
            contexto, 0, Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE,
        )
        v.setOnClickPendingIntent(R.id.widget_raiz, abrir)
        val textos = WidgetTextos.de(m, LocalDate.now(reloj), es24h)
        fun poner(vista: Int, texto: String?) {
            v.setViewVisibility(vista, if (texto.isNullOrBlank()) View.GONE else View.VISIBLE)
            v.setTextViewText(vista, texto.orEmpty())
        }
        fun imagen(vista: Int, bmp: android.graphics.Bitmap?) {
            v.setViewVisibility(vista, if (bmp == null) View.GONE else View.VISIBLE)
            if (bmp != null) v.setImageViewBitmap(vista, bmp)
        }
        imagen(R.id.widget_rotulo, textos.rotulo?.let { dibujo.rotulo(it, anchoMax) })
        imagen(R.id.widget_hora, textos.hora?.let { dibujo.hora(it, textos.sufijo, anchoMax) })
        imagen(R.id.widget_salon, textos.salon?.let { dibujo.salon(it, anchoMax) })
        poner(R.id.widget_hace, textos.nota)
        // En los estados vacíos la nota es la frase entera: hasta 3 líneas.
        v.setInt(R.id.widget_hace, "setMaxLines", if (textos.hora == null) 3 else 1)
        if (v.layoutId == R.layout.widget_siguiente) poner(R.id.widget_nombre, textos.actividad?.takeIf { textos.salon == null })
        if (v.layoutId == R.layout.widget_siguiente_ancho) {
            poner(R.id.widget_actividad, textos.actividad)
            poner(R.id.widget_lugar, textos.lugar)
            poner(R.id.widget_despues, textos.despues)
        }
        v.setContentDescription(R.id.widget_raiz, textos.hablado)
        return v
    }

    companion object {
        /** Pide redibujar todos los widgets colocados (tras reprogramar alarmas o al empezar una sesión). */
        fun actualizar(contexto: Context) {
            val gestor = AppWidgetManager.getInstance(contexto)
            val ids = gestor.getAppWidgetIds(ComponentName(contexto, WidgetSiguiente::class.java))
            if (ids.isEmpty()) return
            contexto.sendBroadcast(
                Intent(contexto, WidgetSiguiente::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }
    }
}

/** Textos del widget: puros, para probarlos sin Android. */
data class WidgetTextos(
    val rotulo: String?,
    val hora: String?,
    val sufijo: String?,
    val salon: String?,
    val nota: String?,
    val actividad: String?,
    val lugar: String?,
    val despues: String?,
    val hablado: String,
) {
    companion object {
        private val ESPANOL = Locale.forLanguageTag("es")

        fun de(m: WidgetModelo, hoy: LocalDate, es24h: Boolean): WidgetTextos = when (m) {
            WidgetModelo.PrimerUso -> vacio("Empieza por tu horario de clases")
            WidgetModelo.SinNada -> vacio("Nada fijo en los próximos días.")
            is WidgetModelo.Siguiente -> {
                val dia = m.dia?.let { if (it == hoy.plusDays(1)) "mañana" else it.dayOfWeek.getDisplayName(TextStyle.FULL, ESPANOL) }
                val estado = if (m.salir) "sal a las" else "empieza"
                val hora = formatearHora(m.hora, es24h)
                val nota = m.hace?.let { "hace ${antiguedad(it)}" }
                val despues = m.despues?.let { d -> listOf("Después: ${formatearHora(d.hora, es24h).enLinea}", d.actividad, d.salon).filter { it.isNotBlank() }.joinToString(" · ") }
                WidgetTextos(
                    rotulo = listOfNotNull(dia, estado).joinToString(", ").uppercase(ESPANOL),
                    hora = hora.cifra,
                    sufijo = hora.sufijo12h,
                    salon = m.salon.ifBlank { null },
                    nota = nota,
                    actividad = m.actividad,
                    lugar = m.lugar.ifBlank { null },
                    despues = despues,
                    hablado = listOfNotNull(
                        listOfNotNull(dia, estado).joinToString(", ") + " " + hora.enLinea,
                        m.actividad,
                        m.salon.ifBlank { null }?.let { "salón $it" },
                        m.lugar.ifBlank { null },
                        nota,
                    ).joinToString(", ").replaceFirstChar { it.uppercase() },
                )
            }
        }

        private fun vacio(frase: String) = WidgetTextos(null, null, null, null, frase, null, null, null, frase)
    }
}
