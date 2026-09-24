package com.dpinta.agenda.ui.components.banda

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.dpinta.agenda.ui.components.BotonRelleno
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.components.subrayado
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaMotion
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import com.dpinta.agenda.ui.theme.IconoAgenda
import com.dpinta.agenda.ui.theme.rememberReducirMovimiento

/** Etiquetas de prueba estables (pruebas de UI y captura con uiautomator). */
object EtiquetasBanda {
    const val BANDA = "banda-salida"
    const val CIFRA = "banda-cifra"
    const val INFO = "banda-info"
    const val MODO = "banda-modo"
}

/** Reloj del teléfono (C1.1). Se relee si cambia la configuración. */
@Composable
fun rememberEs24h(): Boolean {
    val contexto = LocalContext.current
    val configuracion = LocalConfiguration.current
    return remember(contexto, configuracion) { DateFormat.is24HourFormat(contexto) }
}

/**
 * Banda de salida: el componente clave de la dirección A «Tablero de salidas».
 * Sin estado propio: todo llega en [modelo] y las decisiones salen por los callbacks.
 *
 * - C4.3: a sangre por izquierda, derecha y arriba; el texto respeta la barra de estado.
 * - C10: un único objeto que cambia de estado con un fundido de 200 ms (C8.2).
 * - C1.2: primaria a la derecha, secundaria a la izquierda. C1.3: el modo se cambia tocándolo.
 * - C9.3: la información se anuncia como una sola frase; las acciones van aparte.
 */
@Composable
fun BandaSalida(
    modelo: BandaSalidaModelo,
    onAccion: (AccionBanda) -> Unit,
    onCambiarModo: () -> Unit,
    modifier: Modifier = Modifier,
    es24h: Boolean = rememberEs24h(),
    reducirMovimiento: Boolean = rememberReducirMovimiento(),
    bajoBarraDeEstado: Boolean = true,
) {
    // Fundido cruzado de opacidad entre estados (C8.2). SizeTransform nulo: el alto cambia
    // de golpe, nunca se anima el layout (C8.1). Sin animación con movimiento reducido (C8.4).
    AnimatedContent(
        targetState = modelo,
        modifier = modifier.testTag(EtiquetasBanda.BANDA),
        transitionSpec = {
            if (reducirMovimiento) {
                (EnterTransition.None togetherWith ExitTransition.None).using(null)
            } else {
                val curva = tween<Float>(AgendaMotion.CAMBIO_ESTADO_MS, easing = AgendaMotion.salida)
                (fadeIn(curva) togetherWith fadeOut(curva)).using(null)
            }
        },
        label = "estado-banda",
        contentKey = { it.estado },
    ) { m ->
        val textos = remember(m, es24h) { textosBanda(m, es24h) }
        val colores = AgendaTheme.colores.coloresBanda(m.estado)
        if (m.estado == EstadoBanda.SinTraslado) {
            FilaSinTraslado(textos, colores, reducirMovimiento, bajoBarraDeEstado)
        } else {
            CampoBanda(textos, colores, reducirMovimiento, bajoBarraDeEstado, onAccion, onCambiarModo)
        }
    }
}

@Composable
private fun CampoBanda(
    t: TextosBanda,
    c: ColoresBanda,
    reducir: Boolean,
    bajoBarraDeEstado: Boolean,
    onAccion: (AccionBanda) -> Unit,
    onCambiarModo: () -> Unit,
) {
    val tipo = AgendaTheme.tipo
    val margen = AgendaTheme.reticula.margen
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.fondo)
            .semantics { isTraversalGroup = true }
            .then(if (bajoBarraDeEstado) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
            .padding(start = margen, end = margen, top = AgendaSpacing.s24, bottom = AgendaSpacing.s20),
    ) {
        // Bloque de información: una sola frase para TalkBack (C9.3).
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(EtiquetasBanda.INFO)
                .clearAndSetSemantics {
                    contentDescription = t.fraseTalkBack
                    heading()
                    traversalIndex = 0f
                },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                t.rotulo?.let { Text(it, style = tipo.rotulo, color = c.frente) }
                Spacer(Modifier.weight(1f))
                t.cuentaAtras?.let { Text(it, style = tipo.meta, color = c.frente) }
            }
            CifraTablero(
                texto = t.cifra,
                estilo = estiloHoraAcotado(),
                color = c.frente,
                reducir = reducir,
                modifier = Modifier.testTag(EtiquetasBanda.CIFRA),
            )
            t.sufijo?.let { Text(it, style = tipo.sufijo, color = c.frente) }
            t.lineaEstado?.let {
                Spacer(Modifier.height(AgendaSpacing.s8))
                Text(it, style = tipo.cuerpo, color = c.frente)
            }
            Spacer(Modifier.height(AgendaSpacing.s16))
            Text(
                t.actividad,
                style = tipo.actividad,
                color = c.frente,
                maxLines = lineasActividad(),
                overflow = TextOverflow.Ellipsis,
            )
            FilaSalon(t, c)
        }
        AccionesYTrayecto(t, c, onAccion, onCambiarModo)
    }
}

@Composable
private fun FilaSalon(t: TextosBanda, c: ColoresBanda) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(t.salon, style = estiloSalonAcotado(), color = c.frente, modifier = Modifier.alignByBaseline())
        Spacer(Modifier.width(AgendaSpacing.s16))
        Text(
            t.lugar,
            style = AgendaTheme.tipo.cuerpo,
            color = c.frente,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
        )
    }
}

/** Modo (tocable, C1.3) + trayecto + frescura del dato (C10). */
@Composable
private fun FilaTrayecto(t: TextosBanda, c: ColoresBanda, onCambiarModo: () -> Unit, focos: FocosBanda) {
    val modo = t.modo ?: return
    val tipo = AgendaTheme.tipo
    Row(verticalAlignment = Alignment.CenterVertically) {
        val interaccion = remember { MutableInteractionSource() }
        Row(
            Modifier
                .then(with(focos) { Modifier.enModo() })
                .heightIn(min = AgendaMedidas.toque)
                .anilloFoco(interaccion, c.frente)
                .clickable(
                    interactionSource = interaccion,
                    indication = ripple(color = c.frente),
                    role = Role.Button,
                    onClickLabel = "Cambiar modo de transporte",
                    onClick = onCambiarModo,
                )
                .testTag(EtiquetasBanda.MODO)
                .semantics { contentDescription = "Modo: $modo. Toca para cambiarlo" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconoAgenda(modeIcono(modo), c.frente, tamano = AgendaMedidas.iconoEnLinea)
            Spacer(Modifier.width(AgendaSpacing.s4))
            Text(modo, style = tipo.meta, color = c.frente, modifier = Modifier.subrayado(c.frente))
        }
        Spacer(Modifier.width(AgendaSpacing.s12))
        Text(
            listOfNotNull(t.trayecto, t.frescura).joinToString(", "),
            style = tipo.meta,
            color = c.frente,
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { },
        )
    }
}

private fun modeIcono(etiqueta: String) =
    ModoTransporte.entries.first { it.etiqueta == etiqueta }.icono

/**
 * Acciones y trayecto. El orden de foco de C9.4 (primaria, secundaria, modo) no coincide con
 * el visual (modo arriba; secundaria a la izquierda, primaria a la derecha, C1.2). Por eso
 * se COMPONEN en el orden de foco y se COLOCAN en el visual; el teclado y el switch access
 * siguen la composición, y TalkBack sigue [traversalIndex].
 */
@Composable
private fun AccionesYTrayecto(
    t: TextosBanda,
    c: ColoresBanda,
    onAccion: (AccionBanda) -> Unit,
    onCambiarModo: () -> Unit,
) {
    val focoPrimaria = remember { FocusRequester() }
    val focoSecundaria = remember { FocusRequester() }
    val focoModo = remember { FocusRequester() }
    val focos = FocosBanda(
        primaria = focoPrimaria,
        secundaria = if (t.secundaria != null) focoSecundaria else null,
        modo = if (t.modo != null) focoModo else null,
    )
    Layout(
        modifier = with(focos) { Modifier.entrada() },
        content = {
            // 0: acciones (primero en foco)
            Box(Modifier.fillMaxWidth()) { if (t.primaria != null) FilaAcciones(t, c, onAccion, focos) }
            // 1: trayecto con el modo (último en foco)
            Box(Modifier.fillMaxWidth().semantics { traversalIndex = 3f }) { FilaTrayecto(t, c, onCambiarModo, focos) }
        },
    ) { medibles, restricciones ->
        val libres = restricciones.copy(minHeight = 0)
        val acciones = medibles[0].measure(libres)
        val trayecto = medibles[1].measure(libres)
        val arriba = AgendaSpacing.s12.roundToPx()
        val entre = if (acciones.height > 0) AgendaSpacing.s16.roundToPx() else 0
        val alto = arriba + trayecto.height + entre + acciones.height
        // Orden de colocación = orden de foco (Compose recorre por orden de colocación);
        // la posición es la visual.
        layout(restricciones.maxWidth, alto) {
            acciones.place(0, arriba + trayecto.height + entre)
            trayecto.place(0, arriba)
        }
    }
}

/**
 * Foco de C9.4: primaria → secundaria → modo. Se consigue colocando en ese orden (Compose
 * recorre por orden de colocación) y entrando al grupo siempre por la primaria.
 */
private class FocosBanda(
    val primaria: FocusRequester,
    val secundaria: FocusRequester?,
    val modo: FocusRequester?,
) {
    fun Modifier.enPrimaria(): Modifier = focusRequester(primaria)
    fun Modifier.enSecundaria(): Modifier = secundaria?.let { focusRequester(it) } ?: this
    fun Modifier.enModo(): Modifier = modo?.let { focusRequester(it) } ?: this

    /** Al entrar en la banda hacia delante, el foco va a la primaria; hacia atrás, al último. */
    fun Modifier.entrada(): Modifier = this
        .focusProperties {
            onEnter = {
                when (requestedFocusDirection) {
                    FocusDirection.Previous -> (modo ?: secundaria ?: primaria).requestFocus()
                    else -> primaria.requestFocus()
                }
            }
        }
        .focusGroup()
}

/** C1.2: secundaria a la izquierda (texto subrayado), primaria a la derecha (relleno). */
@Composable
private fun FilaAcciones(t: TextosBanda, c: ColoresBanda, onAccion: (AccionBanda) -> Unit, focos: FocosBanda) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            // 0: primaria (primera en foco), 1: secundaria.
            Box(Modifier.semantics { traversalIndex = 1f }) {
                t.primaria?.let { BotonPrimario(it, c, with(focos) { Modifier.enPrimaria() }) { onAccion(it) } }
            }
            Box(Modifier.semantics { traversalIndex = 2f }) {
                t.secundaria?.let { BotonSecundario(it, c, with(focos) { Modifier.enSecundaria() }) { onAccion(it) } }
            }
        },
    ) { medibles, restricciones ->
        val libres = restricciones.copy(minWidth = 0, minHeight = 0)
        val primaria = medibles[0].measure(libres)
        val secundaria = medibles[1].measure(libres.copy(maxWidth = (restricciones.maxWidth - primaria.width).coerceAtLeast(0)))
        val alto = maxOf(primaria.height, secundaria.height)
        layout(restricciones.maxWidth, alto) {
            primaria.place(restricciones.maxWidth - primaria.width, (alto - primaria.height) / 2)
            secundaria.place(0, (alto - secundaria.height) / 2)
        }
    }
}

@Composable
private fun BotonPrimario(accion: AccionBanda, c: ColoresBanda, modifier: Modifier, onClick: () -> Unit) =
    BotonRelleno(accion.etiqueta, relleno = c.frente, tinta = c.fondo, modifier = modifier, onClick = onClick)

@Composable
private fun BotonSecundario(accion: AccionBanda, c: ColoresBanda, modifier: Modifier, onClick: () -> Unit) =
    BotonSubrayado(accion.etiqueta, tinta = c.frente, modifier = modifier, onClick = onClick)

/** C10 «sin traslado»: sin campo; la hora baja a tamaño de salón. */
@Composable
private fun FilaSinTraslado(t: TextosBanda, c: ColoresBanda, reducir: Boolean, bajoBarraDeEstado: Boolean) {
    val tipo = AgendaTheme.tipo
    val margen = AgendaTheme.reticula.margen
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.fondo)
            .then(if (bajoBarraDeEstado) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = margen, end = margen, top = AgendaSpacing.s24, bottom = AgendaSpacing.s20)
                .testTag(EtiquetasBanda.INFO)
                .clearAndSetSemantics {
                    contentDescription = t.fraseTalkBack
                    heading()
                },
        ) {
            CifraTablero(
                texto = t.cifra,
                estilo = estiloSalonAcotado(),
                color = c.frente,
                reducir = reducir,
                modifier = Modifier.testTag(EtiquetasBanda.CIFRA),
            )
            t.sufijo?.let { Text(it, style = tipo.sufijo, color = c.frente) }
            Spacer(Modifier.height(AgendaSpacing.s8))
            Text(
                t.actividad,
                style = tipo.actividad,
                color = c.frente,
                maxLines = lineasActividad(),
                overflow = TextOverflow.Ellipsis,
            )
            FilaSalon(t, c)
        }
        HorizontalDivider(thickness = AgendaMedidas.filete, color = AgendaTheme.colores.filete)
    }
}

/**
 * Cifra tipo tablero (C8.2): cada carácter vive en su celda; solo los que cambian se animan
 * (el nuevo entra de -8 dp a 0 con alpha 0 → 1; el viejo sale hacia abajo). Las cifras
 * tabulares (C2.2) hacen que todas las celdas de dígito midan lo mismo.
 */
@Composable
internal fun CifraTablero(
    texto: String,
    estilo: TextStyle,
    color: Color,
    reducir: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier.semantics { contentDescription = texto }) {
        texto.forEachIndexed { i, caracter ->
            // Clave desde la derecha: los minutos conservan su celda si cambia el número de cifras.
            key(texto.length - i) {
                CeldaTablero(caracter, estilo, color, reducir)
            }
        }
    }
}

@Composable
private fun CeldaTablero(caracter: Char, estilo: TextStyle, color: Color, reducir: Boolean) {
    var actual by remember { mutableStateOf(caracter) }
    var anterior by remember { mutableStateOf<Char?>(null) }
    val progreso = remember { Animatable(1f) }
    val recorrido = with(LocalDensity.current) { AgendaMotion.desplazamientoDigito.toPx() }

    LaunchedEffect(caracter) {
        if (caracter == actual) return@LaunchedEffect
        anterior = actual
        actual = caracter
        if (reducir) {
            progreso.snapTo(1f)
        } else {
            // Interrumpible: un cambio nuevo reinicia desde el carácter visible (C8.5).
            progreso.snapTo(0f)
            progreso.animateTo(1f, tween(AgendaMotion.DIGITO_MS, easing = AgendaMotion.salida))
        }
        anterior = null
    }

    Box {
        anterior?.let { viejo ->
            Text(
                viejo.toString(),
                style = estilo,
                color = color,
                modifier = Modifier.graphicsLayer {
                    alpha = 1f - progreso.value
                    translationY = recorrido * progreso.value
                },
            )
        }
        Text(
            actual.toString(),
            style = estilo,
            color = color,
            modifier = Modifier.graphicsLayer {
                alpha = progreso.value
                translationY = -recorrido * (1f - progreso.value)
            },
        )
    }
}

/** C2.4: con escala de fuente alta, la hora se limita a 112 sp efectivos. */
@Composable
private fun estiloHoraAcotado(): TextStyle = acotar(AgendaTheme.tipo.horaSalida, maximoSp = 112f)

@Composable
private fun estiloSalonAcotado(): TextStyle = AgendaTheme.tipo.salon

@Composable
private fun acotar(estilo: TextStyle, maximoSp: Float): TextStyle {
    val escala = LocalDensity.current.fontScale
    val efectivo = estilo.fontSize.value * escala
    if (efectivo <= maximoSp) return estilo
    val factor = maximoSp / efectivo
    return estilo.copy(
        fontSize = (estilo.fontSize.value * factor).sp,
        lineHeight = (estilo.lineHeight.value * factor).sp,
    )
}

/** C2.3 / C2.4: dos líneas de actividad; tres con escala de fuente ≥ 1,3. */
@Composable
private fun lineasActividad(): Int = if (LocalDensity.current.fontScale >= 1.3f) 3 else 2

// ---------- Previews: cada estado en claro y oscuro, a 360 dp (C4.2 compacta) ----------

@Preview(name = "claro", widthDp = 360, showBackground = true)
@Preview(name = "oscuro", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class PreviewBanda

@Composable
private fun PreviewDe(estado: EstadoBanda, viejo: Boolean = false) {
    AgendaTheme {
        BandaSalida(
            modelo = BandaSalidaMuestras.para(estado, viejo),
            onAccion = {},
            onCambiarModo = {},
            reducirMovimiento = true,
            bajoBarraDeEstado = false,
        )
    }
}

@PreviewBanda @Composable private fun PreviewEspera() = PreviewDe(EstadoBanda.Espera)
@PreviewBanda @Composable private fun PreviewPreparate() = PreviewDe(EstadoBanda.Preparate)
@PreviewBanda @Composable private fun PreviewSalYa() = PreviewDe(EstadoBanda.SalYa)
@PreviewBanda @Composable private fun PreviewVasTarde() = PreviewDe(EstadoBanda.VasTarde)
@PreviewBanda @Composable private fun PreviewEnCamino() = PreviewDe(EstadoBanda.EnCamino)
@PreviewBanda @Composable private fun PreviewSinTraslado() = PreviewDe(EstadoBanda.SinTraslado)
@PreviewBanda @Composable private fun PreviewDatoViejo() = PreviewDe(EstadoBanda.Espera, viejo = true)

