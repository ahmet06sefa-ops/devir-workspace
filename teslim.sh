#!/bin/bash
# GunlukAsistan — teslim paketi hazırlayıcı
# Kullanım: bash ~/teslim.sh
# Derler, APK'yı sürüm adıyla kopyalar, kaynak zip'ini tazeler, özet basar.
set -e
source /home/user/ortam.sh

cd /home/user/GunlukAsistan

VER=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts)
CODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)

echo "=== Derleniyor: v$VER (code $CODE) ==="
gradle :app:assembleDebug --console=plain -q 2>&1 | grep -E "^e: |FAILURE|error:" && {
  echo "DERLEME BAŞARISIZ"; exit 1
}

SRC_APK=app/build/outputs/apk/debug/app-debug.apk
[ -f "$SRC_APK" ] || { echo "APK üretilmedi!"; exit 1; }

cd /home/user
cp "GunlukAsistan/$SRC_APK" "GunlukAsistan-v$VER.apk"

rm -f "kaynak-v$VER-yedek.zip"
zip -q -r "kaynak-v$VER-yedek.zip" GunlukAsistan \
  -x 'GunlukAsistan/app/build/*' 'GunlukAsistan/build/*' 'GunlukAsistan/.gradle/*'

echo
echo "=== TESLİM PAKETİ HAZIR ==="
printf "  APK    : GunlukAsistan-v%s.apk  (%s)\n" "$VER" "$(du -h GunlukAsistan-v$VER.apk | cut -f1)"
printf "  Kaynak : kaynak-v%s-yedek.zip  (%s)\n" "$VER" "$(du -h kaynak-v$VER-yedek.zip | cut -f1)"
printf "  Notlar : PROJE-DURUM.md\n"
printf "  md5    : %s\n" "$(md5sum GunlukAsistan-v$VER.apk | awk '{print $1}')"
printf "  imza   : %s\n" "$(apksigner verify --print-certs GunlukAsistan-v$VER.apk | grep -m1 'SHA-256' | awk '{print $NF}' | cut -c1-16)…"

# ── Kullanıcı tercihi: teslim linkleri HER ZAMAN gofile.io üzerinden verilir ──
gofile_yukle() {
  local dosya="$1"
  for i in 1 2 3; do
    local srv=$(curl -sL --max-time 25 https://api.gofile.io/servers | grep -oP '"name":"\K[^"]+' | head -1)
    local link=$(curl -sL --max-time 350 -F "file=@$dosya" "https://$srv.gofile.io/uploadFile" | grep -oP '"downloadPage":"\K[^"]+')
    if [ -n "$link" ]; then echo "$link"; return 0; fi
    sleep 6
  done
  echo "YUKLEME_BASARISIZ"
  return 1
}
