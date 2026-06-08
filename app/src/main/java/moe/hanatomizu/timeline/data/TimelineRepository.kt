package moe.hanatomizu.timeline.data

import moe.hanatomizu.timeline.data.dao.TimelineDao
import moe.hanatomizu.timeline.data.dao.TimelineEventDao
import moe.hanatomizu.timeline.data.entity.TimelineEntity
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * 统一数据仓库 —— 封装 TimelineDao 和 TimelineEventDao 的操作。
 */
class TimelineRepository(
    private val timelineDao: TimelineDao,
    private val eventDao: TimelineEventDao
) {
    // ── 时间线 ──

    /** 所有时间线的 Flow */
    val allTimelines: Flow<List<TimelineEntity>> = timelineDao.getAllTimelines()

    /** 根据 ID 获取单条时间线 */
    suspend fun getTimelineById(id: Long): TimelineEntity? =
        timelineDao.getTimelineById(id)

    /** 创建新时间线，返回新 ID */
    suspend fun createTimeline(title: String, coverImagePath: String?): Long =
        timelineDao.insertTimeline(
            TimelineEntity(title = title, coverImagePath = coverImagePath)
        )

    /** 删除时间线（级联删除所有时间点） */
    suspend fun deleteTimeline(id: Long) = timelineDao.deleteTimelineById(id)

    // ── 时间点 ──

    /** 获取某时间线下所有时间点的 Flow */
    fun getEventsByTimelineId(timelineId: Long): Flow<List<TimelineEventEntity>> =
        eventDao.getEventsByTimelineId(timelineId)

    /** 根据 ID 获取单个时间点 */
    suspend fun getEventById(id: Long): TimelineEventEntity? =
        eventDao.getEventById(id)

    /** 创建新时间点，返回新 ID */
    suspend fun createEvent(
        timelineId: Long,
        content: String,
        imagePath: String?,
        labelColor: Int,
        eventDate: Long
    ): Long = eventDao.insertEvent(
        TimelineEventEntity(
            timelineId = timelineId,
            content = content,
            imagePath = imagePath,
            labelColor = labelColor,
            eventDate = eventDate
        )
    )

    /** 更新已有时间点 */
    suspend fun updateEvent(event: TimelineEventEntity) = eventDao.updateEvent(event)

    /** 删除一个时间点 */
    suspend fun deleteEvent(event: TimelineEventEntity) = eventDao.deleteEvent(event)
}
