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
        PendingProduct::class,
        PendingHistory::class,
        IdeaRecord::class,
        IdeaHistory::class,
        VoiceNote::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleRecordDao(): SaleRecordDao
    abstract fun pendingProductDao(): PendingProductDao
    abstract fun pendingHistoryDao(): PendingHistoryDao
    abstract fun ideaRecordDao(): IdeaRecordDao
    abstract fun ideaHistoryDao(): IdeaHistoryDao
    abstract fun voiceNoteDao(): VoiceNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val userId = context.packageName + "_" + context.getSystemService(Context.USER_SERVICE).hashCode()
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "3dshop_db_$userId"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 