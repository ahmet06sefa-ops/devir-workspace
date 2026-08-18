#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
# KAYNAK AYNASI — gofile bizim API indirmemize kapalı (error-notPremium),
# filebin ise anonim indirmeye açık. Bu betik en son kaynak ziplerini ve
# git bundle'ı filebin'e yükler; anlık görüntü geriye dönerse uçtan uca
# md5 kanıtlı geri indiririz.
#
#   bash /home/user/YEDEK/kaynak-ayna.sh           # canlılık denetle + yeni zip varsa yükle
#   bash /home/user/YEDEK/kaynak-ayna.sh zorla     # yeni bin aç, her şeyi yükle
#   bash /home/user/YEDEK/kaynak-ayna.sh indir     # en son zip+bundle'ı geri indir (md5'li)
# ═══════════════════════════════════════════════════════════════════
set -e
cd /home/user/YEDEK
DEF=kaynak-ayna.md
BIN=$(grep -oP '^bin: \K\S+' $DEF 2>/dev/null || true)

INDIRILECEKLER() {  # en yeni 1 zip + bundle + betik/belge tar'ı
  tar -czf /tmp/yedek-yedek.tar.gz -C /home/user YEDEK DEVIR \
      --exclude=YEDEK/dl --exclude=YEDEK/paket --exclude=YEDEK/yedek-yedek.tar.gz 2>/dev/null
  mv /tmp/yedek-yedek.tar.gz /home/user/YEDEK/yedek-yedek.tar.gz
  ls -t /home/user/kaynak-v*-yedek.zip 2>/dev/null | head -1
  [ -s /home/user/YEDEK/repo.gitbundle ] && echo /home/user/YEDEK/repo.gitbundle
  echo /home/user/YEDEK/yedek-yedek.tar.gz
}

yukle_bin() {  # $1=bin
  local b=$1 dosya ad
  for dosya in $(INDIRILECEKLER); do
    ad=$(basename "$dosya")
    curl -s --max-time 550 --data-binary "@$dosya" -H "filename: $ad" \
      "https://filebin.net/$b/$ad" > /dev/null
    echo "  yuklendi: $ad"
  done
  md5sum $(INDIRILECEKLER) | sed 's|/home/user/||;s|YEDEK/||' > /tmp/ka-md5.txt
  curl -s --max-time 120 --data-binary "@/tmp/ka-md5.txt" -H "filename: kaynak-md5.txt" \
    "https://filebin.net/$b/kaynak-md5.txt" > /dev/null
}

canli_mi() {  # $1=bin
  [ -n "$1" ] || return 1
  [ "$(curl -s --max-time 25 "https://filebin.net/$1" -H "Accept: application/json" -o /tmp/ka.json -w "%{http_code}" || true)" = 200 ]
}

if [ "$1" = "zorla" ] || ! canli_mi "$BIN"; then
  [ "$1" = "indir" ] && { echo "AYNA_OLU_INDIRILEMEZ"; exit 2; }
  BIN="kaynak-gunluk-$(date +%Y%m%d-%H%M)"
  echo "Yeni kaynak bini: $BIN"
  yukle_bin "$BIN"
  printf 'bin: %s\ntazeleme: %s\n' "$BIN" "$(date '+%Y-%m-%d %H:%M')" > $DEF
  echo "- $(date '+%Y-%m-%d %H:%M') · kaynak aynası: \`$BIN\`" >> ayna-gecmisi.md
  echo "KAYNAK_BIN=$BIN"
fi

if [ "$1" = "indir" ]; then
  canli_mi "$BIN" || { echo "AYNA_OLU (bin=$BIN) — önce: bash kaynak-ayna.sh zorla"; exit 2; }
  curl -sL --max-time 60 --retry 2 "https://filebin.net/$BIN/kaynak-md5.txt" -o /tmp/ka-md5.txt
  while read -r beklenen dosya; do
    hedef="/home/user/$dosya"
    case "$dosya" in YEDEK/*) hedef="/home/user/$dosya";; *) hedef="/home/user/$dosya";; esac
    curl -sL --max-time 550 --retry 2 "https://filebin.net/$BIN/$(basename $dosya)" -o "$hedef"
    gelen=$(md5sum "$hedef" | cut -d' ' -f1)
    [ "$gelen" = "$beklenen" ] && echo "  ✓ indi+md5: $hedef" || { echo "  ✗ md5: $hedef"; exit 3; }
  done < /tmp/ka-md5.txt
  echo "KAYNAK_GERI_INDI"
  exit 0
fi

# Canlı bin: manifestteki zip yerelde yoksa/lokal zip daha yeniyse yükle
YENIZIP=$(ls -t /home/user/kaynak-v*-yedek.zip | head -1)
grep -q "$(basename $YENIZIP)" /tmp/ka.json 2>/dev/null || {
  echo "Yeni zip binde yok — yükleniyor"
  yukle_bin "$BIN"
  printf 'bin: %s\ntazeleme: %s\n' "$BIN" "$(date '+%Y-%m-%d %H:%M')" > $DEF
}
echo "KAYNAK_AYNA_CANLI (bin=$BIN)"
