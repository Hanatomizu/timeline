package moe.hanatomizu.timeline.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图片保存工具 —— 通过 MediaStore API 将图片保存到系统相册。
 *
 * 不需要 WRITE_EXTERNAL_STORAGE 权限（Android 10+ MediaStore 兼容）。
 */
object ImageSaveHelper {

    private const val ALBUM_DIR = "TimelineApp"

    /**
     * 将指定路径的图片保存到系统相册的 TimelineApp 子目录。
     * @return true 保存成功，false 失败
     */
    fun saveImageToGallery(context: Context, sourcePath: String): Boolean {
        return try {
            val file = File(sourcePath)
            if (!file.exists()) return false

            val fileName = "timeline_image_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$ALBUM_DIR"
                )
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return false

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: run {
                // 写入失败，删除已创建的占位
                context.contentResolver.delete(uri, null, null)
                return false
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
