package com.example.rodapp.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rodapp.SupabaseClient
import com.example.rodapp.models.DocumentoAlerta
import com.example.rodapp.models.Moto
import com.example.rodapp.models.RtmRecord
import com.example.rodapp.models.SoatRecord
import com.example.rodapp.models.UserPreferences
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate

class DocumentAlertWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            verificarVencimientos()
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private suspend fun verificarVencimientos() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return

        val prefs = SupabaseClient.client.postgrest.from("user_preferences")
            .select { filter { eq("user_id", userId) }; limit(1L) }
            .decodeList<UserPreferences>().firstOrNull()

        if (prefs?.alertas_mantenimiento == false) return

        val motos = SupabaseClient.client.postgrest.from("motos")
            .select { filter { eq("user_id", userId) } }
            .decodeList<Moto>()

        val today = LocalDate.now()
        val limite = today.plusDays(5)
        val todayStr = today.toString()
        val limiteStr = limite.toString()
        var notifId = 300

        for (moto in motos) {
            val motoId = moto.id ?: continue
            val label = "${moto.marca} ${moto.modelo}"

            // SOAT
            SupabaseClient.client.postgrest.from("soat")
                .select {
                    filter {
                        eq("moto_id", motoId)
                        gte("fecha_vencimiento", todayStr)
                        lte("fecha_vencimiento", limiteStr)
                    }
                    limit(1L)
                }
                .decodeList<SoatRecord>().firstOrNull()
                ?.let {
                    notificar(
                        id = notifId++,
                        titulo = applicationContext.getString(com.example.rodapp.R.string.notif_soat_titulo),
                        cuerpo = applicationContext.getString(
                            com.example.rodapp.R.string.notif_cuerpo_vence, label, it.fecha_vencimiento
                        )
                    )
                }

            // RTM
            SupabaseClient.client.postgrest.from("rtm")
                .select {
                    filter {
                        eq("moto_id", motoId)
                        gte("fecha_vencimiento", todayStr)
                        lte("fecha_vencimiento", limiteStr)
                    }
                    limit(1L)
                }
                .decodeList<RtmRecord>().firstOrNull()
                ?.let {
                    notificar(
                        id = notifId++,
                        titulo = applicationContext.getString(com.example.rodapp.R.string.notif_rtm_titulo),
                        cuerpo = applicationContext.getString(
                            com.example.rodapp.R.string.notif_cuerpo_vence, label, it.fecha_vencimiento
                        )
                    )
                }

            // Documentos adicionales con fecha_vencimiento
            val docs = SupabaseClient.client.postgrest.from("documentos")
                .select {
                    filter {
                        eq("moto_id", motoId)
                        gte("fecha_vencimiento", todayStr)
                        lte("fecha_vencimiento", limiteStr)
                    }
                }
                .decodeList<DocumentoAlerta>()

            for (doc in docs) {
                notificar(
                    id = notifId++,
                    titulo = applicationContext.getString(com.example.rodapp.R.string.notif_doc_titulo),
                    cuerpo = applicationContext.getString(
                        com.example.rodapp.R.string.notif_cuerpo_vence,
                        doc.nombre,
                        doc.fecha_vencimiento ?: ""
                    )
                )
            }
        }
    }

    private fun notificar(id: Int, titulo: String, cuerpo: String) {
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, id, intent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(id, notif)
    }

    companion object {
        const val CHANNEL_ID = "rodapp_alertas"
        const val WORK_NAME = "document_alerts"
    }
}
