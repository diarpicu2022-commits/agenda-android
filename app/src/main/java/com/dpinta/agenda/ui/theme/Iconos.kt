package com.dpinta.agenda.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.dpinta.agenda.R

/*
 * Iconografía. Contrato de diseño, cláusula C7.
 * Material Symbols Sharp (Apache 2.0), instancia estática FILL 0, GRAD 0, opsz 24, wght 500,
 * recortada con fontTools a la lista cerrada de C7.3 (15 glifos, 3 KB). Licencia en docs/licencias.
 * Un icono nuevo exige añadir su glifo de la MISMA familia y variante al subconjunto.
 */
private val MaterialSymbolsSharp = FontFamily(Font(R.font.material_symbols_sharp_c7))

/** Lista cerrada de C7.3, con el código del glifo en Material Symbols. */
enum class Icono(internal val glifo: Char) {
    Hoy(''),
    Semana(''),
    Lista(''),
    Anadir(''),
    Ajustes(''),
    Ubicacion(''),
    Bus(''),
    APie(''),
    Carro(''),
    Moto(''),
    Aviso(''),
    Deshacer(''),
    Cerrar(''),
    Atras(''),
}

/**
 * Dibuja un icono de la lista cerrada. Es decorativo por defecto (sin semántica):
 * el texto o la acción que lo acompaña es quien lo nombra para TalkBack.
 * El tamaño no escala con la fuente del sistema: es un icono, no texto (C7.2).
 */
@Composable
fun IconoAgenda(
    icono: Icono,
    color: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = AgendaMedidas.icono,
) {
    // Dp.toSp ya descuenta fontScale: el glifo mide exactamente [tamano] con cualquier escala.
    val tamanoFuente = with(LocalDensity.current) { tamano.toSp() }
    Text(
        text = icono.glifo.toString(),
        color = color,
        style = TextStyle(
            fontFamily = MaterialSymbolsSharp,
            fontSize = tamanoFuente,
            lineHeight = tamanoFuente,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier
            .size(tamano)
            .clearAndSetSemantics { },
    )
}
