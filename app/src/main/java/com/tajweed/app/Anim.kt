package com.tajweed.app
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

private const val PIf = 3.1415927f
@Composable fun rememberClock(period: Float, speed: Float = 1f, running: Boolean = true): MutableFloatState {
  val t = remember { mutableFloatStateOf(0f) }
  LaunchedEffect(running, speed, period) { if (running) { var last = withFrameNanos { it }
    while (true) { val now = withFrameNanos { it }; t.floatValue = (t.floatValue + (now - last) / 1e9f * speed / period) % 1f; last = now } } }
  return t
}
// ===== نموذج النطق: مواضع كسرية داخل لوحة الرسم =====
data class Art(val zone: String, val cx: Float, val cy: Float, val voiced: Boolean, val stop: Boolean, val nasal: Boolean, val istila: Boolean, val itbaq: Boolean,
  val takrir: Boolean, val tafashi: Boolean, val side: Boolean, val qalqala: Boolean, val istitala: Boolean) {
  val stopLike get() = stop || nasal || takrir || side
}
fun palY(x: Float) = .35f - .07f * sin(PIf * ((x - .12f) / .6f).coerceIn(0f, 1f))
fun restY(x: Float) = .66f + .2f * ((x - .14f) / .7f) - .05f * sin(PIf * ((x - .14f) / .7f).coerceIn(0f, 1f))
fun art(l: Letter): Art { val m = l.makhraj; val s = l.sifat; var zone = "tongue"; var cx = .3f; var cy = 0f
  when {
    "الجوف" in m -> zone = "open"
    "الشفتان" in m -> { zone = "lips"; cx = .07f; cy = .34f }
    "الشفة" in m -> { zone = "lipteeth"; cx = .12f; cy = .44f }
    "أقصى الحلق" in m -> { zone = "throat"; cx = .86f; cy = .78f }
    "وسط الحلق" in m -> { zone = "throat"; cx = .86f; cy = .62f }
    "أدنى الحلق" in m -> { zone = "throat"; cx = .86f; cy = .47f }
    l.name in listOf("ث", "ذ", "ظ") -> { cx = .15f; cy = .45f }
    "حافة" in m -> cx = if ("ما يليها" in m) .24f else .46f
    "أقصى اللسان" in m -> cx = if ("اللهاة" in m) .70f else .62f
    "وسط اللسان" in m -> cx = .42f
    "الثنايا السفلى" in m -> cx = .25f
    "أصول الثنايا العليا" in m -> cx = .20f
    else -> cx = if ("التكرير" in s) .28f else .20f }
  if (zone == "tongue" && cy == 0f) cy = palY(cx)
  return Art(zone, cx, cy, "الجهر" in s, "الشدة" in s, "الغنة" in s, "الاستعلاء" in s, "الإطباق" in s, "التكرير" in s, "التفشي" in s, "الانحراف" in s, "القلقلة" in s, "الاستطالة" in s)
}
fun closure(a: Art, t: Float): Float {
  var c = if (a.zone == "open") (0.5f - 0.5f * cos(2 * PIf * t))
    else if (a.stopLike) when { t < .3f -> t / .3f; t < .6f -> 1f; t < .7f -> 1f - (t - .6f) / .1f; else -> 0f }
    else (0.5f - 0.5f * cos(2 * PIf * t)) * .9f
  if (a.qalqala && t in .7f..0.85f) c = max(c, .35f * sin(PIf * (t - .7f) / .15f))
  return c
}
private val mouthPath = listOf(Offset(.86f, .80f), Offset(.85f, .62f), Offset(.76f, .52f), Offset(.55f, .49f), Offset(.35f, .47f), Offset(.17f, .50f), Offset(.02f, .50f))
private val nosePath = listOf(Offset(.86f, .80f), Offset(.85f, .55f), Offset(.84f, .30f), Offset(.80f, .14f), Offset(.45f, .13f), Offset(.15f, .14f), Offset(.02f, .16f))
private fun pointOn(path: List<Offset>, s: Float): Offset { val lens = path.zipWithNext { a, b -> (b - a).getDistance() }; var d = s.coerceIn(0f, 1f) * lens.sum()
  for (i in lens.indices) { if (d <= lens[i] || i == lens.lastIndex) { val f = (d / lens[i]).coerceIn(0f, 1f); return path[i] + (path[i + 1] - path[i]) * f }; d -= lens[i] }
  return path.last() }
private fun tongueTop(a: Art, x: Float, m: Float, tremor: Float): Float { var y = restY(x)
  if (a.zone == "tongue") { val gap = restY(a.cx) - a.cy; val sig = if (a.istitala) .12f else if (a.cx < .3f) .055f else .09f
    y -= gap * m * (1f - tremor) * exp(-((x - a.cx) / sig).pow(2)) }
  if (a.zone != "open" && a.istila) y -= (restY(.62f) - palY(.62f)) * .55f * m * exp(-((x - .62f) / .10f).pow(2))
  if (a.zone != "open" && a.itbaq) y -= (restY(.42f) - palY(.42f)) * .6f * m * exp(-((x - .42f) / .16f).pow(2))
  return max(y, palY(x) + .005f) }

@Composable fun MouthAnimation(l: Letter) {
  val a = remember(l) { art(l) }; var playing by remember { mutableStateOf(true) }; var speed by remember { mutableFloatStateOf(.6f) }
  val clock = rememberClock(2.4f, speed, playing); LaunchedEffect(l) { clock.floatValue = 0f }
  val ink = MaterialTheme.colorScheme.onSurface; val tongueC = Color(0xFFE57373); val air = Color(0xFF1E88E5); val cords = Color(0xFF43A047); val red = Color(0xFFE53935)
  val tm = rememberTextMeasurer(); val sContact = remember(l) { (0..100).minByOrNull { (pointOn(mouthPath, it / 100f) - Offset(a.cx, a.cy)).getDistance() }!! / 100f }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Canvas(Modifier.fillMaxWidth().height(280.dp)) {
      val t = clock.floatValue; val w = size.width; val h = size.height; val c = closure(a, t)
      fun X(x: Float) = x * w; fun Y(y: Float) = y * h; fun P(x: Float, y: Float) = Offset(X(x), Y(y))
      val jaw = if (a.zone == "open") .1f * c else 0f
      val tremor = if (a.takrir) .35f * (0.5f + 0.5f * sin(2 * PIf * t * 14f)) else 0f
      val nasalAmt = if (a.nasal) 1f else 0f
      // الأنف والحنك والحنك الرخو
      drawRoundRect(ink.copy(alpha = .25f), P(.10f, .06f), Size(X(.76f), Y(.14f)), CornerRadius(30f), style = Stroke(2f))
      val pal = Path().apply { moveTo(X(.12f), Y(palY(.12f))); var x = .12f; while (x <= .72f) { lineTo(X(x), Y(palY(x))); x += .02f } }
      drawPath(pal, ink, style = Stroke(5f, cap = StrokeCap.Round))
      val vEnd = Offset(X(.88f - .08f * nasalAmt), Y(.24f + .18f * nasalAmt)); drawLine(ink, P(.72f, palY(.72f)), vEnd, 5f, StrokeCap.Round)
      // جدار الحلق (يضيق عند الحروف الحلقية)
      val bulge = if (a.zone == "throat") .07f * c else 0f
      val wall = Path().apply { var y = .15f; moveTo(X(.92f), Y(y)); while (y <= .93f) { lineTo(X(.92f - bulge * exp(-((y - a.cy) / .07f).pow(2))), Y(y)); y += .02f } }
      drawPath(wall, ink, style = Stroke(5f)); drawLine(ink.copy(alpha = .5f), P(.10f, .93f + jaw), P(.92f, .93f), 3f)
      // الأسنان
      val teeth = Color(0xFFEEEEEE); drawRoundRect(teeth, P(.085f, .30f), Size(X(.06f), Y(.14f)), CornerRadius(8f)); drawRoundRect(teeth, P(.085f, .56f + jaw), Size(X(.06f), Y(.12f)), CornerRadius(8f))
      // اللسان
      val tp = Path(); var x = .14f; tp.moveTo(X(x), Y(tongueTop(a, x, c, tremor)))
      while (x <= .84f) { x += .02f; tp.lineTo(X(x), Y(tongueTop(a, x, c, tremor))) }
      x = .84f; while (x >= .14f) { tp.lineTo(X(x), Y(restY(x) + .12f + jaw * .5f)); x -= .02f }; tp.close(); drawPath(tp, tongueC); drawPath(tp, ink.copy(alpha = .6f), style = Stroke(2f))
      // الشفتان
      val lipC = if (a.zone == "lips" || a.zone == "lipteeth") c else 0f
      val lly = when (a.zone) { "lips" -> .62f - lipC * (.62f - .355f); "lipteeth" -> .62f - lipC * .17f; else -> .62f + jaw }
      drawLine(Color(0xFFD81B60), P(.03f, .30f), P(.10f, .30f), 16f, StrokeCap.Round); drawLine(Color(0xFFD81B60), P(.03f, lly), P(.10f, lly), 16f, StrokeCap.Round)
      // الوتران الصوتيان
      val g = if (a.zone == "throat" && a.stop) .032f * (1f - c) else if (a.voiced) .012f + .012f * (0.5f + 0.5f * sin(2 * PIf * t * 16f)) else .032f
      drawLine(cords, P(.92f, .80f - g), P(.86f, .80f - g * .3f), 6f, StrokeCap.Round); drawLine(cords, P(.92f, .80f + g), P(.86f, .80f + g * .3f), 6f, StrokeCap.Round)
      // الهواء
      val path = if (a.nasal) nosePath else mouthPath
      for (i in 0 until 14) { var s = (t * 2f + i / 14f) % 1f
        val held = a.stopLike && !a.nasal && c > .85f && s > sContact; if (held) s = sContact - (i % 3) * .015f
        val p0 = pointOn(path, s); val p = Offset(p0.x, p0.y + cos(i * 1.9f) * (if (a.tafashi) .07f else .012f) * s)
        drawCircle(air, if (held) 7f else 4f, P(p.x, p.y)) }
      // موضع الالتقاء
      if (a.zone != "open") { drawCircle(red.copy(alpha = .25f + .6f * c), w * (.022f + .012f * c), P(a.cx, a.cy), style = Stroke(4f)); if (c > .8f) drawCircle(red, 6f, P(a.cx, a.cy)) }
      val st = TextStyle(color = ink, fontSize = 11.sp)
      drawText(tm, "الخيشوم", P(.36f, .075f), st); drawText(tm, "الحنك", P(.34f, .22f), st); drawText(tm, "اللسان", P(.45f, .80f), TextStyle(color = Color.Black, fontSize = 11.sp))
      drawText(tm, "الشفتان", P(.0f, .80f), st); drawText(tm, "الحلق", P(.80f, .96f), st)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
      Button({ playing = !playing }) { Text(if (playing) "إيقاف مؤقت" else "تشغيل") }
      OutlinedButton({ clock.floatValue = 0f }) { Text("من البداية") }; Text("السرعة ${"%.2f".format(speed)}x") }
    Slider(speed, { speed = it }, valueRange = .2f..1.5f)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text("● الالتقاء", color = red, fontSize = 12.sp); Text("● الهواء", color = air, fontSize = 12.sp); Text("● الوتران", color = cords, fontSize = 12.sp) }
    Text("المخرج: ${l.makhraj}", style = MaterialTheme.typography.titleSmall)
    describe(l, a).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
  }
}
fun describe(l: Letter, a: Art): List<String> { val r = mutableListOf<String>()
  r += if (a.zone == "open") "حرف مد: يخرج الهواء حرًّا من الجوف دون التقاء عضوين" else if (a.stop) "الشدة: ينحبس الهواء عند الموضع الأحمر ثم ينفجر" else if (a.stopLike) "يلتقي العضوان التقاءً كاملًا ثم ينفكان" else "الرخاوة/التوسط: يجري الهواء من ضيق دون انحباس تام"
  r += if (a.voiced) "الجهر: الوتران الصوتيان يهتزان (الخطان الأخضران)" else "الهمس: الوتران مفتوحان ويجري النفَس معه"
  if (a.nasal) r += "الغنة: ينزل الحنك الرخو ويخرج صوت من الخيشوم"
  if (a.istila) r += "الاستعلاء: يرتفع أقصى اللسان نحو الحنك فيُفخَّم الحرف"
  if (a.itbaq) r += "الإطباق: ينطبق وسط اللسان على الحنك الأعلى"
  if (a.takrir) r += "التكرير: يرتعد طرف اللسان (احذر المبالغة)"
  if (a.tafashi) r += "التفشي: ينتشر الهواء في الفم"
  if (a.side) r += "الانحراف: يخرج الصوت من جانبي اللسان"
  if (a.qalqala) r += "القلقلة: اضطراب في المخرج بعد السكون يُسمع منه نبرة"
  if (a.istitala) r += "الاستطالة: يمتد الصوت على طول حافة اللسان"
  if (a.zone == "throat") r += "لاحظ تضيّق جدار الحلق عند الموضع الأحمر"
  return r }

// ===== رسوم الصفات =====
data class Sifa(val name: String, val def: String, val letters: String, val reps: List<String>)
val sifat = listOf(
  Sifa("الهمس والجهر", "الهمس: جريان النفَس مع الحرف لضعف الاعتماد على المخرج. الجهر: انحباس جريانه لقوة الاعتماد.", "الهمس: فحثه شخص سكت. الجهر: بقية الحروف.", listOf("س", "ز")),
  Sifa("الشدة والرخاوة", "الشدة: انحباس جريان الصوت. الرخاوة: جريانه. وبينهما التوسط.", "الشدة: أجد قط بكت. التوسط: لن عمر.", listOf("ت", "ث")),
  Sifa("الاستعلاء والاستفال", "الاستعلاء: ارتفاع أقصى اللسان إلى الحنك فيُفخَّم الحرف. الاستفال ضده.", "الاستعلاء: خص ضغط قظ.", listOf("ص", "س")),
  Sifa("الإطباق والانفتاح", "الإطباق: انطباق اللسان على الحنك الأعلى. الانفتاح ضده.", "الإطباق: ص ض ط ظ.", listOf("ط", "ت")),
  Sifa("القلقلة", "اضطراب المخرج عند السكون حتى يُسمع نبرة قوية.", "قطب جد.", listOf("ب", "د")),
  Sifa("الغنة", "صوت لذيذ يخرج من الخيشوم.", "النون والميم.", listOf("ن", "م")),
  Sifa("الصفير", "صوت يشبه الصفير يخرج مع الحرف.", "ص ز س.", listOf("س", "ز")),
  Sifa("التفشي", "انتشار الهواء في الفم عند النطق.", "الشين.", listOf("ش")),
  Sifa("الاستطالة", "امتداد الصوت من أول حافة اللسان إلى آخرها.", "الضاد.", listOf("ض")),
  Sifa("التكرير", "ارتعاد طرف اللسان عند النطق بالراء، مع التحرز من تكرارها.", "الراء.", listOf("ر")),
  Sifa("الانحراف", "انحراف اللسان إلى جانبيه عند النطق.", "اللام والراء.", listOf("ل")),
  Sifa("اللين", "خروج الحرف بسهولة، ومنه الواو والياء الساكنتان بعد فتح.", "الواو والياء.", listOf("ي")))
@Composable fun SifatScreen() { var sel by remember { mutableIntStateOf(0) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { sifat.forEachIndexed { i, s -> FilterChip(sel == i, { sel = i }, label = { Text(s.name) }) } }
    val s = sifat[sel]; Text(s.def); Text("حروفها: ${s.letters}", style = MaterialTheme.typography.titleSmall)
    s.reps.forEach { r -> letters.find { it.name == r }?.let { Text("حرف ${it.name}", style = MaterialTheme.typography.titleMedium); MouthAnimation(it) } } } }

// ===== عدّاد المدود =====
data class MaddT(val name: String, val ex: String, val counts: List<Int>, val note: String)
val maddTypes = listOf(MaddT("الطبيعي", "قَالُوا", listOf(2), "حركتان"), MaddT("المتصل", "جَآءَ", listOf(4, 5), "واجب: 4 أو 5 وصلًا"), MaddT("المنفصل", "يَٰٓأَيُّهَا", listOf(4, 5), "جائز: 4 أو 5 في حفص"),
  MaddT("اللازم", "ٱلضَّآلِّينَ", listOf(6), "6 حركات"), MaddT("العارض للسكون", "نَسْتَعِينُ (وقفًا)", listOf(2, 4, 6), "عند الوقف"), MaddT("اللين", "خَوْفٍ (وقفًا)", listOf(2, 4, 6), "عند الوقف"), MaddT("البدل", "ءَامَنُوا۟", listOf(2), "حركتان"))
@Composable fun MaddAnim() { var sel by remember { mutableIntStateOf(0) }; var ci by remember(sel) { mutableIntStateOf(0) }; var beat by remember { mutableFloatStateOf(.6f) }; var sound by remember { mutableStateOf(false) }; var cur by remember { mutableIntStateOf(0) }
  val mt = maddTypes[sel]; val n = mt.counts[ci.coerceAtMost(mt.counts.lastIndex)]
  val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }; DisposableEffect(Unit) { onDispose { tone.release() } }
  LaunchedEffect(sel, ci, n, beat, sound) { while (true) { for (b in 1..n) { cur = b; if (sound) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 80); delay((beat * 1000).toLong()) }; cur = 0; delay(700) } }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { maddTypes.forEachIndexed { i, m -> FilterChip(sel == i, { sel = i }, label = { Text(m.name) }) } }
    Text(mt.ex, fontSize = 34.sp); Text("${mt.note} — مد الحرف مع العدّ بصوتك: ${n} حركات")
    if (mt.counts.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { mt.counts.forEachIndexed { i, c -> if (ci == i) Button({ ci = i }) { Text("$c") } else OutlinedButton({ ci = i }) { Text("$c") } } }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { for (i in 1..n) Box(Modifier.weight(1f).height(56.dp).background(if (cur >= i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("$i", color = if (cur >= i) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) } }
    Text("زمن الحركة الواحدة: ${"%.2f".format(beat)} ثانية"); Slider(beat, { beat = it }, valueRange = .3f..1.0f)
    Row(verticalAlignment = Alignment.CenterVertically) { Text("نبضة صوتية مع كل حركة"); Spacer(Modifier.weight(1f)); Switch(sound, { sound = it }) } } }

// ===== أحكام النون الساكنة بالرسوم =====
data class NoonK(val name: String, val ex: String, val second: String, val letters: String, val note: String)
val noonKinds = listOf(NoonK("الإظهار", "مَنْ ءَامَنَ", "ءَ", "ء هـ ع ح غ خ", "تُنطق النون واضحة من مخرجها بلا غنة"), NoonK("إدغام بغنة", "مَن يَقُولُ", "يَ", "ي ن م و", "تذوب النون في الحرف الذي بعدها مع غنة"),
  NoonK("إدغام بغير غنة", "مِن رَّبِّهِمْ", "رَّ", "ل ر", "تذوب النون في الحرف بلا غنة"), NoonK("الإقلاب", "مِن بَعْدِ", "بَ", "ب", "تُقلب النون ميمًا مخفاة مع غنة"),
  NoonK("الإخفاء", "مِن قَبْلِ", "قَ", "ص ذ ث ك ج ش ق س د ط ز ف ت ض ظ", "حالة بين الإظهار والإدغام مع غنة"))
@Composable fun NoonAnim() { var k by remember { mutableIntStateOf(0) }; val clock = rememberClock(3f); val p = clock.floatValue; val q = ((p - .25f) / .45f).coerceIn(0f, 1f); val dist = with(LocalDensity.current) { 96.dp.toPx() }
  val nk = noonKinds[k]
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { noonKinds.forEachIndexed { i, n -> FilterChip(k == i, { k = i }, label = { Text(n.name) }) } }
    Text(nk.ex, fontSize = 30.sp)
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.graphicsLayer { translationX = -dist * (when (k) { 1, 2 -> q; 4 -> .5f * q; else -> 0f }); alpha = when (k) { 1, 2 -> 1f - q; 4 -> 1f - .5f * q; else -> 1f } }) {
          Text("نْ", fontSize = 56.sp, modifier = Modifier.graphicsLayer { alpha = if (k == 3) 1f - q else 1f }); if (k == 3) Text("مْ", fontSize = 56.sp, color = Color(0xFFE53935), modifier = Modifier.graphicsLayer { alpha = q }) }
        Text(nk.second, fontSize = 56.sp, modifier = Modifier.graphicsLayer { val s = if (k == 1) 1f + .25f * q else 1f; scaleX = s; scaleY = s }) }
      if (k == 1 || k == 3 || k == 4) Text("〰 غنّة 〰", color = Color(0xFF1E88E5), modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = q }) }
    Text(nk.note, style = MaterialTheme.typography.titleSmall); Text("حروف الحكم: ${nk.letters}") } }
