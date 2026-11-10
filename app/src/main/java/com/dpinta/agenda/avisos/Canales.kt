package com.dpinta.agenda.avisos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Canales de la tabla del anexo. Sin color propio (C3.5): la identidad va en la redacción. */
enum class Canal(val id: String, val nombre: String, val nombreEn: String, val importancia: Int) {
    Salida("salida", "Salida", "Departure", NotificationManager.IMPORTANCE_HIGH),
    SalYa("sal_ya", "Sal ya / Vas tarde", "Leave now / Running late", NotificationManager.IMPORTANCE_HIGH),
    Empieza("empieza", "Empieza", "Starts", NotificationManager.IMPORTANCE_DEFAULT),
    Resumen("resumen", "Resumen matutino", "Morning summary", NotificationManager.IMPORTANCE_LOW),
    ;

    companion object {
        /** Idempotente: crear un canal que ya existe solo actualiza su nombre (así sigue el idioma de la app). */
        fun registrar(contexto: Context) {
            val gestor = contexto.getSystemService(NotificationManager::class.java)
            val l = com.dpinta.agenda.data.ajustes.Idioma.lengua(contexto)
            gestor.createNotificationChannels(entries.map { NotificationChannel(it.id, l(it.nombre, it.nombreEn), it.importancia) })
        }
    }
}
