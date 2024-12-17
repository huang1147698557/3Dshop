package com.sgm.a3dshop.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.sgm.a3dshop.data.entity.Material

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials ORDER BY createdAt DESC")
    fun getAllMaterials(): LiveData<List<Material>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: Material)

    @Update
    suspend fun update(material: Material)

    @Delete
    suspend fun delete(material: Material)

    @Query("SELECT * FROM materials WHERE id = :id")
    suspend fun getMaterialById(id: Long): Material?
} 