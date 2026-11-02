package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_campaigns")
data class EventCampaignEntity(
    @PrimaryKey
    val idCampagne: String,
    val titre: String,
    val description: String,
    val dateCreation: Long = System.currentTimeMillis(),
    val dateCloture: Long? = null,
    val estCloture: Boolean = false,
    val budgetEstime: Double = 0.0,
    val budgetReel: Double = 0.0,
    val creeParUid: String
)
