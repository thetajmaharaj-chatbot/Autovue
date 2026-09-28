package za.co.autovue

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val view = findViewById<PlayerView>(R.id.player)
        val url = findViewById<android.widget.EditText>(R.id.url)
        player = ExoPlayer.Builder(this).build().also { view.player = it }
        findViewById<android.widget.Button>(R.id.play).setOnClickListener {
            val source = url.text.toString().trim()
            if (source.isNotEmpty()) {
                player?.apply {
                    setMediaItem(MediaItem.fromUri(source))
                    prepare()
                    play()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
