package com.sgm.a3dshop.data

import android.os.Parcel
import androidx.room.TypeConverter
import java.util.*

object Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

object DateParcelConverter {
    fun writeDate(parcel: Parcel, date: Date?) {
        parcel.writeLong(date?.time ?: 0)
    }

    fun readDate(parcel: Parcel): Date {
        return Date(parcel.readLong())
    }
} 