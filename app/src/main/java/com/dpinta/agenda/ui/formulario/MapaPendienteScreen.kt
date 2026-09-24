package com.dpinta.agenda.ui.formulario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dpinta.agenda.ui.components.BotonRelleno
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme

/**
 * Elegir el lugar en el mapa (anexo §7, flujo 2): ESQUELETO en estado «pendiente de Maps».
 * No hay clave de Google todavía; no se dibuja un mapa falso. Solo el marco del futuro mapa
 * con el alfiler fijo en el centro (el mapa se moverá debajo) y la salida: escribir el lugar.
 */
@Composable
fun MapaPendientePantalla(onVolver: () -> Unit, modifier: Modifier = Modifier) {
    val c = AgendaTheme.colores
    val m = AgendaTheme.reticula.margen
    Column(modifier.fillMaxSize().background(c.papel)) {
        Cabecera("Elegir en el mapa", onAtras = onVolver)
        Box(
            Modifier
                .fillMaxWidth()
                // Alto del marco: 5 × 64 dp de la escala (C4.1), una pantalla de mapa manejable a una mano.
                .height(AgendaSpacing.s64 * 5)
                .background(c.papel2)
                .drawBehind {
                    // Alfiler fijo: filete vertical de tinta + cuadrado de 10 dp (formas del repertorio C6).
                    val x = size.width / 2
                    val y = size.height / 2
                    val lado = AgendaMedidas.marcadorTipo.toPx()
                    drawLine(c.tinta, Offset(x, y - 3 * lado), Offset(x, y), AgendaMedidas.hilo.toPx())
                    drawRect(c.tinta, topLeft = Offset(x - lado / 2, y - 3 * lado - lado / 2), size = Size(lado, lado))
                }
                .semantics { contentDescription = "Mapa pendiente: todavía no está configurado" },
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                "Mapa pendiente",
                style = AgendaTheme.tipo.seccion,
                color = c.tinta,
                modifier = Modifier.padding(m),
            )
        }
        Text(
            "El mapa llega cuando se configure Google Maps. Mientras tanto, escribe el nombre del lugar en el formulario; " +
                "si coincide con uno que ya usas, se reutiliza.",
            style = AgendaTheme.tipo.cuerpo,
            color = c.tinta,
            modifier = Modifier.padding(horizontal = m, vertical = AgendaSpacing.s16),
        )
        BotonRelleno(
            "Escribir el lugar",
            relleno = c.tinta,
            tinta = c.papel,
            modifier = Modifier.padding(horizontal = m).align(Alignment.End),
            onClick = onVolver,
        )
    }
}

