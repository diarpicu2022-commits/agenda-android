package com.dpinta.agenda

import android.app.Application
import com.dpinta.agenda.avisos.Canal
import com.dpinta.agenda.avisos.ProgramadorAvisos
import com.dpinta.agenda.data.agenda.AgendaRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AgendaApp : Application() {

    @Inject lateinit var repositorio: AgendaRepository
    @Inject lateinit var programador: ProgramadorAvisos

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Canal.registrar(this)
        // Cada cambio de la agenda (crear, editar, cancelar una sesión) rehace las alarmas.
        alcance.launch {
            repositorio.agenda().distinctUntilChanged().collectLatest { programador.reprogramar() }
        }
    }
}
