package org.schabi.newpipe.database.channel.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.OffsetDateTime

@Entity(
    tableName = BlockedChannelEntity.TABLE_NAME,
    indices = [
        Index(
            value = [BlockedChannelEntity.COLUMN_SERVICE_ID, BlockedChannelEntity.COLUMN_URL],
            unique = true
        )
    ]
)
data class BlockedChannelEntity(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,

    @ColumnInfo(name = COLUMN_SERVICE_ID)
    var serviceId: Int = 0,

    @ColumnInfo(name = COLUMN_URL)
    var url: String = "",

    @ColumnInfo(name = COLUMN_NAME)
    var name: String? = null,

    @ColumnInfo(name = COLUMN_AVATAR_URL)
    var avatarUrl: String? = null,

    @ColumnInfo(name = COLUMN_BLOCKED_AT)
    var blockedAt: OffsetDateTime = OffsetDateTime.now()
) {
    companion object {
        const val TABLE_NAME = "blocked_channels"
        const val COLUMN_SERVICE_ID = "service_id"
        const val COLUMN_URL = "url"
        const val COLUMN_NAME = "name"
        const val COLUMN_AVATAR_URL = "avatar_url"
        const val COLUMN_BLOCKED_AT = "blocked_at"
    }
}
