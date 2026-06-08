package moe.hanatomizu.timeline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import moe.hanatomizu.timeline.data.dao.TimelineDao
import moe.hanatomizu.timeline.data.dao.TimelineEventDao
import moe.hanatomizu.timeline.data.entity.TimelineEntity
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity

/**
 * Room 数据库 —— 单例模式，包含时间线表和时间点表。
 *
 * 数据库版本为 1，首次运行自动建表。
 */
@Database(
    entities = [TimelineEntity::class, TimelineEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun timelineDao(): TimelineDao
    abstract fun timelineEventDao(): TimelineEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timeline_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
