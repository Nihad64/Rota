# Güvenli Rotam AI / hesap kurulumu

Bu dosya çalışan canlı hizmet iddiası değildir. Kaynak uygulandı; canlı proje yapılandırması ve E2E doğrulama gereklidir.

- Firebase Authentication: Google sağlayıcısı, Android `com.rota.app`, mevcut imza sertifikasının SHA değerleri ve Web OAuth client ID.
- Güncel `google-services.json` dosyasını `app/` içine koy. Git tarafından dışlanmıştır. Bu dosya Gemini sırrı içermez; Gemini anahtarını içine koyma.
- App Check: Play Integrity kaydı ve callable enforcement. Yan yüklenen APK'nın doğrulanması için Play Integrity dağıtım/cihaz gereksinimlerini ayrıca doğrula. Hata halinde enforcement'ı kapatma. Emülatör geliştirmesinde kayıtlı debug provider ayrı ve yalnız debug derlemede uygulanmalı.
- Firestore: gerçek bölge ve maliyet planı incelemesi sonrası oluştur. Şimdiki kurallar bütün istemci erişimini reddeder; senkronizasyon henüz uygulanmadı.
- Functions: Node 22, `europe-west3`, `rotamAi`; Gemini anahtarı yalnız Secret Manager'da `ROTAM_GEMINI_KEY`. Eski videoda görünmüş anahtar kullanılmaz.
- `cd backend && npm ci && npm test`; ardından seçilen gerçek projeye Functions ve kurallar deploy edilir. Deploy/billing gereksinimi ve maliyet değerlendirilmeden canlıya geçilmez.
- 5 istek/dakika ve 100 istek/gün/kullanıcı; toplam 200/gün. Bunlar Rotam'ın kişisel kullanım için koruma sınırlarıdır, Gemini ücretsiz kota garantisi değildir. Başarısız upstream denemeler de sayılır. Kota UTC gününe göre sıfırlanır. Anahtar başka servislerde kullanılırsa bu uygulama sınırı onları kapsamaz.
- Sunucu, istemci prompt'una güvenmez; boyut/rol sayısı ve yetki/App Check/kota sunucuda uygulanır. Plan verisi veya anahtar loglanmaz. AI doğrudan plan yazmaz; istemci plan doğrulaması ve açık onay korunur.
- Kullanıcı plan paylaşımını ayrıca açar. Şimdiki bağlam son 1 gün + gelecek 14 gün / en fazla 150 planla sınırlıdır. Tüm geçmişe yönelik sorgulama ve izinli kalıcı hafıza henüz uygulanmadı.
- Google giriş/çıkış mevcut yerel veriyi temizlemez veya sessizce hesaba yüklemez. Hesaplar arası veri sahipliği ve taşıma onayı, sync çalışmasında ayrıca uygulanmalıdır.

## Kabul testleri

1. Eksik config: uygulama açılır, manuel plan oluşturulur, anlaşılır AI kurulum durumu görünür.
2. Google giriş/yeniden başlatma/çıkış; kullanıcı iptali; yerel veri korunması.
3. Eksik/yanlış Auth veya App Check reddedilir. Yetkisiz kullanıcı kota kayıtlarını okuyamaz/yazamaz.
4. Gerçek Gemini yanıtı → iki taslak → onay → UID üzerinden tek kayıt.
5. 404 model, 429 kota, 503 sağlayıcı, çevrimdışı, bozuk JSON ve büyük istek.
6. APK'da Gemini API anahtarı yok. Kullanıcı arayüzünde key/model alanı yok.

Resmi kaynaklar (9 Kasım 2026):
- https://firebase.google.com/docs/auth/android/google-signin
- https://firebase.google.com/docs/functions/callable
- https://firebase.google.com/docs/app-check/cloud-functions
- https://firebase.google.com/docs/projects/api-keys
- https://ai.google.dev/gemini-api/docs/rate-limits

## Model erişimi — canlı proje testi zorunlu

9 Kasım tarihindeki resmi model sayfaları Gemini 2.5 erişiminin önceki aktif kullanıma
bağlı olabildiğini ve model yaşam döngüsü/geçiş uyarılarını gösteriyor. Arama indeksiyle
canlı sayfanın tarihleri farklı olabildiğinden, eski bir model adının doğru yazılması
tek başına erişim kanıtı değildir. `gemini-2.5-flash` son onaylı karar olarak kaynakta
korundu; otomatik ücretli/güçlü model fallback'i eklenmedi. Gerçek sunucu projesinde
model listesi ve küçük bir generateContent isteği denenmeden canlı AI kabulü yapılamaz.
Bu, eski telefondaki hatanın olası bir nedenidir; cihaz/sunucu hata kaydı olmadan kesin
kök neden olarak sunulmaz.

- https://ai.google.dev/gemini-api/docs/models
- https://ai.google.dev/gemini-api/docs/deprecations
- https://firebase.google.com/docs/ai-logic/models
