package com.dpinta.agenda.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas del sistema Agenda + CampusWatch (README · «Las formas dependen del objeto»; enmienda 2026-10-07):
 * ficha 6 (chips, insignias, campos), bloque 14 (bloques de actividad, ítems, avisos), lámina 24 (tarjetas
 * protagonistas y hojas inferiores), pastilla (botones, interruptores, selector de día, rótulo Ahora).
 * Círculos solo para estaciones, el anillo de cuenta y el marcador Ahora.
 */
object Formas {
    val ficha = RoundedCornerShape(6.dp)
    val bloque = RoundedCornerShape(14.dp)
    val lamina = RoundedCornerShape(24.dp)
    val laminaArriba = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val pastilla = RoundedCornerShape(percent = 50)
    val estacion = CircleShape
}

/** Material 3 hereda las formas del sistema (campos = ficha, menús y avisos = bloque, hojas = lámina). */
val AgendaShapes = Shapes(
    extraSmall = Formas.ficha,
    small = Formas.ficha,
    medium = Formas.bloque,
    large = Formas.lamina,
    extraLarge = Formas.lamina,
)

/** El punto de la línea de «ahora» marca un instante: círculo. */
val FormaPuntoAhora = CircleShape
