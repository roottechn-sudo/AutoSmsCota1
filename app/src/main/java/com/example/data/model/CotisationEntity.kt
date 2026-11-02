package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
        Index("membreUid")
    ]
)
data class CotisationEntity(
    @PrimaryKey
    val idTransaction: String,
    val idCampagne: String,
    val membreUid: String,
    val montant: Double,
    val datePaiement: Long = System.currentTimeMillis(),
    val methodePaiement: String, // "ORANGE_MONEY" ou "ESPECE"
    val enregistreParUid: String,
    val recuGenere: Boolean = true,
    val syncedToFirebase: Boolean = false,
    val referenceExterne: String? = null // expéditeur / numéro Orange Money
)
