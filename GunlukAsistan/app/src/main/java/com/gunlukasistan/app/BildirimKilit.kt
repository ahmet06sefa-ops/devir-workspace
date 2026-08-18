package com.gunlukasistan.app

import android.content.Context

/**
 * v7.56 — Yönetici bildirim kilidi.
 *
 * ── Kullanıcının isteği ──
 * "Yönetici hariç diğer kullanıcıların bildirim sesi gelmesini kapatmasını
 *  yönetici izin vermeli."
 *
 * ── Nasıl çalışıyor ──
 * Online odaya bağlı ve **yönetici değilsen**, yöneticinin kapattığı
 * bildirim ayarlarını değiştiremezsin. Ayar ekranında ilgili anahtarın
 * yanında 🔒 görünür, dokununca "yetkin yok" açıklaması çıkar.
 *
 * Kontrol edilen dört ayar:
 *   · Sesli uyarı        → [OnlineStore.Islem.SES_KAPAT]
 *   · Titreşim           → [OnlineStore.Islem.TITRESIM_KAPAT]
 *   · Ana bildirim anahtarı → [OnlineStore.Islem.BILDIRIM_TUM_KAPAT]
 *   · Israrlı uyarı      → [OnlineStore.Islem.ZORUNLU_KAPAT]
 *
 * ── Önemli tasarım kararı ──
 * Kilit yalnızca **kapatmayı** engeller, açmayı değil. Üye bildirimi
 * her zaman açabilir; sadece kapatamaz. Amaç "haberdar kalmasını
 * sağlamak", ayarlarını tümden ele geçirmek değil.
 *
 * ── Dürüst sınır ──
 * Bu uygulama içi bir kilittir. Üye Android'in sistem ayarlarından
 * uygulamanın bildirimlerini yine de kapatabilir — hiçbir uygulama
 * bunu engelleyemez. Kullanıcıya v7.52'de açıkça söylendi.
 */
object BildirimKilit {

    private const val TAG = "BildirimKilit"

    /**
     * Bu ayar kilitli mi (üye değiştiremez mi)?
     *
     * Odaya bağlı değilsek veya yöneticiysek hiçbir şey kilitli değildir.
     */
    fun kilitli(context: Context, islem: OnlineStore.Islem): Boolean {
        return try {
            if (!OnlineStore.bagliMi(context)) return false
            val oda = OnlineStore.onbellektenOku(context)
            if (OnlineStore.yoneticiMiyim(context, oda)) return false
            !OnlineStore.izinVar(context, oda, islem)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Kilit durumu okunamadı", e)
            false   // hata olursa engelleme — kullanıcıyı kilitli bırakma
        }
    }

    /**
     * Ayarı kapatmaya çalışırken izin kontrolü.
     *
     * @param acmaIstegi kullanıcı ayarı AÇIYOR mu? Açmak her zaman serbest.
     * @return true ise işlem yapılabilir
     */
    fun izinVar(context: Context, islem: OnlineStore.Islem, acmaIstegi: Boolean): Boolean {
        if (acmaIstegi) return true          // açmak serbest
        return !kilitli(context, islem)
    }

    /** Kilitliyse gösterilecek açıklama. */
    fun mesaj(context: Context, islem: OnlineStore.Islem): String = try {
        OnlineStore.izinMesaji(context, islem)
    } catch (e: Exception) {
        android.util.Log.w(TAG, "Mesaj alınamadı", e)
        context.getString(R.string.on_yetki_yok)
    }

    /** Ayar adının yanına eklenecek kilit işareti. */
    fun etiket(context: Context, ad: String, islem: OnlineStore.Islem): String =
        if (kilitli(context, islem)) ad + "  🔒" else ad

    /**
     * Kilitli bir ayarı değiştirmeye çalışan üyeye açıklama gösterir.
     * @return true ise engellendi (çağıran işlemi iptal etmeli)
     */
    fun engellendiMi(
        context: Context,
        islem: OnlineStore.Islem,
        acmaIstegi: Boolean,
        uyar: (String) -> Unit
    ): Boolean {
        if (izinVar(context, islem, acmaIstegi)) return false
        uyar(mesaj(context, islem))
        return true
    }

    /** Odada herhangi bir bildirim kilidi var mı? Ayar ekranında bilgi şeridi için. */
    fun herhangiKilitVar(context: Context): Boolean =
        kilitli(context, OnlineStore.Islem.SES_KAPAT) ||
            kilitli(context, OnlineStore.Islem.TITRESIM_KAPAT) ||
            kilitli(context, OnlineStore.Islem.BILDIRIM_TUM_KAPAT) ||
            kilitli(context, OnlineStore.Islem.ZORUNLU_KAPAT)
}
