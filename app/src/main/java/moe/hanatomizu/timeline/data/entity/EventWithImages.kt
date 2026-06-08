package moe.hanatomizu.timeline.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 时间点 + 其关联图片列表（一对多关联查询结果）。
 */
data class EventWithImages(
    @Embedded
    val event: TimelineEventEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "eventId",
        entity = EventImageEntity::class
    )
    val images: List<EventImageEntity> = emptyList()
)
