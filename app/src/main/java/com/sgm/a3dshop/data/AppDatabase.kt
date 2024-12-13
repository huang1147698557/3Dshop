package com.sgm.a3dshop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sgm.a3dshop.data.dao.*
import com.sgm.a3dshop.data.entity.*

@Database(
    entities = [
        Product::class,
        SaleRecord::class,
        VoiceNote::class,
        PendingProduct::class,
        PendingHistory::class,
        IdeaRecord::class,
        IdeaHistory::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleRecordDao(): SaleRecordDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun pendingProductDao(): PendingProductDao
    abstract fun pendingHistoryDao(): PendingHistoryDao
    abstract fun ideaRecordDao(): IdeaRecordDao
    abstract fun ideaHistoryDao(): IdeaHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 