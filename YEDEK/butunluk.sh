#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
# Tur başı 60 sn bütünlük denetimi — geriye dönüşleri erken yakala.
# Çıktı SAGLAM/EKSIK satırlarıdır; eksik olanın çaresi satırın sonunda yazar.
#
#   bash /home/user/YEDEK/butunluk.sh
# ═══════════════════════════════════════════════════════════════════
PROJE=/home/user/GunlukAsistan
SRC=$PROJE/app/src/main/java/com/gunlukasistan/app
TST=$PROJE/app/src/test/java/com/gunlukasistan/app

ok(){ echo "  SAGLAM  $1"; }
no(){ echo "  EKSIK   $1  →  $2"; }

echo "== ÇATI =="
[ -f /opt/jdk17/bin/java ] && ok "jdk17" || no "jdk17" "bash YEDEK/kurtar.sh"
[ -d /opt/android-sdk/platforms/android-34 ] && ok "sdk34" || no "sdk34" "bash YEDEK/kurtar.sh"
[ -f /opt/gradle-8.7/bin/gradle ] && ok "gradle 8.7" || no "gradle 8.7" "bash YEDEK/kurtar.sh"
[ "$(du -s /home/user/.gradle-home 2>/dev/null | cut -f1)" -gt 100000 ] 2>/dev/null \
  && ok "gradle önbellek" || no "gradle önbellek" "bash YEDEK/kurtar.sh hafif"
grep -q swapfile /proc/swaps && ok "swap" || no "swap" "sudo swapon (kurtar.sh yapar)"

echo "== KAYNAK AĞAÇ =="
KT=$(ls $SRC/*.kt 2>/dev/null | wc -l)
[ "$KT" -gt 330 ] && ok "$KT kaynak dosya" || no "kaynak sayısı=$KT" "zip'ten geri yükle"
grep -qm1 'versionCode = 198' $PROJE/app/build.gradle.kts \
  && ok "sürüm 198/10.42" || no "build.gradle" "zip'ten geri yükle"

# Kritik dalga dosyaları (yeni dalgada ekle)
E=0
for f in AliskanlikMola AliskanlikNot SeriAnaliz IleriSayim IleriSayimBildirim \
         IleriSayimReceiver PlanAsistan GunlukBildirim SabahReceiver AksamReceiver \
         Store HabitsFragment TimerFragment SayacAyar SayacKadraniView \
         SettingsFragment BootReceiver TimerNotifier MainActivity; do
  [ -f "$SRC/$f.kt" ] || { no "$f.kt" "unzip -oq /home/user/kaynak-v10.42-yedek.zip -d $PROJE"; E=1; }
done
for t in AliskanlikMolaTest AliskanlikNotTest SeriAnalizTest IleriSayimBildirimTest \
         SayacAyarOlcekTest PlanAsistanTest; do
  [ -f "$TST/$t.kt" ] || { no "test $t.kt" "zip'ten geri yükle"; E=1; }
done
[ $E = 0 ] && ok "kritik dosyalar (17+6)"

echo "== SİGORTALAR =="
[ -d $PROJE/.git ] && ok "git ($(cd $PROJE && git log --oneline -1 | cut -c1-30))" \
  || no ".git" "bash YEDEK/git-sigorta.sh geri"
[ -s /home/user/YEDEK/repo.gitbundle ] && ok "git bundle" || no "bundle" "bash YEDEK/git-sigorta.sh yenile"
ls /home/user/kaynak-v10.42-yedek.zip >/dev/null 2>&1 && ok "yerel zip v10.42" \
  || no "yerel zip" "kaynak-ayna.sh indir"
echo "BUTUNLUK_BITTI"
