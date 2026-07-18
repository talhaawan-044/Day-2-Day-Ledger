package com.example.awancoalledger.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.awancoalledger.data.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class BackupData(
    val parties: List<Party>,
    val entries: List<LedgerEntry>,
    val payments: List<Payment>,
    val expenses: List<Expense>,
    val reminderLists: List<ReminderList>,
    val reminders: List<Reminder>,
    val stocks: List<Stock>,
    val stockEntries: List<StockEntry>,
    val notes: List<Note>,
    val folders: List<Folder>,
    val fuelEntries: List<FuelEntry>,
    val maintenanceEntries: List<MaintenanceEntry>,
    val vehicles: List<Vehicle> = emptyList(),
    val settings: Map<String, String?> = emptyMap(),
    val logoBase64: String? = null,
    val signatureBase64: String? = null
)

object DataExchangeUtils {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun serializeBackup(data: BackupData): String {
        return gson.toJson(data)
    }

    fun deserializeBackup(json: String): BackupData? {
        return try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encodes a file to Base64 string for embedding in backup JSON.
     */
    fun fileToBase64(context: Context, fileName: String): String? {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val bytes = file.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodes a Base64 string and saves it as a file in internal storage.
     * Returns the file URI.
     */
    fun base64ToFile(context: Context, base64: String, fileName: String): Uri? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Copies an image from a URI to internal storage to ensure persistent access.
     * Returns the URI of the copied file.
     */
    fun copyImageToInternal(context: Context, uri: Uri, fileName: String): Uri? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, fileName)
            
            if (inputStream != null) {
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    val maxDim = 400
                    val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                    val scaledBitmap = if (ratio < 1) {
                        android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                    } else {
                        bitmap
                    }
                    
                    val outputStream = FileOutputStream(file)
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    
                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle()
                    }
                    bitmap.recycle()
                } else {
                    return null
                }
            } else {
                return null
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun downloadImage(context: Context, urlString: String, fileName: String): Uri? {
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val file = File(context.filesDir, fileName)
            val output = FileOutputStream(file)
            input.use { i ->
                output.use { o ->
                    i.copyTo(o)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    /**
     * Ensures only the latest 5 backup files (JSON) are kept in the backup directory.
     */
    fun cleanupBackups(context: Context) {
        try {
            val directory = File(context.filesDir, "backups")
            if (!directory.exists()) return
            
            val files = directory.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            } ?: return
            
            if (files.size > 5) {
                // Sort by last modified (oldest first)
                files.sortBy { it.lastModified() }
                
                val toDeleteCount = files.size - 5
                for (i in 0 until toDeleteCount) {
                    if (files[i].delete()) {
                        android.util.Log.d("DataExchangeUtils", "Deleted old backup: ${files[i].name}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
