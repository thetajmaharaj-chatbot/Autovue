package za.co.autovue

import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class AutoVueMediaService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibrarySession? = null

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SavedMediaStore.KEY_URL) {
                val itemCount =
                    if (SavedMediaStore.mediaItem(this) == null) 0 else 1
                mediaLibrarySession?.notifyChildrenChanged(
                    ROOT_ID,
                    itemCount,
                    null
                )
            }
        }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this).build()
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            LibraryCallback()
        ).build()

        SavedMediaStore.preferences(this)
            .registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        SavedMediaStore.preferences(this)
            .unregisterOnSharedPreferenceChangeListener(preferenceListener)

        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(
                LibraryResult.ofItem(SavedMediaStore.rootItem(), params)
            )

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = SavedMediaStore.resolve(this@AutoVueMediaService, mediaId)
            return if (item != null) {
                Futures.immediateFuture(LibraryResult.ofItem(item, null))
            } else {
                Futures.immediateFuture(
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                )
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = if (parentId == ROOT_ID) {
                listOfNotNull(SavedMediaStore.mediaItem(this@AutoVueMediaService))
            } else {
                emptyList()
            }

            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
            )
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val matches = SavedMediaStore.search(this@AutoVueMediaService, query)
            session.notifySearchResultChanged(
                browser,
                query,
                matches.size,
                params
            )
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val matches = SavedMediaStore.search(this@AutoVueMediaService, query)
                .take(pageSize)
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(matches), params)
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val resolved = mediaItems.mapNotNull { requested ->
                SavedMediaStore.resolve(
                    this@AutoVueMediaService,
                    requested.mediaId
                ) ?: requested.takeIf { it.localConfiguration != null }
            }
            return Futures.immediateFuture(resolved)
        }
    }

    companion object {
        const val ROOT_ID = "autovue_root"
    }
}
