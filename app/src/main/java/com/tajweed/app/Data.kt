package com.tajweed.app
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Progress(@PrimaryKey val lessonId: String, val done: Boolean, val score: Int = 0)
@Entity data class AyahText(@PrimaryKey val key: String, val surah: Int, val ayah: Int, val html: String)
@Entity data class Bookmark(@PrimaryKey val key: String, val surah: Int, val ayah: Int, val note: String = "")
@Dao interface AppDao {
  @Query("SELECT * FROM Progress") fun progress(): Flow<List<Progress>>
  @Upsert suspend fun save(p: Progress)
  @Query("SELECT * FROM Bookmark") fun bookmarks(): Flow<List<Bookmark>>
  @Upsert suspend fun save(b: Bookmark)
  @Query("SELECT * FROM Progress WHERE lessonId = :id") suspend fun get(id: String): Progress?
  @Query("DELETE FROM Bookmark WHERE `key` = :k") suspend fun delBookmark(k: String)
  @Upsert suspend fun saveAyat(l: List<AyahText>)
  @Query("SELECT * FROM AyahText WHERE surah = :s ORDER BY ayah") fun ayat(s: Int): Flow<List<AyahText>>
  @Query("SELECT DISTINCT surah FROM AyahText") fun downloadedSurahs(): Flow<List<Int>>
}
val MIG_1_2 = object : androidx.room.migration.Migration(1, 2) { override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
  db.execSQL("CREATE TABLE IF NOT EXISTS `AyahText` (`key` TEXT NOT NULL, `surah` INTEGER NOT NULL, `ayah` INTEGER NOT NULL, `html` TEXT NOT NULL, PRIMARY KEY(`key`))") } }
@Database(entities = [Progress::class, Bookmark::class, AyahText::class], version = 2)
abstract class AppDb : RoomDatabase() {
  abstract fun dao(): AppDao
  companion object { @Volatile private var i: AppDb? = null
    fun get(c: Context) = i ?: synchronized(this) { Room.databaseBuilder(c.applicationContext, AppDb::class.java, "tajweed.db").addMigrations(MIG_1_2).build().also { i = it } } }
}
val Context.store by preferencesDataStore("settings")
val DARK = booleanPreferencesKey("dark")
val LAST = stringPreferencesKey("last_pos") // surah:ayah

data class Letter(val name: String, val makhraj: String, val sifat: String, val example: String)
val letters = listOf(
  Letter("ا", "الجوف", "الجهر، الرخاوة", "الٓمٓ"),
  Letter("ب", "الشفتان", "الجهر، الشدة، القلقلة", "بِسْمِ"),
  Letter("ض", "حافة اللسان مع الأضراس", "الاستعلاء، الإطباق، الاستطالة", "الضَّالِّينَ"),
  Letter("ظ", "طرف اللسان مع الثنايا العليا", "الاستعلاء، الإطباق، الرخاوة", "الظَّالِمِينَ"),
  Letter("غ", "أدنى الحلق", "الاستعلاء، الرخاوة", "الْمَغْضُوبِ"),
  Letter("م", "الشفتان + الخيشوم للغنة", "الجهر، التوسط، الغنة", "الرَّحْمَٰنِ"),
  Letter("ت", "طرف اللسان مع أصول الثنايا العليا", "الهمس، الشدة، الاستفال", "التَّوَّابُ"),
  Letter("ط", "طرف اللسان مع أصول الثنايا العليا", "الجهر، الشدة، الاستعلاء، الإطباق، القلقلة", "الطَّيِّبَاتِ"),
  Letter("س", "طرف اللسان مع الثنايا السفلى", "الهمس، الرخاوة، الصفير", "السَّمَاءِ"),
  Letter("ص", "طرف اللسان مع الثنايا السفلى", "الهمس، الرخاوة، الاستعلاء، الإطباق، الصفير", "الصِّرَاطَ"),
  Letter("ذ", "طرف اللسان مع أطراف الثنايا العليا", "الجهر، الرخاوة، الاستفال", "الَّذِينَ"),
  Letter("ز", "طرف اللسان مع الثنايا السفلى", "الجهر، الرخاوة، الصفير", "الزَّكَاةَ"),
  Letter("ء", "أقصى الحلق", "الجهر، الشدة، الاستفال", "أَنْعَمْتَ"), Letter("هـ", "أقصى الحلق", "الهمس، الرخاوة", "هُدًى"),
  Letter("ع", "وسط الحلق", "الجهر، التوسط، الاستفال", "الْعَالَمِينَ"), Letter("ح", "وسط الحلق", "الهمس، الرخاوة", "الْحَمْدُ"),
  Letter("خ", "أدنى الحلق", "الهمس، الرخاوة، الاستعلاء", "خَلَقَ"), Letter("ق", "أقصى اللسان مع اللهاة", "الجهر، الشدة، الاستعلاء، القلقلة", "قُلْ"),
  Letter("ك", "أقصى اللسان أسفل مخرج القاف", "الهمس، الشدة، الاستفال", "كَفَرُوا"), Letter("ج", "وسط اللسان مع الحنك الأعلى", "الجهر، الشدة، القلقلة", "جَاءَ"),
  Letter("ش", "وسط اللسان مع الحنك الأعلى", "الهمس، الرخاوة، التفشي", "شَيْءٍ"), Letter("ي", "وسط اللسان مع الحنك الأعلى", "الجهر، الرخاوة، اللين", "يَوْمِ"),
  Letter("ل", "حافة اللسان مع ما يليها من الحنك", "الجهر، التوسط، الانحراف", "اللَّهِ"), Letter("ن", "طرف اللسان مع الحنك الأعلى", "الجهر، التوسط، الغنة", "نَعْبُدُ"),
  Letter("ر", "طرف اللسان مع الحنك الأعلى", "الجهر، التوسط، التكرير", "رَبِّ"), Letter("د", "طرف اللسان مع أصول الثنايا العليا", "الجهر، الشدة، القلقلة", "الدِّينِ"),
  Letter("ث", "طرف اللسان مع أطراف الثنايا العليا", "الهمس، الرخاوة", "ثُمَّ"), Letter("ف", "باطن الشفة السفلى مع الثنايا العليا", "الهمس، الرخاوة، الإذلاق", "فِي"),
  Letter("و", "الشفتان", "الجهر، الرخاوة، اللين", "وَلَا"))
data class Level(val title: String, val lessons: List<String>)
val levels = listOf(
  Level("المستوى الأول: المبتدئ", listOf("مخارج الحروف", "صفات الحروف", "أحكام النون الساكنة والتنوين", "أحكام الميم الساكنة")),
  Level("المستوى الثاني: المدود", listOf("المد الطبيعي", "المتصل والمنفصل", "اللازم والعارض", "اللين والبدل والصلة والعوض")),
  Level("المستوى الثالث: أبواب أخرى", listOf("الراء ولام الجلالة", "التفخيم والترقيق", "المتماثلان والمتجانسان", "الاستعاذة والبسملة")),
  Level("المستوى الرابع: الإتقان", listOf("الوقف والابتداء", "المتشابهات الصوتية", "السكتات في حفص", "التلاوة المتقدمة")))
fun ayahUrl(s: Int, a: Int) = "https://everyayah.com/data/Husary_64kbps/%03d%03d.mp3".format(s, a)

fun zoneOf(l: Letter): String { val m = l.makhraj; return when { "الحلق" in m -> "الحلق"; "الجوف" in m -> "الجوف"
  "الشفة" in m || "الشفتان" in m -> "الشفتان"; else -> "اللسان" } }
val zones = listOf("الكل", "الحلق", "اللسان", "الشفتان", "الجوف")
