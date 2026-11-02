package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CotisationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CotisationDao {
    /**
     * Anti-doublons idempotent : ignore silencieusement toute transaction déjà enregistrée.
     * Renvoie -1 si la transaction existe déjà.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreDuplicate(cotisation: CotisationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoreDuplicates(cotisations: List<CotisationEntity>): List<Long>

    @Query("SELECT * FROM cotisations ORDER BY datePaiement DESC")
    fun getAllCotisationsFlow(): Flow<List<CotisationEntity>>

    @Query("SELECT * FROM cotisations WHERE membreUid = :membreUid ORDER BY datePaiement DESC")
    fun getCotisationsForMemberFlow(membreUid: String): Flow<List<CotisationEntity>>

    @Query("SELECT * FROM cotisations WHERE idCampagne = :idCampagne ORDER BY datePaiement DESC")
    fun getCotisationsForCampaignFlow(idCampagne: String): Flow<List<CotisationEntity>>

    @Query("SELECT * FROM cotisations WHERE idTransaction = :idTransaction LIMIT 1")
    suspend fun getCotisationById(idTransaction: String): CotisationEntity?

    @Query("SELECT SUM(montant) FROM cotisations WHERE idCampagne = :idCampagne")
    suspend fun getTotalForCampaign(idCampagne: String): Double?

    @Query("SELECT SUM(montant) FROM cotisations")
    suspend fun getTotalCollectedOverall(): Double?

    @Query("SELECT SUM(montant) FROM cotisations WHERE datePaiement >= :startOfMonthTimestamp")
    suspend fun getTotalCollectedSince(startOfMonthTimestamp: Long): Double?

    @Query("SELECT * FROM cotisations WHERE syncedToFirebase = 0")
    suspend fun getUnsyncedCotisations(): List<CotisationEntity>

    @Query("UPDATE cotisations SET syncedToFirebase = 1 WHERE idTransaction = :idTransaction")
    suspend fun markAsSynced(idTransaction: String)

    @Query("SELECT COUNT(*) FROM cotisations")
    suspend fun getCotisationCount(): Int

    @Query("UPDATE cotisations SET statut = :nouveauStatut, valideParUid = :valideParUid, dateValidation = :timestamp, syncedToFirebase = 0 WHERE idTransaction = :idTransaction")
    suspend fun updateStatut(idTransaction: String, nouveauStatut: String, valideParUid: String, timestamp: Long)

    @Query("SELECT * FROM cotisations WHERE statut = 'EN_ATTENTE' ORDER BY datePaiement DESC")
    fun getPendingCotisationsFlow(): Flow<List<CotisationEntity>>

    @Query("SELECT COUNT(*) FROM cotisations WHERE statut = 'EN_ATTENTE'")
    suspend fun getPendingCotisationCount(): Int
}
