package moe.hanatomizu.timeline.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity
import moe.hanatomizu.timeline.ui.theme.TimelineTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间线导出工具 —— 将时间点列表渲染为长图，保存到缓存目录并分享。
 */
object ExportHelper {

    private const val EXPORT_WIDTH_DP = 420
    const val MAX_EXPORT_EVENTS = 100

    /**
     * 生成导出图片并保存到缓存目录。
     *
     * @return 生成的图片文件，失败返回 null
     */
    suspend fun exportToFile(
        context: Context,
        timelineTitle: String,
        coverImagePath: String?,
        events: List<TimelineEventEntity>,
        isDarkTheme: Boolean
    ): File? {
        // 先同步加载所有图片（避免 AsyncImage 异步时序导致导出图片中图片空白）
        val loadedBitmaps = withContext(Dispatchers.IO) {
            preloadImages(context, coverImagePath, events)
        }

        val bitmap = withContext(Dispatchers.Main) {
            renderToBitmap(
                context, timelineTitle, coverImagePath, events, loadedBitmaps, isDarkTheme
            )
        } ?: return null

        return withContext(Dispatchers.IO) {
            saveBitmapToCache(context, bitmap)
        }
    }

    /**
     * 预加载所有图片（封面 + 事件图片）为 Bitmap，供导出组件同步绘制。
     * 返回 Map<原始路径, Bitmap?>，加载失败时 value 为 null。
     */
    private suspend fun preloadImages(
        context: Context,
        coverImagePath: String?,
        events: List<TimelineEventEntity>
    ): Map<String, Bitmap?> {
        val paths = mutableListOf<String>()
        coverImagePath?.let { paths.add(it) }
        events.forEach { it.imagePath?.let { paths.add(it) } }

        val loader = Coil.imageLoader(context)

        val result = mutableMapOf<String, Bitmap?>()
        for (path in paths) {
            if (path in result) continue
            result[path] = try {
                val request = ImageRequest.Builder(context)
                    .data(File(path))
                    .size(320)
                    .build()
                val response = loader.execute(request)
                if (response is SuccessResult) {
                    val original = (response.drawable as? BitmapDrawable)?.bitmap
                    // hardware bitmap 无法被 Canvas.drawBitmap 绘制，强制转为 software
                    original?.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("ExportHelper", "preload image failed: $path", e)
                null
            }
        }
        return result
    }

    /**
     * 获取用于分享的 content:// URI。
     */
    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /** 检查事件数量是否超过限制 */
    fun isOverLimit(count: Int) = count > MAX_EXPORT_EVENTS

    // ── 离屏渲染 ──

    /**
     * 将导出内容渲染为 Bitmap。
     *
     * ComposeView 需要附着到 Activity 的窗口才能正确创建 Composition，
     * 因此临时将 ComposeView 添加到 decor 布局中（由于导出时显示进度对话框，
     * 用户不会察觉到这个短暂添加的 View）。
     *
     * 必须在主线程调用。
     */
    private fun renderToBitmap(
        context: Context,
        timelineTitle: String,
        coverImagePath: String?,
        events: List<TimelineEventEntity>,
        loadedBitmaps: Map<String, Bitmap?>,
        isDarkTheme: Boolean
    ): Bitmap? {
        val activity = context as? Activity ?: return null
        val decorView = activity.window?.decorView as? ViewGroup ?: return null

        val composeView = androidx.compose.ui.platform.ComposeView(context).apply {
            id = View.generateViewId()
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )
        }

        val result: Bitmap? = try {
            // 附着到窗口（ComposeView 需要窗口才能创建 Composition）
            val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            decorView.addView(composeView, params)

            composeView.setContent {
                TimelineTheme(darkTheme = isDarkTheme) {
                    ExportContent(
                        timelineTitle = timelineTitle,
                        coverImagePath = coverImagePath,
                        events = events,
                        loadedBitmaps = loadedBitmaps
                    )
                }
            }

            // 强制测量和布局
            val density = context.resources.displayMetrics.density
            val widthPx = (EXPORT_WIDTH_DP * density).toInt()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
            composeView.measure(widthSpec, View.MeasureSpec.UNSPECIFIED)
            val heightPx = composeView.measuredHeight.coerceAtLeast(1)
            composeView.layout(0, 0, widthPx, heightPx)

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("ExportHelper", "renderToBitmap failed", e)
            null
        } finally {
            // 从窗口移除临时 View
            if (composeView.parent != null) {
                (composeView.parent as? ViewGroup)?.removeView(composeView)
            }
        }
        return result
    }

    // ── 文件保存 ──

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File? {
        return try {
            val fileName = "timeline_export_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.png"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  导出内容 Composable
    // ════════════════════════════════════════════════════════════════

    @Composable
    fun ExportContent(
        timelineTitle: String,
        coverImagePath: String?,
        events: List<TimelineEventEntity>,
        loadedBitmaps: Map<String, Bitmap?>
    ) {
        val dateFormat = remember {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        }

        // 注意：不使用 verticalScroll，因为导出时需要测量完整高度
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            // ── 头部：标题 + 导出时间 ──
            Text(
                text = timelineTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "导出时间: ${dateFormat.format(Date(System.currentTimeMillis()))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 封面图片（使用预加载的 Bitmap）
            if (coverImagePath != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val coverBitmap = loadedBitmaps[coverImagePath]
                if (coverBitmap != null) {
                    Image(
                        painter = BitmapPainter(coverBitmap.asImageBitmap()),
                        contentDescription = "封面",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 加载失败时显示灰色占位
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("封面图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // ── 卡片列表 ──
            events.forEachIndexed { index, event ->
                ExportNode(
                    event = event,
                    dateFormat = dateFormat,
                    loadedBitmaps = loadedBitmaps,
                    isLast = index == events.lastIndex
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // ── 底部 ──
            Text(
                text = "由 Timeline App 生成",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun ExportNode(
        event: TimelineEventEntity,
        dateFormat: SimpleDateFormat,
        loadedBitmaps: Map<String, Bitmap?>,
        isLast: Boolean
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // ── 左侧时间轴 ──
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // 连接线（贯穿卡片，最后一个裁剪到底部圆点处）
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                }
                // 圆点（与第一行文字对齐）
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .padding(top = 4.dp)
                        .clip(CircleShape)
                        .background(Color(event.labelColor))
                )
            }

            // ── 内容 ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, bottom = if (isLast) 0.dp else 16.dp)
            ) {
                Text(
                    text = dateFormat.format(Date(event.eventDate)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 图片缩略图（使用预加载的 Bitmap，避免 AsyncImage 时序问题）
                if (event.imagePath != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val bitmap = loadedBitmaps[event.imagePath]
                    if (bitmap != null) {
                        Image(
                            painter = BitmapPainter(bitmap.asImageBitmap()),
                            contentDescription = "图片",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 加载失败时显示灰色占位
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("图片",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
