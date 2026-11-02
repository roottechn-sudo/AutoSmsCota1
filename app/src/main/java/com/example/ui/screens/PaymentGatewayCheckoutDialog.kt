package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.data.model.PaymentGateway
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeMoneyAccent

@Composable
fun PaymentGatewayCheckoutDialog(
    campaigns: List<EventCampaignEntity>,
    members: List<MemberEntity>,
    initialMemberUid: String? = null,
    initialAmount: Double? = null,
    onDismiss: () -> Unit,
    onPaymentConfirmed: (
        gateway: PaymentGateway,
        campaignId: String,
        memberUid: String,
        amount: Double,
        ref: String,
        typeContributeur: String,
        donateurNom: String?,
        donateurTelephone: String?
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Choix contributeur, montant & passerelle, 2: Validation passerelle

    // Type de contributeur : "MEMBRE", "SYMPATHISANT", "ANONYME"
    var contributorType by remember { mutableStateOf("MEMBRE") }
    var donateurNomText by remember { mutableStateOf("") }
    var donateurTelText by remember { mutableStateOf("+226 ") }

    var selectedGateway by remember { mutableStateOf(PaymentGateway.ORANGE_MONEY) }
    var selectedCampaignId by remember(campaigns) { mutableStateOf(campaigns.firstOrNull()?.idCampagne ?: "") }
    var selectedMemberUid by remember(members) {
        mutableStateOf(initialMemberUid ?: members.firstOrNull()?.uid ?: "")
    }

    val selectedMember = members.find { it.uid == selectedMemberUid }
    var montantText by remember {
        mutableStateOf((initialAmount ?: selectedMember?.soldeArrieres?.takeIf { it > 0 } ?: 10000.0).toInt().toString())
    }

    // Champs spécifiques selon la passerelle
    var mobileMoneyPhone by remember(selectedMember, donateurTelText, contributorType) {
        mutableStateOf(if (contributorType == "MEMBRE") selectedMember?.telephone ?: "+226 " else donateurTelText)
    }
    var cardNumber by remember { mutableStateOf("4532 8901 2345 6789") }
    var cardExpiry by remember { mutableStateOf("12/28") }
    var cardCvc by remember { mutableStateOf("321") }
    var cardHolder by remember(selectedMember, donateurNomText, contributorType) {
        mutableStateOf(
            when (contributorType) {
                "ANONYME" -> "DONATEUR ANONYME"
                "SYMPATHISANT" -> donateurNomText.ifBlank { "AMI DE L'ASSOCIATION" }
                else -> selectedMember?.nomComplet ?: "TITULAIRE"
            }
        )
    }
    var paypalEmail by remember(selectedMember) { mutableStateOf(selectedMember?.email ?: "donateur@gmail.com") }

    var isProcessing by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 12.dp)
                .testTag("payment_gateway_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step == 2) {
                        IconButton(onClick = { step = 1 }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (step == 1) "Saisie & Règlement Cotisation" else "Paiement via ${selectedGateway.displayName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = selectedGateway.brandColor
                        )
                        Text(
                            text = "Membres • Amis & Sympathisants • Dons Anonymes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                if (step == 1) {
                    // ÉTAPE 1 : CHOIX DU TYPE DE CONTRIBUTEUR
                    Text(
                        text = "1. Type de Contributeur :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = contributorType == "MEMBRE",
                            onClick = { contributorType = "MEMBRE" },
                            label = { Text("Membre", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = contributorType == "SYMPATHISANT",
                            onClick = { contributorType = "SYMPATHISANT" },
                            label = { Text("Ami / Externe", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f)
                        )
                        FilterChip(
                            selected = contributorType == "ANONYME",
                            onClick = { contributorType = "ANONYME" },
                            label = { Text("Anonyme", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Affichage conditionnel selon le contributeur
                    when (contributorType) {
                        "MEMBRE" -> {
                            MemberSelectorDropdown(
                                members = members,
                                selectedUid = selectedMemberUid,
                                onSelect = {
                                    selectedMemberUid = it
                                    val m = members.find { mem -> mem.uid == it }
                                    if (m != null && m.soldeArrieres > 0) {
                                        montantText = m.soldeArrieres.toInt().toString()
                                    }
                                }
                            )
                        }
                        "SYMPATHISANT" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Ami, sympathisant ou donateur externe :",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = donateurNomText,
                                        onValueChange = { donateurNomText = it },
                                        label = { Text("Nom complet du donateur ou structure *") },
                                        placeholder = { Text("ex: El Hadj Moussa OUEDRAOGO, Amicale...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = donateurTelText,
                                        onValueChange = { donateurTelText = it },
                                        label = { Text("Téléphone (pour reçu SMS / WhatsApp)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                        "ANONYME" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Cotisation Anonyme : L'identité du donateur restera confidentielle. Le reçu indiquera 'Donateur Anonyme - Bienfaiteur RAVA'.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "2. Campagne de destination :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    CampaignSelectorDropdown(
                        campaigns = campaigns,
                        selectedId = selectedCampaignId,
                        onSelect = { selectedCampaignId = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "3. Montant de la cotisation (FCFA) :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = montantText,
                        onValueChange = { montantText = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { Text("FCFA", modifier = Modifier.padding(end = 12.dp)) }
                    )

                    if (contributorType == "MEMBRE" && selectedMember != null && selectedMember.soldeArrieres > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Arriérés actuels : ${selectedMember.soldeArrieres.toInt()} FCFA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Tout régler",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                ),
                                modifier = Modifier.clickable {
                                    montantText = selectedMember.soldeArrieres.toInt().toString()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "4. Moyen de paiement :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentGateway.entries.forEach { gateway ->
                            GatewayOptionCard(
                                gateway = gateway,
                                isSelected = selectedGateway == gateway,
                                onSelect = { selectedGateway = gateway }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val amount = montantText.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && selectedCampaignId.isNotBlank()) {
                                step = 2
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = selectedGateway.brandColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("continue_to_gateway_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continuer vers le paiement (${montantText} FCFA)")
                    }

                } else {
                    // ÉTAPE 2 : INTERFACE DE LA PASSERELLE SÉLECTIONNÉE
                    val amountVal = montantText.toDoubleOrNull() ?: 0.0

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = selectedGateway.brandColor.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Montant à valider :",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${montantText} FCFA",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = selectedGateway.brandColor
                                )
                            )
                            Text(
                                text = when (contributorType) {
                                    "ANONYME" -> "Cotisation Anonyme"
                                    "SYMPATHISANT" -> "Donateur : ${donateurNomText.ifBlank { "Ami / Sympathisant" }}"
                                    else -> "Adhérent : ${selectedMember?.nomComplet}"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedGateway) {
                        PaymentGateway.ORANGE_MONEY,
                        PaymentGateway.MOOV_MONEY,
                        PaymentGateway.TELECEL_MONEY -> {
                            Text(
                                text = "Numéro ${selectedGateway.displayName} :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = mobileMoneyPhone,
                                onValueChange = { mobileMoneyPhone = it },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Une invite USSD sera envoyée sur ce numéro pour valider la transaction avec votre code secret.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PaymentGateway.WAVE -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(2.dp, selectedGateway.brandColor, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "Wave QR Code",
                                        tint = selectedGateway.brandColor,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scannez avec l'application Wave ou confirmez avec le bouton ci-dessous (frais 1%).",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        PaymentGateway.LIGDICASH -> {
                            Text(
                                text = "Paiement sécurisé LigdiCash Burkina :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Portefeuille : Agrégateur Orange / Moov / Cartes UEMOA",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = "Marchand : RAVA_COTA_042 • Token : LGD-${System.currentTimeMillis().toString().takeLast(6)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        PaymentGateway.STRIPE -> {
                            Text(
                                text = "Informations de la Carte Bancaire :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = cardHolder,
                                onValueChange = { cardHolder = it },
                                label = { Text("Nom sur la carte") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("Numéro de carte") },
                                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text("Expiration") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = cardCvc,
                                    onValueChange = { cardCvc = it },
                                    label = { Text("CVC") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        PaymentGateway.PAYPAL -> {
                            Text(
                                text = "Compte PayPal :",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = paypalEmail,
                                onValueChange = { paypalEmail = it },
                                label = { Text("Adresse e-mail PayPal") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Redirection et autorisation en un clic via PayPal Express Checkout.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PaymentGateway.ESPECE -> {
                            Text(
                                text = "Paiement en espèces au guichet de l'association.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            isProcessing = true
                            val ref = when (selectedGateway) {
                                PaymentGateway.ORANGE_MONEY -> "Orange Money ($mobileMoneyPhone)"
                                PaymentGateway.MOOV_MONEY -> "Moov Money Flooz ($mobileMoneyPhone)"
                                PaymentGateway.TELECEL_MONEY -> "Telecel Money ($mobileMoneyPhone)"
                                PaymentGateway.WAVE -> "Wave Checkout"
                                PaymentGateway.LIGDICASH -> "LigdiCash Token LGD-${System.currentTimeMillis().toString().takeLast(6)}"
                                PaymentGateway.STRIPE -> "Stripe Card (•••• ${cardNumber.takeLast(4)})"
                                PaymentGateway.PAYPAL -> "PayPal ($paypalEmail)"
                                PaymentGateway.ESPECE -> "Espèces directes"
                            }
                            val memberUid = if (contributorType == "MEMBRE") selectedMemberUid else contributorType
                            val donorName = when (contributorType) {
                                "ANONYME" -> "Bienfaiteur Anonyme"
                                "SYMPATHISANT" -> donateurNomText.ifBlank { "Ami & Sympathisant RAVA" }
                                else -> selectedMember?.nomComplet
                            }
                            val donorTel = if (contributorType == "MEMBRE") selectedMember?.telephone else donateurTelText

                            onPaymentConfirmed(
                                selectedGateway,
                                selectedCampaignId,
                                memberUid,
                                amountVal,
                                ref,
                                contributorType,
                                donorName,
                                donorTel
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = selectedGateway.brandColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_gateway_payment_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validation en cours...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmer le Règlement (${montantText} FCFA)")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Modifier informations")
                    }
                }
            }
        }
    }
}

@Composable
fun GatewayOptionCard(
    gateway: PaymentGateway,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) gateway.brandColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) gateway.brandColor else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("gateway_option_${gateway.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(gateway.brandColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gateway.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = gateway.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = gateway.brandColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

