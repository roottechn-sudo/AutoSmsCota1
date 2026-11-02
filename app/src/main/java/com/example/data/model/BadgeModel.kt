package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class BadgeTier(val label: String, val color: Color, val containerColor: Color) {
    BRONZE("Bronze", Color(0xFFCD7F32), Color(0xFFF7EBE1)),
    SILVER("Argent", Color(0xFF757575), Color(0xFFEEEEEE)),
    GOLD("Or", Color(0xFFFFB300), Color(0xFFFFF8E1)),
    EMERALD("Émeraude", Color(0xFF00796B), Color(0xFFE0F2F1))
}

data class ContributionBadge(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val isUnlocked: Boolean,
    val progress: Float, // 0.0f to 1.0f
    val progressText: String,
    val tier: BadgeTier,
    val icon: ImageVector,
    val unlockedDate: Long? = null
)

object BadgeCalculator {

    fun calculateBadges(
        member: MemberEntity,
        cotisations: List<CotisationEntity>,
        campaigns: List<EventCampaignEntity>
    ): List<ContributionBadge> {
        val memberCotisations = cotisations.filter { it.membreUid == member.uid }
        val count = memberCotisations.size
        val totalAmount = memberCotisations.sumOf { it.montant }
        val distinctCampaigns = memberCotisations.map { it.idCampagne }.distinct().size
        val hasZeroArrears = member.soldeArrieres <= 0.0

        val badges = mutableListOf<ContributionBadge>()

        // 1. Premier Pas (First Step)
        val firstStepUnlocked = count >= 1
        badges.add(
            ContributionBadge(
                id = "first_step",
                title = "Premier Pas",
                subtitle = "Première cotisation validée",
                description = "Récompense attribuée dès votre premier engagement financier dans l'association.",
                isUnlocked = firstStepUnlocked,
                progress = if (firstStepUnlocked) 1f else (count / 1f).coerceIn(0f, 1f),
                progressText = if (firstStepUnlocked) "Validé" else "0 / 1 cotisation",
                tier = BadgeTier.BRONZE,
                icon = Icons.Default.Star,
                unlockedDate = memberCotisations.lastOrNull()?.datePaiement
            )
        )

        // 2. Early Bird / Lève-tôt Ponctuel
        val earlyBirdUnlocked = hasZeroArrears && count >= 1
        badges.add(
            ContributionBadge(
                id = "early_bird",
                title = "Lève-tôt Ponctuel",
                subtitle = "À jour sans aucun arriéré",
                description = "Récompense les membres exemplaires qui règlent leurs cotisations sans aucun retard.",
                isUnlocked = earlyBirdUnlocked,
                progress = if (earlyBirdUnlocked) 1f else if (member.soldeArrieres > 0) 0.3f else 0.8f,
                progressText = if (earlyBirdUnlocked) "À jour (0 arriéré)" else "Arriérés: ${member.soldeArrieres.toInt()} FCFA",
                tier = BadgeTier.EMERALD,
                icon = Icons.Default.Bolt,
                unlockedDate = if (earlyBirdUnlocked) System.currentTimeMillis() else null
            )
        )

        // 3. Contributeur Assidu (Consistent Contributor)
        val consistentUnlocked = count >= 3
        badges.add(
            ContributionBadge(
                id = "consistent_contributor",
                title = "Contributeur Assidu",
                subtitle = "3 cotisations régulières",
                description = "Démontre une constance admirable et une fidélité continue aux idéaux de l'association.",
                isUnlocked = consistentUnlocked,
                progress = (count / 3f).coerceIn(0f, 1f),
                progressText = "$count / 3 cotisations",
                tier = BadgeTier.SILVER,
                icon = Icons.Default.Verified,
                unlockedDate = if (consistentUnlocked) memberCotisations.getOrNull(2)?.datePaiement else null
            )
        )

        // 4. Bienfaiteur Majeur (Milestone Hero)
        val milestoneTarget = 50000.0
        val milestoneUnlocked = totalAmount >= milestoneTarget
        badges.add(
            ContributionBadge(
                id = "milestone_hero",
                title = "Bienfaiteur Majeur",
                subtitle = "Plus de 50 000 FCFA cumulés",
                description = "Distinction honorifique pour un impact financier significatif dans la caisse commune.",
                isUnlocked = milestoneUnlocked,
                progress = (totalAmount / milestoneTarget).toFloat().coerceIn(0f, 1f),
                progressText = "${totalAmount.toInt()} / ${milestoneTarget.toInt()} FCFA",
                tier = BadgeTier.GOLD,
                icon = Icons.Default.EmojiEvents,
                unlockedDate = if (milestoneUnlocked) System.currentTimeMillis() else null
            )
        )

        // 5. Pilier des Campagnes (Campaign Champion)
        val campaignChampionUnlocked = distinctCampaigns >= 2
        badges.add(
            ContributionBadge(
                id = "campaign_champion",
                title = "Pilier des Campagnes",
                subtitle = "Soutien à au moins 2 projets distincts",
                description = "Engagement actif dans la diversification des activités et des projets de solidarité.",
                isUnlocked = campaignChampionUnlocked,
                progress = (distinctCampaigns / 2f).coerceIn(0f, 1f),
                progressText = "$distinctCampaigns / 2 projets",
                tier = BadgeTier.EMERALD,
                icon = Icons.Default.Campaign,
                unlockedDate = if (campaignChampionUnlocked) System.currentTimeMillis() else null
            )
        )

        // 6. Gardien des Valeurs (Honor Guard)
        val isHonored = member.categorie in listOf("Membre Fondateur", "Membre d'Honneur") ||
                member.role in listOf(UserRole.ADMINISTRATEUR, UserRole.TRESORIER)
        badges.add(
            ContributionBadge(
                id = "honor_guard",
                title = "Gardien des Valeurs",
                subtitle = "Dévouement et statut d'honneur",
                description = "Attribué aux bâtisseurs et cadres dévoués à la transmission des valeurs ancestrales.",
                isUnlocked = isHonored,
                progress = if (isHonored) 1f else 0.5f,
                progressText = if (isHonored) member.categorie else "Accessible aux fondateurs",
                tier = BadgeTier.GOLD,
                icon = Icons.Default.Shield,
                unlockedDate = if (isHonored) member.dateAdhesion else null
            )
        )

        return badges
    }
}
