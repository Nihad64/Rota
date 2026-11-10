# Rotam kapsamlı güncelleme — çalışma kaydı

İlk aşama sürüyor; tam ürün teslimi değildir. Kaynak kararlar REQUIREMENTS EK A ve sonraki konuşma düzeltmeleridir.

- Gerçek telefondaki mevcut Room DB ve DataStore, uygulama güvenli durdurulduktan sonra özel `.toolchain/backups/before-update-2026-11-10.tar` dosyasına alındı. Kopya DB için integrity_check=ok, 2 plan doğrulandı. Yedek ve cihaz kayıtları GitHub'a gitmez.
- Gerçek AI çağrısı ilk olarak firebaseappcheck.googleapis.com kapalı olduğu için 403 verdi. Service Usage üzerinde API'nin kapalı olduğu ayrıca doğrulandı ve proje rotam-nihad64 içinde etkinleştirildi. Güvenlik enforcement kapatılmadı, billing açılmadı.
- Sonraki gerçek cihaz çağrısı Play Integrity DecodeIntegrityToken için 400 verdi; playintegrity.googleapis.com da kapalı olarak doğrulandı ve etkinleştirildi. Sonraki gerçek cihaz isteği Gemini 3.5 Flash-Lite üzerinden 11 Kasım tarihli, saatsiz okuma taslağı üretti. Onay düğmesi görüldü; kişisel telefonda test taslağı kaydedilmedi. Böylece canlı AI erişimi doğrulandı; tüm AI işlevleri tamamlandı anlamına gelmez.
- App Check 19.0.2 yerine 19.1.0 kullanımı derlendi. Tüm BoM'u 34.14'e yükseltme denemesi Kotlin metadata uyumsuzluğu nedeniyle bırakıldı; auth ve AI için mevcut uyumlu BoM korunup yalnız güvenlik SDK'sı yükseltildi. Metadata denetimi atlanmadı.
- Yeni karşılama: Google hesabı, anlaşılır veri açıklaması, yerel uygulamaya göz atma yolu. Mevcut hesabı/verisi olan kullanıcı zorunlu karşılama ile engellenmez. Tekrarlanan isim formu kaldırıldı.
- Plan ekleme ana navigasyonda tam ekran akışa taşındı; elle ekleme ve geri dönüş korunur. Takvim tarihi, konuşma geçmişindeki kesilebilir bir kullanıcı mesajı yerine sistem bağlamına verilir.
- Ortak hareket süreleri ve sistem animator ölçeğine uyum eklendi. Başarılı taslak kaydına isteğe bağlı yerel kısa onay sesi ve dokunsal geri bildirim eklendi; sessiz/titreşim modunda ses üretilmez.
- Tasarım rehberi design-system/rotam/MASTER.md içinde. Skill'in web landing-page sonucu native ürüne uygun olmadığı için kullanılmadı; üretkenlik ve Compose hareket önerileri seçildi.

Yerel derleme/lint/28 JVM testi ilk değişikliklerde geçti. Karşılama UI testi eklendi ve cihaz test paketi derlendi. Veri temizleyen cihaz testleri kişisel telefonda çalıştırılmaz; emülatör CI'ında çalıştırılacaktır. Yeni tam ürün APK'sı henüz teslim edilmedi.

## Sonraki doğrulamalar

- 3.1.0-dev (37), aynı imzayla gerçek S25 Ultra'daki 3.0.3 üzerine kuruldu. Yerel kullanıcı kayıtları korunarak tam ekran planlama ve yeni App Check SDK'sıyla canlı Gemini taslağı tekrar doğrulandı. Taslak kişisel cihazda kaydedilmedi. Karşılama mevcut kullanıcıyı zorla engellemiyor.
- Yerel geçmiş soruları için dar kapsamlı, deterministik LocalPlanQuestion eklendi: geçen/bu ay ve hafta için açıkça tamamlanma sayısı sorulan spor/okuma/iş soruları. Sınır tarihler, gelecekteki ve silinen kayıtlar, yinelenen UID ve belirsiz niyet testleri geçti. Genel AI geçmiş sorgulamasının tamamlandığı anlamına gelmez.
- Kullanıcı Frankfurt konumunu açıkça onayladı. rotam-nihad64 projesinde `(default)` Firestore veritabanı Standard/Native, europe-west3, freeTier=true ve silme koruması açık olarak oluşturuldu. Billing açılmadı. Henüz kişisel plan yüklenmedi, uygulama senkronizasyonu etkinleştirilmedi. Erişim kuralları kapalı durumda korunuyor; cihazlar arası kimlik/çakışma sözleşmesi tamamlanmadan veri aktarımı yapılmayacak.

Kalan kapsam: güvenli senkronizasyon ve hesap ayrımı, AI hafızası, tüm ekranların görsel tamamlanması, rutin duraklatma, üç widget, hatırlatma iyileştirmeleri, kapsamlı geçmiş arama/AI sorguları, ikonun saf beyaz varyantı ve iOS. Bunlar bitmiş olarak sunulmaz.
