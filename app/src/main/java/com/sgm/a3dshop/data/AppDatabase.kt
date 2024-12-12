package com.sgm.a3dshop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sgm.a3dshop.data.dao.ProductDao
import com.sgm.a3dshop.data.dao.SaleRecordDao
import com.sgm.a3dshop.data.dao.VoiceNoteDao
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.entity.VoiceNote

@Database(
    entities = [
        Product::class,
        SaleRecord::class,
        VoiceNote::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleRecordDao(): SaleRecordDao
    abstract fun voiceNoteDao(): VoiceNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 删除旧表
                database.execSQL("DROP TABLE IF EXISTS sale_records")
                
                // 创建新表
                database.execSQL("""
                    CREATE TABLE sale_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER,
                        name TEXT NOT NULL,
                        salePrice REAL NOT NULL,
                        imageUrl TEXT,
                        note TEXT,
                        createTime INTEGER NOT NULL,
                        FOREIGN KEY(productId) REFERENCES products(id) 
                        ON UPDATE CASCADE 
                        ON DELETE CASCADE
                    )
                """)
                
                // 创建索引
                database.execSQL("CREATE INDEX index_sale_records_productId ON sale_records(productId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 删除旧表
                database.execSQL("DROP TABLE IF EXISTS sale_records")
                
                // 创建新表，使用 SET NULL 而不是 CASCADE
                database.execSQL("""
                    CREATE TABLE sale_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER,
                        name TEXT NOT NULL,
                        salePrice REAL NOT NULL,
                        imageUrl TEXT,
                        note TEXT,
                        createTime INTEGER NOT NULL,
                        FOREIGN KEY(productId) REFERENCES products(id) 
                        ON UPDATE CASCADE 
                        ON DELETE SET NULL
                    )
                """)
                
                // 创建索引
                database.execSQL("CREATE INDEX index_sale_records_productId ON sale_records(productId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 