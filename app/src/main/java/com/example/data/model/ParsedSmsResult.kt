package com.example.data.model

enum class OrangeMoneySmsType(val labelFr: String) {
    RECETTE_DEPOT_TRANSFERT("Recette - Dépôt / Transfert Reçu"),
    DEPENSE_TRANSFERT_EMIS("Dépense - Transfert Émis"),
    DEPENSE_PAIEMENT("Dépense - Paiement Effectué"),
    INCONNU("Format Non Reconnu")
}

data class ParsedSmsResult(
    val isValid: Boolean,
    val type: OrangeMoneySmsType,
    val transactionId: String,
    val amount: Double,
    val party: String, // Expéditeur ou Destinataire
    val fees: Double = 0.0,
    val taxes: Double = 0.0,
    val balanceAfter: Double? = null,
    val rawBody: String,
    val timestamp: Long = System.currentTimeMillis()
)
