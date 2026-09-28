package za.co.autovue

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var carStatus: TextView
    private lateinit var mainUi: LinearLayout
    private lateinit var fullscreenContainer: FrameLayout

    private var player: ExoPlayer? = null
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    private val homeUrl = "https://www.youtube.com/"

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some document providers grant access without persistable permissions.
                }

                address.setText(uri.toString())
                playMedia(uri)
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        address = findViewById(R.id.url)
        carStatus = findViewById(R.id.carStatus)
        web = findViewById(R.id.web)
        mainUi = findViewById(R.id.mainUi)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        val playerView = findViewById<PlayerView>(R.id.player)
        player = ExoPlayer.Builder(this).build().also { playerView.player = it }

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = true
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(
                view: View?,
                callback: CustomViewCallback?
            ) {
                if (view == null || fullscreenView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                fullscreenView = view
                fullscreenCallback = callback
                mainUi.visibility = View.GONE
                fullscreenContainer.visibility = View.VISIBLE
                fullscreenContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }

            override fun onHideCustomView() {
                hideFullscreenVideo()
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                address.setText(url.orEmpty())
            }
        }

        fun browse() {
            val value = normalizeInput(address.text.toString())
            if (value.isNotBlank()) web.loadUrl(value)
        }

        fun updateSavedStatus() {
            val count = SavedMediaStore.all(this).size
            carStatus.text = "Android Auto audio: $count saved"
        }

        fun showSavedMediaManager() {
            val saved = SavedMediaStore.all(this)
            if (saved.isEmpty()) {
                Toast.makeText(
                    this,
                    "No Android Auto audio has been saved yet.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val labels = saved
                .map { "${it.title}\n${it.url}" }
                .toTypedArray()

            AlertDialog.Builder(this)
                .setTitle("Saved Android Auto audio")
                .setItems(labels) { _, index ->
                    SavedMediaStore.remove(this, saved[index].id)
                    updateSavedStatus()
                    Toast.makeText(
                        this,
                        "Removed ${saved[index].title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNeutralButton("Clear all") { _, _ ->
                    SavedMediaStore.clear(this)
                    updateSavedStatus()
                }
                .setNegativeButton("Close", null)
                .show()
        }

        findViewById<View>(R.id.go).setOnClickListener { browse() }
        findViewById<View>(R.id.home).setOnClickListener { web.loadUrl(homeUrl) }
        findViewById<View>(R.id.back).setOnClickListener {
            if (web.canGoBack()) web.goBack()
        }
        findViewById<View>(R.id.forward).setOnClickListener {
            if (web.canGoForward()) web.goForward()
        }
        findViewById<View>(R.id.playDirect).setOnClickListener {
            val source = normalizeInput(address.text.toString())
            if (source.isNotEmpty()) playMedia(Uri.parse(source))
        }
        findViewById<View>(R.id.openFile).setOnClickListener {
            openDocument.launch(arrayOf("audio/*", "video/*"))
        }
        findViewById<View>(R.id.saveForCar).setOnClickListener {
            val source = normalizeInput(address.text.toString())
            if (source.isNotEmpty()) {
                val before = SavedMediaStore.all(this).size
                SavedMediaStore.save(this, source)
                val after = SavedMediaStore.all(this).size
                updateSavedStatus()

                Toast.makeText(
                    this,
                    if (after > before) {
                        "Added to Android Auto audio library."
                    } else {
                        "Already saved in Android Auto audio library."
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        findViewById<View>(R.id.manageForCar).setOnClickListener {
            showSavedMediaManager()
        }

        address.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) {
                browse()
                true
            } else {
                false
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    fullscreenView != null -> hideFullscreenVideo()
                    web.canGoBack() -> web.goBack()
                    else -> finish()
                }
            }
        })

        updateSavedStatus()

        val handled = handleIncomingIntent(intent)
        if (!handled && savedInstanceState == null) {
            web.loadUrl(homeUrl)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent?): Boolean {
        if (incoming == null) return false

        return when (incoming.action) {
            Intent.ACTION_SEND -> {
                val sharedText = incoming.getStringExtra(Intent.EXTRA_TEXT)
                    ?.trim()
                    .orEmpty()

                if (sharedText.isBlank()) {
                    false
                } else {
                    val source = normalizeInput(sharedText)
                    address.setText(source)
                    web.loadUrl(source)
                    true
                }
            }

            Intent.ACTION_VIEW -> {
                val uri = incoming.data ?: return false
                address.setText(uri.toString())

                val type = incoming.type.orEmpty()
                if (type.startsWith("audio/") || type.startsWith("video/")) {
                    playMedia(uri)
                } else {
                    web.loadUrl(uri.toString())
                }
                true
            }

            else -> false
        }
    }

    private fun normalizeInput(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return value

        val parsed = Uri.parse(value)
        return if (!parsed.scheme.isNullOrBlank()) value else "https://$value"
    }

    private fun playMedia(uri: Uri) {
        player?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
    }

    private fun hideFullscreenVideo() {
        val view = fullscreenView ?: return
        fullscreenContainer.removeView(view)
        fullscreenContainer.visibility = View.GONE
        mainUi.visibility = View.VISIBLE
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        hideFullscreenVideo()
        web.destroy()
        player?.release()
        player = null
        super.onDestroy()
    }
}
