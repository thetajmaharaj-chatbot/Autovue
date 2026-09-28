package za.co.autovue

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private var player: ExoPlayer? = null
    private val homeUrl = "https://www.youtube.com/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        address = findViewById(R.id.url)
        web = findViewById(R.id.web)
        val playerView = findViewById<PlayerView>(R.id.player)
        player = ExoPlayer.Builder(this).build().also { playerView.player = it }

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                address.setText(url.orEmpty())
            }
        }

        fun normalizedUrl(raw: String): String {
            val value = raw.trim()
            return if (value.startsWith("http://") || value.startsWith("https://")) value
            else "https://$value"
        }
        fun browse() {
            val value = address.text.toString()
            if (value.isNotBlank()) web.loadUrl(normalizedUrl(value))
        }
        findViewById<android.view.View>(R.id.go).setOnClickListener { browse() }
        findViewById<android.view.View>(R.id.home).setOnClickListener { web.loadUrl(homeUrl) }
        findViewById<android.view.View>(R.id.back).setOnClickListener { if (web.canGoBack()) web.goBack() }
        findViewById<android.view.View>(R.id.forward).setOnClickListener { if (web.canGoForward()) web.goForward() }
        findViewById<android.view.View>(R.id.playDirect).setOnClickListener {
            val source = address.text.toString().trim()
            if (source.isNotEmpty()) player?.apply {
                setMediaItem(MediaItem.fromUri(normalizedUrl(source)))
                prepare()
                play()
            }
        }
        address.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) { browse(); true } else false
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })
        if (savedInstanceState == null) web.loadUrl(homeUrl)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        web.destroy()
        player?.release()
        player = null
        super.onDestroy()
    }
}