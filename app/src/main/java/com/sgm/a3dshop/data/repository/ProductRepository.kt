package com.sgm.a3dshop.data.repository

import com.sgm.a3dshop.data.dao.ProductDao
import com.sgm.a3dshop.data.entity.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun insertProducts(products: List<Product>) {
        productDao.insertProducts(products)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }
    
    suspend fun deleteAllProducts() {
        productDao.deleteAllProducts()
    }
} 