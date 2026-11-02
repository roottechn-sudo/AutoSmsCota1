package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PaymentGateway(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val brandColorHex: Long,
    val isMobileMoney: Boolean = false,
    val isInternational: Boolean = false
) {
    ORANGE_MONEY(
        id = "ORANGE_MONEY",
        displayName = "Orange Money",
        subtitle = "Burkina Faso & UEMOA",
        brandColorHex = 0xFFFF6600,
        isMobileMoney = true
    ),
    MOOV_MONEY(
        id = "MOOV_MONEY",
        displayName = "Moov Money (Flooz)",
        subtitle = "Moov Africa Burkina",
        brandColorHex = 0xFF0066B3,
        isMobileMoney = true
    ),
    TELECEL_MONEY(
        id = "TELECEL_MONEY",
        displayName = "Telecel Money",
        subtitle = "Telecel Faso",
        brandColorHex = 0xFFE60000,
        isMobileMoney = true
    ),
    WAVE(
        id = "WAVE",
        displayName = "Wave Mobile Money",
        subtitle = "Paiement 1% / Scan QR",
        brandColorHex = 0xFF1DC3EC,
        isMobileMoney = true
    ),
    LIGDICASH(
        id = "LIGDICASH",
        displayName = "LigdiCash",
        subtitle = "Passerelle Web & Mobile locale",
        brandColorHex = 0xFF2E7D32,
        isMobileMoney = false
    ),
    STRIPE(
        id = "STRIPE",
        displayName = "Carte Bancaire (Stripe)",
        subtitle = "Visa, Mastercard & International",
        brandColorHex = 0xFF635BFF,
        isInternational = true
    ),
    PAYPAL(
        id = "PAYPAL",
        displayName = "PayPal Checkout",
        subtitle = "Paiement en ligne sécurisé",
        brandColorHex = 0xFF003087,
        isInternational = true
    ),
    ESPECE(
        id = "ESPECE",
        displayName = "Paiement Espèces",
        subtitle = "Remise en mains propres",
        brandColorHex = 0xFF388E3C
    );

    val brandColor: Color
        get() = Color(brandColorHex)

    companion object {
        fun fromId(id: String): PaymentGateway {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: ESPECE
        }
    }
}
