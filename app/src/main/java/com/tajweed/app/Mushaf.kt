package com.tajweed.app
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch

data class W(val t: String, val rule: String = "", val why: String = "")
private fun w(t: String, r: String = "", y: String = "") = W(t, r, y)
// عينة: سورة الفاتحة. تحتاج مراجعة علمية قبل النشر، ويمكن استبدالها بقاعدة بيانات كاملة لاحقًا.
val fatiha = listOf(
  listOf(w("بِسْمِ"), w("ٱللَّهِ", "ترقيق لام الجلالة", "وقعت بعد كسر (ميم بسم) فتُرقَّق"), w("ٱلرَّحْمَٰنِ", "لام شمسية", "اللام بعد «ال» تُدغم في الراء ولا تُنطق"), w("ٱلرَّحِيمِ", "لام شمسية", "تُدغم لام «ال» في الراء")),
  listOf(w("ٱلْحَمْدُ"), w("لِلَّهِ", "ترقيق لام الجلالة", "جاءت بعد كسرة اللام التي قبلها"), w("رَبِّ"), w("ٱلْعَٰلَمِينَ", "لام قمرية ومد طبيعي", "اللام تُظهر مع العين، والألف بعد العين مد طبيعي حركتان")),
  listOf(w("ٱلرَّحْمَٰنِ", "لام شمسية", "تُدغم لام «ال» في الراء"), w("ٱلرَّحِيمِ", "لام شمسية", "تُدغم لام «ال» في الراء")),
  listOf(w("مَٰلِكِ", "مد طبيعي", "ألف المد بعد الميم المفتوحة، حركتان"), w("يَوْمِ"), w("ٱلدِّينِ", "لام شمسية ومد عارض عند الوقف", "تُدغم لام «ال» في الدال، وعند الوقف يجوز مد الياء 2 أو 4 أو 6")),
  listOf(w("إِيَّاكَ"), w("نَعْبُدُ"), w("وَإِيَّاكَ"), w("نَسْتَعِينُ", "مد عارض للسكون عند الوقف", "سكون الوقف بعد ياء المد: 2 أو 4 أو 6")),
  listOf(w("ٱهْدِنَا"), w("ٱلصِّرَٰطَ", "لام شمسية وتفخيم الصاد", "تُدغم اللام في الصاد، والصاد حرف استعلاء وإطباق فتُفخَّم"), w("ٱلْمُسْتَقِيمَ", "لام قمرية", "اللام تُظهر مع الميم")),
  listOf(w("صِرَٰطَ"), w("ٱلَّذِينَ", "لام شمسية", "تُدغم اللام في الذال"), w("أَنْعَمْتَ", "إظهار حلقي", "نون ساكنة بعدها عين (من حروف الحلق) فتُظهر بلا غنة"), w("عَلَيْهِمْ", "إظهار شفوي", "ميم ساكنة بعدها غين فتُظهر"), w("غَيْرِ"), w("ٱلْمَغْضُوبِ", "لام قمرية ومد طبيعي", "اللام تُظهر مع الميم، والواو مد طبيعي"), w("عَلَيْهِمْ", "إظهار شفوي", "ميم ساكنة بعدها واو فتُظهر"), w("وَلَا"), w("ٱلضَّآلِّينَ", "مد لازم كلمي مثقل (6 حركات)", "جاء بعد حرف المد سكون أصلي في كلمة واحدة، وهو اللام المشددة")))
val ikhlas = listOf(
  listOf(w("قُلْ"), w("هُوَ"), w("ٱللَّهُ"), w("أَحَدٌ", "قلقلة كبرى عند الوقف", "الدال ساكنة عند الوقف وهي من حروف القلقلة (قطب جد)")),
  listOf(w("ٱللَّهُ"), w("ٱلصَّمَدُ", "لام شمسية وقلقلة عند الوقف", "تُدغم اللام في الصاد، والدال قلقلة كبرى عند الوقف")),
  listOf(w("لَمْ"), w("يَلِدْ", "قلقلة صغرى", "الدال ساكنة وسط الكلام"), w("وَلَمْ"), w("يُولَدْ", "مد طبيعي وقلقلة", "الواو بعد ضمة مد طبيعي، والدال ساكنة قلقلة")),
  listOf(w("وَلَمْ"), w("يَكُن", "إدغام بغير غنة", "نون ساكنة بعدها لام فتُدغم بلا غنة"), w("لَّهُۥ", "مد صلة صغرى", "هاء الضمير بين متحركين فتوصل بواو حركتين"), w("كُفُوًا"), w("أَحَدٌۢ", "قلقلة كبرى عند الوقف", "الدال حرف قلقلة ساكن عند الوقف")))
val kawthar = listOf(
  listOf(w("إِنَّآ", "مد منفصل", "ألف المد آخر الكلمة والهمزة أول الكلمة التالية"), w("أَعْطَيْنَٰكَ", "مد طبيعي", "الألف بعد النون المفتوحة، حركتان"), w("ٱلْكَوْثَرَ", "لام قمرية", "اللام تُظهر مع الكاف")),
  listOf(w("فَصَلِّ"), w("لِرَبِّكَ"), w("وَٱنْحَرْ", "إظهار حلقي", "نون ساكنة بعدها حاء من حروف الحلق")),
  listOf(w("إِنَّ"), w("شَانِئَكَ", "مد متصل", "حرف المد والهمزة في كلمة واحدة"), w("هُوَ"), w("ٱلْأَبْتَرُ", "لام قمرية", "اللام تُظهر مع الهمزة")))
val asr = listOf(
  listOf(w("وَٱلْعَصْرِ", "لام قمرية", "اللام تُظهر مع العين")),
  listOf(w("إِنَّ"), w("ٱلْإِنسَٰنَ", "إخفاء حقيقي", "نون ساكنة بعدها سين من حروف الإخفاء"), w("لَفِى"), w("خُسْرٍ", "إظهار حلقي", "تنوين بعده همزة في «إلا»")),
  listOf(w("إِلَّا"), w("ٱلَّذِينَ", "لام شمسية", "تُدغم اللام في الذال"), w("ءَامَنُوا۟", "مد بدل", "همزة ثم حرف مد في كلمة، حركتان"), w("وَعَمِلُوا۟"), w("ٱلصَّٰلِحَٰتِ", "لام شمسية ومد طبيعي", "تُدغم اللام في الصاد، وألف بعد اللام مد طبيعي"), w("وَتَوَاصَوْا۟"), w("بِٱلْحَقِّ", "لام قمرية", "اللام تُظهر مع الحاء"), w("وَتَوَاصَوْا۟"), w("بِٱلصَّبْرِ", "لام شمسية", "تُدغم اللام في الصاد")))
val mushafData = mapOf(1 to fatiha, 103 to asr, 108 to kawthar, 112 to ikhlas)
fun colorOf(r: String) = when { "مد" in r -> Color(0xFFD32F2F); "إظهار" in r -> Color(0xFF1976D2); "ترقيق" in r || "تفخيم" in r -> Color(0xFF7B1FA2); r.isNotEmpty() -> Color(0xFF2E7D32); else -> Color.Unspecified }

@OptIn(ExperimentalLayoutApi::class)
@Composable fun MushafScreen(rec: Recitation, db: AppDb, onLesson: () -> Unit) {
  var sel by remember { mutableStateOf<Pair<Int, W>?>(null) }; var size by remember { mutableFloatStateOf(26f) }; var cur by remember { mutableIntStateOf(1) }; val sc = rememberCoroutineScope()
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { mushafData.keys.forEach { k -> if (cur == k) Button({ cur = k }) { Text(surahNames[k - 1]) } else OutlinedButton({ cur = k }) { Text(surahNames[k - 1]) } } }; Text("عينات تجريبية تحتاج مراجعة علمية"); Text("حجم الخط"); Slider(size, { size = it }, valueRange = 18f..44f)
    mushafData[cur]!!.forEachIndexed { i, ay -> Text("آية ${i + 1}", style = MaterialTheme.typography.labelSmall)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ay.forEach { x ->
        Text(x.t, fontSize = size.sp, color = colorOf(x.rule), modifier = Modifier.clickable { sel = (i + 1) to x }) } } }
    Text("أحمر: مد | أزرق: إظهار | بنفسجي: ترقيق/تفخيم | أخضر: غير ذلك. اضغط على كلمة ملونة.")
  }
  sel?.let { (a, x) -> val ctx = LocalContext.current
    AlertDialog(onDismissRequest = { sel = null }, title = { Text(x.t) },
      text = { Text(if (x.rule.isEmpty()) "لا حكم تجويدي خاص بهذه الكلمة في هذا الموضع." else "الحكم: ${x.rule}\n\nالسبب: ${x.why}") },
      confirmButton = { Row { TextButton({ rec.play(cur, a, a, 1, 1f) }) { Text("استمع") }
        TextButton({ sc.launch { db.dao().save(Bookmark("$cur:$a", cur, a)) } }) { Text("علامة") }
        TextButton({ sel = null; onLesson() }) { Text("الدرس") } } },
      dismissButton = { TextButton({ sel = null }) { Text("إغلاق") } }) }
}
@Composable fun DownloadScreen() { val ctx = LocalContext.current; val sc = rememberCoroutineScope()
  var s by remember { mutableIntStateOf(114) }; var prog by remember { mutableIntStateOf(0) }; var busy by remember { mutableStateOf(false) }; var msg by remember { mutableStateOf("") }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("تحميل للاستماع دون إنترنت", style = MaterialTheme.typography.titleLarge)
    Row { OutlinedButton({ if (s > 1) s-- }) { Text("-") }; Text("  ${surahNames[s - 1]} (${ayahCount[s - 1]} آية)  "); OutlinedButton({ if (s < 114) s++ }) { Text("+") } }
    Text(if (Offline.done(ctx, s)) "محمّلة ✓" else "غير محمّلة"); if (busy) LinearProgressIndicator(progress = { prog.toFloat() / ayahCount[s - 1] })
    Text(msg)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button({ if (!busy) { busy = true; msg = ""; sc.launch { try { Offline.download(ctx, s) { prog = it }; msg = "اكتمل التحميل" } catch (e: Exception) { msg = "تعذّر التحميل: تحقق من الإنترنت" }; busy = false } } }) { Text("تحميل") }
      OutlinedButton({ Offline.delete(ctx, s); msg = "حُذفت" }) { Text("حذف") } } }
}
@Composable fun MushafHub(rec: Recitation, db: AppDb, onLesson: () -> Unit) { var m by remember { mutableIntStateOf(0) }
  Column { TabRow(m) { listOf("المشغل", "المصحف", "التحميل").forEachIndexed { i, t -> Tab(m == i, { m = i }, text = { Text(t) }) } }
    Spacer(Modifier.height(8.dp)); when (m) { 0 -> Quran(rec); 1 -> FullMushaf(rec, db, onLesson); else -> DownloadScreen() } }
}
