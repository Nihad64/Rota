# Rotam 3.1 geliştirme kaydı

Bu kayıt teknik değişiklikleri ve test sonuçlarını içerir. Tam ürün teslimi değildir. Kaynak kararlar REQUIREMENTS EK A ve sonraki kullanıcı düzeltmeleridir.

## Tamamlanan kod aşamaları

- AI bağlantısı: gerekli App Check/Play Integrity servislerinin yapılandırması düzeltildi; korumalar kapatılmadı. Firebase AI Logic, auth-only ve sınırlı kullanımlı App Check belirteciyle kullanılır. Model kullanıcı kararıyla Gemini 3.5 Flash-Lite. Kullanıcıdan API anahtarı istenmez. BoM/Kotlin uyumluluğu korunur, metadata doğrulaması atlanmaz.
- Karşılama/hesap: Google profili, yerel kullanım yolu, veri açıklaması; mevcut kullanıcı zorunlu onboarding ile engellenmez. Tekrarlanan isim formu kaldırıldı.
- Room v3: rutin/hedef kalıcı UID, kanonik tekrar örnekleri; yerel sayısal kimlikler buluta taşınmaz. v2 şema fikstürüyle not/odak/ilişki koruması test edildi. v1/v2 yedek okunur.
- Eşitleme: açık ilk hesap bağlama, hesap ayrımı, üç yönlü birleştirme, revizyon CAS, çakışma incelemesi, yerel değişikliği ezmeyen transaction, özel geri dönüş kopyası, bozuk/geriye giden bulut sürümünde güvenli duruş. Sıkıştırılmış sınır 700 KiB, açılmış veri 20 MiB. Çevrimdışı kayıt ve ağ koşullu yeniden deneme. Sınırsız depolama veya kesin periyodik zamanlama iddia edilmez.
- Plan yönetimi: ayrı detay/erteleme ekranları, not/alt adımlar, seçili planı açıklayarak AI ile düzenleme, onay, silme/geri alma. Tarih değişikliği geçmişi, saatsiz ve geceyi aşan süre, tek örnek/sonraki tekrar ayrımı korunur.
- Odak: gerçek monoton sayaç, ayrı mola, yeniden açılışta oturum/özet, bitir ile tamamlamanın ayrılması. Tarihli/kimlikli odak aralıkları gece yarısında doğru günlere bölünür; eski günü bilinmeyen süre korunur ve tahminî güne yazılmaz. Düzenleme/geri alma yeni odak ölçümünü kaybetmez.
- Marka: onaylı mevcut R/elmas silueti yerel vektöre aktarılır; R saf beyaz, elmas turuncu. Normal/round/themed bağlantıları ve derlenmiş APK kaynakları denetlenir. Yeni logo uydurulmadı. iOS marka varlıkları aynı kaynaktan türetilir.
- Widget: üç native RemoteViews boyutu; mevcut sağlayıcı kimliği korunur. Yerel veri, doğru detay/AI ekleme yolları; hızlı tamamlama UID doğrular. Desteklenen layout sınıfları gerçek Android'de açılır.
- Rutin: ara verme/devam etme, geçmiş ve kısmi sonuçları koruma, silinen istisnaları canlandırmama, eski dönemlerin ayrıca açılması. Yeni kopya/rutin eski sonuç ve odak aralıklarını miras almaz.
- Geçmiş kartları: sol/sağ sonuç, basılabilir eşdeğerler, kısmi miktarın elle girişi, erteleme için veri alma, geri alma, kontrollü çıkış/giriş ve azaltılmış hareket.
- İlerlemem: gerçek hafta/ay, gün seçimi ve plan ayrıntıları; gelecek/yanıtsız/kısmi/yapılmadı ayrımı; gerçek ölçüm günlerine göre odak grafiği. Genel başarı puanı/seri baskısı yok.
- Arama: tek alan; Türkçe tarih/dönem, durum/kategori, eski erteleme tarihi, yerel sayım/takvim soruları. İç metadata sözcük araması olmaz; belirsiz soruda kesin sayı uydurulmaz. Karmaşık istek aynı AI onay akışına gider. Eski veri paylaşım izni sessizce genişletilmez.
- Hafıza / Room v4: öneri önce incelenir ve açıkça onaylanır. Hesapla ayrılmış, görünür/düzenlenebilir/silinebilir; bir günlük istisnadan kendiliğinden kalıcı bilgi çıkarılmaz. Yerel saklama, AI kullanımı ve diğer cihazlar için eşitleme ayrıdır. Unutma içeriksiz tombstone bırakır. Özel kayıtlar import'ta korunur. Yedek v3, önceki biçimler okunur.
- Bildirim: yeni planlarda otomatik/özel/kapalı; eski tercihleri koruma. Bağlamsal aşamalar, sessiz saat, bilinen meşgul aralıkları, sınırlandırılmış takip, Başladım/Tamamladım/Biraz sonra ve bildirimi kapatmaya saygı. UID/takvim imzası, görünür kurulum hatası ve yeniden deneme. Ayrıntı: REMINDER-POLICY.md.
- AI önizleme: başlık/tarih/bağımsız saatleri kayıt yapmadan düzenleme, açık nihai onay; hesapla ayrılmış özel cihaz taslağı, otomatik tekrar gönderim yok. Hesap değişince eski konuşmanın yeni hesaba taşınması engellenir. Paylaşım açıklaması erişilebilir.
- Tercih eşitlemesi: ayrı onay, sekiz beyaz listeli ses/bildirim/hareket değeri, alan bazında birleştirme ve yeni cihazda farkları inceleme. Oturum/anahtar/AI izinleri taşınmaz. Ayrıntı: PREFERENCE-SYNC.md.

## Görsel ve etkileşim kontrolü

Sentetik Android emülatör ekranları açılıp incelendi. Hero/deste/günün akışı arasında aynı planın yinelenmesi, AI girişindeki başlık tekrarı, boş rutindeki açıklama tekrarı ve boş geçmiş kısayolu düzeltildi. İlerleme sıfırda yanlış nokta göstermez. Snackbar Rotam renklerini ve kapatma eylemini kullanır, AI yazma alanının üstüne taşınır.

Manrope variable font çevrimdışı kaynak olarak eklendi (164700 bayt). SIL OFL lisansı licenses dizinindedir. Türkçe karakterler CoreText ile doğrulandı; 400–800 ağırlıkları ve tüm başlık stilleri tanımlandı. Sistem hareket tercihleri ve isteğe bağlı kısa yerel onay sesi/haptik korunur.

## Doğrulanmış CI

| Commit | Run | Sonuç |
| --- | --- | --- |
| daea12a | 42898647255 | verify/backend/device başarılı |
| 8d305d2 | 43028708229 | verify/backend/rules başarılı; iki eski UI beklentisi başarısız, sonraki aşamada düzeltildi |
| 6094f6e | 43031812236 | dört iş başarılı; 7 Android testi |
| cb9a40f | 43034026224 | dört iş başarılı; 8 Android testi, ikon piksel/siluet ve üç widget inflate dahil |
| bb7fa5d | 43038595651 | dört iş başarılı; rutin/kart/gerçek odak tarihi akışı |
| 80beddb | 43041350525 | dört iş başarılı; birleşik arama/izin akışı |
| 8e9939f | 43046783804 | dört iş başarılı; 10 Android testi, iki ayrı SDK/Room istemcisinde hafıza dahil |
| 0f06b6a | 43048868646 | dört iş başarılı; sınırlı hatırlatma kodu |
| 684fe53 | 43229581413 | dört iş başarılı; 1.8x metin boyutunda önizleme etkileşimi |
| bcb8882 | 43231253800 | dört iş başarılı; tercih eşitlemesi SDK senaryosu ve kuralları |

(46) yerel doğrulaması: 87 JVM testi, lint, app ve test APK derlemeleri başarılı. Yeni font/snackbar görüntüsü sonraki CI'da tekrar incelenecek. Cihaz testlerinde yalnız sentetik demo Firebase kullanılır; kişisel telefonda veri temizleyen test paketi çalıştırılmaz.

## Devam eden kapsam

Yeni görsel revizyonun CI görüntüleri; kalan ekran/erişilebilirlik ve bildirim yükseltme doğrulaması; iOS uygulama/iki platform sözleşmesi; son veri korumalı yükseltme ve tek açık isimli final APK.

iOS için kullanıcı, başlangıç projesini benim oluşturmamı ve GitHub Mac ortamında test etmemi açıkça onayladı. Yerel Swift doğrulandı; büyük Xcode/Simulator indirmesi yapılmadı. FS-synchronized native app/test/widget hedefleri oluşturuluyor. Resmî Swift kurulum aracıyla güncel Firebase ve Google Sign-In paketleri bağlandı; -ObjC mevcut. Bu, iOS derlemesi/testi geçti anlamına gelmez. Ücretsiz mevcut proje korunur, Android uygulama kaydı değiştirilmez.


## 12 Kasım — Apple veri çekirdeği ve iki yönlü sözleşme

- 846fea8 / Android run 43235250025: dört iş başarılı. Yeni font/snackbar ve diğer görsel değişiklikler emülatör testlerinden geçti; son görüntüler ayrıca inceleniyor. Tercih kuralları demo CI sonrasında dağıtıldı ve uzak kurallarda ayrı yol/16 KiB sınırı/default deny tekrar doğrulandı.
- Swift çekirdeği: sınırlı gzip/JSON, taşınabilir UID, plan/rutin/hedef/hafıza doğrulama, üç yönlü çakışma, atomik özel dosya deposu, hesap bağlama, eski ağ yanıtını uygulamama ve buluta çıkmayan özel hafıza. Silme/unutma metni redakte eder. Bilinmeyen plan metadata'sı korunur.
- Android'in ürettiği sentetik ortak paket Swift'te açıldı; Swift başlık/odak alanlarını değiştirip yeniden paketledi; Android aynı kayıtları alan ve byte düzeyindeki kanonik gövdelerle doğruladı. JSON slash kaçış farkı bulunup düzeltildi; aksi hâlde semantik olarak aynı kayıt gereksiz çakışma çıkarabilirdi. İki gzip dosyası da yalnız sentetik test fikstürüdür.
- Yerel Android 88 JVM testi/lint/app-test APK derlemeleri başarılı. Swift çekirdeği derlendi ve çalışan sözleşme kontrolü UID/not/metadata/bozuk gzip/ters paket/özel hafıza/eskimiş import senaryolarını doğruladı. Yerelde XCTest modülü eksik olduğundan XCTest geçmiş denmiyor; testler GitHub Mac/Xcode işinde çalışacak. Büyük yerel Xcode indirmesi yapılmadı.
- Bu commit yalnız taşınabilir Apple çekirdeği ve CI sözleşmesidir. Native iOS ekranları, platform servisleri ve simülatör testi devam ediyor; iOS uygulaması bitmiş olarak sunulmaz.

## 12 Kasım — Native iOS uygulamasının ilk derleme aşaması

- Önceki sözleşme commit'inde Android run 43240282102 dört işte, Apple core run 43240282513 beş XCTest senaryosunda başarılı oldu.
- Android (47): yeni kullanıcı için ayrı, açık eylemle bildirim izni adımı; reddetme/erteleme kullanım yolunu kesmez. 88 JVM testi, lint, app ve test APK derlemeleri yerelde başarılı. Yeni onboarding etkileşim testi CI'a eklendi.
- Native iOS app/widget/test hedefleri, güncel Firebase/Google SPM bağlantıları, karşılama, Bugün, takvim, rutinler, AI önizleme, elle ekleme, detay/erteleme/kısmi sonuç, ölçülen odak/özet, hesap, izinler, gizlilik/hafıza, bağımsız tercih eşitlemesi, plan yedeği ve üç widget ailesi için ilk uygulama yazıldı. iOS testi yalnız sentetik demo projesi kullanır.
- Swift'in yeni saatsiz planı Android'de tekrar paketlenince bilinen nullable alanların yazım farkı bulundu; yalnız bilinen null alanlar normalize edilir, bilinmeyen alanlar korunur. İki yönlü fikstür native oluşturulmuş yeni planı da içerir.
- Rutin örnekleri kanonik UID kullanır; bitiş günü kayması korunur. AI güncellemesinde eksik saat alanı eski saatleri silemez. Onay topluca ve eski kayıt kontrolüyle uygulanır. Özel taslak son yazım sırasına göre saklanır; eski asenkron yazım yeni metni ezmez.
- Bu aşama native uygulamanın ilk CI derlemesine gönderimidir; iOS derlemesi, simülatör arayüz testi veya gerçek iPhone bağlantısı geçmiş sayılmaz. Fiziksel iOS dağıtımı ve App Attest için Apple imzalama yapılandırması henüz tamamlanmadı. Android gerçek cihaz son yükseltmesi ve son ürün denetimi de bekliyor.
