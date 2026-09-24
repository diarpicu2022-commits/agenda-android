package com.dpinta.agenda.data.db

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dpinta.agenda.data.agenda.ActividadAGuardar
import com.dpinta.agenda.data.agenda.AgendaRoom
import com.dpinta.agenda.data.agenda.Cuando
import com.dpinta.agenda.data.agenda.festivosPropuestos
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.ScheduleExpander
import com.dpinta.agenda.domain.SessionException
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.TravelEstimate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Room en memoria (sin SQLCipher) con el esquema v1 real: DAO + repositorio. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class AgendaRoomTest {

    private lateinit var db: AgendaDatabase
    private lateinit var repo: AgendaRoom
    private val zona = ZoneId.of("America/Bogota")
    private val reloj = Clock.fixed(Instant.parse("2026-09-24T12:00:00Z"), zona)
    private val hoy = LocalDate.of(2026, 9, 24)

    @Before fun abrir() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AgendaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = AgendaRoom(db.agendaDao(), reloj)
    }

    @After fun cerrar() = db.close()

    private fun calculo(id: Long? = null, salon: String = "B-204", lugar: String = "Campus") = ActividadAGuardar(
        id, "Cálculo diferencial", ActivityKind.CLASE,
        Cuando.Semanal(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), LocalTime.of(8, 0), LocalTime.of(10, 0), hoy, hoy.plusWeeks(17)),
        lugar, salon, TransportMode.TRANSPORTE_PUBLICO, Duration.ofMinutes(7), Duration.ofMinutes(15),
    )

    @Test fun guardaLeeYReutilizaElLugar() = runTest {
        val id = repo.guardar(calculo())
        repo.guardar(calculo().copy(titulo = "Física", lugar = "  campus "))
        val a = repo.agenda().first()
        assertEquals(2, a.actividades.size)
        assertEquals(1, a.lugares.size) // «  campus » es el mismo lugar
        val act = a.actividades.getValue(id)
        assertEquals("B-204", act.salon)
        assertEquals(Duration.ofMinutes(7), act.margen)
        val regla = a.reglas.single { it.activityId == id }
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), regla.days)
        assertEquals(LocalTime.of(8, 0), regla.start)
    }

    @Test fun editarSustituyeLaSerieYBorrarLoQuitaTodo() = runTest {
        val id = repo.guardar(calculo())
        repo.guardar(calculo(id = id, salon = "B-210").copy(cuando = Cuando.Puntual(hoy, LocalTime.of(9, 0), LocalTime.of(11, 0))))
        var a = repo.agenda().first()
        assertEquals("B-210", a.actividades.getValue(id).salon)
        assertTrue(a.reglas.none { it.activityId == id })
        assertEquals(hoy.atTime(9, 0), a.puntuales.single().start)
        repo.eliminar(id)
        a = repo.agenda().first()
        assertTrue(a.actividades.isEmpty() && a.puntuales.isEmpty())
    }

    @Test fun excepcionesYSemestreConFestivosLlegamAlDominio() = runTest {
        val id = repo.guardar(calculo())
        val lunes = LocalDate.of(2026, 9, 28)
        repo.guardarExcepcion(SessionException.Cancelled(id, lunes))
        val festivos = festivosPropuestos(hoy, hoy.plusWeeks(17))
        assertTrue(LocalDate.of(2026, 10, 12) in festivos) // Día de la Raza (lunes festivo)
        repo.guardarSemestre("2026-2", hoy, hoy.plusWeeks(17), festivos)
        val a = repo.agenda().first()
        val sem = a.semestreEn(hoy)!!
        assertEquals(festivos.keys, sem.daysOff)
        val sesiones = ScheduleExpander.expand(a.reglas, a.puntuales, { null }, hoy, hoy.plusWeeks(4), sem, a.excepciones)
        assertTrue(sesiones.none { it.start.toLocalDate() == lunes })
        assertTrue(sesiones.none { it.start.toLocalDate() == LocalDate.of(2026, 10, 12) })
        repo.quitarExcepcion(id, lunes)
        assertTrue(repo.agenda().first().excepciones.isEmpty())
    }

    @Test fun estimacionesYTraslados() = runTest {
        val id = repo.guardar(calculo())
        val lugar = repo.agenda().first().actividades.getValue(id).lugarId!!
        assertNull(repo.estimacion(lugar, TransportMode.CARRO))
        repo.guardarEstimacion(lugar, TravelEstimate(Duration.ofMinutes(14), TransportMode.CARRO, reloj.instant(), false))
        assertEquals(Duration.ofMinutes(14), repo.estimacion(lugar, TransportMode.CARRO)!!.duration)
        val otro = repo.guardar(calculo().copy(titulo = "Turno", lugar = "Tienda"))
        val tienda = repo.agenda().first().actividades.getValue(otro).lugarId!!
        repo.guardarTraslado(lugar, tienda, 35)
        assertEquals(35L, repo.agenda().first().minutosEntre(lugar, tienda))
    }
}
