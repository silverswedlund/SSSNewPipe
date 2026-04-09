package org.schabi.newpipe.local.channel

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import org.schabi.newpipe.NewPipeDatabase
import org.schabi.newpipe.database.channel.dao.BlockedChannelDAO
import org.schabi.newpipe.database.channel.model.BlockedChannelEntity

/**
 * Central manager for all channel blocking operations.
 * Maintains an in-memory cache of blocked URLs for fast O(1) lookups.
 * The cache is pre-loaded on a background thread at construction time so that
 * isBlocked() is always safe to call from the main thread.
 */
class BlockedChannelManager(context: Context) {

    private val blockedChannelDAO: BlockedChannelDAO =
        NewPipeDatabase.getInstance(context).blockedChannelDAO()

    // In-memory cache for fast synchronous lookups
    private val blockedUrlsCache: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Latch released once the background load completes — callers on the main
    // thread will wait here for at most a few milliseconds on first use.
    private val cacheReadyLatch = CountDownLatch(1)

    init {
        // Load from DB on an IO thread so we never touch the DB on the main thread.
        Schedulers.io().scheduleDirect {
            blockedUrlsCache.addAll(blockedChannelDAO.getAllUrlsSync())
            cacheReadyLatch.countDown()
        }
    }

    private fun ensureCacheInitialized() {
        cacheReadyLatch.await()
    }

    /**
     * Block a channel. Inserts it into the database and updates the cache.
     */
    fun blockChannel(
        serviceId: Int,
        url: String,
        name: String?,
        avatarUrl: String?
    ): Completable {
        return Completable.fromAction {
            val entity = BlockedChannelEntity(
                serviceId = serviceId,
                url = url,
                name = name,
                avatarUrl = avatarUrl,
                blockedAt = OffsetDateTime.now()
            )
            blockedChannelDAO.insert(entity)
            blockedUrlsCache.add(url)
        }.subscribeOn(Schedulers.io())
    }

    /**
     * Unblock a channel. Removes it from the database and updates the cache.
     */
    fun unblockChannel(url: String): Completable {
        return Completable.fromAction {
            blockedChannelDAO.deleteByUrl(url)
            blockedUrlsCache.remove(url)
        }.subscribeOn(Schedulers.io())
    }

    /**
     * Fast synchronous check if a channel URL is blocked.
     * Uses the in-memory cache for O(1) performance.
     */
    fun isBlocked(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        ensureCacheInitialized()
        return blockedUrlsCache.contains(url)
    }

    /**
     * Reactive check if a channel URL is blocked.
     */
    fun isBlockedSingle(url: String): Single<Boolean> {
        return Single.fromCallable { isBlocked(url) }
            .subscribeOn(Schedulers.io())
    }

    /**
     * Reactive list of all blocked channels, for use in settings UI.
     */
    fun getBlockedChannels(): Flowable<List<BlockedChannelEntity>> {
        return blockedChannelDAO.getAll()
    }

    /**
     * Reactive set of all blocked URLs, for filtering.
     */
    fun getBlockedUrlsFlowable(): Flowable<Set<String>> {
        return blockedChannelDAO.getAllUrls()
            .map { it.toSet() }
            .doOnNext { urls ->
                blockedUrlsCache.clear()
                blockedUrlsCache.addAll(urls)
            }
    }

    /**
     * Force refresh the cache from database. Must be called from a background thread.
     */
    fun refreshCache() {
        val fresh = blockedChannelDAO.getAllUrlsSync()
        blockedUrlsCache.clear()
        blockedUrlsCache.addAll(fresh)
        // In case init hadn't finished yet, release the latch now.
        cacheReadyLatch.countDown()
    }

    companion object {
        @Volatile
        private var instance: BlockedChannelManager? = null

        @JvmStatic
        fun getInstance(context: Context): BlockedChannelManager {
            return instance ?: synchronized(this) {
                instance ?: BlockedChannelManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
