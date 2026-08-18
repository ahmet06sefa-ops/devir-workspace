# 🔗 BAĞLANTI DEFTERİ — Günlük Asistan Yedek & Ortam Kaydı

> **Bu dosya asla silinmez.** Her sürümün 3 linki ve geliştirme ortamının
> yedek linkleri burada kalıcıdır. Kural: eski APK/zip lokalden ancak
> linki BU deftere işlendikten sonra kaldırılır. Defter her teslimde güncellenir.

## 🏗 Geliştirme ortamı yedekleri — sandbox sıfırlanınca TEK KOMUT kurtarma

**Kurtarma:** `bash /home/user/YEDEK/kurtar.sh` → ~2-3 dakikada JDK+SDK+Gradle+ısınmış
önbellek geri gelir (eski yöntem ~10+ dk sürüyordu), `ORTAM_HAZIR` basarsa tamamdır.

| Katman | filebin (scriptli kurtarma) | gofile (yedek ayna) | md5 |
|---|---|---|---|
| Çatı (jdk17+sdk34+gradle8.7, 605M) | https://filebin.net/ortam-gunluk-20260808-2003/ortam-cati.tar.gz | https://gofile.io/d/7YcBoM | 7c3f1f2535b70dae665868aef110fb7e |
| Önbellek (.gradle-home, 339M) | https://filebin.net/ortam-gunluk-20260808-2003/gradle-onbellek.tar.gz | https://gofile.io/d/04YJ94 | 280bcb8f80988eb7afdcfcb38c1581a4 |

- filebin saklaması **~6 gün** (bu binin sonu: 2026-08-15 20:05 UTC) — her tur
  başında `bash YEDEK/ayna-tazele.sh` çalıştırılır; ölüyse otomatik yeniden
  üretip yeni bin'e yükler ve kurtar.sh sabitlerini günceller. Geçmiş: `YEDEK/ayna-gecmisi.md`
- gofile linkleri süresiz durur ama scriptlenmiş indirmeye kapalı (bot koruması —
  `error-token`, kanıtlandı); onlar kullanıcı tarafı için yedek aynadır.
- Tar paketleri lokasyonu: **tutulmaz** (900M snapshot'u şişirir). Kaynak
  `/opt` + `.gradle-home` canlıyken her an yeniden üretilebilir (ayna-tazele.sh).

## 📦 Sürüm arşivi (gofile — kalıcı linkler)

| Sürüm | APK | Kaynak zip | Durum notu | Lokal APK md5 |
|---|---|---|---|---|
| 🔧 **SİGORTA TURU** | — | — | KOD-ATLASI + butunluk/git-sigorta/kaynak-ayna betikleri kuruldu; filebin bin=kaynak-gunluk-20260809-1904 | — |
| **10.45** (kod 201) | [APK](https://gofile.io/d/qixmZz) | [kaynak zip](https://gofile.io/d/oLdfEu) | [notlar](https://gofile.io/d/Vmts0a) | `9a48b92c52a88e2dbbc7fff676826d82` |
| **10.44** (kod 200) | [APK](https://gofile.io/d/laTgJQ) | [kaynak zip](https://gofile.io/d/nJ0wED) | [notlar](https://gofile.io/d/mTnTAS) | `b9bc39f721add23515eed5cde3ca262a` |
| **10.43** (kod 199) | [APK](https://gofile.io/d/pIy1wQ) | [kaynak zip](https://gofile.io/d/sVm5fM) | [notlar](https://gofile.io/d/DmXt6Z) | `67a002d2ec41ece89e9cc99be8316145` |
| **10.42** (kod 198) | [APK](https://gofile.io/d/l5jwVh) | [kaynak zip](https://gofile.io/d/wtoM17) | [notlar](https://gofile.io/d/sxyaKd) | `a2c434674cef19819b658af586eead94` |
| **10.41** (kod 197) | [APK](https://gofile.io/d/INHfMs) | [kaynak zip](https://gofile.io/d/ZHsgl9) | [notlar](https://gofile.io/d/3RZicP) | `439485c28f52c07bce5104ba4a15436d` |
| **10.40** (code 196) | [APK](https://gofile.io/d/aYA31O) | [kaynak zip](https://gofile.io/d/sLkxJg) | [notlar](https://gofile.io/d/4XKIyD) | `4b2e9361e28dc3dee147f182cdcd27eb` |
| **10.39** (code 195) | [APK](https://gofile.io/d/6IaaUt) | [kaynak zip](https://gofile.io/d/PdOYoD) | [notlar](https://gofile.io/d/dtKfuD) | `05c6bf8f360ff709d3395bff1a439009` |
| **10.38** (code 194) | [APK](https://gofile.io/d/lhFe57) | [kaynak zip](https://gofile.io/d/j6scha) | [notlar](https://gofile.io/d/NIVn6K) | `58728b1b0b554023a01ba9bfc9c0285d` |
| **10.37** (code 193) | [APK](https://gofile.io/d/RSKJIS) | [kaynak zip](https://gofile.io/d/NwiKNf) | [notlar](https://gofile.io/d/McMG9V) | `a940910375c75ba878b69ef976b0300f` |
| **10.36** (code 192) | https://gofile.io/d/n5Lz8l | https://gofile.io/d/HCojkY | https://gofile.io/d/4dWqLY | 0340236fdede4fdb731658844ef64cd2 |
| **10.35** (code 191) | https://gofile.io/d/PohxPF | https://gofile.io/d/CEHfvc | https://gofile.io/d/EpABLo | 37144ef92feac82904d43af93941fc44 |
| **10.34** (code 190) | https://gofile.io/d/DpWxbY | https://gofile.io/d/TrfFtt | https://gofile.io/d/d1Ursv | 6d6ce93899fd3b2f36f57b7523b1ebd8 |
| **10.33** (code 189) | https://gofile.io/d/0Coho6 | https://gofile.io/d/A4GebT | https://gofile.io/d/Mhupsb | f774c799f7bdf5a5f44723f60e8999ac |
| **10.32** (code 188) | https://gofile.io/d/9QNRxt | https://gofile.io/d/Zzyj7m | https://gofile.io/d/c45uDV | 38fd332b271844800d4588fc2568dcfa |
| **10.31** (code 187) | https://gofile.io/d/x9Gi8d | https://gofile.io/d/8RcXoK | https://gofile.io/d/lGJ8D4 | c640dd85eb71917f3abb19364dbda8bb |
| **10.30** (code 186) | https://gofile.io/d/dnNprh | https://gofile.io/d/LBUN7C | https://gofile.io/d/wnfZXZ | 189df55720daa07fe3ecc1bcc707ae7c |
| **10.29** (code 185) | https://gofile.io/d/9XaVFf | https://gofile.io/d/iwDGww | https://gofile.io/d/qY1YC1 | fba0e99975b46b0774e85df8380c24bc |
| **10.28** (code 184) | https://gofile.io/d/n1BpEz | https://gofile.io/d/0lr1IK | https://gofile.io/d/nS2LwX | 8650884ededbfa24dc677de77ce32abb |
| **10.27** (code 183) | https://gofile.io/d/dK7dIx | https://gofile.io/d/cS6UFC | https://gofile.io/d/BwKEEc | 1caf113c6ccc6768d734853d8ef62cb5 |
| **10.26** (code 182) | https://gofile.io/d/HiaKPO | https://gofile.io/d/SMq7DU | https://gofile.io/d/BIWivJ | 11c14c54c403bb1f4512f863918d5715 |
| **10.25** (code 181) | https://gofile.io/d/KWNLoW | https://gofile.io/d/h9vat8 | https://gofile.io/d/TBiGjV | 08b2f265de34469762afcb872a0746cc |
| **10.24** (code 180) | https://gofile.io/d/I3Oddb | https://gofile.io/d/BFUIcB | https://gofile.io/d/DRCf55 | 0a694017e6677d90cef66527e033612a |
| **10.23** (code 179) | https://gofile.io/d/qPvU7r | https://gofile.io/d/cxEYxs | https://gofile.io/d/6AB12x | f802a06cc902d045d5abbf073e387805 |
| **10.22** (code 178) | https://gofile.io/d/PgiF0c | https://gofile.io/d/IEaAkC | https://gofile.io/d/K0dIeG | dd6bae75ee3ab922fa10400acd8be20f |
| **10.21** (code 177) | https://gofile.io/d/EmKPwq | https://gofile.io/d/lbZXck | https://gofile.io/d/i7mvV1 | 5fce66313a7885cf146f46b147a4233f |
| 10.20 (code 176) | https://gofile.io/d/jMqPLM | https://gofile.io/d/iBE4TF | https://gofile.io/d/4HZfIl | d89daf7811cd33d1cb515bc262006a56 |
| 10.19 (code 175) | https://gofile.io/d/TXKjiE | https://gofile.io/d/u25DlN | https://gofile.io/d/isOG7I | 0e28cb95a7c41a0f7c3b0344c2e6c8cb |
| 10.18 (code 174) | https://gofile.io/d/sccBiR | https://gofile.io/d/5CDsBd | https://gofile.io/d/jWtOfj | — |
| 10.17 (code 173) | https://gofile.io/d/WIqoiq | https://gofile.io/d/AgmVyA | https://gofile.io/d/AnklCA | — |

> "Durum notu" = o sürümün PROJE-DURUM.md'si (kararlar, hata kayıtları, doğrulama listeleri).

## 📌 Politika (kullanıcı talimatı, 8 Ağu 2026)

1. Hız → sıfırlanma sonrası tek komut kurtarma + ısınmış önbellek aynası.
2. Kaybolma → artefakt, link deftere işlenmeden lokalden kaldırılmaz; defter kalıcı.
3. Hatasızlık → zincir: derleme → test (n/0/0) → öz denetim (referans taraması,
   XML doğrulama, imza+md5) → release → APK doğrulama → 3 link. Ayrıntı:
   `DEVIR/CALISMA-PROTOKOLU.md`.
