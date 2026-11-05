package com.example.saldoandalucia.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

 data class SavedCardRecord(
    val uid: String,
    val name: String,
    val balance: Double,
    val lastRead: Long
)

data class ReadingRecord(
    val id: Long,
    val uid: String,
    val balance: Double,
    val timestamp: Long
)

class LocalStore(context: Context) : SQLiteOpenHelper(context, "consaldo.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE cards (
            uid TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            balance REAL NOT NULL,
            last_read INTEGER NOT NULL
        )""".trimIndent())
        db.execSQL("""CREATE TABLE readings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            uid TEXT NOT NULL,
            balance REAL NOT NULL,
            timestamp INTEGER NOT NULL
        )""".trimIndent())
        db.execSQL("CREATE INDEX idx_readings_uid_time ON readings(uid, timestamp DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { }

    fun card(uid: String): SavedCardRecord? = readableDatabase.rawQuery(
        "SELECT uid,name,balance,last_read FROM cards WHERE uid=?", arrayOf(uid)
    ).use { c -> if (c.moveToFirst()) SavedCardRecord(c.getString(0), c.getString(1), c.getDouble(2), c.getLong(3)) else null }

    fun cards(): List<SavedCardRecord> = readableDatabase.rawQuery(
        "SELECT uid,name,balance,last_read FROM cards ORDER BY last_read DESC", null
    ).use { c -> buildList { while (c.moveToNext()) add(SavedCardRecord(c.getString(0), c.getString(1), c.getDouble(2), c.getLong(3))) } }

    fun saveNewCard(uid: String, name: String, balance: Double, timestamp: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("uid", uid); put("name", name); put("balance", balance)
                put("last_read", timestamp)
            }
            db.insertOrThrow("cards", null, values)
            insertReading(db, uid, balance, timestamp)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun recordReading(uid: String, balance: Double, timestamp: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("UPDATE cards SET balance=?, last_read=? WHERE uid=?", arrayOf(balance, timestamp, uid))
            insertReading(db, uid, balance, timestamp)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun insertReading(db: SQLiteDatabase, uid: String, balance: Double, timestamp: Long) {
        val v = ContentValues().apply { put("uid", uid); put("balance", balance); put("timestamp", timestamp) }
        db.insertOrThrow("readings", null, v)
    }

    fun renameCard(uid: String, name: String) { writableDatabase.execSQL("UPDATE cards SET name=? WHERE uid=?", arrayOf(name, uid)) }
    fun deleteCard(uid: String) { writableDatabase.beginTransaction(); try { writableDatabase.delete("readings", "uid=?", arrayOf(uid)); writableDatabase.delete("cards", "uid=?", arrayOf(uid)); writableDatabase.setTransactionSuccessful() } finally { writableDatabase.endTransaction() } }

    fun readings(uid: String? = null, limit: Int = 250): List<ReadingRecord> {
        val sql = if (uid == null) "SELECT id,uid,balance,timestamp FROM readings ORDER BY timestamp DESC LIMIT ?" else "SELECT id,uid,balance,timestamp FROM readings WHERE uid=? ORDER BY timestamp DESC LIMIT ?"
        val args = if (uid == null) arrayOf(limit.toString()) else arrayOf(uid, limit.toString())
        return readableDatabase.rawQuery(sql, args).use { c -> buildList { while (c.moveToNext()) add(ReadingRecord(c.getLong(0), c.getString(1), c.getDouble(2), c.getLong(3))) } }
    }

    fun monthlyConsumption(uid: String, fromTimestamp: Long): Double {
        val rows = readings(uid, 1000).filter { it.timestamp >= fromTimestamp }.sortedBy { it.timestamp }
        var consumed = 0.0
        for (i in 1 until rows.size) {
            val delta = rows[i - 1].balance - rows[i].balance
            if (delta > 0) consumed += delta
        }
        return consumed
    }
}
