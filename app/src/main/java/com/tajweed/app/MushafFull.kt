package com.tajweed.app
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

const val DEFAULT_BASE = "https://api.quran.com/api/v4"
val SRC_BASE = stringPreferencesKey("src_base"); val SRC_ID = stringPreferencesKey("src_id"); val SRC_TOKEN = stringPreferencesKey("src_token")
data class SrcCfg(val base: String, val id: String, val token: String)

object QuranSource {
  suspend fun cfg(c: Context): SrcCfg { val p = c.store.data.first(); return SrcCfg(p[SRC_BASE] ?: DEFAULT_BASE, p[SRC_ID] ?: "", p[SRC_TOKEN] ?: "") }
  suspend fun fetchSurah(s: Int, cfg: SrcCfg): List<AyahText> = withContext(Dispatchers.IO) {
    val con = URL("${cfg.base.trimEnd('/')}/quran/verses/uthmani_tajweed?chapter_number=$s").openConnection() as HttpURLConnection
    con.connectTimeout = 15000; con.readTimeout = 30000; con.setRequestProperty("Accept", "application/json")
    if (cfg.token.isNotBlank()) { con.setRequestProperty("x-auth-token", cfg.token); con.setRequestProperty("x-client-id", cfg.id) }
    if (con.responseCode != 200) throw IllegalStateException("HTTP ${con.responseCode}")
    parse(con.inputStream.bufferedReader().use { it.readText() }) }
  /** يقبل {"verses":[...]} أو [...] وكل عنصر فيه verse_key و text_uthmani_tajweed */
  fun parse(json: String): List<AyahText> { val t = json.trim(); val arr = if (t.startsWith("[")) JSONArray(t) else JSONObject(t).getJSONArray("verses")
    return (0 until arr.length()).mapNotNull { i -> val o = arr.getJSONObject(i); val key = o.optString("verse_key"); val p = key.split(":")
      val html = o.optString("text_uthmani_tajweed", o.optString("text_uthmani", "")); if (p.size != 2 || html.isEmpty()) null else AyahText(key, p[0].toInt(), p[1].toInt(), html) } }
}
object QFont { const val URL_TTF = "https://verses.quran.foundation/fonts/quran/hafs/uthmanic_hafs/UthmanicHafs1Ver18.ttf"
  fun file(c: Context) = File(c.filesDir, "fonts/UthmanicHafs.ttf")
  suspend fun download(c: Context) = withContext(Dispatchers.IO) { val f = file(c); f.parentFile!!.mkdirs(); val t = File(f.path + ".part")
    URL(URL_TTF).openStream().use { i -> t.outputStream().use { o -> i.copyTo(o) } }; t.renameTo(f) }
}
@Composable fun rememberQuranFont(ver: Int): FontFamily? { val ctx = LocalContext.current
  return remember(ver) { val f = QFont.file(ctx); if (f.exists()) try { FontFamily(androidx.compose.ui.text.font.Typeface(android.graphics.Typeface.createFromFile(f))) } catch (e: Exception) { null } else null } }

// ===== الأحكام (أصناف quran.com) =====
data class TRule(val name: String, val desc: String, val color: Color)
val tajweedRules = mapOf(
  "ham_wasl" to TRule("همزة وصل", "تُنطق عند الابتداء وتسقط عند الوصل", Color(0xFFAAAAAA)),
  "slnt" to TRule("حرف لا يُنطق", "يُكتب ولا يُلفظ", Color(0xFFAAAAAA)),
  "laam_shamsiyah" to TRule("لام شمسية", "لا تُنطق وتُدغم في الحرف الذي بعدها", Color(0xFFAAAAAA)),
  "madda_normal" to TRule("مد طبيعي", "حركتان", Color(0xFF537FFF)),
  "madda_permissible" to TRule("مد جائز", "منفصل أو عارض للسكون أو لين: 2 أو 4 أو 5 أو 6 بحسب نوعه", Color(0xFF5BA3FF)),
  "madda_necessary" to TRule("مد لازم", "ست حركات", Color(0xFF000EBC)),
  "madda_obligatory" to TRule("مد واجب (متصل)", "4 أو 5 حركات", Color(0xFF2144C1)),
  "qalqalah" to TRule("قلقلة", "اضطراب المخرج عند سكون حرف من (قطب جد)", Color(0xFFDD0008)),
  "ikhfa_shafawi" to TRule("إخفاء شفوي", "ميم ساكنة قبل الباء مع غنة", Color(0xFFD500B7)),
  "ikhfa" to TRule("إخفاء حقيقي", "نون ساكنة أو تنوين قبل أحد حروف الإخفاء مع غنة", Color(0xFF9400D3)),
  "idgham_shafawi" to TRule("إدغام شفوي", "ميم ساكنة بعدها ميم", Color(0xFF58B800)),
  "idgham_ghunnah" to TRule("إدغام بغنة", "نون ساكنة أو تنوين قبل (ي ن م و)", Color(0xFF169200)),
  "idgham_wo_ghunnah" to TRule("إدغام بغير غنة", "نون ساكنة أو تنوين قبل (ل ر)", Color(0xFF1B8F5A)),
  "idgham_mutajanisayn" to TRule("إدغام متجانسين", "حرفان اتفقا مخرجًا واختلفا صفة", Color(0xFFA1A1A1)),
  "idgham_mutaqaribayn" to TRule("إدغام متقاربين", "حرفان تقاربا مخرجًا أو صفة", Color(0xFFA1A1A1)),
  "iqlab" to TRule("إقلاب", "نون ساكنة أو تنوين قبل الباء تُقلب ميمًا مخفاة مع غنة", Color(0xFF26BFFD)),
  "ghunnah" to TRule("غنة", "النون والميم المشددتان، حركتان", Color(0xFFFF7E1E)))
data class Seg(val text: String, val rule: String?)
private val tagRe = Regex("<(/?)(tajweed|span)([^>]*)>"); private val classRe = Regex("class=\"?([A-Za-z_]+)")
fun parseTajweed(html: String): List<Seg> { val out = mutableListOf<Seg>(); val stack = ArrayList<String>(); var inEnd = false; var last = 0
  fun emit(t: String) { if (t.isNotEmpty() && !inEnd) out += Seg(t, stack.lastOrNull()?.takeIf { it.isNotEmpty() }) }
  for (m in tagRe.findAll(html)) { emit(html.substring(last, m.range.first)); last = m.range.last + 1
    val closing = m.groupValues[1] == "/"; val attrs = m.groupValues[3]
    if (m.groupValues[2] == "tajweed") { if (closing) { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) } else stack += (classRe.find(attrs)?.groupValues?.get(1) ?: "") }
    else inEnd = !closing && attrs.contains("end") }
  emit(html.substring(last)); return out }
fun arDigits(n: Int) = n.toString().map { "٠١٢٣٤٥٦٧٨٩"[it - '0'] }.joinToString("")
fun buildChunk(chunk: List<AyahText>, colors: Boolean, hl: Int): AnnotatedString = buildAnnotatedString {
  chunk.forEach { ay -> val start = length
    parseTajweed(ay.html).forEach { seg -> val st = length; append(seg.text)
      if (seg.rule != null) { addStringAnnotation("rule", seg.rule, st, length); val c = if (colors) tajweedRules[seg.rule]?.color else null; if (c != null) addStyle(SpanStyle(color = c), st, length) } }
    append(" ﴿${arDigits(ay.ayah)}﴾ "); addStringAnnotation("ayah", ay.ayah.toString(), start, length)
    if (ay.ayah == hl) addStyle(SpanStyle(background = Color(0x66FFC107)), start, length) } }

data class Sel(val ayah: Int, val rule: String?, val text: String)
@Composable fun ChunkText(chunk: List<AyahText>, size: Float, colors: Boolean, hl: Int, font: FontFamily?, onTap: (Sel) -> Unit) {
  val ann = remember(chunk, colors, hl) { buildChunk(chunk, colors, hl) }; var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
  Text(ann, fontSize = size.sp, lineHeight = (size * 1.9f).sp, fontFamily = font, textAlign = TextAlign.Justify, onTextLayout = { layout = it },
    modifier = Modifier.fillMaxWidth().pointerInput(ann) { detectTapGestures { pos -> val l = layout ?: return@detectTapGestures; val off = l.getOffsetForPosition(pos)
      val a = ann.getStringAnnotations("ayah", off, off).firstOrNull()?.item?.toIntOrNull() ?: return@detectTapGestures
      val r = ann.getStringAnnotations("rule", off, off).firstOrNull(); onTap(Sel(a, r?.item, r?.let { ann.text.substring(it.start, it.end) } ?: "")) } }) }

@Composable fun SurahReader(s: Int, rec: Recitation, db: AppDb, font: FontFamily?, onLesson: () -> Unit, onBack: () -> Unit) {
  val sc = rememberCoroutineScope(); val ayat by remember(s) { db.dao().ayat(s) }.collectAsState(emptyList())
  var size by remember { mutableFloatStateOf(28f) }; var colors by remember { mutableStateOf(true) }; var legend by remember { mutableStateOf(false) }; var sel by remember { mutableStateOf<Sel?>(null) }
  val chunks = remember(ayat) { ayat.chunked(8) }; val list = rememberLazyListState(); val cur = rec.current.split(":")
  val hl = if (cur.size == 2 && cur[0].toIntOrNull() == s) cur[1].toIntOrNull() ?: 0 else 0
  LaunchedEffect(hl) { if (hl > 0) { val idx = chunks.indexOfFirst { c -> c.any { it.ayah == hl } }; if (idx >= 0) list.animateScrollToItem(idx + 1) } }
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onBack) { Text("السور") }; Text("سورة ${surahNames[s - 1]}", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.weight(1f))
      Text("تلوين"); Switch(colors, { colors = it }) }
    Row(verticalAlignment = Alignment.CenterVertically) { Text("الحجم"); Slider(size, { size = it }, Modifier.weight(1f), valueRange = 20f..48f); TextButton({ legend = !legend }) { Text(if (legend) "إخفاء الألوان" else "دليل الألوان") } }
    if (legend) tajweedRules.values.distinctBy { it.name }.forEach { Row { Text("● ", color = it.color); Text("${it.name}: ${it.desc}", style = MaterialTheme.typography.bodySmall) } }
    LazyColumn(state = list, modifier = Modifier.weight(1f, fill = false)) {
      item { if (s != 1 && s != 9) Text("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", Modifier.fillMaxWidth().padding(8.dp), fontSize = (size + 2).sp, fontFamily = font, textAlign = TextAlign.Center) else Spacer(Modifier.height(4.dp)) }
      items(chunks.size) { i -> ChunkText(chunks[i], size, colors, hl, font) { sel = it } }
      item { Text("نص المصحف والأحكام وخط KFGQPC Uthmanic Hafs: من Quran Foundation / مجمع الملك فهد.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp)) } } }
  sel?.let { x -> val r = x.rule?.let { tajweedRules[it] }
    AlertDialog({ sel = null }, title = { Text(r?.name ?: "الآية ${x.ayah}") },
      text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (r != null) { Text(r.desc); Text("الموضع: ${x.text}") } else Text("اضغط على كلمة ملونة لمعرفة حكمها.")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { TextButton({ rec.play(s, x.ayah, x.ayah, 1, 1f) }) { Text("استمع") }; TextButton({ rec.play(s, x.ayah, x.ayah, 3, 1f) }) { Text("كرر 3") } }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { TextButton({ rec.play(s, x.ayah, ayahCount[s - 1], 1, 1f); sel = null }) { Text("تلاوة من هنا") }; TextButton({ sc.launch { db.dao().save(Bookmark("$s:${x.ayah}", s, x.ayah)) } }) { Text("علامة") } }
        if (r != null) TextButton({ sel = null; onLesson() }) { Text("إلى الدروس") } } },
      confirmButton = { TextButton({ sel = null }) { Text("إغلاق") } }) }
}

@Composable fun FullMushaf(rec: Recitation, db: AppDb, onLesson: () -> Unit) {
  val ctx = LocalContext.current; val sc = rememberCoroutineScope(); val have by db.dao().downloadedSurahs().collectAsState(emptyList())
  var open by remember { mutableStateOf<Int?>(null) }; var sample by remember { mutableStateOf(false) }; var fontVer by remember { mutableIntStateOf(0) }; val font = rememberQuranFont(fontVer)
  var busy by remember { mutableStateOf(false) }; var prog by remember { mutableIntStateOf(0) }; var msg by remember { mutableStateOf("") }; var adv by remember { mutableStateOf(false) }
  var base by remember { mutableStateOf(DEFAULT_BASE) }; var cid by remember { mutableStateOf("") }; var tok by remember { mutableStateOf("") }
  LaunchedEffect(Unit) { val c = QuranSource.cfg(ctx); base = c.base; cid = c.id; tok = c.token }
  val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) sc.launch {
    try { val txt = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() } }; val l = QuranSource.parse(txt); db.dao().saveAyat(l); msg = "تم استيراد ${l.size} آية" }
    catch (e: Exception) { msg = "ملف غير صالح: يجب أن يحوي verse_key و text_uthmani_tajweed" } } }
  val o = open
  if (sample) Column { TextButton({ sample = false }) { Text("رجوع") }; MushafScreen(rec, db, onLesson) }
  else if (o != null) SurahReader(o, rec, db, font, onLesson) { open = null }
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    item { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text("المصحف الكامل بالتلوين", style = MaterialTheme.typography.titleLarge); Text("السور المحمّلة: ${have.size} من 114")
      if (busy) LinearProgressIndicator(progress = { prog / 114f }, Modifier.fillMaxWidth()); if (msg.isNotEmpty()) Text(msg)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button({ if (!busy) { busy = true; msg = ""; sc.launch { try { val cfg = QuranSource.cfg(ctx); for (n in 1..114) { if (n !in have) { db.dao().saveAyat(QuranSource.fetchSurah(n, cfg)); delay(150) }; prog = n }; msg = "اكتمل تحميل المصحف" }
          catch (e: Exception) { msg = "تعذّر التحميل (${e.message ?: ""}). غيّر المصدر أو استورد ملفًا." }; busy = false } } }) { Text("تحميل كل السور") }
        OutlinedButton({ pick.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("استيراد ملف") } }
      if (!QFont.file(ctx).exists()) Button({ if (!busy) { busy = true; sc.launch { try { QFont.download(ctx); fontVer++; msg = "تم تحميل الخط العثماني" } catch (e: Exception) { msg = "تعذّر تحميل الخط" }; busy = false } } }) { Text("تحميل الخط العثماني (مجمع الملك فهد)") } else Text("الخط العثماني جاهز ✓")
      TextButton({ adv = !adv }) { Text(if (adv) "إخفاء إعدادات المصدر" else "إعدادات مصدر البيانات") }
      if (adv) { OutlinedTextField(base, { base = it }, Modifier.fillMaxWidth(), label = { Text("عنوان الواجهة (Base URL)") }, singleLine = true)
        OutlinedTextField(cid, { cid = it }, Modifier.fillMaxWidth(), label = { Text("x-client-id (اختياري)") }, singleLine = true)
        OutlinedTextField(tok, { tok = it }, Modifier.fillMaxWidth(), label = { Text("x-auth-token (اختياري)") }, singleLine = true)
        Button({ sc.launch { ctx.store.edit { it[SRC_BASE] = base.trim(); it[SRC_ID] = cid.trim(); it[SRC_TOKEN] = tok.trim() }; msg = "حُفظت الإعدادات" } }) { Text("حفظ") } }
      TextButton({ sample = true }) { Text("عيّنة تجريبية بلا إنترنت") } } }
    items(114) { i -> val n = i + 1
      ListItem(headlineContent = { Text("$n. ${surahNames[i]}") }, supportingContent = { Text("${ayahCount[i]} آية") }, trailingContent = { Text(if (n in have) "✓" else "⬇") },
        modifier = Modifier.clickable { if (n in have) open = n else if (!busy) { busy = true; sc.launch { try { db.dao().saveAyat(QuranSource.fetchSurah(n, QuranSource.cfg(ctx))); open = n } catch (e: Exception) { msg = "تعذّر تحميل السورة (${e.message ?: ""})" }; busy = false } } }) } }
}
