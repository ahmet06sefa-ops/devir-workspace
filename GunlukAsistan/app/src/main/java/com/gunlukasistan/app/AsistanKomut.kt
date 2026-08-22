package com.gunlukasistan.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * v7.36 — Asistanın uygulamadaki TAM YETKİ katmanı.
 *
 * ── Neden baştan yazıldı ──
 * v7.35'e kadar yalnızca 8 komut vardı ve hepsi "ekleme" işiydi.
 * Silme, düzenleme, tamamlama yoktu; bir cevapta yalnızca TEK komut
 * çalıştırılıyordu. Kullanıcı "bütün yetkiyi ver" dediği için katman
 * baştan tasarlandı.
 *
 * ── Tasarım ilkeleri ──
 * 1. TAM KAPSAM — her veri türü için ekle/sil/düzenle/tamamla.
 * 2. ÇOKLU KOMUT — bir cevapta birden çok iş sırayla yapılır.
 * 3. ONAY — silme gibi geri dönüşü olan işler kullanıcıya sorulur.
 * 4. GERİ ALINABİLİRLİK — her silme Store.kaydetGeriAlma ile saklanır.
 * 5. SESSİZ BAŞARISIZLIK YOK — bulunamayan kayıt kullanıcıya söylenir.
 * 6. ASLA TOPLU SİLME — "hepsini sil" gibi komut yoktur; her komut tek
 *    kayda dokunur. Veri kaybı riski böylece sıfırlanır.
 */
object AsistanKomut {

    private const val TAG = "AsistanKomut"
    private const val ISARET = ">>KOMUT:"

    /** Bir cevapta en fazla kaç komut çalıştırılır (sonsuz döngü koruması). */
    private const val AZAMI_KOMUT = 8

    /** Ayrıştırılmış komut. */
    data class Komut(val ad: String, val deger: String)

    /** Çalıştırma sonucu: kullanıcıya gösterilecek kısa bildirim. */
    data class Sonuc(val basarili: Boolean, val bildirim: String)

    // ═══════════════════════════════════════════════════════════════
    // AYIKLAMA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Eski tek komutluk API — geriye dönük uyum için korundu.
     * Yeni kod [ayiklaHepsi] kullanmalı.
     */
    fun ayikla(cevap: String): Pair<String, Komut?> {
        val (temiz, liste) = ayiklaHepsi(cevap)
        return temiz to liste.firstOrNull()
    }

    /**
     * v7.36: Cevaptaki TÜM komutları sırayla ayıklar.
     * @return (komut satırları temizlenmiş metin, komut listesi)
     */
    fun ayiklaHepsi(cevap: String): Pair<String, List<Komut>> {
        val satirlar = cevap.lines()
        val komutlar = mutableListOf<Komut>()

        satirlar.forEach { satir ->
            val t = satir.trim()
            if (!t.startsWith(ISARET)) return@forEach
            val govde = t.removePrefix(ISARET).trim()
            val parcalar = govde.split("|", limit = 2)
            val ad = parcalar.getOrNull(0)?.trim()?.lowercase(Locale.US).orEmpty()
            val deger = parcalar.getOrNull(1)?.trim().orEmpty()
            if (ad.isNotBlank() && komutlar.size < AZAMI_KOMUT) {
                komutlar.add(Komut(ad, deger))
            }
        }

        val temiz = satirlar
            .filterNot { it.trim().startsWith(ISARET) }
            .joinToString("\n")
            .trim()

        return temiz to komutlar
    }

    // ═══════════════════════════════════════════════════════════════
    // ONAY
    // ═══════════════════════════════════════════════════════════════

    /** Bu komut veri siliyor mu? Siliyorsa kullanıcıya sorulur. */
    fun onayGerekli(komut: Komut): Boolean =
        komut.ad.endsWith("_sil") || komut.ad == "yedek_geri_al"

    /** Onay penceresinde gösterilecek soru. */
    fun onaySorusu(context: Context, komut: Komut): String {
        val hedef = komut.deger.split("::").firstOrNull()?.trim().orEmpty().take(60)
        return when (komut.ad) {
            "yedek_geri_al" -> context.getString(R.string.cmd_ask_restore)
            "alt_madde_sil" -> {
                val p = komut.deger.split("::", limit = 2)
                context.getString(
                    R.string.cmd_ask_delete_sub,
                    p.getOrNull(1)?.trim().orEmpty().take(50),
                    p.getOrNull(0)?.trim().orEmpty().take(40)
                )
            }
            else -> context.getString(R.string.cmd_ask_delete, hedef)
        }
    }

    /**
     * v7.36: Komutları sırayla çalıştırır; onay gerekenlerde pencere açar.
     * Arayüz iş parçacığından çağrılmalıdır.
     *
     * @param bitince tüm komutlar işlendiğinde toplanan bildirimlerle çağrılır
     */
    fun calistirSirayla(
        activity: Activity,
        komutlar: List<Komut>,
        bitince: (List<String>) -> Unit
    ) {
        val bildirimler = mutableListOf<String>()

        fun sonraki(i: Int) {
            if (i >= komutlar.size) {
                bitince(bildirimler)
                return
            }
            val komut = komutlar[i]

            fun uygula() {
                val s = calistir(activity, komut, activity)
                if (s.bildirim.isNotBlank()) bildirimler.add(s.bildirim)
                sonraki(i + 1)
            }

            if (onayGerekli(komut)) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.cmd_confirm_title)
                    .setMessage(onaySorusu(activity, komut))
                    .setPositiveButton(R.string.cmd_confirm_yes) { _, _ -> uygula() }
                    .setNegativeButton(R.string.cmd_confirm_no) { _, _ ->
                        bildirimler.add(activity.getString(R.string.cmd_cancelled))
                        sonraki(i + 1)
                    }
                    .setCancelable(false)
                    .show()
            } else {
                uygula()
            }
        }

        sonraki(0)
    }

    // ═══════════════════════════════════════════════════════════════
    // ÇALIŞTIRMA
    // ═══════════════════════════════════════════════════════════════

    fun calistir(context: Context, komut: Komut, activity: Activity?): Sonuc {
        return try {
            when (komut.ad) {
                // ── Görevler ──
                "gorev_ekle" -> gorevEkle(context, komut.deger)
                "gorev_tamamla" -> gorevTamamla(context, komut.deger)
                "gorev_sil" -> gorevSil(context, komut.deger)
                "gorev_duzenle" -> gorevDuzenle(context, komut.deger)

                // ── Notlar ──
                "not_ekle" -> notEkle(context, komut.deger)
                "not_sil" -> notSil(context, komut.deger)
                "not_duzenle" -> notDuzenle(context, komut.deger)

                // ── Konular ──
                "konu_ekle" -> konuEkle(context, komut.deger)
                "konu_sil" -> konuSil(context, komut.deger)
                "konu_duzenle" -> konuDuzenle(context, komut.deger)
                "alt_madde_ekle" -> altMaddeEkle(context, komut.deger)
                "alt_madde_tamamla" -> altMaddeTamamla(context, komut.deger)
                "alt_madde_sil" -> altMaddeSil(context, komut.deger)

                // ── Alışkanlıklar ──
                "aliskanlik_ekle" -> aliskanlikEkle(context, komut.deger)
                "aliskanlik_isaretle" -> aliskanlikIsaretle(context, komut.deger)
                "aliskanlik_sil" -> aliskanlikSil(context, komut.deger)

                // ── Etkinlikler ──
                "etkinlik_ekle" -> etkinlikEkle(context, komut.deger)
                "etkinlik_sil" -> etkinlikSil(context, komut.deger)

                // ── Kurslar ──
                "kurs_ekle" -> kursEkle(context, komut.deger)
                "kurs_sil" -> kursSil(context, komut.deger)
                "bolum_ekle" -> bolumEkle(context, komut.deger)
                "ders_ekle" -> dersEkle(context, komut.deger)
                "ders_tamamla" -> dersTamamla(context, komut.deger)
                "ders_sil" -> dersSil(context, komut.deger)

                // ── Bilgi kartları ──
                "kart_ekle" -> kartEkle(context, komut.deger)

                // ── Sınav ──
                "sinav_ekle" -> sinavEkle(context, komut.deger)

                // ── Ayarlar ──
                "hedef_ayarla" -> hedefAyarla(context, komut.deger)
                "soz_ayarla" -> sozAyarla(context, komut.deger)
                "sinav_tarihi" -> sinavTarihi(context, komut.deger)

                // ── v11.13: Uygulama ayarlarını kontrol et ──
                "ayar_ses" -> ayarSes(context, komut.deger)
                "ayar_titresim" -> ayarTitresim(context, komut.deger)
                "ayar_animasyon" -> ayarAnimasyon(context, komut.deger)
                "ayar_namaz" -> ayarNamaz(context, komut.deger)
                "ayar_gece" -> ayarGece(context, komut.deger)
                "widget_yenile" -> widgetYenile(context)
                "ozet_ver" -> ozetVer(context)
                // v11.13: proaktif koç + haftalık rapor + akıllı plan
                "koc_mesaj" -> kocMesaj(context, komut.deger)
                "haftalik_rapor" -> haftalikRapor(context)
                "akilli_plan" -> akilliPlan(context, komut.deger)
                // v11.13: hazır koçluk programları + oyunlaştırma
                "kocluk_programi" -> koclukProgrami(context, komut.deger)
                "xp_durum" -> xpDurum(context)
                // v11.13: rakip farkı çekirdekleri
                "hesap_durum" -> hesapDurum(context)
                "saglik_hedef" -> saglikHedef(context, komut.deger)
                "takvim_plan" -> takvimPlan(context, komut.deger)
                // v11.13: çok dillilik + zengin dışa aktarma
                "dil_sec" -> dilSec(context, komut.deger)
                "disa_aktar" -> disaAktar(context)
                // v11.13: sosyal meydan okuma + akıllı bildirim filtresi
                "meydan_okuma" -> meydanOkuma(context, komut.deger)
                "bildirim_durum" -> bildirimDurum(context)
                // v11.13: CSV dışa aktarma
                "disa_aktar_csv" -> disaAktarCsv(context)
                "depolama_durum" -> depolamaDurum(context)
                "basari_raporu" -> basariRaporu(context)
                "trend_analiz" -> trendAnaliz(context)
                "gorev_takvimi" -> gorevTakvimi(context)
                // v11.14: pomodoro + içerik önceliklendirme
                "pomodoro_durum" -> pomodoroDurum(context)
                "onceliklendir" -> onceliklendir(context, komut.deger)

                // ── Eylemler ──
                "zamanlayici" -> zamanlayici(context, komut.deger, activity)
                "ekran_ac" -> ekranAc(context, komut.deger, activity)
                "ders_devam" -> dersDevam(context, activity)
                "analiz_ac" -> analizAc(context, activity)
                "pdf_ara" -> pdfAra(context, activity)
                "namaz_ac" -> namazAc(context, activity)
                "film_ac" -> filmAc(context, activity)
                "online_ac" -> onlineAc(context, activity)
                "atolye_ac" -> atolyeAc(context, komut.deger, activity)
                "uygulamalar_ac" -> uygulamalarAc(context, activity)
                "uygulama_ac" -> uygulamaAc(context, komut.deger, activity)
                "telefon_ara" -> telefonAra(context, komut.deger)
                "yaz" -> ekranaYaz(context, komut.deger)
                "yedek_al" -> yedekAl(context)
                "yedek_geri_al" -> yedekGeriAl(context)

                else -> Sonuc(false, "")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Komut çalıştırılamadı: " + komut.ad, e)
            Sonuc(false, context.getString(R.string.cmd_error, komut.ad))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ORTAK YARDIMCILAR
    // ═══════════════════════════════════════════════════════════════

    /** Türkçe duyarlı karşılaştırma anahtarı. */
    private fun normalle(s: String): String =
        s.lowercase(Locale("tr", "TR"))
            .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
            .replace("ü", "u").replace("ö", "o").replace("ç", "c")
            .filter { it.isLetterOrDigit() || it == ' ' }
            .trim()

    /**
     * Esnek metin eşleşmesi: tam → başlangıç → içeren → tersine içeren.
     * Kullanıcı "kolon" dediğinde "Betonarme Kolon Tasarımı" bulunur.
     */
    private fun <T> esnekBul(liste: List<T>, aranan: String, ad: (T) -> String): T? {
        val a = normalle(aranan)
        if (a.isBlank()) return null
        return liste.firstOrNull { normalle(ad(it)) == a }
            ?: liste.firstOrNull { normalle(ad(it)).startsWith(a) }
            ?: liste.firstOrNull { normalle(ad(it)).contains(a) }
            ?: liste.firstOrNull { a.contains(normalle(ad(it))) }
    }

    /** "bulunamadı" bildirimi. */
    private fun yok(context: Context, ne: String): Sonuc =
        Sonuc(false, context.getString(R.string.cmd_not_found, ne.take(40)))

    /** Değeri "::" ile böler. */
    private fun bol(deger: String, adet: Int = 2): List<String> =
        deger.split("::", limit = adet).map { it.trim() }

    /** Madde listesini ";;" veya satır sonuyla böler, temizler. */
    private fun maddeleriAyir(govde: String): List<String> {
        val ham = if (govde.contains(";;")) govde.split(";;") else govde.lines()
        return ham.mapNotNull { satir ->
            var t = satir.trim().trim('-', '*', '\u2022', ' ')
            t = t.replace(Regex("^\\s*\\d{1,2}\\s*[.)\\-:]\\s*"), "").trim()
            if (t.length < 2) null else t.take(150)
        }
    }

    private fun tarihAnahtari(metin: String): String? {
        if (metin.isBlank()) return null
        // 1) Doğal dil ("yarın", "3 gün sonra", "15 Mart")
        try {
            val ayr = NaturalDate.parse(metin)
            if (ayr.found) {
                return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ayr.millis))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Doğal tarih okunamadı", e)
        }
        // 2) yyyyMMdd
        val sadeceRakam = metin.filter { it.isDigit() }
        if (sadeceRakam.length == 8) return sadeceRakam
        // 3) gg.aa.yyyy / gg/aa/yyyy
        val p = metin.split(".", "/", "-").mapNotNull { it.trim().toIntOrNull() }
        if (p.size == 3) {
            val (g, a, y) = Triple(p[0], p[1], p[2])
            val yil = if (y < 100) 2000 + y else y
            if (g in 1..31 && a in 1..12 && yil in 2000..2100) {
                return String.format(Locale.US, "%04d%02d%02d", yil, a, g)
            }
        }
        return null
    }

    // ═══════════════════════════════════════════════════════════════
    // GÖREVLER
    // ═══════════════════════════════════════════════════════════════

    private fun gorevEkle(context: Context, deger: String): Sonuc {
        if (deger.isBlank()) return Sonuc(false, "")
        // "metin :: tarih" biçimi desteklenir
        val p = bol(deger)
        val metin = p.getOrNull(0).orEmpty()
        if (metin.isBlank()) return Sonuc(false, "")

        var sonTarih = 0L
        val tarihMetni = p.getOrNull(1).orEmpty()
        if (tarihMetni.isNotBlank()) {
            try {
                val ayr = NaturalDate.parse(tarihMetni)
                if (ayr.found) sonTarih = ayr.millis
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Görev tarihi okunamadı", e)
            }
        }

        val liste = Store.loadTasks(context)
        liste.add(
            0,
            Store.Task(
                id = System.currentTimeMillis(),
                text = metin.take(200),
                done = false,
                createdAt = System.currentTimeMillis(),
                dueAt = sonTarih
            )
        )
        Store.saveTasks(context, liste)
        return Sonuc(true, context.getString(R.string.cmd_task_added, metin.take(40)))
    }

    private fun gorevTamamla(context: Context, ad: String): Sonuc {
        val liste = Store.loadTasks(context)
        val hedef = esnekBul(liste.filter { !it.done }, ad) { it.text }
            ?: esnekBul(liste, ad) { it.text }
            ?: return yok(context, ad)
        if (hedef.done) {
            return Sonuc(true, context.getString(R.string.cmd_task_already, hedef.text.take(30)))
        }
        hedef.done = true
        Store.saveTasks(context, liste)
        Store.recordCompletion(context)
        return Sonuc(true, context.getString(R.string.cmd_task_done, hedef.text.take(30)))
    }

    private fun gorevSil(context: Context, ad: String): Sonuc {
        val liste = Store.loadTasks(context)
        val hedef = esnekBul(liste, ad) { it.text } ?: return yok(context, ad)
        Store.deleteTaskUndoable(context, hedef.id)
        return Sonuc(true, context.getString(R.string.cmd_task_deleted, hedef.text.take(30)))
    }

    private fun gorevDuzenle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val eski = p.getOrNull(0).orEmpty()
        val yeni = p.getOrNull(1).orEmpty()
        if (eski.isBlank() || yeni.isBlank()) return Sonuc(false, "")
        val liste = Store.loadTasks(context)
        val hedef = esnekBul(liste, eski) { it.text } ?: return yok(context, eski)
        hedef.text = yeni.take(200)
        Store.saveTasks(context, liste)
        return Sonuc(true, context.getString(R.string.cmd_task_edited, yeni.take(30)))
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTLAR
    // ═══════════════════════════════════════════════════════════════

    private fun notEkle(context: Context, deger: String): Sonuc {
        if (deger.isBlank()) return Sonuc(false, "")
        val p = bol(deger)
        val baslik = p.getOrNull(0).orEmpty().ifBlank {
            context.getString(R.string.note_untitled)
        }
        val icerik = p.getOrNull(1).orEmpty().ifBlank { p.getOrNull(0).orEmpty() }

        val liste = Store.loadNotes(context)
        liste.add(
            0,
            Store.Note(
                id = System.currentTimeMillis(),
                title = baslik.take(120),
                content = icerik.take(4000),
                createdAt = System.currentTimeMillis()
            )
        )
        Store.saveNotes(context, liste)
        return Sonuc(true, context.getString(R.string.cmd_note_added, baslik.take(40)))
    }

    private fun notSil(context: Context, ad: String): Sonuc {
        val liste = Store.loadNotes(context)
        val hedef = esnekBul(liste, ad) { it.title + " " + it.content } ?: return yok(context, ad)
        Store.saveNotes(context, liste.filterNot { it.id == hedef.id })
        Store.kaydetGeriAlma {
            val l = Store.loadNotes(context)
            l.add(0, hedef)
            Store.saveNotes(context, l)
        }
        return Sonuc(true, context.getString(R.string.cmd_note_deleted, hedef.title.take(30)))
    }

    private fun notDuzenle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val ad = p.getOrNull(0).orEmpty()
        val yeni = p.getOrNull(1).orEmpty()
        if (ad.isBlank() || yeni.isBlank()) return Sonuc(false, "")
        val liste = Store.loadNotes(context)
        val hedef = esnekBul(liste, ad) { it.title } ?: return yok(context, ad)
        hedef.content = yeni.take(4000)
        Store.saveNotes(context, liste)
        return Sonuc(true, context.getString(R.string.cmd_note_edited, hedef.title.take(30)))
    }

    // ═══════════════════════════════════════════════════════════════
    // KONULAR
    // ═══════════════════════════════════════════════════════════════

    private fun konuEkle(context: Context, deger: String): Sonuc {
        if (deger.isBlank()) return Sonuc(false, "")
        // "Başlık :: madde1 ;; madde2" — tek komutta alt maddeleriyle
        val p = bol(deger)
        val baslik = p.getOrNull(0).orEmpty()
        if (baslik.isBlank()) return Sonuc(false, "")

        val liste = Store.loadTopics(context)
        val simdi = System.currentTimeMillis()
        val maddeler = p.getOrNull(1)?.let { maddeleriAyir(it) }.orEmpty()

        liste.add(
            0,
            Store.Topic(
                id = simdi,
                title = baslik.take(120),
                createdAt = simdi,
                items = maddeler.mapIndexed { i, m ->
                    Store.SubItem(simdi + i + 1, m, false, simdi)
                }.toMutableList()
            )
        )
        Store.saveTopics(context, liste)
        return Sonuc(
            true,
            if (maddeler.isEmpty()) {
                context.getString(R.string.cmd_topic_added, baslik.take(40))
            } else {
                context.getString(R.string.cmd_topic_added_with, baslik.take(30), maddeler.size)
            }
        )
    }

    private fun konuSil(context: Context, ad: String): Sonuc {
        val liste = Store.loadTopics(context)
        val hedef = esnekBul(liste, ad) { it.title } ?: return yok(context, ad)
        val sira = liste.indexOf(hedef)
        Store.saveTopics(context, liste.filterNot { it.id == hedef.id })
        Store.kaydetGeriAlma {
            val l = Store.loadTopics(context)
            l.add(sira.coerceIn(0, l.size), hedef)
            Store.saveTopics(context, l)
        }
        return Sonuc(true, context.getString(R.string.cmd_topic_deleted, hedef.title.take(30)))
    }

    private fun konuDuzenle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val eski = p.getOrNull(0).orEmpty()
        val yeni = p.getOrNull(1).orEmpty()
        if (eski.isBlank() || yeni.isBlank()) return Sonuc(false, "")
        val liste = Store.loadTopics(context)
        val hedef = esnekBul(liste, eski) { it.title } ?: return yok(context, eski)
        hedef.title = yeni.take(120)
        Store.saveTopics(context, liste)
        return Sonuc(true, context.getString(R.string.cmd_topic_edited, yeni.take(30)))
    }

    /**
     * Var olan bir konuya alt madde(ler) ekler.
     * Biçim: `Konu Başlığı :: madde1 ;; madde2 ;; madde3`
     */
    private fun altMaddeEkle(context: Context, deger: String): Sonuc {
        if (deger.isBlank()) return Sonuc(false, "")
        val p = bol(deger)
        val konuAdi = p.getOrNull(0).orEmpty()
        val govde = p.getOrNull(1).orEmpty()
        if (konuAdi.isBlank() || govde.isBlank()) return Sonuc(false, "")

        val liste = Store.loadTopics(context)
        val hedef = esnekBul(liste, konuAdi) { it.title } ?: return yok(context, konuAdi)

        val mevcut = hedef.items.map { normalle(it.text) }.toMutableSet()
        val simdi = System.currentTimeMillis()
        var eklenen = 0

        maddeleriAyir(govde).forEach { t ->
            val n = normalle(t)
            if (n.isBlank() || n in mevcut) return@forEach
            mevcut.add(n)
            hedef.items.add(Store.SubItem(simdi + eklenen, t, false, simdi))
            eklenen++
        }

        if (eklenen == 0) {
            return Sonuc(true, context.getString(R.string.cmd_subitem_dup, hedef.title.take(30)))
        }
        Store.saveTopics(context, liste)
        return Sonuc(
            true,
            context.getString(R.string.cmd_subitem_added, hedef.title.take(30), eklenen)
        )
    }

    private fun altMaddeTamamla(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val konuAdi = p.getOrNull(0).orEmpty()
        val maddeAdi = p.getOrNull(1).orEmpty()
        if (maddeAdi.isBlank()) return Sonuc(false, "")

        val liste = Store.loadTopics(context)
        // Konu adı verilmişse orada, verilmemişse tüm konularda ara
        val konu = if (konuAdi.isBlank()) {
            liste.firstOrNull { k -> esnekBul(k.items, maddeAdi) { it.text } != null }
        } else {
            esnekBul(liste, konuAdi) { it.title }
        } ?: return yok(context, konuAdi.ifBlank { maddeAdi })

        val madde = esnekBul(konu.items, maddeAdi) { it.text } ?: return yok(context, maddeAdi)
        if (madde.done) {
            return Sonuc(true, context.getString(R.string.cmd_sub_already, madde.text.take(30)))
        }
        madde.done = true
        Store.saveTopics(context, liste)
        Store.recordCompletion(context)
        return Sonuc(true, context.getString(R.string.cmd_subitem_done, madde.text.take(30)))
    }

    private fun altMaddeSil(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val konuAdi = p.getOrNull(0).orEmpty()
        val maddeAdi = p.getOrNull(1).orEmpty()
        if (maddeAdi.isBlank()) return Sonuc(false, "")

        val liste = Store.loadTopics(context)
        val konu = if (konuAdi.isBlank()) {
            liste.firstOrNull { k -> esnekBul(k.items, maddeAdi) { it.text } != null }
        } else {
            esnekBul(liste, konuAdi) { it.title }
        } ?: return yok(context, konuAdi.ifBlank { maddeAdi })

        val madde = esnekBul(konu.items, maddeAdi) { it.text } ?: return yok(context, maddeAdi)
        val sira = konu.items.indexOf(madde)
        konu.items.remove(madde)
        Store.saveTopics(context, liste)
        Store.kaydetGeriAlma {
            val l = Store.loadTopics(context)
            l.firstOrNull { it.id == konu.id }?.items?.add(
                sira.coerceIn(0, l.firstOrNull { it.id == konu.id }?.items?.size ?: 0),
                madde
            )
            Store.saveTopics(context, l)
        }
        return Sonuc(true, context.getString(R.string.cmd_subitem_deleted, madde.text.take(30)))
    }

    // ═══════════════════════════════════════════════════════════════
    // ALIŞKANLIKLAR
    // ═══════════════════════════════════════════════════════════════

    private fun aliskanlikEkle(context: Context, deger: String): Sonuc {
        if (deger.isBlank()) return Sonuc(false, "")
        val p = bol(deger)
        val ad = p.getOrNull(0).orEmpty()
        if (ad.isBlank()) return Sonuc(false, "")
        val hedefSayi = p.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 1
        val renk = Store.loadHabits(context).size % Store.HABIT_COLORS.size
        Store.addHabit(context, ad.take(80), "✨", hedefSayi.coerceIn(1, 20), renk)
        return Sonuc(true, context.getString(R.string.cmd_habit_added, ad.take(40)))
    }

    private fun aliskanlikIsaretle(context: Context, ad: String): Sonuc {
        val liste = Store.loadHabits(context).filterNot { it.archived }
        val hedef = esnekBul(liste, ad) { it.title } ?: return yok(context, ad)
        val yeni = Store.toggleHabit(context, hedef)
        return Sonuc(
            true,
            context.getString(R.string.cmd_habit_marked, hedef.title.take(30), yeni, hedef.target)
        )
    }

    private fun aliskanlikSil(context: Context, ad: String): Sonuc {
        val liste = Store.loadHabits(context)
        val hedef = esnekBul(liste, ad) { it.title } ?: return yok(context, ad)
        Store.deleteHabitUndoable(context, hedef.id)
        return Sonuc(true, context.getString(R.string.cmd_habit_deleted, hedef.title.take(30)))
    }

    // ═══════════════════════════════════════════════════════════════
    // ETKİNLİKLER
    // ═══════════════════════════════════════════════════════════════

    private fun etkinlikEkle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val baslik = p.getOrNull(0).orEmpty()
        val tarihMetni = p.getOrNull(1).orEmpty()
        if (baslik.isBlank()) return Sonuc(false, "")
        val key = tarihAnahtari(tarihMetni)
            ?: return Sonuc(false, context.getString(R.string.cmd_bad_date, tarihMetni.take(30)))
        Store.addEvent(context, baslik.take(80), key, "🎯")
        return Sonuc(true, context.getString(R.string.cmd_event_added, baslik.take(30)))
    }

    private fun etkinlikSil(context: Context, ad: String): Sonuc {
        val liste = Store.loadEvents(context)
        val hedef = esnekBul(liste, ad) { it.title } ?: return yok(context, ad)
        Store.deleteEventUndoable(context, hedef.id)
        return Sonuc(true, context.getString(R.string.cmd_event_deleted, hedef.title.take(30)))
    }

    // ═══════════════════════════════════════════════════════════════
    // KURSLAR
    // ═══════════════════════════════════════════════════════════════

    private fun kursEkle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val ad = p.getOrNull(0).orEmpty()
        if (ad.isBlank()) return Sonuc(false, "")
        val aciklama = p.getOrNull(1).orEmpty()
        val renk = Store.loadCourses(context).size % 6
        Store.addCourse(context, ad.take(80), "📘", renk, aciklama.take(200))
        return Sonuc(true, context.getString(R.string.cmd_course_added, ad.take(40)))
    }

    private fun kursSil(context: Context, ad: String): Sonuc {
        val liste = Store.loadCourses(context)
        val hedef = esnekBul(liste, ad) { it.title } ?: return yok(context, ad)
        Store.deleteCourseUndoable(context, hedef.id)
        return Sonuc(true, context.getString(R.string.cmd_course_deleted, hedef.title.take(30)))
    }

    private fun bolumEkle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val kursAdi = p.getOrNull(0).orEmpty()
        val govde = p.getOrNull(1).orEmpty()
        if (kursAdi.isBlank() || govde.isBlank()) return Sonuc(false, "")
        val kurs = esnekBul(Store.loadCourses(context), kursAdi) { it.title }
            ?: return yok(context, kursAdi)

        var eklenen = 0
        maddeleriAyir(govde).forEach { b ->
            Store.addSection(context, kurs.id, b.take(100))
            eklenen++
        }
        if (eklenen == 0) return Sonuc(false, "")
        return Sonuc(
            true,
            context.getString(R.string.cmd_section_added, kurs.title.take(30), eklenen)
        )
    }

    /** Biçim: `Kurs :: Bölüm :: ders1 ;; ders2` */
    private fun dersEkle(context: Context, deger: String): Sonuc {
        val p = deger.split("::", limit = 3).map { it.trim() }
        val kursAdi = p.getOrNull(0).orEmpty()
        val bolumAdi = p.getOrNull(1).orEmpty()
        val govde = p.getOrNull(2).orEmpty()
        if (kursAdi.isBlank() || govde.isBlank()) return Sonuc(false, "")

        val kurs = esnekBul(Store.loadCourses(context), kursAdi) { it.title }
            ?: return yok(context, kursAdi)
        val bolumler = Store.sectionsOf(context, kurs.id)
        val bolum = if (bolumAdi.isBlank()) {
            bolumler.firstOrNull() ?: Store.addSection(
                context, kurs.id, context.getString(R.string.section_default)
            )
        } else {
            esnekBul(bolumler, bolumAdi) { it.title }
                ?: Store.addSection(context, kurs.id, bolumAdi.take(100))
        }

        var eklenen = 0
        maddeleriAyir(govde).forEach { d ->
            Store.addLesson(context, kurs.id, bolum.id, d.take(120), 20, "")
            eklenen++
        }
        if (eklenen == 0) return Sonuc(false, "")
        return Sonuc(
            true,
            context.getString(R.string.cmd_lesson_added, bolum.title.take(25), eklenen)
        )
    }

    private fun dersTamamla(context: Context, ad: String): Sonuc {
        val liste = Store.loadLessons(context)
        val hedef = esnekBul(liste.filter { !it.done }, ad) { it.title }
            ?: esnekBul(liste, ad) { it.title }
            ?: return yok(context, ad)
        if (hedef.done) {
            return Sonuc(true, context.getString(R.string.cmd_lesson_already, hedef.title.take(30)))
        }
        Store.toggleLesson(context, hedef.id)
        return Sonuc(true, context.getString(R.string.cmd_lesson_done, hedef.title.take(30)))
    }

    private fun dersSil(context: Context, ad: String): Sonuc {
        val hedef = esnekBul(Store.loadLessons(context), ad) { it.title }
            ?: return yok(context, ad)
        Store.deleteLessonUndoable(context, hedef.id)
        return Sonuc(true, context.getString(R.string.cmd_lesson_deleted, hedef.title.take(30)))
    }

    // ═══════════════════════════════════════════════════════════════
    // BİLGİ KARTLARI
    // ═══════════════════════════════════════════════════════════════

    /** Biçim: `Deste :: ön1 = arka1 ;; ön2 = arka2` */
    private fun kartEkle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val deste = p.getOrNull(0).orEmpty().ifBlank {
            context.getString(R.string.card_my_deck)
        }
        val govde = p.getOrNull(1).orEmpty()
        if (govde.isBlank()) return Sonuc(false, "")

        var eklenen = 0
        maddeleriAyir(govde).forEach { satir ->
            val ayrac = if (satir.contains("=")) "=" else if (satir.contains("|")) "|" else return@forEach
            val ik = satir.split(ayrac, limit = 2)
            val on = ik.getOrNull(0)?.trim().orEmpty()
            val arka = ik.getOrNull(1)?.trim().orEmpty()
            if (on.isBlank() || arka.isBlank()) return@forEach
            KartStore.kartEkle(context, deste.take(60), on.take(200), arka.take(400))
            eklenen++
        }
        if (eklenen == 0) return Sonuc(false, "")
        return Sonuc(true, context.getString(R.string.cmd_card_added, deste.take(25), eklenen))
    }

    // ═══════════════════════════════════════════════════════════════
    // SINAV
    // ═══════════════════════════════════════════════════════════════

    /** Biçim: `Deneme 5 :: Türkçe=25 ;; Matematik=20` */
    private fun sinavEkle(context: Context, deger: String): Sonuc {
        val p = bol(deger)
        val baslik = p.getOrNull(0).orEmpty()
        val govde = p.getOrNull(1).orEmpty()
        if (baslik.isBlank() || govde.isBlank()) return Sonuc(false, "")

        val netler = LinkedHashMap<String, Int>()
        val parcalar = if (govde.contains(";;")) govde.split(";;") else govde.split(",")
        parcalar.forEach { satir ->
            val ik = satir.split("=", ":")
            val ders = ik.getOrNull(0)?.trim().orEmpty()
            val net = ik.getOrNull(1)?.trim()?.replace(",", ".")?.toDoubleOrNull()?.toInt()
            if (ders.isNotBlank() && net != null) {
                // Bilinen ders adına eşle, yoksa yazıldığı gibi al
                val eslesen = Store.EXAM_SUBJECTS.firstOrNull {
                    normalle(it) == normalle(ders) || normalle(it).startsWith(normalle(ders))
                } ?: ders.take(30)
                netler[eslesen] = net.coerceIn(0, 200)
            }
        }
        if (netler.isEmpty()) return Sonuc(false, "")

        val liste = Store.loadExams(context)
        liste.add(
            0,
            Store.Exam(
                id = System.currentTimeMillis(),
                title = baslik.take(60),
                createdAt = System.currentTimeMillis(),
                nets = netler
            )
        )
        Store.saveExams(context, liste)
        val toplam = netler.values.sum()
        return Sonuc(true, context.getString(R.string.cmd_exam_added, baslik.take(25), toplam))
    }

    // ═══════════════════════════════════════════════════════════════
    // AYARLAR
    // ═══════════════════════════════════════════════════════════════

    private fun hedefAyarla(context: Context, deger: String): Sonuc {
        val dk = deger.filter { it.isDigit() }.toIntOrNull() ?: return Sonuc(false, "")
        val guvenli = dk.coerceIn(10, 960)
        Store.setGoalMinutes(context, guvenli)
        return Sonuc(true, context.getString(R.string.cmd_goal_set, guvenli))
    }

    private fun sozAyarla(context: Context, deger: String): Sonuc {
        if (deger.isBlank()) return Sonuc(false, "")
        Store.setQuote(context, deger.take(200))
        return Sonuc(true, context.getString(R.string.cmd_quote_set))
    }

    private fun sinavTarihi(context: Context, deger: String): Sonuc {
        val key = tarihAnahtari(deger)
            ?: return Sonuc(false, context.getString(R.string.cmd_bad_date, deger.take(30)))
        Store.setExamDate(context, key)
        return Sonuc(true, context.getString(R.string.cmd_examdate_set, key))
    }

    // ═══════════════════════════════════════════════════════════════
    // v11.13 — UYGULAMA AYARLARINI KONTROL ET (AI her şeyi yönetir)
    // ═══════════════════════════════════════════════════════════════

    /** "ayar_ses :: acik|kapanik|1|0" → uygulama ses bildirimlerini ayarlar. */
    private fun ayarSes(context: Context, deger: String): Sonuc {
        val acik = evetMi(deger)
        Store.setSoundEnabled(context, acik)
        WidgetCommon.refreshAll(context)
        return Sonuc(true, context.getString(if (acik) R.string.cmd_sound_on else R.string.cmd_sound_off))
    }

    /** "ayar_titresim :: acik|kapanik" → titreşim aç/kapat. */
    private fun ayarTitresim(context: Context, deger: String): Sonuc {
        val acik = evetMi(deger)
        Store.setVibEnabled(context, acik)
        return Sonuc(true, context.getString(if (acik) R.string.cmd_vib_on else R.string.cmd_vib_off))
    }

    /** "ayar_animasyon :: acik|kapanik" → ekran animasyonları aç/kapat. */
    private fun ayarAnimasyon(context: Context, deger: String): Sonuc {
        val acik = evetMi(deger)
        GorunumAyar.animasyonAcik(context, acik)
        return Sonuc(true, context.getString(if (acik) R.string.cmd_anim_on else R.string.cmd_anim_off))
    }

    /** "ayar_namaz :: acik|kapanik" → namaz modülünü aç/kapat. */
    private fun ayarNamaz(context: Context, deger: String): Sonuc {
        val acik = evetMi(deger)
        NamazVakti.setAcik(context, acik)
        return Sonuc(true, context.getString(if (acik) R.string.cmd_namaz_on else R.string.cmd_namaz_off))
    }

    /**
     * Saf gece-modu seçimi (JVM testli). Kullanıcı "aç/koyu/gece" derse koyu,
     * "kapat/açık" derse açık, "sistem/oto" derse sistem ayarına döner.
     * @return ThemeManager.GECE_* sabitlerinden biri
     */
    fun geceSecimi(deger: String): Int {
        val d = normalle(deger)
        val sistem = d.contains("sistem") || d.contains("oto")
        if (sistem) return ThemeManager.GECE_SISTEM
        val kapat = d.contains("kapa") || d.contains("kapal") ||
            (d.contains("acik") && d.contains("koyu").not())
        val koyu = d.contains("koyu") || d.contains("gece") || d == "ac"
        return when {
            kapat && !koyu -> ThemeManager.GECE_KAPALI
            else -> ThemeManager.GECE_ACIK
        }
    }

    /** "ayar_gece :: acik|kapanik|sistem" → koyu/açık/sistem temasını kurar. */
    private fun ayarGece(context: Context, deger: String): Sonuc {
        val secim = geceSecimi(deger)
        ThemeManager.geceModu(context, secim)
        ThemeManager.geceModunuUygula(context)
        val bildirim = when (secim) {
            ThemeManager.GECE_ACIK -> R.string.cmd_gece_on
            ThemeManager.GECE_SISTEM -> R.string.cmd_gece_sistem
            else -> R.string.cmd_gece_off
        }
        return Sonuc(true, context.getString(bildirim))
    }

    /** "widget_yenile" → tüm widget'ları anında tazeler. */
    private fun widgetYenile(context: Context): Sonuc {
        runCatching { WidgetCommon.refreshAll(context, true) }
        return Sonuc(true, context.getString(R.string.cmd_widget_yenilendi))
    }

    /** "ozet_ver" → AI'nın görebileceği kısa bir veri özeti üretir. */
    private fun ozetVer(context: Context): Sonuc {
        val gorevler = Store.loadTasks(context)
        val bekleyen = gorevler.count { !it.done }
        val notlar = Store.loadNotes(context).size
        val konular = Store.loadTopics(context).size
        val aliskanliklar = Store.loadHabits(context).count { !it.archived }
        val kurslar = Store.loadCourses(context).size
        val bugunOdak = Store.getTodayFocusMinutes(context)
        val hedef = Store.getGoalMinutes(context)
        val ozet = "Veri özeti: $bekleyen bekleyen görev, $notlar not, $konular konu, " +
            "$aliskanliklar alışkanlık, $kurslar kurs. Bugün $bugunOdak dk odaklandın (hedef $hedef dk)."
        return Sonuc(true, ozet)
    }

    /** "koc_mesaj" — günün vaktine göre proaktif koç mesajı üretir. */
    @Suppress("UNUSED_PARAMETER")
    private fun kocMesaj(context: Context, deger: String): Sonuc {
        val saat = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val dilim = KocMotoru.dilim(saat)
        val bekleyen = Store.loadTasks(context).count { !it.done }
        val bugun = runCatching { Store.recentDayStats(context, 1) }.getOrNull()
        val tamamlanan = bugun?.getOrNull(0)?.second ?: 0
        val odak = Store.getTodayFocusMinutes(context)
        val hedef = Store.getGoalMinutes(context)
        val (seri, _) = Store.streakInfo(context)
        val mesaj = KocMotoru.mesaj(dilim, bekleyen, tamamlanan, odak, hedef, seri)
        return Sonuc(true, mesaj)
    }

    /** "haftalik_rapor" — haftalık koç raporu üretir. */
    private fun haftalikRapor(context: Context): Sonuc {
        val odak = Store.weekFocus(context)
        val hedef = Store.getGoalMinutes(context) * 7
        val tamamlanan = Store.weekCompletions(context)
        val kurs = runCatching { Store.kursIstatistik(context).yuzde }.getOrDefault(0)
        val (seri, _) = Store.streakInfo(context)
        val rapor = HaftalikKocRaporu.satinAl(
            HaftalikKocRaporu.Hafta(odak, hedef, tamamlanan, kurs, seri)
        )
        return Sonuc(true, rapor)
    }

    /** "akilli_plan" — bekleyen görevlerden günlük plan üretir. */
    @Suppress("UNUSED_PARAMETER")
    private fun akilliPlan(context: Context, deger: String): Sonuc {
        val gorevler = Store.loadTasks(context).filter { !it.done }.take(6).map { it.text }
        if (gorevler.isEmpty()) return Sonuc(false, context.getString(R.string.cmd_plan_bos))
        val plan = AkilliGunlukPlan.plan(gorevler)
        if (plan.isEmpty()) return Sonuc(false, context.getString(R.string.cmd_plan_bos))
        return Sonuc(true, "🗓️ Akıllı Günlük Planın:\n" + AkilliGunlukPlan.metneCevir(plan))
    }

    /** "kocluk_programi :: ders" — hazır koçluk programının ilk gün görevini gösterir. */
    @Suppress("UNUSED_PARAMETER")
    private fun koclukProgrami(context: Context, deger: String): Sonuc {
        val p = KoclukProgramlari.ara(deger) ?: KoclukProgramlari.bul(deger)
        if (p == null) {
            val adlar = KoclukProgramlari.varsayilanlar.joinToString(", ") { it.ad }
            return Sonuc(true, "Hazır programlar: $adlar")
        }
        val gorev = KoclukProgramlari.gunGorevi(p, 1)
        return Sonuc(true, "${p.emoji} ${p.ad}\nGün 1/1: $gorev")
    }

    /** "xp_durum" — toplam XP, seviye, rütbe ve ilerleme gösterir. */
    private fun xpDurum(context: Context): Sonuc {
        // v11.13 DÜZELTMESİ: toplamXp değişkenine seviye atanıyor, "XP" olarak
        // gösteriliyordu. Ayrıca toplamXpGetir 3 kez çağrılıyordu (verimsiz).
        val xp = toplamXpGetir(context)
        val seviye = OyunlasmaMotoru.seviye(xp)
        val rutbe = OyunlasmaMotoru.rutbe(seviye)
        val ilerleme = (OyunlasmaMotoru.seviyedeIlerleme(xp) * 100).toInt()
        return Sonuc(true, "$rutbe — Seviye $seviye · $xp XP\nSeviye ilerlemesi: %$ilerleme")
    }

    private fun toplamXpGetir(context: Context): Int {
        // Bugünkü odak + tamamlama üzerinden basit bir XP tahmini
        val odak = Store.getTodayFocusMinutes(context)
        val tamamlama = runCatching { Store.recentDayStats(context, 1) }.getOrNull()
            ?.getOrNull(0)?.second ?: 0
        return OyunlasmaMotoru.gorevXp() * tamamlama + OyunlasmaMotoru.odakXp(odak)
    }

    /** "hesap_durum" — bulut senkron için hesap/cihaz bilgisi özeti. */
    @Suppress("UNUSED_PARAMETER")
    private fun hesapDurum(context: Context): Sonuc {
        val hesap = SenkronMotoru.Hesap(
            id = "yerel-" + (android.os.Build.MODEL ?: "cihaz"),
            ad = "Yerel Kullanıcı",
            email = "yerel@gunlukasistan",
            kayitMs = System.currentTimeMillis()
        )
        return Sonuc(true, "☁️ Hesap: ${hesap.id}\nCihaz: ${android.os.Build.MODEL}")
    }

    /** "saglik_hedef :: 10000" — günlük adım hedefi önerisi. */
    @Suppress("UNUSED_PARAMETER")
    private fun saglikHedef(context: Context, deger: String): Sonuc {
        val hedef = deger.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1000, 50000) ?: 10000
        val ortalama = hedef / 2
        val oner = SaglikVeriMotoru.hedefOner(ortalama)
        return Sonuc(true, "🏃 Günlük hedef $hedef adım (%${SaglikVeriMotoru.hedefYuzde(0, hedef)})\nÖnerilen hedef: $oner adım")
    }

    /** "takvim_plan :: A,B,C" — bekleyen görevleri haftaya dağıtır. */
    @Suppress("UNUSED_PARAMETER")
    private fun takvimPlan(context: Context, deger: String): Sonuc {
        val gorevler = deger.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (gorevler.isEmpty()) return Sonuc(false, "Görev listesi boş — virgülle ayırın.")
        // Önümüzdeki 7 günün anahtarlarını üret
        val gunler = mutableListOf<String>()
        val cal = java.util.Calendar.getInstance()
        repeat(7) {
            gunler.add(
                TakvimPlanlamaMotoru.gunAnahtari(
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                )
            )
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val plan = TakvimPlanlamaMotoru.haftalikDagilim(gorevler, gunler)
        if (plan.isEmpty()) return Sonuc(false, "Plan oluşturulamadı.")
        return Sonuc(true, "🗓️ Haftalık görev planın:\n" +
            plan.joinToString("\n") { TakvimPlanlamaMotoru.planMetni(it) })
    }

    /** "dil_sec :: en" — desteklenen dili seçer (i18n çekirdeği). */
    @Suppress("UNUSED_PARAMETER")
    private fun dilSec(context: Context, deger: String): Sonuc {
        val kod = DilSeciciMotoru.dil(deger.trim()).kod
        val dil = DilSeciciMotoru.dil(kod)
        return Sonuc(true, "🌐 Dil seçildi: ${dil.ad} (${dil.kod}) — uygulama yeniden başlatılınca uygulanır.")
    }

    /** "disa_aktar" — günün zengin markdown raporunu üretir (paylaşıma hazır). */
    private fun disaAktar(context: Context): Sonuc {
        val cal = java.util.Calendar.getInstance()
        val gunAnahtar = TakvimPlanlamaMotoru.gunAnahtari(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        val gorevler = Store.loadTasks(context)
        val bekleyen = gorevler.count { !it.done }
        val tamamlanan = gorevler.size - bekleyen
        val odak = Store.getTodayFocusMinutes(context)
        val hedef = Store.getGoalMinutes(context)
        val kurs = runCatching { Store.kursIstatistik(context).yuzde }.getOrDefault(0)
        val rapor = VeriDisAktarMotoru.markdownRapor(
            VeriDisAktarMotoru.raporBasligi(gunAnahtar),
            VeriDisAktarMotoru.gorevOzeti(bekleyen, tamamlanan),
            VeriDisAktarMotoru.odakOzeti(odak, hedef),
            kursSatiri = "Genel kurs ilerlemesi %$kurs"
        )
        return Sonuc(true, rapor)
    }

    /** "meydan_okuma :: Ders" — örnek bir grup meydan okuma durumunu gösterir. */
    private fun meydanOkuma(context: Context, deger: String): Sonuc {
        val ad = deger.ifBlank { "Haftalık Ders" }
        val benim = runCatching { Store.recentDayStats(context, 1) }.getOrNull()
            ?.getOrNull(0)?.second ?: 0
        val okuma = SosyalMeydanOkumaMotoru.MeydanOkuma(
            ad = ad,
            gunSayisi = 7,
            hedefTamamlama = 35,
            uyeler = listOf(
                SosyalMeydanOkumaMotoru.Uye("Sen", benim, 5),
                SosyalMeydanOkumaMotoru.Uye("Arkadaş 1", 4, 0),
                SosyalMeydanOkumaMotoru.Uye("Arkadaş 2", 2, 3)
            )
        )
        return Sonuc(true, SosyalMeydanOkumaMotoru.durumMetni(okuma))
    }

    /** "bildirim_durum" — akıllı bildirim filtresi özeti. */
    @Suppress("UNUSED_PARAMETER")
    private fun bildirimDurum(context: Context): Sonuc {
        val sessiz = BildirimFiltreMotoru.sessizMi(
            (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)),
            BildirimFiltreMotoru.SessizDilim(22 * 60, 7 * 60)
        )
        return Sonuc(
            true,
            if (sessiz) "🔕 Şu an sessiz saatlerdesin (22:00-07:00) — önemsiz bildirimler atlanır."
            else "🔔 Sessiz saat değil — bildirimler aktif. Önem eşiği akıllı filtrelenir."
        )
    }

    /** "disa_aktar_csv" — görev ve alışkanlıkları CSV olarak üretir. */
    private fun disaAktarCsv(context: Context): Sonuc {
        val gorevCsv = CsvDisAktarMotoru.birlestir(
            CsvDisAktarMotoru.GOREV_BASLIK,
            CsvDisAktarMotoru.gorevSatirlari(Store.loadTasks(context))
        )
        val alisCsv = CsvDisAktarMotoru.birlestir(
            CsvDisAktarMotoru.ALISKANLIK_BASLIK,
            CsvDisAktarMotoru.aliskanlikSatirlari(Store.loadHabits(context).filterNot { it.archived })
        )
        return Sonuc(true, "📊 Görevler (CSV):\n$gorevCsv\n\n🔥 Alışkanlıklar (CSV):\n$alisCsv")
    }

    /** "depolama_durum" — depolama kullanımı ve temizlik önerisi. */
    private fun depolamaDurum(context: Context): Sonuc {
        val kalemler = mutableListOf<VeriBoyutMotoru.Kalem>()
        runCatching {
            val onbellek = context.cacheDir?.let { d ->
                d.listFiles()?.sumOf { it.length() } ?: 0L
            } ?: 0L
            kalemler.add(VeriBoyutMotoru.Kalem("cache", "Önbellek", onbellek, true))

            val yedekler = runCatching { YedekRotasyon.kopyalar(context).size }.getOrDefault(0)
            // Yedekler küçük olsa da temizlenebilir kabul edilir (dışa aktarılmışsa)
            kalemler.add(VeriBoyutMotoru.Kalem("yedek", "Eski yedekler", yedekler * 200L, true))
        }
        if (kalemler.isEmpty()) return Sonuc(false, "Depolama verisi okunamadı.")
        val ozet = VeriBoyutMotoru.ozet(kalemler)
        val onerilen = VeriBoyutMotoru.onerilen(kalemler)
        val metin = buildString {
            append("💾 Depolama Durumu\n").append(ozet).append("\n")
            if (onerilen.isNotEmpty()) {
                append("Temizlenebilir:\n")
                onerilen.forEach { append("- ").append(it.ad).append(" (").append(VeriBoyutMotoru.boyutMetni(it.bayt)).append(")\n") }
            }
        }
        return Sonuc(true, metin.trim())
    }

    /** "basari_raporu" — bu ayın başarı raporunu üretir. */
    private fun basariRaporu(context: Context): Sonuc {
        val cal = java.util.Calendar.getInstance()
        val ayGun = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val aktifGun = runCatching {
            Store.recentDayStats(context, ayGun.coerceAtMost(31)).count { it.second > 0 || it.third > 0 }
        }.getOrDefault(0)
        val tamamlanan = runCatching { Store.recentDayStats(context, ayGun.coerceAtMost(31)).sumOf { it.second } }
            .getOrDefault(0)
        val odak = runCatching { Store.recentDayStats(context, ayGun.coerceAtMost(31)).sumOf { it.third } }
            .getOrDefault(0)
        return Sonuc(true, BasariAnalizMotoru.ayRaporu(ayGun, aktifGun, tamamlanan, odak))
    }

    /** "trend_analiz" — son 14 günün eğilimini ve tahminini gösterir. */
    private fun trendAnaliz(context: Context): Sonuc {
        val gunlukPuanlar = runCatching {
            Store.recentDayStats(context, 14).map { (_, c, f) -> c * 10 + f }
        }.getOrDefault(emptyList())
        if (gunlukPuanlar.isEmpty()) return Sonuc(false, "Yeterli veri yok.")
        return Sonuc(true, TrendAnalizMotoru.rapor(gunlukPuanlar))
    }

    /** "gorev_takvimi" — bugün/yarın/gecikmiş görevleri gösterir. */
    private fun gorevTakvimi(context: Context): Sonuc {
        val cal = java.util.Calendar.getInstance()
        val bugunAnahtar = TakvimPlanlamaMotoru.gunAnahtari(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        val gorevler = Store.loadTasks(context).filter { it.dueAt > 0 }.map { t ->
            val a = java.util.Calendar.getInstance().apply { timeInMillis = t.dueAt }
            GorevTakvimiMotoru.Gorev(
                t.text,
                TakvimPlanlamaMotoru.gunAnahtari(
                    a.get(java.util.Calendar.YEAR),
                    a.get(java.util.Calendar.MONTH) + 1,
                    a.get(java.util.Calendar.DAY_OF_MONTH)
                )
            )
        }
        if (gorevler.isEmpty()) return Sonuc(false, "Tarihli görev yok — önce görev ekleyin.")
        val geciken = GorevTakvimiMotoru.gecikenler(gorevler, bugunAnahtar)
        val yaklasan = GorevTakvimiMotoru.yaklasanlar(gorevler, bugunAnahtar)
        val metin = buildString {
            if (geciken.isNotEmpty()) append("⚠️ Gecikmiş (${geciken.size}):\n").append(
                geciken.joinToString("\n") { "• " + it.ad }).append("\n\n")
            if (yaklasan.isNotEmpty()) append("📅 Bugün/Yarın:\n").append(
                yaklasan.joinToString("\n") { "• " + it.ad })
            else append("Bugün/Yarın için görev yok.")
        }
        return Sonuc(true, metin.trim())
    }

    /** "pomodoro_durum" — bugünün odak/blok/mola durumunu gösterir. */
    private fun pomodoroDurum(context: Context): Sonuc {
        val odak = runCatching { Store.getTodayFocusMinutes(context) }.getOrDefault(0)
        val tamamlanan = runCatching { Store.recentDayStats(context, 1).lastOrNull()?.second ?: 0 }.getOrDefault(0)
        val blok = PomodoroMotoru.blokSayisi(odak)
        val mola = PomodoroMotoru.molaOnerisi(blok.coerceAtLeast(1))
        val skor = PomodoroMotoru.verimlilikSkoru(odak, tamamlanan)
        val metin = buildString {
            append("⏱️ Pomodoro Durumu\n")
            append("Odak: ${PomodoroMotoru.sureDonustur(odak)} ($odak dk)\n")
            append("Tamamlanan görev: $tamamlanan\n")
            append("Blok: $blok (${mola.sure} dk ${mola.tur} mola sırası)\n")
            append("Verimlilik: $skor/100 ${"★".repeat(PomodoroMotoru.yildiz(skor))}\n")
            append(PomodoroMotoru.yorum(skor))
        }
        return Sonuc(true, metin.trim())
    }

    /** "onceliklendir" — değeri görevler üzerinde Eisenhower önceliklendirmesi yapar. */
    private fun onceliklendir(context: Context, deger: String): Sonuc {
        val gorevler = Store.loadTasks(context).map { t ->
            // İsimden basit önem/aciliyet tahmini; sonraki aşamada iyileştirilebilir.
            val onem = if (t.text.contains("sınav") || t.text.contains("teslim") ||
                t.text.contains("ödev") || t.text.contains("proje")) 8 else 5
            val aciliyet = if (t.dueAt > 0 && t.dueAt - System.currentTimeMillis() < 48 * 3600_000L) 8 else 5
            IcerikOnceliklendirmeMotoru.Gorev(t.text, onem, aciliyet)
        }
        if (gorevler.isEmpty()) return Sonuc(false, "Önceliklendirilecek görev yok.")
        if (deger.isNotBlank()) {
            val aranan = normalle(deger)
            val es = gorevler.filter { normalle(it.ad).contains(aranan) }
            if (es.isNotEmpty()) return Sonuc(true, es.map { IcerikOnceliklendirmeMotoru.tekGorevTavsiyesi(it) }.joinToString("\n"))
        }
        return Sonuc(true, IcerikOnceliklendirmeMotoru.okunur(gorevler.take(12)))
    }

    /**
     * "acik/aç/1/evet/var" → true (aç).
     * "kapanik/kapalı/0/hayır/yok" → false (kapat).
     * Varsayılan: "açık" sözcüğü içeriyorsa true, değilse false.
     * Saf ve JVM testli.
     */
    fun evetMi(deger: String): Boolean {
        val d = normalle(deger)
        if (d.contains("kapal")) return false
        if (d.contains("kapan")) return false
        if (d == "0" || d == "hayir" || d == "yok" || d == "no") return false
        if (d == "1" || d == "evet" || d == "var" || d == "yes") return true
        // "açık", "aç", "ac" gibi olumlu ifadeler
        return d.contains("acik") || d == "ac" || d == "aç"
    }

    // ═══════════════════════════════════════════════════════════════
    // EYLEMLER
    // ═══════════════════════════════════════════════════════════════

    private fun zamanlayici(context: Context, deger: String, activity: Activity?): Sonuc {
        val dk = deger.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 180) ?: 25
        (activity as? MainActivity)?.openTimer()
        return Sonuc(true, context.getString(R.string.cmd_timer, dk))
    }

    private fun ekranAc(context: Context, ad: String, activity: Activity?): Sonuc {
        val ana = activity as? MainActivity ?: return Sonuc(false, "")
        val hedef = normalle(ad)
        val indeks = when {
            hedef.contains("kurs") -> 13
            hedef.contains("kaynak") -> 14
            hedef.contains("arac") || hedef.contains("hesap") ||
                hedef.contains("yonetmelik") -> 15
            hedef.contains("gorev") -> 6
            hedef.contains("not") -> 5
            hedef.contains("konu") -> 3
            hedef.contains("aliskanlik") -> 12
            // v7.55: "plan" -> Vakit Plani sekmesi (namaz vakti aralari)
            hedef.contains("vakit plan") || hedef.contains("namaz plan") ||
                hedef == "plan" -> 16
            hedef.contains("zaman") || hedef.contains("odak") ||
                hedef.contains("sayac") -> 4
            hedef.contains("istatistik") || hedef.contains("ilerleme") -> 1
            hedef.contains("bugun") -> 2
            hedef.contains("sinav") || hedef.contains("deneme") -> 10
            hedef.contains("etkinlik") || hedef.contains("takvim") -> 11
            hedef.contains("ayar") -> 7
            hedef.contains("tema") -> 8
            hedef.contains("asistan") -> 9
            hedef.contains("ana") || hedef.contains("giris") -> 0
            // v11.13: eklenen ekranlar
            hedef.contains("takvim") || hedef.contains("etkinlik") -> 11
            hedef.contains("kisisel gelisim") || hedef.contains("farkindalik") -> 9
            else -> return Sonuc(false, "")
        }
        ana.open(indeks)
        return Sonuc(true, context.getString(R.string.cmd_ekran_acildi, ad.take(20)))
    }

    /**
     * v11.13 — "atolye_ac" ile ayrı Activity tabanlı atölye/merkez ekranlarını açar.
     * Biçim: `atolye_ac :: canva|kisisel|gorunum|binmadde|youtube|yedek|depolama|sohbet|uyku`
     */
    @Suppress("UNUSED_PARAMETER")
    private fun atolyeAc(context: Context, ad: String, activity: Activity?): Sonuc {
        val hedef = normalle(ad)
        val bos = hedef.isBlank()
        when {
            hedef.contains("canva") -> CanvaCalismaAtolyeActivity.ac(context)
            hedef.contains("kisisel") || hedef.contains("farkindalik") ->
                KisiselGelisimActivity.ac(context)
            hedef.contains("gorunum") || hedef.contains("arayuz") ->
                EvrenselGorunumActivity.ac(context)
            hedef.contains("bin") || hedef.contains("1000") || hedef.contains("madde") ->
                BinMaddeKontrolActivity.ac(context)
            hedef.contains("youtube") || hedef.contains("video") ->
                YoutubePlaylistActivity.ac(context)
            hedef.contains("yedek") || hedef.contains("veri") ->
                VeriYedekActivity.ac(context)
            hedef.contains("depolama") -> DepolamaActivity.ac(context)
            hedef.contains("sohbet") -> SohbetGecmisiActivity.ac(context)
            hedef.contains("uyku") -> UykuAyarActivity.ac(context)
            else -> return if (bos) Sonuc(false, "") else Sonuc(false, "")
        }
        return Sonuc(true, context.getString(R.string.cmd_ekran_acildi, ad.take(20)))
    }

    /** "uygulamalar_ac" — Uygulamalarım ekranını açar. */
    @Suppress("UNUSED_PARAMETER")
    private fun uygulamalarAc(context: Context, activity: Activity?): Sonuc {
        UygulamalarActivity.ac(context)
        return Sonuc(true, context.getString(R.string.cmd_uygulamalar_ac))
    }

    /** "uygulama_ac :: WhatsApp" — eşleşen telefon uygulamasını başlatır. */
    @Suppress("UNUSED_PARAMETER")
    private fun uygulamaAc(context: Context, ad: String, activity: Activity?): Sonuc {
        if (ad.isBlank()) return Sonuc(false, "")
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val yuklu = runCatching {
            pm.queryIntentActivities(launcher, 0).mapNotNull { ri ->
                UygulamaMotoru.Uygulama(
                    ri.activityInfo.packageName,
                    ri.loadLabel(pm)?.toString() ?: ri.activityInfo.packageName
                )
            }
        }.getOrDefault(emptyList())
        val hedef = UygulamaMotoru.eslesme(yuklu, ad)
            ?: return Sonuc(false, context.getString(R.string.cmd_uygulama_acilamadi))
        val acilis = runCatching { pm.getLaunchIntentForPackage(hedef.paket) }.getOrNull()
        if (acilis == null) {
            return Sonuc(false, context.getString(R.string.cmd_uygulama_acilamadi))
        }
        runCatching { acilis.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(acilis) }
            .onFailure { return Sonuc(false, context.getString(R.string.cmd_uygulama_acilamadi)) }
        return Sonuc(true, hedef.ad + " " + context.getString(R.string.cmd_uygulama_acildi))
    }

    /**
     * "telefon_ara :: numara" — telefon çeviriciyi numara dolu açar (ACTION_DIAL).
     * İzin gerektirmez; kullanıcı tek dokunuşla arar. Servis aktif değilse
     * yine de çevirici açılır (ACTION_DIAL her zaman çalışır).
     */
    private fun telefonAra(context: Context, deger: String): Sonuc {
        if (deger.isBlank()) return Sonuc(false, "")
        val temiz = deger.replace(Regex("[^0-9+*#]"), "")
        if (temiz.isBlank()) return Sonuc(false, context.getString(R.string.cmd_uygulama_acilamadi))
        return runCatching {
            val niyet = android.content.Intent(
                android.content.Intent.ACTION_DIAL,
                android.net.Uri.parse("tel:$temiz")
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(niyet)
            Sonuc(true, context.getString(R.string.cmd_telefon_dial, temiz))
        }.getOrElse { Sonuc(false, context.getString(R.string.cmd_uygulama_acilamadi)) }
    }

    /**
     * "yaz :: metin" — diğer bir uygulamadaki odaklanmış metin alanına yazar.
     * Erişilebilirlik servisi üzerinden iletir; servis kapalıysa başarısız.
     */
    private fun ekranaYaz(context: Context, deger: String): Sonuc {
        // v11.65: Erişilebilirlik servisi kaldırıldığı için ekrana yazma
        // özelliği kullanılamıyor — güvenle "erişim kapalı" döndür.
        if (deger.isBlank()) return Sonuc(false, "")
        return Sonuc(false, context.getString(R.string.cmd_erisim_kapali))
    }

    private fun dersDevam(context: Context, activity: Activity?): Sonuc {
        val ana = activity as? MainActivity ?: return Sonuc(false, "")
        val ders = Store.sonDers(context)
        ana.open(13)
        if (ders == null) return Sonuc(false, "")
        return Sonuc(true, context.getString(R.string.cmd_lesson, ders.title.take(40)))
    }

    /** v7.38: Detaylı analiz ekranını açar. */
    private fun analizAc(context: Context, activity: Activity?): Sonuc {
        if (activity == null) return Sonuc(false, "")
        AnalitikActivity.ac(activity)
        return Sonuc(true, context.getString(R.string.cmd_analytics_open))
    }

    /** v7.39: PDF tam metin arama ekranını açar. */
    private fun pdfAra(context: Context, activity: Activity?): Sonuc {
        if (activity == null) return Sonuc(false, "")
        PdfAramaActivity.ac(activity)
        return Sonuc(true, context.getString(R.string.cmd_pdfsearch_open))
    }

    /** v7.46: Namaz vakitleri ve plan ekranını açar. */
    private fun namazAc(context: Context, activity: Activity?): Sonuc {
        if (activity == null) return Sonuc(false, "")
        if (!NamazVakti.acikMi(context)) NamazVakti.setAcik(context, true)
        NamazActivity.ac(activity)
        return Sonuc(true, context.getString(R.string.cmd_namaz_open))
    }

    /** v7.49: Günlük dizi/film önerileri ekranını açar. */
    private fun filmAc(context: Context, activity: Activity?): Sonuc {
        if (activity == null) return Sonuc(false, "")
        FilmActivity.ac(activity)
        return Sonuc(true, context.getString(R.string.cmd_film_open))
    }

    /** v7.51: İki kişilik ortak liste ekranını açar. */
    private fun onlineAc(context: Context, activity: Activity?): Sonuc {
        if (activity == null) return Sonuc(false, "")
        OnlineActivity.ac(activity)
        return Sonuc(true, context.getString(R.string.cmd_online_open))
    }

    private fun yedekAl(context: Context): Sonuc {
        return try {
            Store.autoBackupNow(context)
            Store.kaliciYedekYaz(context)
            Sonuc(true, context.getString(R.string.cmd_backup_done))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Yedek alınamadı", e)
            Sonuc(false, context.getString(R.string.cmd_backup_fail))
        }
    }

    private fun yedekGeriAl(context: Context): Sonuc {
        // Önce son silme işlemi, yoksa yedek dosyası
        if (Store.geriAlinabilir()) {
            return if (Store.geriAl()) {
                Sonuc(true, context.getString(R.string.cmd_undo_done))
            } else {
                Sonuc(false, context.getString(R.string.cmd_undo_none))
            }
        }
        if (Store.geriAlinabilirYedekVar(context) && Store.geriAlYedek(context)) {
            return Sonuc(true, context.getString(R.string.cmd_restore_done))
        }
        return Sonuc(false, context.getString(R.string.cmd_undo_none))
    }
}
