package moe.hanatomizu.timeline.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 时间线实体 —— 一条时间线（例如"我的 2024"、"旅行日记"等）
 */
@Entity(tableName = "timelines")
data class TimelineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    /** 封面图片在内部存储中的绝对路径；null 表示无封面 */
    val coverImagePath: String? = null
)
