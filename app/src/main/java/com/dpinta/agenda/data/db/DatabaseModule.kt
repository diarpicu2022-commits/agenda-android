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
        return Room.databaseBuilder(context, AgendaDatabase::class.java, "agenda.db")
            .openHelperFactory(factory)
            .build()
    }

    @Provides
    fun placeDao(db: AgendaDatabase): PlaceDao = db.placeDao()
}
