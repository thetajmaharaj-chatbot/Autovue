package za.co.autovue

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object SavedMediaStore {
    const val PREFS_NAME = "autovue_media"
    const val KEY_URL = "saved_url"
    const val KEY_ITEMS = "saved_items_v2"

    data class SavedMedia(
        val id: String,
        val url: String,
        val title: String
    )

    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, url: String) {
        val items = all(context).toMutableList()
        if (items.any { it.url == url }) return

        items += SavedMedia(
            id = "saved_audio_${UUID.randomUUID()}",
            url = url,
            title = titleFor(url)
        )
        write(context, items)
    }

    fun clear(context: Context) {
        preferences(context)
            .edit()
            .remove(KEY_ITEMS)
            .remove(KEY_URL)
            .apply()
    }

    fun all(context: Context): List<SavedMedia> {
        val prefs = preferences(context)
        val raw = prefs.getString(KEY_ITEMS, null)

        if (raw.isNullOrBlank()) {
            val legacyUrl = prefs.getString(KEY_URL, null)?.trim().orEmpty()
            if (legacyUrl.isBlank()) return emptyList()

            val migrated = listOf(
                SavedMedia(
                    id = "saved_audio_${UUID.randomUUID()}",
                    url = legacyUrl,
                    title = titleFor(legacyUrl)
                )
            )
            write(context, migrated)
            prefs.edit().remove(KEY_URL).apply()
            return migrated
        }

        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val id = obj.optString("id")
                    val url = obj.optString("url")
                    val title = obj.optString("title")
                    if (id.isNotBlank() && url.isNotBlank()) {
                        add(
                            SavedMedia(
                                id = id,
                                url = url,
                                title = title.ifBlank { titleFor(url) }
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
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

    fun mediaItems(context: Context): List<MediaItem> =
        all(context).map(::toMediaItem)

    fun resolve(context: Context, mediaId: String): MediaItem? =
        when (mediaId) {
            AutoVueMediaService.ROOT_ID -> rootItem()
            else -> all(context)
                .firstOrNull { it.id == mediaId }
                ?.let(::toMediaItem)
        }

    fun search(context: Context, query: String): List<MediaItem> {
        val items = all(context)
        if (query.isBlank()) return items.map(::toMediaItem)

        val needle = query.trim().lowercase()
        return items
            .filter {
                it.title.lowercase().contains(needle) ||
                    it.url.lowercase().contains(needle)
            }
            .map(::toMediaItem)
    }

    private fun toMediaItem(saved: SavedMedia): MediaItem =
        MediaItem.Builder()
            .setMediaId(saved.id)
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

    private fun write(context: Context, items: List<SavedMedia>) {
        val array = JSONArray()
        items.forEach { saved ->
            array.put(
                JSONObject()
                    .put("id", saved.id)
                    .put("url", saved.url)
                    .put("title", saved.title)
            )
        }

        preferences(context)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }

    private fun titleFor(url: String): String {
        val uri = Uri.parse(url)
        return when (uri.scheme?.lowercase()) {
            "content" -> "Local media"
            "file" -> uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: "Local media"
            else -> uri.host
                ?.removePrefix("www.")
                ?.takeIf { it.isNotBlank() }
                ?: "Saved audio"
        }
    }
}
