package com.gunlukasistan.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

/**
 * v10.56 — C, D, E, G, H, I ve J Kategorileri Gelişmiş Hayat Atölyesi ([GelismiAtolye]) Arayüzü.
 *
 * Kullanıcının 7 ana temadaki 70 öneri maddesinin temel ve gelişmiş manuel kontrolünü
 * tek ekrandan test edip yönettiği aktivite.
 */
class GelismiAtolyeActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, GelismiAtolyeActivity::class.java))
        }
    }

    private var aiTalimat = GelismiAtolye.AiKocTalimati()
    private var xpDurum = GelismiAtolye.XpDurumu()
    private var frekansAyar = GelismiAtolye.FrekansAyar(gamma40HzAcik = true, alfa10HzAcik = false)
    private var sprintAyar = GelismiAtolye.SprintAyar(calismaDk = 25, molaDk = 5, tasmaDk = 0)
    private var tasarimTercih = GelismiAtolye.TasarimTercihi()
    private var kpssDurum = GelismiAtolye.FeynmanCalismasi(cozulenSoruSayisi = 40, gunlukSoruHedefi = 100)
    private var depolamaAnaliz = GelismiAtolye.DepolamaAnaliz(notlarMb = 2.4f, pdflerMb = 14.8f, cacheMb = 6.2f)

    private lateinit var txtAiPromptDurum: TextView
    private lateinit var txtXpDurum: TextView
    private lateinit var txtFrekansDurum: TextView
    private lateinit var txtSprintDurum: TextView
    private lateinit var txtTasarimSablonDurum: TextView
    private lateinit var txtKpssDurum: TextView
    private lateinit var txtDepolamaDurum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gelismis_atolye)

        txtAiPromptDurum = findViewById(R.id.txtAiPromptDurum)
        txtXpDurum = findViewById(R.id.txtXpDurum)
        txtFrekansDurum = findViewById(R.id.txtFrekansDurum)
        txtSprintDurum = findViewById(R.id.txtSprintDurum)
        txtTasarimSablonDurum = findViewById(R.id.txtTasarimSablonDurum)
        txtKpssDurum = findViewById(R.id.txtKpssDurum)
        txtDepolamaDurum = findViewById(R.id.txtDepolamaDurum)

        ekraniGuncelle()

        // 100 Öneri Katalog Butonu
        findViewById<MaterialButton>(R.id.btnKatalogAc).setOnClickListener {
            Toast.makeText(this, "📚 100 Öneri Katalogu 100-YENI-ONERI-KATALOGU.md dosyasında mevcut! C-D-E-G-H-I-J modülleri aktif.", Toast.LENGTH_LONG).show()
        }

        // Modül C: NLP Görev Çıkar Test
        findViewById<MaterialButton>(R.id.btnGorevCikarTest).setOnClickListener {
            val ornekNot = "Yarın sabah KPSS tarih testini çöz ve 25 dakika odaklan"
            val cikarilan = GelismiAtolye.ModulC_OtonomAi.notlardanGorevCikar(ornekNot)
            Toast.makeText(this, "🤖 Nottan Görev Çıkarıldı: '$cikarilan'", Toast.LENGTH_LONG).show()
        }

        // Modül D: +15 XP (Combo)
        findViewById<MaterialButton>(R.id.btnXpEkle).setOnClickListener {
            xpDurum = GelismiAtolye.ModulD_Oyunlastirma.gorevTamamla(xpDurum, comboMi = true)
            ekraniGuncelle()
            val rutbe = GelismiAtolye.ModulD_Oyunlastirma.rutbeGetir(xpDurum.xp)
            Toast.makeText(this, "🏆 +15 XP Eklendi! Yeni Rütbe: $rutbe", Toast.LENGTH_SHORT).show()
        }

        // Modül D: +40m Maraton
        findViewById<MaterialButton>(R.id.btnMaratonEkle).setOnClickListener {
            xpDurum = GelismiAtolye.ModulD_Oyunlastirma.haftaSonuOdakEkle(xpDurum, 40)
            ekraniGuncelle()
            if (xpDurum.kupaKazandiMi) {
                Toast.makeText(this, "👑 TEBRİKLER! Hafta Sonu 120m Odak Kupası Kazanıldı!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "⏱️ +40m Maraton Eklendi (${xpDurum.haftaSonuOdakDk}/120 dk)", Toast.LENGTH_SHORT).show()
            }
        }

        // Modül E: Frekans Toggle
        findViewById<MaterialButton>(R.id.btnFrekansToggle).setOnClickListener {
            frekansAyar = GelismiAtolye.ModulE_SesVeFrekans.frekansToggle(frekansAyar, frekansAyar.gamma40HzAcik)
            ekraniGuncelle()
            Toast.makeText(this, "🎧 Frekans: ${GelismiAtolye.ModulE_SesVeFrekans.sesOzetiGetir(frekansAyar)}", Toast.LENGTH_SHORT).show()
        }

        // Modül G: Sprint Değiştir
        findViewById<MaterialButton>(R.id.btnSprintDegistir).setOnClickListener {
            val siradaki = when (sprintAyar.calismaDk) {
                25 -> 1 // 50-10
                50 -> 2 // 30-5
                30 -> 3 // 15-0
                else -> 0 // 25-5
            }
            sprintAyar = GelismiAtolye.ModulG_SayacVePip.hazirSprintSec(siradaki)
            ekraniGuncelle()
            Toast.makeText(this, "⏱️ Sprint Ayarlandı: ${sprintAyar.calismaDk}m / ${sprintAyar.molaDk}m", Toast.LENGTH_SHORT).show()
        }

        // Modül G: +5 Dk Taşma
        findViewById<MaterialButton>(R.id.btnTasmaEkle).setOnClickListener {
            sprintAyar = GelismiAtolye.ModulG_SayacVePip.tasmaSuresiEkle(sprintAyar, 5)
            ekraniGuncelle()
            Toast.makeText(this, "➕ +5 Dk Taşma Süresi Eklendi (Toplam: +${sprintAyar.tasmaDk} dk)", Toast.LENGTH_SHORT).show()
        }

        // Modül H: Şablon Değiştir
        findViewById<MaterialButton>(R.id.btnSablonDegistir).setOnClickListener {
            val yeniSablon = when (tasarimTercih.sablon) {
                GelismiAtolye.TasarimSablonu.KOMPAKT_YUVARLAK_16DP -> GelismiAtolye.TasarimSablonu.ULTRA_KESKIN_0DP
                GelismiAtolye.TasarimSablonu.ULTRA_KESKIN_0DP -> GelismiAtolye.TasarimSablonu.GECE_ZEN
                else -> GelismiAtolye.TasarimSablonu.KOMPAKT_YUVARLAK_16DP
            }
            tasarimTercih = tasarimTercih.copy(sablon = yeniSablon)
            ekraniGuncelle()
            Toast.makeText(this, "🎨 Şablon: ${yeniSablon.baslik}", Toast.LENGTH_SHORT).show()
        }

        // Modül I: +10 Soru
        findViewById<MaterialButton>(R.id.btnSoru10Ekle).setOnClickListener {
            kpssDurum = GelismiAtolye.ModulI_DersVeKpss.soruEkle(kpssDurum, 10)
            ekraniGuncelle()
            Toast.makeText(this, "✏️ +10 KPSS Sorusu Eklendi (${kpssDurum.cozulenSoruSayisi}/${kpssDurum.gunlukSoruHedefi})", Toast.LENGTH_SHORT).show()
        }

        // Modül I: Feynman Test
        findViewById<MaterialButton>(R.id.btnFeynmanTest).setOnClickListener {
            val cumle = "Kurtuluş Savaşı halkın birleşmesidir. Düzenli ordu kurulmuştur. Zafer kazanılmıştır."
            val skor = GelismiAtolye.ModulI_DersVeKpss.feynmanAnlatimSkoru(cumle)
            Toast.makeText(this, "💡 Feynman Anlatım Skoru: %$skor (Kısa & Net Cümleler)", Toast.LENGTH_LONG).show()
        }

        // Modül J: Önbellek Temizle
        findViewById<MaterialButton>(R.id.btnOnbellekTemizle).setOnClickListener {
            val eskiCache = depolamaAnaliz.cacheMb
            depolamaAnaliz = GelismiAtolye.ModulJ_SistemVeAnalitik.onbellekTemizle(depolamaAnaliz)
            ekraniGuncelle()
            Toast.makeText(this, "🧹 Önbellek Temizlendi: -$eskiCache MB", Toast.LENGTH_SHORT).show()
        }

        // Modül J: JSON Dışa Aktar
        findViewById<MaterialButton>(R.id.btnCdejDisaAktar).setOnClickListener {
            val json = GelismiAtolye.ModulJ_SistemVeAnalitik.cdejMasterJsonUret(
                aiTalimat, xpDurum, frekansAyar, sprintAyar, tasarimTercih, kpssDurum, depolamaAnaliz
            )
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("CdejMaster", json.toString()))
            Toast.makeText(this, "📋 7 Kategori (C-D-E-G-H-I-J) JSON Kopyalandı!", Toast.LENGTH_SHORT).show()
        }

        // Modül J: JSON İçe Aktar
        findViewById<MaterialButton>(R.id.btnCdejIceAktar).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val pano = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            runCatching {
                val obj = JSONObject(pano)
                if (GelismiAtolye.ModulJ_SistemVeAnalitik.cdejMasterJsonCoz(obj)) {
                    Toast.makeText(this, "📥 7 Kategori (C-D-E-G-H-I-J) Verisi Geri Yüklendi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "⚠️ Geçerli bir C-D-E-G-H-I-J paketi değil!", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(this, "⚠️ Pano verisi JSON formatında değil!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ekraniGuncelle() {
        txtAiPromptDurum.text = "AI Prompt: ${GelismiAtolye.ModulC_OtonomAi.promptHazirla(aiTalimat)}"
        
        val rutbe = GelismiAtolye.ModulD_Oyunlastirma.rutbeGetir(xpDurum.xp)
        val kupa = if (xpDurum.kupaKazandiMi) "EVET 👑" else "HAYIR"
        txtXpDurum.text = "XP: ${xpDurum.xp} · Rütbe: $rutbe · Hafta Sonu Odak: ${xpDurum.haftaSonuOdakDk}/120 dk (Kupa: $kupa)"

        txtFrekansDurum.text = GelismiAtolye.ModulE_SesVeFrekans.sesOzetiGetir(frekansAyar)
        txtSprintDurum.text = GelismiAtolye.ModulG_SayacVePip.sayacDurumMetni(sprintAyar)
        txtTasarimSablonDurum.text = GelismiAtolye.ModulH_TasarimVeAkordiyon.sablonMetniGetir(tasarimTercih)
        txtKpssDurum.text = GelismiAtolye.ModulI_DersVeKpss.kpssDurumMetni(kpssDurum)
        txtDepolamaDurum.text = GelismiAtolye.ModulJ_SistemVeAnalitik.depolamaMetniGetir(depolamaAnaliz)
    }
}
