package com.tajweed.app
import android.content.Context
import kotlinx.coroutines.*
import java.io.File
import java.net.URL
object Offline {
  fun file(c: Context, s: Int, a: Int) = File(c.filesDir, "audio/%03d%03d.mp3".format(s, a))
  fun uri(c: Context, s: Int, a: Int): String { val f = file(c, s, a); return if (f.exists()) f.toURI().toString() else ayahUrl(s, a) }
  fun done(c: Context, s: Int) = (1..ayahCount[s - 1]).all { file(c, s, it).exists() }
  fun delete(c: Context, s: Int) { (1..ayahCount[s - 1]).forEach { file(c, s, it).delete() } }
  suspend fun download(c: Context, s: Int, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
    file(c, s, 1).parentFile!!.mkdirs()
    for (a in 1..ayahCount[s - 1]) { val f = file(c, s, a)
      if (!f.exists()) { val t = File(f.path + ".part"); URL(ayahUrl(s, a)).openStream().use { i -> t.outputStream().use { o -> i.copyTo(o) } }; t.renameTo(f) }
      withContext(Dispatchers.Main) { onProgress(a) } }
  }
}
