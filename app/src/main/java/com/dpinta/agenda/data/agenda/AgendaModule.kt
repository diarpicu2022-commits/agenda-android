package com.dpinta.agenda.data.agenda

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reloj de la app. En producción es el del sistema; el desfase solo lo cambia la app
 * depurable (extra «hora» del intent) para capturas reproducibles.
 */
@Singleton
class RelojAjustable @Inject constructor() : Clock() {
    private val sistema: Clock = Clock.systemDefaultZone()

    @Volatile var desfase: Duration = Duration.ZERO

    override fun getZone(): ZoneId = sistema.zone
    override fun withZone(zone: ZoneId): Clock = Clock.offset(Clock.system(zone), desfase)
    override fun instant(): Instant = sistema.instant() + desfase
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AgendaModule {
    @Binds abstract fun repositorio(impl: AgendaRoom): AgendaRepository

    companion object {
        @Provides @Singleton fun reloj(reloj: RelojAjustable): Clock = reloj

        /** Perfil de la cuenta local como flujo (Hoy lo lee para el saludo; las pruebas pasan uno fijo). */
        @Provides fun perfil(local: com.dpinta.agenda.data.perfil.PerfilLocal): kotlinx.coroutines.flow.StateFlow<com.dpinta.agenda.data.perfil.Perfil> = local.perfil
    }
}
