package com.sgm.a3dshop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sgm.a3dshop.data.dao.ProductDao
import com.sgm.a3dshop.data.dao.SaleRecordDao
import com.sgm.a3dshop.data.dao.VoiceNoteDao
import com.sgm.a3dshop.data.dao.PendingProductDao
import com.sgm.a3dshop.data.entity.Product
import com.sgm.a3dshop.data.entity.SaleRecord
import com.sgm.a3dshop.data.entity.VoiceNote
import com.sgm.a3dshop.data.entity.PendingProduct

@Database(
    entities = [
        Product::class,
        SaleRecord::class,
        VoiceNote::class,
        PendingProduct::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleRecordDao(): SaleRecordDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun pendingProductDao(): PendingProductDao

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