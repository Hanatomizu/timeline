package moe.hanatomizu.timeline.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * 图片文件工具 —— 将用户从相册选取的图片复制到应用内部私有目录。
 *
 * 使用内部存储（context.filesDir）确保即使应用没有存储权限也能读写，
 * 且复制后的路径可持久化到数据库，不会因权限回收而丢失。
 */
object ImageFileHelper {

    private const val IMAGE_SUB_DIR = "images"

    /**
     * 将 URI 指向的图片复制到内部存储，返回绝对路径。
     * 若复制失败（输入流异常、磁盘满等）返回 null。
     */
    fun copyImageToInternal(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "img_${System.currentTimeMillis()}_${hashCode()}.jpg"
            val dir = File(context.filesDir, IMAGE_SUB_DIR)
            if (!dir.exists()) dir.mkdirs()

            val outputFile = File(dir, fileName)
            FileOutputStream(outputFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 删除内部存储中的图片文件。
     */
    fun deleteImage(context: Context, path: String) {
        try {
            val file = File(path)
            // 仅删除位于 images 子目录下的文件，避免误删
            if (file.exists() && file.parentFile?.name == IMAGE_SUB_DIR) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
