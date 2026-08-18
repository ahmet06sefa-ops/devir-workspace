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
 * v10.55 — 10 Özel Yaşam Modülü & Manuel Kontrol Merkezi ([YasamModulleri]) Arayüzü.
 *
 * Kullanıcının her modülü tek ekrandan elle test edip kontrol edebildiği merkezi
 * yönetim aktivitesi.
 */
class YasamModulleriActivity : AppCompatActivity() {

    companion object {
        fun ac(context: Context) {
            context.startActivity(Intent(context, YasamModulleriActivity::class.java))
        }
    }

    // 10 Modülün Çalışma Anı Durumları
    private var ilac = YasamModulleri.IlacKaydi("B12 Vitamini", 1000, "08:00", true, false)
    private var abonelikler = listOf(
        YasamModulleri.Abonelik("Netflix", 150, 15, true),
        YasamModulleri.Abonelik("Spotify", 60, 20, false),
        YasamModulleri.Abonelik("Su & Elektrik", 450, 10, false)
    )
    private var suKafein = YasamModulleri.SuKafeinDurumu()
    private var rozetler = YasamModulleri.RozetKilitMerkezi.varsayilanRozetler()
    private var biyoPlan = YasamModulleri.UykuDonguPlan(uyumaSaat = 23, uyumaDakika = 0, donguSayisi = 5)
    private var ambientAyar = YasamModulleri.AmbientMikserAyari(yagmurSeviye = 50, gamma40HzAcik = true)
    private var harcamalar = mutableListOf(
        YasamModulleri.Harcama("Market", 200, "Kahvaltı"),
        YasamModulleri.Harcama("Ulaşım", 60, "Otobüs")
    )
    private var acilKasa = YasamModulleri.AcilKasa()
    private var aiTon = YasamModulleri.AiTonu.SEFKATLI_ZEN

    private lateinit var txtIlacDurum: TextView
    private lateinit var txtFaturaDurum: TextView
    private lateinit var txtFaturaUyari: TextView
    private lateinit var txtSuKafeinDurum: TextView
    private lateinit var txtRozetOzeti: TextView
    private lateinit var txtBiyoRitimDurum: TextView
    private lateinit var txtAmbientDurum: TextView
    private lateinit var txtHarcamaDurum: TextView
    private lateinit var txtAcilKasaDurum: TextView
    private lateinit var txtAiTonDurum: TextView
    private lateinit var txtKlonDurum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yasam_modulleri)

        txtIlacDurum = findViewById(R.id.txtIlacDurum)
        txtFaturaDurum = findViewById(R.id.txtFaturaDurum)
        txtFaturaUyari = findViewById(R.id.txtFaturaUyari)
        txtSuKafeinDurum = findViewById(R.id.txtSuKafeinDurum)
        txtRozetOzeti = findViewById(R.id.txtRozetOzeti)
        txtBiyoRitimDurum = findViewById(R.id.txtBiyoRitimDurum)
        txtAmbientDurum = findViewById(R.id.txtAmbientDurum)
        txtHarcamaDurum = findViewById(R.id.txtHarcamaDurum)
        txtAcilKasaDurum = findViewById(R.id.txtAcilKasaDurum)
        txtAiTonDurum = findViewById(R.id.txtAiTonDurum)
        txtKlonDurum = findViewById(R.id.txtKlonDurum)

        ekraniGuncelle()

        // 100 Öneri Katalog Butonu
        findViewById<MaterialButton>(R.id.btn100OneriKatalog).setOnClickListener {
            Toast.makeText(this, "📚 100 Yeni Yaşam & Manuel Kontrol Önerisi PROJE-DURUM.md ve KOD-ATLASI.md içine eklendi!", Toast.LENGTH_LONG).show()
        }

        // Modül 1: İlaç Toggle
        findViewById<MaterialButton>(R.id.btnIlacAlindiToggle).setOnClickListener {
            ilac = YasamModulleri.IlacHatirlatisi.ilacDurumuDegistir(ilac, !ilac.alindiMi)
            ekraniGuncelle()
            Toast.makeText(this, "💊 İlaç Durumu: ${if (ilac.alindiMi) "Alındı" else "Bekliyor"}", Toast.LENGTH_SHORT).show()
        }

        // Modül 3: Su Ekle
        findViewById<MaterialButton>(R.id.btnSuEkle).setOnClickListener {
            suKafein = YasamModulleri.SuKafeinSayaci.suEkle(suKafein, 250)
            ekraniGuncelle()
        }

        // Modül 3: Kafein Ekle
        findViewById<MaterialButton>(R.id.btnKafeinEkle).setOnClickListener {
            suKafein = YasamModulleri.SuKafeinSayaci.kafeinEkle(suKafein, 80)
            ekraniGuncelle()
            val uyari = YasamModulleri.SuKafeinSayaci.saglikUyarisi(suKafein)
            if (uyari.contains("SINIRI AŞILDI")) {
                Toast.makeText(this, uyari, Toast.LENGTH_LONG).show()
            }
        }

        // Modül 4: Rozet Kilidi Aç
        findViewById<MaterialButton>(R.id.btnRozetKilidiAc).setOnClickListener {
            val ilkKapali = rozetler.firstOrNull { !it.acildiMi }
            if (ilkKapali != null) {
                rozetler = YasamModulleri.RozetKilitMerkezi.rozetAc(rozetler, ilkKapali.id)
                ekraniGuncelle()
                Toast.makeText(this, "🏆 Rozet Açıldı: ${ilkKapali.baslik}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "👑 Tüm Pofi Rozetleri Zaten Açık!", Toast.LENGTH_SHORT).show()
            }
        }

        // Modül 5: REM Döngüsü Değiştir (4 / 5 / 6)
        findViewById<MaterialButton>(R.id.btnDonguDegistir).setOnClickListener {
            val yeniDongu = when (biyoPlan.donguSayisi) {
                5 -> 6
                6 -> 4
                else -> 5
            }
            biyoPlan = biyoPlan.copy(donguSayisi = yeniDongu)
            ekraniGuncelle()
        }

        // Modül 6: Ambient Mikser Değiştir
        findViewById<MaterialButton>(R.id.btnAmbientToggle).setOnClickListener {
            ambientAyar = if (ambientAyar.yagmurSeviye == 50) {
                YasamModulleri.AmbientMikserAyari(ormanSeviye = 70, beyazGurultuSeviye = 30, gamma40HzAcik = false)
            } else {
                YasamModulleri.AmbientMikserAyari(yagmurSeviye = 50, gamma40HzAcik = true)
            }
            ekraniGuncelle()
        }

        // Modül 7: Harcama Ekle
        findViewById<MaterialButton>(R.id.btnHarcamaEkle).setOnClickListener {
            harcamalar.add(YasamModulleri.Harcama("Kahve", 75, "Kafein mola"))
            ekraniGuncelle()
            Toast.makeText(this, "💰 +75 ₺ Kahve Harcaması Eklendi", Toast.LENGTH_SHORT).show()
        }

        // Modül 8: Acil SOS Güncelle
        findViewById<MaterialButton>(R.id.btnAcilKasaGuncelle).setOnClickListener {
            acilKasa = acilKasa.copy(tibbiNot = "Penisilin Alerjisi · A Rh+")
            ekraniGuncelle()
            Toast.makeText(this, "🚨 Acil Durum Kasası Güncellendi", Toast.LENGTH_SHORT).show()
        }

        // Modül 9: AI Tonu Değiştir
        findViewById<MaterialButton>(R.id.btnAiTonDegistir).setOnClickListener {
            val siradaki = when (aiTon) {
                YasamModulleri.AiTonu.SEFKATLI_ZEN -> YasamModulleri.AiTonu.SERT_ASKER
                YasamModulleri.AiTonu.SERT_ASKER -> YasamModulleri.AiTonu.SOKRATIK_FILOZOF
                YasamModulleri.AiTonu.SOKRATIK_FILOZOF -> YasamModulleri.AiTonu.ESPIRILI_POFI
                YasamModulleri.AiTonu.ESPIRILI_POFI -> YasamModulleri.AiTonu.SEFKATLI_ZEN
            }
            aiTon = siradaki
            ekraniGuncelle()
            Toast.makeText(this, "🤖 AI Tonu: ${aiTon.baslik}", Toast.LENGTH_SHORT).show()
        }

        // Modül 10: JSON Dışa Aktar
        findViewById<MaterialButton>(R.id.btnKlonDisaAktar).setOnClickListener {
            val json = YasamModulleri.VeriKlonlayici.klonJsonUret(suKafein, aiTon, ambientAyar)
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("YasamModulleri", json.toString()))
            txtKlonDurum.text = "Son İşlem: JSON Pano Hafızasına Kopyalandı (10 Modül)"
            Toast.makeText(this, "📋 10 Yaşam Modülü JSON Kopyalandı!", Toast.LENGTH_SHORT).show()
        }

        // Modül 10: JSON İçe Aktar
        findViewById<MaterialButton>(R.id.btnKlonIceAktar).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val pano = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            runCatching {
                val obj = JSONObject(pano)
                val cozum = YasamModulleri.VeriKlonlayici.klonJsonCoz(obj)
                suKafein = cozum.first
                aiTon = cozum.second
                ambientAyar = cozum.third
                ekraniGuncelle()
                txtKlonDurum.text = "Son İşlem: JSON Panodan Yüklendi ve Geri Yüklendi"
                Toast.makeText(this, "📥 10 Yaşam Modülü Verisi Geri Yüklendi!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "⚠️ Pano verisi geçerli bir JSON değil!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ekraniGuncelle() {
        txtIlacDurum.text = YasamModulleri.IlacHatirlatisi.dozMetni(ilac)
        txtFaturaDurum.text = YasamModulleri.FaturaTakipci.faturaOzeti(abonelikler)
        txtFaturaUyari.text = YasamModulleri.FaturaTakipci.gecikenUyarisi(abonelikler, 15)
        txtSuKafeinDurum.text = YasamModulleri.SuKafeinSayaci.saglikUyarisi(suKafein)
        txtRozetOzeti.text = YasamModulleri.RozetKilitMerkezi.rozetKilitOzeti(rozetler)

        val uyanma = YasamModulleri.BiyoRitimAyari.idealUyanmaSaati(biyoPlan)
        val dincPuan = YasamModulleri.BiyoRitimAyari.dincUyanmaPuan(biyoPlan.donguSayisi)
        txtBiyoRitimDurum.text = "23:00 Yatış -> İdeal Uyanma Saati: $uyanma (${biyoPlan.donguSayisi} REM Döngüsü, Dinçlik: %$dincPuan)"

        txtAmbientDurum.text = YasamModulleri.AmbientMikser.mikserOzeti(ambientAyar)
        txtHarcamaDurum.text = YasamModulleri.HizliHarcama.harcamaOzeti(harcamalar, 500)
        txtAcilKasaDurum.text = YasamModulleri.AcilDurumKasasi.acilKasaKartMetni(acilKasa)
        txtAiTonDurum.text = "Aktif Ton: ${YasamModulleri.AiKocTonu.tonBasligiGetir(aiTon)} · (${YasamModulleri.AiKocTonu.tonPromptGetir(aiTon)})"
    }
}
