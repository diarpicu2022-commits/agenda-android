package com.dpinta.agenda.data.agenda

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Una actividad recién guardada, para el snackbar «Guardado» con «Deshacer» (anexo §7, flujo 1). */
data class Guardado(val id: Long, val titulo: String, val nueva: Boolean)

/**
 * Canal de la app para avisar de lo guardado. El formulario se cierra al guardar; quien muestra
 * el snackbar es la raíz, que sigue viva.
 */
@Singleton
class AvisosGuardado @Inject constructor() {
    private val canal = MutableSharedFlow<Guardado>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val avisos: SharedFlow<Guardado> = canal

    fun publicar(g: Guardado) {
        canal.tryEmit(g)
    }
}
