#!/usr/bin/env python3
# v10.14 · Grup E toplu düzenleme — bellekte assert, tek atomik yazım.
import io

TABAN = "/home/user/GunlukAsistan/"

def oku(p):
    with io.open(p, "r", encoding="utf-8") as f:
        return f.read()

def degistir(s, eski, yeni):
    n = s.count(eski)
    assert n == 1, f"Çapa {n} kez bulundu (1 olmalı): {eski[:70]!r}"
    return s.replace(eski, yeni)

degisiklikler = {}
def kaydet(p, icerik):
    assert p not in degisiklikler, f"Çift yazım: {p}"
    degisiklikler[p] = icerik

# ═════════════════════════ 1 · strings.xml ═════════════════════════
p = TABAN + "app/src/main/res/values/strings.xml"
s = oku(p)
assert 'name="ge_' not in s

hy = '''    <!-- ═══════════ v10.14 · ULTRA-30 / GRUP E (E25–E30) ═══════════ -->
    <!-- E25 · Sabah planı -->
    <string name="ge_sabah_baslik">🌅 Gün planın hazır</string>
    <string name="ge_sabah_isle">Görevlere işle</string>
    <string name="ge_sabah_gec">Geç</string>
    <string name="ge_sabah_islendi">%1$d öncelik güne işlendi</string>
    <!-- E26 · Kronotip kartı -->
    <string name="ge_kronotip_baslik">🧭 Kronotip kartın</string>
    <string name="ge_kronotip_veri_az">Kronotip için en az 5 gecelik uyku kaydı biriktir — defter doldukça kart burada oluşur.</string>
    <string name="ge_tip_serce">Erkenci serçe</string>
    <string name="ge_tip_guvencin">Dengeli güvercin</string>
    <string name="ge_tip_gece">Gece kuşu</string>
    <string name="ge_kronotip_uyanis">Ort. uyanış %1$s · yayılım %2$s</string>
    <string name="ge_kronotip_odak">En keskin odak penceren: %1$s</string>
    <string name="ge_kronotip_odak_veri_az">Odak penceresi için biraz daha odak kaydı birikmeli</string>
    <string name="ge_kronotip_simdi">▶ Penceredesin — 25 dk odak başlat</string>
    <string name="ge_kronotip_kur">⏰ %1$s için odak hatırlatması kur</string>
    <string name="ge_kronotip_kuruldu">Hatırlatma kuruldu: %1$s</string>
    <string name="ge_odak_hatirlatma_metin">🎯 Odak penceresi (kronotip önerisi)</string>
    <!-- E27 · Mikro günlük -->
    <string name="ge_gunluk_aksiyon">✍ 3 soruyla kapat</string>
    <string name="ge_gunluk_baslik">Mikro günlük</string>
    <string name="ge_gunluk_aciklama">Günü 30 saniyede kapat: bir puan, bir teşekkür, yarının tek şeyi. Hepsi bu.</string>
    <string name="ge_gunluk_puan">Bugün nasıldı?</string>
    <string name="ge_gunluk_tesekkur">Bugün için teşekkür</string>
    <string name="ge_gunluk_tesekkur_ipucu">Küçük bir şey yeter (ör. güzel bir kahve)</string>
    <string name="ge_gunluk_yarin">Yarının tek şeyi</string>
    <string name="ge_gunluk_yarin_ipucu">Tek bir öncelik yaz (isteğe bağlı)</string>
    <string name="ge_gunluk_kaydet">Kaydet ve günü kapat</string>
    <string name="ge_gunluk_kaydedildi">Gün kapatıldı 🌙</string>
    <string name="ge_duygu_baslik">💗 Duygu haritası (30 gün)</string>
    <string name="ge_duygu_bos">Henüz mikro günlük yok — bu gece iyi geceler bildirimindeki ✍ düğmesiyle başla.</string>
    <string name="ge_duygu_ozet">Ort. %1$.1f puan · %2$d iyi gün</string>
    <string name="ge_duygu_ekle">✍ Bugünü işaretle</string>
    <!-- E28 · Sesli gelen kutusu -->
    <string name="ge_kutu_baslik">Sesli gelen kutusu</string>
    <string name="ge_kutu_aciklama">%1$d sesli not — satıra dokun, hedefine git</string>
    <string name="ge_kutu_bos">Henüz sesli not yok. Bas-konuş-bırak ekranından ilk notu bıraktığında burada birikir.</string>
    <string name="ge_kutu_hafta">Bu hafta</string>
    <string name="ge_kutu_eski">Daha eski</string>
    <string name="ge_kutu_ac">📥 Gelen kutusu (%1$d)</string>
    <!-- E29 · Paylaşım kartı -->
    <string name="ge_kart_paylas">Kart olarak paylaş</string>
    <string name="ge_kart_paylas_hata">Kart paylaşılamadı</string>
    <string name="ge_kart_tamam">Tamamlandı · 🔥 %1$d günlük seri</string>
    <string name="ge_kart_bekliyor">Bugün yapılacak · Günlük Asistan</string>
    <!-- E30 · Senenin Filmi -->
    <string name="ge_film_baslik">Senenin Filmi</string>
    <string name="ge_film_oneri_baslik">🎬 Senenin Filmi hazır</string>
    <string name="ge_film_oneri_metin">%1$d\\'nın hikâyesi: en uzun gün, en güçlü seri, en çalışkan ay — Pofi sunar.</string>
    <string name="ge_film_izle">İzle</string>
    <string name="ge_film_sonra">Sonra</string>
    <string name="ge_film_bolum_baslik">🎬 Senenin Filmi</string>
    <string name="ge_film_bolum_alt">Aralık\\'ta ana ekranda otomatik önerilir; şimdi de izleyebilirsin.</string>
    <string name="ge_film_sahne_acilis">%1$d\\'nın hikâyesi</string>
    <string name="ge_film_sahne_acilis_alt">Bir yılı geriye sarıyoruz…</string>
    <string name="ge_film_sahne_seri">%1$d günlük en güçlü seri</string>
    <string name="ge_film_sahne_seri_alt">Zincir bir kez bile kırılmadı</string>
    <string name="ge_film_sahne_ay">En çalışkan ay: %1$s — %2$d dk odak</string>
    <string name="ge_film_sahne_ay_alt">Bu ay ritim zirvedeydi</string>
    <string name="ge_film_sahne_gun">%1$s — %2$d dk odak</string>
    <string name="ge_film_sahne_gun_alt">Yılın rekor günü</string>
    <string name="ge_film_sahne_final">Toplam %1$d dk odak · %2$d aktif gün</string>
    <string name="ge_film_sahne_final_alt">Pofi: «Sen bu yılı yaşadın. Kanıtın yukarıda.»</string>
    <string name="ge_film_paylas">📤 Özeti paylaş</string>
    <string name="ge_film_kapat">Kapat</string>
    <string name="ge_film_kart_baslik">%1$d\\'nın Filmi</string>
    <string name="ge_film_seri">En güçlü seri: %1$d gün</string>
    <string name="ge_film_ay">En çalışkan ay: %1$s (%2$d dk)</string>
    <string name="ge_film_gun">En uzun gün: %1$s (%2$d dk)</string>
    <string name="ge_film_toplam">Toplam odak: %1$d dk</string>
</resources>'''
s = degistir(s, "</resources>", hy)
kaydet(p, s)

# ═════════ 2 · UykuAksiyonReceiver.kt (E25 bayrak + E27 eylem) ═════════
p = TABAN + "app/src/main/java/com/gunlukasistan/app/UykuAksiyonReceiver.kt"
s = oku(p)

s = degistir(
    s,
    '''        try {
            BildirimUretici.tumKontroller(context, sabahMi = true)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sabah rutini teslim edilemedi", e)
        }
    }
''',
    '''        try {
            BildirimUretici.tumKontroller(context, sabahMi = true)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sabah rutini teslim edilemedi", e)
        }
        // v10.14 · E25: sabah planı taslağı — içerik varsa bayrak kalkar,
        // ana ekran ilk açıldığında tek seferlik diyalog gösterilir.
        try {
            val bugunSon = WidgetCommon.endOfToday()
            val taslak = SabahPlani.sec(
                Store.loadTasks(context).map {
                    SabahPlani.GorevOzet(it.id, it.text, it.dueAt, it.done)
                },
                bugunSon - 86_399_999L, bugunSon
            )
            if (taslak.isNotEmpty()) SabahPlani.beklemeyeAl(context)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Sabah planı hazırlanamadı", e)
        }
    }
''')

s = degistir(
    s,
    '''                .setContentIntent(uygulamayiAc(context, UykuCerceve.BILDIRIM_IYIGECELER))
                .setTimeoutAfter(4 * 3600_000L)
''',
    '''                .setContentIntent(uygulamayiAc(context, UykuCerceve.BILDIRIM_IYIGECELER))
                .setTimeoutAfter(4 * 3600_000L)
                // v10.14 · E27: mikro günlük kapısı — günü 3 soruyla kapat
                .addAction(
                    R.drawable.ic_uyku_aksam,
                    context.getString(R.string.ge_gunluk_aksiyon),
                    PendingIntent.getActivity(
                        context, 5000,
                        Intent(context, MikroGunlukActivity::class.java).apply {
                            data = android.net.Uri.parse("gunlukasistan://mikrogunluk")
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
''')
kaydet(p, s)

# ═════════════════════════ 3 · HomeFragment.kt ═════════════════════════
p = TABAN + "app/src/main/java/com/gunlukasistan/app/HomeFragment.kt"
s = oku(p)
s = degistir(
    s,
    '''        runCatching {
            DaralanBaslik.tazele(
                kaydirici = view as? android.widget.ScrollView,
                buyukBaslik = view?.findViewById(R.id.greetingText)
            )
        }
    }
''',
    '''        runCatching {
            DaralanBaslik.tazele(
                kaydirici = view as? android.widget.ScrollView,
                buyukBaslik = view?.findViewById(R.id.greetingText)
            )
        }
        // v10.14 · E25: uyandım sonrası sabah planı diyaloğu (tek seferlik)
        runCatching { SabahPlani.maybeGoster(requireActivity()) }
        // v10.14 · E30: Aralık'ta senenin filmi önerisi (yılda bir)
        runCatching { SeneFilmi.aralikOnerisi(requireActivity()) }
    }
''')
kaydet(p, s)

# ═════════════════════════ 4 · SesliNotActivity.kt ═════════════════════════
p = TABAN + "app/src/main/java/com/gunlukasistan/app/SesliNotActivity.kt"
s = oku(p)

s = degistir(
    s,
    '''        val onay = SesliNot.kaydet(this, secili, metin)
''',
    '''        // v10.14 · E28: işlenen not gelen kutusunda görünsün
        runCatching { SesliKutu.ekle(this, secili, metin) }
        val onay = SesliNot.kaydet(this, secili, metin)
''')

s = degistir(
    s,
    '''        kap.addView(dugme(getString(R.string.sn_iptal)) { finish() })
    }

    private fun tekrarDugmesi() {
''',
    '''        kap.addView(dugme(getString(R.string.sn_iptal)) { finish() })
        // v10.14 · E28: sesli gelen kutusu girişi (iz varsa)
        val kutuAdet = runCatching { SesliKutu.liste(this).size }.getOrDefault(0)
        if (kutuAdet > 0) {
            kap.addView(
                dugme(getString(R.string.ge_kutu_ac, kutuAdet)) {
                    startActivity(Intent(this, SesliKutuActivity::class.java))
                }
            )
        }
    }

    private fun tekrarDugmesi() {
''')
kaydet(p, s)

# ═════════════════════════ 5 · TasksFragment.kt (E29 menü satırı) ═════════
p = TABAN + "app/src/main/java/com/gunlukasistan/app/TasksFragment.kt"
s = oku(p)
s = degistir(
    s,
    '''        ogeler.add(AltSayfa.Oge(
            getString(R.string.delete), simge = "🗑", yikici = true
        ) { silOnayla(task) })
''',
    '''        // v10.14 · E29: görevi paylaşım kartına dök
        ogeler.add(AltSayfa.Oge(
            getString(R.string.ge_kart_paylas), simge = "🖼"
        ) {
            runCatching {
                val kart = KartUretici.gorevKarti(ctx, task)
                KartUretici.paylas(ctx, kart, "gorev_karti_${task.id}.png")
            }
        })

        ogeler.add(AltSayfa.Oge(
            getString(R.string.delete), simge = "🗑", yikici = true
        ) { silOnayla(task) })
''')
kaydet(p, s)

# ═════════════════════════ 6 · AnalitikActivity.kt ═════════════════════════
p = TABAN + "app/src/main/java/com/gunlukasistan/app/AnalitikActivity.kt"
s = oku(p)

s = degistir(
    s,
    '''import java.util.Locale
''',
    '''import java.util.Calendar
import java.util.Locale
''')

s = degistir(
    s,
    '''        kurslariCiz()
        aylariCiz()
    }
''',
    '''        kurslariCiz()
        aylariCiz()
        // v10.14 · E26/E27/E30: kronotip kartı, duygu haritası, sene filmi kapısı
        kronotipCiz()
        duyguHaritasiCiz()
        seneFilmiKapi()
    }
''')

s = degistir(
    s,
    '''    private fun raporPaylas() {
''',
    '''    // ═══════════════════════════════════════════════════════════════
    // v10.14 · E26 — KRONOTİP KARTI (uyku defteri + saat analizi tek kart)
    // ═══════════════════════════════════════════════════════════════

    private fun kronotipCiz() {
        val kap = findViewById<LinearLayout>(R.id.anContent) ?: return
        kap.addView(bolumBaslik(getString(R.string.ge_kronotip_baslik)))

        val uyanislar = UykuCerceve.defter(this).mapNotNull { gun ->
            if (gun.uyandiMs <= 0L) null
            else Calendar.getInstance().apply { timeInMillis = gun.uyandiMs }
                .let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        }
        if (uyanislar.size < 5) {
            kap.addView(satirYazi(getString(R.string.ge_kronotip_veri_az), 13f, 0.8f))
            return
        }

        val ort = Kronotip.ortUyanis(uyanislar)
        val tip = Kronotip.tip(ort)
        val tipAd = getString(
            when (tip) {
                Kronotip.Tip.SERCE -> R.string.ge_tip_serce
                Kronotip.Tip.GECE_KUSU -> R.string.ge_tip_gece
                else -> R.string.ge_tip_guvencin
            }
        )
        kap.addView(satirYazi(Kronotip.tipEmoji(tip) + "  " + tipAd, 16f, 1f))
        kap.addView(
            satirYazi(
                getString(
                    R.string.ge_kronotip_uyanis,
                    UykuCerceve.saatMetni(ort),
                    UykuCerceve.sureKisa(Kronotip.sapma(uyanislar) * 60_000L)
                ),
                12.5f, 0.75f
            )
        )

        val enIyi = Analitik.enVerimliSaat(this)
        if (enIyi < 0) {
            kap.addView(satirYazi(getString(R.string.ge_kronotip_odak_veri_az), 12.5f, 0.75f))
            return
        }
        val bas = Kronotip.odakPenceresi(enIyi)
        kap.addView(
            satirYazi(getString(R.string.ge_kronotip_odak, Kronotip.saatAralik(bas)), 13f, 0.9f)
        )
        kap.addView(
            aksiyonCip(kronotipAksiyonMetni(bas)) { kronotipAksiyon(bas) }
        )
    }

    private fun kronotipAksiyonMetni(bas: Int): String {
        val simdi = Calendar.getInstance()
        val simdiDk = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE)
        return if (Kronotip.penceredeMi(simdiDk, bas)) {
            getString(R.string.ge_kronotip_simdi)
        } else {
            getString(R.string.ge_kronotip_kur, String.format(Locale.US, "%02d:00", bas))
        }
    }

    /** Penceredeyse 25 dk odak yayını; değilse pencereye hatırlatma kur. */
    private fun kronotipAksiyon(bas: Int) {
        val simdi = Calendar.getInstance()
        val simdiDk = simdi.get(Calendar.HOUR_OF_DAY) * 60 + simdi.get(Calendar.MINUTE)
        if (Kronotip.penceredeMi(simdiDk, bas)) {
            sendBroadcast(
                Intent(this, TimerActionReceiver::class.java).apply {
                    action = TimerActionReceiver.ACTION_BASLAT_DK
                    putExtra(TimerActionReceiver.EXTRA_DAKIKA, 25)
                }
            )
            return
        }
        val hedef = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bas)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        try {
            val gorevler = Store.loadTasks(this)
            val yeni = Store.Task(
                id = System.currentTimeMillis(),
                text = getString(R.string.ge_odak_hatirlatma_metin),
                done = false,
                createdAt = System.currentTimeMillis(),
                dueAt = hedef.timeInMillis
            )
            gorevler.add(yeni)
            Store.saveTasks(this, gorevler)
            runCatching { AlarmScheduler.schedule(this, yeni.id, yeni.text, yeni.dueAt) }
            runCatching { WidgetCommon.refreshAll(this, true) }
            android.widget.Toast.makeText(
                this,
                getString(R.string.ge_kronotip_kuruldu, String.format(Locale.US, "%02d:00", bas)),
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.util.Log.w("AnalitikActivity", "Odak hatırlatması kurulamadı", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.14 · E27 — DUYGU HARİTASI (30 günlük mikro günlük şeridi)
    // ═══════════════════════════════════════════════════════════════

    private fun duyguHaritasiCiz() {
        val kap = findViewById<LinearLayout>(R.id.anContent) ?: return
        kap.addView(bolumBaslik(getString(R.string.ge_duygu_baslik)))

        val kayitlar = MikroGunluk.sonKac(this, 30)
        if (kayitlar.isEmpty()) {
            kap.addView(satirYazi(getString(R.string.ge_duygu_bos), 13f, 0.8f))
        } else {
            val serit = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            kayitlar.forEach { (anahtar, g) ->
                serit.addView(
                    TextView(this).apply {
                        text = (if (g.puan > 0) MikroGunluk.emojiFor(g.puan) else "▫") +
                            "\\n" + anahtar.takeLast(2)
                        textSize = 10f
                        gravity = Gravity.CENTER
                        setPadding(
                            (5 * yogunluk).toInt(), (3 * yogunluk).toInt(),
                            (5 * yogunluk).toInt(), (3 * yogunluk).toInt()
                        )
                    }
                )
            }
            kap.addView(
                android.widget.HorizontalScrollView(this).apply {
                    isHorizontalScrollBarEnabled = false
                    addView(serit)
                }
            )
            val puanlar = kayitlar.map { it.second.puan }.filter { it > 0 }
            if (puanlar.isNotEmpty()) {
                kap.addView(
                    satirYazi(
                        getString(
                            R.string.ge_duygu_ozet,
                            MikroGunluk.ortalama(puanlar).toDouble(),
                            MikroGunluk.iyiSayisi(puanlar)
                        ),
                        12.5f, 0.8f
                    )
                )
            }
        }
        kap.addView(
            aksiyonCip(getString(R.string.ge_duygu_ekle)) {
                startActivity(Intent(this, MikroGunlukActivity::class.java))
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // v10.14 · E30 — SENENİN FİLMİ KAPISI (Aralık'ta ana ekranda önerilir)
    // ═══════════════════════════════════════════════════════════════

    private fun seneFilmiKapi() {
        val kap = findViewById<LinearLayout>(R.id.anContent) ?: return
        kap.addView(bolumBaslik(getString(R.string.ge_film_bolum_baslik)))
        kap.addView(satirYazi(getString(R.string.ge_film_bolum_alt), 12.5f, 0.75f))
        kap.addView(
            aksiyonCip(getString(R.string.ge_film_izle)) {
                startActivity(Intent(this, SeneFilmiActivity::class.java))
            }
        )
    }

    // ---------------- v10.14 yardımcı görünümler ----------------

    private fun bolumBaslik(metin: String) = TextView(this).apply {
        text = metin
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, (22 * yogunluk).toInt(), 0, (4 * yogunluk).toInt())
    }

    private fun aksiyonCip(metin: String, tikla: () -> Unit): TextView {
        val vurgu = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, 0
        )
        return TextView(this).apply {
            text = metin
            textSize = 13.5f
            setTextColor(vurgu)
            setPadding(
                (16 * yogunluk).toInt(), (10 * yogunluk).toInt(),
                (16 * yogunluk).toInt(), (10 * yogunluk).toInt()
            )
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16 * yogunluk
                setStroke((1.5f * yogunluk).toInt(), vurgu)
                setColor((vurgu and 0x00FFFFFF) or 0x22000000)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * yogunluk).toInt() }
            isClickable = true
            setOnClickListener { tikla() }
        }
    }

    private fun raporPaylas() {
''')
kaydet(p, s)

# ═════════════════════════ 7 · AndroidManifest.xml ═════════════════════════
p = TABAN + "app/src/main/AndroidManifest.xml"
s = oku(p)
s = degistir(
    s,
    '''        <!-- v10.13 · B12: görev widget'ı etiket filtresi -->
        <activity
            android:name=".WidgetFiltreActivity"
            android:exported="false"
            android:label="@string/wg_filtre_baslik" />
''',
    '''        <!-- v10.13 · B12: görev widget'ı etiket filtresi -->
        <activity
            android:name=".WidgetFiltreActivity"
            android:exported="false"
            android:label="@string/wg_filtre_baslik" />

        <!-- v10.14 · E27: akşam mikro günlüğü -->
        <activity
            android:name=".MikroGunlukActivity"
            android:exported="false"
            android:label="@string/ge_gunluk_baslik" />

        <!-- v10.14 · E28: sesli gelen kutusu -->
        <activity
            android:name=".SesliKutuActivity"
            android:exported="false"
            android:label="@string/ge_kutu_baslik" />

        <!-- v10.14 · E30: senenin filmi -->
        <activity
            android:name=".SeneFilmiActivity"
            android:exported="false"
            android:label="@string/ge_film_baslik" />
''')
kaydet(p, s)

# ═════════════════════════ 8 · file_paths.xml ═════════════════════════
p = TABAN + "app/src/main/res/xml/file_paths.xml"
s = oku(p)
s = degistir(
    s,
    "</paths>",
    '''    <!-- v10.14 · E29/E30: paylaşım kartları (PNG) -->
    <cache-path name="kartlar" path="kartlar/" />
</paths>''')
kaydet(p, s)

# ═════════════════════════ 9 · build.gradle.kts ═════════════════════════
p = TABAN + "app/build.gradle.kts"
s = oku(p)
s = degistir(s, '        versionCode = 169\n        versionName = "10.13"',
                '        versionCode = 170\n        versionName = "10.14"')
kaydet(p, s)

# ═════════════════════════ TEK ATOMİK YAZIM ═════════════════════════
for yol, icerik in degisiklikler.items():
    veri = icerik.encode("utf-8", "strict")
    with io.open(yol, "wb") as f:
        f.write(veri)
    print("YAZILDI:", yol.rsplit("/", 2)[-2] + "/" + yol.rsplit("/", 1)[-1], len(veri), "bayt")
print(f"TAMAM — {len(degisiklikler)} dosya güncellendi")
