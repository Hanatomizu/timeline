package moe.hanatomizu.timeline.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import moe.hanatomizu.timeline.data.entity.EventImageEntity

/**
 * 事件图片数据访问对象
 */
@Dao
interface EventImageDao {

    /** 获取某个时间点的所有图片，按 sortOrder 升序排列 */
    @Query("SELECT * FROM event_images WHERE eventId = :eventId ORDER BY sortOrder ASC")
    suspend fun getImagesByEventId(eventId: Long): List<EventImageEntity>

    /** 批量插入图片 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<EventImageEntity>)

    /** 删除某时间点的所有图片 */
    @Query("DELETE FROM event_images WHERE eventId = :eventId")
    suspend fun deleteImagesByEventId(eventId: Long)
}
