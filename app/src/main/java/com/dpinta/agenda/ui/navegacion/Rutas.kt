package com.dpinta.agenda.ui.navegacion

import com.dpinta.agenda.ui.theme.Icono
import kotlinx.serialization.Serializable

/** Rutas tipadas (Navigation Compose). */
@Serializable data object RutaHoy

@Serializable data object RutaSemana

@Serializable data object RutaActividades

@Serializable data object RutaAjustes

/** Tiempos de trayecto escritos a mano para un lugar guardado. */
@Serializable data class RutaLugar(val id: Long)

/** Crear (id = −1) o editar un semestre (Ajustes). */
@Serializable data class RutaSemestre(val id: Long = -1L)

/**
 * Crear (id = −1) o editar una actividad. [escenario] solo lo usa la app depurable para
 * capturas reproducibles (vacío en producción).
 */
@Serializable data class RutaFormulario(val id: Long = -1L, val escenario: String = "")

/** Elegir el lugar en el mapa: esqueleto «pendiente de Maps». */
@Serializable data object RutaMapa

/** Solo depuración: se enlaza desde Ajustes únicamente si la app es depurable. */
@Serializable data object RutaDemoBanda
@Serializable data object RutaPrivacidad

/** Los tres destinos de la barra inferior (anexo §7). */
enum class Pestana(val etiqueta: String, @param:androidx.annotation.StringRes val texto: Int, val icono: Icono, val ruta: Any) {
    Hoy("Hoy", com.dpinta.agenda.R.string.pestana_hoy, Icono.Hoy, RutaHoy),
    Semana("Semana", com.dpinta.agenda.R.string.pestana_semana, Icono.Semana, RutaSemana),
    Actividades("Actividades", com.dpinta.agenda.R.string.pestana_actividades, Icono.Lista, RutaActividades),
}
