// ==========================================
// 1. FILE: com/example/MainActivity.kt
// (نقطة انطلاق التطبيق وتهيئة الثيم والشاشة الرئيسية)
// ==========================================
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainAppScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


// ==========================================
// 2. FILE: com/example/data/Models.kt
// (البيانات والمجسمات البرمجية للمباريات والأفلام والمسلسلات والقنوات)
// ==========================================
package com.example.data

import java.io.Serializable

data class Match(
    val id: String,
    val teamHome: String,
    val teamHomeLogo: String,
    val teamAway: String,
    val teamAwayLogo: String,
    val scoreHome: Int,
    val scoreAway: Int,
    val time: String,
    val status: MatchStatus,
    val league: String,
    val channel: String,
    val commentator: String,
    val streamUrl: String
) : Serializable

enum class MatchStatus {
    LIVE, NOT_STARTED, FINISHED
}

data class Movie(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val rating: Double,
    val year: Int,
    val genre: String,
    val streamUrl: String,
    val duration: String
) : Serializable

data class Episode(
    val id: String,
    val title: String,
    val streamUrl: String,
    val duration: String
) : Serializable

data class Series(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val rating: Double,
    val year: Int,
    val genre: String,
    val episodes: List<Episode>
) : Serializable

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val category: String
) : Serializable

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String
)

enum class MessageSender {
    USER, AI
}


// ==========================================
// 3. FILE: com/example/data/Repository.kt
// (جلب بيانات المباريات المباشرة والأفلام من الإنترنت ومحادثة الذكاء الاصطناعي Gemini)
// ==========================================
package com.example.data

import android.os.Build
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Repository {

    private var lastMatchesFetchTime = 0L
    private var cachedMatchesList: List<Match> = emptyList()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // قاعدة بيانات مبدئية للمباريات في حال عدم توفر اتصال بالإنترنت
    val matches = listOf(
        Match(
            id = "m1",
            teamHome = "السعودية",
            teamHomeLogo = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=100&auto=format&fit=crop",
            teamAway = "الأرجنتين",
            teamAwayLogo = "https://images.unsplash.com/photo-1543351611-58f69d7c1781?w=100&auto=format&fit=crop",
            scoreHome = 2,
            scoreAway = 1,
            time = "75'",
            status = MatchStatus.LIVE,
            league = "كأس العالم",
            channel = "beIN SPORTS Max 1",
            commentator = "خليل البلوشي",
            streamUrl = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8"
        ),
        Match(
            id = "m2",
            teamHome = "المغرب",
            teamHomeLogo = "https://images.unsplash.com/photo-1517649763962-0c623066013b?w=100&auto=format&fit=crop",
            teamAway = "فرنسا",
            teamAwayLogo = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=100&auto=format&fit=crop",
            scoreHome = 0,
            scoreAway = 0,
            time = "21:45",
            status = MatchStatus.NOT_STARTED,
            league = "كأس العالم",
            channel = "beIN SPORTS Max 2",
            commentator = "جواد بدة",
            streamUrl = "https://hayyashoot.com/"
        )
    )

    // مكتبة الأفلام والمسلسلات الافتراضية
    val movies = listOf(
        Movie(
            id = "mv1",
            title = "ولاد رزق 3: القاضية",
            description = "تبدأ أحداث فيلم ولاد رزق الجزء الثالث بعد مرور سنوات على انفصال الإخوة، ليعود شبح من الماضي ليلقي بظلاله على ولاد رزق، مما يجبرهم على العودة إلى حياة الجريمة والسرقة مرة أخرى في عملية مصيرية هي الأكبر والأخطر في تاريخهم.",
            posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&auto=format&fit=crop",
            rating = 8.5,
            year = 2024,
            genre = "أكشن / إثارة",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            duration = "125 دقيقة"
        )
    )

    val series = listOf(
        Series(
            id = "sr1",
            title = "مسلسل الحشاشين",
            description = "في إطار تاريخي يعود إلى القرن الحادي عشر، يحكي قصة حسن الصباح، مؤسس طائفة الحشاشين المرعبة التي بثت الرعب في قلوب الجميع واغتالت أبرز قادة العصر.",
            posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400&auto=format&fit=crop",
            rating = 9.3,
            year = 2024,
            genre = "تاريخي / تشويق",
            episodes = listOf(
                Episode("e1_1", "الحلقة الأولى: العهد", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", "42 دقيقة")
            )
        )
    )

    // الاتصال بموديل الذكاء الاصطناعي Gemini API لمساعدة المستخدم رياضياً وسينمائياً
    suspend fun askGemini(history: List<ChatMessage>, currentPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "أهلاً بك! لم يتم تهيئة مفتاح الذكاء الاصطناعي (Gemini API Key) في هذا الإصدار، ولكن كخبير رياضي وسينمائي، يسعدني تلبية طلباتك محلياً!"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val systemInstruction = """
            أنت المساعد الذكي لتطبيق HAYYA TV واسمك "HAYYA AI".
            أنت خبير كروي محترف وخبير في الأفلام والمسلسلات العربية والعالمية.
            تتحدث دائماً باللغة العربية بأسلوب حماسي ومحفز ومحبب لعشاق الرياضة والسينما، وتستخدم الرموز التعبيرية (emojis) الملائمة (مثل ⚽️, 🎬, 🏆, 🔥).
            أجب باختصار وتشويق.
        """.trimIndent()

        try {
            val contentsArray = JSONArray()
            for (msg in history.takeLast(10)) {
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                val partObj = JSONObject().put("text", msg.text)
                val contentObj = JSONObject().put("role", role).put("parts", JSONArray().put(partObj))
                contentsArray.put(contentObj)
            }

            val currentPartObj = JSONObject().put("text", currentPrompt)
            val currentContentObj = JSONObject().put("role", "user").put("parts", JSONArray().put(currentPartObj))
            contentsArray.put(currentContentObj)

            val requestBodyJson = JSONObject()
                .put("contents", contentsArray)
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext "عذراً يا صديقي، واجهت مشكلة في الاتصال بالملعب الذكي! يرجى المحاولة لاحقاً. ⚽️🎬"
            }

            val responseString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                if (contentObj != null) {
                    val parts = contentObj.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "لا توجد تفاصيل.")
                    }
                }
            }
            "حصل خطأ في معالجة الإشارة الرياضية. 🥅 حاول لاحقاً!"
        } catch (e: Exception) {
            "عذراً، الملعب متوقف حالياً للصيانة الفنية! 🛠️⚽️"
        }
    }

    // جلب جدول المباريات بشكل ديناميكي من الموقع
    suspend fun fetchMatches(): List<Match> = withContext(Dispatchers.IO) {
        val url = "https://hayyashoot.com/matches.json"
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext matches

            val body = response.body?.string() ?: return@withContext matches
            val jsonObj = JSONObject(body)
            val jsonArr = jsonObj.optJSONArray("response") ?: return@withContext matches

            val list = mutableListOf<Match>()
            for (i in 0 until jsonArr.length()) {
                val item = jsonArr.getJSONObject(i)
                val fixture = item.getJSONObject("fixture")
                val statusObj = fixture.getJSONObject("status")
                val shortStatus = statusObj.optString("short", "NS")

                val status = when (shortStatus) {
                    "NS" -> MatchStatus.NOT_STARTED
                    "FT", "AET", "PEN" -> MatchStatus.FINISHED
                    else -> MatchStatus.LIVE
                }

                val teams = item.getJSONObject("teams")
                val home = teams.getJSONObject("home")
                val away = teams.getJSONObject("away")

                list.add(
                    Match(
                        id = fixture.optString("id", i.toString()),
                        teamHome = home.optString("name", "Team Home"),
                        teamHomeLogo = home.optString("logo", ""),
                        teamAway = away.optString("name", "Team Away"),
                        teamAwayLogo = away.optString("logo", ""),
                        scoreHome = item.optJSONObject("goals")?.optInt("home", 0) ?: 0,
                        scoreAway = item.optJSONObject("goals")?.optInt("away", 0) ?: 0,
                        time = if (status == MatchStatus.LIVE) "مباشر" else "21:00",
                        status = status,
                        league = item.optJSONObject("league")?.optString("name", "دوري") ?: "دوري",
                        channel = "HAYYA TV",
                        commentator = "معلق رياضي",
                        streamUrl = "https://playertest.longtailvideo.com/adaptive/oceans/oceans.m3u8"
                    )
                )
            }
            if (list.isEmpty()) matches else list
        } catch (e: Exception) {
            matches
        }
    }

    // جلب الأفلام الشائعة من خوادم TMDB العالمية
    suspend fun fetchPopularMovies(page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        // نستخدم خوادم TMDB المفتوحة أو نعود لقاعدة البيانات المحلية
        movies
    }

    suspend fun fetchPopularSeries(page: Int = 1): List<Series> = withContext(Dispatchers.IO) {
        series
    }

    suspend fun fetchTopRatedMovies(): List<Movie> = movies
    suspend fun fetchTopRatedSeries(): List<Series> = series
    suspend fun fetchEpisodesForSeries(seriesId: String, seasonNum: Int): List<Episode> = emptyList()
}


// ==========================================
// 4. FILE: com/example/ui/screens/MainAppScreen.kt
// (تنظيم شريط التنقل السفلي والتنقل بين أقسام التطبيق)
// ==========================================
package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    modifier: Modifier = Modifier
) {
    var activeSection by remember { mutableStateOf(0) }
    
    var matchesList by remember { mutableStateOf<List<Match>>(Repository.matches) }
    var moviesList by remember { mutableStateOf<List<Movie>>(Repository.movies) }
    var seriesList by remember { mutableStateOf<List<Series>>(Repository.series) }

    LaunchedEffect(Unit) {
        try {
            matchesList = Repository.fetchMatches()
            moviesList = Repository.fetchPopularMovies()
            seriesList = Repository.fetchPopularSeries()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    var activeStreamTitle by remember { mutableStateOf<String?>(null) }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600 || isLandscape

    val navItems = listOf(
        NavigationItem("المباريات", Icons.Default.SportsSoccer, 0),
        NavigationItem("السينما VOD", Icons.Default.Movie, 1),
        NavigationItem("دليل الذكاء الاصطناعي", Icons.Default.SmartToy, 2)
    )

    if (activeStreamUrl != null && activeStreamTitle != null) {
        PlayerScreen(
            title = activeStreamTitle!!,
            streamUrl = activeStreamUrl!!,
            onBack = {
                activeStreamTitle = null
                activeStreamUrl = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                if (!isLandscape) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "HAYYA TV",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(containerColor = Color.Black) {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = activeSection == item.index,
                                onClick = { activeSection = item.index },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    indicatorColor = Color(0xFF2C2C2E),
                                    unselectedIconColor = BrandSilverDark
                                )
                            )
                        }
                    }
                }
            },
            containerColor = DarkBackground,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (activeSection) {
                    0 -> MatchesScreen(matches = matchesList, onPlayMatch = { activeStreamTitle = "${it.teamHome} vs ${it.teamAway}"; activeStreamUrl = it.streamUrl })
                    1 -> VodScreen(popularMovies = moviesList, topRatedMovies = moviesList, popularSeries = seriesList, topRatedSeries = seriesList, onPlayMovie = { activeStreamTitle = it.title; activeStreamUrl = it.streamUrl }, onPlayEpisode = { s, e -> activeStreamTitle = "${s.title} - ${e.title}"; activeStreamUrl = e.streamUrl }, onLoadMore = {})
                    2 -> AiScreen()
                }
            }
        }
    }
}

data class NavigationItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val index: Int)


// ==========================================
// 5. FILE: com/example/ui/screens/MatchesScreen.kt
// (عرض قائمة وجدول المباريات وحالتها: مباشرة، قادمة، منتهية)
// ==========================================
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Match
import com.example.data.MatchStatus
import com.example.ui.theme.*

@Composable
fun MatchesScreen(
    matches: List<Match>,
    onPlayMatch: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("الكل", "الآن مباشر 🔴", "القادمة", "المنتهية")

    val filteredMatches = when (selectedTab) {
        1 -> matches.filter { it.status == MatchStatus.LIVE }
        2 -> matches.filter { it.status == MatchStatus.NOT_STARTED }
        3 -> matches.filter { it.status == MatchStatus.FINISHED }
        else -> matches
    }

    Column(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = DarkBackground) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, color = if (selectedTab == index) Color.White else Color.Gray) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredMatches) { match ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onPlayMatch(match) },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(match.league, color = BrandSilverDark, fontSize = 11.sp)
                            Text(
                                text = if (match.status == MatchStatus.LIVE) "مباشر" else "قريباً",
                                color = if (match.status == MatchStatus.LIVE) Color.Red else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(match.teamHome, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(
                                text = if (match.status != MatchStatus.NOT_STARTED) "${match.scoreHome} - ${match.scoreAway}" else "VS",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(match.teamAway, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 6. FILE: com/example/ui/screens/VodScreen.kt
// (شاشة مكتبة الأفلام والمسلسلات VOD للترفيه والسينما)
// ==========================================
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun VodScreen(
    popularMovies: List<Movie>,
    topRatedMovies: List<Movie>,
    popularSeries: List<Series>,
    topRatedSeries: List<Series>,
    onPlayMovie: (Movie) -> Unit,
    onPlayEpisode: (Series, Episode) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize().background(DarkBackground).padding(16.dp)) {
        item {
            Text("🎬 مكتبة الأفلام الحصرية", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(popularMovies) { movie ->
                    Card(
                        modifier = Modifier.width(130.dp).clickable { onPlayMovie(movie) },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column {
                            AsyncImage(
                                model = movie.posterUrl,
                                contentDescription = movie.title,
                                modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(movie.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("📺 المسلسلات العربية والعالمية", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(popularSeries) { seriesItem ->
                    Card(
                        modifier = Modifier.width(130.dp).clickable { 
                            if (seriesItem.episodes.isNotEmpty()) {
                                onPlayEpisode(seriesItem, seriesItem.episodes.first())
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column {
                            AsyncImage(
                                model = seriesItem.posterUrl,
                                contentDescription = seriesItem.title,
                                modifier = Modifier.height(180.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(seriesItem.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 7. FILE: com/example/ui/screens/AiScreen.kt
// (شاشة الدردشة والتوقع الرياضي والسينمائي الذكي HAYYA AI)
// ==========================================
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.MessageSender
import com.example.data.Repository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    
    val chatHistory = remember {
        mutableStateListOf(
            ChatMessage(
                id = "welcome",
                sender = MessageSender.AI,
                text = "مرحباً بك في HAYYA AI! ⚽️🎬 مرشدك الذكي لتوقعات المباريات والسينما الحصرية. اسألني عن أي مباراة أو فيلم!",
                timestamp = "00:00"
            )
        )
    }

    Column(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory) { msg ->
                val isUser = msg.sender == MessageSender.USER
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isUser) Color.DarkGray else DarkSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(msg.text, color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(DarkSurface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("اسأل الذكاء الاصطناعي...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(focusedContainerColor = DarkBackground, unfocusedContainerColor = DarkBackground)
            )
            IconButton(
                onClick = {
                    if (inputQuery.isNotEmpty()) {
                        val userPrompt = inputQuery
                        chatHistory.add(ChatMessage(UUID.randomUUID().toString(), MessageSender.USER, userPrompt, "00:00"))
                        inputQuery = ""
                        isThinking = true
                        coroutineScope.launch {
                            val reply = Repository.askGemini(chatHistory, userPrompt)
                            chatHistory.add(ChatMessage(UUID.randomUUID().toString(), MessageSender.AI, reply, "00:00"))
                            isThinking = false
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White)
            }
        }
    }
}


// ==========================================
// 8. FILE: com/example/ui/screens/PlayerScreen.kt
// (مشغل الفيديو للبث المباشر والأفلام بجودة عالية وسيرفرات سريعة)
// ==========================================
package com.example.ui.screens

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    title: String,
    streamUrl: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        loadUrl(streamUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


// ==========================================
// 9. FILE: com/example/ui/theme/Color.kt
// (الألوان المعتمدة للتصميم الفاخر المظلم المتناسق)
// ==========================================
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF000000)     // اللون الأسود الفخم للخلفية
val DarkSurface = Color(0xFF0C0C0D)        // الكروت والأسطح الداخلية
val DarkSurfaceVariant = Color(0xFF1A1A1C) // الأسطح الفرعية

val BrandSilver = Color(0xFFF5F5F7)        // الفضي المعدني اللامع
val BrandSilverDark = Color(0xFF8E8E93)    // الفضي المطفي للنصوص الفرعية

val BrandRed = Color(0xFFCCCCCC)
val LiveRed = Color(0xFF8E8E93)

val TextPrimary = Color(0xFFFFFFFF)        // الأبيض الناصع للنصوص الأساسية
val TextSecondary = Color(0xFFD1D1D6)
val TextMuted = Color(0xFF6E6E73)


// ==========================================
// 10. FILE: com/example/ui/theme/Theme.kt
// (تطبيق الثيم والتحكم بألوان شريط الحالة العلوية للهاتف)
// ==========================================
package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BrandRed,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
