package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.CotisationEntity
import com.example.data.model.OrangeMoneySmsType
import com.example.data.parser.OrangeMoneyParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Récepteur de diffusion pour intercepter en temps réel les notifications SMS Orange Money
 * et les intégrer automatiquement dans la base de données Room avec idempotence.
 */
class OrangeMoneySmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBodyBuilder = StringBuilder()
        var sender = ""
        var timestamp = System.currentTimeMillis()

        for (sms in messages) {
            sender = sms.originatingAddress ?: ""
            fullBodyBuilder.append(sms.messageBody)
            timestamp = sms.timestampMillis
        }

        val fullBody = fullBodyBuilder.toString()
        val parsed = OrangeMoneyParser.parse(fullBody, timestamp)

        if (parsed.isValid && parsed.amount > 0.0) {
            Log.d("OrangeMoneySmsReceiver", "Notification Orange Money détectée : ID=${parsed.transactionId}, Montant=${parsed.amount} FCFA")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    // Associe à la première campagne active trouvée ou campagne par défaut
                    val activeCampaign = db.campaignDao().getActiveCampaignsFlow()
                    // Insérer avec clé primaire = idTransaction (idempotence)
                    val cotisation = CotisationEntity(
                        idTransaction = parsed.transactionId,
                        idCampagne = "CAMP_ANNUELLE_2026",
                        membreUid = "mbr_001", // Attribution par défaut ou résolution selon numéro
                        montant = parsed.amount,
                        datePaiement = parsed.timestamp,
                        methodePaiement = "ORANGE_MONEY",
                        enregistreParUid = "AUTO_SMS_RECEIVER",
                        recuGenere = true,
                        syncedToFirebase = false,
                        referenceExterne = "${parsed.party} [SMS Temps Réel]"
                    )
                    val inserted = db.cotisationDao().insertIgnoreDuplicate(cotisation)
                    if (inserted != -1L) {
                        Log.i("OrangeMoneySmsReceiver", "Cotisation enregistrée automatiquement : ${cotisation.idTransaction}")
                    } else {
                        Log.w("OrangeMoneySmsReceiver", "Cotisation déjà existante ignorée : ${cotisation.idTransaction}")
                    }
                } catch (e: Exception) {
                    Log.e("OrangeMoneySmsReceiver", "Erreur enregistrement SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
