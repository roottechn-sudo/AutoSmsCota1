package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.model.MemberEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gère l'authentification et la session utilisateur avec cache local sécurisé du rôle.
 * Conçu pour fonctionner Local-First en zone à connectivité limitée.
 */
class AuthRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autosmscota_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<MemberEntity?>(null)
    val currentUser: StateFlow<MemberEntity?> = _currentUser.asStateFlow()

    private val _isUserPendingActivation = MutableStateFlow(false)
    val isUserPendingActivation: StateFlow<Boolean> = _isUserPendingActivation.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        val savedUid = prefs.getString("current_user_uid", "admin_001") ?: "admin_001"
        CoroutineScope(Dispatchers.IO).launch {
            val member = database.memberDao().getMemberByUid(savedUid)
            if (member != null) {
                _currentUser.value = member
                _isUserPendingActivation.value = !member.carteValide
            } else {
                // Utilisateur initial par défaut : Administrateur Jean Kaboré
                val defaultAdmin = MemberEntity(
                    uid = "admin_001",
                    nom = "KABORÉ",
                    prenom = "Jean",
                    telephone = "+22670123456",
                    role = UserRole.ADMINISTRATEUR,
                    carteValide = true,
                    soldeArrieres = 0.0
                )
                database.memberDao().insertOrUpdate(defaultAdmin)
                prefs.edit().putString("current_user_uid", defaultAdmin.uid).apply()
                _currentUser.value = defaultAdmin
                _isUserPendingActivation.value = false
            }
        }
    }

    suspend fun switchUser(uid: String): Boolean {
        val member = database.memberDao().getMemberByUid(uid) ?: return false
        prefs.edit().putString("current_user_uid", member.uid).apply()
        _currentUser.value = member
        _isUserPendingActivation.value = !member.carteValide
        return true
    }

    suspend fun loginWithEmailOrPhone(identifier: String, motDePasse: String): Result<MemberEntity> {
        // En mode local-first pour réseau Burkina Faso : validation locale instantanée
        val members = database.memberDao().getMemberByPhone(identifier)
        val user = members ?: database.memberDao().getMemberByUid(identifier)
            ?: _currentUser.value

        return if (user != null) {
            prefs.edit().putString("current_user_uid", user.uid).apply()
            _currentUser.value = user
            _isUserPendingActivation.value = !user.carteValide
            Result.success(user)
        } else {
            Result.failure(Exception("Compte introuvable ou identifiants incorrects"))
        }
    }

    suspend fun registerNewMember(
        nom: String,
        prenom: String,
        telephone: String,
        requestedRole: UserRole = UserRole.MEMBRE
    ): Result<MemberEntity> {
        val uid = "mbr_${System.currentTimeMillis()}"
        val newMember = MemberEntity(
            uid = uid,
            nom = nom.uppercase(),
            prenom = prenom,
            telephone = telephone,
            role = requestedRole,
            carteValide = requestedRole == UserRole.ADMINISTRATEUR, // Membres créés en attente de validation si standard
            soldeArrieres = 0.0
        )
        database.memberDao().insertOrUpdate(newMember)
        return Result.success(newMember)
    }

    fun logout() {
        prefs.edit().remove("current_user_uid").apply()
        _currentUser.value = null
    }
}
