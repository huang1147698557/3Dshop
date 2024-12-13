package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.IdeaRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaRecordDao {
    @Query("SELECT * FROM idea_records ORDER BY createdAt DESC")
    fun getAllByCreatedAtDesc(): Flow<List<IdeaRecord>>

    @Query("SELECT * FROM idea_records ORDER BY createdAt ASC")
    fun getAllByCreatedAtAsc(): Flow<List<IdeaRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ideaRecord: IdeaRecord): Long

    @Update
    suspend fun update(ideaRecord: IdeaRecord)

    @Delete
    suspend fun delete(ideaRecord: IdeaRecord)

    @Query("SELECT * FROM idea_records WHERE id = :id")
    suspend fun getById(id: Int): IdeaRecord?

    @Query("DELETE FROM idea_records")
    suspend fun deleteAll()
} 