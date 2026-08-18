package com.gunlukasistan.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri

/**
 * v7.86 — Zamanlayıcının tüm ayarları.
 *
 * ── Kullanıcının isteği (ekran görüntüsüyle) ──
 * Google Saat'in Zamanlayıcı ayarları gibi: sistem sesi kapalıyken
 * titreşime düşme, bitiş sesi, titreşim, mini zamanlayıcı, yaklaşan
 * bitiş bildirimi.
 *
 * ── Neden ayrı dosya ──
 * Ayarlar bugüne kadar üç yere dağılmıştı: [TimerEngine] (ses indeksi),
 * [Store] (`getVibEnabled`, `getSoundEnabled`) ve [ZorunluUyari] (ısrarlı
 * uyarı). Kullanıcı "zamanlayıcının her şeyini ayarlayabileyim" dediği
 * için hepsinin **tek ekrandan** yönetilmesi gerekiyordu. Bu sınıf o
 * ekranın veri katmanı.
 *
 * Mevcut anahtarlara dokunulmadı — [Store.getVibEnabled] hâlâ genel
 * titreşim ayarı. Buradaki [titresim] yalnızca zamanlayıcıya özel ve
 * ikisi birlikte değerlendiriliyor ([titresimEtkinMi]).
 */
object SayacAyar {

    private const val PREF = "sayac_ayar_v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // ═══════════════════════════════════════════════════════════════
    // SES
    // ═══════════════════════════════════════════════════════════════

    /** Süre bitince ses çalsın mı. */
    fun ses(context: Context): Boolean = prefs(context).getBoolean("ses", true)

    fun setSes(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("ses", acik).apply()
    }

    /** Seçilen zil sesi (URI metni). Boş = sistem varsayılan alarmı. */
    fun sesUri(context: Context): String = prefs(context).getString("ses_uri", "") ?: ""

    fun sesAdi(context: Context): String =
        prefs(context).getString("ses_adi", "") ?: ""

    fun setSesSecimi(context: Context, uri: String, ad: String) {
        prefs(context).edit().putString("ses_uri", uri).putString("ses_adi", ad).apply()
    }

    /** Çalınacak sesin adresi — seçim yoksa sistem alarm sesi. */
    fun cozulmusSesUri(context: Context): Uri? = try {
        val secili = sesUri(context)
        if (secili.isNotBlank()) Uri.parse(secili)
        else android.media.RingtoneManager
            .getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
    } catch (e: Exception) {
        android.util.Log.w("SayacAyar", "Ses adresi çözülemedi", e)
        null
    }

    /** Bitiş sesinin kaç saniye çalacağı. */
    fun sesSureSn(context: Context): Int =
        prefs(context).getInt("ses_sure", 15).coerceIn(3, 120)

    fun setSesSureSn(context: Context, sn: Int) {
        prefs(context).edit().putInt("ses_sure", sn.coerceIn(3, 120)).apply()
    }

    /** Ses gittikçe yükselsin mi — ani gürültü yerine yumuşak uyanma. */
    fun kademeliSes(context: Context): Boolean =
        prefs(context).getBoolean("kademeli", true)

    fun setKademeliSes(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("kademeli", acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // TİTREŞİM
    // ═══════════════════════════════════════════════════════════════

    fun titresim(context: Context): Boolean = prefs(context).getBoolean("titresim", true)

    fun setTitresim(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("titresim", acik).apply()
    }

    /**
     * Titreşim gerçekten çalışacak mı?
     *
     * Hem zamanlayıcıya özel ayar hem de uygulamanın genel titreşim
     * ayarı açık olmalı. Genel ayarı yok saymak, kullanıcının "hiç
     * titreşim istemiyorum" tercihini çiğnemek olurdu.
     */
    fun titresimEtkinMi(context: Context): Boolean =
        titresim(context) && Store.getVibEnabled(context)

    /** Titreşim deseni: 0 kısa · 1 normal · 2 uzun/ısrarcı. */
    fun titresimDeseni(context: Context): Int =
        prefs(context).getInt("t_desen", 1).coerceIn(0, 2)

    fun setTitresimDeseni(context: Context, d: Int) {
        prefs(context).edit().putInt("t_desen", d.coerceIn(0, 2)).apply()
    }

    fun desenAdi(context: Context, d: Int): String = context.getString(
        when (d) {
            0 -> R.string.sa_desen_kisa
            2 -> R.string.sa_desen_uzun
            else -> R.string.sa_desen_normal
        }
    )

    fun desenDizisi(d: Int): LongArray = when (d) {
        0 -> longArrayOf(0, 220, 120, 220)
        2 -> longArrayOf(0, 600, 200, 600, 200, 600, 200, 900)
        else -> longArrayOf(0, 450, 180, 450, 180, 700)
    }

    // ═══════════════════════════════════════════════════════════════
    // SESSİZ MOD DAVRANIŞI
    // ═══════════════════════════════════════════════════════════════

    /**
     * "Sistem sesi kapalıyken alarmları sustur."
     *
     * Ekran görüntüsündeki ilk seçenek. Açıkken telefon sessiz/titreşim
     * modundaysa **ses çalmaz, titreşim verilir**. Kapalıyken zamanlayıcı
     * sessiz modu yok sayar ve yine de çalar (alarm kanalı üzerinden).
     */
    fun sessizdeSustur(context: Context): Boolean =
        prefs(context).getBoolean("sessizde_sustur", true)

    fun setSessizdeSustur(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("sessizde_sustur", acik).apply()
    }

    /** Telefon şu an sessiz ya da titreşim modunda mı. */
    fun telefonSessizMi(context: Context): Boolean = try {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        am?.ringerMode != AudioManager.RINGER_MODE_NORMAL
    } catch (e: Exception) {
        false
    }

    /**
     * Şu anki koşullarda ses çalınmalı mı?
     *
     * Ses ayarı kapalıysa → hayır.
     * Sessiz moddayız ve "sessizde sustur" açıksa → hayır (titreşim kalır).
     */
    fun sesCalinsinMi(context: Context): Boolean {
        if (!ses(context)) return false
        if (sessizdeSustur(context) && telefonSessizMi(context)) return false
        return true
    }

    /** Ses akışı — sessiz modu delmek gerekiyorsa ALARM kullanılır. */
    fun sesNiteligi(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // ═══════════════════════════════════════════════════════════════
    // MİNİ ZAMANLAYICI
    // ═══════════════════════════════════════════════════════════════

    /**
     * "Mini zamanlayıcıyı göster" — diğer ekranlarda küçük bir sayaç
     * rozeti. Ekran görüntüsündeki seçeneğin karşılığı.
     */
    fun miniGoster(context: Context): Boolean =
        prefs(context).getBoolean("mini", true)

    fun setMiniGoster(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("mini", acik).apply()
    }

    /**
     * v7.92 — Ön plan servisi kullanılsın mı?
     *
     * ── Neden kapatılabilir oldu ──
     * v7.88'de bildirimi "kaldırılamaz" yapmak için ön plan servisi
     * eklendi. Ama bazı cihazlarda (özellikle Samsung One UI) ön plan
     * servisinin sahip olduğu bildirim panelde hiç görünmüyor: sistem onu
     * "arka planda çalışan uygulamalar" grubuna katlıyor.
     *
     * Kullanıcının kanıtı kesindi: **duraklatınca bildirim görünüyor**
     * (sıradan `notify()` yolu), **çalışırken görünmüyor** (servis yolu).
     * Yani sorun kanal/izin değil, servisin bildirime sahip olmasıydı.
     *
     * ── Neden varsayılan KAPALI ──
     * Görünürlük, "kaldırılamazlık"tan daha önemli. Sayacın doğruluğu
     * zaten servise bağlı değil: süre [TimerEngine] içinde duvar saatiyle
     * hesaplanıyor, bitiş [TimerAlarm] ile kesin alarma bağlı. Servis
     * ölse bile sayaç doğru kalır ve bitiş bildirimi gelir.
     */
    fun onPlanServisi(context: Context): Boolean =
        prefs(context).getBoolean("on_plan", false)

    fun setOnPlanServisi(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("on_plan", acik).apply()
    }

    /**
     * v7.93 — Uyumluluk modu.
     *
     * ── Kanıta dayalı teşhis ──
     * Kullanıcının cihazında **duraklatılmış** bildirim görünüyor,
     * **çalışan** bildirim görünmüyordu. İkisi arasındaki tek yapısal fark:
     *   · çalışırken  → `setOngoing(true)` + `setUsesChronometer(true)`
     *   · duraklatınca → ikisi de yok
     *
     * Samsung One UI, kronometreli/ongoing bildirimleri "Canlı bildirimler"
     * (Now Bar) alanına yönlendiriyor ve normal bildirim listesinden
     * çıkarıyor. Uygulama bu alana kabul edilmezse bildirim hiçbir yerde
     * görünmüyor.
     *
     * ── Uyumluluk modu ne yapar ──
     * Çalışan bildirimi, kanıtlanmış biçimde görünen duraklatılmış
     * bildirimle **yapısal olarak aynı** hâle getirir: kronometre yok,
     * ongoing yok, ön plan servisi yok.
     *
     * Kayıp: sayaç sistem tarafından saniye saniye sayılmaz. Bunun yerine
     * metin periyodik tazelenir ve **bitiş saati** yazılır — bitiş saati
     * hiç bayatlamaz, tazeleme gecikse bile doğru bilgi ekranda kalır.
     */
    fun uyumlulukModu(context: Context): Boolean =
        prefs(context).getBoolean("uyumluluk", true)

    fun setUyumlulukModu(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("uyumluluk", acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // YAKLAŞAN BİTİŞ UYARISI
    // ═══════════════════════════════════════════════════════════════

    /** Bitişten kaç saniye önce hafif uyarı. 0 = kapalı. */
    fun yaklasanSn(context: Context): Int =
        prefs(context).getInt("yaklasan", 0).coerceIn(0, 300)

    fun setYaklasanSn(context: Context, sn: Int) {
        prefs(context).edit().putInt("yaklasan", sn.coerceIn(0, 300)).apply()
    }

    fun yaklasanAdi(context: Context, sn: Int): String = when (sn) {
        0 -> context.getString(R.string.sa_kapali)
        else -> context.getString(R.string.sa_saniye_once, sn)
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.2 — BİTİŞ DENEYİMİ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Uyanık bitiş — süre dolunca telefonu uyandıran tam ekran alarm.
     *
     * Samsung Saat'in bitişi gibi: ekran kapalıyken/kilitliyken bile
     * tam ekran "Süre doldu" açılır. Android 14+'ta sistem izni
     * gerekebilir (`canUseFullScreenIntent`); izin yoksa normal
     * yüksek önemli bildirime düşülür — ikisi de [TimerNotifier]'da.
     */
    fun uyanikBitis(context: Context): Boolean =
        prefs(context).getBoolean("uyanik_bitis", true)

    fun setUyanikBitis(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("uyanik_bitis", acik).apply()
    }

    /**
     * Flaş bildirimi — süre bitince kamera flaşı kısaca çakar.
     * Sessiz ortamlarda (kütüphane, sınıf) ses yerine ışıklı uyarı.
     * Varsayılan KAPALI: çoğu kullanıcı için sürpriz olmasın.
     */
    fun flasBildirim(context: Context): Boolean =
        prefs(context).getBoolean("flas", false)

    fun setFlasBildirim(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("flas", acik).apply()
    }

    /**
     * Başlangıç ritüeli — "Başlat"a basınca 3-2-1 geri sayım kartı.
     * Google Timer hissi; odak psikolojisine giriş köprüsü.
     */
    fun baslangic321(context: Context): Boolean =
        prefs(context).getBoolean("baslangic321", true)

    fun setBaslangic321(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("baslangic321", acik).apply()
    }

    /**
     * v10.4 · A9 — sesli geri sayım (TTS). Yalnız sayaç ekranı
     * öndeyken konuşur; varsayılan kapalı (sürpriz ses olmamalı).
     */
    fun tts(context: Context): Boolean = prefs(context).getBoolean("tts", false)

    fun setTts(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("tts", acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // KİMLİK (v10.4 · A5)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Oturum etiketi — "Odak", "Ders", "Kodlama" gibi. Boş bırakılırsa
     * yüzeyler varsayılan metinlerine düşer. Bildirimde alt metin
     * olarak görünür; 24 karakterle sınırlıdır.
     */
    fun etiket(context: Context): String =
        prefs(context).getString("etiket", "") ?: ""

    fun setEtiket(context: Context, yeni: String) {
        prefs(context).edit().putString("etiket", yeni.take(24).trim()).apply()
    }

    /**
     * Etikete eklenen bildirim alt metni. Boş etikette boş döner —
     * çağıran setSubText'i atlamayı seçsin.
     */
    fun etiketAltMetin(context: Context): String {
        val e = etiket(context)
        return if (e.isBlank()) "" else "🏷️ $e"
    }

    // ═══════════════════════════════════════════════════════════════
    // EKRAN VE DAVRANIŞ
    // ═══════════════════════════════════════════════════════════════

    /** Sayaç ekranı açıkken ekran sönmesin. */
    fun ekranAcikKalsin(context: Context): Boolean =
        prefs(context).getBoolean("ekran_acik", false)

    fun setEkranAcikKalsin(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("ekran_acik", acik).apply()
    }

    /** Süre bitince tam ekran uyarı açılsın mı. */
    fun tamEkranUyari(context: Context): Boolean =
        prefs(context).getBoolean("tam_ekran", false)

    fun setTamEkranUyari(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("tam_ekran", acik).apply()
    }

    /** Varsayılan süre (dakika) — yeni sayaç bununla başlar. */
    fun varsayilanDk(context: Context): Int =
        prefs(context).getInt("varsayilan_dk", 25).coerceIn(1, 600)

    fun setVarsayilanDk(context: Context, dk: Int) {
        prefs(context).edit().putInt("varsayilan_dk", dk.coerceIn(1, 600)).apply()
    }

    /**
     * Süre bitince otomatik yeniden başlasın mı (pomodoro döngüsü).
     *
     * Varsayılan kapalı: beklenmedik biçimde yeniden başlayan bir sayaç
     * kullanıcıyı şaşırtır; isteyen bilerek açsın.
     */
    fun otomatikTekrar(context: Context): Boolean =
        prefs(context).getBoolean("oto_tekrar", false)

    fun setOtomatikTekrar(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("oto_tekrar", acik).apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // ÖZET
    // ═══════════════════════════════════════════════════════════════

    /** Ayarlar satırında gösterilecek kısa durum. */
    fun ozet(context: Context): String {
        val parcalar = mutableListOf<String>()
        parcalar.add(
            context.getString(
                if (ses(context)) R.string.sa_ozet_ses_acik else R.string.sa_ozet_ses_kapali
            )
        )
        if (titresimEtkinMi(context)) {
            parcalar.add(context.getString(R.string.sa_ozet_titresim))
        }
        parcalar.add(context.getString(R.string.koc_dk, varsayilanDk(context)))
        return parcalar.joinToString(" · ")
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.41 · Kullanıcı maddesi #2 — KADRAN YAZI BOYUTU
    // ═══════════════════════════════════════════════════════════════
    //
    // Kadran metinleri özel View'da px ile çizildiği için sistem/uygulama
    // yazı ölçeğinden etkilenmiyor; kullanıcı "orantısız büyük" diye
    // şikayet etti. Dört kademe: ×0,80 · ×0,90 · ×1,00 · ×1,15.

    private const val K_KADRAN_OLCEK = "kadran_yazi_olcek"

    /** Kademe → çarpan (saf — birim testli). Sınır dışı kenetlenir. */
    fun kadranCarpani(kademe: Int): Float = when (kademe.coerceIn(0, 3)) {
        0 -> 0.80f
        1 -> 0.90f
        2 -> 1.00f
        else -> 1.15f
    }

    /** Ayar satırında görünen yüzde (saf — birim testli). */
    fun kadranYuzde(kademe: Int): Int = (kadranCarpani(kademe) * 100).toInt()

    fun kadranYaziKademe(context: Context): Int =
        prefs(context).getInt(K_KADRAN_OLCEK, 2).coerceIn(0, 3)

    fun setKadranYaziKademe(context: Context, kademe: Int) {
        prefs(context).edit().putInt(K_KADRAN_OLCEK, kademe.coerceIn(0, 3)).apply()
    }

    /** [SayacKadraniView.yaziOlcek]'e bağlanan canlı değer. */
    fun kadranOlcek(context: Context): Float = kadranCarpani(kadranYaziKademe(context))

    /** Seçim diyaloğundaki etiket. */
    fun kadranOlcekAdi(context: Context, kademe: Int): String = context.getString(
        when (kademe.coerceIn(0, 3)) {
            0 -> R.string.w41_kadran_kucuk
            1 -> R.string.w41_kadran_orta
            2 -> R.string.w41_kadran_varsayilan
            else -> R.string.w41_kadran_buyuk
        }
    )

    // ═══════════════════════════════════════════════════════════════
    // v10.50 — 10 AŞIRI İŞLEVSEL ODAK & ZAMANLAYICI AYARI (#1..#10)
    // ═══════════════════════════════════════════════════════════════

    // #1 Akıllı Kesinti Günlüğü
    fun kesintiKaydiAcik(c: Context): Boolean = prefs(c).getBoolean("kesinti_kaydi_acik", true)
    fun setKesintiKaydiAcik(c: Context, v: Boolean) = prefs(c).edit().putBoolean("kesinti_kaydi_acik", v).apply()

    // #4 Akıllı Taşma (Overrun) Modu
    fun tasmaAcik(c: Context): Boolean = prefs(c).getBoolean("tasma_acik", true)
    fun setTasmaAcik(c: Context, v: Boolean) = prefs(c).edit().putBoolean("tasma_acik", v).apply()

    // #5 Oturum Sonu Çıktı Hasadı
    fun ciktiHasadiAcik(c: Context): Boolean = prefs(c).getBoolean("cikti_hasadi_acik", true)
    fun setCiktiHasadiAcik(c: Context, v: Boolean) = prefs(c).edit().putBoolean("cikti_hasadi_acik", v).apply()

    // #6 Binaural Ritim Modu (0: Kapalı, 1: Alfa 10Hz, 2: Gama 40Hz)
    fun binauralMod(c: Context): Int = prefs(c).getInt("binaural_mod", 0).coerceIn(0, 2)
    fun setBinauralMod(c: Context, m: Int) = prefs(c).edit().putInt("binaural_mod", m.coerceIn(0, 2)).apply()

    // #7 Çarpışma Bekçisi
    fun carpismaBekcisiAcik(c: Context): Boolean = prefs(c).getBoolean("carpisma_bekcisi_acik", true)
    fun setCarpismaBekcisiAcik(c: Context, v: Boolean) = prefs(c).edit().putBoolean("carpisma_bekcisi_acik", v).apply()

    // #9 Masaya Dönüş Geri Sayımı
    fun masayaDonusAcik(c: Context): Boolean = prefs(c).getBoolean("masaya_donus_acik", true)
    fun setMasayaDonusAcik(c: Context, v: Boolean) = prefs(c).edit().putBoolean("masaya_donus_acik", v).apply()

    // ── v10.50 #1: Kesinti Kayıtları ───────────────────────────────
    fun kesintiKaydet(c: Context, sebep: OdakMotoru.KesintiSebep, sureSn: Int = 0) {
        val ham = prefs(c).getString("kesinti_listesi", "") ?: ""
        val dize = "${sebep.name}|$sureSn|${System.currentTimeMillis()}"
        val yeni = if (ham.isBlank()) dize else "$ham;$dize"
        prefs(c).edit().putString("kesinti_listesi", yeni).apply()
    }

    fun kesintiListesi(c: Context): List<OdakMotoru.KesintiKaydi> {
        val ham = prefs(c).getString("kesinti_listesi", "") ?: ""
        if (ham.isBlank()) return emptyList()
        return ham.split(";").mapNotNull { par ->
            val p = par.split("|")
            if (p.size == 3) {
                val s = runCatching { OdakMotoru.KesintiSebep.valueOf(p[0]) }.getOrNull()
                val sn = p[1].toIntOrNull() ?: 0
                val ms = p[2].toLongOrNull() ?: 0L
                if (s != null) OdakMotoru.KesintiKaydi(s, sn, ms) else null
            } else null
        }
    }

    fun kesintileriTemizle(c: Context) {
        prefs(c).edit().remove("kesinti_listesi").apply()
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.83 — KULLANICI SEÇİMLİ HAZIR SÜRELER (PRESETLER) & TEMA / SES KONTROLÜ
    // ═══════════════════════════════════════════════════════════════

    /** Buton 1, Buton 2 ve Buton 3 için kullanıcının tanımladığı dakika süreleri. */
    fun presetlerGetir(context: Context): List<Int> {
        val sp = prefs(context)
        val p1 = sp.getInt("preset_1", 5).coerceIn(1, 300)
        val p2 = sp.getInt("preset_2", 10).coerceIn(1, 300)
        val p3 = sp.getInt("preset_3", 25).coerceIn(1, 300)
        return listOf(p1, p2, p3)
    }

    fun presetlerKaydet(context: Context, p1: Int, p2: Int, p3: Int) {
        val sp = prefs(context)
        sp.edit()
            .putInt("preset_1", p1.coerceIn(1, 300))
            .putInt("preset_2", p2.coerceIn(1, 300))
            .putInt("preset_3", p3.coerceIn(1, 300))
            .apply()
    }

    /**
     * Odak sesleri ve alev temaları vb. görsel saat temalarının açık olup olmadığı.
     * Kullanıcının talimatı doğrultusunda varsayılan olarak KAPALIDIR (false).
     */
    fun odakSesVeTemaAcikMi(context: Context? = null): Boolean {
        if (context == null) return false
        return try { prefs(context).getBoolean("odak_ses_ve_tema", false) } catch (_: Exception) { false }
    }

    fun setOdakSesVeTemaAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("odak_ses_ve_tema", acik).apply()
    }

    fun odakSesVeTemaDurumMetni(context: Context? = null): Pair<String, String> {
        val acik = odakSesVeTemaAcikMi(context)
        return if (acik) {
            Pair(
                "🔥 / 🎧 Odak Sesleri & Görsel Saat Temaları (AÇIK)",
                "AÇIK — Sayaç ekranında odak müzikleri ve alev vb. görsel saat temaları gösteriliyor."
            )
        } else {
            Pair(
                "🔥 / 🎧 Odak Sesleri & Görsel Saat Temaları (KAPALI)",
                "KAPALI — Sayaç ekranı sadeleştirildi; odak müzikleri, alev ve görsel temalar gizlendi. Açmak için dokunun."
            )
        }
    }

    /**
     * v10.84 — Arka Plan Medya Kumandası (YouTube, Spotify, Karnaval Radyo vb.)
     * Sayaç ekranında Durdur/Başlat, İleri ve Geri butonlarını gösterir.
     * Kullanıcı talebi doğrultusunda varsayılan olarak AÇIKTIR (true).
     */
    fun arkaPlanMedyaKumandasiAcikMi(context: Context? = null): Boolean {
        if (context == null) return true
        return try { prefs(context).getBoolean("arka_plan_medya_kumandasi", true) } catch (_: Exception) { true }
    }

    fun setArkaPlanMedyaKumandasiAcik(context: Context, acik: Boolean) {
        prefs(context).edit().putBoolean("arka_plan_medya_kumandasi", acik).apply()
    }

    /**
     * v11.00 — Zamanlayıcıda alarm çalınca alarmı durdurmak için telefon kapatma (güç) tuşuna
     * bir kez basıldığında alarmın o an susturulup susturulmayacağı anahtarı.
     * Varsayılan: true (açık).
     */
    fun isKapatmaTusuyleAlarmDurdur(context: Context? = null): Boolean {
        if (context == null) return true
        return try { prefs(context).getBoolean("kapatma_tusuyle_alarm_durdur", true) } catch (_: Exception) { true }
    }

    fun setKapatmaTusuyleAlarmDurdur(context: Context? = null, acik: Boolean) {
        if (context == null) return
        try { prefs(context).edit().putBoolean("kapatma_tusuyle_alarm_durdur", acik).apply() } catch (_: Exception) {}
    }

    fun kapatmaTusuyleAlarmDurdurDurumMetni(context: Context? = null): Pair<String, String> {
        val acik = isKapatmaTusuyleAlarmDurdur(context)
        return if (acik) {
            Pair(
                "🔘 Telefon Kapatma / Güç Tuşuyla Alarmları Durdur (AÇIK)",
                "AÇIK — Zamanlayıcı alarmı çalarken telefon kapatma / güç tuşuna bir kere basıldığında alarm o an anında susturulur."
            )
        } else {
            Pair(
                "🔘 Telefon Kapatma / Güç Tuşuyla Alarmları Durdur (KAPALI)",
                "KAPALI — Alarm çalarken güç tuşu alarmı susturmaz; ekrandaki durdurma butonunu kullanmak gerekir."
            )
        }
    }

    // v11.02 — Çalışma Zamanı Ekranı Tek Ekran / Kompakt Mod
    private const val K_TEK_EKRAN_KOMPAKT_MOD = "tek_ekran_kompakt_mod_v1"

    fun isTekEkranKompaktMod(c: Context? = null): Boolean {
        if (c == null) return true
        return try { prefs(c).getBoolean(K_TEK_EKRAN_KOMPAKT_MOD, true) } catch (_: Exception) { true }
    }

    fun setTekEkranKompaktMod(c: Context? = null, acik: Boolean) {
        if (c == null) return
        try { prefs(c).edit().putBoolean(K_TEK_EKRAN_KOMPAKT_MOD, acik).apply() } catch (_: Exception) {}
    }

    fun tekEkranKompaktModDurumMetni(c: Context? = null): Pair<String, String> {
        val acik = isTekEkranKompaktMod(c)
        return if (acik) {
            Pair(
                "📱 Çalışma Zamanı Tek Ekran / Kompakt Mod (AÇIK)",
                "AÇIK — Saat kadrani küçültülür, süre ve yazılar hemen altında yer alır ve tüm ekran aşağı kaydırmasız tek ekrana sığar."
            )
        } else {
            Pair(
                "📱 Çalışma Zamanı Tek Ekran / Kompakt Mod (KAPALI)",
                "KAPALI — Saat kadrani tam genişlikte çizilir."
            )
        }
    }

    fun arkaPlanMedyaKumandasiDurumMetni(context: Context? = null): Pair<String, String> {
        val acik = arkaPlanMedyaKumandasiAcikMi(context)
        return if (acik) {
            Pair(
                "🎵 Arka Plan Müzik / Radyo Kumandası (AÇIK)",
                "AÇIK — Sayaç ekranında YouTube, Spotify, Karnaval Radyo vb. için Durdur/Başlat, İleri ve Geri kumandası gösteriliyor."
            )
        } else {
            Pair(
                "🎵 Arka Plan Müzik / Radyo Kumandası (KAPALI)",
                "KAPALI — Sayaç ekranında arka plan medya kontrolcüsü gizlendi. Açmak için dokunun."
            )
        }
    }

    /**
     * v11.05: Kalan süreyi dakika ve saniye cinsinden ("18:45 kaldı (18 dk 45 sn)") olarak biçimlendirir.
     */
    fun kalanSureDakikaSaniyeMetni(millis: Long): String {
        val totalSeconds = millis / 1000L
        val m = totalSeconds / 60L
        val s = totalSeconds % 60L
        val mStr = if (m < 10) "0$m" else "$m"
        val sStr = if (s < 10) "0$s" else "$s"
        return "$mStr:$sStr kaldı ($m dk $s sn)"
    }
}
