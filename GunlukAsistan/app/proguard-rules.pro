# ══════════════════════════════════════════════════════════════════
# v8.8 · Öneri 14 — R8 kuralları
# ══════════════════════════════════════════════════════════════════
#
# R8 kullanılmayan kodu atar ve isimleri kısaltır. Sorun şu: YANSIMA
# (reflection) ile erişilen sınıfları göremez. Bir sınıf yalnızca
# Manifest'ten, XML'den veya JSON alan adından çağrılıyorsa R8 onu
# "kullanılmıyor" sanıp siler — uygulama çalışma anında çöker.
#
# Bu dosya tam olarak o sınıfları koruyor. Her kural için NEDEN
# gerektiği yazılı; ileride biri "bu satır ne işe yarıyor" diye
# sormasın.


# ══════════════════════════════════════════════════════════
# 1. Android bileşenleri — Manifest'ten yansımayla oluşturulur
# ══════════════════════════════════════════════════════════
# Activity/Service/Receiver/Provider adları Manifest'te METİN olarak
# yazılı. R8 bu bağı göremez.

-keep class com.gunlukasistan.app.MainActivity { *; }
-keep class com.gunlukasistan.app.App { *; }

# Tüm Activity, Service, Receiver ve widget sağlayıcıları
-keep class com.gunlukasistan.app.**Activity { *; }
-keep class com.gunlukasistan.app.**Servisi { *; }
-keep class com.gunlukasistan.app.**Service { *; }
-keep class com.gunlukasistan.app.**Receiver { *; }
-keep class com.gunlukasistan.app.**Alici { *; }
-keep class com.gunlukasistan.app.**Widget { *; }

# v9.3: MainActivity mutlaka korunmalı — 22 yerden Intent ile
# açılıyor ve launcher girişi artık .SimgeVarsayilan alias'ında.
-keep class com.gunlukasistan.app.MainActivity { *; }

# activity-alias hedefleri (v8.3 · öneri 12: alternatif simgeler).
# Manifest'te `.SimgeKaramel` gibi adlar var ama Kotlin sınıfı yok;
# targetActivity MainActivity'yi gösteriyor. MainActivity zaten
# yukarıda korunuyor.


# ══════════════════════════════════════════════════════════
# 2. Özel View'lar — XML layout'tan yansımayla şişirilir
# ══════════════════════════════════════════════════════════
# `<com.gunlukasistan.app.SayacKadraniView .../>` satırını R8 okumaz.
# Sınıf silinirse layout şişirme `ClassNotFoundException` atar.
# İki parametreli yapıcı (Context, AttributeSet) ŞART.

-keep class com.gunlukasistan.app.**View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    *;
}

# İsmi "View" ile bitmeyen özel görünümler (v8.6'da eklendi)
-keep class com.gunlukasistan.app.Kutlama {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    *;
}
-keep class com.gunlukasistan.app.Iskelet {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    *;
}

# Genel güvenlik ağı: View'dan türeyen her şeyin XML yapıcısı
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}


# ══════════════════════════════════════════════════════════
# 3. Fragment'lar — isimle oluşturuluyor
# ══════════════════════════════════════════════════════════
# `MainActivity.createFragment()` doğrudan sınıf çağırıyor ama
# FragmentManager durum geri yüklemede İSİMLE yeniden oluşturuyor.
# (Ekran döndürme, süreç öldürülüp geri gelme)

-keep class com.gunlukasistan.app.**Fragment { *; }
-keep class * extends androidx.fragment.app.Fragment {
    public <init>();
}


# ══════════════════════════════════════════════════════════
# 4. Room — varlıklar ve DAO'lar
# ══════════════════════════════════════════════════════════
# Room, sütun adlarını alan adlarından türetiyor. İsimler
# kısaltılırsa veritabanı şeması bozulur ve MEVCUT VERİ OKUNAMAZ.
# Bu, veri kaybı riski taşıyan tek kural — en kritik olanı.

-keep class com.gunlukasistan.app.veri.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**


# ══════════════════════════════════════════════════════════
# 5. JSON serileştirme — alan adları veri biçiminin parçası
# ══════════════════════════════════════════════════════════
# `Store.exportJson` alan adlarını anahtar olarak yazıyor
# (biçim sürümü 18). İsimler kısaltılırsa ESKİ YEDEKLER OKUNAMAZ.
#
# Store elle JSON kuruyor (put("baslik", ...)) yani alan adları
# zaten sabit metin — ama veri sınıflarını yine de koruyoruz,
# ileride otomatik serileştirmeye geçilirse diye.

-keep class com.gunlukasistan.app.Store$* { *; }
-keepclassmembers class com.gunlukasistan.app.Store { *; }

# org.json — Android'in kendi kütüphanesi
-dontwarn org.json.**


# ══════════════════════════════════════════════════════════
# 6. PDFBox — yansımalı font/kaynak yükleme
# ══════════════════════════════════════════════════════════
# PDFBox kaynakları classpath'ten isimle okuyor.

-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.tom_roush.fontbox.**

# BouncyCastle: pqc veri dosyalarını packaging ile ATTIK (öneri 13)
# ama sınıfları R8'in atmasına da izin veriyoruz — kullanılmıyorlar.
# Uyarıları sustur ki derleme kırılmasın.
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**


# ══════════════════════════════════════════════════════════
# 7. Kotlin ve coroutine altyapısı
# ══════════════════════════════════════════════════════════
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlin.**
-dontwarn kotlinx.**


# ══════════════════════════════════════════════════════════
# 8. Enum'lar — valueOf/values yansımayla çağrılıyor
# ══════════════════════════════════════════════════════════
# `NamazVakti.Vakit.entries`, `ZamanCizelgesiView.Tur` vb.

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}


# ══════════════════════════════════════════════════════════
# 9. Parcelable / Serializable
# ══════════════════════════════════════════════════════════
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# ══════════════════════════════════════════════════════════
# 10. RemoteViews / widget servisleri
# ══════════════════════════════════════════════════════════
# `RemoteViewsService.RemoteViewsFactory` Manifest'ten çağrılıyor.

-keep class * extends android.widget.RemoteViewsService { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }


# ══════════════════════════════════════════════════════════
# 11. Hata ayıklama kolaylığı
# ══════════════════════════════════════════════════════════
# Satır numaraları korunmazsa çökme kayıtları okunamaz hale gelir.
# Kullanıcı hata bildirdiğinde tek elimizdeki bu.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Açıklama (annotation) bilgileri — Room ve AndroidX bunlara bakıyor
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod


# ══════════════════════════════════════════════════════════
# 12. Uyarı bastırma — eksik isteğe bağlı bağımlılıklar
# ══════════════════════════════════════════════════════════
-dontwarn org.slf4j.**
-dontwarn org.apache.**
-dontwarn com.google.errorprone.**
-dontwarn javax.annotation.**


# ══════════════════════════════════════════════════════════
# 13. WorkManager (v9.8 · Öneri 47)
# ══════════════════════════════════════════════════════════
#
# ⚠️ BU KURALLAR OLMAZSA YEDEKLEME SESSİZCE ÖLÜR.
#
# WorkManager, Worker sınıflarını YANSIMA (reflection) ile
# oluşturuyor: iş kuyruğa alınırken sınıf adı METİN olarak
# veritabanına yazılıyor, çalıştırma anında o metinden sınıf
# bulunuyor.
#
# R8 bu bağlantıyı göremiyor. Sınıfı "kullanılmıyor" sanıp
# silebilir veya adını değiştirebilir. Sonuç:
#   ClassNotFoundException → iş çalışmaz → yedek alınmaz
#
# Ve bu hata SESSİZ: kullanıcı bir şey fark etmez, yalnızca
# telefonunu kaybedince yedeğin olmadığını görür.
#
# Aynı tuzağa v9.7'de Activity/Receiver için düşmemiştik çünkü
# 25-30. satırlardaki `**Activity` / `**Receiver` kalıpları
# onları kapsıyordu. `GenelIsci` hiçbir kalıba uymuyor.

-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
# İç sınıf olarak tanımlanan işçimiz
-keep class com.gunlukasistan.app.ArkaPlanIs$** { *; }
-keep class com.gunlukasistan.app.ArkaPlanIs { *; }

# WorkManager'ın kendi başlatıcısı (App Startup)
-keep class androidx.startup.** { *; }
