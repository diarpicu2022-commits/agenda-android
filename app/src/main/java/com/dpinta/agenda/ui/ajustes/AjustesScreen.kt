package com.dpinta.agenda.ui.ajustes

import com.dpinta.agenda.ui.components.actividad
import com.dpinta.agenda.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dpinta.agenda.data.agenda.AgendaRepository
import com.dpinta.agenda.data.agenda.SemestreGuardado
import com.dpinta.agenda.avisos.ProgramadorAvisos
import com.dpinta.agenda.data.ajustes.AjusteResumen
import com.dpinta.agenda.data.ajustes.AjustesAvisos
import com.dpinta.agenda.ui.components.CampoTexto
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp
import com.dpinta.agenda.ui.components.DivisorAjustes
import com.dpinta.agenda.ui.components.FilaInterruptor
import com.dpinta.agenda.ui.components.FilaAjuste
import com.dpinta.agenda.ui.components.GrupoAjustes
import com.dpinta.agenda.ui.components.NotaPrivacidad
import com.dpinta.agenda.ui.components.Opcion
import com.dpinta.agenda.ui.components.Segmentado
import com.dpinta.agenda.ui.components.banda.formatearHora
import com.dpinta.agenda.ui.components.banda.rememberEs24h
import com.dpinta.agenda.ui.formulario.Interprete
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dpinta.agenda.ui.components.BotonSubrayado
import com.dpinta.agenda.ui.components.Cabecera
import com.dpinta.agenda.ui.components.NotaPantalla
import com.dpinta.agenda.ui.components.anilloFoco
import com.dpinta.agenda.ui.theme.AgendaMedidas
import com.dpinta.agenda.ui.theme.AgendaSpacing
import com.dpinta.agenda.ui.theme.AgendaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val repositorio: AgendaRepository,
    private val ajustes: AjustesAvisos,
    private val programador: ProgramadorAvisos,
    private val perfilLocal: com.dpinta.agenda.data.perfil.PerfilLocal,
    private val ajustesTema: com.dpinta.agenda.data.ajustes.AjustesTema,
) : ViewModel() {
    val tema: StateFlow<com.dpinta.agenda.data.ajustes.TemaApp> = ajustesTema.tema

    /**
     * «Exportar mis datos» (derecho a conocer, Ley 1581): las sesiones de seis meses atrás a seis meses adelante como
     * eventos .ics que abre cualquier calendario, con salón y lugar, y la asistencia en las clases pasadas.
     */
    suspend fun exportarIcs(asististe: String, faltaste: String): String {
        val agenda = repositorio.agenda().first()
        val hoy = java.time.LocalDate.now()
        val marcas = repositorio.asistencias().first().associateBy { it.activityId to it.date }
        val eventos = programador.sesiones(agenda, hoy.minusMonths(6), hoy.plusMonths(6)).mapNotNull { s ->
            val act = agenda.actividades[s.activityId] ?: return@mapNotNull null
            val lugar = act.lugarId?.let { agenda.lugares[it]?.nombre }
            val donde = listOfNotNull(act.salon.ifBlank { null }, lugar).joinToString(" · ").ifBlank { null }
            val asistencia = when (marcas[s.activityId to s.start.toLocalDate()]?.mark) {
                com.dpinta.agenda.domain.AttendanceMark.ASISTI -> asististe
                com.dpinta.agenda.domain.AttendanceMark.FALTE -> faltaste
                null -> null
            }
            com.dpinta.agenda.domain.ics.IcsEvent(
                uid = "agenda-${s.activityId}-${s.start}@dpinta",
                summary = act.titulo,
                location = donde,
                description = asistencia,
                start = s.start,
                end = s.end,
            )
        }
        return com.dpinta.agenda.domain.ics.IcsWriter.write(eventos, java.time.ZoneId.systemDefault(), java.time.Instant.now())
    }

    /** Al cambiar el idioma se rehacen las alarmas: los avisos ya programados llevan su texto. */
    fun idiomaCambiado() {
        viewModelScope.launch { programador.reprogramar() }
    }

    /** Cambia el tema y vuelve a publicar el día: el reloj en «Como el teléfono» sigue este tema. */
    fun tema(t: com.dpinta.agenda.data.ajustes.TemaApp) {
        if (t == ajustesTema.tema.value) return
        ajustesTema.guardar(t)
        viewModelScope.launch { programador.reprogramar() }
    }

    val perfil: StateFlow<com.dpinta.agenda.data.perfil.Perfil> = perfilLocal.perfil

    fun perfil(p: com.dpinta.agenda.data.perfil.Perfil) = perfilLocal.guardar(p)

    val semestres: StateFlow<List<SemestreGuardado>?> =
        repositorio.semestres().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val resumen: StateFlow<AjusteResumen> = ajustes.resumen

    fun resumen(ajuste: AjusteResumen) {
        if (ajuste == ajustes.resumen.value) return
        ajustes.guardar(ajuste)
        viewModelScope.launch { programador.reprogramar() }
    }
}

/** «del 3 ago al 28 nov · 5 días sin clase» (sin «→»: Atkinson no lo tiene, C2.2). En inglés, «Aug 3 to Nov 28 · …». */
@Composable
internal fun resumenSemestre(s: SemestreGuardado): String {
    val formato = com.dpinta.agenda.ui.components.formatoFecha(R.string.fmt_dia_mes)
    fun LocalDate.corta() = formato.format(this).replace(".", "")
    val dias = if (s.diasSinClase.isEmpty()) stringResource(R.string.semestre_sin_libres)
    else pluralStringResource(R.plurals.semestre_dias_sin_clase, s.diasSinClase.size, s.diasSinClase.size)
    return stringResource(R.string.semestre_rango, s.inicio.corta(), s.fin.corta(), dias)
}

@Composable
fun AjustesRuta(
    onLugares: () -> Unit = {},
    onHoras: () -> Unit = {},
    onAtras: () -> Unit,
    onSemestre: (Long?) -> Unit,
    onDemoBanda: () -> Unit,
    onPrivacidad: () -> Unit = {},
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    // «Exportar mis datos»: el selector de archivos del sistema (sin permisos); se escribe donde la persona elija.
    val contexto = LocalContext.current
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    val asististe = stringResource(R.string.export_asististe)
    val faltaste = stringResource(R.string.export_faltaste)
    val exportar = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/calendar"),
    ) { uri ->
        if (uri != null) alcance.launch {
            val texto = viewModel.exportarIcs(asististe, faltaste)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                contexto.contentResolver.openOutputStream(uri)?.use { it.write(texto.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    val semestres by viewModel.semestres.collectAsStateWithLifecycle()
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val perfil by viewModel.perfil.collectAsStateWithLifecycle()
    val tema by viewModel.tema.collectAsStateWithLifecycle()
    // Idioma: desde Android 13 el sistema recrea la actividad; antes, se recrea a mano.
    var idioma by remember { mutableStateOf(com.dpinta.agenda.data.ajustes.Idioma.elegido(contexto)) }
    val archivo = stringResource(R.string.ajustes_exportar_archivo)
    AjustesPantalla(semestres, onAtras, onSemestre, onDemoBanda, resumen = resumen, onResumen = viewModel::resumen,
        perfil = perfil, onPerfil = viewModel::perfil, onLugares = onLugares, onHoras = onHoras, tema = tema, onTema = viewModel::tema, onPrivacidad = onPrivacidad,
        onExportar = { exportar.launch(archivo) },
        idioma = idioma,
        onIdioma = { nuevo ->
            if (nuevo != idioma) {
                idioma = nuevo
                com.dpinta.agenda.data.ajustes.Idioma.guardar(contexto, nuevo)
                viewModel.idiomaCambiado()
                if (android.os.Build.VERSION.SDK_INT < 33) contexto.actividad()?.recreate()
            }
        })
}

/** Ajustes (anexo §7). Por ahora: Semestre. En compilaciones depurables enlaza la demostración de la banda. */
@Composable
fun AjustesPantalla(
    semestres: List<SemestreGuardado>?,
    onAtras: () -> Unit,
    onSemestre: (Long?) -> Unit,
    onDemoBanda: () -> Unit,
    modifier: Modifier = Modifier,
    resumen: AjusteResumen = AjusteResumen(),
    onResumen: (AjusteResumen) -> Unit = {},
    perfil: com.dpinta.agenda.data.perfil.Perfil = com.dpinta.agenda.data.perfil.Perfil(),
    onPerfil: (com.dpinta.agenda.data.perfil.Perfil) -> Unit = {},
    onLugares: () -> Unit = {},
    onHoras: () -> Unit = {},
    tema: com.dpinta.agenda.data.ajustes.TemaApp = com.dpinta.agenda.data.ajustes.TemaApp.SISTEMA,
    onTema: (com.dpinta.agenda.data.ajustes.TemaApp) -> Unit = {},
    onPrivacidad: () -> Unit = {},
    onExportar: () -> Unit = {},
    idioma: com.dpinta.agenda.data.ajustes.IdiomaApp = com.dpinta.agenda.data.ajustes.IdiomaApp.SISTEMA,
    onIdioma: (com.dpinta.agenda.data.ajustes.IdiomaApp) -> Unit = {},
) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    val contexto = LocalContext.current
    val depurable = remember(contexto) { (contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    var confirmarBorrado by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(c.papel)) {
        Cabecera(stringResource(R.string.ajustes_titulo), onAtras = onAtras)
        // PantallaAjustes del sistema: nota de privacidad arriba y grupos Perfil, Agenda, Avisos y Datos.
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = m, vertical = AgendaSpacing.s8),
            verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s24),
        ) {
            NotaPrivacidad(stringResource(R.string.ajustes_privacidad_titulo), stringResource(R.string.ajustes_privacidad_cuerpo))
            TuPerfil(perfil, onPerfil)
            // Apariencia (pedido del profesor): claro u oscuro a elección, con el Segmentado del sistema.
            GrupoAjustes(stringResource(R.string.ajustes_apariencia)) {
                Column(Modifier.padding(16.dp)) {
                    com.dpinta.agenda.ui.components.Segmentado(
                        opciones = listOf(
                            com.dpinta.agenda.ui.components.Opcion(com.dpinta.agenda.data.ajustes.TemaApp.CLARO, stringResource(R.string.tema_claro)),
                            com.dpinta.agenda.ui.components.Opcion(com.dpinta.agenda.data.ajustes.TemaApp.OSCURO, stringResource(R.string.tema_oscuro)),
                            com.dpinta.agenda.ui.components.Opcion(com.dpinta.agenda.data.ajustes.TemaApp.SISTEMA, stringResource(R.string.tema_sistema)),
                        ),
                        seleccion = tema,
                        onSeleccion = onTema,
                        etiquetaPrueba = "tema",
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.ajustes_idioma), style = t.cuerpoFuerte, color = c.tinta)
                    Spacer(Modifier.height(8.dp))
                    com.dpinta.agenda.ui.components.Segmentado(
                        opciones = listOf(
                            com.dpinta.agenda.ui.components.Opcion(com.dpinta.agenda.data.ajustes.IdiomaApp.ES, stringResource(R.string.idioma_es)),
                            com.dpinta.agenda.ui.components.Opcion(com.dpinta.agenda.data.ajustes.IdiomaApp.EN, stringResource(R.string.idioma_en)),
                            com.dpinta.agenda.ui.components.Opcion(com.dpinta.agenda.data.ajustes.IdiomaApp.SISTEMA, stringResource(R.string.tema_sistema)),
                        ),
                        seleccion = idioma,
                        onSeleccion = onIdioma,
                        etiquetaPrueba = "idioma",
                    )
                }
            }
            GrupoAjustes(stringResource(R.string.ajustes_agenda)) {
                when {
                    semestres == null -> Unit
                    semestres.isEmpty() -> FilaAjuste(stringResource(R.string.ajustes_semestre), stringResource(R.string.ajustes_sin_semestre), ayuda = stringResource(R.string.ajustes_sin_semestre_ayuda)) { onSemestre(null) }
                    else -> semestres.forEachIndexed { i, s ->
                        if (i > 0) DivisorAjustes()
                        FilaAjuste(if (semestres.size == 1) stringResource(R.string.ajustes_semestre) else s.nombre, if (semestres.size == 1) s.nombre else null, ayuda = resumenSemestre(s)) { onSemestre(s.id) }
                    }
                }
                DivisorAjustes()
                FilaAjuste(stringResource(R.string.ajustes_anadir_semestre)) { onSemestre(null) }
                DivisorAjustes()
                FilaAjuste(stringResource(R.string.ajustes_lugares), ayuda = stringResource(R.string.ajustes_lugares_ayuda)) { onLugares() }
                DivisorAjustes()
                FilaAjuste(stringResource(R.string.ajustes_horas), ayuda = stringResource(R.string.ajustes_horas_ayuda)) { onHoras() }
            }
            ResumenMatutino(resumen, onResumen)
            GrupoAjustes(stringResource(R.string.ajustes_datos)) {
                FilaAjuste(stringResource(R.string.ajustes_exportar), ayuda = stringResource(R.string.ajustes_exportar_ayuda)) { onExportar() }
                FilaAjuste(stringResource(R.string.ajustes_politica), ayuda = stringResource(R.string.ajustes_politica_ayuda, com.dpinta.agenda.ui.privacidad.PoliticaPrivacidad.VERSION)) { onPrivacidad() }
                FilaAjuste(stringResource(R.string.ajustes_borrar), ayuda = stringResource(R.string.ajustes_borrar_ayuda), peligro = true) { confirmarBorrado = true }
                if (depurable) {
                    DivisorAjustes()
                    FilaAjuste(stringResource(R.string.ajustes_demo_banda), ayuda = stringResource(R.string.ajustes_demo_banda_ayuda)) { onDemoBanda() }
                }
            }
            Spacer(Modifier.height(AgendaSpacing.s24))
        }
    }
    if (confirmarBorrado) {
        val ds = AgendaTheme.ds
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            containerColor = ds.superficie,
            title = { Text(stringResource(R.string.ajustes_borrar_pregunta), style = t.encabezado, color = ds.tinta) },
            text = { Text(stringResource(R.string.ajustes_borrar_detalle), style = t.cuerpo, color = ds.tinta) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmarBorrado = false
                    // Derecho de supresión: Android borra todos los datos de la app (base cifrada, preferencias, alarmas).
                    contexto.getSystemService(android.app.ActivityManager::class.java).clearApplicationUserData()
                }) { Text(stringResource(R.string.ajustes_borrar_todo), style = t.cuerpoFuerte, color = ds.critico) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmarBorrado = false }) { Text(stringResource(R.string.cancelar), style = t.cuerpoFuerte, color = ds.ruta) }
            },
        )
    }
}

/**
 * Tu perfil (cuenta local): nombre para el saludo de Hoy y, si quieres, carrera y universidad. Se guarda solo en este
 * teléfono; se guarda al escribir.
 */
@Composable
private fun TuPerfil(perfil: com.dpinta.agenda.data.perfil.Perfil, onCambio: (com.dpinta.agenda.data.perfil.Perfil) -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    var nombre by rememberSaveable { mutableStateOf(perfil.nombre) }
    var carrera by rememberSaveable { mutableStateOf(perfil.carrera) }
    var universidad by rememberSaveable { mutableStateOf(perfil.universidad) }
    fun guardar() = onCambio(com.dpinta.agenda.data.perfil.Perfil(nombre, carrera, universidad))
    GrupoAjustes(stringResource(R.string.perfil_titulo)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(AgendaSpacing.s12)) {
            Text(stringResource(R.string.perfil_cuenta_local), style = t.meta, color = AgendaTheme.ds.tintaSuave)
            CampoTexto(stringResource(R.string.perfil_nombre), nombre, { nombre = it; guardar() }, error = null, ayuda = "Diego")
            CampoTexto(stringResource(R.string.perfil_carrera), carrera, { carrera = it; guardar() }, error = null, ayuda = stringResource(R.string.perfil_carrera_ejemplo))
            CampoTexto(stringResource(R.string.perfil_universidad), universidad, { universidad = it; guardar() }, error = null)
        }
    }
}

/** Resumen matutino (arquitectura P2.8): encendido o apagado y a qué hora llega. */
@Composable
private fun ResumenMatutino(ajuste: AjusteResumen, onCambio: (AjusteResumen) -> Unit) {
    val c = AgendaTheme.colores
    val t = AgendaTheme.tipo
    val m = AgendaTheme.reticula.margen
    val es24h = rememberEs24h()
    // Sin clave de la hora: guardar mientras se escribe no debe reescribir el campo.
    var texto by rememberSaveable { mutableStateOf(formatearHora(ajuste.hora, es24h).enLinea) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val errorHora = stringResource(R.string.error_hora)
    GrupoAjustes(stringResource(R.string.avisos_titulo)) {
        FilaInterruptor(
            stringResource(R.string.resumen_manana),
            if (ajuste.activo) stringResource(R.string.resumen_activo, formatearHora(ajuste.hora, es24h).enLinea) else stringResource(R.string.apagado),
            ajuste.activo,
        ) { onCambio(ajuste.copy(activo = it)) }
        if (ajuste.activo) {
            DivisorAjustes()
            Column(Modifier.padding(16.dp)) {
                CampoTexto(
                    etiqueta = stringResource(R.string.resumen_hora),
                    valor = texto,
                    onCambio = { nuevo ->
                        texto = nuevo
                        val hora = Interprete.hora(nuevo)
                        error = if (hora == null && nuevo.isNotBlank()) errorHora else null
                        if (hora != null) onCambio(ajuste.copy(hora = hora))
                    },
                    error = error,
                    ayuda = "6:00",
                )
            }
        }
    }
}

@Composable
private fun FilaSemestre(s: SemestreGuardado, onEditar: () -> Unit) {
    val c = AgendaTheme.colores
    val interaccion = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .anilloFoco(interaccion, c.tinta)
            .clickable(interactionSource = interaccion, indication = ripple(color = c.tinta), onClickLabel = stringResource(R.string.editar), role = Role.Button, onClick = onEditar),
    ) {
        Column(Modifier.heightIn(min = AgendaSpacing.s64).padding(horizontal = AgendaTheme.reticula.margen, vertical = AgendaSpacing.s12)) {
            Text(s.nombre, style = AgendaTheme.tipo.cuerpo, color = c.tinta)
            Text(resumenSemestre(s), style = AgendaTheme.tipo.meta, color = c.tinta)
        }
        HorizontalDivider(thickness = AgendaMedidas.filete, color = c.filete)
    }
}
