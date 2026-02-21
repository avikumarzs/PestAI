package com.avikshit.PestAI.data

class ScanRepository(private val scanDao: ScanDao) {
    fun insertScan(scanEntity: ScanEntity): Long = scanDao.insertScan(scanEntity)

    fun getAllScans(): List<ScanEntity> = scanDao.getAllScans()

    fun getScanCount(): Int = scanDao.getScanCount()
}
