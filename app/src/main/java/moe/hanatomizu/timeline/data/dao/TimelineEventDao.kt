package moe.hanatomizu.timeline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * 时间点数据访问对象
 */
@Dao
interface TimelineEventDao {

    /** 获取某时间线下的所有时间点，按时间升序排列 */
    @Query("SELECT * FROM timeline_events WHERE timelineId = :timelineId ORDER BY eventDate ASC")
    fun getEventsByTimelineId(timelineId: Long): Flow<List<TimelineEventEntity>>

    /** 按 ID 查找单个时间点 */
    @Query("SELECT * FROM timeline_events WHERE id = :id")
    suspend fun getEventById(id: Long): TimelineEventEntity?

    /** 插入新时间点，返回生成的主键 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TimelineEventEntity): Long

    /** 更新已有时间点 */
    @Update
    suspend fun updateEvent(event: TimelineEventEntity)

    /** 删除一个时间点 */
    @Delete
    suspend fun deleteEvent(event: TimelineEventEntity)

    /** 按 ID 删除时间点 */
    @Query("DELETE FROM timeline_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}
