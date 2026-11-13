# 3.0.3 düzeltmesi — 9 Kasım 2026

## Teslim edilen 3.0.2 hakkında

Telefon görüntüsü 3.0.2-dev (35) sürümünü doğruluyor; kullanıcı yanlış APK kurmamış. Bu paket kapsamlı tasarım ve işlev hedefini karşılamadı. Firebase proje/OAuth/AI Logic kurulumu, mevcut imza ile yükseltme, klavye yerleşimi ve plan durum takibi çalışmaları yapılmıştı. Bunlar çalışan Google hesabı, canlı AI, senkronizasyon veya bütün tasarımların tamamlandığı anlamına gelmez. Önceki testler gerçek hesapla giriş veya canlı AI yanıtını kapsamıyordu.

## Bu düzeltmenin kapsamı

- Eski APK başlatıcısı `com.rota.app.MainActivity`; yeni kod `com.nihad64.rotam.MainActivity` kullanıyordu. Eski bileşeni koruyan başlatıcı alias'ı yeni etkinliğe ve onaylanan ikona yönlendirilir. Güncelleme sonrası eski sabitlenmiş ikonun davranışı kullanıcının telefonunda henüz doğrulanmadı.
- Uygulama içi eski vektör yerine kullanıcının onayladığı asıl görselden üretilen transparan logo kullanılır; widget aynı kaynağı paylaşır.
- Google düğmesi artık Activity'yi ContextWrapper içinden de bulur. Etkileşimli Sign in with Google seçeneği kullanılır. Yüklenme, iptal ve hata mesajları düğmenin yanında gösterilir; hatalar sayfanın altında kaybolmaz.
- Kullanıcı önceki cevabını düzelterek güncel modele geçişi onayladı. Aktif model `gemini-3.5-flash-lite`; sessiz alternatif yok. Canlı yanıt doğrulanmadı.
- Plan ekleme önce birlikte planlama ekranını açar. Elle ekleme korunur; takvimde seçilen tarih iki yola da aktarılır. AI önerileri onaylanmadan kaydedilmez.
- Boş ana sayfa, turuncu birincil düğmeler, takvim seçimi ve gezinme vurguları yenilendi.
- Rutinler gerçek tekrar serilerini, sıradaki örneği ve düzenleme bağlantısını gösterir. Önceki hedef kayıtları ayrı bağlantıyla korunur. Duraklatma henüz eklenmedi.
- Ayarlar ana ekranı yedi menüye ayrıldı. Yedekleme, hesap ve bildirim kontrolleri alt sayfalardadır.

## Doğrulama ve sınırlar

Yerel Android 35 emülatöründe beş cihaz testi geçti: eski 2.5.6 üzerine yükseltmede sentetik plan/not/saat korunması; planlama → elle ekleme → kayıt/düzenleme; başlatıcı alias ve logo çözümlemesi; hata dönen kimlik doğrulamasında düğmenin işlemi çağırması ve görünür hata; kısmi sonuç ve kart kaydırma. Google etkileşim testi kontrollü hata kullanır; Google sunucusunda gerçek hesapla giriş testi değildir. JVM testleri, lint ve APK derlemesi geçti. APK içindeki başlatıcı görseli asıl kaynakla eşleşir.

Gerçek Google hesabı, Play Integrity kabulü ve canlı model yanıtı halen doğrulanmadı. Bulut senkronizasyonu, kalıcı AI hafızası, rutin duraklatma, bütün ekranların tasarım eşleşmesi ve iOS uygulaması tamamlanmadı. Bu paket tam sürüm olarak sunulmamalıdır.

Bu turda mevcut JDK/Gradle/SDK/emülatör önbellekleri kullanıldı; yeni geliştirme aracı veya sistem imajı indirilmedi. Önceki 6–7 GB tüketimin ölçümlü dökümü bulunmuyor; kesin trafik miktarı veya kaynağı iddia edilmiyor.
