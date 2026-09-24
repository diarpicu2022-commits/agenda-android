package com.dpinta.agenda.data.db

import android.content.Context
import androidx.room.Room
import com.dpinta.agenda.data.security.DatabaseKeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AgendaDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(DatabaseKeyManager(context).passphrase())
        // Sin fallbackToDestructiveMigration: un cambio de esquema sin Migration debe fallar,
        // nunca borrar los datos de Diego.
        return Room.databaseBuilder(context, AgendaDatabase::class.java, AgendaDatabase.NOMBRE)
            .openHelperFactory(factory)
            .addMigrations(*AgendaDatabase.MIGRACIONES)
            .build()
    }

    @Provides
    fun agendaDao(db: AgendaDatabase): AgendaDao = db.agendaDao()
}
