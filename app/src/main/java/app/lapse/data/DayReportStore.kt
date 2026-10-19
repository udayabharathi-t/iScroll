package app.lapse.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DayReportStore(context: Context) {
    private val helper = Helper(context.applicationContext)
    private val lock = Any()
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState.asStateFlow()

    fun add(
        day: String,
        instagramMs: Long = 0,
        youtubeMs: Long = 0,
        redditMs: Long = 0,
        reels: Int = 0,
        shorts: Int = 0,
        posts: Int = 0,
    ) {
        if (instagramMs == 0L && youtubeMs == 0L && redditMs == 0L && reels == 0 && shorts == 0 && posts == 0) {
            return
        }
        synchronized(lock) {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val updated = db.compileStatement(
                    """
                    UPDATE days SET
                        instagram_ms = instagram_ms + ?,
                        youtube_ms = youtube_ms + ?,
                        reddit_ms = reddit_ms + ?,
                        reels = reels + ?,
                        shorts = shorts + ?,
                        posts = posts + ?
                    WHERE day = ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.bindLong(1, instagramMs)
                    statement.bindLong(2, youtubeMs)
                    statement.bindLong(3, redditMs)
                    statement.bindLong(4, reels.toLong())
                    statement.bindLong(5, shorts.toLong())
                    statement.bindLong(6, posts.toLong())
                    statement.bindString(7, day)
                    statement.executeUpdateDelete()
                }
                if (updated == 0) {
                    db.execSQL(
                        """
                        INSERT INTO days(day, instagram_ms, youtube_ms, reddit_ms, reels, shorts, posts)
                        VALUES(?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf<Any>(day, instagramMs, youtubeMs, redditMs, reels, shorts, posts),
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            revisionState.value = revisionState.value + 1
        }
    }

    fun today(today: LocalDate = LocalDate.now()): DayReport {
        return day(today.toString()) ?: DayReport.empty(today.toString())
    }

    fun day(day: String): DayReport? = synchronized(lock) {
        helper.readableDatabase.rawQuery(SELECT + " WHERE day = ?", arrayOf(day)).use { cursor ->
            if (!cursor.moveToFirst()) null else read(cursor)
        }
    }

    fun recent(limit: Int = 90): List<DayReport> = synchronized(lock) {
        helper.readableDatabase.rawQuery(
            "$SELECT ORDER BY day DESC LIMIT ?",
            arrayOf(limit.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(read(cursor))
            }
        }
    }

    private fun read(cursor: Cursor) = DayReport(
        day = cursor.getString(0),
        instagramMs = cursor.getLong(1),
        youtubeMs = cursor.getLong(2),
        redditMs = cursor.getLong(3),
        reels = cursor.getInt(4),
        shorts = cursor.getInt(5),
        posts = cursor.getInt(6),
    )

    private class Helper(context: Context) : SQLiteOpenHelper(context, "lapse.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE days (
                    day TEXT PRIMARY KEY,
                    instagram_ms INTEGER NOT NULL DEFAULT 0,
                    youtube_ms INTEGER NOT NULL DEFAULT 0,
                    reddit_ms INTEGER NOT NULL DEFAULT 0,
                    reels INTEGER NOT NULL DEFAULT 0,
                    shorts INTEGER NOT NULL DEFAULT 0,
                    posts INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private companion object {
        const val SELECT =
            "SELECT day, instagram_ms, youtube_ms, reddit_ms, reels, shorts, posts FROM days"
    }
}
