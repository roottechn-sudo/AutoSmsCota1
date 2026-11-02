package com.example.data.model

enum class UserRole(val labelFr: String, val badgeColorHex: Long) {
    MEMBRE("Membre Simple", 0xFF607D8B),
    SECRETAIRE("Secrétaire Général", 0xFF1976D2),
    TRESORIER("Trésorier", 0xFF388E3C),
    ADMINISTRATEUR("Administrateur", 0xFFD32F2F)
}
