package moe.hanatomizu.timeline.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.hanatomizu.timeline.util.ImageSaveHelper
import java.io.File

/**
 * 全屏图片预览界面。
 *
 * 支持：
 * - 双指捏合缩放（1× ~ 5×）
 * - 拖拽移动（缩放后）
 * - 单击 / 关闭按钮关闭
 * - 长按保存到相册
 */
@Composable
fun FullScreenImageView(
    imagePath: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        // ── 缩放 + 拖拽手势（在图片层） ──
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(imagePath))
                .crossfade(true)
                .build(),
            contentDescription = "预览",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        // 缩放时以手势中心为锚点调整偏移
                        if (zoom != 1f) {
                            val centroidOffset = Offset(
                                centroid.x - size.width / 2,
                                centroid.y - size.height / 2
                            )
                            offset = Offset(
                                x = centroidOffset.x - (centroidOffset.x - offset.x) * (newScale / scale),
                                y = centroidOffset.y - (centroidOffset.y - offset.y) * (newScale / scale)
                            )
                        } else {
                            offset = Offset(
                                x = (offset.x + pan.x)
                                    .coerceIn(-(newScale - 1) * size.width / 2, (newScale - 1) * size.width / 2),
                                y = (offset.y + pan.y)
                                    .coerceIn(-(newScale - 1) * size.height / 2, (newScale - 1) * size.height / 2)
                            )
                        }
                        scale = newScale
                    }
                },
            contentScale = ContentScale.Fit,
            onLoading = { isLoading = true },
            onSuccess = { isLoading = false; hasError = false },
            onError = { isLoading = false; hasError = true }
        )

        // ── 单击 / 长按手势层（透明覆盖层） ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClose() },
                        onLongPress = {
                            if (hasError) {
                                Toast.makeText(
                                    context, "无法保存，图片文件丢失",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@detectTapGestures
                            }
                            val saved = ImageSaveHelper.saveImageToGallery(context, imagePath)
                            if (saved) {
                                Toast.makeText(
                                    context, "图片已保存到相册",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context, "保存失败，请重试",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
        )

        // ── 加载中 ──
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        // ── 加载失败 ──
        if (hasError) {
            Text(
                text = "图片加载失败\n文件可能已被移动或删除",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── 关闭按钮（右上角） ──
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }

        // ── 底部操作提示 ──
        Text(
            text = "单击关闭 · 长按保存",
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
