package com.gunlukasistan.app

import android.content.Context
import java.util.Calendar

/**
 * v7.43 — Bildirim içeriklerini üretir ve gönderir.
 *
 * `BildirimMerkezi` altyapıyı (kanal, ayar, sessiz saat, tavan) sağlar;
 * bu sınıf **ne zaman hangi bildirimin gönderileceğine** karar verir.
 *
 * Günde bir kez `BildirimZamanlayici` tarafından çağrılır (varsayılan 09:00
 * ve 19:00). Her kontrol kendi koşulunu değerlendirir; uymayan atlanır.
 *
 * Tasarım: her bildirim `bugunGonderildiMi()` ile korunur — aynı bildirim
 * gün içinde iki kez düşmez.
 */
object BildirimUretici {

    private const val TAG = "BildirimUretici"

    // Bildirim kimlikleri — çakışmasın diye sabit
    private const val ID_KART = 7001
    private const val ID_QUIZ = 7002
    private const val ID_YARIM = 7003
    private const val ID_UNUTMA = 7004
    private const val ID_GKART = 7005
    private const val ID_SINAV = 7006
    private const val ID_ROZET = 7007
    private const val ID_HEDEF = 7008
    private const val ID_REKOR = 7009
    private const val ID_SRISK = 7010
    private const val ID_HILERLEME = 7011
    private const val ID_GDONUS = 7012
    private const val ID_OODAK = 7013
    private const val ID_AYLIK = 7014

    /**
     * Tüm kontrolleri sırayla çalıştırır.
     * @param sabahMi true = sabah turu, false = akşam turu
     */
    fun tumKontroller(context: Context, sabahMi: Boolean) {
        try {
            if (sabahMi) {
                // v10.4 · B18: geceden/tavandan yutulanlar önce tek
                // özet olarak teslim edilir; sonra günün turu başlar.
                ozetTeslim(context)
                // v10.3 · B23: sabahın ilk sorusu "bugün ne önemli?"
                // Günün odağı listenin başında; kart/sınav hatırlatmaları
                // ondan sonra gelsin.
                GunOdakBildirim.dene(context)
                gunlukKart(context)      // 7
                sinavSayaci(context)     // 8
                odakOnerisi(context)     // 18
                aylikRapor(context)      // 14
                geriDonusDaveti(context) // 15
            } else {
                kartTekrari(context)     // 1
                quizTekrari(context)     // 2
                yarimDers(context)       // 4
                unutmaUyarisi(context)   // 5
                seriRiski(context)       // 6
                hedefIlerlemesi(context) // 9
            }
            // Her turda kontrol edilenler
            rozetKontrol(context)        // 11
            seriRekoru(context)          // 12
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Bildirim kontrolleri başarısız", e)
        }
    }

    /**
     * v10.4 · B18 — Sessiz saat / tavan yüzünden yutulan bildirimlerin
     * sabah özeti. Defter boşsa hiç çıkmaz; çıkarsa defter kapanır.
     * Özet kendi tavana takılırsa (uç durum) defter açık kalır,
     * ertesi sabah tekrar denenir — biriktirme istisnası sayesinde
     * özet kendi başlığını deftere yazmaz.
     */
    fun ozetTeslim(context: Context) {
        try {
            val biriken = BildirimOzeti.liste(context)
            if (biriken.isEmpty()) return
            val gonderildi = BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.OZET,
                7030,
                context.getString(R.string.bo_baslik),
                context.getString(R.string.bo_kisa, biriken.size),
                genisMetin = BildirimOzeti.ozetMetni(context, biriken),
                acilisIntent = BildirimMerkezi.ekranAc(context, 0, 7030)
            )
            if (gonderildi) BildirimOzeti.temizle(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Özet teslimi başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v11.13 — PROAKTİF AKILLI KOÇ
    // ═══════════════════════════════════════════════════════════════

    /**
     * Günün vaktine göre proaktif koç mesajı gönderir (günde en fazla 1 kez).
     * [BildirimZamanlayici]'ın sabah/akşam turlarından çağrılır.
     */
    fun proaktifKoc(context: Context) {
        try {
            if (BildirimMerkezi.bugunGonderildiMi(context, "koc")) return
            val saat = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val dilim = KocMotoru.dilim(saat)
            val bekleyen = Store.loadTasks(context).count { !it.done }
            val bugun = runCatching { Store.recentDayStats(context, 1) }.getOrNull()
            val tamamlanan = bugun?.getOrNull(0)?.second ?: 0
            val odak = Store.getTodayFocusMinutes(context)
            val hedef = Store.getGoalMinutes(context)
            val (seri, _) = Store.streakInfo(context)
            val mesaj = KocMotoru.mesaj(dilim, bekleyen, tamamlanan, odak, hedef, seri)

            val gonderildi = BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.KOC,
                7040,
                KocMotoru.baslik(dilim),
                mesaj,
                acilisIntent = BildirimMerkezi.ekranAc(context, 9, 7040)
            )
            if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "koc")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Proaktif koç başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. KART TEKRAR HATIRLATICISI
    // ═══════════════════════════════════════════════════════════════

    fun kartTekrari(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "kart")) return
        val adet = try {
            KartStore.bekleyenSayisi(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kart sayısı okunamadı", e); 0
        }
        if (adet < 3) return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.KART_TEKRAR,
            ID_KART,
            context.getString(R.string.n_kart_title, adet),
            context.getString(R.string.n_kart_text),
            genisMetin = context.getString(R.string.n_kart_big, adet),
            acilisIntent = BildirimMerkezi.aktiviteAc(context, KartActivity::class.java, 7001)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "kart")
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. QUIZ TEKRAR ZAMANI
    // ═══════════════════════════════════════════════════════════════

    fun quizTekrari(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "quiz")) return
        val adet = try {
            QuizStore.tekrarSayisi(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Quiz sayısı okunamadı", e); 0
        }
        if (adet < 1) return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.QUIZ_TEKRAR,
            ID_QUIZ,
            context.getString(R.string.n_quiz_title, adet),
            context.getString(R.string.n_quiz_text),
            acilisIntent = BildirimMerkezi.ekranAc(context, 13, 7002)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "quiz")
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. YARIM KALAN ÖĞRETMEN DERSİ
    // ═══════════════════════════════════════════════════════════════

    fun yarimDers(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "yarim")) return
        val oturum = try {
            OgretmenStore.yarimOturumlar(context).firstOrNull()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yarım oturum okunamadı", e); null
        } ?: return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.YARIM_DERS,
            ID_YARIM,
            context.getString(R.string.n_yarim_title),
            context.getString(R.string.n_yarim_text, oturum.dersAdi, oturum.adim + 1),
            acilisIntent = BildirimMerkezi.ekranAc(context, 13, 7003)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "yarim")
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. UNUTMA EĞRİSİ UYARISI
    // ═══════════════════════════════════════════════════════════════

    fun unutmaUyarisi(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "unutma")) return
        // Tamamlanmış ama uzun süredir dokunulmamış ders
        val ders = try {
            val sonDers = Store.sonDers(context)
            Store.loadLessons(context)
                .filter { it.done && it.id != sonDers?.id }
                .randomOrNull()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Ders okunamadı", e); null
        } ?: return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.UNUTMA,
            ID_UNUTMA,
            context.getString(R.string.n_unutma_title),
            context.getString(R.string.n_unutma_text, ders.title),
            acilisIntent = BildirimMerkezi.ekranAc(context, 13, 7004)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "unutma")
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. DERS SERİSİ KORUMA
    // ═══════════════════════════════════════════════════════════════

    fun seriRiski(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "srisk")) return
        val seri = try {
            Store.kursSeri(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Seri okunamadı", e); null
        } ?: return

        // Serisi olan ama bugün hiç çalışmamış kullanıcı
        if (seri.bugunCalisildi || seri.gunSayisi < 2) return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.SERI_RISK,
            ID_SRISK,
            context.getString(R.string.n_srisk_title, seri.gunSayisi),
            context.getString(R.string.n_srisk_text),
            acilisIntent = BildirimMerkezi.ekranAc(context, 13, 7010)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "srisk")
    }

    // ═══════════════════════════════════════════════════════════════
    // 7. GÜNLÜK KELİME / KAVRAM
    // ═══════════════════════════════════════════════════════════════

    fun gunlukKart(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "gkart")) return
        val kart = try {
            KartStore.kartlariYukle(context).filter { it.gecerli }.randomOrNull()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kart okunamadı", e); null
        } ?: return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.GUNLUK_KART,
            ID_GKART,
            context.getString(R.string.n_gkart_title),
            kart.on,
            genisMetin = kart.on + "\n\n" + kart.arka,
            acilisIntent = BildirimMerkezi.aktiviteAc(context, KartActivity::class.java, 7005)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "gkart")
    }

    // ═══════════════════════════════════════════════════════════════
    // 8. SINAV GERİ SAYIM KİLOMETRE TAŞLARI
    // ═══════════════════════════════════════════════════════════════

    fun sinavSayaci(context: Context) {
        val millis = try {
            Store.getExamDateMillis(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sınav tarihi okunamadı", e); 0L
        }
        if (millis <= 0L) return

        val kalan = ((millis - System.currentTimeMillis()) / 86_400_000L).toInt() + 1
        // Yalnızca kilometre taşlarında bildir
        if (kalan !in listOf(30, 14, 7, 3, 1)) return
        if (BildirimMerkezi.bugunGonderildiMi(context, "sinav" + kalan)) return

        // Hazırlık yüzdesi — tamamlanan alt madde oranı
        val yuzde = try {
            val maddeler = Store.loadTopics(context).flatMap { it.items }
            if (maddeler.isEmpty()) 0 else maddeler.count { it.done } * 100 / maddeler.size
        } catch (_: Exception) { 0 }

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.SINAV_SAYAC,
            ID_SINAV,
            context.getString(R.string.n_sinav_title, kalan),
            context.getString(R.string.n_sinav_text, yuzde),
            acilisIntent = BildirimMerkezi.ekranAc(context, 1, 7006)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "sinav" + kalan)
    }

    // ═══════════════════════════════════════════════════════════════
    // 9. GÜNLÜK HEDEF İLERLEMESİ
    // ═══════════════════════════════════════════════════════════════

    fun hedefIlerlemesi(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "hilerleme")) return
        val odak = Store.getTodayFocusMinutes(context)
        val hedef = Store.getGoalMinutes(context)
        if (hedef <= 0 || odak >= hedef) return
        val kalan = hedef - odak
        // Yalnızca hedefe yaklaşıldıysa hatırlat (yarısı geçilmiş)
        if (odak == 0 || kalan > hedef / 2) return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.HEDEF_ILERLEME,
            ID_HILERLEME,
            context.getString(R.string.n_hilerleme_title, kalan),
            context.getString(R.string.n_hilerleme_text),
            acilisIntent = BildirimMerkezi.ekranAc(context, 4, 7011)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "hilerleme")
    }

    // ═══════════════════════════════════════════════════════════════
    // 10. HEDEF TAMAMLANDI KUTLAMASI (anlık — Store'dan çağrılır)
    // ═══════════════════════════════════════════════════════════════

    fun hedefTamamlandi(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "hedeftamam")) return
        val odak = Store.getTodayFocusMinutes(context)
        val hedef = Store.getGoalMinutes(context)
        if (hedef <= 0 || odak < hedef) return

        val (seri, _) = Store.streakInfo(context)
        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.HEDEF_TAMAM,
            ID_HEDEF,
            context.getString(R.string.n_hedef_title),
            context.getString(R.string.n_hedef_text, odak, seri),
            acil = true,
            acilisIntent = BildirimMerkezi.ekranAc(context, 1, 7008)
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "hedeftamam")
    }

    // ═══════════════════════════════════════════════════════════════
    // 11. ROZET KAZANMA
    // ═══════════════════════════════════════════════════════════════

    private const val PREF_ROZET = "rozet_bildirim_v1"

    /**
     * Yeni kazanılan rozetleri bulur ve bildirir.
     * Badges.kt rozetleri hesaplıyordu ama kimse haber vermiyordu.
     */
    fun rozetKontrol(context: Context) {
        try {
            val p = context.getSharedPreferences(PREF_ROZET, Context.MODE_PRIVATE)
            val bilinen = p.getStringSet("kazanilan", emptySet()) ?: emptySet()
            val simdiki = Badges.earned(context).map { it.id }.toSet()
            val yeniler = simdiki - bilinen

            // İlk çalıştırma: mevcut rozetleri sessizce kaydet, bildirim gönderme
            if (bilinen.isEmpty() && simdiki.isNotEmpty()) {
                p.edit().putStringSet("kazanilan", simdiki).apply()
                return
            }
            if (yeniler.isEmpty()) return

            val rozet = Badges.all.firstOrNull { it.id in yeniler } ?: return
            BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.ROZET,
                ID_ROZET,
                context.getString(R.string.n_rozet_title),
                rozet.emoji + " " + rozet.title,
                genisMetin = rozet.emoji + " " + rozet.title + "\n" + rozet.desc +
                    "\n\n" + context.getString(
                        R.string.n_rozet_count, simdiki.size, Badges.all.size
                    ),
                acil = true,
                acilisIntent = BildirimMerkezi.ekranAc(context, 0, 7007)
            )
            p.edit().putStringSet("kazanilan", simdiki).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Rozet kontrolü başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 12. SERİ REKORU KIRMA
    // ═══════════════════════════════════════════════════════════════

    private const val PREF_REKOR = "rekor_bildirim_v1"

    fun seriRekoru(context: Context) {
        try {
            val (simdiki, rekor) = Store.streakInfo(context)
            if (simdiki < 3 || simdiki < rekor) return

            val p = context.getSharedPreferences(PREF_REKOR, Context.MODE_PRIVATE)
            val sonBildirilen = p.getInt("son", 0)
            if (simdiki <= sonBildirilen) return

            val gonderildi = BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.SERI_REKOR,
                ID_REKOR,
                context.getString(R.string.n_rekor_title, simdiki),
                context.getString(R.string.n_rekor_text),
                acil = true,
                acilisIntent = BildirimMerkezi.ekranAc(context, 1, 7009)
            )
            if (gonderildi) p.edit().putInt("son", simdiki).apply()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Rekor kontrolü başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 14. AYLIK İLERLEME RAPORU
    // ═══════════════════════════════════════════════════════════════

    fun aylikRapor(context: Context) {
        val c = Calendar.getInstance()
        if (c.get(Calendar.DAY_OF_MONTH) != 1) return
        if (BildirimMerkezi.bugunGonderildiMi(context, "aylik")) return

        try {
            val gecen = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            val y = gecen.get(Calendar.YEAR)
            val m = gecen.get(Calendar.MONTH)
            val dakika = Store.monthFocus(context, y, m)
            val madde = Store.monthCompletions(context, y, m)
            val gun = Store.monthActiveDays(context, y, m)
            if (dakika == 0 && madde == 0) return

            val degisim = Analitik.aylikDegisim(context)
            val saat = dakika / 60

            val gonderildi = BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.AYLIK,
                ID_AYLIK,
                context.getString(R.string.n_aylik_title),
                context.getString(R.string.n_aylik_text, saat, madde),
                genisMetin = context.getString(
                    R.string.n_aylik_big, saat, madde, gun,
                    if (degisim >= 0) "+" + degisim else degisim.toString()
                ),
                acilisIntent = BildirimMerkezi.aktiviteAc(
                    context, AnalitikActivity::class.java, 7014
                )
            )
            if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "aylik")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Aylık rapor başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 15. GERİ DÖNÜŞ DAVETİ
    // ═══════════════════════════════════════════════════════════════

    fun geriDonusDaveti(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "gdonus")) return
        try {
            // Son 3 gün hiç aktivite yok mu?
            val aktifVar = (0..2).any { Store.dayWasActive(context, it) }
            if (aktifVar) return
            // Hiç kullanmamış kullanıcıya davet gönderme
            if (Analitik.toplamAktifGun(context) < 3) return

            val gonderildi = BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.GERI_DONUS,
                ID_GDONUS,
                context.getString(R.string.n_gdonus_title),
                context.getString(R.string.n_gdonus_text),
                acilisIntent = BildirimMerkezi.ekranAc(context, 2, 7012)
            )
            if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "gdonus")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Geri dönüş daveti başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 18. ODAK ÖNERİSİ (akıllı saat)
    // ═══════════════════════════════════════════════════════════════

    fun odakOnerisi(context: Context) {
        if (BildirimMerkezi.bugunGonderildiMi(context, "oodak")) return
        try {
            if (!Analitik.saatVerisiVarMi(context)) return
            val enIyi = Analitik.enVerimliSaat(context)
            if (enIyi < 0) return
            val simdi = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            // Verimli saate 1 saat kala hatırlat
            if (simdi != (enIyi - 1 + 24) % 24) return
            if (Store.getTodayFocusMinutes(context) > 0) return

            val gonderildi = BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.ODAK_ONERI,
                ID_OODAK,
                context.getString(R.string.n_oodak_title),
                context.getString(R.string.n_oodak_text, enIyi),
                acilisIntent = BildirimMerkezi.ekranAc(context, 4, 7013)
            )
            if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "oodak")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Odak önerisi başarısız", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 20. UZUN OTURUM UYARISI (TimerEngine'den çağrılır)
    // ═══════════════════════════════════════════════════════════════

    fun uzunOturumUyarisi(context: Context, gecenDakika: Int) {
        if (gecenDakika < 90) return
        if (BildirimMerkezi.bugunGonderildiMi(context, "uzun" + (gecenDakika / 90))) return

        val gonderildi = BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.UZUN_OTURUM,
            7020,
            context.getString(R.string.n_uzun_title),
            context.getString(R.string.n_uzun_text, gecenDakika),
            acil = true
        )
        if (gonderildi) BildirimMerkezi.bugunIsaretle(context, "uzun" + (gecenDakika / 90))
    }

    // ═══════════════════════════════════════════════════════════════
    // 22-24. ARKA PLAN İŞ BİLDİRİMLERİ
    // ═══════════════════════════════════════════════════════════════

    /** 22. Kaynak bulma tamamlandı. */
    fun kaynakBulundu(context: Context, pdf: Int, video: Int) {
        BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.ARKAPLAN_IS,
            7022,
            context.getString(R.string.n_kaynak_title),
            context.getString(R.string.n_kaynak_text, pdf, video),
            acil = true,
            acilisIntent = BildirimMerkezi.ekranAc(context, 14, 7022)
        )
    }

    /** 23. Kurs üretimi tamamlandı. */
    fun kursUretildi(context: Context, kursAdi: String, dersSayisi: Int) {
        BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.ARKAPLAN_IS,
            7023,
            context.getString(R.string.n_kurs_title),
            context.getString(R.string.n_kurs_text, kursAdi, dersSayisi),
            acil = true,
            acilisIntent = BildirimMerkezi.ekranAc(context, 13, 7023)
        )
    }

    /** 24. Yedekleme tamamlandı. */
    fun yedekAlindi(context: Context, kayitSayisi: Int) {
        BildirimMerkezi.gonder(
            context,
            BildirimMerkezi.Tur.YEDEK,
            7024,
            context.getString(R.string.n_yedek_title),
            context.getString(R.string.n_yedek_text, kayitSayisi)
        )
    }

    /** 21. PDF indeksleme ilerlemesi. */
    fun pdfIndeksIlerleme(context: Context, islenen: Int, toplam: Int, bitti: Boolean) {
        if (bitti) {
            BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.ARKAPLAN_IS,
                7021,
                context.getString(R.string.n_pdf_done_title),
                context.getString(R.string.n_pdf_done_text, toplam),
                acil = true
            )
        } else {
            BildirimMerkezi.gonder(
                context,
                BildirimMerkezi.Tur.ARKAPLAN_IS,
                7021,
                context.getString(R.string.n_pdf_title),
                context.getString(R.string.n_pdf_text, islenen, toplam),
                acil = true,
                ilerleme = islenen to toplam
            )
        }
    }
}
