#!/bin/bash
# PARALEL gofile yükleyici (v2 hız protokolü)
# Kullanım: bash yukle-gofile.sh DOSYA1 [DOSYA2 DOSYA3 ...]
# Sonuç: /home/user/yukle-sonuc.txt — "dosya|OK|link" veya "dosya|HATA|yanıt"
set -u
OUT=/home/user/yukle-sonuc.txt
: > "$OUT"
srv=$(curl -sL --max-time 25 https://api.gofile.io/servers | grep -oP '"name":"\K[^"]+' | head -1)
if [ -z "${srv:-}" ]; then echo "SUNUCU_BULUNAMADI" | tee "$OUT"; exit 1; fi
for f in "$@"; do
  base=$(basename "$f")
  (
    resp=$(curl -sL --max-time 550 -F "file=@$f" "https://$srv.gofile.io/uploadFile")
    link=$(printf '%s' "$resp" | python3 -c "import sys,json
try:
    d=json.load(sys.stdin); print(d.get('data',{}).get('downloadPage','') or '')
except Exception:
    print('')" 2>/dev/null)
    if [ -n "$link" ]; then echo "$base|OK|$link" >> "$OUT"; else echo "$base|HATA|$resp" >> "$OUT"; fi
  ) &
done
wait
cat "$OUT"
