package moe.hanatomizu.timeline.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import moe.hanatomizu.timeline.data.AppDatabase
import moe.hanatomizu.timeline.data.TimelineRepository
import moe.hanatomizu.timeline.data.entity.TimelineEntity
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity
import moe.hanatomizu.timeline.util.ImageFileHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 时间线详情界面 ViewModel。
 *
 * 管理某个时间线的所有时间点，以及右侧表单的编辑/新建状态。
 */
class TimelineDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TimelineRepository

    private var timelineId: Long = -1

    // ── 数据 ──
    private val _timeline = MutableStateFlow<TimelineEntity?>(null)
    val timeline: StateFlow<TimelineEntity?> = _timeline.asStateFlow()

    private val _events = MutableStateFlow<List<TimelineEventEntity>>(emptyList())
    val events: StateFlow<List<TimelineEventEntity>> = _events.asStateFlow()

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

    private val _formImagePath = MutableStateFlow<String?>(null)
    val formImagePath: StateFlow<String?> = _formImagePath.asStateFlow()

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
        repository = TimelineRepository(db.timelineDao(), db.timelineEventDao())
    }

    /**
     * 初始化 —— 传入时间线 ID，加载数据。
     */
    fun initialize(timelineId: Long) {
        if (this.timelineId == timelineId) return // 避免重复初始化
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
                // 如果当前选中的事件已被删除，重置为新建模式
                val selected = _selectedEvent.value
                if (selected != null && sortedList.none { it.id == selected.id }) {
                    resetToNewMode()
                }
            }
        }
    }

    // ── 事件选择 ──

    /** 选中某个时间点，表单切换到编辑模式 */
    fun selectEvent(event: TimelineEventEntity) {
        editingEventId = event.id
        _selectedEvent.value = event
        _formContent.value = event.content
        _formImagePath.value = event.imagePath
        _formLabelColor.value = event.labelColor
        _formEventDate.value = event.eventDate
        _isNewEventMode.value = false
        _errorMessage.value = null
    }

    /** 切换到新建时间点模式，清空表单 */
    fun startNewEvent() {
        resetToNewMode()
    }

    private fun resetToNewMode() {
        editingEventId = null
        _selectedEvent.value = null
        _formContent.value = ""
        _formImagePath.value = null
        _formLabelColor.value = 0xFF9E9E9E.toInt()
        _formEventDate.value = normalizeToMinute(System.currentTimeMillis())
        _isNewEventMode.value = true
        _errorMessage.value = null
    }

    // ── 表单更新 ──

    fun updateFormContent(content: String) { _formContent.value = content }
    fun updateFormImagePath(path: String?) { _formImagePath.value = path }
    fun updateFormLabelColor(color: Int) { _formLabelColor.value = color }
    /**
     * 更新日期部分（保留当前表单中的时:分）。
     * DatePicker 返回的是当天 00:00 UTC 时间戳，需合并当前时:分。
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

    /** 保存（新建或更新）时间点 */
    fun saveEvent() {
        val content = _formContent.value.trim()
        if (content.isEmpty()) {
            _errorMessage.value = "内容不能为空"
            return
        }

        viewModelScope.launch {
            if (_isNewEventMode.value) {
                // 新建
                val newId = repository.createEvent(
                    timelineId = timelineId,
                    content = content,
                    imagePath = _formImagePath.value,
                    labelColor = _formLabelColor.value,
                    eventDate = _formEventDate.value
                )
                // 新建成功后选中它
                repository.getEventById(newId)?.let { selectEvent(it) }
            } else {
                // 更新
                val id = editingEventId ?: return@launch
                val existing = repository.getEventById(id) ?: return@launch

                // 若图片被更换或移除，清理旧图片文件
                val oldPath = existing.imagePath
                val newPath = _formImagePath.value
                if (oldPath != null && oldPath != newPath) {
                    ImageFileHelper.deleteImage(getApplication(), oldPath)
                }

                val updated = existing.copy(
                    content = content,
                    imagePath = newPath,
                    labelColor = _formLabelColor.value,
                    eventDate = _formEventDate.value
                )
                repository.updateEvent(updated)
                _selectedEvent.value = updated
            }
        }
    }

    /** 删除当前选中的时间点（同时删除关联的图片文件） */
    fun deleteEvent() {
        val event = _selectedEvent.value ?: return
        viewModelScope.launch {
            // 先删除关联的图片文件
            event.imagePath?.let { path ->
                ImageFileHelper.deleteImage(getApplication(), path)
            }
            repository.deleteEvent(event)
            resetToNewMode()
        }
    }

    /** 根据 ID 直接删除时间点（用于卡片列表长按删除） */
    fun deleteEventById(eventId: Long) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId) ?: return@launch
            event.imagePath?.let { path ->
                ImageFileHelper.deleteImage(getApplication(), path)
            }
            repository.deleteEvent(event)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ── 排序 ──

    /** 切换时间点列表排序顺序（升序 ↔ 降序） */
    fun toggleSortOrder() {
        _isAscending.value = !_isAscending.value
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
