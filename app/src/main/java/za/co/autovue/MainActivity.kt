package za.co.autovue

import android.annotation.SuppressLint
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

        fun normalizedUrl(raw: String): String {
            val value = raw.trim()
            return if (value.startsWith("http://") || value.startsWith("https://")) {
                value
            } else {
                "https://$value"
            }
        }

        fun browse() {
            val value = address.text.toString()
            if (value.isNotBlank()) web.loadUrl(normalizedUrl(value))
        }

        fun updateSavedStatus() {
            val saved = SavedMediaStore.get(this)
            carStatus.text = if (saved == null) {
                "Android Auto audio: no saved stream"
            } else {
                "Android Auto audio: ${saved.title}"
            }
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
            val source = address.text.toString().trim()
            if (source.isNotEmpty()) {
                player?.apply {
                    setMediaItem(MediaItem.fromUri(normalizedUrl(source)))
                    prepare()
                    play()
                }
            }
        }
        findViewById<View>(R.id.saveForCar).setOnClickListener {
            val source = address.text.toString().trim()
            if (source.isNotEmpty()) {
                SavedMediaStore.save(this, normalizedUrl(source))
                updateSavedStatus()
                Toast.makeText(
                    this,
                    "Saved for Android Auto audio. Use a direct playable media URL.",
                    Toast.LENGTH_LONG
                ).show()
            }
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
        if (savedInstanceState == null) web.loadUrl(homeUrl)
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
