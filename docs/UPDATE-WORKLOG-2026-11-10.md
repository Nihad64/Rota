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

## 11 Kasım — gerçek aktarım, plan detayı ve oturum özeti

- GitHub 8d305d2 / run 43028708229: verify, backend, firestore-rules başarılı; device paketinde 2 test eski Hesabım metnini beklediği için başarısız oldu. Bu beklentiler yeni hesap kartının sabit semantics etiketiyle güncellendi. Tüm CI geçmiş gibi raporlanmaz; yeni koşu beklenir.
- Firestore demo emülatöründe 8 test geçti: owner CRUD kapsamı, farklı/anonim hesap retleri, alan/tip/limit, revizyon, timestamp ve ilgisiz yollar. Kurallar Firebase CLI ile dağıtıldı ve uzak içerik tekrar okundu.
- S25 Ultra tekrar bağlandı. Kurulumdan önce yeni özel yedek alındı: 119 plan, 2 rutin, 0 hedef, integrity_check=ok. 3.1.0-dev (38) aynı imzayla güncellendi; gerçek v2→v3 geçişinden sonra eski alanlar satır satır karşılaştırıldı. 119 plan ve 2 rutin eksiksiz korundu, integrity_check=ok. Yalnız rutin örneklerinin UID'si onaylı kalıcı kimliğe normalleştirildi.
- İlk bulut aktarımı otomatik onay denetiminde durduruldu. Kullanıcı 119 plan, 2 rutin ve bunlara bağlı not/odak verisinin mevcut hesabıyla Frankfurt'a yüklenmesini açıkça onayladı. Sonrasında cihazda Planların güncel durumu görüldü. Sunucu ayrıca okundu: revision=1, PLAN=119, SERIES=2, gzip=3152 bayt. Telefonda tutulan eşitleme geçmişi ve sunucu snapshot'ı, 121 taşınabilir kayıt için içerik olarak birebir karşılaştırıldı. Kişisel içerikler GitHub'a/log çıktısına eklenmedi. Bu tek gerçek telefon doğrulamasıdır, iki fiziksel cihaz testi değildir.
- Odak oturumları mola süresini ayrı ölçer. Süreç yeniden açıldığında sayaç ve bekleyen özet korunur. Bitir eylemi yalnız süreyi kaydeder; Evet planı tamamla ayrı seçimdir. Ölçülen süreler ve mola ayrımı deterministic clock testiyle doğrulandı; başka planın açık oturumu eski bir ekran tarafından bitirilemez.
- Ayrı Plan detayı ve Ertele ekranları eklendi. Alt adım, not, hatırlatma, odak, AI ile seçili planı paylaşarak değişiklik önerme, tamamlama, silme ve geri alma eylemleri ayrıldı. Ertelemede önceki tarih/saat geçmişe kaydolur, geceyi aşan süre ve saatsiz/son-teslim biçimleri korunur. Tek örnek rutin bitişinden sonraya taşınabilir; sonraki tekrarlar için bitiş tutarlılığı ayrıca kontrol edilir.
- Widget/yakın plan niyetleri AI planlama veya detay yoluna yönlenir. Gerçek planı seçerek AI ile paylaşma, genel plan paylaşım izninden ayrı olarak açıklanır.
- 53 JVM testi, lint, uygulama ve cihaz test APK'ları yerelde geçti. Yeni sentetik entegrasyon testi, aynı Android emülatörü içinde iki ayrı Firebase SDK istemcisi ve iki ayrı Room veritabanını kullanacak: geri yükleme, çevrimdışı değişiklik, çakışma seçimi, aynı sayısal ID'li bağımsız kayıtlar ve silinmiş kayıtlar. Bu test yalnız demo-rotam-sync projesine ve CI emülatörüne bağlanır; kişisel telefonda çalıştırılmaz.

Bu aşamanın kodu 3.1.0-dev (39) olarak hazırlanır. iOS, onaylı AI hafızası/tercihlerin buluta dönüşü, üç widget, doğal dil araması, rutin duraklatma, ekranların kalan tasarım/hareket iyileştirmeleri ve saf beyaz logo varyantı henüz tamamlanmış değildir. Tam ürün teslimi değildir.

## 11 Kasım — beyaz marka işareti ve üç Android widget

- 6094f6e / GitHub run 43031812236: verify, backend, firestore-rules ve device başarılı. Android emülatöründeki 7 testin içinde iki ayrı Room veritabanı/Firebase istemcisiyle gerçek SDK taşıma testi, yenilenen plan detayı→odak→özet→düzenleme yolu ve hesap hata akışı geçti. İki fiziksel cihaz olarak raporlanmaz.
- Kullanıcının sonraki saf beyaz isteği uygulanıyor: özgün onaylı maskenin iki mevcut bileşeni (R ve turuncu elmas) deterministik olarak yerel VectorDrawable yollarına aktarılır. R #FFFFFF, elmas #FF803F; yeni logo uydurulmadı. Android normal/round foreground artık bu vektöre gider; orijinal temalı bitmap maske korunur. Kaynak PNG'ler tarihsel referans olarak korunur. In-app ve widget marka işareti aynı sıkı kırpımlı vektörün türevidir.
- scripts/prepare_brand_vectors.swift yalnız mevcut siluetten yerel vektör üretir; aynı yollar iOS SVG/1024 PNG marka kaynağı olarak dışa aktarılır. Bu, iOS uygulamasının yapıldığı veya test edildiği anlamına gelmez.
- Derlenmiş APK denetimi aktif vektör yollarının kaynakla eşleşmesini ve beyaz/turuncu renklerini doğrular. Android cihaz testi ayrıca saf beyaz pikseli, turuncu elması, normal/round eşleşmesini ve eski maskeyle en az %99.5 siluet örtüşmesini kontrol edecek.
- Küçük/Sıradaki plan, mevcut kimliği korunan orta/Bugün ve büyük/Günün rotası widget sağlayıcıları eklendi. Yerel Room verileri, günün sayıları/ilerlemesi, saatsiz işler, yanıtsız geçmiş sayısı, AI ekleme ve plan detayına geçiş bulunur. Orta/büyük boyutta hızlı tamamlama kalıcı plan UID'sini doğrular; eski sayısal ID başka kayda denk gelirse yanlış kayıt tamamlanmaz. Küçük boyutta detay açılır. Boyuta göre gösterilen satır sayısı sınırlanır.
- Widget gövdesinde Android RemoteViews'in izin verdiği yerel layout sınıfları kullanılır. Widget'lar ana ekranda kullanıcı eklemesiyle görünür; kişisel ana ekran düzenine otomatik widget eklenmedi.
- Bu aşamada 56 JVM testi, lint, app/test APK derlemeleri ve paket ikon bağlantı denetimi yerelde geçti. Yeni beyaz ikon ve üç widget için gerçek Android raster/inflate testleri sonraki CI'da çalışacak. Yeni geliştirme paketi: 3.1.0-dev (40). Henüz final ürün değildir.

## 11 Kasım — rutinlere ara verme, kart hareketi ve gerçek odak tarihleri

- cb9a40f / run 43034026224: verify, backend, firestore-rules ve device başarılı. Android'de 8 test geçti; beyaz R/turuncu elmas pikseli, orijinal siluet örtüşmesi ve üç widget'ın gerçek RemoteViews olarak açılması test edildi.
- 3.1.0-dev (40), yeni özel yedek sonrasında kişisel telefona aynı imzayla güncellendi. Uygulama içindeki saf beyaz R görüldü. Dış başlatıcı kontrolü için ekran işlemleri sırasında görüşme penceresi görüldüğünden telefonla etkileşim bırakıldı; dış ikonun son hâli henüz ayrıca gözlemlenmedi. Özel ekran görüntüsü GitHub'a eklenmez.
- Rutinlere ara verme/devam etme: bugünden itibaren bekleyen örnekler geri alınabilir metadata ile gizlenir; üretim durur ve alarmlar yeniden planlanır. Geçmiş, tamamlanan ve kısmi sonuçlar korunur. Devam etme yalnız duraklatma işareti taşıyan gelecekteki örnekleri geri açar, kullanıcının sildiği istisnaları canlandırmaz; aradaki günler başarısız kayıt olarak geri gelmez. Geçmiş dönemler ayrıca açılabilir; okul/iş için seri baskısı yoktur.
- Geçmiş-plan destesine veri kaydından bağımsız çıkış/giriş hareketi, hafif derinlik ve eşik altı kaydırmada geri dönüş eklendi. Sistem/yerel azaltılmış hareket tercihi korunur. Sol=Yapmadım, sağ=Yaptım ve düğme alternatifleri aynı; miktar elle girilir, geri alma korunur. Ana uygulamadaki Ertele düğmesi artık tam gün/saat/kapsam ekranına gider.
- Odak ölçümleri yalnız planın toplamında tutulduğunda, plan ertelenince eski süre yanlış güne taşınabiliyordu. Additive focusSegments metadata'sı artık ölçülen aralığı kalıcı kimlik, gerçek başlangıç ve süreyle kaydeder; aynı aralık tekrar işlenirse çoğalmaz. Gece yarısı doğru günlere bölünür, yeni rutin örnekleri eski odak ölçümünü miras almaz. Eski günü bilinmeyen odak toplamı korunur, grafikte tahminî güne yazılmaz.
- Plan düzenleme transaction'ı yeni odak ölçümünü korur; başka işlemde not/tarih/durum değişmişse eski editör onu sessizce ezemez. Güncel planı gözden geçirme hatası gösterilir; girilen düzenleme formu kaybolmaz.
- İlerlemem: gerçek hafta/ay sınırları, önceki/sonraki dönem, seçilen gün ayrıntısı ve plan detayına geçiş; tamamlanan/sırada/yanıtsız/kısmi/yapılmadı ayrımı; gerçek ölçüm günlerine göre odak grafiği. Genel seri/başarı puanı kaldırıldı. Gelecek ve yanıtsız kayıtlar yapılmadı sayılmaz.
- 65 JVM testi, lint, uygulama ve Android test APK derlemeleri yerelde geçti. Bu yeni aşamanın cihaz CI testi henüz çalışacak. Yeni kod 3.1.0-dev (41); kişisel telefonda hâlen (40) var. Onaylı AI hafızası/tercih geri dönüşü, birleşik doğal dil araması, akıllı takip iyileştirmeleri, kalan tasarım/erişilebilirlik ve iOS uygulaması sürüyor. Tam ürün teslimi değildir.

## 11 Kasım — tek arama alanı ve izinli geçmiş bağlamı

- bb7fa5d / run 43038595651: verify, backend, firestore-rules ve device başarılı. Yeni rutin/kart/gerçek odak tarihi/İlerlemem değişikliklerinin Android test yolu geçti.
- Tek alanda Türkçe ay/yıl, tam tarih, bu/geçen/gelecek hafta/ay, durum ve kategori araması eklendi. Örnekler: Mayıs 2024, Geçen hafta yaptıklarım, Bu hafta hangi günler çalışıyorum?, Geçen ay kaç kere spor yaptım?. Basit arama/sayı ve desteklenen iş takvimi soruları yerelde çözülür, Gemini'ye veri gönderilmez. İç metadata (outcomeHistory vb.) sözcük eşleşmesi olarak aranmaz.
- Ertelenen kayıt, eski plan tarihi üzerinden bulunabilir ve UID ile tek sayılır. Kısmi/yanıtsız kayıt tamamlanma sayısına, gelecek kayıt başarısızlığa katılmaz. Anlaşılamayan ek sözcükler varsa kesin tamamlanma sayısı uydurulmaz. Bu kurallar tüm Türkçe doğal dili çözen bir model olarak sunulmaz.
- Karmaşık değişiklik arama klavyesindeki Ara veya Soruyu gönder ile aynı AI taslak/onay akışına gider; ayrı normal/AI arama modu yoktur. Genişletilmiş plan bağlamı açık değilse uygulama içi veri açıklaması ve onay gerekir.
- Eski plan-paylaşım izninin son 1 gün/gelecek 14 gün sınırı sessizce genişletilmez. Yeni ai_context_version tercihi ancak açık onayla 2 olur; bu durumda sorunun tarih aralığından en fazla 150 başlık/kategori/tarih-saat/tamamlanma özeti seçilir. Genel AI veri izni kapatılabilir. Yerel sayım için izin gerekmez.
- Arama girdisi/filtreler ekran yeniden kurulurken korunur; sonuçlar gecikmeli ve ana UI iş parçacığı dışında hesaplanır. Yeni Android testi tarih sonucu/sayı güncellemesini, karmaşık sorunun paylaşım izni olmadan gönderilmemesini ve vazgeçince metnin korunmasını kontrol edecek.
- 69 JVM testi, lint, uygulama ve cihaz test APK derlemeleri yerelde geçti. Bu son arama testinin cihaz CI sonucu henüz yok. Kişisel telefonda (40) sürümü korunuyor; görüşme sırasında ekranına müdahale edilmedi.


## 11 Kasım — onaylı, hesapla ayrılmış AI hafızası

- 80beddb / GitHub run 43041350525: verify, backend, firestore-rules ve device başarılı. Birleşik arama/izin değişiklikleri Android emülatöründe geçti.
- AI artık yalnız hatırlama önerisi sunar. Kalıcı kayıt için kullanıcı metni inceler/düzenler ve Onayla ve hatırla der. Bir günlük istisnalar kalıcı bilgi yapılmaz; mevcut kayıt değişiklikleri eski sürüm karşılaştırmasıyla korunur. Modelin metninde hatırladım demesi kayıt oluşturmaz.
- Her kayıt mevcut Google hesabına aittir; başka hesaba geçildiğinde o hesabın hafızası görünmez ve modele gönderilmez. Gizlilik ekranında ekleme, düzenleme, unutma ve AI yanıtlarında hafıza kullanımını kapatma bulunur. En fazla 50 etkin bilgi / bilgi başına 500 karakter; geçersiz/uzun öneri otomatik kesilip kaydedilmez.
- Diğer cihazlarım için de eşitle ayrı onaydır. Yalnız cihazda tutulan bilgi bulut snapshot'ına eklenmez; AI kullanımı açıkken Gemini'ye gönderilebileceği ayrıca açıklanır. Unutulan kaydın metni silinir, içeriksiz kimlik işareti çevrimdışı cihazdan kendiliğinden canlanmayı engeller. Özel kayıtlar bulut içe aktarımında korunur.
- Room v3→v4 yalnız memories tablosunu ekler. Gerçek v3 şema fikstüründe eski not/odak alanları korundu. Yedek biçimi v3, eski v1/v2 yedekler okunur. Sıkıştırılmış eşitleme iç zarfı v2, dış Firestore sözleşmesi v1 kalır; eski uygulama desteklemediği iç biçimi okuyup yazmaya çalışmadan durur. İki cihazın güncel sürüme geçmesi gerekir.
- 75 JVM testi, lint, uygulama ve Android test APK derlemeleri yerelde geçti. Yeni hafıza onay ekranı ve iki ayrı Firebase SDK istemcisiyle hafıza aktarımı/unutma/özel kayıt koruma testleri CI'da ayrıca çalışacak; henüz geçmiş sayılmaz. Gerçek kullanıcı hafızası oluşturulmadı/yüklenmedi. Kişisel telefonda (40) sürümü korunuyor.

Bu aşama 3.1.0-dev (42). Tercihlerin geri dönüşü, akıllı takip, kalan ekran/erişilebilirlik ve iOS çalışmaları devam ediyor; tam ürün teslimi değildir.


## 11 Kasım — duruma bağlı ve sınırlı hatırlatmalar

- Yeni AI/elle oluşturulan planlarda Otomatik / Özel / Kapalı seçimi. Eski kapalı seçimler açılmaz, eski özel tek hatırlatma korunur. Otomatik bildirim takvimi plan alanlarına saat yazmaz. Ayrıntılı varsayımlar ve Android sınırları docs/REMINDER-POLICY.md içinde.
- Plan saatine/kategorisine göre hazırlık, zamanında uyarı ve giderek seyrekleşen takip. Plan başına en fazla dört otomatik aşama; genel altı/gün ve 15 dakika aralık sınırı. Sessiz saatlerde birleşen aşamalar tekilleşir. Bilinen iş/saat aralığı içindeki takip sorusu aralığın sonuna taşınır. Eski yanıtsız kayıtlar topluca bildirim üretmez.
- Bildirim eylemleri Başladım / Tamamladım / Biraz sonra. Başladım yalnız durumdur, odak süresi uydurmaz. Gerçek odak başlatma da takibi durdurur. Biraz sonra yalnız bildirimi 30 dakika erteler, planı taşımaz; üç kez sınırı ve bildirimi kapatınca o örneğin takibini durdurma var. Bu tercihler başka örneğe yayılmaz.
- Alarm/eylemler kalıcı plan UID'si ve takvim imzasıyla doğrulanır. Ertelenen kaydın eski bildirimi veya geri yüklemede tekrar kullanılan sayısal kimlik yanlış kaydı değiştiremez. Birden çok eşzamanlı alarm günlük bütçeyi aynı kilit altında paylaşır. Hata artık sessiz catch ile gizlenmez; Ayarlar/Hatırlatmalar'da hata ve yeniden deneme bulunur. Android'in kapalı kanalı/izinleri aşılmaz.
- Yerelde 82 JVM testi, lint, app ve test APK derlemeleri geçti. Senaryolar: eski tercih koruma, saatsiz plan, gece/sessiz saat, çalışma aralığı, ertelenince eski imza, içerik manipülasyonu, bildirim kapatma, günlük sınırın yeniden açılışta korunması, başlanınca iptal, saati değiştirmeyen bildirim ertelemesi. Kişisel telefona bu sürüm kurulmadı; Android cihaz CI doğrulaması sonraki commit'te yapılacak.
- Geri al işlemi sonradan ölçülmüş odak aralıklarını da koruyacak şekilde düzeltildi; yalnız toplam saniyenin korunması yeterli değildi.

Kod 3.1.0-dev (43). Tam ürün teslimi değildir; önceki kalan kapsam devam ediyor.


## 11 Kasım — AI önizlemesi, özel taslak ve ana ekran düzeni

- Hafıza commit'i 8e9939f / GitHub run 43046783804: dört iş başarılı, Android emülatöründe 10 test geçti. Bunlara açık hafıza onayı ve iki ayrı SDK/veritabanında aktarım, unutma, özel kayıt koruma dahil.
- Önceki sentetik CI ekranları gerçekten açılıp incelendi. Gecikmiş planın hem hero hem destede yinelenmesi kaldırıldı; aynı plan günün devamında tekrar çizilmez. Etkin odak oturumu önceliği korunur. Günlük ilerleme referanstaki gibi turuncu bölümlere ayrılır; sıfırda yanlış tamamlanma noktası gösterilmez. Rotam AI erişimi gün akışında bulunur.
- AI girişi, tek ana başlık, referansın kart/mesaj hiyerarşisi, yazma alanına metin yerleştiren başlangıç örnekleri, kullanıcı mesaj balonu, kısa yanıt giriş hareketi ve net önizleme kartlarıyla düzenlendi. Paylaşılan veriler açıklaması erişilebilir Paylaşım alanında; onay/taslak bilgisi saklanmadı.
- Plan önizlemesindeki Düzenle ile başlık/gün/bağımsız başlangıç/bitiş ve geceyi aşan bitiş günü kayıt yapmadan değişebilir. Geçersiz taslak pencerede kalır. Nihai kayıt ayrı Onayla ve ekle işlemidir.
- Yarım AI metni ve bekleyen istek, hesap kimliğinin karmasıyla adlandırılan özel no_backup dosyalarında cihazda tutulur. Yeniden açılışta metin geri gelir; otomatik yeniden gönderim ve otomatik kayıt yoktur. Başka hesaba/yerel misafire metin geçmez. Hesap değişince bekleyen istek iptal edilir, eski sohbet yeni isteğin geçmişine eklenmez. Hatalı dosya boş taslak sayılıp sessizce üzerine yazılmaz; saklama hatası da görünürdür. Yeniden dene, o sırada yazılmış farklı bir sonraki mesajı silmez.
- AI bağlamında tamamlanmadı alanının başarısızlık kanıtı olmadığı açıklandı; yanıtsız/kısmi/yapılmadı/ertelendi ayrımı korunur.
- 85 JVM testi, lint, app/test APK derlemeleri yerelde geçti. Taslağın yeniden açılış/hesap ayrımı/bozuk dosya korunması ve hero yinelenmesi test edildi. Android'de 1.8x metin boyutuyla önizleme düğmelerine erişim/geçersiz taslağın kayıt yapmaması testi eklendi; bu yeni UI testinin CI sonucu henüz yok.

Yeni kod (44). Tercih senkronizasyonu, kalan görsel/erişilebilirlik ve iOS hâlâ sürüyor; tam ürün teslimi değildir.


## 12 Kasım — ayrı onayla tercihlerin geri dönüşü

- Hatırlatma commit'i 0f06b6a / run 43048868646: dört CI işi başarılı. Önizleme/taslak commit'i 684fe53 için run 43229581413 başladı; yeni erişilebilirlik testi henüz bitmiş sayılmaz.
- Tercihlerim, plan eşitlemesinden ayrı onayla sekiz beyaz listeli ses/bildirim/hareket değerini taşır. Google oturumu/API anahtarı/AI paylaşımı/hafıza kullanım izni/onboarding/sistem izinleri taşınmaz. Eski plan-eşitleme onayı sessizce genişletilmez.
- Yeni cihazın farklı varsayılanları veya eşzamanlı aynı alan değişikliği iki tarafın gerçek değeriyle incelenir. DataStore atomik karşılaştırma/uygulaması yeni yerel değişikliği ezmez. Farklı alanlardaki çevrimdışı değişiklikler birleşir. Hesap/izin kontrolü ağ yazımından önce tekrar yapılır; bu kontrol mevcut plan eşitlemesine de eklendi.
- Ayrı owner-only preferences/snapshot yolu, sıkı dış alan/tip/zaman/revizyon ve 16 KiB sınırı. Plan snapshot'ı ayrı kalır. İç tercih zarfı schema=3, mevcut plan/hafıza schema=2. Kuralların CLI dry-run doğrulaması geçti. Yeni emülatör kuralları/SDK testleri CI'dan önce canlıya dağıtılmayacak. Kişisel tercih yüklenmedi.
- 87 JVM testi, lint, uygulama/test APK derlemeleri yerelde başarılı. İki ayrı DataStore ile geri dönüş, gizlilik izinlerinin korunması, çevrimdışı alan birleştirme, eski inceleme, bilinmeyen/secret alan reddi ve karşılaştırmalı yazma doğrulandı. Android'deki iki SDK istemcisine tercihler ve hesap ayrımı senaryosu eklendi; CI sonucu beklenecek. Teknik ayrıntı docs/PREFERENCE-SYNC.md içinde.

Kod (45). Kalan kapsam: yeni UI/tercih cihaz CI sonuçları, kalan ekran/hareket/bildirim gerçek cihaz doğrulaması, iOS, son veri yükseltmesi ve tek final APK. Tam ürün teslimi değildir.
