package moe.hanatomizu.timeline.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import moe.hanatomizu.timeline.data.dao.EventImageDao
import moe.hanatomizu.timeline.data.dao.TimelineDao
import moe.hanatomizu.timeline.data.dao.TimelineEventDao
import moe.hanatomizu.timeline.data.entity.EventImageEntity
import moe.hanatomizu.timeline.data.entity.TimelineEntity
import moe.hanatomizu.timeline.data.entity.TimelineEventEntity

/**
 * Room 数据库 —— 单例模式。
 *
 * version 1 → 2：新增 event_images 子表，将原有 imagePath 迁移到子表。
 */
@Database(
    entities = [TimelineEntity::class, TimelineEventEntity::class, EventImageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun timelineDao(): TimelineDao
    abstract fun timelineEventDao(): TimelineEventDao
    abstract fun eventImageDao(): EventImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timeline_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 创建 event_images 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `event_images` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventId` INTEGER NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`eventId`) REFERENCES `timeline_events`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_event_images_eventId` ON `event_images`(`eventId`)")

                // 2. 迁移旧数据：将 timeline_events.imagePath 非空值插入 event_images
                db.execSQL("""
                    INSERT INTO event_images (eventId, imagePath, sortOrder)
                    SELECT id, imagePath, 0 FROM timeline_events WHERE imagePath IS NOT NULL
                """)

                // 3. 删除 imagePath 列（SQLite 不支持直接 DROP COLUMN，需建新表替换）
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `timeline_events_v2` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timelineId` INTEGER NOT NULL,
                        `content` TEXT NOT NULL,
                        `labelColor` INTEGER NOT NULL,
                        `eventDate` INTEGER NOT NULL,
                        FOREIGN KEY(`timelineId`) REFERENCES `timelines`(`id`) ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    INSERT INTO timeline_events_v2 (id, timelineId, content, labelColor, eventDate)
                    SELECT id, timelineId, content, labelColor, eventDate FROM timeline_events
                """)
                db.execSQL("DROP TABLE timeline_events")
                db.execSQL("ALTER TABLE timeline_events_v2 RENAME TO timeline_events")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_events_timelineId` ON `timeline_events`(`timelineId`)")
            }
        }
    }
}
