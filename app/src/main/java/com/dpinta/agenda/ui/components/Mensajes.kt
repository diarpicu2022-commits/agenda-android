package com.dpinta.agenda.ui.components

/**
 * Textos que el ViewModel o el dominio guardan en español (errores de Semestre, «Sin clase», nombres de festivos) y que
 * la pantalla traduce al mostrarlos. Lo que no está en la tabla, como un motivo escrito por la persona, se deja igual.
 */
object Mensajes {
    private val ingles = mapOf(
        // Semestre
        "Escribe la fecha como 12/10" to "Write the date like 12/10 (day/month)",
        "Ese día no está dentro del semestre" to "That day isn't within the semester",
        "Sin clase" to "No class",
        "Ponle un nombre, por ejemplo 2026-2" to "Give it a name, for example 2026-2",
        "Escribe la fecha como 3/8" to "Write the date like 3/8 (day/month)",
        "Escribe la fecha como 28/11" to "Write the date like 28/11 (day/month)",
        "Termina antes de empezar" to "It ends before it starts",
        // Festivos de Colombia: el nombre oficial se conserva y la traducción va entre paréntesis.
        "Año Nuevo" to "Año Nuevo (New Year's Day)",
        "Jueves Santo" to "Jueves Santo (Maundy Thursday)",
        "Viernes Santo" to "Viernes Santo (Good Friday)",
        "Día del Trabajo" to "Día del Trabajo (Labour Day)",
        "Día de la Independencia" to "Día de la Independencia (Independence Day)",
        "Batalla de Boyacá" to "Batalla de Boyacá (Battle of Boyacá)",
        "Inmaculada Concepción" to "Inmaculada Concepción (Immaculate Conception)",
        "Navidad" to "Navidad (Christmas)",
        "Reyes Magos" to "Reyes Magos (Epiphany)",
        "San José" to "San José (Saint Joseph's Day)",
        "Ascensión del Señor" to "Ascensión del Señor (Ascension Day)",
        "Corpus Christi" to "Corpus Christi",
        "Sagrado Corazón" to "Sagrado Corazón (Sacred Heart)",
        "San Pedro y San Pablo" to "San Pedro y San Pablo (Saints Peter and Paul)",
        "Asunción de la Virgen" to "Asunción de la Virgen (Assumption Day)",
        "Día de la Raza" to "Día de la Raza (Columbus Day)",
        "Todos los Santos" to "Todos los Santos (All Saints' Day)",
        "Independencia de Cartagena" to "Independencia de Cartagena (Cartagena Independence Day)",
    )

    fun de(texto: String, l: Lengua): String {
        if (l == Lengua.ES) return texto
        if (texto.startsWith("Festivo: ")) return "Holiday: " + de(texto.removePrefix("Festivo: "), l)
        return ingles[texto] ?: texto
    }

    /** Las claves en español, para la prueba que comprueba que la tabla cubre todos los festivos. */
    val claves: Set<String> get() = ingles.keys
}
