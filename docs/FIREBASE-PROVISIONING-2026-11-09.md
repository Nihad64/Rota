# Rotam Firebase kurulumu — 9 Kasım 2026

Kullanıcı ayrı proje oluşturulmasını onayladı. Google oturumu Firebase aracıyla doğrulandı.

- Proje: `rotam-nihad64`, proje numarası `612038382105`; mevcut Ardiyehanem projesi kullanılmadı.
- Android app: `1:612038382105:android:7bc018e5a6872a32d91088`, paket `com.rota.app`.
- Gerçek mevcut imza SHA-1 ve SHA-256 değerleri Firebase'e kaydedildi.
- Google Authentication yapılandırması `auth` deploy ile başarıyla yayımlandı. Web OAuth istemcisi ve Android OAuth istemcisi üretilip SDK yapılandırmasına alındı.
- Firebase AI Logic / Gemini Developer API etkinleştirildi. Uygulama anahtar istemez; Gemini sağlayıcı sırrını Google'ın sunucu geçidi yönetir.
- Sunucuda `security.auth-only = true` doğrulandı; oturumsuz AI isteği kabul edilmez.
- AI Logic App Check `ENFORCED` doğrulandı.
- Play Integrity: yan yüklenen APK için resmî öneri olan Device Integrity, lisans şartı yok, tanınmayan sürüm kabulü; sertifika kaydı yapıldı. Bu ayarlar doğrulamanın kaldırılması değildir. Gerçek telefondaki attestation hâlâ test edilmelidir.
- Ücretli Blaze/faturalandırma açılmadı. Önceki özel Functions kodu bu planda canlıya alınmadı. Yeni aktif istemci yönetilen AI geçidini kullanır; eski özel günlük kota kodunun etkin olduğu iddia edilmez. Sağlayıcının ve Firebase AI Logic'in sunucu kotaları geçerlidir.
- Kullanıcı önceki seçiminin yanlışlıkla yapıldığını düzeltti ve önerilen güncel modele geçişi onayladı: `gemini-3.5-flash-lite`. Canlı hesapla model yanıtı henüz doğrulanmadı; otomatik fallback yok. Model erişim hataları kullanıcıya gösterilir.
- `google-services.json` normal Firebase istemci proje yapılandırmasıdır; içindeki Firebase API anahtarı proje tanımlayıcısıdır. Gemini sağlayıcı anahtarı, OAuth istemci sırrı veya App Check debug sırrı içermez. Bu dosya tek başına yetki vermez; yetki Auth/App Check/Firestore kurallarıyla korunur.
- Yerel derleyiciyle uyumsuz Kotlin 2.4 metadata kullanan Firebase Auth 25 nedeniyle en yeni BoM 35.1 doğrudan kullanılamadı. Metadata kontrolü kapatılmadı. Google Maven'dan doğrulanan uyumlu BoM 34.11 (Auth 24.0.1 / AI 17.10.1) ile derleme ve testler geçti.

## Açık kabul ölçütleri

- Gerçek kullanıcıyla Google giriş/çıkış/yeniden açılış ve cihaz attestation testi.
- Gerçek Gemini 2.5 yanıtı, iki taslak ve onay sonrası tekil kayıt.
- Firestore veritabanı, hesap sahipliği, offline yazma, çakışma/silme ve iki cihaz senkronizasyonu henüz kurulmuş değildir. Google Auth kurulması tek başına sync değildir.
- Kalıcı AI hafızası, hesap silme/dışa aktarma, iOS bağlantısı sonraki veri aşamasındadır.

Resmî kaynaklar:
- https://firebase.google.com/docs/ai-logic/auth-mode
- https://firebase.google.com/docs/app-check/android/play-integrity-provider
- https://firebase.google.com/docs/ai-logic/quotas
- https://firebase.google.com/docs/projects/api-keys

## Yerel doğrulama

28 birim testi, lint, APK derlemesi ve Android 35 ARM64 üzerinde 4 cihaz testi geçti. Yeni test gerçek Firebase proje/app/OAuth ayarlarının pakete girdiğini ve oturumsuz AI çağrısının model isteğinden önce reddedildiğini doğrular. Eski APK üzerine yükseltme/plan verisi, klavye/oluşturma/düzenleme, kart/geri alma ve launcher testleri de korundu. Bu testler gerçek Google kullanıcı girişinin veya gerçek Gemini yanıtının yerine geçmez. [Makine okunur sonuç](verification/firebase-3.0.2-results.json).

Tek yeni test çıktısı: `dist/Rotam-3.0.2-test.apk` (sürüm kodu 35). Firebase API anahtarı ve OAuth istemci kimliği standart halka açık istemci yapılandırmasıdır; Gemini sağlayıcı sırrı ve App Check debug sırrı pakette bulunmaz. Kullanıcıdan anahtar istenmez.

## İzleme ve onay incelemesi

AI izlemeyi kapatma yönündeki komut, proje genelinde kalıcı gözlemlenebilirlik değişikliği için açık onay olmadığı gerekçesiyle otomatik inceleme tarafından çalıştırılmadan reddedildi. Sonraki salt okunur kontrolde `monitoring.state = false` olduğu görüldü; dolayısıyla ayar değişikliği gerekmedi ve uygulanmadı. `auth-only = true`, App Check `ENFORCED` ve yalnız Gemini Developer API sağlayıcısı etkin olduğu yeniden okundu. [Doğrulanan yapılandırma](verification/firebase-security-state.json).
