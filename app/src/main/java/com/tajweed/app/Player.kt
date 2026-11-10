package com.tajweed.app
import android.content.Context
import androidx.compose.runtime.*
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer

class Recitation(val ctx: Context) {
  val exo = ExoPlayer.Builder(ctx).build()
  var current by mutableStateOf(""); var error by mutableStateOf("")
  private var loopsLeft = 1
  init { exo.addListener(object : Player.Listener {
    override fun onMediaItemTransition(m: MediaItem?, reason: Int) { current = m?.mediaId ?: ""; error = "" }
    override fun onPlayerError(e: PlaybackException) { error = "تعذّر التشغيل: تحقق من الإنترنت أو حمّل السورة من تبويب التحميل" }
    override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED && (loopsLeft == 0 || loopsLeft > 1)) { if (loopsLeft > 1) loopsLeft--; exo.seekTo(0, 0L); exo.play() } } }) }
  /** يشغل آيات من..إلى، التكرار: 1 مرة، أو N مرات، أو 0 بلا توقف */
  fun play(surah: Int, from: Int, to: Int, repeat: Int, speed: Float) {
    loopsLeft = repeat
    exo.setMediaItems((from..to).map { MediaItem.Builder().setUri(Offline.uri(ctx, surah, it)).setMediaId("$surah:$it").build() })
    exo.repeatMode = Player.REPEAT_MODE_OFF
    exo.playbackParameters = PlaybackParameters(speed)
    exo.prepare(); exo.play()
  }
  fun sleepAfter(ms: Long, h: android.os.Handler) { h.postDelayed({ exo.pause() }, ms) }
  fun release() = exo.release()
}
