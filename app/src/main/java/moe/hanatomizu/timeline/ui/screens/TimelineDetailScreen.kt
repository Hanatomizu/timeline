package moe.hanatomizu.timeline.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity
import moe.hanatomizu.timeline.util.ColorPickerDialog
import moe.hanatomizu.timeline.util.ImageFileHelper
import moe.hanatomizu.timeline.viewmodel.TimelineDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 时间线详情界面 —— 卡片列表 + 编辑弹窗布局。
 *
 * - 列表：按日期排列的卡片（默认升序，右上角可切换升降序），每张卡片显示日期、内容、缩略图、标签色带。
 * - 点击卡片弹出编辑弹窗；长按卡片直接删除（需确认）。
 * - FAB 按钮创建新时间点。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineDetailScreen(
    timelineId: Long,
    onBack: () -> Unit,
    viewModel: TimelineDetailViewModel
) {
    val timeline by viewModel.timeline.collectAsState()
    val events by viewModel.events.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()

    var showEventDialog by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<Long?>(null) }

    // 初始化 ViewModel
    LaunchedEffect(timelineId) {
        viewModel.initialize(timelineId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = timeline?.title ?: "时间线详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSortOrder() }) {
                        Icon(
                            imageVector = if (isAscending) Icons.Default.ArrowUpward
                                         else Icons.Default.ArrowDownward,
                            contentDescription = if (isAscending) "升序排列" else "降序排列"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.startNewEvent()
                showEventDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "创建时间点")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            // ── 空状态 ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无时间点\n点击右下角 + 按钮创建",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // ── 卡片列表 ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = {
                            viewModel.selectEvent(event)
                            showEventDialog = true
                        },
                        onLongClick = {
                            deleteTargetId = event.id
                        }
                    )
                }
            }
        }
    }

    // ── 编辑/新建弹窗 ──
    if (showEventDialog) {
        EventEditDialog(
            viewModel = viewModel,
            onDismiss = { showEventDialog = false },
            onDeleteSuccess = {
                showEventDialog = false
            }
        )
    }

    // ── 长按删除确认 ──
    deleteTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除时间点") },
            text = { Text("确定要删除这个时间点吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEventById(id)
                    deleteTargetId = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) {
                    Text("取消")
                }
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  事件卡片
// ════════════════════════════════════════════════════════════════

/**
 * 单条时间点卡片。
 *
 * 左侧有颜色条带，展示日期、内容文本（最多 3 行）、图片缩略图（可选）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventCard(
    event: TimelineEventEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            // ── 左侧颜色条 ──
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(Color(event.labelColor))
            )

            // ── 内容区 ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // 日期
                Text(
                    text = dateFormat.format(Date(event.eventDate)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 内容（最多3行）
                Text(
                    text = event.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 图片缩略图
                if (event.imagePath != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(event.imagePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = "事件图片",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  编辑 / 新建弹窗
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditDialog(
    viewModel: TimelineDetailViewModel,
    onDismiss: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val context = LocalContext.current

    val isNewMode by viewModel.isNewEventMode.collectAsState()
    val formContent by viewModel.formContent.collectAsState()
    val formImagePath by viewModel.formImagePath.collectAsState()
    val formLabelColor by viewModel.formLabelColor.collectAsState()
    val formEventDate by viewModel.formEventDate.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 图片选取器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = ImageFileHelper.copyImageToInternal(context, it)
            if (path != null) {
                viewModel.updateFormImagePath(path)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // ── 标题 ──
                Text(
                    text = if (isNewMode) "新建时间点" else "编辑时间点",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── 可滚动表单区域 ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 内容
                    OutlinedTextField(
                        value = formContent,
                        onValueChange = { viewModel.updateFormContent(it) },
                        label = { Text("内容（必填）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 6
                    )

                    // 错误提示
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── 图片 ──
                    if (formImagePath != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(formImagePath!!))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "事件图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            // 删除图片按钮
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { viewModel.updateFormImagePath(null) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除图片",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (formImagePath == null) "选择图片" else "更换图片")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── 标签颜色 ──
                    Text(
                        text = "标签颜色",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(formLabelColor))
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { showColorPicker = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── 日期 ──
                    Text(
                        text = "日期",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = dateFormat.format(Date(formEventDate)))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 底部按钮 ──
                Divider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.saveEvent()
                            // 保存成功后关闭弹窗（校验失败时 errorMessage 同步设置，弹窗保持打开）
                            if (viewModel.errorMessage.value == null) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isNewMode) "保存" else "更新")
                    }

                    if (!isNewMode) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除")
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            }
        }
    }

    // ── 颜色选择器（子对话框） ──
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = formLabelColor,
            onColorSelected = { color ->
                viewModel.updateFormLabelColor(color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // ── 日期选择器（子对话框） ──
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formEventDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateFormEventDate(millis)
                    }
                    showDatePicker = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── 弹窗内的删除二次确认 ──
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除时间点") },
            text = { Text("确定要删除这个时间点吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteEvent()
                    onDeleteSuccess()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
