package com.dpinta.agenda.wear

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Idioma de CampusWatch: el que se eligió en Agenda (llega con el tema por /campuswatch/tema). Mientras no llegue, el
 * del reloj. Solo español o inglés: cualquier otro idioma del reloj se ve en español.
 */
object IdiomaReloj {
    /** Estado de Compose: al cambiar, las pantallas se vuelven a componer en el otro idioma. */
    var ingles by mutableStateOf(Resources.getSystem().configuration.locales[0].language == "en")
        private set

    fun cargar(c: Context) {
        ingles = (PreferenciaTema.idiomaTelefono(c) ?: Resources.getSystem().configuration.locales[0].language) == "en"
    }

    val locale: Locale get() = Locale.forLanguageTag(if (ingles) "en" else "es-CO")
}

/** El texto en el idioma de CampusWatch: `t("Sal ya", "Leave now")`. */
fun t(es: String, en: String): String = if (IdiomaReloj.ingles) en else es
