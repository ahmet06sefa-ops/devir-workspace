#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
# Ortam aynası TAZELEME — her çalışma turunun başında bir kez çağrılır.
# filebin saklaması ~6 gün; bu betik linkin ölü olup olmadığını ölçer,
# ölüyse /opt ve önbellek (henüz canlıysa) paketten yeniden üretip
# YENİ bin'e yükler, kurtar.sh + bu dosyanın içindeki sabitleri günceller,
# sonucu YEDEK/BAGLANTI-DEFTERI.md geçmişine işler.
#
#   bash /home/user/YEDEK/ayna-tazele.sh        # denetle, gerekirse yenile
#   bash /home/user/YEDEK/ayna-tazele.sh zorla  # koşulsuz yeniden üret+yükle
# ═══════════════════════════════════════════════════════════════════
set -e
cd /home/user/YEDEK

BIN=$(grep -oP '^BIN="\K[^"]+' kurtar.sh)
# Canlılık denetimi BIN kökünden yapılır — dosya URL'si 302 (CDN yönlendirmesi)
# döndürür; onu "ölü" sanıp gereksiz paket üretmek bu sürümde yaşandı (düzeltildi).
YAS=$(curl -s --max-time 30 "https://filebin.net/$BIN" -H "Accept: application/json" \
  -o /tmp/ayna-meta.json -w "%{http_code}" || true)

if [ "$1" != "zorla" ] && [ "$YAS" = "200" ]; then
  BITIS=$(python3 -c "import json;print(json.load(open('/tmp/ayna-meta.json')).get('bin',{}).get('expired_at','?'))" 2>/dev/null || echo "?")
  echo "AYNA_CANLI (bin=$BIN, son: $BITIS) — tazeleme gerekmedi."
  exit 0
fi

echo "Ayna ölü/zorla — yeniden üretiliyor…"
mkdir -p paket
cd paket
[ -f ortam-cati.tar.gz ] || tar -I 'gzip -1' -cf ortam-cati.tar.gz -C / opt/jdk17 opt/android-sdk opt/gradle-8.7
rm -f gradle-onbellek.tar.gz
tar -I 'gzip -1' -cf gradle-onbellek.tar.gz --exclude=.gradle-home/daemon --exclude=.gradle-home/notifications -C /home/user .gradle-home
md5sum *.tar.gz > ../md5.txt

# gofile aynası (kullanıcıya da gösterilebilen kalıcı sayfa)
srv=$(curl -sL --max-time 25 https://api.gofile.io/servers | grep -oP '"name":"\K[^"]+' | head -1)
curl -sL --max-time 550 -F "file=@ortam-cati.tar.gz" "https://$srv.gofile.io/uploadFile" > upload-cati.json
curl -sL --max-time 550 -F "file=@gradle-onbellek.tar.gz" "https://$srv.gofile.io/uploadFile" > upload-onbellek.json

# filebin aynası (scriptli kurtarma için)
YENI_BIN="ortam-gunluk-$(date +%Y%m%d-%H%M)"
curl -s --max-time 550 --data-binary "@ortam-cati.tar.gz" -H "filename: ortam-cati.tar.gz" "https://filebin.net/$YENI_BIN/ortam-cati.tar.gz" > /tmp/fb1.json
curl -s --max-time 550 --data-binary "@gradle-onbellek.tar.gz" -H "filename: gradle-onbellek.tar.gz" "https://filebin.net/$YENI_BIN/gradle-onbellek.tar.gz" > /tmp/fb2.json

CATI_MD5=$(grep ortam-cati ../md5.txt | cut -d' ' -f1)
ONB_MD5=$(grep gradle-onbellek ../md5.txt | cut -d' ' -f1)

# kurtar.sh sabitlerini güncelle
python3 - "$YENI_BIN" "$CATI_MD5" "$ONB_MD5" <<'PYEOF'
import io, re, sys
bin_, cm, om = sys.argv[1], sys.argv[2], sys.argv[3]
p = "/home/user/YEDEK/kurtar.sh"
s = io.open(p, encoding="utf-8").read()
s = re.sub(r'BIN="[^"]*"', 'BIN="%s"' % bin_, s, count=1)
s = re.sub(r'CATI_MD5="[^"]*"', 'CATI_MD5="%s"' % cm, s, count=1)
s = re.sub(r'ONB_MD5="[^"]*"', 'ONB_MD5="%s"' % om, s, count=1)
io.open(p, "wb").write(s.encode("utf-8", "strict"))
print("kurtar.sh güncellendi:", bin_)
PYEOF

# paketleri lokasyonda tutma — 900MB snapshot'u şişirir; kaynak (/opt + önbellek) yerinde
rm -f ortam-cati.tar.gz gradle-onbellek.tar.gz

echo "- $(date '+%Y-%m-%d %H:%M') · ayna tazelendi: filebin bin=\`$YENI_BIN\` (çatı md5=$CATI_MD5, önbellek md5=$ONB_MD5)" >> /home/user/YEDEK/ayna-gecmisi.md
echo "AYNA_TAZELENDI: $YENI_BIN"
