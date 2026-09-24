package com.dpinta.agenda.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radio 0 como CornerBasedShape (Material 3 lo exige en Shapes). */
private val RadioCero = RoundedCornerShape(0.dp)

/**
 * Formas. Contrato de diseño, cláusula C5.1: radio 0 en todo.
 * Se sobrescriben las cinco formas de Material 3 para que ningún componente estándar
 * (hojas, menús, snackbar, campos, botones) traiga esquinas redondeadas.
 */
val AgendaShapes = Shapes(
    extraSmall = RadioCero,
    small = RadioCero,
    medium = RadioCero,
    large = RadioCero,
    extraLarge = RadioCero,
)

/** Única excepción (C5.4): el punto de la línea de «ahora» marca un instante, no un bloque. */
val FormaPuntoAhora = CircleShape
