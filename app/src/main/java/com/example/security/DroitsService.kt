package com.example.security

import com.example.data.model.UserRole

/**
 * Service de sécurité applicative multi-couches.
 * Valide les autorisations locales selon la matrice des rôles RAVA / AutoSmsCota.
 */
object DroitsService {

    fun canCreateCampaign(role: UserRole): Boolean {
        return role == UserRole.SECRETAIRE || role == UserRole.ADMINISTRATEUR
    }

    fun canEditCampaign(role: UserRole): Boolean {
        return role == UserRole.SECRETAIRE || role == UserRole.ADMINISTRATEUR
    }

    fun canCloseCampaign(role: UserRole): Boolean {
        return role == UserRole.SECRETAIRE || role == UserRole.ADMINISTRATEUR
    }

    fun canRecordCotisation(role: UserRole): Boolean {
        return role == UserRole.TRESORIER || role == UserRole.ADMINISTRATEUR
    }

    fun canScanSms(role: UserRole): Boolean {
        return role == UserRole.TRESORIER || role == UserRole.ADMINISTRATEUR
    }

    fun canGenerateReceipt(role: UserRole): Boolean {
        // Le trésorier et l'administrateur génèrent; les membres peuvent consulter/partager leur reçu
        return true
    }

    fun canManageMembers(role: UserRole): Boolean {
        return role == UserRole.ADMINISTRATEUR
    }

    fun canChangeUserRole(role: UserRole): Boolean {
        return role == UserRole.ADMINISTRATEUR
    }

    fun canViewFinancialReports(role: UserRole): Boolean {
        return role == UserRole.TRESORIER || role == UserRole.ADMINISTRATEUR
    }

    fun getDeniedMessage(role: UserRole, action: String): String {
        return when (role) {
            UserRole.MEMBRE -> "Votre rôle [Membre Simple] est en lecture seule. Vous ne pouvez pas $action. Veuillez contacter le bureau exécutif de l'association."
            UserRole.SECRETAIRE -> "Accès réservé au pôle Financier (Trésorier/Admin). Le rôle [Secrétaire Général] gère uniquement les campagnes et PVs."
            UserRole.TRESORIER -> "Accès réservé au Secrétariat ou à la Direction. Le rôle [Trésorier] ne gère que les flux financiers et cotisations."
            UserRole.ADMINISTRATEUR -> "Action non autorisée."
        }
    }
}
