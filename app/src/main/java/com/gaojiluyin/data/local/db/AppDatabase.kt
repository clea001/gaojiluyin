package com.gaojiluyin.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gaojiluyin.data.local.db.dao.DocumentDao
import com.gaojiluyin.data.local.db.dao.RecordingDao
import com.gaojiluyin.data.local.db.entity.DocumentEntity
import com.gaojiluyin.data.local.db.entity.RecordingEntity

@Database(
    entities = [RecordingEntity::class, DocumentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun documentDao(): DocumentDao
}
