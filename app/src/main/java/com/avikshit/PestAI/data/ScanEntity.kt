package com.avikshit.PestAI.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pestName: String,
    val confidence: Float,
    val timestamp: Long,
    val remedySuggested: String
)
