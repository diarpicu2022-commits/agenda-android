package com.dpinta.agenda.data.ajustes

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Tema de la app elegido en Ajustes (pedido del profesor: claro u oscuro a elección). Por defecto, el del sistema. */
enum class TemaApp { CLARO, OSCURO, SISTEMA }

/** Preferencia de tema. No es un dato personal: va en preferencias, no en la base cifrada (igual que [AjustesAvisos]). */
@Singleton
class AjustesTema @Inject constructor(@param:ApplicationContext private val contexto: Context) {
    private val preferencias = contexto.getSharedPreferences("ajustes_tema", Context.MODE_PRIVATE)
    private val _tema = MutableStateFlow(
        preferencias.getString(CLAVE, null)?.let { runCatching { TemaApp.valueOf(it) }.getOrNull() } ?: TemaApp.SISTEMA,
    )
    val tema: StateFlow<TemaApp> = _tema.asStateFlow()

    fun guardar(t: TemaApp) {
        preferencias.edit { putString(CLAVE, t.name) }
        _tema.value = t
    }

    /** Si la app se ve en oscuro ahora mismo (lo que «Como el teléfono» del reloj debe seguir). */
    fun oscuroEfectivo(): Boolean = when (_tema.value) {
        TemaApp.CLARO -> false
        TemaApp.OSCURO -> true
        TemaApp.SISTEMA -> (contexto.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private companion object { const val CLAVE = "tema" }
}
