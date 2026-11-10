package com.tajweed.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(b: Bundle?) { super.onCreate(b)
    val rec = Recitation(this); val db = AppDb.get(this)
    setContent {
      val dark by store.data.let { f -> remember { f } }.collectAsState(initial = null)
      val isDark = dark?.get(DARK) ?: false
      val scheme = if (isDark) darkColorScheme(primary = Color(0xFF7CC4A0)) else lightColorScheme(primary = Color(0xFF1B5E49))
      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme) { App(rec, db, isDark) }
      }
    }
  }
}
@Composable fun App(rec: Recitation, db: AppDb, isDark: Boolean) {
  var tab by remember { mutableIntStateOf(0) }; var jump by remember { mutableStateOf<String?>(null) }
  val tabs = listOf("الرئيسية" to Icons.Default.Home, "التعلم" to Icons.Default.School, "المخارج" to Icons.Default.RecordVoiceOver,
    "المصحف" to Icons.Default.MenuBook, "حسابي" to Icons.Default.Person)
  Scaffold(bottomBar = { NavigationBar { tabs.forEachIndexed { i, (t, ic) ->
    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(ic, t) }, label = { Text(t) }) } } }) { pad ->
    Box(Modifier.padding(pad).padding(12.dp)) { when (tab) {
      0 -> HomeHub(db, rec) { jump = it; tab = 2 }; 1 -> Learn(db); 2 -> Makharij(rec, jump); 3 -> MushafHub(rec, db) { tab = 1 }; else -> Account(isDark, db) } }
  }
}
@Composable fun Home(db: AppDb) {
  val p by db.dao().progress().collectAsState(emptyList()); val total = levels.sumOf { it.lessons.size }
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("السلام عليكم", style = MaterialTheme.typography.headlineMedium); Streak()
    Text("نسبة إكمال المنهج: ${p.count { it.done && levels.any { lv -> it.lessonId in lv.lessons } } * 100 / total}%")
    LinearProgressIndicator(progress = { p.count { it.done && levels.any { lv -> it.lessonId in lv.lessons } }.toFloat() / total }, Modifier.fillMaxWidth())
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("آية اليوم التجويدية", style = MaterialTheme.typography.titleMedium)
      val d = dailyAyat[((System.currentTimeMillis() / 86400000L) % dailyAyat.size).toInt()]; Text(d.first, style = MaterialTheme.typography.titleLarge); Text(d.second) } }
  }
}
@Composable fun LearnList(db: AppDb) { val sc = rememberCoroutineScope(); val p by db.dao().progress().collectAsState(emptyList())
  LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { levels.forEach { lv ->
    item { Text(lv.title, style = MaterialTheme.typography.titleLarge) }
    items(lv.lessons) { l -> val done = p.any { it.lessonId == l && it.done }
      ListItem(headlineContent = { Text(l) }, trailingContent = { Checkbox(done, { sc.launch { db.dao().save(Progress(l, it)) } }) }) } } }
}
@Composable fun Makharij(rec: Recitation, jump: String? = null) { var zone by remember { mutableStateOf("الكل") }; var sel by remember(jump) { mutableStateOf<Letter?>(letters.find { it.name == jump }) }
  sel?.let { l -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    TextButton({ sel = null }) { Text("رجوع") }
    Text("حرف ${l.name}", style = MaterialTheme.typography.headlineLarge)
    Text("المخرج: ${l.makhraj}"); Text("الصفات: ${l.sifat}"); Text("مثال قرآني: ${l.example}")
    LetterExtras(l, rec) }
  } ?: LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    item(span = { GridItemSpan(maxLineSpan) }) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      zones.forEach { z -> FilterChip(zone == z, { zone = z }, label = { Text(z) }) } } }
    items(letters.filter { zone == "الكل" || zoneOf(it) == zone }) { l -> Card(Modifier.clickable { sel = l }) { Text(l.name, Modifier.padding(24.dp), fontSize = 32.sp) } } }
}
@Composable fun Quran(rec: Recitation) { var s by remember { mutableIntStateOf(1) }; var from by remember { mutableIntStateOf(1) }
  var to by remember { mutableIntStateOf(7) }; var rep by remember { mutableIntStateOf(3) }; var sp by remember { mutableFloatStateOf(1f) }
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("مشغل المصحف — الشيخ الحصري", style = MaterialTheme.typography.titleLarge)
    val ctx0 = LocalContext.current
    val last by remember { ctx0.store.data.map { it[LAST] } }.collectAsState(initial = null)
    LaunchedEffect(rec.current) { if (rec.current.isNotEmpty()) ctx0.store.edit { it[LAST] = rec.current } }
    if (rec.current.isNotEmpty()) Text("يُشغَّل الآن: " + rec.current.split(":").let { "${surahNames[it[0].toInt() - 1]} آية ${it[1]}" })
    if (rec.error.isNotEmpty()) Text(rec.error, color = MaterialTheme.colorScheme.error)
    last?.let { l -> val pr = l.split(":"); if (pr.size == 2) OutlinedButton({ val ss = pr[0].toInt(); s = ss; from = pr[1].toInt(); rec.play(ss, from, ayahCount[ss - 1], 1, sp) }) { Text("تابع من: ${surahNames[pr[0].toInt() - 1]} آية ${pr[1]}") } }
    Row { OutlinedButton({ if (s > 1) s-- }) { Text("-") }; Text("  ${surahNames[s - 1]}  "); OutlinedButton({ if (s < 114) s++ }) { Text("+") } }
    Text("من آية $from إلى $to"); Slider(from.toFloat(), { from = it.toInt() }, valueRange = 1f..ayahCount[s - 1].toFloat())
    Slider(to.toFloat(), { to = it.toInt() }, valueRange = 1f..ayahCount[s - 1].toFloat())
    Text("التكرار: $rep (0 = بلا توقف)  |  السرعة: ${"%.2f".format(sp)}x")
    Slider(sp, { sp = it }, valueRange = 0.5f..2f)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button({ val n = ayahCount[s - 1]; val f = from.coerceIn(1, n); rec.play(s, f, to.coerceIn(f, n), rep, sp) }) { Text("تشغيل") }
      OutlinedButton({ rec.exo.pause() }) { Text("إيقاف") }
      OutlinedButton({ rec.exo.seekToNext() }) { Text("التالي") } }
    val ctx = LocalContext.current; val sc = rememberCoroutineScope(); val h = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton({ rec.sleepAfter(15 * 60_000L, h) }) { Text("مؤقت 15 د") }
      OutlinedButton({ sc.launch { AppDb.get(ctx).dao().save(Bookmark("$s:$from", s, from)) } }) { Text("إضافة للمفضلة") }
      OutlinedButton({ sc.launch { ctx.store.edit { it[LAST] = "$s:$from" } } }) { Text("حفظ الموضع") } }
  }
}
@Composable fun Account(isDark: Boolean, db: AppDb) { val p by db.dao().progress().collectAsState(emptyList()); val ctx = LocalContext.current; val sc = rememberCoroutineScope()
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("إحصائياتي", style = MaterialTheme.typography.titleLarge)
    Text("الدروس المنجزة: ${p.count { it.done && it.lessonId != "quiz" }} من ${levels.sumOf { it.lessons.size }}")
    Text("آخر نتيجة اختبار: ${p.find { it.lessonId == "quiz" }?.score ?: "-"} من ${p.find { it.lessonId == "quiztotal" }?.score ?: "-"}")
    Text("نقاط الضعف", style = MaterialTheme.typography.titleMedium)
    topics.forEach { t -> val c = p.find { it.lessonId == "c:$t" }?.score ?: 0; val w = p.find { it.lessonId == "w:$t" }?.score ?: 0
      if (c + w > 0) Text("$t: ${c * 100 / (c + w)}% صحيح" + if (w > c) "  ← راجع هذا الباب" else "") }
    if (p.none { it.lessonId.startsWith("c:") || it.lessonId.startsWith("w:") }) Text("أجب عن اختبارات لتظهر نقاط ضعفك هنا")
    Text("الإعدادات", style = MaterialTheme.typography.titleLarge)
    Text("المحتوى التعليمي (الدروس والأسئلة والأحكام) كُتب للتجربة ويحتاج مراجعة معلّم مجاز بالقراءات قبل الاعتماد عليه. راجع ملف REVIEW.md.", style = MaterialTheme.typography.bodySmall)
    Text("نص المصحف وأحكامه وخطه: Quran Foundation / مجمع الملك فهد لطباعة المصحف الشريف. الصوت: everyayah.com.", style = MaterialTheme.typography.bodySmall)
    Row { Text("الوضع الليلي"); Spacer(Modifier.weight(1f)); Switch(isDark, { v -> sc.launch { ctx.store.edit { it[DARK] = v } } }) } }
}
