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

## 11 Kasım — devam eden eşitleme uygulaması

Önceki commit daea12a için GitHub run 42898647255: verify, backend ve device başarılı. Bu sonuç aşağıdaki yeni kodun test sonucu değildir.

- Room v3 geçişi: rutin/hedef için kalıcı UID; tekrar örnekleri rutin UID+tarih üzerinden tanınır. Sayısal yerel kimlikler buluta gönderilmez. Eski v1 yedekler okunur, yeni v2 yedek UID'leri korur. Gerçek v2 şema fikstürüyle not, odak süresi, plan/rutin/hedef bağlantıları ve kimliklerin korunması test edildi.
- Üç yönlü birleştirme: eşzamanlı farklı değişiklikler kullanıcı seçimi olmadan yazılmaz. Ağ beklerken yapılan yerel değişiklik, eski çakışma seçimi ve eksik üst kayıt güvenli durur. Rutin/hedef silme ile yeni bağlı planın çakışması tutarlı sürümlerle incelemeye açılır. Daha önce eşitlenmiş bulut kopyasının kaybolması yerel kayıtları silmez.
- Yerel kayıt, önceki bulut sürümü ve cihazlar arası kimlik sözleşmesi ayrıdır. İçe aktarmadan önce özel geri dönüş kopyası yazılır; Room karşılaştırma ve uygulaması tek transaction içinde yapılır. Bozuk eşitleme geçmişi boş kabul edilmez.
- Ayarlarda açık onay ile ilk hesaba bağlama, hesabı değiştirince veri aktarımını durdurma, durum/hata/çakışma ekranı, elle eşitleme, çevrimiçi WorkManager denemesi ve 15 dk periyodik iş eklendi. Periyodik iş Android pil/ağ koşullarına bağlıdır, kesin zaman garantisi yoktur. Bu aşama yalnız plan/not/hedef/rutin/odak verisini kapsar; cihaz ses/bildirim tercihleri taşınmaz.
- Taşıma, revizyon karşılaştırmalı tek atomik kişisel snapshot kullanır. Sıkıştırılmış üst sınır 700 KiB, açılmış veri 20 MiB. Sınır aşılırsa kayıtlar korunarak hata gösterilir; sınırsız eşitleme olarak sunulmaz.
- Firestore kuralları yalnız hesabın kendi snapshot yolunu açar, create/update tip-boyut-alan-zaman/revizyon doğrular. Özel kurallar sözdizimi doğrulamasından geçti; canlıya dağıtmadan önce GitHub demo Firestore emülatöründe kötüye kullanım testleri çalıştırılacak. Kurallar prototip olarak ele alınır. Yerelde büyük emülatör indirilmedi. Kişisel kayıtlar henüz buluta gönderilmedi.
- Ayarlar ana sayfası referanslara uygun gruplu satırlar ve Google profilinden gelen hesap kartıyla düzenlendi. Tam tasarımın bütün ekranları bitmiş değildir.

Yeni kod yerel JVM/Room testleri, lint, uygulama APK'sı ve cihaz test APK'sı ile doğrulanıyor. Bu aşama henüz kişisel telefona kurulmadı; canlı iki cihaz testi yapılmış değildir. Tam ürün teslimi değildir; önceki kalan kapsam sürmektedir.
