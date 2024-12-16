package com.sgm.a3dshop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加新字段
                database.execSQL("ALTER TABLE products ADD COLUMN laborCost REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE products ADD COLUMN plateCount INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE products ADD COLUMN materialUnitPrice REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE products ADD COLUMN profitRate REAL NOT NULL DEFAULT 0.3")
                database.execSQL("ALTER TABLE products ADD COLUMN weight REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE products ADD COLUMN printTime INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN postProcessingCost REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
} 