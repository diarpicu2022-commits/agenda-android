package com.dpinta.agenda.data.agenda

/**
 * Enganche para las compilaciones de depuración (semilla de datos de ejemplo y escenarios para
 * capturas). En release es una implementación vacía (src/release); en debug, src/debug.
 */
interface HerramientasDepuracion {
    /** @param ejemplo valor del extra «ejemplo» del intent («cruce» = datos con un solape), o null. */
    suspend fun alArrancar(ejemplo: String?)
}
