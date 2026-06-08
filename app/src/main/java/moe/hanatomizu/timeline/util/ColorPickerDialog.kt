package moe.hanatomizu.timeline.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 预设颜色列表（Material 风格） */
val PRESET_COLORS: List<Int> = listOf(
    0xFFEF5350.toInt(), // 红
    0xFFE91E63.toInt(), // 粉
    0xFFAB47BC.toInt(), // 紫
    0xFF5C6BC0.toInt(), // 靛蓝
    0xFF42A5F5.toInt(), // 蓝
    0xFF26A69A.toInt(), // 青
    0xFF66BB6A.toInt(), // 绿
    0xFF9CCC65.toInt(), // 浅绿
    0xFFFFCA28.toInt(), // 黄/琥珀
    0xFFFF7043.toInt(), // 深橙
    0xFF8D6E63.toInt(), // 棕
    0xFF78909C.toInt(), // 蓝灰
    0xFF9E9E9E.toInt(), // 灰
    0xFF000000.toInt(), // 黑
)

/**
 * 颜色选择器对话框 —— 从预设色块中选取标签颜色。
 *
 * @param currentColor 当前已选颜色
 * @param onColorSelected 用户选中颜色后的回调
 * @param onDismiss 关闭对话框
 */
@Composable
fun ColorPickerDialog(
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择标签颜色", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 当前颜色预览
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(currentColor))
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "当前颜色",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 预设颜色网格（4 列）
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(PRESET_COLORS) { color ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .then(
                                    if (color == currentColor)
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                .clickable { onColorSelected(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
