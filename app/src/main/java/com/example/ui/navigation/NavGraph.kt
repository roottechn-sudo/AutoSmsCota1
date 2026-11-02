package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberEntity
import com.example.data.model.UserRole
import com.example.security.DroitsService
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CampaignsScreen
import com.example.ui.screens.CotisationsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SmsCollectorScreen
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.MainViewModel

/**
 * Itinéraires et destinations de l'application AutoSmsCota.
 */
sealed class AppRoute(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val requiredRoleDescription: String? = null
) {
    data object Dashboard : AppRoute("dashboard", "Accueil", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Cotisations : AppRoute("cotisations", "Cotisations", Icons.Filled.Payments, Icons.Outlined.Payments)
    data object Campaigns : AppRoute("campaigns", "Campagnes", Icons.Filled.Campaign, Icons.Outlined.Campaign)
    data object Members : AppRoute("members", "Membres", Icons.Filled.Group, Icons.Outlined.Group)

    // Itinéraires protégés par la barrière d'interception des rôles
    data object Analytics : AppRoute(
        route = "analytics",
        title = "Stats Trésorerie",
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart,
        requiredRoleDescription = "Trésorier ou Administrateur"
    )

    data object Reminders : AppRoute(
        route = "reminders",
        title = "Rappels SMS",
        selectedIcon = Icons.Filled.NotificationsActive,
        unselectedIcon = Icons.Outlined.NotificationsActive,
        requiredRoleDescription = "Secrétaire, Trésorier ou Administrateur"
    )

    data object SmsScan : AppRoute(
        route = "sms_scan",
        title = "Collecte SMS",
        selectedIcon = Icons.Filled.PhoneAndroid,
        unselectedIcon = Icons.Outlined.PhoneAndroid,
        requiredRoleDescription = "Trésorier ou Administrateur"
    )

    data class AccessDenied(
        val attemptedRoute: String,
        val requiredRole: String,
        val reason: String
    ) : AppRoute(
        route = "access_denied",
        title = "Accès Refusé",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Filled.Security
    )
}

/**
 * Gardien de navigation (Route Guard / RBAC) :
 * Vérifie si l'utilisateur possède le rôle requis dans son état de session pour accéder à la route ciblée.
 */
object RouteGuard {

    fun evaluateNavigation(targetRoute: AppRoute, currentUser: MemberEntity?): NavigationResult {
        val userRole = currentUser?.role ?: UserRole.MEMBRE

        return when (targetRoute) {
            is AppRoute.Dashboard,
            is AppRoute.Cotisations,
            is AppRoute.Campaigns,
            is AppRoute.Members -> {
                NavigationResult.Allowed
            }

            is AppRoute.Analytics -> {
                if (DroitsService.canViewFinancialReports(userRole)) {
                    NavigationResult.Allowed
                } else {
                    NavigationResult.Denied(
                        requiredRole = "Trésorier ou Administrateur",
                        attemptedRoute = targetRoute.title,
                        reason = DroitsService.getDeniedMessage(userRole, "consulter les bilans financiers et graphiques d'évolution")
                    )
                }
            }

            is AppRoute.SmsScan -> {
                if (DroitsService.canScanSms(userRole)) {
                    NavigationResult.Allowed
                } else {
                    NavigationResult.Denied(
                        requiredRole = "Trésorier ou Administrateur",
                        attemptedRoute = targetRoute.title,
                        reason = DroitsService.getDeniedMessage(userRole, "scanner et importer les SMS financiers Orange Money")
                    )
                }
            }

            is AppRoute.Reminders -> {
                if (userRole != UserRole.MEMBRE) {
                    NavigationResult.Allowed
                } else {
                    NavigationResult.Denied(
                        requiredRole = "Bureau Exécutif (Secrétaire / Trésorier)",
                        attemptedRoute = targetRoute.title,
                        reason = DroitsService.getDeniedMessage(userRole, "configurer et envoyer des relances SMS d'arriérés")
                    )
                }
            }

            is AppRoute.AccessDenied -> NavigationResult.Allowed
        }
    }
}

sealed class NavigationResult {
    data object Allowed : NavigationResult()
    data class Denied(
        val requiredRole: String,
        val attemptedRoute: String,
        val reason: String
    ) : NavigationResult()
}

/**
 * Routeur sécurisé intégrant l'interception de navigation et la redirection d'accès non autorisé.
 */
@Composable
fun SecureNavGraph(
    currentRoute: AppRoute,
    currentUser: MemberEntity?,
    viewModel: MainViewModel,
    onNavigate: (AppRoute) -> Unit,
    onOpenCreateCampaignDialog: () -> Unit,
    onOpenRecordCotisationDialog: () -> Unit,
    onOpenAddMemberDialog: () -> Unit,
    onSwitchToAdminSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Interception systématique du bouton retour physique pour revenir à l'accueil
    if (currentRoute !is AppRoute.Dashboard) {
        BackHandler {
            onNavigate(AppRoute.Dashboard)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentRoute) {
            is AppRoute.Dashboard -> {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCampaigns = {
                        safeNavigate(AppRoute.Campaigns, currentUser, onNavigate)
                    },
                    onNavigateToCotisations = {
                        safeNavigate(AppRoute.Cotisations, currentUser, onNavigate)
                    },
                    onNavigateToSmsCollector = {
                        safeNavigate(AppRoute.SmsScan, currentUser, onNavigate)
                    },
                    onNavigateToMembers = {
                        safeNavigate(AppRoute.Members, currentUser, onNavigate)
                    },
                    onNavigateToAnalytics = {
                        safeNavigate(AppRoute.Analytics, currentUser, onNavigate)
                    },
                    onOpenCreateCampaignDialog = onOpenCreateCampaignDialog,
                    onOpenRecordCotisationDialog = onOpenRecordCotisationDialog
                )
            }

            is AppRoute.Cotisations -> {
                CotisationsScreen(
                    viewModel = viewModel,
                    onOpenRecordDialog = onOpenRecordCotisationDialog
                )
            }

            is AppRoute.Campaigns -> {
                CampaignsScreen(
                    viewModel = viewModel,
                    onOpenCreateCampaignDialog = onOpenCreateCampaignDialog
                )
            }

            is AppRoute.Members -> {
                MembersScreen(
                    viewModel = viewModel,
                    onOpenAddMemberDialog = onOpenAddMemberDialog
                )
            }

            is AppRoute.Analytics -> {
                AnalyticsScreen(viewModel = viewModel)
            }

            is AppRoute.Reminders -> {
                RemindersScreen(viewModel = viewModel)
            }

            is AppRoute.SmsScan -> {
                SmsCollectorScreen(viewModel = viewModel)
            }

            is AppRoute.AccessDenied -> {
                AccesRefuseScreen(
                    attemptedRoute = currentRoute.attemptedRoute,
                    requiredRole = currentRoute.requiredRole,
                    reason = currentRoute.reason,
                    currentUserRole = currentUser?.role?.labelFr ?: "Visiteur",
                    onNavigateBack = { onNavigate(AppRoute.Dashboard) },
                    onSwitchToAdmin = onSwitchToAdminSession
                )
            }
        }
    }
}

/**
 * Fonction de navigation protégée vérifiant les droits avant transition d'état.
 */
fun safeNavigate(
    targetRoute: AppRoute,
    currentUser: MemberEntity?,
    onNavigate: (AppRoute) -> Unit
) {
    when (val evaluation = RouteGuard.evaluateNavigation(targetRoute, currentUser)) {
        is NavigationResult.Allowed -> {
            onNavigate(targetRoute)
        }
        is NavigationResult.Denied -> {
            onNavigate(
                AppRoute.AccessDenied(
                    attemptedRoute = evaluation.attemptedRoute,
                    requiredRole = evaluation.requiredRole,
                    reason = evaluation.reason
                )
            )
        }
    }
}

/**
 * Écran d'erreur graphique 'AccesRefuseScreen' dédié ("Accès Refusé - Droits Insuffisants").
 * Conçu avec une disposition fluide en pourcentages, sans aucun risque d'overflow.
 */
@Composable
fun AccesRefuseScreen(
    attemptedRoute: String,
    requiredRole: String,
    reason: String,
    currentUserRole: String,
    onNavigateBack: () -> Unit,
    onSwitchToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("access_denied_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("access_denied_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icône Bouclier Rouge / Terracotta
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(TerracottaContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Accès Refusé",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Terracotta,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Privilèges Insuffisants",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bloc d'information sur la restriction
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Espace demandé :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = attemptedRoute,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Votre rôle actuel :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentUserRole,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Terracotta
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Rôle minimum requis :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = requiredRole,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Boutons d'action
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("access_denied_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retour au Tableau de Bord")
                    }

                    OutlinedButton(
                        onClick = onSwitchToAdmin,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("access_denied_switch_admin_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Passer en Session Administrateur")
                    }
                }
            }
        }
    }
}

/**
 * Alias de compatibilité pour AccesRefuseScreen.
 */
@Composable
fun AccessDeniedScreen(
    attemptedRoute: String,
    requiredRole: String,
    reason: String,
    currentUserRole: String,
    onNavigateBack: () -> Unit,
    onSwitchToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) = AccesRefuseScreen(
    attemptedRoute = attemptedRoute,
    requiredRole = requiredRole,
    reason = reason,
    currentUserRole = currentUserRole,
    onNavigateBack = onNavigateBack,
    onSwitchToAdmin = onSwitchToAdmin,
    modifier = modifier
)

