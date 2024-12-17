package com.sgm.a3dshop.ui.materials

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sgm.a3dshop.data.AppDatabase
import com.sgm.a3dshop.data.entity.Material
import kotlinx.coroutines.launch
import java.util.*

class MaterialsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val materialDao = database.materialDao()

    private val _sortByRemaining = MutableLiveData(false)
    val sortByRemaining: LiveData<Boolean> = _sortByRemaining

    private val _materials = MediatorLiveData<List<Material>>()
    val materials: LiveData<List<Material>> = _materials

    private val rawMaterials = materialDao.getAllMaterials()

    init {
        _materials.addSource(rawMaterials) { list ->
            updateSortedList(list, _sortByRemaining.value ?: false)
        }
        _materials.addSource(_sortByRemaining) { sortByRemaining ->
            updateSortedList(rawMaterials.value ?: emptyList(), sortByRemaining)
        }
    }

    private fun updateSortedList(list: List<Material>, sortByRemaining: Boolean) {
        Log.d("MaterialsViewModel", "Updating sorted list, sortByRemaining: $sortByRemaining")
        val sortedList = if (sortByRemaining) {
            Log.d("MaterialsViewModel", "Sorting by remaining percentage")
            list.sortedByDescending { it.remainingPercentage }
        } else {
            Log.d("MaterialsViewModel", "Sorting by updated time")
            list.sortedByDescending { it.updatedAt.time }
        }
        _materials.value = sortedList
    }

    fun toggleSortByRemaining() {
        Log.d("MaterialsViewModel", "Toggle sort by remaining called")
        val newValue = !(_sortByRemaining.value ?: false)
        Log.d("MaterialsViewModel", "New sort value: $newValue")
        _sortByRemaining.value = newValue
    }

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