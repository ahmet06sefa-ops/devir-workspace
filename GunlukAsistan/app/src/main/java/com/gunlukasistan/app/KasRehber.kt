package com.gunlukasistan.app

/**
 * v11.40 — Kas iskeleti rehberi.
 *
 * Her kas grubu için Türkçe açıklama, geliştirme ipuçları ve set/tekrar
 * önerisi. Egzersiz listesi [FitnessMotor] üzerinden kas grubuna göre
 * çekilir (free-exercise-db).
 */
object KasRehber {

    data class Kas(
        val kod: String,
        val ad: String,
        val emoji: String,
        val islev: String,          // ne işe yarar
        val gelistirme: String,     // nasıl geliştirilir
        val setOneri: String        // kaç set/tekrar
    )

    private val liste: Map<String, Kas> = listOf(
        Kas("neck", "Boyun", "🧍",
            "Başı taşır ve dengede tutar; omurganın üst kısmını destekler. Kafa hareketlerinin (çevirme, eğme) ana kasıdır.",
            "Düşük ağırlık, yüksek tekrar. Boyun fleksiyon/ekstansiyon, yan direnç hareketleri. Sakın ağır zorlamayın — omurilik güvenliği için kontrollü ve hafif çalışın.",
            "2-3 set × 12-15 tekrar (hafif)"),
        Kas("traps", "Trapez", "🏋️",
            "Omuzları kaldırır (omuz silkme), kürek kemiğini yukarı ve içe çeker; üst sırt görünümünü verir.",
            "Omuz silkme (dumbbell/barbell), farmer's walk, üst row. Kası her kaldırışta üstte sıkıştırıp 1 sn tutun.",
            "3-4 set × 10-15 tekrar"),
        Kas("shoulders", "Omuz", "🏋️",
            "Kolun her yönde kalkmasını sağlar (öne, yana, yukarı). Geniş omuz görünümünün temelidir.",
            "Shoulder press, lateral raise, front raise, reverse fly. Hacim için lateral raise, güç için press. Omuz çok hassastır — ısınmadan ağır yüklenmeyin.",
            "3-4 set × 8-15 tekrar"),
        Kas("chest", "Göğüs", "🫀",
            "Kolları gövdeye yaklaştırır (push itme hareketleri). Bench press, şınav gibi itme hareketlerinin ana kasıdır.",
            "Bench press, incline press, push-up, chest fly. Göğsün üst, orta ve alt bölgesini dengelemek için açıları değiştirin.",
            "3-4 set × 8-12 tekrar"),
        Kas("abdominals", "Karın", "🫃",
            "Gövdeyi büker ve döndürür; duruşu ve dengeyi korur. 'Altı paket' görünümünü verir.",
            "Crunch, plank, leg raise, mountain climber. Unutmayın: karın kası mutfakta şekillenir — düşük vücut yağı gerekir.",
            "3-4 set × 15-20 tekrar veya süreli plank"),
        Kas("biceps", "Biceps", "💪",
            "Dirseği büker (kendine çekme/curl hareketleri). Kolun ön üst kısmındaki 'pazı' kasıdır.",
            "Barbell/dumbbell curl, hammer curl, concentration curl. Negatif (iniş) fazını yavaş yapın — kasın en çok çalıştığı an budur.",
            "3-4 set × 8-12 tekrar"),
        Kas("triceps", "Triceps", "💪",
            "Dirseği uzatır (itme hareketleri). Kol hacminin asıl kaynağıdır — kolun üçte ikisini triceps oluşturur.",
            "Skull crusher, rope pushdown, diamond push-up, close-grip bench. Triceps hızlı toparlanır, haftada 2 kez çalışılabilir.",
            "3-4 set × 8-15 tekrar"),
        Kas("forearms", "Ön Kol", "💪",
            "Bileği ve parmakları kontrol eder; kavrama (grip) gücünü verir.",
            "Wrist curl, reverse curl, farmer's walk, dead hang. Kavrama gücü çoğu egzersizde yan fayda olarak çalışır.",
            "2-3 set × 12-20 tekrar"),
        Kas("lats", "Sırt Kanadı", "🏋️",
            "Kolu gövdeye çeker ve aşağı indirir; 'V' şeklindeki geniş sırt görünümünü verir.",
            "Pull-up, lat pulldown, dumbbell row, seated row. Omuzları aşağı ve geri çekerek sırtı sıkıştırarak çalışın.",
            "3-4 set × 8-12 tekrar"),
        Kas("middle back", "Orta Sırt", "🏋️",
            "Kürek kemiğini geri çeker; sırtın kalınlığını ve duruşu destekler.",
            "Barbell row, chest-supported row, T-bar row. Dirsekleri kalça yönünde çekin, sırtı düz tutun.",
            "3-4 set × 8-12 tekrar"),
        Kas("lower back", "Bel", "🫀",
            "Omurgayı stabilize eder ve eğilmeyi/doğrulmayı sağlar. Deadlift'in ana kasıdır.",
            "Deadlift, back extension, good morning. Bel ağır yük altında hassastır — form çok önemli, yükü yavaşça artırın.",
            "3-4 set × 5-12 tekrar (deadlift)"),
        Kas("glutes", "Kalça", "🍑",
            "Kalçayı uzatır (itme/çömelme hareketleri); yürüme, koşma ve çömelmenin motoru.",
            "Squat, hip thrust, glute bridge, lunge, Romanian deadlift. Kalça kası güçlüdür — ağır çalışmayı kaldırır.",
            "3-4 set × 8-15 tekrar"),
        Kas("quadriceps", "Ön Bacak", "🦵",
            "Dizi uzatır; çömelme, koşma, merdiven çıkmanın ana kası. Bacak ön üst kısmı.",
            "Squat, leg press, lunge, leg extension, step-up. Derin squat ön bacağı daha çok çalıştırır.",
            "3-4 set × 8-15 tekrar"),
        Kas("hamstrings", "Arka Bacak", "🦵",
            "Dizi büker ve kalçayı uzatır; koşu hızı ve deadlift gücü için kritik.",
            "Romanian deadlift, leg curl, hip bridge, Nordic curl. Sık sık yaralanan kas — esnemeyi ve kontrollü negatifleri ihmal etmeyin.",
            "3-4 set × 8-15 tekrar"),
        Kas("calves", "Baldır", "🦵",
            "Ayak parmağı üzerinde durmayı ve zıplamayı sağlar; bacak alt kısmı.",
            "Standing/calf raise, seated calf raise. Baldır genetik dirençlidir — ağır ve kontrollü, tam açılımla çalışın.",
            "3-4 set × 12-20 tekrar"),
        Kas("abductors", "Kaçıranlar", "🦵",
            "Bacağı vücuttan dışarı açar (yana); kalça dengesi ve yürüme için önemli.",
            "Hip abduction machine, side-lying leg raise, lateral band walk. Kalça stabilizasyonunda çok etkilidir.",
            "3 set × 15-20 tekrar"),
        Kas("adductors", "Yaklaştırıcılar", "🦵",
            "Bacağı vücudun ortasına çeker (içe); iç bacak kasları.",
            "Hip adduction machine, sumo squat, side lunge. Squat ve koşuda stabiliteyi artırır.",
            "3 set × 12-20 tekrar")
    ).associateBy { it.kod }

    fun getir(kod: String): Kas? = liste[kod]

    fun hepsi(): List<Kas> = liste.values.sortedBy { it.ad }

    /** Haritada gösterilecek kas kodu → bölge etiketi (ön/arka). */
    fun etiket(kod: String): String = getir(kod)?.ad ?: kod
}
