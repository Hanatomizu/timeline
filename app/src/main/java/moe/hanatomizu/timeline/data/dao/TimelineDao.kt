package moe.hanatomizu.timeline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import moe.hanatomizu.timeline.data.entity.TimelineEntity
import kotlinx.coroutines.flow.Flow

/**
 * 时间线数据访问对象
 */
@Dao
interface TimelineDao {

    /** 获取所有时间线，按创建倒序（最新的在前） */
    @Query("SELECT * FROM timelines ORDER BY id DESC")
    fun getAllTimelines(): Flow<List<TimelineEntity>>

    /** 按 ID 查找单条时间线 */
    @Query("SELECT * FROM timelines WHERE id = :id")
    suspend fun getTimelineById(id: Long): TimelineEntity?

    /** 插入（或替换）一条时间线，返回生成的主键 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeline(timeline: TimelineEntity): Long

    /** 删除一条时间线（同时级联删除其所有时间点） */
    @Delete
    suspend fun deleteTimeline(timeline: TimelineEntity)

    /** 按 ID 删除时间线 */
    @Query("DELETE FROM timelines WHERE id = :id")
    suspend fun deleteTimelineById(id: Long)
}
