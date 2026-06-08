package moe.hanatomizu.timeline.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 事件图片实体 —— 一个时间点可关联多张图片。
 *
 * 通过外键 [eventId] 关联到时间点，级联删除。
 */
@Entity(
    tableName = "event_images",
    foreignKeys = [
        ForeignKey(
            entity = TimelineEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId")]
)
data class EventImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 所属时间点 ID */
    val eventId: Long,
    /** 图片绝对路径 */
    val imagePath: String,
    /** 排序顺序（从 0 递增） */
    val sortOrder: Int = 0
)
