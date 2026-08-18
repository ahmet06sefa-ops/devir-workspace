#!/bin/bash
# ═══════════════════════════════════════════════════════════════════
# git sigortası — .git klasörü anlık görüntüden dönmediğinde tek dosyalık
# paketten (repo.gitbundle) tam geçmişi geri kurar.
#
#   bash /home/user/YEDEK/git-sigorta.sh yenile   # tur SONU: bundle tazele
#   bash /home/user/YEDEK/git-sigorta.sh geri     # tur BAŞI: .git yoksa kur
# ═══════════════════════════════════════════════════════════════════
set -e
PROJE=/home/user/GunlukAsistan
BUNDLE=/home/user/YEDEK/repo.gitbundle
cd "$PROJE"

case "$1" in
  geri)
    if [ -d .git ]; then echo "GIT_YERINDE"; exit 0; fi
    [ -s "$BUNDLE" ] || { echo "GIT_SIGORTA_YOK"; exit 1; }
    git clone "$BUNDLE" /tmp/git-geri 2>/dev/null
    mv /tmp/git-geri/.git ./.git && rm -rf /tmp/git-geri
    git read-tree HEAD && git checkout-index -q -f -a 2>/dev/null || true
    echo "GIT_GERI_KURULDU: $(git log --oneline -1)"
    ;;
  yenile|"")
    [ -d .git ] || { echo "GIT_YOK_ONCE_GERI"; exit 1; }
    git bundle create "$BUNDLE.yeni" --all >/dev/null 2>&1
    mv "$BUNDLE.yeni" "$BUNDLE"
    git bundle verify "$BUNDLE" >/dev/null 2>&1 || { echo "BUNDLE_BOZUK"; exit 1; }
    echo "GIT_SIGORTA_TAZE: $(du -h "$BUNDLE" | cut -f1) · $(git log --oneline -1)"
    ;;
esac
