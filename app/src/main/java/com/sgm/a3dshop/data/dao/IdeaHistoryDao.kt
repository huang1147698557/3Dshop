package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.IdeaHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaHistoryDao {
    @Query("SELECT * FROM idea_history ORDER BY deletedAt DESC")
    fun getAllByDeletedAtDesc(): Flow<List<IdeaHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ideaHistory: IdeaHistory)

    @Delete
    suspend fun delete(ideaHistory: IdeaHistory)

    @Query("DELETE FROM idea_history")
    suspend fun deleteAll()
} 