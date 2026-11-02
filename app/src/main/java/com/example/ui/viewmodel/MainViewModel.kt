package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CotisationEntity
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.ParsedSmsResult
import com.example.data.model.UserRole
import com.example.data.parser.OrangeMoneyParser
import com.example.data.repository.AssociationRepository
import com.example.data.repository.AuthRepository
import com.example.security.DroitsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiNotification(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val authRepository = AuthRepository(application, database)
    val associationRepository = AssociationRepository(application, database)

    val currentUser: StateFlow<MemberEntity?> = authRepository.currentUser
    val isUserPendingActivation: StateFlow<Boolean> = authRepository.isUserPendingActivation

    val members: StateFlow<List<MemberEntity>> = associationRepository.membersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCampaigns: StateFlow<List<EventCampaignEntity>> = associationRepository.allCampaignsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeCampaigns: StateFlow<List<EventCampaignEntity>> = associationRepository.activeCampaignsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cotisations: StateFlow<List<CotisationEntity>> = associationRepository.cotisationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Écran de blocage de sécurité
    private val _securityBlockReason = MutableStateFlow<String?>(null)
    val securityBlockReason: StateFlow<String?> = _securityBlockReason.asStateFlow()

    // Notification toast / snackbar
    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    // Résultats du scan SMS
    private val _scannedSmsResults = MutableStateFlow<List<ParsedSmsResult>>(emptyList())
    val scannedSmsResults: StateFlow<List<ParsedSmsResult>> = _scannedSmsResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Reçu sélectionné pour affichage modal
    private val _selectedReceiptCotisation = MutableStateFlow<CotisationEntity?>(null)
    val selectedReceiptCotisation: StateFlow<CotisationEntity?> = _selectedReceiptCotisation.asStateFlow()

    init {
        viewModelScope.launch {
            associationRepository.initSeedDataIfEmpty()
        }
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun dismissSecurityBlock() {
        _securityBlockReason.value = null
    }

    fun showSecurityBlock(reason: String) {
        _securityBlockReason.value = reason
    }

    fun selectReceipt(cotisation: CotisationEntity?) {
        _selectedReceiptCotisation.value = cotisation
    }

    fun switchUserSession(uid: String) {
        viewModelScope.launch {
            authRepository.switchUser(uid)
            val user = authRepository.currentUser.value
            _notification.value = UiNotification("Session basculée : ${user?.nomComplet} [${user?.role?.labelFr}]")
        }
    }

    fun createCampaign(
        titre: String,
        description: String,
        budgetEstime: Double
    ) {
        val user = currentUser.value
        if (user == null || !DroitsService.canCreateCampaign(user.role)) {
            val reason = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "créer une campagne")
            _securityBlockReason.value = reason
            return
        }

        viewModelScope.launch {
            val id = "CAMP_${System.currentTimeMillis()}"
            val campaign = EventCampaignEntity(
                idCampagne = id,
                titre = titre,
                description = description,
                budgetEstime = budgetEstime,
                creeParUid = user.uid
            )
            associationRepository.createCampaign(campaign)
            _notification.value = UiNotification("Campagne créée avec succès !")
        }
    }

    fun closeCampaign(idCampagne: String) {
        val user = currentUser.value
        if (user == null || !DroitsService.canCloseCampaign(user.role)) {
            val reason = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "clôturer une campagne")
            _securityBlockReason.value = reason
            return
        }

        viewModelScope.launch {
            associationRepository.closeCampaign(idCampagne)
            _notification.value = UiNotification("Campagne clôturée.")
        }
    }

    fun recordCotisationManuelle(
        campaignId: String,
        memberUid: String,
        montant: Double,
        reference: String
    ) {
        val user = currentUser.value
        if (user == null || !DroitsService.canRecordCotisation(user.role)) {
            val reason = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "enregistrer une cotisation")
            _securityBlockReason.value = reason
            return
        }

        viewModelScope.launch {
            val transId = "ESP_${System.currentTimeMillis()}"
            val cotisation = CotisationEntity(
                idTransaction = transId,
                idCampagne = campaignId,
                membreUid = memberUid,
                montant = montant,
                methodePaiement = "ESPECE",
                enregistreParUid = user.uid,
                recuGenere = true,
                syncedToFirebase = false,
                referenceExterne = reference.ifEmpty { "Paiement direct en espèces" }
            )
            val success = associationRepository.recordManualCotisation(cotisation)
            if (success) {
                _notification.value = UiNotification("Cotisation de ${formatFcfa(montant)} enregistrée !")
                _selectedReceiptCotisation.value = cotisation
            } else {
                _notification.value = UiNotification("Échec : transaction en double ignorée.", isError = true)
            }
        }
    }

    fun importParsedSms(
        parsed: ParsedSmsResult,
        targetCampaignId: String,
        targetMemberUid: String
    ) {
        val user = currentUser.value
        if (user == null || !DroitsService.canScanSms(user.role)) {
            val reason = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "importer des SMS Orange Money")
            _securityBlockReason.value = reason
            return
        }

        viewModelScope.launch {
            val success = associationRepository.processParsedSms(
                parsed = parsed,
                targetCampaignId = targetCampaignId,
                targetMemberUid = targetMemberUid,
                recordedByUid = user.uid
            )
            if (success) {
                _notification.value = UiNotification(
                    "SMS importé avec succès : ${parsed.transactionId} (${formatFcfa(parsed.amount)})"
                )
            } else {
                _notification.value = UiNotification(
                    "Ignoré : La transaction ${parsed.transactionId} existe déjà dans la base (Idempotence).",
                    isError = true
                )
            }
        }
    }

    fun scanInboxSms(startDate: Long?, endDate: Long?) {
        val user = currentUser.value
        if (user == null || !DroitsService.canScanSms(user.role)) {
            _securityBlockReason.value = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "scanner les SMS")
            return
        }

        viewModelScope.launch {
            _isScanning.value = true
            val results = associationRepository.scanHistoricalSmsInbox(startDate, endDate)
            _scannedSmsResults.value = results
            _isScanning.value = false
            if (results.isEmpty()) {
                _notification.value = UiNotification("Aucun SMS Orange Money trouvé sur la plage sélectionnée.")
            } else {
                _notification.value = UiNotification("${results.size} SMS Orange Money détectés avec succès !")
            }
        }
    }

    fun testParseRawSms(rawText: String): ParsedSmsResult {
        return OrangeMoneyParser.parse(rawText)
    }

    fun updateMemberRole(uid: String, newRole: UserRole) {
        val user = currentUser.value
        if (user == null || !DroitsService.canChangeUserRole(user.role)) {
            _securityBlockReason.value = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "modifier les rôles d'utilisateurs")
            return
        }

        viewModelScope.launch {
            associationRepository.updateMemberRole(uid, newRole)
            _notification.value = UiNotification("Rôle mis à jour avec succès : ${newRole.labelFr}")
        }
    }

    fun toggleCardValidation(uid: String, currentStatus: Boolean) {
        val user = currentUser.value
        if (user == null || !DroitsService.canManageMembers(user.role)) {
            _securityBlockReason.value = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "activer ou suspendre des cartes de membre")
            return
        }

        viewModelScope.launch {
            associationRepository.updateMemberCardStatus(uid, !currentStatus)
            _notification.value = UiNotification(if (!currentStatus) "Compte membre activé !" else "Compte membre suspendu.")
        }
    }

    fun formatFcfa(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.FRENCH)
        formatter.maximumFractionDigits = 0
        return "${formatter.format(amount)} FCFA"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
        return sdf.format(Date(timestamp))
    }
}
