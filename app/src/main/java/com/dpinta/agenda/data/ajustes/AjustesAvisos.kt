package com.dpinta.agenda.data.ajustes

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Resumen matutino (arquitectura P2.8: «a la hora que elijas»). */
data class AjusteResumen(val activo: Boolean = true, val hora: LocalTime = LocalTime.of(6, 0))

/** Ajustes de avisos. No son datos personales sensibles: van en preferencias, no en la base cifrada. */
@Singleton
class AjustesAvisos @Inject constructor(@param:ApplicationContext contexto: Context) {

    private val preferencias = contexto.getSharedPreferences("ajustes_avisos", Context.MODE_PRIVATE)
    private val _resumen = MutableStateFlow(leer())
    val resumen: StateFlow<AjusteResumen> = _resumen.asStateFlow()

    private fun leer() = AjusteResumen(
        activo = preferencias.getBoolean(ACTIVO, true),
        hora = LocalTime.ofSecondOfDay(preferencias.getInt(HORA, 6 * 3600).toLong()),
    )

    fun guardar(ajuste: AjusteResumen) {
        preferencias.edit {
            putBoolean(ACTIVO, ajuste.activo)
            putInt(HORA, ajuste.hora.withSecond(0).withNano(0).toSecondOfDay())
        }
        _resumen.value = ajuste
    }

    private companion object {
        const val ACTIVO = "resumen_activo"
        const val HORA = "resumen_hora"
    }
}
