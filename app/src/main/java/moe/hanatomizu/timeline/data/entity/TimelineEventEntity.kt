package moe.hanatomizu.timeline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 时间点实体 —— 时间线上一个具体的事件/记录。
 *
 * 通过外键 [timelineId] 关联到父时间线，级联删除（删除时间线时自动删除所有时间点）。
 * 图片存储在 [EventImageEntity] 子表中，通过 [images] 字段访问。
 */
@Entity(
    tableName = "timeline_events",
    foreignKeys = [
        ForeignKey(
            entity = TimelineEntity::class,
            parentColumns = ["id"],
            childColumns = ["timelineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("timelineId")]
)
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 所属时间线 ID */
    val timelineId: Long,
    /** 事件内容描述（必填） */
    val content: String,
    /** 标签颜色 ARGB 值，如 0xFFEF5350；默认灰色 */
    val labelColor: Int = 0xFF9E9E9E.toInt(),
    /** 事件日期时间 —— 精确到分钟的时间戳（毫秒），用于排序和显示 */
    val eventDate: Long
)
