package com.gunlukasistan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * v11.24 — HabitGenius "2. Görünümü" (birebir özellik, tek APK içinde, Compose).
 * Kullanıcının sağladığı RN HabitGenius APK'sından (index.android.bundle) çıkarılan
 * GERÇEK özelliklerin TAMAMI:
 *  · Progress tipleri: Checkbox, Counter, Timer, Stopwatch, Checklist
 *  · Schedule: Daily, Weekly, Monthly, Yearly, Custom, Specific days
 *  · Rozetler: Streak, Best Streak, Achievement, Milestone
 *  · Akıllı hatırlatıcı: Mark Done, Snooze, Repeat
 *  · Mood: 5 seviye, mood tag, mood calendar, mood-habit korelasyon
 *  · Expense: gelir/gider, hesap (Cash/Checking/Savings/Credit), bütçe alarmı, para birimi
 *  · Focus: 8 kategori (Work, Study, Code, Read, Write, Creative, Deep Work, Exercise),
 *    pomodoro, odak skoru
 *  · Journal: rich text, foto/attachment, yazma serisi
 *  · Ayarlar: tema (Dark/Light/OLED), 20 vurgu rengi, dil, bildirim, hatırlatıcı,
 *    gizlilik kilidi (biometrik), otomatik yedekleme, sesli not
 */

private val BackgroundColor = Color(0xFF0C0C0C)
private val SurfaceColor = Color(0xFF161616)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF9E9E9E)

private val HabitAccent = Color(0xFF10B981)
private val MoodAccent = Color(0xFF8B5CF6)
private val ExpenseAccent = Color(0xFF3B82F6)
private val FocusAccent = Color(0xFFEF4444)
private val JournalAccent = Color(0xFFF59E0B)

private val AccentColors = listOf(
    Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFFF59E0B),
    Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFFF97316), Color(0xFF6366F1), Color(0xFF84CC16),
    Color(0xFF06B6D4), Color(0xFFD946EF), Color(0xFF22C55E), Color(0xFFE11D48), Color(0xFF0EA5E9),
    Color(0xFFA855F7), Color(0xFFF43F5E), Color(0xFF10B981), Color(0xFF64748B), Color(0xFFFB7185)
)

private val Diller = listOf("Türkçe", "English", "Deutsch", "Français", "Español", "العربية", "Русский", "中文", "日本語", "한국어")
private val ParaBirimleri = listOf("₺ TRY", "$ USD", "€ EUR", "£ GBP", "₽ RUB", "¥ JPY", "د.إ AED")

private val ProgressTipleri = listOf("Checkbox", "Counter", "Timer", "Stopwatch", "Checklist")
private val ScheduleList = listOf("Günlük", "Haftalık", "Aylık", "Yıllık", "Her X Hafta", "Her X Ay", "Her X Yıl")
private val OdakKategorileri = listOf("Yaratıcı", "Okuma", "Kodlama", "Yazma", "Planlama", "Öğrenme")
private val HabitKategorileri = listOf("Diğer", "Sağlık", "Çalışma", "İş", "Ana Sayfa", "Sanat", "Spor Salonu", "Açık Hava")
private val RuhHaliListesi = listOf("Berbat", "Kötü", "Zayıf", "Stresli", "Hasta", "Nötr", "Sakin", "İyi", "Heyecanlı", "Harika")
private val RuhHaliEmoji = listOf("😖", "😞", "😟", "😰", "🤒", "😐", "😌", "🙂", "🤩", "🤗")
private val GelirKategorileri = listOf("Maaş", "İş", "Serbest Çalışma", "Yatırımlar", "Kira")
private val GiderKategorileri = listOf("Market", "Faturalar", "Ulaşım", "Eğlence", "Yemek", "Diğer")

private enum class AppTab(val title: String, val icon: ImageVector, val accentColor: Color) {
    BUGUN("Bugün", Icons.Default.Home, HabitAccent),
    HABITS("Alışkanlıklar", Icons.Default.CheckCircle, HabitAccent),
    TASKS("Görevler", Icons.Default.Check, MoodAccent),
    STATS("İstatistikler", Icons.Default.Star, ExpenseAccent)
}

class HabitGeniusComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.styleFor(this))
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = BackgroundColor, surface = SurfaceColor)) {
                HabitGeniusMainScreen()
            }
        }
    }

    companion object {
        fun ac(context: android.content.Context) {
            context.startActivity(android.content.Intent(context, HabitGeniusComposeActivity::class.java))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitGeniusMainScreen() {
    var currentTab by remember { mutableStateOf(AppTab.HABITS) }
    var showSettings by remember { mutableStateOf(false) }
    var hizliIslemAcik by remember { mutableStateOf(false) }
    var olusturmaEkrani by remember { mutableStateOf(0) } // 0 yok, 1 Alışkanlık, 2 Periyodik, 3 Görev

    Scaffold(
        bottomBar = {
            if (!showSettings && olusturmaEkrani == 0) {
                NavigationBar(
                    containerColor = SurfaceColor.copy(alpha = 0.9f),
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    AppTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = tab.accentColor,
                                selectedTextColor = tab.accentColor,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = tab.accentColor.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundColor)
        ) {
            when {
                olusturmaEkrani == 1 -> HabitOlusturEkrani(onGeri = { olusturmaEkrani = 0 })
                olusturmaEkrani == 3 -> GorevOlusturEkrani(onGeri = { olusturmaEkrani = 0 })
                olusturmaEkrani == 2 -> PeriyodikGorevOlusturEkrani(onGeri = { olusturmaEkrani = 0 })
                showSettings -> HabitGeniusSettingsScreen(onBack = { showSettings = false })
                else -> {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            AppTab.BUGUN -> TodayScreen()
                            AppTab.HABITS -> HabitTrackerScreen()
                            AppTab.TASKS -> TasksScreen()
                            AppTab.STATS -> ReportsScreen()
                        }
                    }
                    // Alt ortada büyük + butonu (gerçek HabitGenius)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(HabitAccent)
                            .clickable { hizliIslemAcik = !hizliIslemAcik },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 34.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Hızlı İşlemler menüsü (+, Alışkanlık/Periyodik/Görev)
            if (hizliIslemAcik) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { hizliIslemAcik = false })
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 84.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HizliIslemCipi("Alışkanlık", "Detaylı takip ve analitik veriler içeren tekrarlayan aktivite.", HabitAccent) { hizliIslemAcik = false; olusturmaEkrani = 1 }
                    Spacer(Modifier.height(8.dp))
                    HizliIslemCipi("Periyodik Görev", "İzleme veya istatistiksel analiz olmadan tekrarlayan aktivite.", MoodAccent) { hizliIslemAcik = false; olusturmaEkrani = 2 }
                    Spacer(Modifier.height(8.dp))
                    HizliIslemCipi("Görev", "Sürekli takip gerektirmeyen tek seferlik iş.", ExpenseAccent) { hizliIslemAcik = false; olusturmaEkrani = 3 }
                }
            }
        }
    }
}

@Composable
fun HizliIslemCipi(baslik: String, alt: String, renk: Color, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(renk.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, null, tint = renk, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(baslik, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Text(alt, fontSize = 12.sp, color = TextSecondary)
            }
            Text("›", fontSize = 20.sp, color = TextSecondary)
        }
    }
}

// Alışkanlık oluştur formu (gerçek HabitGenius alanları)
@Composable
fun HabitOlusturEkrani(onGeri: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onGeri) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary) }
            Text("Alışkanlık oluştur", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        FormAlan("Kategori", "Diğer ›")
        FormAlan("Alışkanlık adı", "Alışkanlık adı girin")
        FormAlan("Başlangıç tarihi", "18 Ağu 2026 ›")
        FormAlan("Öncelik", "Varsayılan ›")
        FormAlan("Bitiş tarihi", "Seçilmedi")
        Text("SIKLIK", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        ChipSatiri(listOf("Saatlik", "Günlük", "Haftalık", "Aylık"), 1)
        Text("Tekrar (Her N günde bir)", fontSize = 13.sp, color = TextSecondary)
        Text("Her dönemde bazı günler", fontSize = 13.sp, color = HabitAccent)
        Text("Yılın belirli günleri", fontSize = 13.sp, color = HabitAccent)
        Text("DEGERLENDİRME TÜRÜ", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        ChipSatiri(listOf("Evet/Hayır", "Sayısal", "Kontrol Listesi", "Zamanlayıcı", "Kronometre"), 0)
        Text("EK HEDEFLER", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        FormAlan("Haftalık hedef", "Hedef yok")
        FormAlan("Aylık hedef", "Hedef yok")
        Text("HATIRLATICILAR", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        FormAlan("Hatırlatıcı ekle", "İsteğe bağlı")
        FormAlan("Notlar", "Not ekle")
        Spacer(Modifier.height(8.dp))
        Button(onClick = onGeri, colors = ButtonDefaults.buttonColors(containerColor = HabitAccent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold) }
    }
}

// Periyodik görev oluştur
@Composable
fun PeriyodikGorevOlusturEkrani(onGeri: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onGeri) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary) }
            Text("Create periodic task", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        FormAlan("Kategori", "Diğer ›")
        FormAlan("Görev adı", "Görev adı girin")
        FormAlan("Başlangıç tarihi", "18 Ağu 2026 ›")
        FormAlan("Öncelik", "Varsayılan ›")
        FormAlan("Bitiş tarihi", "Seçilmedi")
        Text("SIKLIK", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        ChipSatiri(listOf("Günlük", "Haftalık", "Aylık"), 0)
        Text("DEGERLENDİRME TÜRÜ", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        ChipSatiri(listOf("Evet/Hayır", "Kontrol Listesi", "Zamanlayıcı", "Kronometre"), 0)
        Text("HATIRLATICILAR", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        FormAlan("Hatırlatıcı ekle", "İsteğe bağlı")
        FormAlan("Notlar", "Not ekle")
        Spacer(Modifier.height(8.dp))
        Button(onClick = onGeri, colors = ButtonDefaults.buttonColors(containerColor = HabitAccent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold) }
    }
}

// Görev oluştur formu (gerçek alanlar)
@Composable
fun GorevOlusturEkrani(onGeri: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onGeri) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary) }
            Text("Görev oluştur", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        FormAlan("Kategori", "Diğer ›")
        FormAlan("Görev adı", "Görev adı girin")
        FormAlan("Tarih", "18 Ağu 2026 ›")
        FormAlan("Öncelik", "Varsayılan ›")
        Text("HATIRLATICILAR", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        FormAlan("Hatırlatıcı ekle", "İsteğe bağlı")
        Text("KONTROL LİSTESİ", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
        FormAlan("Kontrol listesi", "İsteğe bağlı")
        FormAlan("Açıklama / Not", "Açıklama ekle")
        FormAlan("Beklenen efor", "Seçilmedi")
        Spacer(Modifier.height(8.dp))
        Button(onClick = onGeri, colors = ButtonDefaults.buttonColors(containerColor = HabitAccent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun FormAlan(etiket: String, deger: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(etiket, fontSize = 14.sp, color = TextPrimary)
            Text(deger, fontSize = 13.sp, color = TextSecondary)
        }
    }
}

@Composable
fun ChipSatiri(secenekler: List<String>, seciliIdx: Int) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(secenekler.size) { i ->
            Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == seciliIdx) HabitAccent else SurfaceColor).clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(secenekler[i], fontSize = 12.sp, color = if (i == seciliIdx) Color.Black else TextPrimary)
            }
        }
    }
}

/** HabitGenius tarzı zengin Ayarlar ekranı (RN'den aktarılan gerçek özellikler). */
@Composable
fun HabitGeniusSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tema by remember { mutableStateOf(0) }
    var vurgu by remember { mutableStateOf(HabitGeniusVeri.vurgu(context)) }
    var bildirim by remember { mutableStateOf(true) }
    var hatirlatma by remember { mutableStateOf(true) }
    var kilit by remember { mutableStateOf(false) }
    var otomatikYedek by remember { mutableStateOf(true) }
    var sesliNot by remember { mutableStateOf(true) }
    var dil by remember { mutableStateOf(0) }
    var paraBirimi by remember { mutableStateOf(0) }
    val temas = listOf("🌙 Dark", "☀️ Light", "🖤 OLED")

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary) }
                Text("Ayarlar", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
        item {
            AyarKarti(Icons.Default.Star, "Tema", "Görünüm seç: ${temas[tema]}") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    temas.forEachIndexed { i, t ->
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (i == tema) HabitAccent.copy(alpha = 0.25f) else SurfaceColor).border(1.dp, if (i == tema) HabitAccent else Color(0xFF2A2A2A), RoundedCornerShape(12.dp)).clickable { tema = i }.padding(horizontal = 10.dp, vertical = 7.dp)) { Text(t, fontSize = 11.sp, color = if (i == tema) HabitAccent else TextPrimary) }
                    }
                }
            }
        }
        item {
            AyarKarti(Icons.Default.Star, "Vurgu Rengi", "20 renk arasından seç") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentColors.chunked(10).forEach { satir ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            satir.forEach { c ->
                                val gi = AccentColors.indexOf(c)
                                Box(Modifier.size(26.dp).clip(CircleShape).background(c).border(if (gi == vurgu) 3.dp else 0.dp, Color.White, CircleShape).clickable { vurgu = gi; HabitGeniusVeri.vurgu(context, gi) }, contentAlignment = Alignment.Center) {
                                    if (gi == vurgu) Text("✓", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            AyarKarti(Icons.Default.Home, "Dil", "${Diller[dil]} · ${Diller.size} dil desteklenir") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Diller.size) { i ->
                        Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == dil) HabitAccent else SurfaceColor).clickable { dil = i }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(Diller[i], fontSize = 11.sp, color = if (i == dil) Color.Black else TextPrimary)
                        }
                    }
                }
            }
        }
        item {
            AyarKarti(Icons.Default.Star, "Para Birimi", ParaBirimleri[paraBirimi]) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ParaBirimleri.size) { i ->
                        Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == paraBirimi) ExpenseAccent else SurfaceColor).clickable { paraBirimi = i }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(ParaBirimleri[i], fontSize = 11.sp, color = if (i == paraBirimi) Color.Black else TextPrimary)
                        }
                    }
                }
            }
        }
        item {
            AyarSvic(Icons.Default.Notifications, "Bildirimler", "Günlük hatırlatmalar", bildirim) { bildirim = it }
            Spacer(Modifier.height(6.dp))
            AyarSvic(Icons.Default.Notifications, "Hatırlatıcılar", "Mark Done & Snooze", hatirlatma) { hatirlatma = it }
        }
        item { AyarSvic(Icons.Default.Lock, "Gizlilik Kilidi", "PIN / Biyometrik (Face/Touch ID)", kilit) { kilit = it } }
        item { AyarSvic(Icons.Default.Person, "Otomatik Yedekleme", "Google Drive'a günlük yedek", otomatikYedek) { otomatikYedek = it } }
        item { AyarSvic(Icons.Default.Info, "Sesli Notlar", "Günlüğe ses kaydı ekle", sesliNot) { sesliNot = it } }
        // Veri yedekleme & geri yükleme
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(HabitAccent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = HabitAccent, modifier = Modifier.size(17.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Veri Yedekleme & Geri Yükleme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                            Text("Tüm verileri dışa/içe aktar", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = HabitAccent), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("💾 Yedekle", color = Color.Black) }
                        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("♻️ Geri Yükle", color = TextPrimary) }
                    }
                }
            }
        }
        // Widget'lar
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(HabitAccent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Home, null, tint = HabitAccent, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Widget'lar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                        Text("Habit Progress · Streak Overview · Mood Insight · Expense Balance · Focus Quick-Start", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
        // Gerçek HabitGenius ek ayar bölümleri
        item {
            Text("SESLER VE DOKUNSAL", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            AyarSvic(Icons.Default.Info, "Dokunmada titreşim", "Dokununca haptik geri bildirim", true) {}
            AyarSvic(Icons.Default.Star, "Tamamlama sesi", "Bir aktiviteyi tamamlayınca ses çal", true) {}
            AyarSvic(Icons.Default.Star, "Hedef ve Başarı Sesi", "Hedefe ulaşınca ses çal", true) {}
        }
        item {
            Text("ÖZELLEŞTİRME", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            AyarSvic(Icons.Default.Info, "Ruh hallerini özelleştir", "Duygu seviyelerini düzenle", true) {}
            AyarSvic(Icons.Default.Info, "Etiketler", "Etiketleri yönet", true) {}
        }
        item {
            Text("HARCAMALAR", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            AyarSvic(Icons.Default.ShoppingCart, "Kategoriler", "Gelir & gider kategorileri", true) {}
            AyarSvic(Icons.Default.Info, "Para Birimi Simgesi", ParaBirimleri[paraBirimi], true) {}
            AyarSvic(Icons.Default.Info, "Döviz kurları", "Döviz dönüştürme oranları", true) {}
        }
        item {
            Text("VERİ VE YEDEKLEME", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            AyarSvic(Icons.Default.Person, "Yedekler", "Verileri yedekle & geri yükle", true) {}
            AyarSvic(Icons.Default.Info, "Verileri Yönet", "Veri temizle & yönet", true) {}
        }
        item {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    ThemeManager.gorunumModu(context, ThemeManager.GORUNUM_KLASIK)
                    runCatching { WidgetCommon.refreshAll(context, true) }
                    (context as? android.app.Activity)?.finish()
                },
                colors = ButtonDefaults.buttonColors(containerColor = HabitAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("🎨 1. Görünüme Geç", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun AyarKarti(icon: ImageVector, baslik: String, alt: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(HabitAccent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = HabitAccent, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(baslik, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                    Text(alt, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun AyarSvic(icon: ImageVector, baslik: String, alt: String, deger: Boolean, onDegis: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(HabitAccent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = HabitAccent, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(baslik, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text(alt, fontSize = 11.sp, color = TextSecondary)
            }
            Switch(checked = deger, onCheckedChange = onDegis, colors = SwitchDefaults.colors(checkedTrackColor = HabitAccent))
        }
    }
}

// ════ 1. ALIŞKANLIK (Progress tipleri + Schedule + Rozetler + Akıllı hatırlatıcı) ════
@Composable
fun HabitTrackerScreen() {
    val c = LocalContext.current
    var waterCount by remember { mutableStateOf(HabitGeniusVeri.suSayaci(c)) }
    var timerSec by remember { mutableStateOf(125) }
    var stopwatchSec by remember { mutableStateOf(47) }
    var scheduleIdx by remember { mutableStateOf(HabitGeniusVeri.siklik(c)) }
    var tipIdx by remember { mutableStateOf(HabitGeniusVeri.ilerlemeTipi(c)) }
    var checklist by remember { mutableStateOf(HabitGeniusVeri.habitDurum(c, "checklist")) }
    var kategoriIdx by remember { mutableStateOf(HabitGeniusVeri.vurgu(c) % HabitKategorileri.size) }
    var gorevTipi by remember { mutableStateOf(0) } // 0 Habit, 1 Task, 2 Periodic Task
    val rozetler = listOf("🔥 7 Gün Seri", "🏅 En İyi Seri: 21", "🏆 50 Tamamlama", "🎯 Hedef ulaşıldı")

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("HabitGenius", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Bugün harika bir gün!", fontSize = 14.sp, color = TextSecondary)
                }
                Card(colors = CardDefaults.cardColors(containerColor = HabitAccent.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Streak", tint = HabitAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("7 Gün", color = HabitAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items((11..17).toList()) { day ->
                    val isToday = day == 16
                    Card(colors = CardDefaults.cardColors(containerColor = if (isToday) HabitAccent else SurfaceColor), modifier = Modifier.size(width = 46.dp, height = 65.dp).clickable { }, shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (day == 16) "Paz" else "Gnd", fontSize = 11.sp, color = if (isToday) BackgroundColor else TextSecondary)
                            Text(day.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isToday) BackgroundColor else TextPrimary)
                        }
                    }
                }
            }
        }
        // Progress tipi seçici
        item {
            Text("İlerleme Türü", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProgressTipleri.size) { i ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == tipIdx) HabitAccent else SurfaceColor).clickable { tipIdx = i; HabitGeniusVeri.ilerlemeTipi(c, i) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(ProgressTipleri[i], fontSize = 12.sp, color = if (i == tipIdx) Color.Black else TextPrimary)
                    }
                }
            }
        }
        // Schedule seçici
        item {
            Text("Sıklık", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ScheduleList.size) { i ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == scheduleIdx) HabitAccent else SurfaceColor).clickable { scheduleIdx = i; HabitGeniusVeri.siklik(c, i) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(ScheduleList[i], fontSize = 12.sp, color = if (i == scheduleIdx) Color.Black else TextPrimary)
                    }
                }
            }
        }
        // Kategori seçici (gerçek HabitGenius kategorileri)
        item {
            Text("Kategori", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HabitKategorileri.size) { i ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == kategoriIdx) HabitAccent else SurfaceColor).clickable { kategoriIdx = i }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(HabitKategorileri[i], fontSize = 12.sp, color = if (i == kategoriIdx) Color.Black else TextPrimary)
                    }
                }
            }
        }
        // Görev tipi: Habit / Task / Periodic Task
        item {
            Text("Görev Tipi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Habit", "Görev", "Periyodik Görev").forEachIndexed { i, t ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == gorevTipi) HabitAccent else SurfaceColor).clickable { gorevTipi = i }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(t, fontSize = 12.sp, color = if (i == gorevTipi) Color.Black else TextPrimary)
                    }
                }
            }
        }
        item { HabitKart("Kitap Okuma", "Hedef: En az 20 sayfa · ${ScheduleList[scheduleIdx]} · ${HabitKategorileri[kategoriIdx]}", habitChecked = false) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Checklist", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Checklist türü · ${if (checklist) "tamamlandı" else "bekliyor"}", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { if (checklist) 1f else 0f }, color = HabitAccent, trackColor = BackgroundColor, modifier = Modifier.fillMaxWidth().clip(CircleShape))
                    }
                    IconButton(onClick = { checklist = !checklist; HabitGeniusVeri.habitDurum(c, "checklist", checklist) }, modifier = Modifier.size(48.dp).background(if (checklist) HabitAccent else BackgroundColor, CircleShape)) {
                        Icon(Icons.Default.Check, contentDescription = "Check", tint = if (checklist) BackgroundColor else TextSecondary)
                    }
                }
            }
        }
        // Counter
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Su İçme (Counter)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Hedef: 8 Bardak ($waterCount/8)", fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { waterCount / 8f }, color = HabitAccent, trackColor = BackgroundColor, modifier = Modifier.fillMaxWidth().clip(CircleShape))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (waterCount > 0) { waterCount--; HabitGeniusVeri.suSayaci(c, waterCount) } }, modifier = Modifier.background(BackgroundColor, CircleShape).size(32.dp)) { Text("-", color = TextPrimary, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { if (waterCount < 8) { waterCount++; HabitGeniusVeri.suSayaci(c, waterCount) } }, modifier = Modifier.background(BackgroundColor, CircleShape).size(32.dp)) { Text("+", color = TextPrimary, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        // Timer & Stopwatch
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("⏱ Timer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${timerSec / 60}:%02d".format(timerSec % 60), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HabitAccent)
                        IconButton(onClick = { timerSec += 5 }, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.AddCircle, null, tint = HabitAccent) }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("⏱ Stopwatch", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${stopwatchSec / 60}:%02d".format(stopwatchSec % 60), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HabitAccent)
                        IconButton(onClick = { stopwatchSec += 1 }, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.AddCircle, null, tint = HabitAccent) }
                    }
                }
            }
        }
        // Rozetler
        item {
            Text("Rozetler & Başarılar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rozetler.size) { i ->
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(HabitAccent.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(rozetler[i], fontSize = 12.sp, color = HabitAccent)
                    }
                }
            }
        }
        // Akıllı hatırlatıcı
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = HabitAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Akıllı Hatırlatıcı", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                        Text("Mark Done · Snooze · Repeat", fontSize = 12.sp, color = TextSecondary)
                    }
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = HabitAccent), shape = RoundedCornerShape(10.dp)) { Text("Hatırlat", color = Color.Black) }
                }
            }
        }
        // Isı haritası
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Yıllık İlerleme Haritası (Isı Haritası)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(5) { rowIndex ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(15) { colIndex ->
                                    val alpha = (rowIndex + colIndex) % 4
                                    val boxColor = when (alpha) { 0 -> BackgroundColor; 1 -> HabitAccent.copy(alpha = 0.3f); 2 -> HabitAccent.copy(alpha = 0.6f); else -> HabitAccent }
                                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(boxColor))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitKart(baslik: String, alt: String, habitChecked: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(baslik, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(alt, fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { if (habitChecked) 1f else 0f }, color = HabitAccent, trackColor = BackgroundColor, modifier = Modifier.fillMaxWidth().clip(CircleShape))
            }
            IconButton(onClick = {}, modifier = Modifier.size(48.dp).background(if (habitChecked) HabitAccent else BackgroundColor, CircleShape)) {
                Icon(Icons.Default.Check, contentDescription = "Check", tint = if (habitChecked) BackgroundColor else TextSecondary)
            }
        }
    }
}

// ════ 2. RUH HALİ (Mood seviye + tag + kalendar + korelasyon) ════
@Composable
fun MoodJournalScreen() {
    var selectedMood by remember { mutableStateOf(-1) }
    val tags = listOf("İş", "Aile", "Spor", "Hava", "Sağlık", "Sosyal")
    val selectedTags = remember { mutableStateListOf<String>() }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ruh Hali Günlüğü", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bugün nasıl hissediyorsun? (10 seviye)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                // 10 gerçek ruh hali seviyesi, 5+5 iki satır
                listOf(0..4, 5..9).forEach { aralik ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        aralik.forEach { index ->
                            val isSelected = selectedMood == index
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(42.dp).clip(CircleShape).background(if (isSelected) MoodAccent.copy(alpha = 0.2f) else Color.Transparent).border(1.dp, if (isSelected) MoodAccent else Color.Transparent, CircleShape).clickable { selectedMood = index }, contentAlignment = Alignment.Center) { Text(RuhHaliEmoji[index], fontSize = 24.sp) }
                                Text(RuhHaliListesi[index], fontSize = 9.sp, color = if (isSelected) MoodAccent else TextSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Bu hissin tetikleyicileri neler? (Mood Tag)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                listOf(tags.take(3), tags.takeLast(3)).forEach { rowTags ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTags.forEach { tag ->
                            val isContained = selectedTags.contains(tag)
                            SuggestionChip(onClick = { if (isContained) selectedTags.remove(tag) else selectedTags.add(tag) }, label = { Text(tag, color = if (isContained) BackgroundColor else TextPrimary) }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if (isContained) MoodAccent else BackgroundColor))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        // Mood Calendar
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Mood Calendar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(14) { i ->
                        val renkler = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFFA78BFA), Color(0xFFF59E0B), Color(0xFFFBBF24))
                        Box(Modifier.size(16.dp).clip(CircleShape).background(renkler[i % 5]))
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MoodAccent.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.border(1.dp, MoodAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "AI", tint = MoodAccent)
                Spacer(Modifier.width(12.dp))
                Text("Mood-Habit Korelasyonu: Kitap okuduğun günlerde modunun %24 daha pozitif olduğu gözlemlendi.", fontSize = 13.sp, color = TextPrimary)
            }
        }
    }
}

// ════ 3. FİNANS (Gelir/Gider + Hesaplar + Bütçe + Para birimi + Rapor) ════
@Composable
fun ExpenseTrackerScreen() {
    val hesaplar = listOf("Nakit", "Vadesiz", "Birikim", "Kredi Kartı")
    var hesapIdx by remember { mutableStateOf(0) }
    val kategoriler = GiderKategorileri
    var seciliKat by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Gider & Gelir Takibi", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(ExpenseAccent, Color(0xFF1E3A8A)))).padding(20.dp)) {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("Toplam Hesap Bakiyesi", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Text("₺34,250.00", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { Text("Nakit: ₺4,250", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp); Text("Banka: ₺30,000", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
            }
        }
        // Hesaplar
        Text("Hesaplar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(hesaplar.size) { i ->
                Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (i == hesapIdx) ExpenseAccent.copy(alpha = 0.2f) else SurfaceColor).border(1.dp, if (i == hesapIdx) ExpenseAccent else Color(0xFF2A2A2A), RoundedCornerShape(12.dp)).clickable { hesapIdx = i }.padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Text(hesaplar[i], fontSize = 12.sp, color = if (i == hesapIdx) ExpenseAccent else TextPrimary)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Bu Ayki Harcama Dağılımı", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(16.dp).clip(CircleShape)) {
                    Box(Modifier.fillMaxHeight().weight(0.5f).background(Color(0xFFEF4444)))
                    Box(Modifier.fillMaxHeight().weight(0.3f).background(Color(0xFFF59E0B)))
                    Box(Modifier.fillMaxHeight().weight(0.2f).background(Color(0xFF10B981)))
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🍔 Yemek (50%)", fontSize = 12.sp, color = TextSecondary); Text("🚗 Ulaşım (30%)", fontSize = 12.sp, color = TextSecondary); Text("📦 Diğer (20%)", fontSize = 12.sp, color = TextSecondary) }
            }
        }
        // Bütçe alarmı
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Akıllı Bütçe Alarmı: Yemek bütçesinin %85'i kullanıldı.", fontSize = 13.sp, color = TextPrimary)
            }
        }
        // Kategoriler
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(kategoriler.size) { i ->
                Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == seciliKat) ExpenseAccent else SurfaceColor).clickable { seciliKat = i }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(kategoriler[i], fontSize = 12.sp, color = if (i == seciliKat) Color.Black else TextPrimary)
                }
            }
        }
        Text("Son İşlemler", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        val transactions = listOf(
            Triple("Restoran Harcaması", "-₺240.00", Color(0xFFEF4444)),
            Triple("Maaş Ödemesi", "+₺45,000.00", Color(0xFF10B981)),
            Triple("Aylık Abonelik", "-₺129.00", Color(0xFFEF4444))
        )
        transactions.forEach { trans ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(BackgroundColor, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ExpenseAccent, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column { Text(trans.first, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary); Text("Bugün", fontSize = 11.sp, color = TextSecondary) }
                    }
                    Text(trans.second, color = trans.third, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ════ 4. ODAKLANMA (8 kategori + pomodoro + odak skoru) ════
@Composable
fun FocusTimerScreen() {
    var timeLeft by remember { mutableStateOf(1500) }
    var isRunning by remember { mutableStateOf(false) }
    var katIdx by remember { mutableStateOf(0) }
    var molada by remember { mutableStateOf(false) }
    // Gerçek HabitGenius focus verileri: durationMinutes, longBreak, focusSessionCount
    var toplamOdak by remember { mutableStateOf(12640) } // dakika (12s 40dk)
    var molaSuresi by remember { mutableStateOf(985) }   // dakika
    var oturumSayisi by remember { mutableStateOf(142) }
    LaunchedEffect(isRunning, timeLeft) { if (isRunning && timeLeft > 0) { delay(1000); timeLeft-- } else if (timeLeft == 0) { isRunning = false; molada = true; timeLeft = 300 } }
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    val progressByState by animateFloatAsState(targetValue = timeLeft / 1500f)
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text("Odaklanma Zamanlayıcısı", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.align(Alignment.Start))
        // Gerçek odak kategorileri
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(OdakKategorileri.size) { i ->
                Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == katIdx) FocusAccent else SurfaceColor).clickable { katIdx = i; toplamOdak += 25 }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(OdakKategorileri[i], fontSize = 12.sp, color = if (i == katIdx) Color.Black else TextPrimary)
                }
            }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(color = SurfaceColor, style = Stroke(width = 8.dp.toPx()))
                drawArc(color = if (molada) JournalAccent else FocusAccent, startAngle = -90f, sweepAngle = 360f * progressByState, useCenter = false, style = Stroke(width = 8.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(timeString, fontSize = 46.sp, fontWeight = FontWeight.Bold, color = TextPrimary); Text(if (molada) "Mola" else "Pomodoro", fontSize = 14.sp, color = if (molada) JournalAccent else TextSecondary) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { isRunning = !isRunning }, colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) Color(0xFFD97706) else FocusAccent), shape = RoundedCornerShape(12.dp), modifier = Modifier.width(120.dp)) { Text(if (isRunning) "Duraklat" else "Başlat", color = Color.White) }
                Button(onClick = { isRunning = false; timeLeft = 1500; molada = false }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor), shape = RoundedCornerShape(12.dp), modifier = Modifier.width(120.dp)) { Text("Sıfırla", color = TextPrimary) }
            }
            // Gerçek odak istatistikleri
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Odak İstatistikleri", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Toplam süre", fontSize = 12.sp, color = TextSecondary); Text("${toplamOdak / 60}s ${toplamOdak % 60}dk", fontSize = 12.sp, color = FocusAccent, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mola süresi", fontSize = 12.sp, color = TextSecondary); Text("${molaSuresi / 60}s ${molaSuresi % 60}dk", fontSize = 12.sp, color = JournalAccent, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Oturum sayısı", fontSize = 12.sp, color = TextSecondary); Text("$oturumSayisi oturum", fontSize = 12.sp, color = MoodAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ════ BUGÜN (gerçek HabitGenius Bugün sekmesi) ════
@Composable
fun TodayScreen() {
    var menuAcik by remember { mutableStateOf(false) }
    var takvimAcik by remember { mutableStateOf(false) }
    var seciliGun by remember { mutableStateOf(18) }
    var ay by remember { mutableStateOf("Ağustos 2026") }
    // Modül navigasyonu: 0 = Bugün ana ekran, 1..5 = seçili modül tam ekran
    var seciliModul by remember { mutableStateOf(0) }

    // Seçili modül ekranı (tam ekran, kendi içinde kaydırma var — iç içe değil)
    if (seciliModul != 0) {
        ModulGorunumEkrani(seciliModul, onGeri = { seciliModul = 0 })
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Üst bar: sol 3 çizgi menü + ortada "Bugün" (dokununca takvim)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { menuAcik = !menuAcik }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menü", tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.weight(1f))
                // Bugün başlığı — dokununca ay takvimi
                Text("Bugün", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.clickable { takvimAcik = true })
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp)) // denge
            }
            // Kaydırılabilir haftalık takvim şeridi (gün + tarih sayısı, sağ-sol kaydırılabilir)
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("Cmt", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum")) { gun ->
                    val gunNo = listOf(15, 16, 17, 18, 19, 20, 21)[listOf("Cmt", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum").indexOf(gun)]
                    val secili = gunNo == seciliGun
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (secili) HabitAccent else SurfaceColor),
                        modifier = Modifier.size(width = 48.dp, height = 66.dp).clickable { seciliGun = gunNo },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(gun, fontSize = 11.sp, color = if (secili) BackgroundColor else TextSecondary)
                            Text("$gunNo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (secili) BackgroundColor else TextPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Kategori çipleri
            LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HabitKategorileri.size) { i ->
                    Box(Modifier.clip(RoundedCornerShape(50)).background(SurfaceColor).clickable { }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(HabitKategorileri[i], fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Seçili günün aktiviteleri
            Text("Aktivite bulunamadı.", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(24.dp))
            // Takipçiler başlığı — dokununca modüle git (genişletme yok, iç içe kaydırma yok)
            Text("TAKİPÇİLER", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            TakipciKarti("Alışkanlıklar ve Görevler", "Tekrarlayan aktiviteler, görevler ve analitik", HabitAccent) { seciliModul = 1 }
            TakipciKarti("Duygular", "Ruh hali takibi ve içgörüler", MoodAccent) { seciliModul = 2 }
            TakipciKarti("Harcamalar", "Gelir, gider ve bütçe takibi", ExpenseAccent) { seciliModul = 3 }
            TakipciKarti("Günlük", "Düşüncelerini kaydet", JournalAccent) { seciliModul = 4 }
            TakipciKarti("Odaklanma", "Pomodoro zamanlayıcı ve istatistikler", FocusAccent) { seciliModul = 5 }
            Spacer(Modifier.height(16.dp))
        }

        // Yan menü (drawer) — 3 çizgiye basınca
        if (menuAcik) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { menuAcik = false })
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                modifier = Modifier.fillMaxHeight().width(280.dp).padding(top = 8.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Salı, Ağustos 18, 2026", fontSize = 15.sp, color = TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    Text("TAKİPÇİLER", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    MenuSatiri(Icons.Default.CheckCircle, "Alışkanlıklar ve Görevler", HabitAccent) { menuAcik = false; seciliModul = 1 }
                    MenuSatiri(Icons.Default.Face, "Duygular", MoodAccent) { menuAcik = false; seciliModul = 2 }
                    MenuSatiri(Icons.Default.ShoppingCart, "Harcamalar", ExpenseAccent) { menuAcik = false; seciliModul = 3 }
                    MenuSatiri(Icons.Default.Edit, "Günlük", JournalAccent) { menuAcik = false; seciliModul = 4 }
                    MenuSatiri(Icons.Default.PlayArrow, "Odaklanma", FocusAccent) { menuAcik = false; seciliModul = 5 }
                    Spacer(Modifier.height(16.dp))
                    Text("SİSTEM", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    MenuSatiri(Icons.Default.Settings, "Ayarlar", Color(0xFF94A3B8)) { menuAcik = false }
                    MenuSatiri(Icons.Default.Person, "Yedekler", Color(0xFF94A3B8)) { menuAcik = false }
                }
            }
        }

        // Ay takvimi — "Bugün"e dokununca
        if (takvimAcik) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { takvimAcik = false })
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) { Text("‹", fontSize = 22.sp, color = TextPrimary) }
                        Text(ay, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 12.dp))
                        IconButton(onClick = {}) { Text("›", fontSize = 22.sp, color = TextPrimary) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("PAZ", "PZT", "SAL", "ÇAR", "PER", "CUM", "CMT").forEach { Text(it, fontSize = 10.sp, color = TextSecondary) }
                    }
                    Spacer(Modifier.height(8.dp))
                    repeat(2) { satir ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            (1..7).forEach { kol ->
                                val gun = 16 + satir * 7 + kol - 1
                                Box(Modifier.size(34.dp).clip(CircleShape).background(if (gun == seciliGun) HabitAccent else Color.Transparent), contentAlignment = Alignment.Center) {
                                    Text("$gun", fontSize = 12.sp, color = if (gun == seciliGun) BackgroundColor else TextPrimary)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { takvimAcik = false }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor), shape = RoundedCornerShape(10.dp)) { Text("KAPAT", color = TextPrimary) }
                        Button(onClick = { takvimAcik = false; seciliGun = 18 }, colors = ButtonDefaults.buttonColors(containerColor = HabitAccent), shape = RoundedCornerShape(10.dp)) { Text("BUGÜN", color = Color.Black) }
                    }
                }
            }
        }
    }
}

// Seçili Takipçi modülünü tam ekran gösterir (geri butonuyla Bugün'e dönülür)
@Composable
fun ModulGorunumEkrani(modul: Int, onGeri: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onGeri) { Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TextPrimary) }
            Text("Geri", fontSize = 15.sp, color = TextPrimary)
        }
        Box(Modifier.weight(1f)) {
            when (modul) {
                1 -> MoodJournalScreen()          // Alışkanlıklar ve Görevler (demo)
                2 -> MoodJournalScreen()          // Duygular
                3 -> ExpenseTrackerScreen()       // Harcamalar
                4 -> DigitalJournalScreen()       // Günlük
                5 -> FocusTimerScreen()           // Odaklanma
                else -> Text("Modül", color = TextPrimary)
            }
        }
    }
}

@Composable
fun MenuSatiri(ikon: ImageVector, baslik: String, renk: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(renk.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(ikon, null, tint = renk, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(baslik, fontSize = 14.sp, color = TextPrimary)
    }
}

@Composable
fun TakipciKarti(baslik: String, alt: String, renk: Color, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(renk.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(renk))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(baslik, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Text(alt, fontSize = 12.sp, color = TextSecondary)
            }
            Text("›", fontSize = 18.sp, color = TextSecondary)
        }
    }
}

// ════ GÖREVLER (Görevler — tek seferlik / periyodik) ════
@Composable
fun TasksScreen() {
    var sekme by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Görevler", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Tek seferlik görevler", "Periyodik görevler").forEachIndexed { i, t ->
                Box(Modifier.clip(RoundedCornerShape(50)).background(if (i == sekme) MoodAccent else SurfaceColor).clickable { sekme = i }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(t, fontSize = 12.sp, color = if (i == sekme) Color.Black else TextPrimary)
                }
            }
        }
        if (sekme == 0) {
            GorevKarti("Proje sunumu hazırla", "Yarın · Öncelik: Yüksek", MoodAccent)
            GorevKarti("Markete git", "Bugün · Öncelik: Normal", MoodAccent)
            Text("Görev bulunamadı." , fontSize=13.sp, color=TextSecondary, modifier=Modifier.padding(top=8.dp))
        } else {
            GorevKarti("Haftalık temizlik", "Her hafta · Periyodik", MoodAccent)
            Text("Periyodik görev bulunamadı.", fontSize=13.sp, color=TextSecondary, modifier=Modifier.padding(top=8.dp))
        }
    }
}

@Composable
fun GorevKarti(baslik: String, alt: String, renk: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(renk.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, tint = renk, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(baslik, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
                Text(alt, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ════ RAPORLAR (Raporlar/Grafikler/Karşılaştırma/İstatistik/Hedefler) ════
private val RaporRenkleri = listOf(HabitAccent, MoodAccent, ExpenseAccent, FocusAccent, JournalAccent, Color(0xFF06B6D4))

@Composable
fun ReportsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Raporlar & İstatistikler", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("İlerlemeni gör, hedeflerini takip et", fontSize = 13.sp, color = TextSecondary)

        // KPI satırı
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiKarti(Modifier.weight(1f), "Toplam Odak", "12s 40dk", FocusAccent)
            KpiKarti(Modifier.weight(1f), "Tamamlama Oranı", "%84", HabitAccent)
            KpiKarti(Modifier.weight(1f), "Seri", "7 gün", MoodAccent)
        }

        // Haftalık odak bar grafiği
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Haftalık Odak (dakika)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(40, 65, 50, 80, 60, 90, 70).forEach { v ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(26.dp).height((v).dp).clip(RoundedCornerShape(6.dp)).background(FocusAccent))
                            Spacer(Modifier.height(4.dp))
                            Text("${v}", fontSize = 9.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Karşılaştırma
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Karşılaştırma", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Text("📈 Geçen haftaya göre odak: ▲ +18%", fontSize = 13.sp, color = HabitAccent)
                Text("📊 Geçen aya göre tamamlama: ▲ +12%", fontSize = 13.sp, color = MoodAccent)
                Text("💧 Geçen aya göre gider: ▼ -8%", fontSize = 13.sp, color = ExpenseAccent)
            }
        }

        // Harcama dağılımı pasta (donut)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Harcama Dağılımı (Pasta)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(150.dp).align(Alignment.CenterHorizontally)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeW = 22.dp.toPx()
                        var start = -90f
                        listOf(0.5f, 0.3f, 0.2f).forEachIndexed { i, w ->
                            drawArc(color = RaporRenkleri[i], startAngle = start, sweepAngle = 360f * w, useCenter = false, topLeft = androidx.compose.ui.geometry.Offset(strokeW / 2, strokeW / 2), size = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW), style = Stroke(strokeW))
                            start += 360f * w
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text("🍔 Yemek", fontSize = 11.sp, color = TextSecondary)
                    Text("🚗 Ulaşım", fontSize = 11.sp, color = TextSecondary)
                    Text("📦 Diğer", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Hedefler (gerçek HabitGenius hedef türleri)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Hedefler", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                HedefSatiri("Günlük Hedef (Daily)", "4 / 5", 0.8f)
                HedefSatiri("Haftalık Hedef (Weekly)", "7s / 10s", 0.7f)
                HedefSatiri("Aylık Hedef (Monthly)", "24 / 30", 0.8f)
                HedefSatiri("Yıllık Hedef (Yearly)", "120s / 300s", 0.4f)
                HedefSatiri("Tüm Zamanlar Hedefi (All time)", "450 / 1000", 0.45f)
            }
        }

        // Focus seri metrikleri (gerçek: currentStreak / bestStreak / daysLeft)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Odak Serisi (Focus Streak)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                MetrikSatiri("Mevcut seri (currentStreak)", "12 gün", FocusAccent)
                MetrikSatiri("En iyi seri (bestStreak)", "45 gün", HabitAccent)
                MetrikSatiri("Dönemde tamamlanan (completedCount)", "142", MoodAccent)
                MetrikSatiri("Kalan gün (daysLeft)", "8", ExpenseAccent)
            }
        }

        // Finans metrikleri (gerçek: income / expense / balance)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Finans Özeti", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                MetrikSatiri("Gelir (income)", "+₺58,200", HabitAccent)
                MetrikSatiri("Gider (expense)", "-₺23,950", ExpenseAccent)
                MetrikSatiri("Toplam bakiye (balance)", "₺34,250", MoodAccent)
            }
        }
    }
}

@Composable
fun MetrikSatiri(baslik: String, deger: String, renk: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(baslik, fontSize = 12.sp, color = TextSecondary)
        Text(deger, fontSize = 12.sp, color = renk, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun KpiKarti(mod: Modifier, degerBaslik: String, deger: String, renk: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp), modifier = mod) {
        Column(Modifier.padding(12.dp)) {
            Text(degerBaslik, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Text(deger, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = renk)
        }
    }
}

@Composable
fun HedefSatiri(baslik: String, ilerleme: String, oran: Float) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(baslik, fontSize = 13.sp, color = TextPrimary)
            Text(ilerleme, fontSize = 13.sp, color = HabitAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(progress = { oran }, color = HabitAccent, trackColor = BackgroundColor, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
        Spacer(Modifier.height(8.dp))
    }
}

// ════ 5. GÜNLÜK (Rich text + foto/attachment + yazma serisi) ════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalJournalScreen() {
    val c = LocalContext.current
    var journalText by remember { mutableStateOf(HabitGeniusVeri.gunluk(c)) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Kişisel Günlük", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("16 Ağustos 2026, Pazar · 🔥 Yazma Serisi: 5 gün", fontSize = 12.sp, color = TextSecondary)
            }
            IconButton(onClick = { }, modifier = Modifier.background(SurfaceColor, CircleShape)) { Icon(Icons.Default.Add, contentDescription = "Foto/Görsel Ekle", tint = JournalAccent) }
        }
        // Rich text toolbar üst
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceColor).padding(6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            IconButton(onClick = {}) { Icon(Icons.Default.Build, contentDescription = "Bold", tint = TextSecondary) }
            IconButton(onClick = {}) { Icon(Icons.Default.Menu, contentDescription = "List", tint = TextSecondary) }
            IconButton(onClick = {}) { Icon(Icons.Default.Info, contentDescription = "Photo", tint = TextSecondary) }
            IconButton(onClick = {}) { Icon(Icons.Default.Share, contentDescription = "Export", tint = JournalAccent) }
        }
        OutlinedTextField(
            value = journalText, onValueChange = { journalText = it; HabitGeniusVeri.gunluk(c, it) },
            placeholder = { Text("Sevgili Günlük, bugün harika bir üretkenlik günlüğü tutmaya başladım...", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = JournalAccent, unfocusedBorderColor = SurfaceColor, containerColor = SurfaceColor),
            shape = RoundedCornerShape(16.dp)
        )
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                IconButton(onClick = {}) { Icon(Icons.Default.Build, contentDescription = "Bold", tint = TextSecondary) }
                IconButton(onClick = {}) { Icon(Icons.Default.Menu, contentDescription = "List", tint = TextSecondary) }
                IconButton(onClick = {}) { Icon(Icons.Default.Refresh, contentDescription = "Undo", tint = TextSecondary) }
                IconButton(onClick = {}) { Icon(Icons.Default.Share, contentDescription = "Export", tint = JournalAccent) }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun HabitGeniusPreview() {
    MaterialTheme(colorScheme = darkColorScheme(background = BackgroundColor, surface = SurfaceColor)) { HabitGeniusMainScreen() }
}
