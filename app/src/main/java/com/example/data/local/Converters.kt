package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole?): String? {
        return role?.name
    }

    @TypeConverter
    fun toUserRole(value: String?): UserRole {
        return value?.let {
            try {
                UserRole.valueOf(it)
            } catch (e: Exception) {
                UserRole.MEMBRE
            }
        } ?: UserRole.MEMBRE
    }
}
