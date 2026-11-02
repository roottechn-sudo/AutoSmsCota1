package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CotisationEntity
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.ParsedSmsResult
import com.example.data.model.PaymentGateway
import com.example.data.model.ReminderConfig
import com.example.data.model.UserRole
import com.example.data.local.ThemeMode
import com.example.data.local.ThemePreferencesRepository
import com.example.widget.TreasurerWidgetProvider
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
    private val themeRepository = ThemePreferencesRepository(application)

    val themeMode: StateFlow<ThemeMode> = themeRepository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    fun updateTreasurerWidget() {
        TreasurerWidgetProvider.updateAllWidgets(getApplication())
    }

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

    // Configuration des rappels SMS
    private val _reminderConfig = MutableStateFlow(associationRepository.getReminderConfig())
    val reminderConfig: StateFlow<ReminderConfig> = _reminderConfig.asStateFlow()

    // Membre sélectionné pour affichage du profil détaillé et historique
    private val _selectedMemberForDetail = MutableStateFlow<MemberEntity?>(null)
    val selectedMemberForDetail: StateFlow<MemberEntity?> = _selectedMemberForDetail.asStateFlow()

    init {
        viewModelScope.launch {
            associationRepository.initSeedDataIfEmpty()
            updateTreasurerWidget()
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

    fun selectMemberForDetail(member: MemberEntity?) {
        _selectedMemberForDetail.value = member
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

    /**
     * Traite et enregistre un paiement multi-passerelle (Stripe, Moov, Wave, LigdiCash, PayPal, Orange Money, Espèces)
     * et génère instantanément le reçu numérique.
     */
    fun processGatewayPayment(
        gateway: PaymentGateway,
        campaignId: String,
        memberUid: String,
        montant: Double,
        referenceExterne: String,
        typeContributeur: String = "MEMBRE",
        donateurNom: String? = null,
        donateurTelephone: String? = null
    ) {
        val user = currentUser.value
        viewModelScope.launch {
            val prefix = when (gateway) {
                PaymentGateway.STRIPE -> "STRIPE"
                PaymentGateway.PAYPAL -> "PP"
                PaymentGateway.WAVE -> "WAVE"
                PaymentGateway.MOOV_MONEY -> "MOOV"
                PaymentGateway.TELECEL_MONEY -> "TEL"
                PaymentGateway.LIGDICASH -> "LIGDI"
                PaymentGateway.ORANGE_MONEY -> "OM"
                PaymentGateway.ESPECE -> "ESP"
            }
            val transId = "${prefix}_${System.currentTimeMillis()}"

            val isAnon = typeContributeur == "ANONYME"
            val finalMemberUid = when (typeContributeur) {
                "ANONYME" -> "ANONYMOUS"
                "SYMPATHISANT" -> "SYMPATHISANT"
                else -> memberUid.ifEmpty { "ANONYMOUS" }
            }

            val cotisation = CotisationEntity(
                idTransaction = transId,
                idCampagne = campaignId,
                membreUid = finalMemberUid,
                montant = montant,
                datePaiement = System.currentTimeMillis(),
                methodePaiement = gateway.id,
                enregistreParUid = user?.uid ?: "PORTAIL_EN_LIGNE",
                recuGenere = true,
                syncedToFirebase = false,
                referenceExterne = referenceExterne,
                isAnonyme = isAnon,
                donateurNom = donateurNom,
                donateurTelephone = donateurTelephone,
                typeContributeur = typeContributeur
            )

            val success = associationRepository.recordGatewayPayment(cotisation)
            if (success) {
                val donorLabel = when (typeContributeur) {
                    "ANONYME" -> "donateur anonyme"
                    "SYMPATHISANT" -> "ami / sympathisant"
                    else -> "membre"
                }
                _notification.value = UiNotification(
                    "Paiement de ${formatFcfa(montant)} ($donorLabel) validé via ${gateway.displayName} ! Reçu généré."
                )
                _selectedReceiptCotisation.value = cotisation
                updateTreasurerWidget()
            } else {
                _notification.value = UiNotification("Échec du paiement : transaction rejetée.", isError = true)
            }
        }
    }

    fun recordCotisationManuelle(
        campaignId: String,
        memberUid: String,
        montant: Double,
        reference: String
    ) {
        processGatewayPayment(
            gateway = PaymentGateway.ESPECE,
            campaignId = campaignId,
            memberUid = memberUid,
            montant = montant,
            referenceExterne = reference.ifEmpty { "Versement espèces guichet" }
        )
    }

    /**
     * Valide une transaction en attente (Anti-spoofing SMS).
     * Réservé au Trésorier ou Administrateur.
     */
    fun validateCotisation(idTransaction: String) {
        val user = currentUser.value
        if (user == null || !DroitsService.canRecordCotisation(user.role)) {
            val reason = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "valider des transactions financières")
            _securityBlockReason.value = reason
            return
        }

        viewModelScope.launch {
            val success = associationRepository.validateCotisation(idTransaction, user.uid, user.role)
            if (success) {
                _notification.value = UiNotification("Transaction $idTransaction validée avec succès ! Arriérés et budget mis à jour.")
                updateTreasurerWidget()
            } else {
                _notification.value = UiNotification("Échec de la validation de la transaction.", isError = true)
            }
        }
    }

    /**
     * Rejette une transaction suspecte ou frauduleuse.
     */
    fun rejectCotisation(idTransaction: String) {
        val user = currentUser.value
        if (user == null || !DroitsService.canRecordCotisation(user.role)) {
            val reason = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "rejeter des transactions financières")
            _securityBlockReason.value = reason
            return
        }

        viewModelScope.launch {
            val success = associationRepository.rejectCotisation(idTransaction, user.uid, user.role)
            if (success) {
                _notification.value = UiNotification("Transaction $idTransaction marquée comme rejetée.")
                updateTreasurerWidget()
            } else {
                _notification.value = UiNotification("Échec du rejet de la transaction.", isError = true)
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

    // GESTION DES MEMBRES
    fun addMember(
        nom: String,
        prenom: String,
        telephone: String,
        email: String,
        role: UserRole,
        categorie: String,
        arrieresInitiaux: Double,
        notes: String
    ) {
        val user = currentUser.value
        if (user == null || !DroitsService.canManageMembers(user.role)) {
            _securityBlockReason.value = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "ajouter un membre")
            return
        }

        viewModelScope.launch {
            val newUid = "mbr_${System.currentTimeMillis()}"
            val newMember = MemberEntity(
                uid = newUid,
                nom = nom.uppercase().trim(),
                prenom = prenom.trim(),
                telephone = telephone.trim(),
                role = role,
                carteValide = true,
                soldeArrieres = arrieresInitiaux,
                email = email.trim().ifEmpty { null },
                categorie = categorie,
                dateAdhesion = System.currentTimeMillis(),
                notes = notes.trim().ifEmpty { null }
            )
            associationRepository.saveMember(newMember)
            _notification.value = UiNotification("Nouveau membre ajouté : ${newMember.nomComplet}")
        }
    }

    fun editMember(member: MemberEntity) {
        val user = currentUser.value
        if (user == null || !DroitsService.canManageMembers(user.role)) {
            _securityBlockReason.value = DroitsService.getDeniedMessage(user?.role ?: UserRole.MEMBRE, "modifier les informations du membre")
            return
        }

        viewModelScope.launch {
            associationRepository.updateMemberProfile(member)
            _selectedMemberForDetail.value = member
            _notification.value = UiNotification("Profil de ${member.nomComplet} mis à jour.")
        }
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

    // GESTION DES RAPPELS SMS POUR ARRIÉRÉS
    fun updateReminderConfig(thresholdDays: Int, template: String) {
        val newConfig = _reminderConfig.value.copy(
            daysPastDueThreshold = thresholdDays,
            messageTemplate = template
        )
        _reminderConfig.value = newConfig
        associationRepository.saveReminderConfig(newConfig)
        _notification.value = UiNotification("Paramètres des rappels SMS sauvegardés !")
    }

    fun sendPersonalizedReminderSms(
        context: Context,
        member: MemberEntity,
        campaignTitle: String
    ) {
        val config = _reminderConfig.value
        val formattedAmount = formatFcfa(member.soldeArrieres)
        val message = config.formatMessage(member, campaignTitle, formattedAmount)

        try {
            val uri = Uri.parse("smsto:${member.telephone}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            _notification.value = UiNotification("SMS de rappel préparé pour ${member.nomComplet}")
        } catch (e: Exception) {
            _notification.value = UiNotification("Erreur lors de l'ouverture de l'application SMS.", isError = true)
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

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    /**
     * Synchronise immédiatement l'ensemble des données (membres, cotisations, logs de dons anonymes)
     * avec Cloud Firestore.
     */
    fun syncWithCloudFirestore() {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                var syncedMembers = 0
                for (m in members.value) {
                    val res = associationRepository.firestoreService.saveMember(m)
                    if (res.isSuccess) syncedMembers++
                }

                var syncedCampaigns = 0
                for (c in allCampaigns.value) {
                    val res = associationRepository.firestoreService.saveCampaign(c)
                    if (res.isSuccess) syncedCampaigns++
                }

                val unsyncedCotisations = database.cotisationDao().getUnsyncedCotisations()
                var syncedCotisations = 0
                for (cotis in unsyncedCotisations) {
                    val res = associationRepository.firestoreService.saveCotisation(cotis)
                    if (res.isSuccess) {
                        database.cotisationDao().markAsSynced(cotis.idTransaction)
                        syncedCotisations++
                    }
                }

                _notification.value = UiNotification(
                    "Cloud Firestore synchronisé : $syncedMembers membres, $syncedCampaigns campagnes, $syncedCotisations cotisations !"
                )
            } catch (e: Exception) {
                _notification.value = UiNotification("Cloud Firestore : sauvegarde locale active (sync différée)", isError = false)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }
}

