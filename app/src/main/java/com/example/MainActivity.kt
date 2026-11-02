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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.UserRole
import com.example.ui.components.AppHeader
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.SecurityBlockedDialog
import com.example.ui.screens.CampaignsScreen
import com.example.ui.screens.CotisationsScreen
import com.example.ui.screens.CreateCampaignDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.RecordCotisationDialog
import com.example.ui.screens.SmsCollectorScreen
import com.example.ui.theme.AutoSmsCotaTheme
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MainViewModel

sealed class AppDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : AppDestination("dashboard", "Tableau de Bord", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Campaigns : AppDestination("campaigns", "Campagnes", Icons.Filled.Campaign, Icons.Outlined.Campaign)
    data object Cotisations : AppDestination("cotisations", "Cotisations", Icons.Filled.Payments, Icons.Outlined.Payments)
    data object SmsScan : AppDestination("sms_scan", "Collecte SMS", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid)
    data object Members : AppDestination("members", "Membres", Icons.Filled.Group, Icons.Outlined.Group)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AutoSmsCotaTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Dashboard) }

    val currentUser by viewModel.currentUser.collectAsState()
    val allMembers by viewModel.members.collectAsState()
    val allCampaigns by viewModel.allCampaigns.collectAsState()
    val activeCampaigns by viewModel.activeCampaigns.collectAsState()

    val securityBlockReason by viewModel.securityBlockReason.collectAsState()
    val selectedReceipt by viewModel.selectedReceiptCotisation.collectAsState()
    val notification by viewModel.notification.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateCampaignDialog by remember { mutableStateOf(false) }
    var showRecordCotisationDialog by remember { mutableStateOf(false) }

    // Navigation items
    val navigationItems = listOf(
        AppDestination.Dashboard,
        AppDestination.Campaigns,
        AppDestination.Cotisations,
        AppDestination.SmsScan,
        AppDestination.Members
    )

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
                navigationItems.forEach { destination ->
                    val isSelected = currentDestination.route == destination.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title
                            )
                        },
                        label = { Text(destination.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${destination.route}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                is AppDestination.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCampaigns = { currentDestination = AppDestination.Campaigns },
                        onNavigateToCotisations = { currentDestination = AppDestination.Cotisations },
                        onNavigateToSmsCollector = { currentDestination = AppDestination.SmsScan },
                        onNavigateToMembers = { currentDestination = AppDestination.Members },
                        onOpenCreateCampaignDialog = { showCreateCampaignDialog = true },
                        onOpenRecordCotisationDialog = { showRecordCotisationDialog = true }
                    )
                }
                is AppDestination.Campaigns -> {
                    CampaignsScreen(
                        viewModel = viewModel,
                        onOpenCreateCampaignDialog = { showCreateCampaignDialog = true }
                    )
                }
                is AppDestination.Cotisations -> {
                    CotisationsScreen(
                        viewModel = viewModel,
                        onOpenRecordDialog = { showRecordCotisationDialog = true }
                    )
                }
                is AppDestination.SmsScan -> {
                    SmsCollectorScreen(viewModel = viewModel)
                }
                is AppDestination.Members -> {
                    MembersScreen(viewModel = viewModel)
                }
            }
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

    // Modal Saisie de Cotisation
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
}
