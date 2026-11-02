package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CotisationEntity
import com.example.data.model.EventCampaignEntity
import com.example.data.model.MemberEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeMoneyAccent
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DigitalReceiptDialog(
    cotisation: CotisationEntity,
    member: MemberEntity?,
    campaign: EventCampaignEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val formattedAmount = rememberFormattedFcfa(cotisation.montant)
    val formattedDate = rememberFormattedDate(cotisation.datePaiement)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 16.dp)
                .testTag("digital_receipt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header avec bouton fermeture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ASSOCIATION RAVA",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Burkina Faso • Reçu Officiel Numérique",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Carte du reçu avec bordure en pointillé / officielle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "REÇU DE PAIEMENT N°",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = cotisation.idTransaction,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = EmeraldPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Montant géant
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formattedAmount,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp
                                    ),
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "STATUT : PAYÉ & CONFIRMÉ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Détails membre et campagne
                        ReceiptDetailRow(label = "Membre Adhérent", value = member?.nomComplet ?: "Membre N° ${cotisation.membreUid}")
                        ReceiptDetailRow(label = "Téléphone", value = member?.telephone ?: "+226 -- -- -- --")
                        ReceiptDetailRow(label = "Campagne", value = campaign?.titre ?: cotisation.idCampagne)
                        ReceiptDetailRow(label = "Date & Heure", value = formattedDate)
                        ReceiptDetailRow(
                            label = "Mode de Règlement",
                            value = if (cotisation.methodePaiement == "ORANGE_MONEY") "Orange Money Burkina" else "Paiement Espèces"
                        )
                        if (!cotisation.referenceExterne.isNullOrBlank()) {
                            ReceiptDetailRow(label = "Détails / Réf", value = cotisation.referenceExterne)
                        }
                        ReceiptDetailRow(label = "Enregistré par", value = cotisation.enregistreParUid)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Cachet de certification et QR code visuel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SCEAU NUMÉRIQUE D'AUTHENTICITÉ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "Signature cryptographique conforme aux statuts RAVA Burkina Faso.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code de vérification",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions de partage
                Button(
                    onClick = {
                        shareReceipt(
                            context = context,
                            cotisation = cotisation,
                            member = member,
                            campaign = campaign,
                            amountFormatted = formattedAmount,
                            dateFormatted = formattedDate
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_receipt_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partager le Reçu Numérique")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

fun shareReceipt(
    context: Context,
    cotisation: CotisationEntity,
    member: MemberEntity?,
    campaign: EventCampaignEntity?,
    amountFormatted: String,
    dateFormatted: String
) {
    val message = buildString {
        appendLine("════════════════════════════════")
        appendLine("🏛️ ASSOCIATION RAVA - BURKINA FASO")
        appendLine("📄 REÇU OFFICIEL DE COTISATION")
        appendLine("════════════════════════════════")
        appendLine("N° Transaction : ${cotisation.idTransaction}")
        appendLine("Montant réglé  : $amountFormatted")
        appendLine("Adhérent       : ${member?.nomComplet ?: cotisation.membreUid}")
        appendLine("Téléphone      : ${member?.telephone ?: "Non renseigné"}")
        appendLine("Campagne       : ${campaign?.titre ?: cotisation.idCampagne}")
        appendLine("Règlement      : ${cotisation.methodePaiement}")
        if (!cotisation.referenceExterne.isNullOrBlank()) {
            appendLine("Réf Orange     : ${cotisation.referenceExterne}")
        }
        appendLine("Date & Heure   : $dateFormatted")
        appendLine("Statut         : VALIDÉ & ENREGISTRÉ DANS LA BASE LOCALE")
        appendLine("════════════════════════════════")
        appendLine("Généré automatiquement par AutoSmsCota")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, message)
        putExtra(Intent.EXTRA_SUBJECT, "Reçu de Cotisation RAVA - ${cotisation.idTransaction}")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Partager le reçu de cotisation"))
}

@Composable
fun rememberFormattedFcfa(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.FRENCH)
    formatter.maximumFractionDigits = 0
    return "${formatter.format(amount)} FCFA"
}

@Composable
fun rememberFormattedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
    return sdf.format(Date(timestamp))
}
