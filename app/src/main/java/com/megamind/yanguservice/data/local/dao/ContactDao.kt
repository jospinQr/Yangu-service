package com.megamind.yanguservice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.megamind.yanguservice.data.local.dto.ContactDto
import com.megamind.yanguservice.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<ContactDto>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContactDto?

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    @Query("SELECT phoneNumber FROM contacts")
    suspend fun getAllPhoneNumbers(): List<String>

    @Insert
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String)
}
