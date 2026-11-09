package com.nihad64.rotam.data.db

import android.content.Context
import androidx.room.*
import com.nihad64.rotam.data.model.CategoryCount
import com.nihad64.rotam.data.model.Goal
import com.nihad64.rotam.data.model.Plan
import com.nihad64.rotam.data.model.PlanSeries
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
 @Query("SELECT * FROM plans WHERE isDeleted=0 ORDER BY date DESC") fun observeAll(): Flow<List<Plan>>
 @Query("SELECT * FROM plans") suspend fun all(): List<Plan>
 @Query("SELECT * FROM plans WHERE uid=:uid") suspend fun byUid(uid:String):Plan?
 @Query("SELECT * FROM plans WHERE seriesId=:id") suspend fun bySeries(id:Long):List<Plan>

    @Query("SELECT * FROM plans WHERE isDeleted=0 AND (date = :date OR (date<:date AND endDate>=:date)) ORDER BY COALESCE(startTime,endTime,'99:99')")
    fun observeByDate(date: String): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE isDeleted=0 AND (date BETWEEN :from AND :to OR (date<:from AND endDate>=:from)) ORDER BY date, COALESCE(startTime,endTime,'99:99')")
    fun observeRange(from: String, to: String): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE isDeleted=0 AND (date BETWEEN :from AND :to OR (date<:from AND endDate>=:from)) ORDER BY date, COALESCE(startTime,endTime,'99:99')")
    suspend fun range(from: String, to: String): List<Plan>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun byId(id: Long): Plan?

    @Query("SELECT * FROM plans WHERE date >= :from AND isCompleted = 0 AND isSkipped = 0 AND isDeleted=0")
    suspend fun upcoming(from: String): List<Plan>

    @Query("SELECT * FROM plans WHERE date BETWEEN :from AND :to AND isCompleted = 0 AND isSkipped = 0 AND isDeleted=0")
    suspend fun pendingBetween(from: String, to: String): List<Plan>

    @Query("SELECT category, COUNT(*) AS count FROM plans WHERE isCompleted = 1 AND isDeleted=0 GROUP BY category ORDER BY count DESC")
    fun observeCompletedByCategory(): Flow<List<CategoryCount>>

    @Query("SELECT DISTINCT date FROM plans WHERE isCompleted = 1 AND isDeleted=0 ORDER BY date DESC")
    fun observeCompletedDays(): Flow<List<String>>

    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insert(plan: Plan): Long
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertAll(plans: List<Plan>): List<Long>
    @Query("UPDATE plans SET focusSeconds=:seconds WHERE id=:id") suspend fun setFocusSeconds(id:Long,seconds:Long)
    @Update suspend fun update(plan: Plan)
    @Delete suspend fun delete(plan: Plan)
    @Query("DELETE FROM plans") suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE id=:id") suspend fun byId(id:Long):Goal?
 @Query("SELECT * FROM goals") suspend fun all():List<Goal>
    @Query("SELECT * FROM goals ORDER BY isCompleted, dueDate")
    fun observeAll(): Flow<List<Goal>>
    @Insert suspend fun insert(goal: Goal): Long
    @Update suspend fun update(goal: Goal)
    @Delete suspend fun delete(goal: Goal)
    @Query("DELETE FROM goals") suspend fun clear()
}

@Dao interface SeriesDao {
 @Query("SELECT * FROM series ORDER BY id DESC") fun observeAll():Flow<List<PlanSeries>>
 @Query("SELECT * FROM series") suspend fun all():List<PlanSeries>
 @Query("SELECT * FROM series WHERE id=:id") suspend fun byId(id:Long):PlanSeries?
 @Insert suspend fun insert(s:PlanSeries):Long
 @Update suspend fun update(s:PlanSeries)
 @Query("DELETE FROM series") suspend fun clear()
}
@Database(entities = [Plan::class, Goal::class, PlanSeries::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun seriesDao():SeriesDao
    abstract fun planDao(): PlanDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rotam.db")
                .addMigrations(Migrations.V1_2)
                .build().also { instance = it }
        }
    }
}
