package com.tajweed.app
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

class Recorder(val ctx: Context) {
  private var mr: MediaRecorder? = null; private var mp: MediaPlayer? = null
  val file = File(ctx.cacheDir, "practice.m4a")
  fun start() { mr = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()).apply {
    setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(file.path); prepare(); start() } }
  fun stop() { try { mr?.stop() } catch (_: Exception) {}; mr?.release(); mr = null }
  fun play() { if (!file.exists()) return; mp?.release(); mp = MediaPlayer().apply { setDataSource(file.path); prepare(); start() } }
}
val commonMistakes = mapOf("ض" to "إخراجه ظاءً أو دالًا مفخمة؛ تدرّب على إخراجه من حافة اللسان مع الاستطالة",
  "ظ" to "إخراجه ضادًا؛ طرف اللسان بين الثنايا لا حافته", "ط" to "ترقيقه فيصير تاءً؛ حافظ على الاستعلاء والإطباق",
  "ص" to "نطقه سينًا؛ فخّمه مع الإطباق", "ذ" to "نطقه زايًا؛ اجعل طرف اللسان بين الثنايا",
  "ق" to "ترقيقه فيصير كافًا؛ فخّمه وأخرجه من أقصى اللسان", "ح" to "نطقه هاءً؛ أخرجه من وسط الحلق", "ع" to "تبديله همزة")
fun mouthPos(m: String): Offset = when {
  "الشفتان" in m || "الشفة" in m -> Offset(.07f, .55f); "أقصى الحلق" in m -> Offset(.93f, .75f); "وسط الحلق" in m -> Offset(.9f, .65f)
  "أدنى الحلق" in m -> Offset(.88f, .55f); "الجوف" in m -> Offset(.55f, .5f); "حافة" in m -> Offset(.6f, .4f)
  "أقصى اللسان" in m -> Offset(.75f, .42f); "وسط اللسان" in m -> Offset(.55f, .3f); "طرف" in m -> Offset(.2f, .5f); else -> Offset(.5f, .5f) }

@Composable fun LetterExtras(l: Letter, rec0: Recitation) {
  val ctx = LocalContext.current; val rec = remember { Recorder(ctx) }; var recording by remember { mutableStateOf(false) }
  val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) { rec.start(); recording = true } }
  val pulse by rememberInfiniteTransition(label = "p").animateFloat(8f, 18f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "r")
  val col = MaterialTheme.colorScheme.primary; val line = MaterialTheme.colorScheme.onSurface
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MouthAnimation(l)
    exampleAyah[l.name]?.let { (s, a) -> OutlinedButton({ rec0.play(s, a, a, 1, 1f) }, Modifier.fillMaxWidth()) { Text("استمع لمثال بصوت الحصري: ${surahNames[s - 1]} آية $a") } }
    commonMistakes[l.name]?.let { Text("خطأ شائع: $it") } ?: Text("خطأ شائع: عدم ضبط المخرج وإهمال الصفة")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button({ if (!recording) { if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { rec.start(); recording = true } else perm.launch(Manifest.permission.RECORD_AUDIO) } else { rec.stop(); recording = false } }) { Text(if (recording) "إيقاف التسجيل" else "سجّل صوتك") }
      OutlinedButton({ rec.play() }) { Text("استمع لتسجيلك") } }
    val opts = remember(l) { (letters.filter { it.makhraj != l.makhraj }.map { it.makhraj }.distinct().shuffled().take(2) + l.makhraj).shuffled() }
    var pick by remember(l) { mutableStateOf<String?>(null) }
    Text("اختبار: ما مخرج حرف ${l.name}؟")
    opts.forEach { o -> OutlinedButton({ pick = o }, Modifier.fillMaxWidth()) { Text(o + if (pick != null && o == l.makhraj) " ✓" else if (pick == o) " ✗" else "") } }
  }
}

val exampleAyah = mapOf("ب" to (1 to 1), "ا" to (1 to 2), "ض" to (1 to 7), "غ" to (1 to 7), "ء" to (1 to 7), "م" to (1 to 1), "ت" to (1 to 7), "س" to (1 to 5), "ص" to (1 to 6),
  "ذ" to (1 to 7), "ع" to (1 to 2), "ح" to (1 to 2), "خ" to (103 to 2), "ق" to (112 to 1), "ك" to (108 to 1), "ج" to (110 to 1), "ش" to (108 to 3), "ي" to (1 to 4),
  "ل" to (1 to 1), "ن" to (1 to 5), "ر" to (1 to 1), "د" to (1 to 4), "ف" to (110 to 3), "و" to (1 to 5), "هـ" to (112 to 1))
