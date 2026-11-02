package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ShowChart
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberEntity
import com.example.data.model.UserRole
import com.example.security.DroitsService
import com.example.ui.components.AppHeader
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.MoreOptionsMenuSheet
import com.example.ui.components.SecurityBlockedDialog
import com.example.ui.screens.AddEditMemberDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CampaignsScreen
import com.example.ui.screens.CotisationsScreen
import com.example.ui.screens.CreateCampaignDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MemberDetailDialog
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.PaymentGatewayCheckoutDialog
import com.example.ui.screens.RecordCotisationDialog
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SmsCollectorScreen
import com.example.ui.theme.AutoSmsCotaTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MainViewModel

import com.example.ui.navigation.AppRoute
import com.example.ui.navigation.RouteGuard
import com.example.ui.navigation.SecureNavGraph
import com.example.ui.navigation.safeNavigate

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                com.example.data.local.ThemeMode.LIGHT -> false
                com.example.data.local.ThemeMode.DARK -> true
                com.example.data.local.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            AutoSmsCotaTheme(darkTheme = isDark) {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf<AppRoute>(AppRoute.Dashboard) }

    val currentUser by viewModel.currentUser.collectAsState()
    val allMembers by viewModel.members.collectAsState()
    val allCampaigns by viewModel.allCampaigns.collectAsState()
    val activeCampaigns by viewModel.activeCampaigns.collectAsState()
    val allCotisations by viewModel.cotisations.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()

    val securityBlockReason by viewModel.securityBlockReason.collectAsState()
    val selectedReceipt by viewModel.selectedReceiptCotisation.collectAsState()
    val selectedMemberForDetail by viewModel.selectedMemberForDetail.collectAsState()
    val notification by viewModel.notification.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog state controllers
    var showCreateCampaignDialog by remember { mutableStateOf(false) }
    var showRecordCotisationDialog by remember { mutableStateOf(false) }
    var showPaymentGatewayDialog by remember { mutableStateOf(false) }
    var gatewayTargetMemberUid by remember { mutableStateOf<String?>(null) }
    var gatewayTargetAmount by remember { mutableStateOf<Double?>(null) }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var editingMemberTarget by remember { mutableStateOf<MemberEntity?>(null) }
    var showMoreOptionsMenu by remember { mutableStateOf(false) }

    // 4 boutons principaux sur la barre du bas + 1 bouton 'Plus' pour aérer l'interface
    val primaryNavigationItems: List<AppRoute> = listOf(
        AppRoute.Dashboard,
        AppRoute.Cotisations,
        AppRoute.Campaigns,
        AppRoute.Members
    )

    val secondaryNavItems: List<AppRoute> = listOf(
        AppRoute.Analytics,
        AppRoute.Reminders,
        AppRoute.SmsScan
    )

    // Retour au tableau de bord avec le bouton retour physique quand on est sur un sous-écran
    if (currentDestination in secondaryNavItems) {
        BackHandler {
            currentDestination = AppRoute.Dashboard
        }
    }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppHeader(
                currentUser = currentUser,
                allMembers = allMembers,
                onSwitchUser = { uid ->
                    viewModel.switchUserSession(uid)
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("app_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                primaryNavigationItems.forEach { destination ->
                    val isSelected = currentDestination.route == destination.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { safeNavigate(destination, currentUser) { currentDestination = it } },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.route}")
                    )
                }

                // 5ème bouton : Menu "Plus" regroupant Stats, Rappels SMS, Scan SMS & Cloud Firestore
                val isMoreActive = currentDestination in secondaryNavItems
                NavigationBarItem(
                    selected = isMoreActive || showMoreOptionsMenu,
                    onClick = { showMoreOptionsMenu = true },
                    icon = {
                        Icon(
                            imageVector = if (isMoreActive) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                            contentDescription = "Plus d'options"
                        )
                    },
                    label = {
                        Text(
                            text = if (isMoreActive) currentDestination.title else "Plus",
                            fontSize = 11.sp,
                            fontWeight = if (isMoreActive) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldContainer
                    ),
                    modifier = Modifier.testTag("nav_item_more")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecureNavGraph(
                currentRoute = currentDestination,
                currentUser = currentUser,
                viewModel = viewModel,
                onNavigate = { currentDestination = it },
                onOpenCreateCampaignDialog = { showCreateCampaignDialog = true },
                onOpenRecordCotisationDialog = {
                    gatewayTargetMemberUid = null
                    gatewayTargetAmount = null
                    showPaymentGatewayDialog = true
                },
                onOpenAddMemberDialog = { showAddMemberDialog = true },
                onSwitchToAdminSession = {
                    val admin = allMembers.find { it.role == UserRole.ADMINISTRATEUR }
                    if (admin != null) {
                        viewModel.switchUserSession(admin.uid)
                    }
                    currentDestination = AppRoute.Dashboard
                }
            )
        }
    }

    // Modal Création de Campagne
    if (showCreateCampaignDialog) {
        CreateCampaignDialog(
            onDismiss = { showCreateCampaignDialog = false },
            onConfirm = { titre, desc, budget ->
                viewModel.createCampaign(titre, desc, budget)
            }
        )
    }

    // Modal Passerelle de Paiement Multi-Options (Stripe, Orange Money, Moov, Wave, LigdiCash, PayPal, Espèces)
    if (showPaymentGatewayDialog) {
        PaymentGatewayCheckoutDialog(
            campaigns = activeCampaigns,
            members = allMembers,
            initialMemberUid = gatewayTargetMemberUid,
            initialAmount = gatewayTargetAmount,
            onDismiss = { showPaymentGatewayDialog = false },
            onPaymentConfirmed = { gateway, campaignId, memberUid, amount, ref, typeContributeur, donateurNom, donateurTelephone ->
                viewModel.processGatewayPayment(
                    gateway = gateway,
                    campaignId = campaignId,
                    memberUid = memberUid,
                    montant = amount,
                    referenceExterne = ref,
                    typeContributeur = typeContributeur,
                    donateurNom = donateurNom,
                    donateurTelephone = donateurTelephone
                )
            }
        )
    }

    // Modal Saisie de Cotisation Espèces simple
    if (showRecordCotisationDialog) {
        RecordCotisationDialog(
            campaigns = activeCampaigns,
            members = allMembers,
            onDismiss = { showRecordCotisationDialog = false },
            onConfirm = { campId, memUid, montant, ref ->
                viewModel.recordCotisationManuelle(campId, memUid, montant, ref)
            }
        )
    }

    // Modal Détail et Historique du Membre
    selectedMemberForDetail?.let { member ->
        val canManage = DroitsService.canManageMembers(currentUser?.role ?: UserRole.MEMBRE)
        val defaultCampaignTitle = activeCampaigns.firstOrNull()?.titre ?: "Cotisations Statutaires RAVA 2026"

        MemberDetailDialog(
            member = member,
            cotisations = allCotisations,
            campaigns = allCampaigns,
            canManage = canManage,
            onDismiss = { viewModel.selectMemberForDetail(null) },
            onEditMember = {
                editingMemberTarget = member
                viewModel.selectMemberForDetail(null)
            },
            onOpenPaymentForMember = { mem ->
                gatewayTargetMemberUid = mem.uid
                gatewayTargetAmount = if (mem.soldeArrieres > 0) mem.soldeArrieres else 10000.0
                showPaymentGatewayDialog = true
                viewModel.selectMemberForDetail(null)
            },
            onSendReminder = { mem ->
                viewModel.sendPersonalizedReminderSms(context, mem, defaultCampaignTitle)
            },
            onSelectReceipt = { cotis ->
                viewModel.selectReceipt(cotis)
            },
            formatFcfa = { viewModel.formatFcfa(it) },
            formatDate = { viewModel.formatDate(it) }
        )
    }

    // Modal Ajout de Membre
    if (showAddMemberDialog) {
        AddEditMemberDialog(
            existingMember = null,
            onDismiss = { showAddMemberDialog = false },
            onSave = { nom, prenom, tel, email, role, cat, arrieres, notes ->
                viewModel.addMember(nom, prenom, tel, email, role, cat, arrieres, notes)
            }
        )
    }

    // Modal Édition de Membre
    editingMemberTarget?.let { memberToEdit ->
        AddEditMemberDialog(
            existingMember = memberToEdit,
            onDismiss = { editingMemberTarget = null },
            onSave = { nom, prenom, tel, email, role, cat, arrieres, notes ->
                val updated = memberToEdit.copy(
                    nom = nom.uppercase().trim(),
                    prenom = prenom.trim(),
                    telephone = tel.trim(),
                    email = email.trim().ifEmpty { null },
                    role = role,
                    categorie = cat,
                    soldeArrieres = arrieres,
                    notes = notes.trim().ifEmpty { null }
                )
                viewModel.editMember(updated)
                editingMemberTarget = null
            }
        )
    }

    // Modal Reçu Numérique Officiel
    selectedReceipt?.let { cotisation ->
        val member = allMembers.find { it.uid == cotisation.membreUid }
        val campaign = allCampaigns.find { it.idCampagne == cotisation.idCampagne }
        DigitalReceiptDialog(
            cotisation = cotisation,
            member = member,
            campaign = campaign,
            onDismiss = { viewModel.selectReceipt(null) }
        )
    }

    // Dialogue d'alerte / blocage de sécurité
    securityBlockReason?.let { reason ->
        SecurityBlockedDialog(
            reason = reason,
            currentUser = currentUser,
            onDismiss = { viewModel.dismissSecurityBlock() },
            onSwitchToAdmin = {
                val admin = allMembers.find { it.role == UserRole.ADMINISTRATEUR }
                if (admin != null) {
                    viewModel.switchUserSession(admin.uid)
                }
                viewModel.dismissSecurityBlock()
            }
        )
    }

    // Modal Feuille 'Plus' pour les fonctionnalités secondaires et la synchronisation Cloud
    if (showMoreOptionsMenu) {
        val themeMode by viewModel.themeMode.collectAsState()
        MoreOptionsMenuSheet(
            currentDestination = currentDestination,
            isCloudSyncing = isCloudSyncing,
            currentThemeMode = themeMode,
            onSelectThemeMode = { mode ->
                viewModel.setThemeMode(mode)
            },
            onSelectDestination = { dest ->
                safeNavigate(dest, currentUser) { currentDestination = it }
                showMoreOptionsMenu = false
            },
            onTriggerCloudSync = {
                viewModel.syncWithCloudFirestore()
            },
            onDismiss = { showMoreOptionsMenu = false }
        )
    }
}
