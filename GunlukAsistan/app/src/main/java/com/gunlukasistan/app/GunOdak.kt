package com.gunlukasistan.app

import android.content.Context
import java.util.Calendar

/**
 * v9.9 — Günün tek odağı (görsel öneri 7).
 *
 * ══════════════════════════════════════════════════════════════════
 * SORUN — HER ŞEY EŞİT AĞIRLIKTA
 * ══════════════════════════════════════════════════════════════════
 * Ana ekran şu an sekiz blok gösteriyor: geri sayım, kurslar,
 * rozetler, istatistikler, grafik, ızgara, konular, kısayollar.
 * Hepsi aynı boyutta, aynı ağırlıkta.
 *
 * Uygulamayı açtığında gözün gidecek **tek bir nokta yok**. Kullanıcı
 * ekranı tarayıp "ne yapmalıyım" sorusunu kendi cevaplamak zorunda.
 *
 * Bu sınıf o soruyu uygulama adına cevaplıyor: **şu an en önemli tek
 * şey ne?**
 *
 * ══════════════════════════════════════════════════════════════════
 * ÖNCELİK SIRASI — VE NEDEN BU SIRA
 * ══════════════════════════════════════════════════════════════════
 * Sıralama "aciliyet × kaçırılma maliyeti" mantığına göre:
 *
 *  1. **Geçmiş ilaç/fatura** — kaçırılırsa geri alınamaz (sağlık, para)
 *  2. **Bugünkü ilaç saati**  — zamana bağlı, ertelenemez
 *  3. **Bugün biten görev**   — söz verilmiş iş
 *  4. **Bekleyen tekrar**     — kaçırılırsa öğrenme kaybı (SM-2 eğrisi)
 *  5. **Sınava az kaldı**     — uzun vadeli ama yaklaşıyor
 *  6. **Günlük hedef**        — esnek, gün içinde yapılabilir
 *  7. **Seri riski**          — motive edici, kritik değil
 *
 * Sağlık ve para en üstte çünkü bunların telafisi yok. Öğrenme
 * kaybı telafi edilebilir; motivasyon en altta çünkü ertelemenin
 * maliyeti yok.
 *
 * ══════════════════════════════════════════════════════════════════
 * NEDEN TEK ÖNERİ
 * ══════════════════════════════════════════════════════════════════
 * Üç öneri göstermek hiç göstermemekle aynı: kullanıcı yine seçim
 * yapmak zorunda kalır. Tek bir şey söylemek karar yükünü kaldırıyor.
 *
 * Diğer bekleyenler zaten kendi ekranlarında duruyor; buradaki
 * amaç "en önemlisi" filtresi.
 */
object GunOdak {

    private const val TAG = "GunOdak"

    /**
     * Odak önerisi.
     *
     * @param metin kullanıcıya gösterilecek cümle
     * @param emoji satır başındaki simge
     * @param ekranIndeksi dokunulunca gidilecek ana ekran sekmesi (-1 = yok)
     * @param aktivite dokunulunca açılacak Activity (null = ekranIndeksi kullan)
     * @param aciliyet 0 = bilgi · 1 = dikkat · 2 = acil
     */
    data class Odak(
        val metin: String,
        val emoji: String,
        val ekranIndeksi: Int = -1,
        val aktivite: Class<*>? = null,
        val aciliyet: Int = 0
    )

    /**
     * Şu anki en önemli tek şeyi döndürür.
     *
     * ⚠️ Disk okuyor (görevler, takip kayıtları, tekrar programı).
     * Ana iş parçacığında çağrılabilir ama arka plan tercih edilir.
     *
     * @return öneri veya null (yapılacak bir şey yoksa)
     */
    fun bul(context: Context): Odak? {
        // ── 1. Geçmiş kalmış takip kaydı (ilaç/fatura/belge) ──
        runCatching {
            val uyarilar = Takip.uyarilar(context)
            val acil = uyarilar.firstOrNull { it.seviye == 2 }
            if (acil != null) {
                return Odak(
                    metin = context.getString(
                        R.string.od_takip_acil, acil.kayit.ad, acil.mesaj
                    ),
                    emoji = acil.kayit.tur.emoji,
                    aktivite = TakipActivity::class.java,
                    aciliyet = 2
                )
            }
        }

        // ── 2. Bugün alınacak ilaç (saati geçmemiş) ──
        runCatching {
            val simdi = Calendar.getInstance()
            val suAnDk = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE)
            val ilaclar = Takip.turdekiler(context, Takip.Tur.ILAC)
            // Önümüzdeki 2 saat içinde alınacak doz var mı
            val yaklasan = ilaclar.flatMap { k -> k.saatler.map { k to it } }
                .filter { (_, dk) -> dk in suAnDk..(suAnDk + 120) }
                .minByOrNull { it.second }
            if (yaklasan != null) {
                return Odak(
                    metin = context.getString(
                        R.string.od_ilac,
                        yaklasan.first.ad,
                        Takip.saatMetni(yaklasan.second)
                    ),
                    emoji = "💊",
                    aktivite = TakipActivity::class.java,
                    aciliyet = 1
                )
            }
        }

        // ── 3. Bugün biten görev ──
        runCatching {
            val simdi = System.currentTimeMillis()
            val gunSonu = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
            val bugunkuler = Store.loadTasks(context)
                .filter { !it.done && it.dueAt in 1..gunSonu }
                .sortedBy { it.dueAt }
            if (bugunkuler.isNotEmpty()) {
                val ilk = bugunkuler.first()
                val gecmis = ilk.dueAt < simdi
                return Odak(
                    metin = if (bugunkuler.size == 1) {
                        context.getString(
                            if (gecmis) R.string.od_gorev_gecmis else R.string.od_gorev_tek,
                            ilk.text.take(40)
                        )
                    } else {
                        context.getString(
                            R.string.od_gorev_coklu,
                            ilk.text.take(30), bugunkuler.size - 1
                        )
                    },
                    emoji = if (gecmis) "⏰" else "✅",
                    ekranIndeksi = 6,
                    aciliyet = if (gecmis) 2 else 1
                )
            }
        }

        // ── 4. Bekleyen konu tekrarı (SM-2) ──
        // NOT: API `bugunkuSayi`, `bugunBekleyen` DEĞİL.
        // Tekrar kapalıysa hiç sorma — kapalıyken sayı 0 döner
        // ama gereksiz disk okuması olur.
        runCatching {
            if (KonuTekrar.acikMi(context)) {
                val sayi = KonuTekrar.bugunkuSayi(context)
                if (sayi > 0) {
                    return Odak(
                        metin = context.getString(R.string.od_tekrar, sayi),
                        emoji = "🔁",
                        aktivite = TekrarActivity::class.java,
                        aciliyet = 1
                    )
                }
            }
        }

        // ── 5. Sınav yaklaşıyor ──
        runCatching {
            val ms = Store.getExamDateMillis(context)
            if (ms > 0) {
                val kalan = ((ms - System.currentTimeMillis()) / 86_400_000L).toInt() + 1
                if (kalan in 1..14) {
                    return Odak(
                        metin = context.getString(R.string.od_sinav, kalan),
                        emoji = "📚",
                        ekranIndeksi = 16,
                        aciliyet = if (kalan <= 3) 2 else 1
                    )
                }
            }
        }

        // ── 6. Günlük hedef ──
        runCatching {
            val hedef = Store.getGoalMinutes(context)
            val bugun = Store.getTodayFocusMinutes(context)
            if (hedef > 0 && bugun < hedef) {
                val kalan = hedef - bugun
                return Odak(
                    metin = if (bugun == 0)
                        context.getString(R.string.od_hedef_basla, hedef)
                    else
                        context.getString(R.string.od_hedef_kalan, kalan),
                    emoji = "🎯",
                    ekranIndeksi = 4,
                    aciliyet = 0
                )
            }
        }

        // ── 7. Hedef tamamlandı — kutlama ──
        runCatching {
            val hedef = Store.getGoalMinutes(context)
            val bugun = Store.getTodayFocusMinutes(context)
            if (hedef > 0 && bugun >= hedef) {
                return Odak(
                    metin = context.getString(R.string.od_hedef_tamam, bugun),
                    emoji = "🎉",
                    ekranIndeksi = 1,
                    aciliyet = 0
                )
            }
        }

        return null
    }

    /**
     * Saate göre selamlama.
     *
     * Mevcut `greetingText` sabit dize kaynağı kullanıyor; burada
     * daha ince kademeler var (gece geç saat ayrı) çünkü hero kart
     * kişisel bir his vermeli.
     */
    fun selamlama(context: Context, saat: Int = -1): String {
        val s = if (saat >= 0) saat
        else Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val res = when (s) {
            in 0..4 -> R.string.od_sel_gece
            in 5..11 -> R.string.od_sel_sabah
            in 12..17 -> R.string.od_sel_oglen
            in 18..21 -> R.string.od_sel_aksam
            else -> R.string.od_sel_yatsi
        }
        return context.getString(res)
    }

    /**
     * Günün ilerleme yüzdesi — hero halkası için.
     *
     * Odak dakikası hedefe oranı. Hedef yoksa 0.
     */
    fun gunYuzdesi(context: Context): Int = runCatching {
        val hedef = Store.getGoalMinutes(context)
        if (hedef <= 0) return 0
        ((Store.getTodayFocusMinutes(context) * 100) / hedef).coerceIn(0, 100)
    }.getOrDefault(0)
}
