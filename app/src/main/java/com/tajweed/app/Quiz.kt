package com.tajweed.app
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class Q(val topic: String, val text: String, val options: List<String>, val answer: Int, val why: String)
val topics = listOf("المخارج", "الصفات", "النون والميم", "المدود", "الوقف")
val quiz = listOf(
  Q("المخارج", "ما مخرج حرف الباء؟", listOf("الشفتان", "الجوف", "أدنى الحلق", "اللسان"), 0, "الباء من انطباق الشفتين."),
  Q("المخارج", "مخرج الفاء:", listOf("الشفتان معًا", "باطن الشفة السفلى مع أطراف الثنايا العليا", "أقصى الحلق", "الجوف"), 1, "الفاء شفوية أسنانية."),
  Q("المخارج", "عدد الحروف الحلقية:", listOf("3", "6", "8", "10"), 1, "ء هـ ع ح غ خ."),
  Q("المخارج", "الهمزة والهاء تخرجان من:", listOf("أقصى الحلق", "وسط الحلق", "أدنى الحلق", "الجوف"), 0, "أبعد الحلق عن الفم."),
  Q("المخارج", "عدد المخارج الخاصة عند الجمهور (ابن الجزري):", listOf("14", "16", "17", "29"), 2, "سبعة عشر مخرجًا في خمسة مواضع عامة."),
  Q("المخارج", "القاف تخرج من:", listOf("أقصى اللسان مع الحنك الأعلى", "طرف اللسان", "الشفتان", "وسط الحلق"), 0, "من أقصى اللسان مما يلي الحلق."),
  Q("المخارج", "حروف الجوف:", listOf("حروف المد: ا و ي", "ء هـ", "ب م", "ض ظ"), 0, "الألف والواو الساكنة بعد ضم والياء الساكنة بعد كسر."),
  Q("المخارج", "الضاد تخرج من:", listOf("حافة اللسان مع الأضراس", "طرف اللسان", "الشفتان", "الحلق"), 0, "من إحدى حافتي اللسان أو كليهما."),
  Q("الصفات", "حروف القلقلة:", listOf("ق ط ب ج د", "ص ض ط ظ", "ف ر م ن ل ب", "ء هـ ع ح غ خ"), 0, "قطب جد."),
  Q("الصفات", "حروف الاستعلاء:", listOf("خص ضغط قظ", "فحثه شخص سكت", "أجد قط بكت", "لن عمر"), 0, "سبعة حروف مفخمة."),
  Q("الصفات", "حروف الهمس:", listOf("خص ضغط قظ", "فحثه شخص سكت", "أجد قط بكت", "لن عمر"), 1, "عشرة حروف يجري معها النفس."),
  Q("الصفات", "حروف الشدة:", listOf("خص ضغط قظ", "فحثه شخص سكت", "أجد قط بكت", "لن عمر"), 2, "ثمانية حروف ينحبس معها الصوت."),
  Q("الصفات", "حروف الإطباق:", listOf("ص ض ط ظ", "س ز ص", "ق ك", "ث ذ ظ"), 0, "الأربعة المطبقة."),
  Q("الصفات", "حروف الصفير:", listOf("ص ز س", "ش ج ي", "ط د ت", "غ خ ع"), 0, "يخرج معها صوت كالصفير."),
  Q("الصفات", "الحرفان اللذان فيهما غنة:", listOf("النون والميم", "الباء والتاء", "الراء واللام", "القاف والكاف"), 0, "الغنة صفة لازمة للنون والميم."),
  Q("الصفات", "الضاد والظاء يختلفان في:", listOf("المخرج", "الجهر", "الاستعلاء", "الإطباق"), 0, "الضاد من حافة اللسان والظاء من طرفه."),
  Q("النون والميم", "ما حكم النون الساكنة قبل الباء؟", listOf("إظهار", "إقلاب", "إدغام بغنة", "إخفاء"), 1, "تُقلب ميمًا مخفاة مع غنة."),
  Q("النون والميم", "حروف الإظهار الحلقي:", listOf("ء هـ ع ح غ خ", "ي ر م ل و ن", "ب فقط", "ق ك"), 0, "ستة حروف حلقية."),
  Q("النون والميم", "حروف الإدغام (يرملون):", listOf("ي ر م ل و ن", "ء هـ ع ح غ خ", "ب", "ق ك"), 0, "ستة حروف."),
  Q("النون والميم", "«مِنْ رَبِّهِمْ»: حكم النون:", listOf("إدغام بغير غنة", "إدغام بغنة", "إخفاء", "إقلاب"), 0, "قبل الراء تُدغم بلا غنة."),
  Q("النون والميم", "«مَنْ يَقُولُ»: حكم النون:", listOf("إدغام بغير غنة", "إدغام بغنة", "إخفاء", "إظهار"), 1, "قبل الياء تُدغم بغنة."),
  Q("النون والميم", "الميم الساكنة قبل الباء:", listOf("إخفاء شفوي", "إدغام شفوي", "إظهار شفوي", "إقلاب"), 0, "تُخفى مع غنة."),
  Q("النون والميم", "«لَهُم مَّا»:", listOf("إدغام شفوي (مثلين صغير)", "إخفاء شفوي", "إظهار شفوي", "إقلاب"), 0, "ميم ساكنة بعدها ميم."),
  Q("النون والميم", "«أَنْعَمْتَ»: النون:", listOf("إظهار حلقي", "إخفاء", "إقلاب", "إدغام"), 0, "بعدها عين."),
  Q("المدود", "المد في «قَالُوا»:", listOf("طبيعي", "متصل", "منفصل", "لازم"), 0, "لا همز ولا سكون بعده."),
  Q("المدود", "«جَاءَ»: المد:", listOf("طبيعي", "متصل", "منفصل", "بدل"), 1, "المد والهمز في كلمة واحدة."),
  Q("المدود", "مقدار المد الطبيعي:", listOf("حركتان", "4 حركات", "6 حركات", "حركة"), 0, "الأصل حركتان."),
  Q("المدود", "مقدار المد اللازم:", listOf("2", "4", "6 حركات", "حركة"), 2, "ست حركات لا غير."),
  Q("المدود", "«وَلَا الضَّالِّينَ»:", listOf("لازم كلمي مثقل", "منفصل", "بدل", "طبيعي"), 0, "سكون أصلي بعد المد."),
  Q("المدود", "«يَا أَيُّهَا»: المد في «يا»:", listOf("منفصل", "متصل", "بدل", "لازم"), 0, "ألف في كلمة والهمز في التي بعدها."),
  Q("المدود", "«آمَنُوا»:", listOf("بدل", "متصل", "لازم", "عارض"), 0, "همزة ثم حرف مد."),
  Q("المدود", "«نَسْتَعِينُ» عند الوقف:", listOf("عارض للسكون", "لازم", "منفصل", "بدل"), 0, "السكون عارض بسبب الوقف."),
  Q("الوقف", "علامة «ج» في المصحف:", listOf("الوقف والوصل جائزان", "وقف لازم", "ممنوع", "الوقف أولى"), 0, "جائز."),
  Q("الوقف", "علامة «قلى»:", listOf("الوقف أولى", "الوصل أولى", "ممنوع", "لازم"), 0, "الوقف أولى."),
  Q("الوقف", "علامة «صلى»:", listOf("الوصل أولى", "الوقف أولى", "ممنوع", "لازم"), 0, "الوصل أولى."),
  Q("الوقف", "الوقف الذي لا يتم به المعنى:", listOf("القبيح", "التام", "الكافي", "الحسن"), 0, "القبيح."))

@Composable fun Learn(db: AppDb) { var inQuiz by remember { mutableStateOf(false) }
  Column { TextButton({ inQuiz = !inQuiz }) { Text(if (inQuiz) "الدروس" else "الاختبارات") }
    if (inQuiz) QuizScreen(db) else LessonsList(db) } }

@Composable fun QuizScreen(db: AppDb) { var topic by remember { mutableStateOf<String?>(null) }; var started by remember { mutableStateOf(false) }
  if (!started) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("اختر باب الاختبار (10 أسئلة عشوائية)", style = MaterialTheme.typography.titleMedium)
    (listOf("الكل") + topics).forEach { t -> Button({ topic = if (t == "الكل") null else t; started = true }, Modifier.fillMaxWidth()) { Text(t) } } }
  else QuizRun(db, topic) { started = false } }

@Composable fun QuizRun(db: AppDb, topic: String?, onExit: () -> Unit) { val sc = rememberCoroutineScope()
  val items = remember { (if (topic == null) quiz else quiz.filter { it.topic == topic }).shuffled().take(10).map { q ->
    val o = q.options.withIndex().shuffled(); Triple(q, o.map { it.value }, o.indexOfFirst { it.index == q.answer }) } }
  var i by remember { mutableIntStateOf(0) }; var pick by remember { mutableIntStateOf(-1) }; var score by remember { mutableIntStateOf(0) }
  if (i >= items.size) {
    LaunchedEffect(Unit) { db.dao().save(Progress("quiz", true, score)); db.dao().save(Progress("quiztotal", true, items.size)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("النتيجة: $score / ${items.size}", style = MaterialTheme.typography.headlineMedium); Button({ onExit() }) { Text("اختبار آخر") } }
  } else { val (q, opts, ans) = items[i]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("سؤال ${i + 1} من ${items.size} — ${q.topic}"); Text(q.text, style = MaterialTheme.typography.titleLarge)
      opts.forEachIndexed { k, o -> OutlinedButton({ if (pick < 0) { pick = k; val ok = k == ans; if (ok) score++
        sc.launch { val key = (if (ok) "c:" else "w:") + q.topic; val cur = db.dao().get(key)?.score ?: 0; db.dao().save(Progress(key, false, cur + 1)) } } }, Modifier.fillMaxWidth()) {
        Text(o + when { pick >= 0 && k == ans -> "  ✓"; pick == k -> "  ✗"; else -> "" }) } }
      if (pick >= 0) { Text(q.why); Button({ i++; pick = -1 }) { Text("التالي") } } } } }
