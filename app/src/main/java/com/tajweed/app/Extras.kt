package com.tajweed.app
import kotlinx.coroutines.launch
import android.content.Intent
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

val pairs = listOf(Triple("س / ص", "السين مرققة همس رخاوة استفال، والصاد مفخمة استعلاء إطباق، ومخرجهما واحد", ""),
  Triple("ذ / ز", "الذال من طرف اللسان مع أطراف الثنايا العليا، والزاي من طرفه مع الثنايا السفلى وفيها صفير", ""),
  Triple("ض / ظ", "الضاد من حافة اللسان وفيها استطالة، والظاء من طرف اللسان مع أطراف الثنايا العليا", ""),
  Triple("ت / ط", "التاء مرققة مهموسة، والطاء مفخمة مجهورة مطبقة قلقلة", ""))
val waqf = listOf("الوقف التام" to "يتم المعنى ولا يتعلق بما بعده لفظًا ولا معنى، كنهاية قصة", "الوقف الكافي" to "يتم المعنى ويتعلق بما بعده معنى لا لفظًا",
  "الوقف الحسن" to "يتم الكلام في نفسه لكنه متعلق بما بعده لفظًا ومعنى، فيحسن الوقف ولا يحسن الابتداء بما بعده", "الوقف القبيح" to "لا يتم به المعنى، كالوقف على المضاف دون المضاف إليه، ولا يوقف عليه إلا لضرورة",
  "علامات الوقف" to "م: لازم، لا: ممنوع، ج: جائز، قلى: الوقف أولى، صلى: الوصل أولى، ∴ معانقة", "الابتداء" to "ابدأ بما يتم به المعنى ولا توهم خلاف المراد")
val mistakes = listOf("تضييع الغنة أو زيادتها في غير موضعها", "قلب الضاد ظاءً", "ترك القلقلة أو المبالغة فيها", "قصر المد المنفصل واللازم", "ترقيق الراء المفخمة أو العكس", "إظهار الإخفاء وإخفاء الإظهار", "إطباق الشفتين بقوة عند الإخفاء الشفوي", "الوقف على متحرك دون سكون")
val shortSurahs = listOf(1, 114, 113, 112, 111, 110, 109, 108)

@Composable fun HomeHub(db: AppDb, rec: Recitation, onPractice: (String) -> Unit) {
  var sub by remember { mutableStateOf<String?>(null) }; val sc = rememberCoroutineScope()
  val items = listOf("compare" to "مقارنة الحروف", "mistakes" to "الأخطاء الشائعة", "waqf" to "الوقف والابتداء", "videos" to "الفيديوهات", "favs" to "المفضلة", "speed" to "مراتب القراءة", "path" to "تجويد سورة بسورة", "sifat" to "رسوم الصفات", "madd" to "عدّاد المدود", "noon" to "رسوم النون الساكنة")
  if (sub == null) Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { Home(db)
    items.chunked(2).forEach { r -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { r.forEach { (k, t) -> Button({ sub = k }, Modifier.weight(1f)) { Text(t) } } } } }
  else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    TextButton({ sub = null }) { Text("رجوع") }
    when (sub) {
      "compare" -> pairs.forEach { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(it.first, style = MaterialTheme.typography.titleMedium); Text(it.second) } } }
      "mistakes" -> mistakes.forEach { Text("• $it") }
      "waqf" -> waqf.forEach { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(it.first, style = MaterialTheme.typography.titleMedium); Text(it.second) } } }
      "videos" -> VideoScreen(onPractice)
      "sifat" -> SifatScreen(); "madd" -> MaddAnim(); "noon" -> NoonAnim()
      "favs" -> { val b by db.dao().bookmarks().collectAsState(emptyList())
        if (b.isEmpty()) Text("لا مفضلة بعد")
        b.forEach { x -> Row { OutlinedButton({ rec.play(x.surah, x.ayah, x.ayah, 1, 1f) }, Modifier.weight(1f)) { Text("${surahNames[x.surah - 1]} : ${x.ayah}") }; TextButton({ sc.launch { db.dao().delBookmark(x.key) } }) { Text("حذف") } } } }
      "speed" -> { Text("مراتب القراءة (على الفاتحة)"); listOf("التحقيق" to .8f, "التدوير" to 1f, "الحدر" to 1.3f).forEach { (n, s) -> Button({ rec.play(1, 2, 7, 1, s) }, Modifier.fillMaxWidth()) { Text("$n  ${s}x") } } }
      else -> shortSurahs.forEach { s -> OutlinedButton({ rec.play(s, 1, ayahCount[s - 1], 1, 1f) }, Modifier.fillMaxWidth()) { Text("سورة ${surahNames[s - 1]}") } }
    } }
}
val lessonText = mapOf(
  "صفات الحروف" to "لها ضد: الهمس (فحثه شخص سكت) ضده الجهر، الشدة (أجد قط بكت) والتوسط (لن عمر) والباقي رخاوة، الاستعلاء (خص ضغط قظ) ضده الاستفال، الإطباق (ص ض ط ظ) ضده الانفتاح، الإذلاق (فر من لب) ضده الإصمات.\nبلا ضد: القلقلة، الصفير، اللين، الانحراف، التكرير، التفشي، الاستطالة، الغنة.",
  "اللين والبدل والصلة والعوض" to "مد اللين: واو أو ياء ساكنة بعد فتح عند الوقف بسكون عارض.\nمد البدل: همزة ثم حرف مد، حركتان، مثل «آمنوا».\nمد الصلة الصغرى: هاء الضمير بين متحركين، حركتان.\nمد العوض: تنوين الفتح عند الوقف يُبدل ألفًا، حركتان.",
  "التفخيم والترقيق" to "حروف الاستعلاء السبعة (خص ضغط قظ) مفخمة دائمًا، وتُخفف إذا كُسرت. بقية الحروف مرققة إلا الراء ولام الجلالة فلهما أحكام بحسب الحركة وما قبلهما.",
  "المتماثلان والمتجانسان" to "المتماثلان: اتفقا مخرجًا وصفة، مثل: اضرب بعصاك (باءان). المتجانسان: اتفقا مخرجًا واختلفا صفة، مثل: قد تبين (دال وتاء). المتقاربان: تقاربا مخرجًا أو صفة. ويسمى صغيرًا إذا كان الأول ساكنًا، وكبيرًا إذا تحرك.",
  "الاستعاذة والبسملة" to "الاستعاذة: أعوذ بالله من الشيطان الرجيم. في أول السورة (غير براءة) أربعة أوجه: قطع الجميع، قطع الاستعاذة ووصل البسملة بأول السورة، وصل الجميع، وصل الاستعاذة بالبسملة وقطعها عن أول السورة.",
  "الوقف والابتداء" to "الوقف التام: يتم المعنى ولا يتعلق بما بعده. الكافي: يتعلق معنى لا لفظًا. الحسن: يتعلق لفظًا ومعنى. القبيح: لا يتم به المعنى. علامات المصحف: م لازم، لا ممنوع، ج جائز، قلى الوقف أولى، صلى الوصل أولى.",
  "السكتات في حفص" to "أربع سكتات بلا تنفس: بعد «عوجا» في الكهف، وبعد «مرقدنا» في يس، وبعد «من» في «من راق» بالقيامة، وبعد «بل» في «بل ران» بالمطففين.",
  "المتشابهات الصوتية" to "افتح «مقارنة الحروف» من الرئيسية: س/ص، ذ/ز، ض/ظ، ت/ط، وتدرب بالتسجيل ثم قارن مع صوت الحصري.",
  "التلاوة المتقدمة" to "طبّق الأحكام على ما تحفظ من سور، وسجّل تلاوتك وقارنها بتلاوة الحصري آية بآية، وكرر المقطع الذي أخطأت فيه من مشغل المصحف.",
  "أحكام النون الساكنة والتنوين" to "الإظهار: عند حروف الحلق (ء هـ ع ح غ خ).\nالإدغام: بغنة عند (ي ن م و)، وبغير غنة عند (ل ر).\nالإقلاب: عند الباء، تُقلب ميمًا مخفاة مع غنة.\nالإخفاء: عند باقي الحروف الخمسة عشر.",
  "أحكام الميم الساكنة" to "الإخفاء الشفوي: عند الباء.\nالإدغام الشفوي: عند ميم مثلها.\nالإظهار الشفوي: عند باقي الحروف.",
  "المد الطبيعي" to "مد بلا سبب همز أو سكون، مقداره حركتان، وحروفه الألف والواو والياء الساكنات بعد حركة تناسبها.",
  "المتصل والمنفصل" to "المتصل: حرف المد والهمز في كلمة واحدة، يمد 4 أو 5 وصلًا.\nالمنفصل: حرف المد آخر كلمة والهمز أول التي بعدها، يمد 4 أو 5 في رواية حفص.",
  "اللازم والعارض" to "اللازم: السكون أصلي بعد حرف المد، 6 حركات.\nالعارض للسكون: السكون عارض بسبب الوقف، 2 أو 4 أو 6.",
  "مخارج الحروف" to "خمسة مخارج عامة: الجوف، الحلق، اللسان، الشفتان، الخيشوم. افتح تبويب المخارج لكل حرف.",
  "الراء ولام الجلالة" to "لام الجلالة تفخم بعد فتح أو ضم وترقق بعد كسر. الراء تفخم أو ترقق بحسب حركتها وما قبلها.")
@Composable fun LessonsList(db: AppDb) { val sc = rememberCoroutineScope(); val p by db.dao().progress().collectAsState(emptyList()); var open by remember { mutableStateOf<String?>(null) }
  LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) { levels.forEach { lv -> item { Text(lv.title, style = MaterialTheme.typography.titleLarge) }
    items(lv.lessons) { l -> val d = p.any { it.lessonId == l && it.done }
      ListItem(headlineContent = { Text(l) }, modifier = Modifier.clickable { open = l }, trailingContent = { Checkbox(d, { sc.launch { db.dao().save(Progress(l, it)) } }) }) } } }
  open?.let { l -> AlertDialog({ open = null }, title = { Text(l) }, text = { Text(lessonText[l] ?: "محتوى الدرس قيد الإضافة. يمكن تحديثه لاحقًا من قاعدة البيانات.") },
    confirmButton = { TextButton({ open = null }) { Text("إغلاق") } }) }
}

data class Vid(val title: String, val letter: String, val query: String, val links: List<Pair<String, String>>)
private const val Y = "https://www.youtube.com/watch?v="
val videoList = listOf(
  Vid("مخارج الحروف (شرح عام)", "ب", "مخارج الحروف تجويد شرح", listOf("شرح مبسط - تيسير التجويد (صهيب البيومي)" to Y + "OAqgvXEAhAk", "شرح مبسط ومختصر بتقسيم زمني" to Y + "4wHxPUPFl7s", "كل المخارج في 37 دقيقة" to Y + "aWdTMX4xgLU", "المخارج حرفًا حرفًا" to Y + "CerlgvNyQ84")),
  Vid("الجوف وطرف اللسان", "ا", "مخرج الجوف تجويد", listOf("مخرج الجوف للمبتدئين" to Y + "B6nIKle0ddM", "مخرج طرف اللسان" to Y + "U8lXEHFXZPM")),
  Vid("الضاد والظاء", "ض", "مخرج الضاد تجويد", listOf("مخرج الضاد مع طريقة التدريب" to Y + "svjc39MMQU0", "الفرق بين الضاد والظاء" to Y + "pcGTC0HmDAs", "كيف نميز الضاد عن الظاء وندرب اللسان" to Y + "0B-h4Zra5KY")),
  Vid("صفات الحروف", "ط", "صفات الحروف تجويد", listOf("الهمس والجهر والشدة والرخاوة والتوسط" to Y + "Ncz7I0pAq4E", "الصفات التي لها ضد وبلا ضد (أحمد عبد المنعم)" to Y + "F46l_5Ox0O4", "صفات الحروف كاملة" to Y + "n_27zj83iJQ", "الصفات التي لها ضد" to Y + "7qZnLGEe6A4")),
  Vid("أحكام النون الساكنة والتنوين", "ن", "أحكام النون الساكنة والتنوين", listOf("الإظهار والإدغام والإقلاب والإخفاء" to Y + "xwJ2e7qfvzE", "في أقل من عشر دقائق" to Y + "DjKe4Xeeu3c", "شرح كامل مع تدريب" to Y + "bh9xLe73dyI")),
  Vid("أحكام الميم الساكنة", "م", "أحكام الميم الساكنة", listOf("الأحكام الثلاثة مع الرسم المصحفي" to Y + "KKX1Zqag0cY", "في عشر دقائق" to Y + "0S3rQ_fYZmI", "الدرس الخامس - منصة أفنان" to Y + "0OxT_hks_ak")),
  Vid("أنواع المدود", "ا", "أنواع المدود تجويد حفص", listOf("شرح مختصر لكل المدود مع تنبيهات" to Y + "H62qyyO8ibQ", "المدود كاملة في فيديو واحد" to Y + "I5U33u_fkVY", "المدود للمبتدئين (أحمد البربري)" to Y + "zNb099XZ130", "المدود من كتاب البرهان" to Y + "CgzkU6iB3Vg")),
  Vid("الوقف والابتداء", "ا", "الوقف والابتداء تجويد", listOf("الوقف من كتاب البرهان" to Y + "aAd1iFhTX6s", "التام والكافي والحسن والقبيح بالتطبيق (حفص)" to Y + "cEr55mTsYVc", "الوقف الحسن والقبيح مع الأمثلة" to Y + "tRrVuNpTMPA", "الوقف الجائز: التام والكافي والحسن" to Y + "u3fOS9cK6as")))
@Composable fun VideoScreen(onPractice: (String) -> Unit) { val ctx = LocalContext.current; val sc = rememberCoroutineScope()
  val prefs by ctx.store.data.collectAsState(initial = null); val exo = remember { ExoPlayer.Builder(ctx).build() }
  DisposableEffect(Unit) { onDispose { exo.release() } }; var playing by remember { mutableStateOf(false) }; var sp by remember { mutableFloatStateOf(1f) }
  var ytId by remember { mutableStateOf<String?>(null) }; var curUrl by remember { mutableStateOf("") }; var err by remember { mutableStateOf("") }
  var yt by remember { mutableStateOf<YouTubePlayer?>(null) }; val lo = LocalLifecycleOwner.current
  val ytView = remember { YouTubePlayerView(ctx).also { v -> lo.lifecycle.addObserver(v)
    v.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
      override fun onReady(youTubePlayer: YouTubePlayer) { yt = youTubePlayer }
      override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) { err = "لا يسمح صاحب الفيديو بتشغيله داخل التطبيق أو حدث خطأ في التحميل." } }) } }
  DisposableEffect(Unit) { onDispose { ytView.release() } }
  LaunchedEffect(ytId, yt) { val p = yt; val id = ytId; if (p != null && id != null) p.loadVideo(id, 0f) }
  fun open(u: String) { if (u.endsWith(".mp4")) { exo.setMediaItem(MediaItem.fromUri(u)); exo.prepare(); exo.play(); playing = true }
    else { val id = Regex("v=([\\w-]{11})").find(u)?.groupValues?.get(1)
      if (id != null) { ytId = id; curUrl = u; err = "" } else ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    AndroidView({ ytView }, Modifier.fillMaxWidth().height(if (ytId != null) 220.dp else 0.dp))
    if (ytId != null) { if (err.isNotEmpty()) { Text(err); Button({ ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(curUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("افتح في يوتيوب") } }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("0.5x" to PlayerConstants.PlaybackRate.RATE_0_5, "1x" to PlayerConstants.PlaybackRate.RATE_1, "1.5x" to PlayerConstants.PlaybackRate.RATE_1_5, "2x" to PlayerConstants.PlaybackRate.RATE_2)
          .forEach { (t, r) -> OutlinedButton({ yt?.setPlaybackRate(r) }) { Text(t) } }
        OutlinedButton({ yt?.seekTo(0f); yt?.play() }) { Text("إعادة") } } }
    if (playing) { AndroidView({ PlayerView(it).apply { player = exo } }, Modifier.fillMaxWidth().height(200.dp))
      Text("السرعة ${"%.1f".format(sp)}x"); Slider(sp, { sp = it; exo.playbackParameters = PlaybackParameters(it) }, valueRange = .5f..2f)
      OutlinedButton({ exo.seekTo(0); exo.play() }) { Text("إعادة المقطع") } }
    videoList.forEachIndexed { i, v -> val key = stringPreferencesKey("vid$i"); val saved = prefs?.get(key) ?: ""; var link by remember(saved) { mutableStateOf(saved) }
      Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(v.title, style = MaterialTheme.typography.titleMedium)
        v.links.forEach { (t, u) -> TextButton({ open(u) }) { Text("▶ $t") } }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Button({ open("https://www.youtube.com/results?search_query=" + Uri.encode(v.query)) }) { Text("ابحث في يوتيوب") }
          OutlinedButton({ onPractice(v.letter) }) { Text("تدرب الآن") } }
        OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth(), label = { Text("ألصق رابط الفيديو الذي اخترته") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton({ sc.launch { ctx.store.edit { it[key] = link.trim() } } }) { Text("حفظ") }
          if (saved.isNotEmpty()) Button({ open(saved) }) { Text("شغّل المحفوظ") } } } } } }
}

val STREAK = intPreferencesKey("streak"); val LASTDAY = longPreferencesKey("lastday")
@Composable fun Streak() { val ctx = LocalContext.current; var n by remember { mutableIntStateOf(0) }
  LaunchedEffect(Unit) { ctx.store.edit { val today = System.currentTimeMillis() / 86400000L; val last = it[LASTDAY] ?: 0L; val cur = it[STREAK] ?: 0
    val nv = when (last) { today -> cur; today - 1 -> cur + 1; else -> 1 }; it[LASTDAY] = today; it[STREAK] = nv; n = nv } }
  Text("🔥 أيام المتابعة المتتالية: $n") }
val dailyAyat = listOf("وَلَا ٱلضَّآلِّينَ" to "مد لازم كلمي مثقل (6 حركات)، وتخرج الضاد من حافة اللسان", "أَنْعَمْتَ عَلَيْهِمْ" to "إظهار حلقي في «أنعمت» وإظهار شفوي في «عليهم»",
  "قُلْ هُوَ ٱللَّهُ أَحَدٌ" to "قلقلة كبرى في «أحد» عند الوقف", "إِنَّآ أَعْطَيْنَٰكَ ٱلْكَوْثَرَ" to "مد منفصل في «إنا أعطيناك»", "وَٱلْعَصْرِ" to "لام قمرية تُظهر مع العين",
  "لَمْ يَلِدْ وَلَمْ يُولَدْ" to "قلقلة صغرى في «يلد» و«يولد»، ومد طبيعي في «يولد»", "إِنَّ شَانِئَكَ هُوَ ٱلْأَبْتَرُ" to "مد متصل في «شانئك»")
