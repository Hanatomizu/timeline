package moe.hanatomizu.timeline.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import moe.hanatomizu.timeline.data.AppDatabase
import moe.hanatomizu.timeline.data.TimelineRepository
import moe.hanatomizu.timeline.data.entity.TimelineEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 时间线列表界面 ViewModel。
 *
 * 管理时间线列表的增删，以及创建对话框的状态。
 */
class TimelineListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TimelineRepository

    /** 所有时间线数据流 */
    val allTimelines: Flow<List<TimelineEntity>>

    // ── 创建对话框状态 ──
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _newTitle = MutableStateFlow("")
    val newTitle: StateFlow<String> = _newTitle.asStateFlow()

    private val _newCoverImagePath = MutableStateFlow<String?>(null)
    val newCoverImagePath: StateFlow<String?> = _newCoverImagePath.asStateFlow()

    // ── 删除确认状态 ──
    private val _timelineToDelete = MutableStateFlow<Long?>(null)
    val timelineToDelete: StateFlow<Long?> = _timelineToDelete.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TimelineRepository(db.timelineDao(), db.timelineEventDao())
        allTimelines = repository.allTimelines
    }

    // ── 创建对话框 ──

    fun showCreateDialog() {
        _newTitle.value = ""
        _newCoverImagePath.value = null
        _showCreateDialog.value = true
    }

    fun dismissCreateDialog() {
        _showCreateDialog.value = false
    }

    fun updateNewTitle(title: String) {
        _newTitle.value = title
    }

    fun updateNewCoverImagePath(path: String?) {
        _newCoverImagePath.value = path
    }

    /** 保存新建时间线到数据库 */
    fun createTimeline() {
        val title = _newTitle.value.trim()
        if (title.isEmpty()) return // UI 层已校验，这里做安全防护

        viewModelScope.launch {
            repository.createTimeline(title, _newCoverImagePath.value)
            _showCreateDialog.value = false
        }
    }

    // ── 删除 ──

    fun requestDeleteTimeline(timelineId: Long) {
        _timelineToDelete.value = timelineId
    }

    fun cancelDelete() {
        _timelineToDelete.value = null
    }

    /** 确认删除时间线及其所有时间点 */
    fun confirmDelete() {
        val id = _timelineToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteTimeline(id)
            _timelineToDelete.value = null
        }
    }
}
