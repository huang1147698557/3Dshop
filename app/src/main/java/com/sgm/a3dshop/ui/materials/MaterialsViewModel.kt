package com.sgm.a3dshop.ui.materials

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Material
import kotlinx.coroutines.launch
import java.util.*

class MaterialsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val materialDao = database.materialDao()

    val materials: LiveData<List<Material>> = materialDao.getAllMaterials()

    fun addMaterial(material: Material) {
        viewModelScope.launch {
            materialDao.insert(material)
        }
    }

    fun updateMaterial(material: Material) {
        viewModelScope.launch {
            materialDao.update(material)
        }
    }

    suspend fun getMaterialById(id: Long): Material? {
        return materialDao.getMaterialById(id)
    }

    fun updateMaterialQuantity(material: Material, newQuantity: Int) {
        viewModelScope.launch {
            val updatedMaterial = material.copy(
                quantity = newQuantity,
                updatedAt = Date()
            )
            materialDao.update(updatedMaterial)
        }
    }

    fun updateMaterialRemaining(material: Material, newPercentage: Int) {
        viewModelScope.launch {
            materialDao.update(material.copy(
                remainingPercentage = newPercentage,
                updatedAt = Date()
            ))
        }
    }

    fun deleteMaterial(material: Material) {
        viewModelScope.launch {
            materialDao.delete(material)
        }
    }
} 