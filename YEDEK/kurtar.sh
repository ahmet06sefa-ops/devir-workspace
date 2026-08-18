#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
# Günlük Asistan — TEK KOMUT ORTAM KURTARMA (sandbox sıfırlanınca)
#
#   bash /home/user/YEDEK/kurtar.sh            # çatı + önbellek (tam kurulum)
#   bash /home/user/YEDEK/kurtar.sh hafif      # yalnız gradle önbelleği
#
# Paketler filebin.net'te (uçtan uca md5 ile kanıtlandı).
# DİKKAT: filebin saklama ~6 gün — süre dolmuşsa bu betik md5 denetiminde
# yakalar ve ESKİ YÖNTEME (kur-ortam.sh) yönlendirir; ayrıca her çalışma
# turu başında ayna tazelenir (bkz. DEVIR/CALISMA-PROTOKOLU.md madde 0).
#
# Paket üretimi (ortam değişince):
#   tar -I 'gzip -1' -cf ortam-cati.tar.gz -C / opt/jdk17 opt/android-sdk opt/gradle-8.7
#   tar -I 'gzip -1' -cf gradle-onbellek.tar.gz --exclude=.gradle-home/daemon \
#       --exclude=.gradle-home/notifications -C /home/user .gradle-home
# ═══════════════════════════════════════════════════════════════════
set -e

BIN="ortam-gunluk-20260808-2003"
CATI_URL="https://filebin.net/$BIN/ortam-cati.tar.gz"
ONB_URL="https://filebin.net/$BIN/gradle-onbellek.tar.gz"
CATI_MD5="7c3f1f2535b70dae665868aef110fb7e"
ONB_MD5="280bcb8f80988eb7afdcfcb38c1581a4"
DL="/home/user/YEDEK/dl"
mkdir -p "$DL"

indir_dogrula() {  # $1=url $2=hedef $3=beklenen md5
  local hedef="$2"
  curl -sL --retry 3 --max-time 900 -o "$hedef" "$1"
  [ -s "$hedef" ] || { echo "HATA: indirilemedi $1"; exit 2; }
  local gelen
  gelen=$(md5sum "$hedef" | cut -d' ' -f1)
  if [ "$gelen" != "$3" ]; then
    echo "HATA: md5 uyumsuz ($hedef)  gelen=$gelen beklenen=$3"
    echo "→ Ayna süresi dolmuş olabilir: yeni ayna yüklenip defter güncellenmeli,"
    echo "  veya yedek yöntem: bash /home/user/kur-ortam.sh"
    rm -f "$hedef"; exit 3
  fi
  echo "  ✓ md5 uyumlu: $(du -h "$hedef" | cut -f1)"
}

echo "=== [0/3] Takas (build OOM koruması) ==="
if ! grep -q swapfile /proc/swaps; then
  sudo fallocate -l 3G /swapfile 2>/dev/null || true
  sudo chmod 600 /swapfile 2>/dev/null || true
  sudo mkswap /swapfile >/dev/null 2>&1 || true
  sudo swapon /swapfile 2>/dev/null || true
fi
free -m | head -2 | tail -1

if [ "$1" != "hafif" ]; then
  echo "=== [1/3] Çatı (JDK + SDK + Gradle) ==="
  indir_dogrula "$CATI_URL" "$DL/ortam-cati.tar.gz" "$CATI_MD5"
  sudo tar xzf "$DL/ortam-cati.tar.gz" -C /
  sudo chown -R "$(id -u)":"$(id -g)" /opt/android-sdk /opt/jdk17 /opt/gradle-8.7 2>/dev/null || true
  rm -f "$DL/ortam-cati.tar.gz"
else
  echo "=== [1/3] Çatı atlandı (hafif mod) ==="
fi

echo "=== [2/3] Gradle önbelleği (derleme hızı) ==="
indir_dogrula "$ONB_URL" "$DL/gradle-onbellek.tar.gz" "$ONB_MD5"
mkdir -p /home/user/.gradle-home
tar xzf "$DL/gradle-onbellek.tar.gz" -C /home/user
rm -f "$DL/gradle-onbellek.tar.gz"

echo "=== [3/3] Doğrulama ==="
echo "JAVA : $(/opt/jdk17/bin/java -version 2>&1 | head -1)"
echo "SDK  : $(ls /opt/android-sdk/platforms)"
echo "GRDL : $(/opt/gradle-8.7/bin/gradle -v 2>&1 | grep -m1 '^Gradle')"
echo "CACHE: $(du -sh /home/user/.gradle-home | cut -f1)"
echo "ORTAM_HAZIR"
