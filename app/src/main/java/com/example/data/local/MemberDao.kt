package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MemberEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY nom ASC, prenom ASC")
    fun getAllMembersFlow(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE uid = :uid LIMIT 1")
    suspend fun getMemberByUid(uid: String): MemberEntity?

    @Query("SELECT * FROM members WHERE telephone = :phone LIMIT 1")
    suspend fun getMemberByPhone(phone: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>)

    @Query("UPDATE members SET role = :newRole WHERE uid = :uid")
    suspend fun updateRole(uid: String, newRole: UserRole)

    @Query("UPDATE members SET soldeArrieres = :solde WHERE uid = :uid")
    suspend fun updateArrears(uid: String, solde: Double)

    @Query("UPDATE members SET carteValide = :isValid WHERE uid = :uid")
    suspend fun updateCardStatus(uid: String, isValid: Boolean)

    @Query("SELECT COUNT(*) FROM members")
    suspend fun getMemberCount(): Int
}
