package com.dpinta.agenda.data.perfil

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Perfil de la cuenta local: solo en este teléfono (sin servidor, sin copia en la nube: allowBackup = false). */
data class Perfil(val nombre: String = "", val carrera: String = "", val universidad: String = "") {
    /** Primer nombre para el saludo («Buenos días, Diego»), o null si no lo ha escrito. */
    val primerNombre: String? get() = nombre.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() }
}

@Singleton
class PerfilLocal @Inject constructor(@param:ApplicationContext contexto: Context) {
    private val preferencias = contexto.getSharedPreferences("perfil_local", Context.MODE_PRIVATE)
    private val _perfil = MutableStateFlow(leer())
    val perfil: StateFlow<Perfil> = _perfil.asStateFlow()

    private fun leer() = Perfil(
        nombre = preferencias.getString(NOMBRE, "").orEmpty(),
        carrera = preferencias.getString(CARRERA, "").orEmpty(),
        universidad = preferencias.getString(UNIVERSIDAD, "").orEmpty(),
    )

    fun guardar(p: Perfil) {
        val limpio = Perfil(p.nombre.trim().take(60), p.carrera.trim().take(80), p.universidad.trim().take(80))
        preferencias.edit {
            putString(NOMBRE, limpio.nombre)
            putString(CARRERA, limpio.carrera)
            putString(UNIVERSIDAD, limpio.universidad)
        }
        _perfil.value = limpio
    }

    /** Derecho de supresión: borra el perfil del teléfono. */
    fun borrar() {
        preferencias.edit { clear() }
        _perfil.value = Perfil()
    }

    private companion object {
        const val NOMBRE = "nombre"
        const val CARRERA = "carrera"
        const val UNIVERSIDAD = "universidad"
    }
}
