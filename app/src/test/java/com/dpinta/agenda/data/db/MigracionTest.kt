package com.dpinta.agenda.data.db

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dpinta.agenda.domain.TransportMode
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * La migración 1 → 2 conserva los datos: se crea una base v1 real con el SQL que Room exportó en
 * `schemas/…/1.json`, se mete un trayecto y se abre con la versión actual.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class MigracionTest {

    @Test fun deUnoADosConservaLosTrayectos() = runTest {
        val contexto = ApplicationProvider.getApplicationContext<Application>()
        val archivo = contexto.getDatabasePath("migracion.db").apply { parentFile?.mkdirs(); delete() }
        val esquema = JSONObject(File("schemas/com.dpinta.agenda.data.db.AgendaDatabase/1.json").readText()).getJSONObject("database")

        SQLiteDatabase.openOrCreateDatabase(archivo, null).use { v1 ->
            val tablas = esquema.getJSONArray("entities")
            for (i in 0 until tablas.length()) {
                val t = tablas.getJSONObject(i)
                v1.execSQL(t.getString("createSql").replace("\${TABLE_NAME}", t.getString("tableName")))
                val indices = t.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    v1.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", t.getString("tableName")))
                }
            }
            val setup = esquema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) v1.execSQL(setup.getString(i))
            v1.execSQL("INSERT INTO lugar (id, nombre, nombre_clave) VALUES (1, 'Campus', 'campus')")
            v1.execSQL("INSERT INTO trayecto (lugar_id, modo, minutos, calculado_en) VALUES (1, 'TRANSPORTE_PUBLICO', 23, 0)")
            v1.version = 1
        }

        val db = Room.databaseBuilder(contexto, AgendaDatabase::class.java, archivo.absolutePath)
            .addMigrations(*AgendaDatabase.MIGRACIONES)
            .allowMainThreadQueries()
            .build()
        val trayecto = db.agendaDao().trayecto(1, TransportMode.TRANSPORTE_PUBLICO)!!
        assertEquals(23, trayecto.minutos)
        assertFalse(trayecto.manual)
        db.close()
    }
}
