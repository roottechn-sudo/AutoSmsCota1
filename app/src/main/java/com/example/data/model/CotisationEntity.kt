package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Énumération des statuts possibles pour une cotisation / transaction.
 *
 * Directives de sécurité et logique métier :
 * - EN_ATTENTE : Transaction capturée (ex: notification SMS Orange Money) ou déclarée,
 *   en attente de validation physique par le Trésorier ou l'Administrateur pour contrer
 *   les attaques d'usurpation / spoofing de SMS.
 * - VALIDEE : Transaction vérifiée et validée. Le montant est officiellement déduit des
 *   arriérés du membre et comptabilisé dans le budget de la campagne.
 * - REJETEE : Transaction frauduleuse, doublon ou invalide rejetée par le bureau.
 */
enum class StatutCotisation(
    val code: String,
    val labelFr: String,
    val badgeColorHex: Long = 0xFFFF9800
) {
    EN_ATTENTE("EN_ATTENTE", "En Attente de Validation", 0xFFFF9800),
    VALIDEE("VALIDEE", "Validée", 0xFF2E7D32),
    REJETEE("REJETEE", "Rejetée", 0xFFD32F2F);

    companion object {
        fun fromCode(code: String?): StatutCotisation {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: EN_ATTENTE
        }
    }
}

/**
 * Modèle de données persistant Room pour une cotisation / transaction financière.
 *
 * Intègre les champs essentiels requis :
 * - id / idTransaction : Clé primaire unique de la transaction
 * - montant : Montant de la cotisation en FCFA
 * - date / datePaiement : Horodatage de l'opération
 * - nomMembre : Nom complet du membre ou contributeur
 * - statut : Statut sécurisé sous forme d'Enum (EN_ATTENTE, VALIDEE, REJETEE)
 */
@Entity(
    tableName = "cotisations",
    foreignKeys = [
        ForeignKey(
            entity = EventCampaignEntity::class,
            parentColumns = ["idCampagne"],
            childColumns = ["idCampagne"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("idCampagne"),
        Index("membreUid"),
        Index("statut")
    ]
)
data class CotisationEntity(
    @PrimaryKey
    val idTransaction: String, // Identifiant unique de transaction (Room @PrimaryKey)
    val idCampagne: String = "CAMP_ANNUELLE_2026",
    val membreUid: String = "",
    val nomMembre: String? = null, // Nom dénormalisé du membre ou bienfaiteur
    val montant: Double,
    val datePaiement: Long = System.currentTimeMillis(),
    val methodePaiement: String = "ORANGE_MONEY",
    val enregistreParUid: String = "",
    val recuGenere: Boolean = true,
    val syncedToFirebase: Boolean = false,
    val referenceExterne: String? = null,
    val isAnonyme: Boolean = false,
    val donateurNom: String? = null,
    val donateurTelephone: String? = null,
    val typeContributeur: String = "MEMBRE",
    val statut: String = StatutCotisation.EN_ATTENTE.code,
    val valideParUid: String? = null,
    val dateValidation: Long? = null
) {
    /**
     * Constructeur secondaire pratique initialisant les champs requis par l'énoncé.
     * Ignoré par Room pour préserver le constructeur principal d'hydratation SQLite.
     */
    @Ignore
    constructor(
        id: String,
        montant: Double,
        date: Long = System.currentTimeMillis(),
        nomMembre: String? = null,
        statut: StatutCotisation = StatutCotisation.EN_ATTENTE
    ) : this(
        idTransaction = id,
        idCampagne = "CAMP_ANNUELLE_2026",
        membreUid = "",
        nomMembre = nomMembre,
        montant = montant,
        datePaiement = date,
        methodePaiement = "ORANGE_MONEY",
        enregistreParUid = "",
        recuGenere = true,
        syncedToFirebase = false,
        referenceExterne = null,
        isAnonyme = false,
        donateurNom = nomMembre,
        donateurTelephone = null,
        typeContributeur = "MEMBRE",
        statut = statut.code,
        valideParUid = null,
        dateValidation = null
    )

    // =========================================================================
    // Accesseurs conformes aux spécifications de l'énoncé ('id', 'date', 'statut')
    // =========================================================================
    val id: String
        get() = idTransaction

    val date: Long
        get() = datePaiement

    val statutEnum: StatutCotisation
        get() = StatutCotisation.fromCode(statut)

    val isValidee: Boolean
        get() = statut == StatutCotisation.VALIDEE.code

    val isEnAttente: Boolean
        get() = statut == StatutCotisation.EN_ATTENTE.code

    val isRejetee: Boolean
        get() = statut == StatutCotisation.REJETEE.code

    /**
     * Nom d'affichage contextuel pour les reçus et listes.
     */
    fun getDisplayName(memberName: String?): String {
        return when (typeContributeur) {
            "ANONYME" -> "Donateur Anonyme"
            "SYMPATHISANT" -> nomMembre ?: donateurNom?.ifBlank { null } ?: "Ami & Sympathisant RAVA"
            else -> memberName ?: nomMembre ?: donateurNom ?: "Membre N° $membreUid"
        }
    }
}
