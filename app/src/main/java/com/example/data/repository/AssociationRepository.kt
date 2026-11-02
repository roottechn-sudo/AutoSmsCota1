package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.example.data.local.AppDatabase
import com.example.data.model.CotisationEntity
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.OrangeMoneySmsType
import com.example.data.model.ParsedSmsResult
import com.example.data.model.ReminderConfig
import com.example.data.model.StatutCotisation
import com.example.data.model.UserRole
import com.example.security.DroitsService
import com.example.data.parser.OrangeMoneyParser
import com.example.data.firebase.FirestoreService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.Calendar

class AssociationRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val memberDao = database.memberDao()
    private val campaignDao = database.campaignDao()
    private val cotisationDao = database.cotisationDao()
    val firestoreService = FirestoreService(context)

    private val reminderPrefs: SharedPreferences =
        context.getSharedPreferences("autosmscota_reminders_prefs", Context.MODE_PRIVATE)

    val membersFlow: Flow<List<MemberEntity>> = memberDao.getAllMembersFlow()
    val allCampaignsFlow: Flow<List<EventCampaignEntity>> = campaignDao.getAllCampaignsFlow()
    val activeCampaignsFlow: Flow<List<EventCampaignEntity>> = campaignDao.getActiveCampaignsFlow()
    val cotisationsFlow: Flow<List<CotisationEntity>> = cotisationDao.getAllCotisationsFlow()

    fun getCotisationsForMember(uid: String): Flow<List<CotisationEntity>> =
        cotisationDao.getCotisationsForMemberFlow(uid)

    fun getCotisationsForCampaign(idCampagne: String): Flow<List<CotisationEntity>> =
        cotisationDao.getCotisationsForCampaignFlow(idCampagne)

    suspend fun initSeedDataIfEmpty() = withContext(Dispatchers.IO) {
        if (memberDao.getMemberCount() == 0) {
            val initialMembers = listOf(
                MemberEntity(
                    uid = "mbr_005",
                    nom = "OUEDRAOGO",
                    prenom = "Abdoul Aziz",
                    telephone = "+22667103585",
                    role = UserRole.MEMBRE,
                    carteValide = true,
                    soldeArrieres = 0.0,
                    email = "retourauxvaleursancestrales@gmail.com",
                    categorie = "Membre Actif",
                    matricule = "KEMET019325-BF",
                    sexe = "Masculin",
                    residence = "Ouagadougou",
                    contactUrgenceNom = "PAKODE Assami",
                    contactUrgenceTel = "76 47 72 67",
                    dateAdhesion = 1771113600000L, // 15/02/2026
                    notes = "Adhérent actif - Retour aux Valeurs Ancestrales (Fiers de nos valeurs, fiers d'être africains !)"
                ),
                MemberEntity(
                    uid = "admin_001",
                    nom = "KABORÉ",
                    prenom = "Jean",
                    telephone = "+22670123456",
                    role = UserRole.ADMINISTRATEUR,
                    carteValide = true,
                    soldeArrieres = 0.0,
                    email = "jean.kabore@rava-burkina.bf",
                    categorie = "Membre Fondateur",
                    matricule = "KEMET000001-BF",
                    sexe = "Masculin",
                    residence = "Ouagadougou",
                    dateAdhesion = System.currentTimeMillis() - 730L * 24 * 3600 * 1000,
                    notes = "Président fondateur du comité de pilotage RAVA."
                ),
                MemberEntity(
                    uid = "tres_001",
                    nom = "OUÉDRAOGO",
                    prenom = "Aminata",
                    telephone = "+22676891234",
                    role = UserRole.TRESORIER,
                    carteValide = true,
                    soldeArrieres = 0.0,
                    email = "aminata.oued@rava-burkina.bf",
                    categorie = "Membre Fondateur",
                    dateAdhesion = System.currentTimeMillis() - 600L * 24 * 3600 * 1000,
                    notes = "Trésorière générale en charge des comptes Orange Money et banques."
                ),
                MemberEntity(
                    uid = "sec_001",
                    nom = "SAWADOGO",
                    prenom = "Moussa",
                    telephone = "+22665432109",
                    role = UserRole.SECRETAIRE,
                    carteValide = true,
                    soldeArrieres = 0.0,
                    email = "moussa.sawadogo@rava-burkina.bf",
                    categorie = "Membre Actif",
                    dateAdhesion = System.currentTimeMillis() - 500L * 24 * 3600 * 1000,
                    notes = "Secrétaire général chargé des campagnes et des procès-verbaux."
                ),
                MemberEntity(
                    uid = "mbr_001",
                    nom = "TRAORÉ",
                    prenom = "Fatou",
                    telephone = "+22654766084",
                    role = UserRole.MEMBRE,
                    carteValide = true,
                    soldeArrieres = 15000.0,
                    email = "fatou.traore@gmail.com",
                    categorie = "Membre Actif",
                    dateAdhesion = System.currentTimeMillis() - 180L * 24 * 3600 * 1000,
                    joursRetardEstimes = 22,
                    notes = "En retard de cotisation annuelle exercice 2026."
                ),
                MemberEntity(
                    uid = "mbr_002",
                    nom = "BOUGROUMA",
                    prenom = "Jean-Baptiste",
                    telephone = "+22671987654",
                    role = UserRole.MEMBRE,
                    carteValide = true,
                    soldeArrieres = 5000.0,
                    email = "jb.bougrouma@fasonet.bf",
                    categorie = "Étudiant / Jeunesse",
                    dateAdhesion = System.currentTimeMillis() - 90L * 24 * 3600 * 1000,
                    joursRetardEstimes = 8,
                    notes = "Section jeunesse - cotisation partielle solidarité santé."
                ),
                MemberEntity(
                    uid = "mbr_003",
                    nom = "KIENON",
                    prenom = "Alexis (Multi Services)",
                    telephone = "+22631773630",
                    role = UserRole.MEMBRE,
                    carteValide = false, // En attente
                    soldeArrieres = 25000.0,
                    email = "kienon.services@yahoo.fr",
                    categorie = "Sympathisant",
                    dateAdhesion = System.currentTimeMillis() - 40L * 24 * 3600 * 1000,
                    joursRetardEstimes = 35,
                    notes = "Partenaire entreprise locale. Compte en cours d'activation."
                ),
                MemberEntity(
                    uid = "mbr_004",
                    nom = "SANOGO",
                    prenom = "Dr. Idrissa",
                    telephone = "+22678112233",
                    role = UserRole.MEMBRE,
                    carteValide = true,
                    soldeArrieres = 0.0,
                    email = "sanogo.idrissa@univ-ouaga.bf",
                    categorie = "Membre d'Honneur",
                    dateAdhesion = System.currentTimeMillis() - 400L * 24 * 3600 * 1000,
                    notes = "Conseiller honorifique de l'association."
                )
            )
            memberDao.insertAll(initialMembers)
        }

        if (campaignDao.getActiveCampaignCount() == 0) {
            val initialCampaigns = listOf(
                EventCampaignEntity(
                    idCampagne = "CAMP_ANNUELLE_2026",
                    titre = "Cotisations Annuelles d'Adhésion 2026",
                    description = "Collecte statutaire obligatoire pour l'exercice 2026 de l'Association RAVA.",
                    dateCreation = System.currentTimeMillis() - 30L * 24 * 3600 * 1000,
                    dateCloture = null,
                    estCloture = false,
                    budgetEstime = 1500000.0,
                    budgetReel = 350000.0,
                    creeParUid = "sec_001"
                ),
                EventCampaignEntity(
                    idCampagne = "CAMP_SOLIDARITE_SANTE",
                    titre = "Fonds d'Urgence et Solidarité Santé",
                    description = "Assistance mutuelle et couverture des évacuations médicales des familles membres.",
                    dateCreation = System.currentTimeMillis() - 15L * 24 * 3600 * 1000,
                    dateCloture = null,
                    estCloture = false,
                    budgetEstime = 800000.0,
                    budgetReel = 245000.0,
                    creeParUid = "sec_001"
                ),
                EventCampaignEntity(
                    idCampagne = "CAMP_ASSEMBLEE_GEN_2025",
                    titre = "Organisation AG Extraordinaire 2025",
                    description = "Rénovation de la salle des fêtes et logistique de l'Assemblée Générale.",
                    dateCreation = System.currentTimeMillis() - 90L * 24 * 3600 * 1000,
                    dateCloture = System.currentTimeMillis() - 10L * 24 * 3600 * 1000,
                    estCloture = true,
                    budgetEstime = 500000.0,
                    budgetReel = 520000.0,
                    creeParUid = "sec_001"
                )
            )
            campaignDao.insertAll(initialCampaigns)
        }

        if (cotisationDao.getCotisationCount() == 0) {
            val initialCotisations = listOf(
                CotisationEntity(
                    idTransaction = "CI261031.1040.8675069",
                    idCampagne = "CAMP_ANNUELLE_2026",
                    membreUid = "mbr_003",
                    montant = 10000.0,
                    datePaiement = System.currentTimeMillis() - 3600 * 1000 * 5,
                    methodePaiement = "ORANGE_MONEY",
                    enregistreParUid = "tres_001",
                    recuGenere = true,
                    syncedToFirebase = true,
                    referenceExterne = "3177363 KIENON MULTI SERVICES"
                ),
                CotisationEntity(
                    idTransaction = "MP261031.1009.97358916",
                    idCampagne = "CAMP_SOLIDARITE_SANTE",
                    membreUid = "mbr_002",
                    montant = 5000.0,
                    datePaiement = System.currentTimeMillis() - 3600 * 1000 * 20,
                    methodePaiement = "ORANGE_MONEY",
                    enregistreParUid = "tres_001",
                    recuGenere = true,
                    syncedToFirebase = true,
                    referenceExterne = "Bebe jean-Baptiste Bougrouma"
                ),
                CotisationEntity(
                    idTransaction = "ESP_20261101_001",
                    idCampagne = "CAMP_ANNUELLE_2026",
                    membreUid = "mbr_001",
                    montant = 25000.0,
                    datePaiement = System.currentTimeMillis() - 3600 * 1000 * 48,
                    methodePaiement = "ESPECE",
                    enregistreParUid = "tres_001",
                    recuGenere = true,
                    syncedToFirebase = false,
                    referenceExterne = "Reçu papier N° 458"
                ),
                CotisationEntity(
                    idTransaction = "WAVE_TX_9872145",
                    idCampagne = "CAMP_ANNUELLE_2026",
                    membreUid = "mbr_004",
                    montant = 50000.0,
                    datePaiement = System.currentTimeMillis() - 3600 * 1000 * 72,
                    methodePaiement = "WAVE",
                    enregistreParUid = "tres_001",
                    recuGenere = true,
                    syncedToFirebase = true,
                    referenceExterne = "Wave QR Checkout (+226 78 11 22 33)"
                ),
                CotisationEntity(
                    idTransaction = "LIGDI_778129",
                    idCampagne = "CAMP_SOLIDARITE_SANTE",
                    membreUid = "SYMPATHISANT",
                    montant = 15000.0,
                    datePaiement = System.currentTimeMillis() - 3600 * 1000 * 80,
                    methodePaiement = "LIGDICASH",
                    enregistreParUid = "tres_001",
                    recuGenere = true,
                    syncedToFirebase = true,
                    referenceExterne = "Don externe via LigdiCash",
                    isAnonyme = false,
                    donateurNom = "El Hadj Oumar Traoré (Ami & Sympathisant)",
                    donateurTelephone = "+22670998877",
                    typeContributeur = "SYMPATHISANT"
                ),
                CotisationEntity(
                    idTransaction = "STRIPE_ANON_4491",
                    idCampagne = "CAMP_SOLIDARITE_SANTE",
                    membreUid = "ANONYMOUS",
                    montant = 30000.0,
                    datePaiement = System.currentTimeMillis() - 3600 * 1000 * 96,
                    methodePaiement = "STRIPE",
                    enregistreParUid = "tres_001",
                    recuGenere = true,
                    syncedToFirebase = true,
                    referenceExterne = "Don en ligne carte bancaire",
                    isAnonyme = true,
                    donateurNom = "Bienfaiteur Anonyme",
                    typeContributeur = "ANONYME",
                    statut = StatutCotisation.VALIDEE.code
                ),
                // Transaction SMS capturée en attente de vérification Trésorerie (Anti-Spoofing)
                CotisationEntity(
                    idTransaction = "OM_SMS_CI261102.1030.9841",
                    idCampagne = "CAMP_ANNUELLE_2026",
                    membreUid = "mbr_005",
                    montant = 10000.0,
                    datePaiement = System.currentTimeMillis() - 1800 * 1000,
                    methodePaiement = "ORANGE_MONEY",
                    enregistreParUid = "AUTO_SMS_SCAN",
                    recuGenere = false,
                    syncedToFirebase = false,
                    referenceExterne = "76477267 PAKODE Assami (Frais: 0 FCFA)",
                    statut = StatutCotisation.EN_ATTENTE.code
                )
            )
            cotisationDao.insertAllIgnoreDuplicates(initialCotisations)
            recalculateCampaignBudgets()
        }
    }

    suspend fun createCampaign(campaign: EventCampaignEntity) = withContext(Dispatchers.IO) {
        campaignDao.insertOrUpdate(campaign)
    }

    suspend fun closeCampaign(idCampagne: String) = withContext(Dispatchers.IO) {
        campaignDao.closeCampaign(idCampagne)
    }

    suspend fun recordManualCotisation(cotisation: CotisationEntity): Boolean = withContext(Dispatchers.IO) {
        val inserted = cotisationDao.insertIgnoreDuplicate(cotisation)
        if (inserted != -1L) {
            updateMemberArrears(cotisation.membreUid, cotisation.montant)
            recalculateCampaignBudgets()
            true
        } else {
            false
        }
    }

    /**
     * Enregistre un paiement complété via une passerelle (Stripe, Moov, Wave, LigdiCash, PayPal, Telecel, Orange Money).
     * Garantit l'idempotence et met à jour les arriérés du membre et la campagne.
     */
    suspend fun recordGatewayPayment(cotisation: CotisationEntity): Boolean = withContext(Dispatchers.IO) {
        val rowId = cotisationDao.insertIgnoreDuplicate(cotisation)
        if (rowId != -1L) {
            updateMemberArrears(cotisation.membreUid, cotisation.montant)
            recalculateCampaignBudgets()
            true
        } else {
            false
        }
    }

    suspend fun saveMember(member: MemberEntity) = withContext(Dispatchers.IO) {
        memberDao.insertOrUpdate(member)
    }

    suspend fun updateMemberProfile(member: MemberEntity) = withContext(Dispatchers.IO) {
        memberDao.insertOrUpdate(member)
    }

    fun getReminderConfig(): ReminderConfig {
        val threshold = reminderPrefs.getInt("days_threshold", 15)
        val defaultTemplate = "Bonjour {PRENOM}, l'Association RAVA vous rappelle que votre cotisation pour '{CAMPAGNE}' présente un arriéré de {MONTANT}. Merci de régulariser par Orange Money, Moov, Wave ou LigdiCash. Info bureau : +226 70 12 34 56"
        val template = reminderPrefs.getString("message_template", defaultTemplate) ?: defaultTemplate
        val auto = reminderPrefs.getBoolean("auto_send", false)
        val lastRun = reminderPrefs.getLong("last_run", 0L)
        return ReminderConfig(
            daysPastDueThreshold = threshold,
            messageTemplate = template,
            autoSendEnabled = auto,
            lastRunDate = lastRun
        )
    }

    fun saveReminderConfig(config: ReminderConfig) {
        reminderPrefs.edit()
            .putInt("days_threshold", config.daysPastDueThreshold)
            .putString("message_template", config.messageTemplate)
            .putBoolean("auto_send", config.autoSendEnabled)
            .putLong("last_run", config.lastRunDate)
            .apply()
    }

    /**
     * Importe un SMS Orange Money analysé dans la table Cotisations.
     */
    suspend fun processParsedSms(
        parsed: ParsedSmsResult,
        targetCampaignId: String,
        targetMemberUid: String,
        recordedByUid: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!parsed.isValid || parsed.amount <= 0.0) return@withContext false

        val cotisation = CotisationEntity(
            idTransaction = parsed.transactionId,
            idCampagne = targetCampaignId,
            membreUid = targetMemberUid,
            montant = parsed.amount,
            datePaiement = parsed.timestamp,
            methodePaiement = "ORANGE_MONEY",
            enregistreParUid = recordedByUid,
            recuGenere = false,
            syncedToFirebase = false,
            referenceExterne = "${parsed.party} (Frais: ${parsed.fees} FCFA)",
            statut = StatutCotisation.EN_ATTENTE.code
        )

        val rowId = cotisationDao.insertIgnoreDuplicate(cotisation)
        if (rowId != -1L) {
            // Anti-spoofing : la transaction SMS reste EN_ATTENTE de vérification.
            // Les arriérés ne sont déduits que lors de la validation par le Trésorier/Admin.
            try {
                firestoreService.saveCotisation(cotisation)
            } catch (e: Exception) {
                Log.w("AssociationRepository", "Synchronisation Firestore différée", e)
            }
            true
        } else {
            false
        }
    }

    /**
     * Validation manuelle d'une transaction capturée par SMS ou passerelle.
     * Accessible uniquement aux Trésoriers et Administrateurs (Double validation).
     */
    suspend fun validateCotisation(
        idTransaction: String,
        validatorUid: String,
        role: UserRole
    ): Boolean = withContext(Dispatchers.IO) {
        if (!DroitsService.canRecordCotisation(role)) {
            Log.w("AssociationRepository", "Validation rejetée : rôle insuffisant ($role)")
            return@withContext false
        }

        val cotis = cotisationDao.getCotisationById(idTransaction) ?: return@withContext false
        val now = System.currentTimeMillis()
        cotisationDao.updateStatut(idTransaction, StatutCotisation.VALIDEE.code, validatorUid, now)

        // Déduction des arriérés et mise à jour du budget après confirmation
        updateMemberArrears(cotis.membreUid, cotis.montant)
        recalculateCampaignBudgets()

        // Mise à jour Cloud Firestore et journal d'audit
        try {
            firestoreService.updateCotisationStatut(idTransaction, StatutCotisation.VALIDEE.code, validatorUid)
        } catch (e: Exception) {
            Log.w("AssociationRepository", "Erreur audit Firestore", e)
        }
        true
    }

    /**
     * Rejet d'une transaction invalide ou suspecte.
     */
    suspend fun rejectCotisation(
        idTransaction: String,
        validatorUid: String,
        role: UserRole
    ): Boolean = withContext(Dispatchers.IO) {
        if (!DroitsService.canRecordCotisation(role)) return@withContext false

        val now = System.currentTimeMillis()
        cotisationDao.updateStatut(idTransaction, StatutCotisation.REJETEE.code, validatorUid, now)

        try {
            firestoreService.updateCotisationStatut(idTransaction, StatutCotisation.REJETEE.code, validatorUid)
        } catch (e: Exception) {
            Log.w("AssociationRepository", "Erreur audit Firestore", e)
        }
        true
    }

    suspend fun scanHistoricalSmsInbox(
        startDate: Long?,
        endDate: Long?
    ): List<ParsedSmsResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ParsedSmsResult>()
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.Inbox._ID,
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )

        var selection: String? = null
        val selectionArgsList = mutableListOf<String>()

        if (startDate != null && endDate != null) {
            selection = "${Telephony.Sms.Inbox.DATE} >= ? AND ${Telephony.Sms.Inbox.DATE} <= ?"
            selectionArgsList.add(startDate.toString())
            selectionArgsList.add(endDate.toString())
        } else if (startDate != null) {
            selection = "${Telephony.Sms.Inbox.DATE} >= ?"
            selectionArgsList.add(startDate.toString())
        }

        val selectionArgs = if (selectionArgsList.isNotEmpty()) selectionArgsList.toTypedArray() else null

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.Inbox.DATE} DESC"
            )

            cursor?.use {
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

                while (it.moveToNext()) {
                    val body = it.getString(bodyIndex) ?: continue
                    val date = it.getLong(dateIndex)

                    val parsed = OrangeMoneyParser.parse(body, date)
                    if (parsed.isValid) {
                        results.add(parsed)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission non accordée
        } catch (e: Exception) {
            // Erreur de lecture
        }
        results
    }

    private suspend fun updateMemberArrears(memberUid: String, amountPaid: Double) {
        if (memberUid.isBlank() || memberUid == "ANONYMOUS" || memberUid == "SYMPATHISANT") return
        val member = memberDao.getMemberByUid(memberUid) ?: return
        val newArrears = maxOf(0.0, member.soldeArrieres - amountPaid)
        memberDao.updateArrears(memberUid, newArrears)
    }

    private suspend fun recalculateCampaignBudgets() {
        // Optionnel : recalcul des agrégats
    }

    suspend fun updateMemberRole(uid: String, newRole: UserRole) = withContext(Dispatchers.IO) {
        memberDao.updateRole(uid, newRole)
    }

    suspend fun updateMemberCardStatus(uid: String, isValid: Boolean) = withContext(Dispatchers.IO) {
        memberDao.updateCardStatus(uid, isValid)
    }

    suspend fun getTotalCollectedOverall(): Double = withContext(Dispatchers.IO) {
        cotisationDao.getTotalCollectedOverall() ?: 0.0
    }

    suspend fun getTotalCollectedThisMonth(): Double = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cotisationDao.getTotalCollectedSince(cal.timeInMillis) ?: 0.0
    }

    suspend fun getMemberByUid(uid: String): MemberEntity? = withContext(Dispatchers.IO) {
        memberDao.getMemberByUid(uid)
    }

    suspend fun getCampaignById(id: String): EventCampaignEntity? = withContext(Dispatchers.IO) {
        campaignDao.getCampaignById(id)
    }
}
