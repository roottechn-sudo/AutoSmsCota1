package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CotisationEntity
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.StatutCotisation
import com.example.data.model.UserRole
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Service Cloud Firestore pour AutoSmsCota.
 * Initialise l'instance Firestore avec l'identifiant personnalisé de la base de données
 * déclaré dans R.string.firestore_database_id (firebase_applet_config.xml).
 *
 * Gère le stockage sécurisé et la synchronisation des :
 * - Profils membres (collection "members")
 * - Cotisations et reçus numériques (collection "cotisations")
 * - Journaux de paiements anonymes (collection "anonymous_payment_logs")
 * - Campagnes d'adhésion et de projets (collection "event_campaigns")
 */
class FirestoreService(private val context: Context) {

    // Initialisation conforme aux exigences : R.string.firestore_database_id
    val db: FirebaseFirestore by lazy {
        val databaseId = try {
            context.getString(R.string.firestore_database_id)
        } catch (e: Exception) {
            Log.w(TAG, "R.string.firestore_database_id introuvable, fallback vers l'ID par défaut", e)
            "ai-studio-android-autosmsc-f3227dee-d81b-4601-8152-cd7a3f4dd7f0"
        }
        Log.i(TAG, "Initialisation de l'instance Cloud Firestore avec databaseId: $databaseId")
        FirebaseFirestore.getInstance(databaseId)
    }

    /**
     * Enregistre ou met à jour le profil d'un membre dans Firestore (collection 'members').
     */
    suspend fun saveMember(member: MemberEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = hashMapOf(
                "uid" to member.uid,
                "nom" to member.nom,
                "prenom" to member.prenom,
                "telephone" to member.telephone,
                "role" to member.role.name,
                "carteValide" to member.carteValide,
                "soldeArrieres" to member.soldeArrieres,
                "email" to (member.email ?: ""),
                "categorie" to member.categorie,
                "matricule" to member.matricule,
                "sexe" to member.sexe,
                "residence" to member.residence,
                "contactUrgenceNom" to member.contactUrgenceNom,
                "contactUrgenceTel" to member.contactUrgenceTel,
                "dateAdhesion" to member.dateAdhesion,
                "notes" to (member.notes ?: ""),
                "joursRetardEstimes" to member.joursRetardEstimes,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection(COLLECTION_MEMBERS)
                .document(member.uid)
                .set(payload, SetOptions.merge())
                .await()

            Log.d(TAG, "Profil membre synchronisé dans Cloud Firestore: ${member.uid} (${member.nomComplet})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de sauvegarde du membre dans Firestore: ${member.uid}", e)
            Result.failure(e)
        }
    }

    /**
     * Enregistre une cotisation dans Firestore (collection 'cotisations').
     * Si la cotisation est anonyme, elle est également répertoriée dans le journal d'audit
     * des dons anonymes (collection 'anonymous_payment_logs').
     * Garantit l'idempotence grâce à l'utilisation de idTransaction comme ID de document.
     */
    suspend fun saveCotisation(cotisation: CotisationEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = hashMapOf(
                "idTransaction" to cotisation.idTransaction,
                "idCampagne" to cotisation.idCampagne,
                "membreUid" to cotisation.membreUid,
                "montant" to cotisation.montant,
                "datePaiement" to cotisation.datePaiement,
                "methodePaiement" to cotisation.methodePaiement,
                "enregistreParUid" to cotisation.enregistreParUid,
                "recuGenere" to cotisation.recuGenere,
                "referenceExterne" to (cotisation.referenceExterne ?: ""),
                "isAnonyme" to cotisation.isAnonyme,
                "donateurNom" to (cotisation.donateurNom ?: ""),
                "donateurTelephone" to (cotisation.donateurTelephone ?: ""),
                "typeContributeur" to cotisation.typeContributeur,
                "statut" to cotisation.statut,
                "valideParUid" to (cotisation.valideParUid ?: ""),
                "dateValidation" to (cotisation.dateValidation ?: 0L),
                "timestamp" to FieldValue.serverTimestamp()
            )

            // Stockage principal dans 'cotisations'
            db.collection(COLLECTION_COTISATIONS)
                .document(cotisation.idTransaction)
                .set(payload, SetOptions.merge())
                .await()

            // Journal dédié pour les paiements anonymes
            if (cotisation.isAnonyme || cotisation.typeContributeur == "ANONYME") {
                val anonPayload = HashMap(payload).apply {
                    put("typeLog", "PAIEMENT_ANONYME")
                    put("dateEnregistrement", FieldValue.serverTimestamp())
                }
                db.collection(COLLECTION_ANONYMOUS_PAYMENTS)
                    .document(cotisation.idTransaction)
                    .set(anonPayload, SetOptions.merge())
                    .await()
                Log.d(TAG, "Paiement anonyme journalisé dans Firestore: ${cotisation.idTransaction}")
            }

            Log.d(TAG, "Cotisation enregistrée dans Cloud Firestore: ${cotisation.idTransaction} (${cotisation.montant} FCFA, statut: ${cotisation.statut})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de sauvegarde cotisation dans Firestore: ${cotisation.idTransaction}", e)
            Result.failure(e)
        }
    }

    /**
     * Met à jour le statut d'une cotisation (ex: validation manuelle Trésorier/Admin)
     * et consigne un log d'audit immuable dans la collection 'audit_logs'.
     */
    suspend fun updateCotisationStatut(
        idTransaction: String,
        newStatus: String,
        validatorUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updateData = mapOf(
                "statut" to newStatus,
                "valideParUid" to validatorUid,
                "dateValidation" to FieldValue.serverTimestamp()
            )

            db.collection(COLLECTION_COTISATIONS)
                .document(idTransaction)
                .set(updateData, SetOptions.merge())
                .await()

            // Journal d'audit de traçabilité dans 'audit_logs'
            val auditLog = hashMapOf(
                "action" to "CHANGEMENT_STATUT_TRANSACTION",
                "idTransaction" to idTransaction,
                "nouveauStatut" to newStatus,
                "auteurUid" to validatorUid,
                "timestamp" to FieldValue.serverTimestamp()
            )
            db.collection("audit_logs").add(auditLog).await()

            Log.d(TAG, "Statut de la cotisation $idTransaction mis à jour vers $newStatus par $validatorUid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour du statut dans Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Enregistre ou met à jour une campagne dans Firestore.
     */
    suspend fun saveCampaign(campaign: EventCampaignEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = hashMapOf(
                "idCampagne" to campaign.idCampagne,
                "titre" to campaign.titre,
                "description" to campaign.description,
                "dateCreation" to campaign.dateCreation,
                "dateCloture" to (campaign.dateCloture ?: 0L),
                "estCloture" to campaign.estCloture,
                "budgetEstime" to campaign.budgetEstime,
                "budgetReel" to campaign.budgetReel,
                "creeParUid" to campaign.creeParUid,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection(COLLECTION_CAMPAIGNS)
                .document(campaign.idCampagne)
                .set(payload, SetOptions.merge())
                .await()

            Log.d(TAG, "Campagne synchronisée dans Cloud Firestore: ${campaign.idCampagne}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de sauvegarde campagne dans Firestore: ${campaign.idCampagne}", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronise un lot de cotisations locales en attente.
     */
    suspend fun syncBatch(cotisations: List<CotisationEntity>): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (item in cotisations) {
            val res = saveCotisation(item)
            if (res.isSuccess) count++
        }
        count
    }

    suspend fun fetchRemoteCotisations(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection(COLLECTION_COTISATIONS).get().await()
            val list = snapshot.documents.mapNotNull { it.data }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur fetchRemoteCotisations", e)
            Result.failure(e)
        }
    }

    /**
     * Écoute en temps réel des cotisations dans Cloud Firestore.
     * Déclenche une émission dès qu'un nouveau paiement est validé ou inséré.
     */
    fun listenToCotisationsRealtime(): Flow<List<CotisationEntity>> = callbackFlow {
        val listener = db.collection(COLLECTION_COTISATIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Erreur écoute temps réel cotisations Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            CotisationEntity(
                                idTransaction = doc.id,
                                idCampagne = data["idCampagne"] as? String ?: "CAMP_ANNUELLE_2026",
                                membreUid = data["membreUid"] as? String ?: "",
                                montant = (data["montant"] as? Number)?.toDouble() ?: 0.0,
                                datePaiement = (data["datePaiement"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                methodePaiement = data["methodePaiement"] as? String ?: "ORANGE_MONEY",
                                enregistreParUid = data["enregistreParUid"] as? String ?: "",
                                recuGenere = data["recuGenere"] as? Boolean ?: true,
                                syncedToFirebase = true,
                                referenceExterne = data["referenceExterne"] as? String,
                                isAnonyme = data["isAnonyme"] as? Boolean ?: false,
                                donateurNom = data["donateurNom"] as? String,
                                donateurTelephone = data["donateurTelephone"] as? String,
                                typeContributeur = data["typeContributeur"] as? String ?: "MEMBRE",
                                statut = data["statut"] as? String ?: StatutCotisation.EN_ATTENTE.code,
                                valideParUid = data["valideParUid"] as? String,
                                dateValidation = (data["dateValidation"] as? Number)?.toLong(),
                                nomMembre = data["nomMembre"] as? String ?: data["donateurNom"] as? String
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Erreur parsing document cotisation ${doc.id}", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Écoute en temps réel des campagnes dans Cloud Firestore.
     */
    fun listenToCampaignsRealtime(): Flow<List<EventCampaignEntity>> = callbackFlow {
        val listener = db.collection(COLLECTION_CAMPAIGNS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Erreur écoute temps réel campagnes Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            EventCampaignEntity(
                                idCampagne = doc.id,
                                titre = data["titre"] as? String ?: "",
                                description = data["description"] as? String ?: "",
                                dateCreation = (data["dateCreation"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                dateCloture = (data["dateCloture"] as? Number)?.toLong(),
                                estCloture = data["estCloture"] as? Boolean ?: false,
                                budgetEstime = (data["budgetEstime"] as? Number)?.toDouble() ?: 0.0,
                                budgetReel = (data["budgetReel"] as? Number)?.toDouble() ?: 0.0,
                                creeParUid = data["creeParUid"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Erreur parsing campagne ${doc.id}", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Écoute en temps réel des membres dans Cloud Firestore.
     */
    fun listenToMembersRealtime(): Flow<List<MemberEntity>> = callbackFlow {
        val listener = db.collection(COLLECTION_MEMBERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Erreur écoute temps réel membres Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val roleStr = data["role"] as? String ?: "MEMBRE"
                            val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.MEMBRE }
                            MemberEntity(
                                uid = doc.id,
                                nom = data["nom"] as? String ?: "",
                                prenom = data["prenom"] as? String ?: "",
                                telephone = data["telephone"] as? String ?: "",
                                role = role,
                                carteValide = data["carteValide"] as? Boolean ?: true,
                                soldeArrieres = (data["soldeArrieres"] as? Number)?.toDouble() ?: 0.0,
                                email = data["email"] as? String,
                                categorie = data["categorie"] as? String ?: "Membre Adhérent",
                                matricule = data["matricule"] as? String ?: "",
                                sexe = data["sexe"] as? String ?: "M",
                                residence = data["residence"] as? String ?: "",
                                contactUrgenceNom = data["contactUrgenceNom"] as? String ?: "",
                                contactUrgenceTel = data["contactUrgenceTel"] as? String ?: "",
                                dateAdhesion = (data["dateAdhesion"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                notes = data["notes"] as? String,
                                joursRetardEstimes = (data["joursRetardEstimes"] as? Number)?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Erreur parsing membre ${doc.id}", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    companion object {
        private const val TAG = "FirestoreService"
        const val COLLECTION_MEMBERS = "members"
        const val COLLECTION_COTISATIONS = "cotisations"
        const val COLLECTION_CAMPAIGNS = "event_campaigns"
        const val COLLECTION_ANONYMOUS_PAYMENTS = "anonymous_payment_logs"
    }
}
