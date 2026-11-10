package com.dpinta.agenda.ui.components

/**
 * Idioma de las frases que se arman en código puro (banda, boleto, avisos, widget, TalkBack). Esas frases llevan cifras,
 * plurales y orden de palabras propios de cada idioma y se prueban en la JVM sin Android, así que el par español/inglés
 * va junto en el código en vez de en `strings.xml`. Los rótulos fijos de pantalla sí van en recursos.
 *
 * En la UI se obtiene con `lengua()` (Formatos.kt); fuera de la UI, con `Idioma.lengua(contexto)`.
 */
enum class Lengua {
    ES, EN;

    /** El texto de este idioma: `l("Sal ya", "Leave now")`. */
    operator fun invoke(es: String, en: String): String = if (this == EN) en else es

    companion object {
        fun de(etiquetaIdioma: String?): Lengua = if (etiquetaIdioma == "en") EN else ES
    }
}
