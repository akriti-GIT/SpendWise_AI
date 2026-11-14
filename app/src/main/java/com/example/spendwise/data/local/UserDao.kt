package com.example.spendwise.data.local

import androidx.room.*
import com.example.spendwise.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email OR LOWER(TRIM(email)) = LOWER(TRIM(:email)) LIMIT 1")
    fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(user: UserEntity): Long

    @Update
    fun update(user: UserEntity): Int

    @Delete
    fun delete(user: UserEntity): Int

    @Query("SELECT COUNT(*) FROM users")
    fun getCount(): Int
}
