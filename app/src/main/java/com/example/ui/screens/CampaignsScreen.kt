package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalContext
import com.example.util.CalendarSyncHelper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EventCampaignEntity
import com.example.data.model.UserRole
import com.example.security.DroitsService
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeMoneyAccent
import com.example.ui.theme.Terracotta
import com.example.ui.viewmodel.MainViewModel

@Composable
fun CampaignsScreen(
    viewModel: MainViewModel,
    onOpenCreateCampaignDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val campaigns by viewModel.allCampaigns.collectAsState()
    val cotisations by viewModel.cotisations.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "CLOSED"

    val currentRole = currentUser?.role ?: UserRole.MEMBRE
    val canCreate = DroitsService.canCreateCampaign(currentRole)
    val canClose = DroitsService.canCloseCampaign(currentRole)

    val filteredList = when (selectedFilter) {
        "ACTIVE" -> campaigns.filter { !it.estCloture }
        "CLOSED" -> campaigns.filter { it.estCloture }
        else -> campaigns
    }

    Box(modifier = modifier.fillMaxSize().testTag("campaigns_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Campagnes d'Événements",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Projets, adhésions et collectes statutaires",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Filtres rapides
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Toutes (${campaigns.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "ACTIVE",
                        onClick = { selectedFilter = "ACTIVE" },
                        label = { Text("En cours (${campaigns.count { !it.estCloture }})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "CLOSED",
                        onClick = { selectedFilter = "CLOSED" },
                        label = { Text("Clôturées (${campaigns.count { it.estCloture }})") }
                    )
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucune campagne trouvée",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Les nouvelles campagnes créées par le Secrétariat apparaîtront ici.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList) { campaign ->
                    // Calcul de la somme réelle des cotisations pour cette campagne
                    val campaignTotalCollected = cotisations
                        .filter { it.idCampagne == campaign.idCampagne }
                        .sumOf { it.montant }

                    CampaignCard(
                        campaign = campaign,
                        collectedAmount = campaignTotalCollected,
                        formattedCollected = viewModel.formatFcfa(campaignTotalCollected),
                        formattedBudget = viewModel.formatFcfa(campaign.budgetEstime),
                        canClose = canClose,
                        onClose = {
                            viewModel.closeCampaign(campaign.idCampagne)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB pour créer une campagne si autorisé, ou pour tester la sécurité si non autorisé
        FloatingActionButton(
            onClick = {
                if (canCreate) {
                    onOpenCreateCampaignDialog()
                } else {
                    viewModel.showSecurityBlock(
                        DroitsService.getDeniedMessage(currentRole, "créer une campagne")
                    )
                }
            },
            containerColor = if (canCreate) EmeraldPrimary else Color.Gray,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_campaign_fab")
        ) {
            Icon(
                imageVector = if (canCreate) Icons.Default.Add else Icons.Default.Lock,
                contentDescription = "Créer une campagne"
            )
        }
    }
}

@Composable
fun CampaignCard(
    campaign: EventCampaignEntity,
    collectedAmount: Double,
    formattedCollected: String,
    formattedBudget: String,
    canClose: Boolean,
    onClose: () -> Unit
) {
    val progress = if (campaign.budgetEstime > 0) {
        (collectedAmount / campaign.budgetEstime).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val percentInt = (progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("campaign_card_${campaign.idCampagne}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.titre,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "ID: ${campaign.idCampagne}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (campaign.estCloture) Color.Gray.copy(alpha = 0.15f) else EmeraldContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (campaign.estCloture) "Clôturée" else "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (campaign.estCloture) Color.Gray else EmeraldPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = campaign.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progression financière
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Collecté / Objectif",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$formattedCollected / $formattedBudget",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "$percentInt%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = EmeraldPrimary,
                trackColor = EmeraldPrimary.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )

            val context = LocalContext.current

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { CalendarSyncHelper.syncCampaignToCalendar(context, campaign) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("sync_calendar_${campaign.idCampagne}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agenda", fontSize = 12.sp)
                }

                if (!campaign.estCloture && canClose) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("close_campaign_${campaign.idCampagne}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clôturer", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
