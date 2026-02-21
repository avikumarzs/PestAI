package com.avikshit.PestAI.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScan(scanEntity: ScanEntity): Long

    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): List<ScanEntity>

    @Query("SELECT COUNT(*) FROM scans")
    fun getScanCount(): Int
}
