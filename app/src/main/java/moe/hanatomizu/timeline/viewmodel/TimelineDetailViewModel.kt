package moe.hanatomizu.timeline.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import moe.hanatomizu.timeline.data.AppDatabase
import moe.hanatomizu.timeline.data.TimelineRepository
import moe.hanatomizu.timeline.data.entity.TimelineEntity
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity
import moe.hanatomizu.timeline.util.ExportHelper
import moe.hanatomizu.timeline.util.ImageFileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

/**
 * 时间线详情界面 ViewModel。
 *
 * 管理某个时间线的所有时间点，以及编辑/新建表单状态。
 */
class TimelineDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TimelineRepository

    private var timelineId: Long = -1

    // ── 数据 ──
    private val _timeline = MutableStateFlow<TimelineEntity?>(null)
    val timeline: StateFlow<TimelineEntity?> = _timeline.asStateFlow()

    private val _events = MutableStateFlow<List<TimelineEventEntity>>(emptyList())
    val events: StateFlow<List<TimelineEventEntity>> = _events.asStateFlow()

    /** 事件 ID → 图片路径列表缓存，供卡片和预览使用 */
    private val _eventImageMap = MutableStateFlow<Map<Long, List<String>>>(emptyMap())
    val eventImageMap: StateFlow<Map<Long, List<String>>> = _eventImageMap.asStateFlow()

    // ── 排序状态 ──
    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    // ── 选中状态 ──
    private val _selectedEvent = MutableStateFlow<TimelineEventEntity?>(null)
    val selectedEvent: StateFlow<TimelineEventEntity?> = _selectedEvent.asStateFlow()

    private val _isNewEventMode = MutableStateFlow(true)
    val isNewEventMode: StateFlow<Boolean> = _isNewEventMode.asStateFlow()

    // ── 表单字段 ──

    private val _formContent = MutableStateFlow("")
    val formContent: StateFlow<String> = _formContent.asStateFlow()

    /** 多张图片路径列表（最多 9 张） */
    private val _formImagePaths = MutableStateFlow<List<String>>(emptyList())
    val formImagePaths: StateFlow<List<String>> = _formImagePaths.asStateFlow()

    private val _formLabelColor = MutableStateFlow(0xFF9E9E9E.toInt())
    val formLabelColor: StateFlow<Int> = _formLabelColor.asStateFlow()

    private val _formEventDate = MutableStateFlow(normalizeToMinute(System.currentTimeMillis()))
    val formEventDate: StateFlow<Long> = _formEventDate.asStateFlow()

    /** 校验错误信息 */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 当前正在编辑的时间点 ID（新建模式下为 null） */
    private var editingEventId: Long? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TimelineRepository(db.timelineDao(), db.timelineEventDao(), db.eventImageDao())
    }

    /**
     * 初始化 —— 传入时间线 ID，加载数据。
     */
    fun initialize(timelineId: Long) {
        if (this.timelineId == timelineId) return
        this.timelineId = timelineId

        viewModelScope.launch {
            _timeline.value = repository.getTimelineById(timelineId)
        }
        viewModelScope.launch {
            combine(
                repository.getEventsByTimelineId(timelineId),
                _isAscending
            ) { list, ascending ->
                if (ascending) list.sortedBy { it.eventDate }
                else list.sortedByDescending { it.eventDate }
            }.collect { sortedList ->
                _events.value = sortedList
                val selected = _selectedEvent.value
                if (selected != null && sortedList.none { it.id == selected.id }) {
                    resetToNewMode()
                }
            }
        }
        // 并行加载每个事件对应的图片列表
        viewModelScope.launch {
            _events.collect { list ->
                val map = mutableMapOf<Long, List<String>>()
                for (event in list) {
                    map[event.id] = repository.getImagesByEventId(event.id)
                        .map { it.imagePath }
                }
                _eventImageMap.value = map
            }
        }
    }

    // ── 事件选择 ──

    /** 选中某个时间点，表单切换到编辑模式，并加载其图片列表 */
    fun selectEvent(event: TimelineEventEntity) {
        editingEventId = event.id
        _selectedEvent.value = event
        _formContent.value = event.content
        _formLabelColor.value = event.labelColor
        _formEventDate.value = event.eventDate
        _isNewEventMode.value = false
        _errorMessage.value = null
        // 异步加载图片
        viewModelScope.launch {
            val images = repository.getImagesByEventId(event.id)
            _formImagePaths.value = images.map { it.imagePath }
        }
    }

    /** 切换到新建时间点模式，清空表单 */
    fun startNewEvent() { resetToNewMode() }

    private fun resetToNewMode() {
        editingEventId = null
        _selectedEvent.value = null
        _formContent.value = ""
        _formImagePaths.value = emptyList()
        _formLabelColor.value = 0xFF9E9E9E.toInt()
        _formEventDate.value = normalizeToMinute(System.currentTimeMillis())
        _isNewEventMode.value = true
        _errorMessage.value = null
    }

    // ── 表单更新 ──

    fun updateFormContent(content: String) { _formContent.value = content }

    /** 添加一张图片到表单列表 */
    fun addFormImagePath(path: String) {
        val list = _formImagePaths.value
        if (list.size >= 9) return
        _formImagePaths.value = list + path
    }

    /** 从表单列表中删除一张图片（同时删除本地文件） */
    fun removeFormImagePath(index: Int) {
        val list = _formImagePaths.value.toMutableList()
        if (index !in list.indices) return
        val removed = list.removeAt(index)
        ImageFileHelper.deleteImage(getApplication(), removed)
        _formImagePaths.value = list
    }

    fun updateFormLabelColor(color: Int) { _formLabelColor.value = color }

    /**
     * 更新日期部分（保留当前表单中的时:分）。
     */
    fun updateFormEventDate(dateMillis: Long) {
        val timeCal = Calendar.getInstance().apply { timeInMillis = _formEventDate.value }
        val hour = timeCal.get(Calendar.HOUR_OF_DAY)
        val minute = timeCal.get(Calendar.MINUTE)

        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        _formEventDate.value = cal.timeInMillis
    }

    /** 单独更新时间部分（时:分），不影响日期 */
    fun updateFormEventTime(hourOfDay: Int, minute: Int) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = _formEventDate.value
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        _formEventDate.value = cal.timeInMillis
    }

    /** 保存（新建或更新）时间点，同时保存图片列表 */
    fun saveEvent() {
        val content = _formContent.value.trim()
        if (content.isEmpty()) {
            _errorMessage.value = "内容不能为空"
            return
        }

        viewModelScope.launch {
            if (_isNewEventMode.value) {
                val newId = repository.createEvent(
                    timelineId = timelineId,
                    content = content,
                    labelColor = _formLabelColor.value,
                    eventDate = _formEventDate.value
                )
                // 保存图片
                repository.replaceImages(newId, _formImagePaths.value)
                repository.getEventById(newId)?.let { selectEvent(it) }
            } else {
                val id = editingEventId ?: return@launch
                val existing = repository.getEventById(id) ?: return@launch

                // 清理不再使用的旧图片文件
                val oldImages = repository.getImagesByEventId(id)
                val newPaths = _formImagePaths.value
                oldImages.forEach { img ->
                    if (img.imagePath !in newPaths) {
                        ImageFileHelper.deleteImage(getApplication(), img.imagePath)
                    }
                }

                val updated = existing.copy(
                    content = content,
                    labelColor = _formLabelColor.value,
                    eventDate = _formEventDate.value
                )
                repository.updateEvent(updated)
                repository.replaceImages(id, newPaths)
                _selectedEvent.value = updated
            }
        }
    }

    /** 删除当前选中的时间点（同时删除所有关联图片文件） */
    fun deleteEvent() {
        val event = _selectedEvent.value ?: return
        viewModelScope.launch {
            val images = repository.getImagesByEventId(event.id)
            images.forEach { ImageFileHelper.deleteImage(getApplication(), it.imagePath) }
            repository.deleteEvent(event)
            resetToNewMode()
        }
    }

    /** 根据 ID 直接删除时间点（用于卡片列表长按删除） */
    fun deleteEventById(eventId: Long) {
        viewModelScope.launch {
            val images = repository.getImagesByEventId(eventId)
            images.forEach { ImageFileHelper.deleteImage(getApplication(), it.imagePath) }
            val event = repository.getEventById(eventId) ?: return@launch
            repository.deleteEvent(event)
        }
    }

    fun clearError() { _errorMessage.value = null }

    // ── 排序 ──

    fun toggleSortOrder() { _isAscending.value = !_isAscending.value }

    // ── 导出 ──

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportFile = MutableStateFlow<File?>(null)
    val exportFile: StateFlow<File?> = _exportFile.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    fun clearExportState() { _exportFile.value = null; _exportError.value = null }

    fun exportTimeline(activityContext: Context, isDarkTheme: Boolean) {
        if (_isExporting.value) return
        val events = _events.value
        val timeline = _timeline.value ?: return

        if (events.isEmpty()) { _exportError.value = "没有时间点可导出"; return }
        if (ExportHelper.isOverLimit(events.size)) {
            _exportError.value = "时间点过多（超过 ${ExportHelper.MAX_EXPORT_EVENTS} 个），请减少后重试"; return
        }

        _isExporting.value = true
        _exportFile.value = null
        _exportError.value = null

        viewModelScope.launch {
            // 导出时需要把每个事件的所有图片路径带过去
            val eventsWithImages = events.map { event ->
                val images = repository.getImagesByEventId(event.id)
                event to images.map { it.imagePath }
            }
            val file = ExportHelper.exportToFile(
                context = activityContext,
                timelineTitle = timeline.title,
                coverImagePath = timeline.coverImagePath,
                eventsWithImages = eventsWithImages,
                isDarkTheme = isDarkTheme
            )
            if (file != null) _exportFile.value = file
            else _exportError.value = "导出失败，请重试"
            _isExporting.value = false
        }
    }

    /** 将时间戳归一化为精确到分钟（保留时:分，秒和毫秒置零） */
    private fun normalizeToMinute(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
