package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CotisationEntity
import com.example.data.model.UserRole
import com.example.security.DroitsService
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeMoneyAccent
import com.example.ui.theme.OrangeMoneyContainer
import com.example.ui.viewmodel.MainViewModel

@Composable
fun CotisationsScreen(
    viewModel: MainViewModel,
    onOpenRecordDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cotisations by viewModel.cotisations.collectAsState()
    val members by viewModel.members.collectAsState()
    val campaigns by viewModel.allCampaigns.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var methodFilter by remember { mutableStateOf("ALL") } // "ALL", "ORANGE_MONEY", "ESPECE"
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "VALIDEE", "EN_ATTENTE"

    val currentRole = currentUser?.role ?: UserRole.MEMBRE
    val canRecord = DroitsService.canRecordCotisation(currentRole)

    // Si rôle MEMBRE simple, filtre uniquement sur ses propres cotisations selon les spécifications
    val baseList = if (currentRole == UserRole.MEMBRE && currentUser != null) {
        cotisations.filter { it.membreUid == currentUser?.uid }
    } else {
        cotisations
    }

    val filteredList = baseList.filter { cotisation ->
        val matchesMethod = when (methodFilter) {
            "ORANGE_MONEY" -> cotisation.methodePaiement == "ORANGE_MONEY"
            "ESPECE" -> cotisation.methodePaiement == "ESPECE"
            else -> true
        }

        val matchesStatus = when (statusFilter) {
            "VALIDEE" -> cotisation.isValidee
            "EN_ATTENTE" -> cotisation.isEnAttente
            else -> true
        }

        val member = members.find { it.uid == cotisation.membreUid }
        val memberName = member?.nomComplet ?: ""
        val campaign = campaigns.find { it.idCampagne == cotisation.idCampagne }
        val campaignTitle = campaign?.titre ?: ""

        val matchesSearch = searchQuery.isBlank() ||
                cotisation.idTransaction.contains(searchQuery, ignoreCase = true) ||
                memberName.contains(searchQuery, ignoreCase = true) ||
                campaignTitle.contains(searchQuery, ignoreCase = true) ||
                (cotisation.referenceExterne?.contains(searchQuery, ignoreCase = true) == true)

        matchesMethod && matchesStatus && matchesSearch
    }

    Box(modifier = modifier.fillMaxSize().testTag("cotisations_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentRole == UserRole.MEMBRE) "Mes Cotisations & Reçus" else "Gestion des Cotisations",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (currentRole == UserRole.MEMBRE)
                        "Historique personnel de vos versements et reçus numériques officiels"
                    else "Registre des encaissements Orange Money et Espèces (Anti-doublons)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Barre de recherche
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_cotisations_input"),
                    placeholder = { Text("Rechercher par N° transaction, membre, motif...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Filtres par mode de paiement
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = methodFilter == "ALL",
                        onClick = { methodFilter = "ALL" },
                        label = { Text("Tous (${baseList.size})") }
                    )
                    FilterChip(
                        selected = methodFilter == "ORANGE_MONEY",
                        onClick = { methodFilter = "ORANGE_MONEY" },
                        label = { Text("Orange Money (${baseList.count { it.methodePaiement == "ORANGE_MONEY" }})") }
                    )
                    FilterChip(
                        selected = methodFilter == "ESPECE",
                        onClick = { methodFilter = "ESPECE" },
                        label = { Text("Espèces (${baseList.count { it.methodePaiement == "ESPECE" }})") }
                    )
                }
            }

            // Filtres par statut (Anti-Spoofing & Validation)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = statusFilter == "ALL",
                        onClick = { statusFilter = "ALL" },
                        label = { Text("Tous statuts") }
                    )
                    FilterChip(
                        selected = statusFilter == "VALIDEE",
                        onClick = { statusFilter = "VALIDEE" },
                        label = { Text("Validées (${baseList.count { it.isValidee }})") }
                    )
                    FilterChip(
                        selected = statusFilter == "EN_ATTENTE",
                        onClick = { statusFilter = "EN_ATTENTE" },
                        label = { Text("En attente SMS (${baseList.count { it.isEnAttente }})") }
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
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aucune cotisation trouvée",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Saisissez un versement ou importez les SMS Orange Money.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList) { cotisation ->
                    val member = members.find { it.uid == cotisation.membreUid }
                    val campaign = campaigns.find { it.idCampagne == cotisation.idCampagne }

                    CotisationCardItem(
                        cotisation = cotisation,
                        memberName = member?.nomComplet ?: cotisation.getDisplayName(null),
                        campaignTitle = campaign?.titre ?: cotisation.idCampagne,
                        formattedAmount = viewModel.formatFcfa(cotisation.montant),
                        formattedDate = viewModel.formatDate(cotisation.datePaiement),
                        canValidate = canRecord,
                        onValidate = {
                            viewModel.validateCotisation(cotisation.idTransaction)
                        },
                        onClick = {
                            if (!cotisation.isEnAttente) viewModel.selectReceipt(cotisation)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB pour enregistrer une cotisation
        FloatingActionButton(
            onClick = {
                if (canRecord) {
                    onOpenRecordDialog()
                } else {
                    viewModel.showSecurityBlock(
                        DroitsService.getDeniedMessage(currentRole, "enregistrer une cotisation")
                    )
                }
            },
            containerColor = if (canRecord) EmeraldPrimary else Color.Gray,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("record_cotisation_fab")
        ) {
            Icon(
                imageVector = if (canRecord) Icons.Default.Add else Icons.Default.Lock,
                contentDescription = "Saisir une cotisation"
            )
        }
    }
}

@Composable
fun CotisationCardItem(
    cotisation: CotisationEntity,
    memberName: String,
    campaignTitle: String,
    formattedAmount: String,
    formattedDate: String,
    canValidate: Boolean,
    onValidate: () -> Unit,
    onClick: () -> Unit
) {
    val gateway = com.example.data.model.PaymentGateway.fromId(cotisation.methodePaiement)
    val isPending = cotisation.isEnAttente

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) OrangeMoneyContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("cotisation_row_${cotisation.idTransaction}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(gateway.brandColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                gateway.isMobileMoney -> Icons.Default.PhoneAndroid
                                gateway.isInternational -> Icons.Default.CreditCard
                                else -> Icons.Default.Payments
                            },
                            contentDescription = null,
                            tint = gateway.brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = memberName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${gateway.displayName} • $campaignTitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPending) OrangeMoneyAccent else EmeraldPrimary
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPending) OrangeMoneyAccent.copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = if (isPending) "EN ATTENTE" else "VALIDÉE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPending) OrangeMoneyAccent else Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${cotisation.idTransaction}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (cotisation.syncedToFirebase) EmeraldContainer else Color(0xFFFFF8E1)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (cotisation.syncedToFirebase) Icons.Default.CloudDone else Icons.Default.SyncProblem,
                                contentDescription = null,
                                tint = if (cotisation.syncedToFirebase) EmeraldPrimary else Color(0xFFF57F17),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (cotisation.syncedToFirebase) "Cloud Sync" else "Local Room",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (cotisation.syncedToFirebase) EmeraldPrimary else Color(0xFFF57F17)
                            )
                        }
                    }

                    if (!isPending) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reçu",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bouton de validation rapide si en attente (Anti-spoofing SMS)
            if (isPending && canValidate) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vérification requise :",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Button(
                        onClick = onValidate,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("validate_cotisation_btn_${cotisation.idTransaction}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Valider la Recette", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
