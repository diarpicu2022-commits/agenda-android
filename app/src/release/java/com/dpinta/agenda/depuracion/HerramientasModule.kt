package com.dpinta.agenda.depuracion

import com.dpinta.agenda.data.agenda.HerramientasDepuracion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Release: sin semilla ni escenarios. La agenda empieza vacía y se llena con lo que Diego guarde. */
@Module
@InstallIn(SingletonComponent::class)
object HerramientasModule {
    @Provides fun herramientas(): HerramientasDepuracion = object : HerramientasDepuracion {
        override suspend fun alArrancar(ejemplo: String?) = Unit
    }
}
