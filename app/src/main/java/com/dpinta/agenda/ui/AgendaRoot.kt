package com.dpinta.agenda.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dpinta.agenda.ui.actividades.ActividadesRuta
import com.dpinta.agenda.ui.ajustes.AjustesRuta
import com.dpinta.agenda.ui.formulario.FormularioRuta
import com.dpinta.agenda.ui.formulario.MapaPendientePantalla
import com.dpinta.agenda.ui.debug.DemoBandaPantalla
import com.dpinta.agenda.ui.hoy.HoyRuta
import com.dpinta.agenda.ui.navegacion.BarraInferior
import com.dpinta.agenda.ui.navegacion.Pestana
import com.dpinta.agenda.ui.navegacion.RutaActividades
import com.dpinta.agenda.ui.navegacion.RutaAjustes
import com.dpinta.agenda.ui.navegacion.RutaSemestre
import com.dpinta.agenda.ui.navegacion.RutaLugar
import com.dpinta.agenda.ui.lugar.LugarRuta
import com.dpinta.agenda.ui.semestre.SemestreRuta
import com.dpinta.agenda.ui.navegacion.RutaFormulario
import com.dpinta.agenda.ui.navegacion.RutaMapa
import com.dpinta.agenda.ui.navegacion.RutaDemoBanda
import com.dpinta.agenda.ui.navegacion.RutaHoy
import com.dpinta.agenda.ui.navegacion.RutaSemana
import com.dpinta.agenda.ui.semana.SemanaRuta
import com.dpinta.agenda.ui.theme.AgendaTheme

/**
 * Raíz: navegación con rutas tipadas y barra inferior (anexo §7).
 * Nada se anima al cambiar de destino (C8.3). La barra solo está en los tres destinos
 * principales; Ajustes, Crear y la demostración son pantallas con «Volver».
 *
 * @param pestanaInicial solo la cambian las compilaciones depurables (extra del intent).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AgendaRoot(
    pestanaInicial: Pestana = Pestana.Hoy,
    escenarioFormulario: String = "",
    raiz: RaizViewModel = hiltViewModel(),
) {
    AgendaTheme {
        val nav = rememberNavController()
        val avisos = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            if (escenarioFormulario.isNotBlank()) nav.navigate(RutaFormulario(escenario = escenarioFormulario))
        }
        LaunchedEffect(raiz) {
            raiz.guardados.collect { g ->
                val r = avisos.showSnackbar(
                    message = "Guardado: ${g.titulo}",
                    actionLabel = if (g.nueva) "Deshacer" else null,
                    duration = SnackbarDuration.Short,
                )
                if (r == SnackbarResult.ActionPerformed) raiz.deshacer(g)
            }
        }
        val entrada by nav.currentBackStackEntryAsState()
        val actual = Pestana.entries.firstOrNull { p ->
            entrada?.destination?.hierarchy?.any { it.hasRoute(p.ruta::class) } == true
        }
        val alCrear: () -> Unit = { nav.navigate(RutaFormulario()) }
        val alEditar: (Long) -> Unit = { id -> nav.navigate(RutaFormulario(id = id)) }
        Column(
            Modifier
                .fillMaxSize()
                .background(AgendaTheme.colores.papel)
                .semantics { testTagsAsResourceId = true },
        ) {
            Box(Modifier.weight(1f)) {
            NavHost(
                navController = nav,
                startDestination = pestanaInicial.ruta,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable<RutaHoy> { HoyRuta(onAjustes = { nav.navigate(RutaAjustes) }, onCrear = alCrear, onEditar = alEditar) }
                composable<RutaSemana> { SemanaRuta(onCrear = alCrear, onEditar = alEditar) }
                composable<RutaActividades> { ActividadesRuta(onCrear = alCrear, onEditar = alEditar, onLugar = { nav.navigate(RutaLugar(it)) }) }
                composable<RutaLugar> { LugarRuta(onCerrar = { nav.popBackStack() }) }
                composable<RutaAjustes> {
                    AjustesRuta(
                        onAtras = { nav.popBackStack() },
                        onSemestre = { id -> nav.navigate(RutaSemestre(id ?: -1L)) },
                        onDemoBanda = { nav.navigate(RutaDemoBanda) },
                        onLugares = { nav.navigate(RutaActividades) },
                    )
                }
                composable<RutaSemestre> { SemestreRuta(onCerrar = { nav.popBackStack() }) }
                composable<RutaFormulario> {
                    FormularioRuta(onCerrar = { nav.popBackStack() }, onMapa = { nav.navigate(RutaMapa) })
                }
                composable<RutaMapa> { MapaPendientePantalla(onVolver = { nav.popBackStack() }) }
                composable<RutaDemoBanda> { DemoBandaPantalla(onAtras = { nav.popBackStack() }) }
            }
            // «Guardado» con «Deshacer» (anexo §7, flujo 1): radio 0 y colores inversos del tema.
            SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter))
            }
            if (actual != null) {
                BarraInferior(
                    actual = actual,
                    onPestana = { p ->
                        nav.navigate(p.ruta) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCrear = alCrear,
                )
            }
        }
    }
}
