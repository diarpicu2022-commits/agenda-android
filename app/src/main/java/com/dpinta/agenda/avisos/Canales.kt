package com.dpinta.agenda.avisos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/** Canales de la tabla del anexo. Sin color propio (C3.5): la identidad va en la redacción. */
enum class Canal(val id: String, val nombre: String, val importancia: Int) {
    Salida("salida", "Salida", NotificationManager.IMPORTANCE_HIGH),
    SalYa("sal_ya", "Sal ya / Vas tarde", NotificationManager.IMPORTANCE_HIGH),
    Empieza("empieza", "Empieza", NotificationManager.IMPORTANCE_DEFAULT),
    Resumen("resumen", "Resumen matutino", NotificationManager.IMPORTANCE_LOW),
    ;

    companion object {
        /** Idempotente: crear un canal que ya existe solo actualiza su nombre. */
        fun registrar(contexto: Context) {
            val gestor = contexto.getSystemService(NotificationManager::class.java)
            gestor.createNotificationChannels(entries.map { NotificationChannel(it.id, it.nombre, it.importancia) })
        }
    }
}
