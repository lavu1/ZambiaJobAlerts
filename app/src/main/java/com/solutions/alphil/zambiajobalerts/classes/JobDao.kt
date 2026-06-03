package com.solutions.alphil.zambiajobalerts.classes

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY date DESC")
    fun getAllJobs(): LiveData<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertJobs(jobs: List<JobEntity>)

    @Query("DELETE FROM jobs")
    fun deleteAllJobs()

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    fun getJobById(jobId: Int): JobEntity?

    @Query("SELECT * FROM jobs WHERE title LIKE '%' || :query || '%' OR company LIKE '%' || :query || '%'")
    fun searchJobs(query: String): LiveData<List<JobEntity>>
}
