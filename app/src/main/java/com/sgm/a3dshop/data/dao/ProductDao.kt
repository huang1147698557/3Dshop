package com.sgm.a3dshop.data.dao

import androidx.room.*
import com.sgm.a3dshop.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE :query ORDER BY createdAt DESC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: Int): Product

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    suspend fun getAllProductsSync(): List<Product>

    @Query("UPDATE products SET remainingCount = :newCount WHERE id = :productId")
    suspend fun updateProductRemainingCount(productId: Int, newCount: Int)

    @Query("SELECT remainingCount FROM products WHERE id = :productId")
    suspend fun getProductRemainingCount(productId: Int): Int
} 