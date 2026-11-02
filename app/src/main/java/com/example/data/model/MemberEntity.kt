package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey
    val uid: String,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val role: UserRole,
    val carteValide: Boolean = true,
    val soldeArrieres: Double = 0.0,
    val email: String? = null,
    val categorie: String = "Membre Actif", // "Membre Actif", "Membre Fondateur", "Membre d'Honneur", "Étudiant / Jeunesse", "Sympathisant"
    val dateAdhesion: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val joursRetardEstimes: Int = 0 // Jours de retard pour les arriérés
) {
    val nomComplet: String
        get() = "$prenom $nom".trim()

    companion object {
        val CATEGORIES = listOf(
            "Membre Actif",
            "Membre Fondateur",
            "Membre d'Honneur",
            "Étudiant / Jeunesse",
            "Sympathisant"
        )
    }
}
