package com.example.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import java.util.Calendar

object CalendarSyncHelper {

    /**
     * Ouvre l'application Calendrier du système avec un événement pré-rempli pour une campagne
     */
    fun syncCampaignToCalendar(context: Context, campaign: EventCampaignEntity) {
        val calendar = Calendar.getInstance()
        // Si la campagne a une date de clôture, on l'utilise, sinon dans 14 jours
        val eventTimeMillis = campaign.dateCloture ?: (System.currentTimeMillis() + 14L * 24 * 3600 * 1000)
        calendar.timeInMillis = eventTimeMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Échéance Cotisation : ${campaign.titre}")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Cotisation pour l'association Retour aux Valeurs Ancestrales (RAVA).\n" +
                        "Description : ${campaign.description}\n" +
                        "Objectif budgétaire : ${campaign.budgetEstime.toInt()} FCFA\n" +
                        "Paiements acceptés : Orange Money, Moov, Wave, Espèces au siège."
            )
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Siège RAVA Ouagadougou / Mobile Money")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventTimeMillis + 2 * 3600 * 1000) // 2 heures
            putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(context, "Ouverture de votre Calendrier...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Impossible d'ouvrir le calendrier : aucune application trouvée",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Ouvre le calendrier pour une échéance de cotisation mensuelle ou arriérés d'un membre
     */
    fun syncMemberDueToCalendar(context: Context, member: MemberEntity, amountDue: Double, campaignTitle: String) {
        val calendar = Calendar.getInstance()
        // Échéance fixée au prochain 5 du mois ou dans 7 jours
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 0)
        val eventTimeMillis = calendar.timeInMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Rappel Cotisation RAVA : ${member.nomComplet}")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Échéance de cotisation statutaire RAVA.\n" +
                        "Membre : ${member.nomComplet} (Matricule: ${member.matricule.ifEmpty { member.uid }})\n" +
                        "Campagne : $campaignTitle\n" +
                        "Montant prévu : ${amountDue.toInt()} FCFA\n" +
                        "Contact Trésorerie : +226 76 89 12 34 / 70 12 34 56"
            )
            putExtra(CalendarContract.Events.EVENT_LOCATION, "Ouagadougou / Mobile Money Burkina")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventTimeMillis + 3600 * 1000)
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(context, "Rappel ajouté à l'agenda pour ${member.prenom}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Aucune application de calendrier disponible", Toast.LENGTH_LONG).show()
        }
    }
}
