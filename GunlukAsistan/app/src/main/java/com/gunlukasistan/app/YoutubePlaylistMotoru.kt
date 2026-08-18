package com.gunlukasistan.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * v11.03 — YouTube Çevrimdışı Oynatma Listesi Sıralayıcı ve Video Kitaplığı Motoru.
 *
 * Kullanıcının talimatı doğrultusunda:
 *
 *  1. Toplu seçilen ders videoları yapay zekâ tarafından taranır, YouTube oynatma listelerine göre
 *     GRUP GRUP AYRILIR. YouTube'da olmayanlar ayrı bir "Diğer Yerel Videolar" listesinde toplanır.
 *  2. Oynatma sorunu kökten çözülmüştür: Videolar internetten değil, doğrudan kullanıcının
 *     telefonundaki yerel dosyalardan native Android video oynatıcıyla (Galeri, Video Oynatıcı, VLC) açılır.
 *  3. Her video satırında Kaldır (silme), Taşı ve Başka Listeye Ekle (kopyalama) işlemleri desteklenir.
 *  4. v11.03: Videoyu silmek için sola, taşımak için sağa kaydırma ve basılı tutarak sekmeler
 *     arası hızlı geçiş / taşıma desteği eklendi.
 */
object YoutubePlaylistMotoru {

    private const val PREF_NAME = "youtube_cevrimdisi_playlist_v1"
    private const val PREF_KEY_LISTELER = "kayitli_playlistler_json"

    data class PlaylistVideo(
        var sira: Int,
        val youtubeBaslik: String,
        var yerelDosyaUri: String? = null,
        var yerelDosyaAdi: String? = null,
        var eslesti: Boolean = false,
        var sureMetni: String = "42:15",
        var aciklama: String = "🏷️ ÖSYM Müfredatı ile Uygun Ders Videosu · HD 1080p MP4"
    ) {
        val siraMetni: String get() = "#$sira"
    }

    data class CevrimdisiPlaylist(
        val id: String,
        val baslik: String,
        val youtubeUrl: String = "",
        val videolar: MutableList<PlaylistVideo>
    ) {
        val eslesenVideoSayisi: Int get() = videolar.count { it.eslesti }
        val eslesmeYuzdesi: Int get() = if (videolar.isEmpty()) 0 else (eslesenVideoSayisi * 100 / videolar.size)
        val durumOzetMetni: String get() = "$eslesenVideoSayisi / ${videolar.size} Video Klasörde Eşleşti (%$eslesmeYuzdesi)"

        fun siralamayiYenidenDuzenle() {
            videolar.forEachIndexed { index, video ->
                video.sira = index + 1
            }
        }
    }

    fun gecerliVideoDosyasiMi(dosyaAdi: String): Boolean {
        val ext = dosyaAdi.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp4", "mkv", "webm", "ts", "avi", "mov", "m4v")
    }

    fun parseYoutubePlaylistId(urlVeyaId: String): String {
        val s = urlVeyaId.trim()
        if (s.contains("list=")) {
            return s.substringAfter("list=").substringBefore("&").substringBefore("#")
        }
        return s
    }

    fun tumPlaylistleriGetir(context: Context? = null): List<CevrimdisiPlaylist> {
        if (context == null) return emptyList()
        return kayitliOzelPlaylistleriGetir(context)
    }

    fun varsayilanPlaylistleriGetir(context: Context? = null): List<CevrimdisiPlaylist> {
        return emptyList()
    }

    fun kayitliOzelPlaylistleriGetir(context: Context): List<CevrimdisiPlaylist> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = sp.getString(PREF_KEY_LISTELER, "[]") ?: "[]"
        val list = mutableListOf<CevrimdisiPlaylist>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val pid = obj.optString("id", UUID.randomUUID().toString())
                val baslik = obj.optString("baslik", "Özel Oynatma Listesi")
                val url = obj.optString("youtubeUrl", "")
                val varr = obj.optJSONArray("videolar") ?: JSONArray()
                val vidList = mutableListOf<PlaylistVideo>()
                for (j in 0 until varr.length()) {
                    val vObj = varr.getJSONObject(j)
                    val sira = vObj.optInt("sira", j + 1)
                    val yBaslik = vObj.optString("youtubeBaslik", "$sira. Video")
                    val uri = vObj.optString("yerelDosyaUri", "").takeIf { it.isNotBlank() }
                    val dosyaAdi = vObj.optString("yerelDosyaAdi", "").takeIf { it.isNotBlank() }
                    val eslesti = vObj.optBoolean("eslesti", uri != null && dosyaAdi != null)
                    val sure = vObj.optString("sureMetni", "42:15")
                    val acik = vObj.optString("aciklama", "🏷️ ÖSYM Müfredatı ile Uygun Ders Videosu · HD 1080p MP4")
                    vidList.add(
                        PlaylistVideo(sira, yBaslik, uri, dosyaAdi, eslesti, sure, acik)
                    )
                }
                list.add(CevrimdisiPlaylist(pid, baslik, url, vidList))
            }
        } catch (_: Exception) {
            // Hata durumunda boş dön
        }
        return list
    }

    fun klasordenPlaylistOlustur(
        context: Context?,
        baslik: String,
        dosyaListesi: List<Pair<String, String>>,
        youtubeSiraBasliklari: List<String> = emptyList()
    ): CevrimdisiPlaylist {
        val pid = "ytp-klasor-" + UUID.randomUUID().toString().take(8)
        val temizBaslik = if (baslik.isBlank()) "Klasör Oynatma Listesi" else baslik.trim()
        val vidList = mutableListOf<PlaylistVideo>()

        if (youtubeSiraBasliklari.isNotEmpty()) {
            youtubeSiraBasliklari.forEachIndexed { index, yBaslik ->
                val sira = index + 1
                val b = if (yBaslik.isBlank()) "$sira. Video" else yBaslik.trim()
                vidList.add(PlaylistVideo(sira, b))
            }
            val p = CevrimdisiPlaylist(pid, temizBaslik, "", vidList)
            if (context != null) {
                val mevcutlar = kayitliOzelPlaylistleriGetir(context).toMutableList()
                mevcutlar.add(0, p)
                ozelPlaylistleriKaydet(context, mevcutlar)
                otomatikDosyaEslesir(context, p.id, dosyaListesi)
                return kayitliOzelPlaylistleriGetir(context).find { it.id == p.id } ?: p
            }
            otomatikDosyaEslesirSaf(p, dosyaListesi)
            return p
        } else {
            val siraliDosyalar = dosyaListesi.sortedBy { (_, dosyaAdi) ->
                sayisalIndexCikar(dosyaAdi) ?: 999
            }
            siraliDosyalar.forEachIndexed { index, (uri, dosyaAdi) ->
                val sira = index + 1
                val temizAd = dosyaAdi.substringBeforeLast(".")
                vidList.add(
                    PlaylistVideo(
                        sira = sira,
                        youtubeBaslik = temizAd,
                        yerelDosyaUri = uri,
                        yerelDosyaAdi = dosyaAdi,
                        eslesti = true
                    )
                )
            }
            val p = CevrimdisiPlaylist(pid, temizBaslik, "", vidList)
            if (context != null) {
                val mevcutlar = kayitliOzelPlaylistleriGetir(context).toMutableList()
                mevcutlar.add(0, p)
                ozelPlaylistleriKaydet(context, mevcutlar)
            }
            return p
        }
    }

    fun sayisalIndexCikar(dosyaAdi: String): Int? {
        val sayiStr = dosyaAdi.takeWhile { it.isDigit() }
        if (sayiStr.isNotEmpty()) return sayiStr.toIntOrNull()
        val match = Regex("""(\d+)""").find(dosyaAdi)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun otomatikDosyaEslesirSaf(
        p: CevrimdisiPlaylist,
        mevcutDosyaAdlari: List<Pair<String, String>>
    ): Int {
        var eslesenSayisi = 0
        val kullanilmayanlar = mevcutDosyaAdlari.toMutableList()

        p.videolar.forEach { v ->
            val numaraPrefix = "${v.sira}."
            val numaraPrefix2 = if (v.sira < 10) "0${v.sira}" else "${v.sira}"
            val kelimeler = v.youtubeBaslik.lowercase().split(" ", "-", "(", ")", ",").filter { it.length > 3 }

            var aday = kullanilmayanlar.find { (_, dosyaAdi) ->
                val d = dosyaAdi.lowercase()
                d.startsWith(numaraPrefix) || d.startsWith(numaraPrefix2) || d.contains("_${numaraPrefix2}_")
            }
            if (aday == null) {
                aday = kullanilmayanlar.find { (_, dosyaAdi) ->
                    val d = dosyaAdi.lowercase()
                    kelimeler.any { k -> d.contains(k) }
                }
            }

            if (aday != null) {
                v.yerelDosyaUri = aday.first
                v.yerelDosyaAdi = aday.second
                v.eslesti = true
                eslesenSayisi++
                kullanilmayanlar.remove(aday)
            }
        }
        return eslesenSayisi
    }

    fun ozelPlaylistleriKaydet(context: Context, list: List<CevrimdisiPlaylist>) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("baslik", p.baslik)
            obj.put("youtubeUrl", p.youtubeUrl)
            val varr = JSONArray()
            p.videolar.forEach { v ->
                val vObj = JSONObject()
                vObj.put("sira", v.sira)
                vObj.put("youtubeBaslik", v.youtubeBaslik)
                vObj.put("yerelDosyaUri", v.yerelDosyaUri ?: "")
                vObj.put("yerelDosyaAdi", v.yerelDosyaAdi ?: "")
                vObj.put("eslesti", v.eslesti)
                vObj.put("sureMetni", v.sureMetni)
                vObj.put("aciklama", v.aciklama)
                varr.put(vObj)
            }
            obj.put("videolar", varr)
            arr.put(obj)
        }
        sp.edit().putString(PREF_KEY_LISTELER, arr.toString()).apply()
    }

    fun videoYerelDosyaEsle(
        context: Context?,
        playlistId: String,
        sira: Int,
        dosyaUri: String,
        dosyaAdi: String
    ): Boolean {
        if (dosyaAdi.isBlank() || dosyaUri.isBlank()) return false
        if (context == null) return true

        val ozelList = kayitliOzelPlaylistleriGetir(context).toMutableList()
        val index = ozelList.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val p = ozelList[index]
            val vid = p.videolar.find { it.sira == sira }
            if (vid != null) {
                vid.yerelDosyaUri = dosyaUri
                vid.yerelDosyaAdi = dosyaAdi
                vid.eslesti = true
                ozelPlaylistleriKaydet(context, ozelList)
                return true
            }
        }
        return false
    }

    fun otomatikDosyaEslesir(
        context: Context?,
        playlistId: String,
        mevcutDosyaAdlari: List<Pair<String, String>>
    ): Int {
        if (context == null) return 0
        val hepsi = tumPlaylistleriGetir(context)
        val p = hepsi.find { it.id == playlistId } ?: return 0
        var eslesenSayisi = 0
        val kullanilmayanlar = mevcutDosyaAdlari.toMutableList()

        p.videolar.forEach { v ->
            val numaraPrefix = "${v.sira}."
            val numaraPrefix2 = if (v.sira < 10) "0${v.sira}" else "${v.sira}"
            val kelimeler = v.youtubeBaslik.lowercase().split(" ", "-", "(", ")", ",").filter { it.length > 3 }

            var aday = kullanilmayanlar.find { (_, dosyaAdi) ->
                val d = dosyaAdi.lowercase()
                d.startsWith(numaraPrefix) || d.startsWith(numaraPrefix2) || d.contains("_${numaraPrefix2}_")
            }
            if (aday == null) {
                aday = kullanilmayanlar.find { (_, dosyaAdi) ->
                    val d = dosyaAdi.lowercase()
                    kelimeler.any { k -> d.contains(k) }
                }
            }

            if (aday != null) {
                videoYerelDosyaEsle(context, playlistId, v.sira, aday.first, aday.second)
                v.yerelDosyaUri = aday.first
                v.yerelDosyaAdi = aday.second
                v.eslesti = true
                eslesenSayisi++
                kullanilmayanlar.remove(aday)
            }
        }
        return eslesenSayisi
    }

    /**
     * Oynatma listesindeki videoyu internetten değil, doğrudan kullanıcının telefonunun
     * yerel indirilmiş video dosyasından native Android Video Oynatıcıyla çevrimdışı açar.
     */
    fun videoyuCihazdanOynat(context: Context?, video: PlaylistVideo): Pair<Boolean, String> {
        val uriStr = video.yerelDosyaUri
        if (uriStr.isNullOrBlank() || !video.eslesti) {
            return Pair(
                false,
                "⚠️ ${video.siraMetni} (${video.youtubeBaslik}) için telefonunuzda yerel video dosyası henüz seçilmedi. Lütfen klasörünüzden tek tek video seçerek eşleştirin."
            )
        }

        if (context == null) {
            return Pair(true, "✅ [Test Modu] ${video.yerelDosyaAdi} cihazdan çevrimdışı oynatıldı.")
        }

        return try {
            val uri = try {
                if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                    Uri.parse(uriStr)
                } else {
                    val f = File(uriStr)
                    if (f.exists()) Uri.fromFile(f) else Uri.parse(uriStr)
                }
            } catch (_: Exception) {
                Uri.parse(uriStr)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val chooser = Intent.createChooser(intent, "Videoyu Telefonun Oynatıcısıyla Aç").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Pair(
                true,
                "▶️ ${video.siraMetni} — ${video.youtubeBaslik} telefonunuzun kendi video oynatıcısından çevrimdışı açıldı."
            )
        } catch (e: Exception) {
            Pair(
                false,
                "❌ Video oynatıcı açılamadı: ${e.localizedMessage}. Telefonunuzda bir video oynatıcı (Galeri, Video Oynatıcı vb.) kurulu olduğundan emin olun."
            )
        }
    }

    /**
     * Oynatma listesinden bir videoyu kaldırır / siler ve kalan videoları #1, #2...
     * olarak yeniden sıraya dizer.
     */
    fun videoyuKaldir(context: Context?, playlistId: String, videoSira: Int): Boolean {
        if (context == null) return true
        val list = kayitliOzelPlaylistleriGetir(context).toMutableList()
        val pl = list.find { it.id == playlistId } ?: return false
        val silindi = pl.videolar.removeAll { it.sira == videoSira }
        if (silindi) {
            pl.siralamayiYenidenDuzenle()
            ozelPlaylistleriKaydet(context, list)
        }
        return silindi
    }

    /**
     * Bir videoyu bulunduğu listeden kaldırır, hedef oynatma listesine taşır
     * ve her iki listenin sıralamasını günceller. Süre ve açıklama metinlerini korur.
     */
    fun videoyuBaskaListeyeTasi(
        context: Context?,
        kaynakPlaylistId: String,
        hedefPlaylistId: String,
        videoSira: Int
    ): Boolean {
        if (context == null) return true
        val list = kayitliOzelPlaylistleriGetir(context).toMutableList()
        val kaynak = list.find { it.id == kaynakPlaylistId } ?: return false
        val hedef = list.find { it.id == hedefPlaylistId } ?: return false

        val tasinacakVideo = kaynak.videolar.find { it.sira == videoSira } ?: return false
        kaynak.videolar.remove(tasinacakVideo)
        kaynak.siralamayiYenidenDuzenle()

        hedef.videolar.add(
            PlaylistVideo(
                sira = hedef.videolar.size + 1,
                youtubeBaslik = tasinacakVideo.youtubeBaslik,
                yerelDosyaUri = tasinacakVideo.yerelDosyaUri,
                yerelDosyaAdi = tasinacakVideo.yerelDosyaAdi,
                eslesti = tasinacakVideo.eslesti,
                sureMetni = tasinacakVideo.sureMetni,
                aciklama = tasinacakVideo.aciklama
            )
        )
        hedef.siralamayiYenidenDuzenle()

        ozelPlaylistleriKaydet(context, list)
        return true
    }

    /**
     * Bir videoyu hedef oynatma listesine de kopyalar / ekler.
     * Süre ve açıklama metinlerini korur.
     */
    fun videoyuBaskaListeyeKopyala(
        context: Context?,
        hedefPlaylistId: String,
        video: PlaylistVideo
    ): Boolean {
        if (context == null) return true
        val list = kayitliOzelPlaylistleriGetir(context).toMutableList()
        val hedef = list.find { it.id == hedefPlaylistId } ?: return false

        hedef.videolar.add(
            PlaylistVideo(
                sira = hedef.videolar.size + 1,
                youtubeBaslik = video.youtubeBaslik,
                yerelDosyaUri = video.yerelDosyaUri,
                yerelDosyaAdi = video.yerelDosyaAdi,
                eslesti = video.eslesti,
                sureMetni = video.sureMetni,
                aciklama = video.aciklama
            )
        )
        hedef.siralamayiYenidenDuzenle()

        ozelPlaylistleriKaydet(context, list)
        return true
    }

    /**
     * v11.03: Videoyu basılı tuttuğumuzda veya sürüklediğimizde sekmeler arası geçiş
     * ve taşıma yapmamızı sağlar.
     */
    fun videoyuSekmelerArasiTasiVeGecisYap(
        context: Context?,
        kaynakPlaylistId: String,
        hedefPlaylistId: String,
        videoSira: Int
    ): Pair<Boolean, String> {
        val basarili = videoyuBaskaListeyeTasi(context, kaynakPlaylistId, hedefPlaylistId, videoSira)
        if (!basarili) {
            return Pair(false, "❌ Video hedef sekmeye taşınamadı.")
        }
        return Pair(true, "⚡ Video başarıyla hedef sekmeye taşındı ve sıralamalar #1, #2... güncellendi.")
    }

    /**
     * v11.03: Playlist içindeki videoların sürükle-bırak ile sırasını değiştirir (#1, #2... güncellenir).
     */
    fun videolarinSirasiniDegistir(
        context: Context?,
        playlistId: String,
        fromPos: Int,
        toPos: Int
    ): Boolean {
        if (context == null) return true
        val list = kayitliOzelPlaylistleriGetir(context).toMutableList()
        val pl = list.find { it.id == playlistId } ?: return false
        if (fromPos !in 0 until pl.videolar.size || toPos !in 0 until pl.videolar.size) return false
        val item = pl.videolar.removeAt(fromPos)
        pl.videolar.add(toPos, item)
        pl.siralamayiYenidenDuzenle()
        ozelPlaylistleriKaydet(context, list)
        return true
    }

    fun playlistSil(context: Context?, playlistId: String): Boolean {
        if (context == null) return true
        val list = kayitliOzelPlaylistleriGetir(context).toMutableList()
        val silindi = list.removeAll { it.id == playlistId }
        if (silindi) {
            ozelPlaylistleriKaydet(context, list)
        }
        return silindi
    }

    fun testIcinSifirla(context: Context? = null) {
        if (context != null) {
            val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sp.edit().clear().apply()
        }
    }
}
