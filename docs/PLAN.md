# Rotam 3.0 — Tasarım ve güncelleme planı

Tarih: 2 Kasım 2026

Durum: Planlanan kapsam. Tamamlanmış sürüm notları değildir. Yeni logo ve ana ekran görsel yönü kullanıcı tarafından onaylandı. Kaynak ZIP üzerinde ilk statik inceleme yapıldı; derleme ve cihaz testleri yapılmadı. Bu belge uygulama kodunu değiştirmez.

İşaretler: **Karar** kullanıcının açık tercihidir. **Öneri** tasarımcı tarafından önerilen davranıştır. **Gerekli düzeltme** incelemede bulunan eksikliği veya kabul edilmiş davranışı mümkün kılacak işi belirtir. Öneriler uygulanmış ya da ayrıca onaylanmış sayılmaz.

## 1. Ürün ve görsel kimlik

**Karar:** Seçilen logo, iç boşluğu rota oluşturan yuvarlatılmış sıcak beyaz R ve küçük turuncu başlangıç işaretidir. Onaylanan ana ekran: “Bugün, senin ritmin.” başlığı, ilerleme özeti, kıvrımlı aktif plan yüzeyi ve girintisine yerleşen Başlat düğmesi, kutusuz gün akışı ve küçük Rotam AI düğmesi.

- Koyu zemin, sıcak beyaz ana metin, okunabilir gri ikincil metin ve ölçülü turuncu vurgu.
- Emoji kullanılmaz. İkonlar tutarlı kalınlık, köşe ve optik ağırlığa sahip olur.
- Oluşturma, düzenleme, takvim, hedefler, AI paneli, istatistikler, ayarlar, saat seçici, uyarılar, boş/yükleniyor/hata durumları aynı görsel dili paylaşır.
- Kıvrımlı yüzeyler ve boşluklar anlamlı yerlerde kullanılır; her alan dekoratif kesik kart haline getirilmez.
- **Öneri:** Büyük yazı ayarlarında taşmayan düzen, ekran okuyucu etiketleri, rahat dokunma alanları ve hareketi azaltma seçeneği. Durumlar yalnız renkle anlatılmaz.
- **Öneri:** Gerçek cihaz boyutunda okunurluk kontrolü; uzun plan isimleri, yoğun günler ve boş günler için ayrı tasarım kontrolü.

## 2. Başlangıç ve bitiş bağımsız, isteğe bağlı

**Karar:** Hiçbir saat zorunlu değildir; başlangıç ve bitiş birbirinden bağımsız seçilir.

| Başlangıç | Bitiş | Anlam | Örnek gösterim |
|---|---|---|---|
| Yok | Yok | Gün içindeki saatsiz iş | Bugün · Saat yok |
| 10:00 | Yok | 10:00'da başlar, bitişi belli değildir | 10:00'dan itibaren |
| Yok | 18:00 | En geç 18:00'e kadar tamamlanmalıdır | 18:00'e kadar |
| 10:00 | 11:00 | Belirli zaman aralığı | 10:00–11:00 · 1 saat |

**Önerilen tamamlayıcı kurallar:**

- Saatler ilk açılışta boş gelir; kullanıcı adına otomatik saat veya süre atanmaz. Her alan ayrı temizlenebilir.
- Kaydırmalı saat/dakika seçici yanında doğrudan yazma seçeneği olur. Tık sesi ve titreşim ayrı kapatılabilir.
- Süre ancak iki uç belliyken hesaplanır. Başlangıç + süre seçimi, kullanıcı işlemiyle bitişi belirleyebilir; bitiş zorunlu hale gelmez.
- 22:00–02:00 için “Ertesi gün 02:00” açıkça seçilir/onaylanır. Sistem sessizce gün değiştirmez. Birden fazla güne uzanan planda bitiş tarihi ayrıca görünür.
- Yalnız başlangıcı olan iş, varsayımsal bir süre dolunca gecikmiş sayılmaz. Başlangıcı geçmiş ama tamamlanmamış olarak gösterilir.
- Yalnız bitişi olan iş, son teslim zamanından önce yapılabilir; sabit süreli takvim bloğu oluşturmaz.
- Saatsiz planın süresi bilinmiyor demektir, sıfır dakika demek değildir. İstatistikler bu ayrımı korur.
- **Öneri:** Tarihsiz işler için “Sonra” listesi eklenebilir. Bu, tarihli ama saatsiz plandan ayrı tutulur.

## 3. Adım adım plan oluşturma

**Karar:** Tüm seçenekler aynı anda gösterilmez. Akış sıralıdır; geri dönünce girilen bilgiler korunur.

1. **Ne yapacaksın?** Plan adı.
2. **Kategori ve ilgili ayrıntılar:** İlgisiz alanlar görünmez; kategori isteğe bağlıdır.
3. **Ne zaman?** Tarih, bağımsız başlangıç ve bitiş; ikisi de boş bırakılabilir.
4. **Tekrar ve hatırlatma:** Varsayılan olarak tekrar yok; hatırlatma isteğe bağlı.
5. **Son bakış:** Anlaşılır kısa özet ve “Planı oluştur”. Özete dokunarak ilgili adıma dönülebilir.

**Öneri:** Başlık girildikten sonra “Ayrıntısız kaydet” kısa yolu; atlanabilen isteğe bağlı adımlar; otomatik taslak; kısa geçişler ve küçük ilerleme göstergesi. Sonraki adıma geçmek planı kaydetmez. Çift dokunma iki kayıt oluşturmaz.

## 4. Kategoriye özel alanlar

**Karar:** Seçilen işe göre uygun alanlar açılır. **Önerilen alanlar:**

| Kategori | İsteğe bağlı ayrıntılar |
|---|---|
| Kitap / Okuma | Kitap adı, sayfa hedefi veya aralığı, okuma notu |
| İş | İş yeri, vardiya, mola, hazırlık notu |
| İçerik / Video | Konu, platform, hazırlık–çekim–kurgu–yayın alt görevleri |
| Spor | Aktivite, süre veya tekrar hedefi, not |
| İbadet | Kullanıcının belirlediği faaliyet, miktar/tekrar hedefi, kişisel hatırlatma |
| Randevu | Yer, ilgili kişi için isteğe bağlı ad alanı, hazırlık/yola çıkış hatırlatması |
| Yemek | Yemek adı, hazırlık adımları, alışveriş notu |
| Öğrenme | Konu, ders/kaynak, dakika veya çalışma hedefi |
| Kişisel / Diğer | Serbest not ve alt görevler |

Alanlar kısa tutulur; hepsi zorunlu olmaz. Otomatik namaz vakti hesabı bu kategorinin kendiliğinden bir parçası sayılmaz. Özel kategori oluşturma öneridir. Kategori değişiminde eski ayrıntılar sessizce silinmez.

## 5. Plan ayrıntısı ve düzenleme

**Karar:** Seçilen tüm bilgiler tek, kaydırılabilir ve düzenlenebilir sayfada görünür. Oluşturma sihirbazına dönmek gerekmez.

- Başlık, kategori bilgileri, tarih, başlangıç/bitiş, tekrar, hatırlatmalar, alt görevler ve notlar gruplanır.
- Eksik alan için açık “Ekle” eylemi bulunur. Uzun not açılabilir; temel seçimler saklanmaz.
- **Öneri:** Planı çoğalt, tamamla, ertele, atla ve sil işlemleri; silme/tamamlama için geri alma.
- Kaydetmeden çıkışta taslak veya değişiklik uyarısı; zaman değişirse hatırlatma da güncellenir.
- Tekrarlı plan düzenlenirken “Yalnız bu / Bu ve sonrakiler / Tüm seri” seçilir.

## 6. Ana ekran ve plan durumları

**Karar:** Onaylanan ana ekran esas alınır. **Önerilen işleyiş:**

- Aktif iş varsa merkezde gösterilir; yoksa sıradaki başlangıç veya yaklaşan son teslim gösterilir. Henüz başlamamış iş “Şimdi” diye etiketlenmez.
- Saatsiz işler ayrı “Bugün içinde” bölümünde; yalnız bitişi olanlar “... kadar” etiketiyle görünür.
- “Başlat” odak takibini başlatır; zamanın gelmesi işi otomatik tamamlamaz.
- Durumlar: planlandı, devam ediyor, tamamlandı, atlandı. Gecikme zaman koşulundan hesaplanır; erteleme geçmişi ayrıca tutulur.
- Tamamlanan iş geri alınabilir. Geçen planlar kaybolmaz; uygulama açılışında toplu kontrol edilebilir.
- Boş günde anlamlı “İlk planını ekle”, gün tamamlandığında sakin bir kapanış gösterilir.
- Gün ilerlemesinin paydası açık olur; ertelenen ve atlanan işler başarı sayılmaz.

## 7. Takvim

**Kapsam:** Gün, hafta ve ay görünümü. **Öneri:** Yıl özeti ikinci aşamada.

- İki saati olan işler süreli blok; başlangıç-only işler başlangıç işareti; bitiş-only işler son teslim işareti; saatsiz işler ayrı bölüm olur.
- Süresi bilinmeyen iş bütün günü dolu göstermez; AI ve çakışma hesabı da sahte süre üretmez.
- Tarihe dokunarak plan ekleme, Bugüne dön, kategori/durum filtresi.
- **Öneri:** Sürükleyerek taşıma ve geri alma; dokunarak düzenleme alternatifi her zaman bulunur.
- Geceyi aşan plan iki günle bağlantılı gösterilir; bir plan olarak korunur.

## 8. Tekrarlayan planlar

- Her gün, belirli hafta günleri, haftalık ve özel aralıklar.
- **Öneri:** Aylık, her N gün/hafta, bitiş tarihi veya belirli tekrar sayısı; sonu olmayan seri.
- Serinin bir örneğini atlamak, taşımak veya değiştirmek diğerlerini bozmaz.
- Ayın 31'i gibi tarihler için “o ay atla / ayın son günü” tercihi açıkça belirlenir.
- **Gerekli düzeltme:** ZIP'teki 60 günlük üretim sınırı kaldırılır veya ileri tarihleri sürdüren güvenilir mekanizma eklenir. 61. günde seri sessizce bitmez.

## 9. Hatırlatmalar ve kaçırılan işler

**Kapsam:** Uygulama ekranda açık değilken hatırlatma, bildirimde Yaptım / Ertele / Atla, yeniden başlatma sonrası hatırlatmaların korunması.

- Başlangıç-only: başlangıca göre; bitiş-only: son teslim zamanına göre; iki uç: istenirse her iki uç için ayrı hatırlatma.
- Saatsiz iş: ayrı hatırlatma saati seçilebilir. Bu saat işin başlangıç saati haline gelmez; seçilmezse saatli alarm kurulmaz.
- **Öneri:** Erteleme menüsünde 10/30/60 dakika ve özel zaman. “Bildirimi ertele” ile “Planı taşı” açıkça ayrılır.
- Sessiz saatler, ses, titreşim ve çoklu hatırlatma tercihleri.
- **Gerekli düzeltme:** Plana özel hatırlatma süresi alarm kurulurken gerçekten kullanılmalıdır; ZIP'te genel süre kullanılıyor.
- Kaçırılanlar yalnız son iki güne sıkışmaz. Toplu veya tek tek değerlendirilebilir; otomatik başarısız/tamamlandı sayılmaz.
- Gerekli telefon izinleri ve pil sınırlamaları anlaşılır şekilde gösterilir. Her cihazda koşulsuz zaman garantisi verilmez; gerçek cihaz testleri yapılır.

## 10. Rotam AI / Gemini

**Karar:** Sağ altta küçük düğme; mevcut ekranın üstünde açılan panel. Ayrı zorunlu AI sayfası yok. Doğal dille birden fazla plan oluşturma ve değiştirme. Sesli yanıt okuma; Live zorunluluğu yok.

- “Yarın kitap okumayı ekle, saat belli değil”: saatsiz plan.
- “10'da kurguya başlayacağım”: yalnız başlangıç.
- “Videoyu cuma 18'e kadar bitirmeliyim”: son teslim.
- “Pazartesi ve perşembe 14–22 çalışıyorum”: tekrar önerisi.
- AI önerileri önizlemede tek tek veya toplu seçilir; kullanıcı onayından sonra kaydedilir.
- **Öneri:** Değişiklik öncesi/sonrası karşılaştırması, belirsiz tarihte kısa soru, geri alma, çakışma kontrolü, açıklanabilir erteleme önerisi.
- Süre belirsizse uygun boşluk hesabı için süre sorar veya tahminini açıkça onaya sunar.
- AI internetsizken anlaşılır durum gösterir; normal planlama kullanılmaya devam eder.
- AI'ya hangi plan verilerinin gönderileceği açık olur. Yanlış/eksik yanıtlar kaydedilmeden doğrulanır; tekrar deneme çift kayıt oluşturmaz.

## 11. Hedefler, odak ve istatistikler

- Günlük/haftalık/aylık hedefler, bağlı planlar, alt adımlar ve ilerleme.
- **Öneri:** Bir plan tamamlandığında bağlı hedef güncellenir; geri alındığında da düzelir. Sayfa gibi miktar hedefleriyle görev sayısı ayrılır.
- Odak ekranında sade görev adı, sayaç, duraklat/devam/tamamla; Pomodoro isteğe bağlı.
- Başlangıç-only işte açık uçlu kronometre; süreli işte isteğe bağlı geri sayım. Sayaç bitince iş otomatik tamamlanmaz.
- İstatistikler: tamamlanan/ertelenen/atlanan işler, hafta/ay eğilimi, kategori dağılımı, devamlılık.
- Planlanan zaman ile ölçülen çalışma zamanı ayrı gösterilir. Bitişi olmayan işe hayali süre yazılmaz.
- Yoğun başarı animasyonları yerine kısa ve kapatılabilir geri bildirim önerilir.

## 12. Arama, widget ve ayarlar

- Başlık/not/kategori araması; tarih ve durum filtreleri; geçmiş planlara erişim.
- **Öneri:** Plan şablonları, çoklu seçme, toplu taşıma ve geri alma.
- Widget: sıradaki plan, günün listesi, tamamla ve hızlı ekle; logo ve uygulamayla uyumlu sade görünüm.
- Ayarlar: bildirim, sessiz saat, ses/titreşim, animasyon, saat biçimi, AI, yedekleme ve sürüm.
- Koyu tema ilk tasarım referansıdır. Açık/sistem teması ayrıca tasarlanacak öneridir; renkleri ters çevirmek yeterli sayılmaz.
- Her ayar ve izin ekranı modern tasarım sistemine dahildir.

## 13. Veriler ve güncelleme güvenilirliği

- **Gerekli düzeltme:** Eski planları ve tercihleri yeni yapıya taşıma; taşıma öncesi yedek, tekrar çalıştırmada çift kayıt üretmeme, hata halinde eski veriyi koruma.
- Saat yokluğu gerçek boş değer olarak korunur; 00:00 ile temsil edilmez.
- Kullanıcı uygulamayı silmeye zorlanmadan güncelleme hedeflenir; mevcut paket/imzalama uyumluluğu gerçek proje ve APK üzerinden doğrulanır.
- JSON dışa/içe aktarma, sürüm bilgili yedekler, içe aktarmadan önce kaç kayıt ve hangi işlem uygulanacağına dair özet.
- **Öneri:** Yerel hatırlatma ve plan yönetimi çevrimdışı çalışır. Bulut senkronizasyonu ayrı kapsamdır; hesap, sunucu ve çakışma davranışı belirlenmeden varmış gibi sunulmaz.
- İmza dosyaları ve anahtarların mevcut durumu incelenir; gizli değerler raporlara yazılmaz. AI anahtar yönetimi kodlama öncesi belirlenir; yalnız derleme ayarına taşımak tek başına güvenlik doğrulaması sayılmaz.

## 14. Kaynak ZIP için inceleme durumu

Gerçek Kotlin kaynakları, ekranlar ve Room altyapısı bulunuyor. İlk statik inceleme tamamlandı, çalışır sürüm doğrulaması yapılmadı. Bulunanlar: eski veri aktarımı yok; tekrar üretimi 60 günle sınırlı; alarm kurulurken plana özel süre yerine genel süre kullanılıyor. Yeni dört zaman türü mevcut zorunlu başlangıç/bitiş yapısının değiştirilmesini gerektiriyor. Daha kapsamlı kod/derleme incelemesi ayrıca yapılacak.

## 15. Yapım sırası ve kabul koşulları

1. Onaylanan logo/ana ekranın tasarım ölçülerini netleştir; oluşturma ve düzenleme taslaklarını göster.
2. Diğer ekranlar ve ortak bileşenleri aynı sistemde tasarla.
3. Mevcut depoyu, ZIP'i, eski veri biçimini ve imzalamayı doğrula; dört zaman türünü ve veri geçişini kur.
4. Plan oluşturma/düzenleme, takvim, tekrar ve bildirimleri tamamla.
5. AI, hedefler, odak, istatistikler ve yedeklemeyi bağla; widget ve ek önerileri sırala.
6. Derleme, işlev, gerçek cihaz ve eski sürüm üstüne güncelleme testlerinden sonra APK hazırla.

Kabul kontrolleri:
- Dört saat kombinasyonu oluşturma, düzenleme, takvim, AI ve bildirimde aynı anlamı taşır.
- Geceyi aşan aralık, saat silme, saatsiz hatırlatma ve tarih değişimi doğru çalışır.
- Eski veriler kaybolmaz; yedekten geri dönüş denenir.
- Tekrar 60 gün sonrasında devam eder; tek örnek ve seri düzenlemeleri birbirini bozmaz.
- Plan değişikliği eski alarmı bırakmaz; tamamlanan/silinen plan bildirim üretmez.
- Yeniden başlatma, bildirim izni verilmemesi, saat dilimi değişimi ve çevrimdışı kullanım denenir.
- AI onaysız yazmaz; hata veya tekrar deneme çift kayıt oluşturmaz.
- Büyük yazı, uzun başlık, klavye, boş gün ve yoğun takvimde ekranlar okunabilir kalır.
- İmzalı APK mevcut uygulamanın üzerine yüklenerek veri koruma kontrol edilir.

Kapsam önceliği: Önce veri koruma, dört zaman türü, ana plan akışı ve tutarlı tasarım. Ardından AI, hedef/odak/istatistik ve widget. Yıl görünümü, bulut senkronizasyonu ve gelişmiş otomasyonlar ayrıca değerlendirilecek önerilerdir. Kesin teslim tarihi veya çalışır sürüm sözü bu planın parçası değildir.
