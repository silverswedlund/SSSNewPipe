package org.schabi.newpipe.database.channel.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import org.schabi.newpipe.database.channel.model.BlockedChannelEntity

@Dao
abstract class BlockedChannelDAO {

    @Query("SELECT * FROM blocked_channels ORDER BY blocked_at DESC")
    abstract fun getAll(): Flowable<List<BlockedChannelEntity>>

    @Query("SELECT url FROM blocked_channels")
    abstract fun getAllUrls(): Flowable<List<String>>

    @Query("SELECT url FROM blocked_channels")
    abstract fun getAllUrlsSync(): List<String>

    @Query("SELECT * FROM blocked_channels WHERE url = :url LIMIT 1")
    abstract fun getByUrl(url: String): Maybe<BlockedChannelEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_channels WHERE url = :url)")
    abstract fun isBlocked(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(entity: BlockedChannelEntity): Long

    @Delete
    abstract fun delete(entity: BlockedChannelEntity)

    @Query("DELETE FROM blocked_channels WHERE url = :url")
    abstract fun deleteByUrl(url: String)

    @Query("DELETE FROM blocked_channels")
    abstract fun deleteAll(): Int
}
