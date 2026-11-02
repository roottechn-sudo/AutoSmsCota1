package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.ui.theme.EmeraldPrimary

@Composable
fun CreateCampaignDialog(
    onDismiss: () -> Unit,
    onConfirm: (titre: String, description: String, budget: Double) -> Unit
) {
    var titre by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("500000") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("create_campaign_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Créer une Nouvelle Campagne",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                Text(
                    text = "Réservé au Secrétariat Général ou Administrateur",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it; isError = false },
                    label = { Text("Titre de la campagne *") },
                    placeholder = { Text("ex: Solidarité Tabaski 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = isError && titre.isBlank()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Objectifs") },
                    placeholder = { Text("Détails sur l'utilisation des fonds collectés") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = { Text("Budget estimé (FCFA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            if (titre.isNotBlank()) {
                                val budget = budgetText.toDoubleOrNull() ?: 0.0
                                onConfirm(titre, description, budget)
                                onDismiss()
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_create_campaign_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Créer")
                    }
                }
            }
        }
    }
}

@Composable
fun RecordCotisationDialog(
    campaigns: List<EventCampaignEntity>,
    members: List<MemberEntity>,
    onDismiss: () -> Unit,
    onConfirm: (campaignId: String, memberUid: String, montant: Double, reference: String) -> Unit
) {
    var selectedCampaignId by remember(campaigns) { mutableStateOf(campaigns.firstOrNull()?.idCampagne ?: "") }
    var selectedMemberUid by remember(members) { mutableStateOf(members.firstOrNull()?.uid ?: "") }
    var montantText by remember { mutableStateOf("10000") }
    var referenceText by remember { mutableStateOf("Versement direct espèces") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("record_cotisation_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Saisir une Cotisation (Espèces)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                Text(
                    text = "Réservé au Trésorier ou Administrateur",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campagne
                CampaignSelectorDropdown(
                    campaigns = campaigns,
                    selectedId = selectedCampaignId,
                    onSelect = { selectedCampaignId = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Membre
                MemberSelectorDropdown(
                    members = members,
                    selectedUid = selectedMemberUid,
                    onSelect = { selectedMemberUid = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = montantText,
                    onValueChange = { montantText = it },
                    label = { Text("Montant réglé (FCFA) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    label = { Text("Référence / Quittance") },
                    placeholder = { Text("ex: Reçu manuel N° 045") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Annuler")
                    }

                    Button(
                        onClick = {
                            val montant = montantText.toDoubleOrNull() ?: 0.0
                            if (selectedCampaignId.isNotBlank() && selectedMemberUid.isNotBlank() && montant > 0) {
                                onConfirm(selectedCampaignId, selectedMemberUid, montant, referenceText)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_record_cotisation_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}
