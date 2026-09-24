package com.dpinta.agenda.ui.navegacion

import com.dpinta.agenda.ui.theme.Icono
import kotlinx.serialization.Serializable

/** Rutas tipadas (Navigation Compose). */
@Serializable data object RutaHoy

@Serializable data object RutaSemana

@Serializable data object RutaActividades

@Serializable data object RutaAjustes

/**
 * Crear (id = −1) o editar una actividad. [escenario] solo lo usa la app depurable para
 * capturas reproducibles (vacío en producción).
 */
@Serializable data class RutaFormulario(val id: Long = -1L, val escenario: String = "")

/** Elegir el lugar en el mapa: esqueleto «pendiente de Maps». */
@Serializable data object RutaMapa

/** Solo depuración: se enlaza desde Ajustes únicamente si la app es depurable. */
@Serializable data object RutaDemoBanda

/** Los tres destinos de la barra inferior (anexo §7). */
enum class Pestana(val etiqueta: String, val icono: Icono, val ruta: Any) {
    Hoy("Hoy", Icono.Hoy, RutaHoy),
    Semana("Semana", Icono.Semana, RutaSemana),
    Actividades("Actividades", Icono.Lista, RutaActividades),
}
