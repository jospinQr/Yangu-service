package com.megamind.yanguservice.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.megamind.yanguservice.data.local.dao.ContactDao
import com.megamind.yanguservice.data.local.entity.ContactEntity

@Database(entities = [ContactEntity::class], version = 1)
abstract class AppDataBase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
}
