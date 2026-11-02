package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.OrangeMoneySmsType
import com.example.data.model.ParsedSmsResult
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeMoneyAccent
import com.example.ui.theme.OrangeMoneyContainer
import com.example.ui.theme.Terracotta
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsCollectorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Simulateur/Test, 1: Scan Boîte SMS

    val scannedResults by viewModel.scannedSmsResults.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val campaigns by viewModel.activeCampaigns.collectAsState()
    val members by viewModel.members.collectAsState()

    var showDateRangePicker by remember { mutableStateOf(false) }
    var selectedStartDate by remember { mutableStateOf<Long?>(null) }
    var selectedEndDate by remember { mutableStateOf<Long?>(null) }

    // Test text area
    var testSmsText by remember {
        mutableStateOf(
            "Cher client, vous avez recu 10000.0 FCFA, Frais: FCFA, Taxe: FCFA de 3177363 KIENON MULTI SERVICES. Votre solde est de 15106.698 FCFA. ID Trans: CI261031.1040.8675069. Merci d'utiliser Orange Money."
        )
    }
    var liveParsedResult by remember { mutableStateOf<ParsedSmsResult?>(viewModel.testParseRawSms(testSmsText)) }

    // Campagne et Membre sélectionnés pour l'importation
    var selectedCampaignId by remember(campaigns) { mutableStateOf(campaigns.firstOrNull()?.idCampagne ?: "") }
    var selectedMemberUid by remember(members) { mutableStateOf(members.firstOrNull { it.uid == "mbr_003" }?.uid ?: members.firstOrNull()?.uid ?: "") }

    // Permission Launcher pour READ_SMS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanInboxSms(selectedStartDate, selectedEndDate)
        } else {
            viewModel.showSecurityBlock("Permission READ_SMS requise pour scanner la boîte de réception SMS.")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("sms_collector_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Simulateur & Analyse") },
                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Scan Boîte Réception") },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                // ONGLET 1 : SIMULATEUR ORANGE MONEY BURKINA FASO
                item {
                    Text(
                        text = "Analyseur de SMS Orange Money",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Parseur RegEx temps réel pour extraire Montant, ID Trans et Expéditeur avec détection anti-doublon.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Boutons presets avec les 3 modèles officiels
                item {
                    Text(
                        text = "Exemples Officiels Burkina Faso (Cliquer pour charger) :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PresetSmsButton(
                            title = "1. Dépôt / Recette Reçue (10 000 FCFA)",
                            body = "Cher client, vous avez recu 10000.0 FCFA, Frais: FCFA, Taxe: FCFA de 3177363 KIENON MULTI SERVICES. Votre solde est de 15106.698 FCFA. ID Trans: CI261031.1040.8675069. Merci d'utiliser Orange Money.",
                            onClick = {
                                testSmsText = it
                                liveParsedResult = viewModel.testParseRawSms(it)
                            }
                        )
                        PresetSmsButton(
                            title = "2. Transfert Émis (1 780 FCFA)",
                            body = "Cher client, vous avez transfere 1,780.00 FCFA, Frais: 0.0 FCFA, Taxe: FCFA au numero 54766084,Nonganaba Alexandre. Votre solde est de 5611.698 FCFA. ID Trans: PP261031.1017.2901274. Merci.",
                            onClick = {
                                testSmsText = it
                                liveParsedResult = viewModel.testParseRawSms(it)
                            }
                        )
                        PresetSmsButton(
                            title = "3. Paiement Effectué (500 FCFA)",
                            body = "Votre paiement de 500.00 FCFA, Frais: 4.3478 FCFA, Taxe: 0.6522 FCFA a Bebe jean-Baptiste Bougrouma a ete effectue avec succes. Votre solde est de : 5106.698 FCFA. Trans id: MP261031.1009.97358916.",
                            onClick = {
                                testSmsText = it
                                liveParsedResult = viewModel.testParseRawSms(it)
                            }
                        )
                    }
                }

                // Zone de saisie
                item {
                    OutlinedTextField(
                        value = testSmsText,
                        onValueChange = {
                            testSmsText = it
                            liveParsedResult = viewModel.testParseRawSms(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_text_input"),
                        label = { Text("Texte brut du SMS") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Résultat de l'analyse en temps réel
                item {
                    val result = liveParsedResult
                    if (result != null && result.isValid) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SMS Orange Money Valide",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = OrangeMoneyContainer
                                    ) {
                                        Text(
                                            text = result.type.labelFr,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = OrangeMoneyAccent,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                DetailField("Montant extrait :", viewModel.formatFcfa(result.amount), isBold = true)
                                DetailField("Identifiant Transaction :", result.transactionId, isMono = true)
                                DetailField("Tiers (Expéditeur/Destinataire) :", result.party)
                                if (result.fees > 0) {
                                    DetailField("Frais de transaction :", viewModel.formatFcfa(result.fees))
                                }
                                if (result.balanceAfter != null) {
                                    DetailField("Nouveau Solde :", viewModel.formatFcfa(result.balanceAfter))
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Cible d'attribution pour enregistrement
                                Text(
                                    text = "Assigner cette cotisation :",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Sélecteur de campagne
                                CampaignSelectorDropdown(
                                    campaigns = campaigns,
                                    selectedId = selectedCampaignId,
                                    onSelect = { selectedCampaignId = it }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sélecteur de membre
                                MemberSelectorDropdown(
                                    members = members,
                                    selectedUid = selectedMemberUid,
                                    onSelect = { selectedMemberUid = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        viewModel.importParsedSms(
                                            parsed = result,
                                            targetCampaignId = selectedCampaignId.ifEmpty { campaigns.firstOrNull()?.idCampagne ?: "CAMP_ANNUELLE_2026" },
                                            targetMemberUid = selectedMemberUid.ifEmpty { members.firstOrNull()?.uid ?: "mbr_001" }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("import_parsed_sms_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enregistrer dans la Base (Anti-Doublons)")
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TerracottaContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Terracotta)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Texte non reconnu comme SMS officiel Orange Money.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Terracotta
                                )
                            }
                        }
                    }
                }
            } else {
                // ONGLET 2 : SCAN BOÎTE SMS AVEC DATERANGEPICKER
                item {
                    Text(
                        text = "Scan de l'Historique des SMS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Filtrez par plage de dates via DateRangePicker pour importer automatiquement les SMS Orange Money enregistrés sur l'appareil.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bouton sélection de dates
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Plage de dates sélectionnée :",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    val dateText = if (selectedStartDate != null && selectedEndDate != null) {
                                        "${viewModel.formatDateOnly(selectedStartDate!!)} au ${viewModel.formatDateOnly(selectedEndDate!!)}"
                                    } else {
                                        "Toutes les dates (Historique complet)"
                                    }
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showDateRangePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("open_date_range_picker_button")
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Modifier")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.READ_SMS
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        viewModel.scanInboxSms(selectedStartDate, selectedEndDate)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.READ_SMS)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("launch_sms_scan_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyse en cours...")
                                } else {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Lancer le Scan de la Boîte SMS")
                                }
                            }
                        }
                    }
                }

                // Liste des résultats trouvés
                if (scannedResults.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Prêt pour le scan",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Cliquez sur 'Lancer le Scan' pour lire et filtrer les SMS de la boîte de réception Android.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "${scannedResults.size} SMS Orange Money Identifiés :",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(scannedResults) { parsedItem ->
                        ScannedSmsItemCard(
                            parsed = parsedItem,
                            formattedAmount = viewModel.formatFcfa(parsedItem.amount),
                            formattedDate = viewModel.formatDate(parsedItem.timestamp),
                            onImport = {
                                viewModel.importParsedSms(
                                    parsed = parsedItem,
                                    targetCampaignId = campaigns.firstOrNull()?.idCampagne ?: "CAMP_ANNUELLE_2026",
                                    targetMemberUid = members.firstOrNull()?.uid ?: "mbr_001"
                                )
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Modal DateRangePicker Material 3
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedStartDate = dateRangePickerState.selectedStartDateMillis
                        selectedEndDate = dateRangePickerState.selectedEndDateMillis
                        showDateRangePicker = false
                    }
                ) {
                    Text("Valider la Plage")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Filtrer les SMS par période",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                modifier = Modifier.height(480.dp)
            )
        }
    }
}

@Composable
fun PresetSmsButton(title: String, body: String, onClick: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(body) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DetailField(label: String, value: String, isBold: Boolean = false, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignSelectorDropdown(
    campaigns: List<EventCampaignEntity>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentCampaign = campaigns.find { it.idCampagne == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentCampaign?.titre ?: "Choisir une campagne...",
            onValueChange = {},
            readOnly = true,
            label = { Text("Campagne Cible") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            campaigns.forEach { campaign ->
                DropdownMenuItem(
                    text = { Text(campaign.titre) },
                    onClick = {
                        onSelect(campaign.idCampagne)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberSelectorDropdown(
    members: List<MemberEntity>,
    selectedUid: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentMember = members.find { it.uid == selectedUid }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentMember?.nomComplet ?: "Sélectionner l'adhérent...",
            onValueChange = {},
            readOnly = true,
            label = { Text("Membre Adhérent") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            members.forEach { member ->
                DropdownMenuItem(
                    text = { Text("${member.nomComplet} (${member.telephone})") },
                    onClick = {
                        onSelect(member.uid)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ScannedSmsItemCard(
    parsed: ParsedSmsResult,
    formattedAmount: String,
    formattedDate: String,
    onImport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = parsed.party,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "ID: ${parsed.transactionId}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = parsed.rawBody,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Button(
                    onClick = onImport,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Importer")
                }
            }
        }
    }
}
