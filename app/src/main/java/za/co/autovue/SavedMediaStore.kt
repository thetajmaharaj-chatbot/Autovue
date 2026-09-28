package za.co.autovue

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

object SavedMediaStore {
    const val PREFS_NAME = "autovue_media"
    const val KEY_URL = "saved_url"
    private const val MEDIA_ID = "saved_audio"

    data class SavedMedia(
        val url: String,
        val title: String
    )

    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, url: String) {
        preferences(context)
            .edit()
            .putString(KEY_URL, url)
            .apply()
    }

    fun get(context: Context): SavedMedia? {
        val url = preferences(context)
            .getString(KEY_URL, null)
            ?.trim()
            .orEmpty()

        if (url.isBlank()) return null

        val title = Uri.parse(url).host
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?: "Saved audio"

        return SavedMedia(url = url, title = title)
    }

    fun rootItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(AutoVueMediaService.ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("AutoVue Audio")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    fun mediaItem(context: Context): MediaItem? {
        val saved = get(context) ?: return null
        return MediaItem.Builder()
            .setMediaId(MEDIA_ID)
            .setUri(saved.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(saved.title)
                    .setSubtitle("Saved direct audio stream")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    fun resolve(context: Context, mediaId: String): MediaItem? =
        when (mediaId) {
            AutoVueMediaService.ROOT_ID -> rootItem()
            MEDIA_ID -> mediaItem(context)
            else -> null
        }

    fun search(context: Context, query: String): List<MediaItem> {
        val item = mediaItem(context) ?: return emptyList()
        val saved = get(context) ?: return emptyList()
        if (query.isBlank()) return listOf(item)

        val needle = query.trim().lowercase()
        return if (
            saved.title.lowercase().contains(needle) ||
            saved.url.lowercase().contains(needle)
        ) {
            listOf(item)
        } else {
            emptyList()
        }
    }
}
