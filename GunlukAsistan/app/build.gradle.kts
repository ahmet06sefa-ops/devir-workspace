plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.gunlukasistan.app"

    signingConfigs {
        named("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            // v1 (JAR) imzası şart: bazı telefonların paket yükleyicisi
            // yalnızca v2 imzalı APK'ları "ayrıştırılamadı" diye reddediyor.
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gunlukasistan.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 298
        versionName = "11.43"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // v8.8 · Öneri 14: R8 açıldı.
            //
            // ── Neden şimdiye kadar kapalıydı ──
            // Kapalıyken kullanılmayan sınıflar APK'da kalıyor ve
            // isimler kısaltılmıyordu. Ölçüm: 194 dosyalık projede
            // PDFBox + BouncyCastle gibi kütüphanelerin büyük kısmı
            // hiç çağrılmıyor.
            //
            // ── Neden riskli ──
            // R8 yansıma (reflection) ile erişilen sınıfları göremez;
            // Room varlıkları, JSON serileştirme ve widget sağlayıcıları
            // korunmalı. Kurallar `proguard-rules.pro` içinde.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // ══════════════════════════════════════════════════════════════
    // v8.8 · Öneri 13 — APK'dan 7,9 MB ölü kod çıkarıldı
    // ══════════════════════════════════════════════════════════════
    //
    // ── Ölçüm ──
    // `unzip -l` çıktısı: BouncyCastle'ın post-quantum kripto veri
    // dosyaları 7,87 MB yer kaplıyordu:
    //   lowmc.properties  3.555 KB   (Picnic imza şeması)
    //   p751.properties   1.894 KB   (SIKE anahtar değişimi)
    //   p610.properties   1.128 KB
    //   p503.properties     747 KB
    //   p434.properties     648 KB
    //
    // ── Neden APK'daydı ──
    // `pdfbox-android` bağımlılığı BouncyCastle'ı çekiyor (şifreli PDF
    // açmak için). BouncyCastle da bütün deneysel kripto şemalarını
    // birlikte getiriyor.
    //
    // ── Neden güvenle çıkarılabilir ──
    // Kod taraması: uygulamada `bouncycastle` geçen TEK BİR satır yok.
    // Picnic ve SIKE, NIST post-quantum yarışmasının deneysel
    // şemaları — şifreli PDF açmak için de kullanılmıyorlar.
    // Şifreli PDF desteği bozulmaz: onu sağlayan sınıflar
    // (`org.bouncycastle.crypto.*`) duruyor, yalnız `pqc` veri
    // dosyaları çıkarılıyor.
    packaging {
        resources {
            excludes += setOf(
                "org/bouncycastle/pqc/crypto/picnic/*.properties",
                "org/bouncycastle/pqc/crypto/sike/*.properties",
                // Sertifika hata mesajlarının çevirileri (48 KB × 2).
                // Uygulama bu mesajları hiç göstermiyor.
                "org/bouncycastle/x509/CertPathReviewerMessages*.properties",
                // Kütüphane meta verileri — çalışma anında gereksiz
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
                "kotlin/**",
                "DebugProbesKt.bin"
            )
        }
    }

    buildFeatures {
        // v8.8 · Öneri 12: hata raporunda sürüm bilgisi göstermek için
        // BuildConfig gerekiyor (AGP 8'de varsayılan kapalı).
        buildConfig = true
        // v8.8 · Öneri 18 hazırlığı: ViewBinding. Faz 2'de kullanılacak;
        // şimdiden açıp derlemenin bozulmadığını doğruluyoruz.
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    // v7.58: üstten aşağı çekince yenileme (pull-to-refresh)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    // v8.2: açılış ekranı (öneri 6). Android 12 öncesinde de aynı
    // görünümü verir, sonrasında sistemin kendi splash'ını kullanır.
    implementation("androidx.core:core-splashscreen:1.0.1")
    // v8.2: kaydırma jesti ve düzen geçişleri için (öneri 4, 1)
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    // v7.76 DENEME: Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    // v6.7: PDF okuma/bölme (içindekiler tablosu + sayfa aralığı ayırma)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // v7.19: fotoğrafın EXIF yön bilgisini okumak için (el yazısı tarama)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // v8.9 · Öneri 16: coroutine + yaşam döngüsüne bağlı kapsam.
    //
    // ── Neden gerekli ──
    // Kod tabanında 87 × `runOnUiThread` vardı ve arka plan işleri
    // elle `Executors` ile yönetiliyordu. Sorun: fragment yok
    // edildikten sonra gelen sonuç `requireContext()` çağırıp
    // çöküyordu. `viewLifecycleOwner.lifecycleScope` bu işleri
    // görünüm yok olduğunda OTOMATİK iptal ediyor.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // v9.8 · Öneri 47: WorkManager.
    //
    // ── NEDEN GEREKLİ ──
    // Yedekleme şu an `Performans.geciktir` ile 2,5 saniye sonra
    // çalışıyor. Bu, uygulama AÇIKKEN işe yarıyor. Ama:
    //   · Kullanıcı uygulamadan çıkarsa iş yarıda kalır
    //   · Sistem uygulamayı öldürürse iş kaybolur
    //   · Cihaz yeniden başlarsa iş bir daha denenmez
    //
    // WorkManager bunları çözüyor: iş diske kaydediliyor, uygulama
    // ölse de yeniden başlatılsa da çalıştırılıyor, başarısız
    // olursa üstel geri çekilmeyle tekrar deneniyor.
    //
    // ── NEDEN HER ŞEYİ TAŞIMADIM ──
    // Alarmlar (`AlarmManager`) WorkManager'a taşınmadı. WorkManager
    // TAM ZAMAN garantisi vermiyor — "yaklaşık" çalışıyor. İlaç
    // hatırlatması 08:00'de gelmeli, 08:20'de değil. Alarmlar
    // AlarmManager'da kalıyor; WorkManager yalnızca zamanı kritik
    // OLMAYAN işler için: yedekleme, senkron, temizlik.
    //
    // Boyut maliyeti: ~200 KB (R8 sonrası daha az).
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // v7.99: birim testleri (öneri 10).
    //
    // Projede 62.000 satır kod vardı ve HİÇ test yoktu. v7.62'de dört
    // sürüm boyunca tüm butonlar kırık kaldı; ancak kullanıcı bildirince
    // fark edildi. Saf mantık fonksiyonları (tarih hesabı, şifreleme,
    // komut çözümleme, JSON serileştirme) test edilebilir ve bu tür
    // regresyonları derleme anında yakalar.
    //
    // Robolectric yerine sade JUnit: Android çerçevesine bağımlı olmayan
    // fonksiyonlar seçildi, böylece testler saniyeler içinde çalışıyor.
    testImplementation("junit:junit:4.13.2")
    // v8.9: coroutine testleri için
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // v9.8: birim testlerinde GERÇEK org.json.
    //
    // ── Neden gerekli ──
    // Android'in `android.jar` saplaması (stub) birim testlerinde
    // `org.json` çağrılarında "not mocked" hatası fırlatıyor.
    // `Guncelleme.ayristir` JSON çözüyor ve testleri bu yüzden
    // patlamıştı — kodda hata yoktu, test ortamı eksikti.
    //
    // Alternatif `testOptions { unitTests.isReturnDefaultValues = true }`
    // olurdu ama o TÜM Android çağrılarını sessizce null/0 yapar;
    // gerçek hataları gizler. Bu daha dar ve dürüst çözüm.
    //
    // testImplementation → APK'ya GİRMEZ, boyutu etkilemez.
    testImplementation("org.json:json:20231013")
}
