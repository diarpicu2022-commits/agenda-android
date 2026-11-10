package com.dpinta.agenda.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.time.format.DateTimeFormatter
import java.util.Locale

/** El idioma en que se ve la app ahora (el elegido en Ajustes o el del sistema). */
@Composable
fun localeApp(): Locale = LocalConfiguration.current.locales[0]

/**
 * Formato de fecha cuyo patrón vive en los recursos: el orden cambia con el idioma («d 'de' MMMM» frente a «MMMM d»),
 * así que el patrón se traduce junto con el texto.
 */
@Composable
fun formatoFecha(@StringRes patron: Int): DateTimeFormatter {
    val texto = stringResource(patron)
    val locale = localeApp()
    return remember(texto, locale) { DateTimeFormatter.ofPattern(texto, locale) }
}

/** La actividad detrás de un contexto envuelto (para recrearla al cambiar de idioma en Android 10 a 12). */
tailrec fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}
