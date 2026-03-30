package com.example.frontend.core.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaDownloadManager(private val context: Context) {

    suspend fun downloadAndSaveMedia(url: String?, isVideo: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val extension = if (isVideo) ".mp4" else ".jpg"
            val tempFile = File.createTempFile("temp_media_", extension, context.cacheDir)

            URL(url).openStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!isVideo) {
                updateExifDateToCurrent(tempFile)
            }

            val savedUri = saveToGallery(tempFile, isVideo)

            tempFile.delete()

            if (savedUri != null) {
                Result.success("Đã tải xuống thư mục SocialConnect")
            } else {
                Result.failure(Exception("Không thể lưu vào thư viện"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun updateExifDateToCurrent(file: File) {
        try {
            val exif = ExifInterface(file.absolutePath)

            val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val currentDateStr = sdf.format(Date())

            exif.setAttribute(ExifInterface.TAG_DATETIME, currentDateStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, currentDateStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, currentDateStr)

            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)

            exif.saveAttributes()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToGallery(file: File, isVideo: Boolean): android.net.Uri? {
        val resolver = context.contentResolver
        val currentTimeMillis = System.currentTimeMillis()
        val currentTimeSeconds = currentTimeMillis / 1000

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val directoryName = "SocialConnect"
        val directoryPath = Environment.DIRECTORY_DCIM + "/" + directoryName

        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val fileName = "SocialConnect_${currentTimeMillis}.${if (isVideo) "mp4" else "jpg"}"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DATE_ADDED, currentTimeSeconds)
            put(MediaStore.MediaColumns.DATE_MODIFIED, currentTimeSeconds)

            if (!isVideo) {
                put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis)
            } else {
                put(MediaStore.Video.Media.DATE_TAKEN, currentTimeMillis)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, directoryPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val socialConnectDir = File(publicDir, directoryName)
                if (!socialConnectDir.exists()) {
                    socialConnectDir.mkdirs()
                }
                val actualFile = File(socialConnectDir, fileName)
                put(MediaStore.MediaColumns.DATA, actualFile.absolutePath)
            }
        }

        val uri = resolver.insert(collection, values)

        uri?.let { insertedUri ->
            resolver.openOutputStream(insertedUri)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                values.put(MediaStore.MediaColumns.DATE_ADDED, currentTimeSeconds)
                values.put(MediaStore.MediaColumns.DATE_MODIFIED, currentTimeSeconds)
                resolver.update(insertedUri, values, null, null)
            }
        }

        return uri
    }
}