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
        InventoryLog::class
    ],
    version = 6,
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加remainingCount字段，默认值设置为quantity
                database.execSQL("ALTER TABLE products ADD COLUMN remainingCount INTEGER NOT NULL DEFAULT 1")
                database.execSQL("UPDATE products SET remainingCount = quantity")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建库存日志表
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建临时表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sale_records_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER,
                        name TEXT NOT NULL,
                        salePrice REAL NOT NULL,
                        imageUrl TEXT,
                        note TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (productId) REFERENCES products(id) ON DELETE SET NULL
                    )
                """)

                // 复制数据
                database.execSQL("""
                    INSERT INTO sale_records_temp (id, productId, name, salePrice, imageUrl, note, createdAt)
                    SELECT id, productId, name, salePrice, imageUrl, note, createdAt FROM sale_records
                """)

                // 删除旧表
                database.execSQL("DROP TABLE sale_records")

                // 重命名新表
                database.execSQL("ALTER TABLE sale_records_temp RENAME TO sale_records")

                // 创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sale_records_productId ON sale_records(productId)")
            }
        }
    }
} 