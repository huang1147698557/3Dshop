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
        IdeaHistory::class,
        Material::class,
        InventoryLog::class
    ],
    version = 8,
    exportSchema = true
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
    abstract fun materialDao(): MaterialDao
    abstract fun inventoryLogDao(): InventoryLogDao

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
                .addMigrations(
                    MIGRATION_1_2, 
                    MIGRATION_2_3, 
                    MIGRATION_3_4, 
                    MIGRATION_4_5, 
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS materials (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        remainingPercentage INTEGER NOT NULL,
                        imageUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        operationType TEXT NOT NULL,
                        beforeCount INTEGER NOT NULL,
                        afterCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 空迁移，保持数据库结构不变
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 空迁移，保持数据库结构不变
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 空迁移，保持数据库结构不变
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 重新创建数据库表以确保架构一致性
                database.execSQL("DROP TABLE IF EXISTS materials")
                database.execSQL("DROP TABLE IF EXISTS inventory_logs")
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS materials (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        remainingPercentage INTEGER NOT NULL,
                        imageUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        operationType TEXT NOT NULL,
                        beforeCount INTEGER NOT NULL,
                        afterCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建临时表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS materials_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        material TEXT NOT NULL DEFAULT 'PLA',
                        color TEXT,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        remainingPercentage INTEGER NOT NULL,
                        imageUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                // 复制数据到临时表
                database.execSQL("""
                    INSERT INTO materials_temp (
                        id, name, color, price, quantity, remainingPercentage, 
                        imageUrl, createdAt, updatedAt
                    )
                    SELECT id, name, color, price, quantity, remainingPercentage, 
                           imageUrl, createdAt, updatedAt
                    FROM materials
                """)

                // 删除旧表
                database.execSQL("DROP TABLE materials")

                // 重命名临时表为正式表
                database.execSQL("ALTER TABLE materials_temp RENAME TO materials")
            }
        }
    }
} 