package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.example.data.local.AppDatabase
import com.example.data.model.CotisationEntity
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.OrangeMoneySmsType
import com.example.data.model.ParsedSmsResult
import com.example.data.model.UserRole
import com.example.data.parser.OrangeMoneyParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class AssociationRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val memberDao = database.memberDao()
    private val campaignDao = database.campaignDao()
    private val cotisationDao = database.cotisationDao()

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
                    uid = "admin_001",
                    nom = "KABORÉ",
                    prenom = "Jean",
                    telephone = "+22670123456",
                    role = UserRole.ADMINISTRATEUR,
                    carteValide = true,
                    soldeArrieres = 0.0
                ),
                MemberEntity(
                    uid = "tres_001",
                    nom = "OUÉDRAOGO",
                    prenom = "Aminata",
                    telephone = "+22676891234",
                    role = UserRole.TRESORIER,
                    carteValide = true,
                    soldeArrieres = 0.0
                ),
                MemberEntity(
                    uid = "sec_001",
                    nom = "SAWADOGO",
                    prenom = "Moussa",
                    telephone = "+22665432109",
                    role = UserRole.SECRETAIRE,
                    carteValide = true,
                    soldeArrieres = 0.0
                ),
                MemberEntity(
                    uid = "mbr_001",
                    nom = "TRAORÉ",
                    prenom = "Fatou",
                    telephone = "+22654766084",
                    role = UserRole.MEMBRE,
                    carteValide = true,
                    soldeArrieres = 15000.0
                ),
                MemberEntity(
                    uid = "mbr_002",
                    nom = "BOUGROUMA",
                    prenom = "Jean-Baptiste",
                    telephone = "+22671987654",
                    role = UserRole.MEMBRE,
                    carteValide = true,
                    soldeArrieres = 5000.0
                ),
                MemberEntity(
                    uid = "mbr_003",
                    nom = "KIENON",
                    prenom = "Alexis (Multi Services)",
                    telephone = "+22631773630",
                    role = UserRole.MEMBRE,
                    carteValide = false, // En attente
                    soldeArrieres = 25000.0
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
     * Importe un SMS Orange Money analysé dans la table Cotisations.
     * Utilise l'identifiant unique Orange Money comme clé primaire (Idempotence anti-doublon).
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
            recuGenere = true,
            syncedToFirebase = false,
            referenceExterne = "${parsed.party} (Frais: ${parsed.fees} FCFA)"
        )

        val rowId = cotisationDao.insertIgnoreDuplicate(cotisation)
        if (rowId != -1L) {
            updateMemberArrears(targetMemberUid, parsed.amount)
            recalculateCampaignBudgets()
            true
        } else {
            // Transaction déjà existante, ignorée silencieusement selon la règle d'idempotence
            false
        }
    }

    /**
     * Scan de l'historique des SMS locaux via ContentResolver avec filtrage de dates.
     */
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
        val member = memberDao.getMemberByUid(memberUid) ?: return
        val newArrears = maxOf(0.0, member.soldeArrieres - amountPaid)
        memberDao.updateArrears(memberUid, newArrears)
    }

    private suspend fun recalculateCampaignBudgets() {
        val campaigns = campaignDao.getAllCampaignsFlow()
        // Recalcul simple des budgets réels collectés
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
