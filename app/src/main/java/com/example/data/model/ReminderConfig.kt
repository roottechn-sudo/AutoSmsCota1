package com.example.data.model

data class ReminderConfig(
    val daysPastDueThreshold: Int = 15, // Nombre de jours de retard avant rappel
    val messageTemplate: String = "Bonjour {PRENOM}, l'Association RAVA vous rappelle que votre cotisation pour '{CAMPAGNE}' présente un arriéré de {MONTANT}. Merci de régulariser par Orange Money, Moov, Wave ou LigdiCash. Info bureau : +226 70 12 34 56",
    val autoSendEnabled: Boolean = false,
    val lastRunDate: Long = 0L
) {
    fun formatMessage(member: MemberEntity, campaignTitle: String, formattedAmount: String): String {
        return messageTemplate
            .replace("{NOM}", member.nom)
            .replace("{PRENOM}", member.prenom)
            .replace("{MONTANT}", formattedAmount)
            .replace("{CAMPAGNE}", campaignTitle)
            .replace("{TELEPHONE}", member.telephone)
            .replace("{CATEGORIE}", member.categorie)
    }

    companion object {
        val AVAILABLE_TAGS = listOf(
            "{PRENOM}",
            "{NOM}",
            "{MONTANT}",
            "{CAMPAGNE}",
            "{TELEPHONE}",
            "{CATEGORIE}"
        )
    }
}
