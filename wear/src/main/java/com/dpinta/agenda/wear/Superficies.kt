package com.dpinta.agenda.wear

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.dpinta.agenda.domain.Urgency
import com.dpinta.agenda.domain.WatchSession
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/** Pide a la Tarjeta y a las Complicaciones redibujarse cuando llega un día nuevo del teléfono. */
object Superficies {
    fun actualizar(contexto: Context) {
        runCatching { TileService.getUpdater(contexto).requestUpdate(TarjetaProxima::class.java) }
        runCatching { ComplicationDataSourceUpdateRequester.create(contexto, ComponentName(contexto, ComplicacionSalida::class.java)).requestUpdateAll() }
    }
}

/** Qué mostrar fuera de la app: la siguiente sesión, su urgencia y la hora de referencia (salida o inicio). */
private data class Resumen(val s: WatchSession, val u: Urgency, val referencia: LocalDateTime) {
    val salir get() = s.leaveAt != null
}

private fun resumen(contexto: Context, ahora: LocalDateTime): Resumen? {
    val dia = AlmacenDia.dia(contexto).value ?: return null
    val s = dia.next(ahora) ?: return null
    return Resumen(s, s.urgency(ahora, ZoneId.systemDefault()), s.leaveAt ?: s.start)
}

private fun argbDe(c: Long) = argb(c.toInt())

private fun <T : Any> listo(v: T): ListenableFuture<T> = CallbackToFutureAdapter.getFuture { it.set(v); "listo" }

/**
 * Tarjeta / Tile (WatchTile): «Próxima», nombre, «hora · aula» y «Salir HH:MM» en grande, con el arco de la cuenta.
 * Las Tiles no cargan fuentes de la app: usa la condensada del sistema (desviación anotada en el anexo).
 */
class TarjetaProxima : TileService() {
    override fun onTileRequest(pedido: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val ahora = LocalDateTime.now()
        val r = resumen(this, ahora)
        val raiz = if (r == null) vacia() else tarjeta(r, ahora)
        val clic = ModifiersBuilders.Clickable.Builder()
            .setOnClick(androidx.wear.protolayout.ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(androidx.wear.protolayout.ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName).setClassName(CampusWatchActivity::class.java.name).build()).build())
            .setId("abrir").build()
        val todo = Box.Builder().setWidth(expand()).setHeight(expand())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clic)
                .setBackground(ModifiersBuilders.Background.Builder().setColor(argbDe(CW.fondo)).build()).build())
            .addContent(raiz).build()
        return listo(
            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(60_000)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(todo))
                .build(),
        )
    }

    override fun onTileResourcesRequest(pedido: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        listo(ResourceBuilders.Resources.Builder().setVersion("1").build())

    private fun texto(t: String, tam: Float, color: Long, negrita: Boolean = false) = Text.Builder().setText(t).setMaxLines(1)
        .setFontStyle(FontStyle.Builder().setSize(sp(tam)).setColor(argbDe(color))
            .setWeight(if (negrita) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL).build())
        .build()

    private fun vacia(): LayoutElementBuilders.LayoutElement = Column.Builder()
        .addContent(texto("Nada fijo por ahora", 17f, CW.tinta, true))
        .addContent(texto("CampusWatch", 15f, CW.tintaSuave))
        .build()

    private fun tarjeta(r: Resumen, ahora: LocalDateTime): LayoutElementBuilders.LayoutElement {
        val color = colorUrgencia(r.u, r.s.exam)
        val minutos = Duration.between(ahora, r.s.start).toMinutes().coerceIn(0, 60)
        val riel = Arc.Builder()
            .setAnchorAngle(androidx.wear.protolayout.DimensionBuilders.degrees(0f))
            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
            .addContent(ArcLine.Builder().setLength(androidx.wear.protolayout.DimensionBuilders.degrees(360f))
                .setThickness(dp(6f)).setColor(argbDe(CW.superficieFuerte)).build())
            .build()
        val arco = Arc.Builder()
            .setAnchorAngle(androidx.wear.protolayout.DimensionBuilders.degrees(0f))
            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
            .addContent(ArcLine.Builder().setLength(androidx.wear.protolayout.DimensionBuilders.degrees(360f * minutos / 60f))
                .setThickness(dp(6f)).setColor(argbDe(color)).build())
            .build()
        val datos = Column.Builder()
            .addContent(texto(if (r.s.exam) "PARCIAL" else "Próxima", 15f, if (r.s.exam) CW.ciruela else CW.tintaSuave, r.s.exam))
            .addContent(texto(r.s.title, 20f, CW.tinta, true))
            // Con trayecto: hora de inicio · aula (la grande es la salida). Sin trayecto la grande ya es el inicio: aula · lugar.
            .addContent(texto((if (r.salir) listOf(Formato.hora(r.s.start.toLocalTime()), r.s.room.ifBlank { r.s.place }) else listOf(r.s.room, r.s.place))
                .filter { it.isNotBlank() }.joinToString(" · "), 15f, CW.tintaSuave))
            .addContent(Spacer.Builder().setHeight(dp(4f)).build())
            .addContent(
                Row.Builder()
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(texto(if (r.salir) "Salir " else "Empieza ", 15f, CW.tintaSuave))
                    .addContent(texto(Formato.hora(r.referencia.toLocalTime()), 32f, CW.hora, true))
                    .build(),
            )
            .build()
        val margen = ModifiersBuilders.Modifiers.Builder().setPadding(ModifiersBuilders.Padding.Builder().setAll(dp(4f)).build()).build()
        return Box.Builder().setWidth(expand()).setHeight(expand()).setModifiers(margen)
            .addContent(riel).addContent(arco).addContent(datos).build()
    }
}

private fun colorUrgencia(u: Urgency, examen: Boolean): Long = when {
    examen && u < Urgency.LEAVE_NOW && u != Urgency.MISSED -> CW.ciruela
    else -> when (u) {
        Urgency.CALM -> CW.urgCalma
        Urgency.UPCOMING -> CW.urgProxima
        Urgency.SOON -> CW.urgPronto
        Urgency.LEAVE_NOW -> CW.urgSalir
        Urgency.URGENT -> CW.urgUrgente
        Urgency.MISSED -> CW.urgPerdida
    }
}

/**
 * Complicación (WatchComplications): anillo de minutos para salir (RANGED_VALUE, 0–60) o «18 min» (SHORT_TEXT).
 * El sistema la tiñe y la recorta según la esfera.
 */
class ComplicacionSalida : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? = datos(type, 18, "MAT", "Salir en 18 min")

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val ahora = LocalDateTime.now()
        val r = resumen(this, ahora) ?: return datos(request.complicationType, null, "—", "Nada fijo por ahora")
        val min = Duration.between(ahora, r.referencia).toMinutes().coerceAtLeast(0).toInt()
        val corto = r.s.title.take(3).uppercase()
        val frase = if (r.salir) "Salir en ${Formato.cuenta(Duration.ofMinutes(min.toLong()))}" else "${r.s.title} en ${Formato.cuenta(Duration.ofMinutes(min.toLong()))}"
        return datos(request.complicationType, min, corto, frase)
    }

    private fun datos(type: ComplicationType, minutos: Int?, corto: String, frase: String): ComplicationData? {
        val descripcion = PlainComplicationText.Builder(frase).build()
        return when (type) {
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = (minutos ?: 0).coerceAtMost(60).toFloat(), min = 0f, max = 60f, contentDescription = descripcion,
            ).setText(PlainComplicationText.Builder(minutos?.toString() ?: corto).build()).build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(minutos?.let { "$it min" } ?: corto).build(), descripcion,
            ).setTitle(PlainComplicationText.Builder(corto).build()).build()
            else -> null
        }
    }
}
