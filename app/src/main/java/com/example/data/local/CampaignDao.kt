package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EventCampaignEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM event_campaigns ORDER BY dateCreation DESC")
    fun getAllCampaignsFlow(): Flow<List<EventCampaignEntity>>

    @Query("SELECT * FROM event_campaigns WHERE estCloture = 0 ORDER BY dateCreation DESC")
    fun getActiveCampaignsFlow(): Flow<List<EventCampaignEntity>>

    @Query("SELECT * FROM event_campaigns WHERE idCampagne = :idCampagne LIMIT 1")
    suspend fun getCampaignById(idCampagne: String): EventCampaignEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(campaign: EventCampaignEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(campaigns: List<EventCampaignEntity>)

    @Update
    suspend fun update(campaign: EventCampaignEntity)

    @Query("UPDATE event_campaigns SET estCloture = 1, dateCloture = :dateCloture WHERE idCampagne = :idCampagne")
    suspend fun closeCampaign(idCampagne: String, dateCloture: Long = System.currentTimeMillis())

    @Query("UPDATE event_campaigns SET budgetReel = :budgetReel WHERE idCampagne = :idCampagne")
    suspend fun updateBudgetReel(idCampagne: String, budgetReel: Double)

    @Query("SELECT COUNT(*) FROM event_campaigns WHERE estCloture = 0")
    suspend fun getActiveCampaignCount(): Int
}
