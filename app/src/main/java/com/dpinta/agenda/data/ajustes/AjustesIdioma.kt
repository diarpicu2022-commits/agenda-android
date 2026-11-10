package com.dpinta.agenda.data.ajustes

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** Idioma de la app elegido en Ajustes (pedido del profesor: español o inglés). Por defecto, el del sistema. */
enum class IdiomaApp(val etiqueta: String?) { ES("es"), EN("en"), SISTEMA(null) }

/**
 * Preferencia de idioma. No es un dato personal: va en preferencias, como el tema.
 *
 * Desde Android 13 se guarda también en el sistema (LocaleManager), así el idioma por app de Ajustes del teléfono y
 * el de Agenda son el mismo. En Android 10 a 12 la actividad, los avisos y el widget envuelven su contexto con
 * [contexto]. Solo hay dos idiomas: lo que no sea inglés se ve en español.
 */
object Idioma {
    private const val PREFS = "ajustes_idioma"
    private const val CLAVE = "idioma"

    fun elegido(c: Context): IdiomaApp {
        if (Build.VERSION.SDK_INT >= 33) {
            val sistema = c.getSystemService(LocaleManager::class.java).applicationLocales
            if (!sistema.isEmpty) return if (sistema[0].language == "en") IdiomaApp.EN else IdiomaApp.ES
            return IdiomaApp.SISTEMA
        }
        val guardado = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE, null)
        return guardado?.let { runCatching { IdiomaApp.valueOf(it) }.getOrNull() } ?: IdiomaApp.SISTEMA
    }

    fun guardar(c: Context, idioma: IdiomaApp) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(CLAVE, idioma.name) }
        if (Build.VERSION.SDK_INT >= 33) {
            c.getSystemService(LocaleManager::class.java).applicationLocales =
                idioma.etiqueta?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        }
    }

    /** El idioma en que se ve la app ahora: el elegido, o el del sistema si es inglés, o español. */
    fun locale(c: Context): Locale {
        val etiqueta = elegido(c).etiqueta
            ?: if (sistemaEnIngles()) "en" else "es"
        return Locale.forLanguageTag(etiqueta)
    }

    /** El idioma de las frases armadas en código, para avisos y widget. */
    fun lengua(c: Context): com.dpinta.agenda.ui.components.Lengua =
        com.dpinta.agenda.ui.components.Lengua.de(locale(c).language)

    private fun sistemaEnIngles(): Boolean =
        android.content.res.Resources.getSystem().configuration.locales[0].language == "en"

    /** Contexto con el idioma de la app (actividad en Android 10 a 12, avisos y widget en todas las versiones). */
    fun contexto(c: Context): Context {
        val locale = locale(c)
        if (c.resources.configuration.locales[0] == locale) return c
        val config = Configuration(c.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return c.createConfigurationContext(config)
    }
}
