package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BudgetLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {

    @Query("SELECT * FROM budget_limits ORDER BY category ASC")
    fun getAllLimits(): Flow<List<BudgetLimitEntity>>

    @Query("SELECT * FROM budget_limits ORDER BY category ASC")
    suspend fun getAllLimitsSync(): List<BudgetLimitEntity>

    @Query("SELECT * FROM budget_limits WHERE category = :category LIMIT 1")
    suspend fun getLimitForCategory(category: String): BudgetLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimit(limit: BudgetLimitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimits(limits: List<BudgetLimitEntity>)

    @Update
    suspend fun updateLimit(limit: BudgetLimitEntity)

    @Query("DELETE FROM budget_limits WHERE category = :category")
    suspend fun deleteLimit(category: String)

    @Query("SELECT COUNT(*) FROM budget_limits")
    suspend fun getCount(): Int
}
