package com.gunlukasistan.app

import org.json.JSONObject
import java.util.Locale

/**
 * v10.53 — Kullanıcı maddesi 1-30 + Bonus 31-32:
 * 32 Maddelik Tasarım ve Yerleşim Özelleştirme Atölyesi (saf, JVM testli).
 *
 * Renk paleti, kart geometrisi, tipografi, ana sayfa/bugün yerleşimi,
 * zamanlayıcı kadranı, üst bar ve alt menü ayarlarını bütüncül bir profil
 * ([AtolyeProfili]) üzerinde yönetir.
 */
object TasarimAtolye {

    // ── ÖZELLEŞTİRME PROFİLİ VERİ MODELİ ──
    data class AtolyeProfili(
        val ad: String = "Varsayılan v2",
        // Grup 1: Renk, Vurgu ve Palet (#1-#5)
        val ozelHexVurgu: String = "#4C7DFF",
        val kartSaydamlikYuzde: Int = 100,
        val konuRenkIndeks: Int = 0,
        val durumDoygunluk: Int = 100,
        val geceSicaklikYuzde: Int = 0,
        // Grup 2: Kart, Köşe ve Kenarlık (#6-#10)
        val koseYaricapiIndeks: Int = 2, // 0: 0dp, 1: 12dp, 2: 16dp, 3: 24dp
        val kenarlikDp: Int = 1,
        val yukseltiDp: Int = 0,
        val sagElMi: Boolean = true,
        val ilerlemeBicimi: IlerlemeBicimi = IlerlemeBicimi.YATAY_CUBUK,
        // Grup 3: Tipografi ve Metin (#11-#15)
        val baslikFontIndeks: Int = 0,
        val govdeFontIndeks: Int = 0,
        val baslikKalinlik: Int = 700,
        val maxLines: Int = 3,
        val harfAraligiEm: Float = 0.0f,
        val rozetKonumu: RozetKonum = RozetKonum.SAG_UST,
        // Grup 4: Ana Sayfa, Bugün ve Liste (#16-#20)
        val statModu: StatModu = StatModu.YATAY_SERIT,
        val akordiyonDurum: AkordiyonDurum = AkordiyonDurum.SADECE_DOLUYKEN_ACIK,
        val planStili: PlanStili = PlanStili.HERO_VE_COMPACT,
        val kaydirmaAksiyonu: KaydirmaAksiyonu = KaydirmaAksiyonu.TAMAMLA,
        // Grup 5: Zamanlayıcı ve Kadran (#21-#25)
        val kadranCarpani: Float = 1.00f,
        val saniyeEfekti: SaniyeEfekti = SaniyeEfekti.RULO_FLIP,
        val bitisEfekti: BitisEfekti = BitisEfekti.KONFETI,
        val sesGorunumu: SesGorunumu = SesGorunumu.KOMPAKT_SERIT,
        val rippleSiddeti: Int = 100,
        // Grup 6: Üst Bar, Alt Menü ve FAB (#26-#30)
        val altNavIkonDp: Int = 22,
        val yuzenKonum: YuzenKonum = YuzenKonum.ALT_NAV_USTUNDE,
        val fabKonumu: FabKonum = FabKonum.SAG_ALT
    )

    enum class IlerlemeBicimi { YATAY_CUBUK, KALIN_CUBUK, MINI_HALKA, SADECE_YUZDE }
    enum class RozetKonum { BASLIK_ALTI, SAG_UST }
    enum class StatModu { YATAY_SERIT, IZGARA_2X2, TEK_MANSET }
    enum class AkordiyonDurum { HER_ZAMAN_ACIK, HER_ZAMAN_KAPALI, SADECE_DOLUYKEN_ACIK }
    enum class PlanStili { HERO_VE_COMPACT, ESIT_KARTLAR, ZAMAN_CIZELGESI }
    enum class KaydirmaAksiyonu { SIL, TAMAMLA, ERTELE, ARSIVE_TASI }
    enum class SaniyeEfekti { RULO_FLIP, DUZ_METIN, GIZLI_ZEN }
    enum class BitisEfekti { KONFETI, PARLAMA_HARE, SADECE_TITRESIM }
    enum class SesGorunumu { KOMPAKT_SERIT, BUYUK_IZGARA, ALT_MENU }
    enum class YuzenKonum { ALT_NAV_USTUNDE, TEPE_SABIT, YUZEN_BUBBLE }
    enum class FabKonum { SAG_ALT, ALT_NAV_ORTA, GIZLI_UST_BAR }

    // 1. #1 Hex Renk Doğrulama
    fun hexRenkDogrula(hex: String): Boolean {
        val h = hex.trim()
        val regex = Regex("""^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$""")
        return regex.matches(h)
    }

    fun parseHexVeyaVarsayilan(hex: String, varsayilanHex: String = "#4C7DFF"): String {
        return if (hexRenkDogrula(hex)) hex.trim().uppercase(Locale.US) else varsayilanHex
    }

    // 2. #2 Kart İç Zemin Saydamlık
    fun kartSaydamlikAlfa(yuzde: Int): Int {
        val clamp = yuzde.coerceIn(0, 100)
        return ((clamp / 100f) * 255).toInt()
    }

    // 3. #3 Konu Özel Renk Hex
    fun konuOzelRenkHex(indeks: Int): String {
        val palet = arrayOf("#4C7DFF", "#22C55E", "#F59E0B", "#EF4444", "#C97C5D", "#7A8FA6", "#8E5BA6", "#2FA8A0")
        return palet[indeks.coerceIn(0, palet.lastIndex)]
    }

    // 4. #6 Köşe Yarıçapı Dp
    fun koseYaricapiDp(secim: Int): Int {
        return when (secim.coerceIn(0, 3)) {
            0 -> 0
            1 -> 12
            2 -> 16
            else -> 24
        }
    }

    // 5. #13 Max Lines Sınırla
    fun maxLinesSinirla(satir: Int): Int = satir.coerceIn(1, 10)

    // 6. #18 Akordiyon Durum Kararı
    fun akordiyonDurumKarari(durum: AkordiyonDurum, elemanSayisi: Int): Boolean {
        return when (durum) {
            AkordiyonDurum.HER_ZAMAN_ACIK -> true
            AkordiyonDurum.HER_ZAMAN_KAPALI -> false
            AkordiyonDurum.SADECE_DOLUYKEN_ACIK -> elemanSayisi > 0
        }
    }

    // 7. #30 JSON Serileştirme & Ayrıştırma
    fun profilJsonUret(profil: AtolyeProfili): JSONObject {
        return JSONObject().apply {
            put("ad", profil.ad)
            put("hex", profil.ozelHexVurgu)
            put("alfa", profil.kartSaydamlikYuzde)
            put("kose", profil.koseYaricapiIndeks)
            put("lines", profil.maxLines)
            put("stat", profil.statModu.name)
            put("plan", profil.planStili.name)
            put("efekt", profil.saniyeEfekti.name)
        }
    }

    fun profilJsonCoz(json: JSONObject?): AtolyeProfili {
        if (json == null) return fabrikaVarsayilanProfili()
        return fabrikaVarsayilanProfili().copy(
            ad = json.optString("ad", "Varsayılan v2"),
            ozelHexVurgu = parseHexVeyaVarsayilan(json.optString("hex", "#4C7DFF")),
            kartSaydamlikYuzde = json.optInt("alfa", 100).coerceIn(0, 100),
            koseYaricapiIndeks = json.optInt("kose", 2).coerceIn(0, 3),
            maxLines = json.optInt("lines", 3).coerceIn(1, 10)
        )
    }

    // 8. #32 Fabrika Ayarlarına Dönüş
    fun fabrikaVarsayilanProfili(): AtolyeProfili = AtolyeProfili()

    // 9. #31 Canlı Arayüz Önizleme Aynası Metni
    fun canliOnizlemeKartMetni(profil: AtolyeProfili): String {
        val koseDp = koseYaricapiDp(profil.koseYaricapiIndeks)
        return "🎛️ ÖNİZLEME: '${profil.ad}' · Vurgu: ${profil.ozelHexVurgu} · Köşe: ${koseDp}dp · Saydamlık: %${profil.kartSaydamlikYuzde}"
    }
}
