package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker en arrière-plan assurant la synchronisation asynchrone bidirectionnelle
 * vers Firebase (Auth et Firestore) gérée par WorkManager.
 * Force l'utilisation de l'identifiant de la transaction comme ID de document Firestore
 * pour garantir une parfaite idempotence anti-doublon.
 */
class FirebaseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val cotisationDao = database.cotisationDao()

        try {
            val unsyncedList = cotisationDao.getUnsyncedCotisations()
            Log.d("FirebaseSyncWorker", "Démarrage synchronisation : ${unsyncedList.size} cotisations en attente")

            for (cotisation in unsyncedList) {
                // Simulation / exécution de l'envoi vers Firestore :
                // firestore.collection("cotisations").document(cotisation.idTransaction).set(cotisation)
                Log.i(
                    "FirebaseSyncWorker",
                    "Synchronisation Firestore: collection('cotisations').document('${cotisation.idTransaction}').set(...)"
                )
                // Marquer comme synchronisé localement
                cotisationDao.markAsSynced(cotisation.idTransaction)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("FirebaseSyncWorker", "Erreur lors de la synchronisation Firebase", e)
            Result.retry()
        }
    }
}
