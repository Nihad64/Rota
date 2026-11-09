# ROTAM — Kapsamlı Proje Dosyası

**Belge amacı:** Rotam Android uygulaması için bugüne kadar konuşmada erişilebilen gereksinimleri, kararları, tasarım yönünü, hata bildirimlerini ve kalan işleri tek yerde toplamak. Bu dosya normal sohbete aktarılıp güncellenecek; Codex uygulama aşamasında kaynak belge olarak kullanılabilir.

**Durum:** Gereksinim ve tasarım derleme aşaması. Kullanıcının son isteği doğrultusunda bu belge hazırlanırken GitHub deposunda veya uygulama kodunda değişiklik yapılmamıştır.

## 1. Kapsam ve erişim sınırı

Bu belge, mevcut konuşma bağlamında görünen proje özeti, bu sohbette yer alan kararlar, mevcut yerel proje ağacındaki `docs/PLAN.md` ve `docs/IMPLEMENTATION.md` hakkındaki önceki kayıtlar ve oluşturulmuş tasarım görselleri üzerinden hazırlandı. Eski sohbetlerin ve ekli videonun ham içeriğinin tamamı bu oturumda bağımsız olarak yeniden taranamadı. Bu nedenle erişilemeyen kısımlar doldurulmadı; eski konuşmalara ilişkin özet bilgiler ayrıca işaretlendi. Tasarım görselleri konsept/mockup niteliğindedir; çalışan ekran veya kod olarak değerlendirilmemelidir.

## 2. Ürün amacı ve tasarım ilkeleri

Rotam; kullanıcının planlarını yazı veya sesle kolayca eklediği, tekrar eden rutinlerini izlediği, takviminden gününü gördüğü ve isteğe bağlı odak oturumları kullanabildiği kişisel planlama uygulamasıdır.

- Plan ekleme hızlı olmalı: kullanıcı anlatır, AI anlamlandırır, belirsizlik varsa sorar, taslağı gösterir ve kullanıcı onaylayınca kaydeder.
- Kategori seçimi zorunlu olmamalı. Uygulama uygun türü kendisi çıkarsın; tanımadığı işi özel plan olarak ele alsın.
- Rutinler, tekrarlayan planlamanın doğal biçimi olmalı; ayrı ve karmaşık hedef kategorileri dayatılmamalı.
- Hatırlatmalar makul varsayılanlarla çalışmalı. Her plan eklerken bildirim ayarı doldurmak gerekmemeli.
- Görsel dil: çok koyu/siyaha yakın zemin, sıcak kırık beyaz tipografi, kontrollü turuncu, yuvarlak ve akışkan/kavisli kart kesitleri, marka logosundaki kıvrımlı yol ve turuncu elmas motifi. Aşırı gri, iri teknik kartlar, iç içe kontroller ve gereksiz ayar düğmeleri azaltılmalı.
- Ekranlar arasında aynı logo, renk, tipografi, ikon ve hareket hissi korunmalı. Hareket erişilebilir olmalı; animasyonları sıradan bir ayar düğmesi yapmak istenmiyor.

## 3. Karar durumu özeti

### Konuşulup tasarım yönü belirlenenler (henüz uygulandığı doğrulanmadı)

- Ana sayfa düzeni ve görsel dili; sağ üstteki harf/avatar yaklaşımının kaldırılması.
- Plan ekleme ile Rotam AI’ın tek akışta birleşmesi; metin/ses, teyit, taslak ve son onay.
- Varsayılanları gizli, plan bağlamına göre akıllı hatırlatmalar.
- Tekrarlayan hedeflerin **Rutinler** adıyla sunulması.
- Takvimde ay görünümü, seçili gün akışı; Takvim’den **İlerlemem** ekranına geçiş.
- Plan detayı, AI ile değişiklik önerisi ve erteleme akışları.
- Odaklan, oturum özeti ve ilerleme ekranlarının ayrı görevleri.
- Google ile giriş ve ilk kullanım bilgileri; senkronizasyon/yedekleme beklentisi.
- Ayarların sade bölümlere ayrılması; teknik ve gereksiz seçeneklerin azaltılması.
- Logo ve ana sayfa estetiğinin kalan ekranlarda daha belirgin kullanılması.

### Mevcut kodda bulunduğu kayıtlı olanlar (tasarım kararlarıyla karıştırılmamalı)

Önceki proje özetine göre depo `feature/rotam-3-redesign` dalında, draft PR #2 ile ilerliyordu. Android uygulama Kotlin/Jetpack Compose, Room kullanıyordu. Kodda ana sayfa, takvim, hedefler, ayarlar, istatistik, arama, AI, plan ekleme/düzenleme, odak ekranı, widget, bildirim ve yedekleme bileşenleri mevcut olarak listelenmişti. Ayrıca tekrar kuralları, plan çakışmaları, yedekleme/geri yükleme ve AI servisi gibi altyapılar vardı.

Önceki teknik durum notu: Son doğrulanmış kaynak için GitHub Actions CI başarıyla çalışmış, 16 JVM/Robolectric testi ve Android 35 emulator akışı geçmişti. Bu başarı, kullanıcının kendi telefonunda bildirdiği görsel/donanımsal sorunların giderildiği anlamına gelmiyor. Sonraki görsel tasarım kararları da henüz repoya aktarılmış değil. Bu bilgiler eski oturum anlık görüntüsüne dayanıyor; mevcut HEAD/CI bu belge hazırlanırken yeniden kontrol edilmedi.

## 4. Ekran ve özellik gereksinimleri

### 4.1 Ana sayfa — Bugün

- Başlık fikri: **“Bugün, senin ritmin.”**
- Günlük tamamlanma, aktif/öncelikli plan ve günün devamı sade hiyerarşiyle gösterilir.
- Büyük, yumuşak köşeli/kavisli plan kartı; saat ve kısa bağlam; tek belirgin eylem.
- Plan ekleme görünür ve hızlı olmalı.
- Ayrı Rotam AI yüzen düğmesi kaldırılmalı; AI plan/rutin ekleme alanına gömülmeli.
- Sağ üstte Google hesabı varmış gibi görünen “N” harfi tasarımı istenmiyor. Hesap yönetimi Ayarlar içinden olmalı.
- Alt gezinme son karar doğrultusunda: **Bugün · Takvim · Rutinler · Ayarlar**. Eski Hedefler etiketini güncelle.
- Ana sayfa önceki görseli genel olarak beğenildi; düzeni koruyup turuncu/marka dokunuşlarını dengeli kullanma isteniyor.

### 4.2 Plan ekleme + Rotam AI

Akış: **Anlat → Birlikte netleştir → Onayla ve ekle.**

- Metin yazma veya mikrofondan konuşarak dikte etme. Sürekli canlı sohbet şart değil; kullanıcı komutunu söyler, AI metin yanıtı verir. İstenirse yanıtı sesli dinleme seçeneği bulunur.
- Kullanıcı “Yarın 14–22 çalışıyorum, öncesinde süt ve yumurta alacağım” derse iki ayrı plan taslağı çıkarabilir. Verilen bilgi tekrar sorulmaz.
- AI kategori/türü otomatik çıkarır. Özel/alışılmadık iş özel plan olur. Kullanıcıdan zorunlu kategori seçimi istenmez.
- Sadece ilgili ek alanlar gösterilir. Başlangıç ve bitiş saatleri bağımsız isteğe bağlıdır. Belirsiz metinden tarih, saat, süre veya rutin uydurulmaz.
- Birden çok taslak düzenlenebilir; kullanıcı metin, ses veya kart ayarından düzeltme yapabilir.
- Kayıt öncesi AI plan özetini açıkça gösterir ve kullanıcıdan onay ister. Onay yoksa kalıcı kaydetme yapılmaz; yinelenen kayıt olmamalı.
- AI çalışmazsa elle plan ekleme yolu korunur; hata kullanıcıya anlaşılır anlatılır.
- Klavye açıkken giriş ve yazılan metin görünür kalmalı, sayfa zıplamamalı.
- Eski kategori ızgarası ve her yerde tekrarlanan hatırlatma bilgileri kaldırılmalı.

### 4.3 Hatırlatmalar

- Alarm kurmak gibi kolay varsayılan davranış; plan ekleme formunda hatırlatma süresi seçmeye mecbur bırakma.
- Bağlama uygun, makul zamanlama: gerekiyorsa öncesinde, zamanı geldiğinde, yanıt/başlama yoksa sonrasında takip. “1 saat ve 5 dakika” geçmişte örnek olarak söylenmişti; sabit evrensel kural değildir.
- Saat içermeyen planlara sahte saat atama. Gün içi işler için uygun takip davranışı tasarlanmalı.
- Başlatıldı/tamamlandı durumu ilgili takipleri durdurmalı. Başlangıç zamanı geçti ile son tarih aşıldı ayrıştırılmalı.
- Duolingo’dan ilham alan ısrarcı ama eğlenceli bildirim tonu düşünülebilir; sonsuz spam değil, aralığı açılan ve sınırlandırılmış hatırlatma. E-posta bu kapsamda istenmiyor.
- Bildirim eylemleri örnekleri: **Başladım · Tamamladım · Biraz sonra**.
- İzin yalnız gerektiğinde istenir; Ayarlar’daki teknik izin kartları yerine açıkça eksikse doğrudan sistem iznine yönlendir.
- Varsayılanlar genel deneyimde görünmez; kullanıcı ayrıntı istediğinde plan ayarından değiştirir.

### 4.4 Rutinler (önceki adı Hedefler)

- Spor, okul, iş, okuma gibi düzenli tekrarlar. AI destekli yazı/ses ile oluşturulur; kategori seçtirmez.
- Rutinlerin günü gelen örnekleri Bugün ve Takvim’de görünür; ayrı ikinci bildirim üretip çakışma yaratmaz.
- Kullanıcı geçmişi korunur. Tekrarlı plan değişirken kapsam sorulur: **Yalnız bu gün / Bu ve sonraki tekrarlar** (tasarım alternatifinde “Sonraki tekrarlar”). Bir günlük iptal bütün seriyi bozmaz.
- Rutin duraklatılabilir; duraklatma geçmişi silmez.
- İlerleme rutinin kendi yapısına göre sunulur (ör. spor 2/3, okuma 5/7); iş/okul gibi rutinlere zoraki seri/başarı puanı yüklenmez.
- Rutin kartında plan adı, tekrar gün/saat, sıradaki zaman ve gerekiyorsa sade ilerleme.

### 4.5 Takvim

- Ay görünümü, yatay ay geçişi ve **Bugün** kısayolu. Günlerde küçük plan işaretleri; seçili tarih turuncu, bugün ince çerçeve.
- Seçili günün akışı altta görünür. Başlangıç/bitişi olan bloklar aralığı gösterir; yalnız başlangıç varsa bitiş uydurulmaz.
- Saati olmayan işler ayrı **Gün içinde** bölümünde gösterilir.
- Seçili güne plan ekleme `+` düğmesi ortak AI Plan ekle akışını o tarih ile açar.
- Rutin örnekleri ilgili günlerde otomatik görünür. Çakışmalar işaretlenir ama kaydetme zorla engellenmez.
- Sağ üstte **İlerlemem** düğmesi; istatistikler Ayarlar’da olmamalı.

### 4.6 Plan detayı / düzenleme / erteleme

- Plan adı, tarih-saat, varsa yapılacaklar listesi ve notlar. Boş alanlar gereksiz yer kaplamaz.
- Her plan için “Başlat” zorunlu değil. **Tamamladım** ayrı ve görünür.
- **Ertele** hızlı seçenekleri ve tarih/saat belirleme sunar; süre korunuyorsa bitiş hesaplanır, belirsiz alanlar sessizce uydurulmaz.
- AI’ye yazıp/söyleyerek düzenleme; öneri/taslak gösterilir, kullanıcı onayıyla uygulanır.
- Hatırlatma ayrıntıları plan içinde ikincil satırdır.
- Çoğalt/sil gibi işlemler üç nokta menüsünde. Silme onaylanır ve mümkünse geri alınabilir.
- Rutin örneği değişiklik kapsamı açıkça seçilir.
- Odaklan isteğe bağlıdır; iş/okul gibi her plan sayaç gerektirmez.

### 4.7 Odaklan

- Plan adı/iş, büyük geçen süre sayacı. Varsayılan süre serbest sayım; süre belirlemek isteğe bağlıdır.
- Aktif kullanımda **Mola ver** ve **Bitir**. Mola süresi odak süresine eklenmez; devam kaldığı yerden sürer.
- Kilit ekranı/uygulamadan çıkışta oturum sürmeli, geri gelince kaybolmamalı.
- Süre tamamlanması planı otomatik tamamlamaz. Oturum bitişinde özet gösterilir ve **Evet, planı tamamla / Sonra devam edeceğim** sorulur.
- Kısa ses/titreşim düşünülebilir. Odak oturumu verisi gerçek kaydedilen süre olmalı.

### 4.8 İlerlemem

- Takvimden açılır. Hafta/Ay görünümü ve dönem seçimi.
- Tamamlanan/planlanan/geçmiş işler; rutinlerin bireysel devamlılığı; odak süresi ve günlere dağılım.
- Gelecek planı başarısız sayılmaz; ertelenmiş iş iki kere sayılmaz; mola odak süresine eklenmez.
- Veri yokken puan uydurmak yerine ilk eylemi davet eden boş durum.
- Genel başarı puanı ve okul/iş rutinlerine seri baskısı yok.
- Güne dokununca günlük ayrıntı açılır.

### 4.9 Google girişi ve ilk kurulum

- Google ile giriş; hesapla ilişkilendirilmiş temel profil. Giriş tek başına yedek/senkronizasyon değildir; gerçek veri saklama/senkronizasyon servisi ayrıca tasarlanıp uygulanmalı.
- Google’dan gelen ad-soyad gösterilip düzenlenebilir. Kullanıcıya hitap adı sorulabilir. Yaş isteğe bağlı; gerekmeyen kişisel veri zorunlu toplanmamalı.
- Hatırlatma izni kısa açıklamayla istenir. **Şimdi değil** seçeneği. Mikrofon izni onboarding’de değil, ilk sesli plan girişinde.
- İlk kurulum sonunda boş ana sayfada **İlk planını birlikte oluşturalım** çağrısı.
- Oturum tekrar açıldığında onboarding yeniden çıkmamalı. Google hesabı geçişi, çıkış ve hesap kurtarma halleri tanımlanmalı.

### 4.10 Ayarlar ve alt sayfaları

Ana ayarlar listesi tasarlanmış: **Hesabım; Hatırlatmalar; AI bağlantısı; Verilerim; Gizlilik; Rotam hakkında; Sorun bildir.**

- Hesabım: Google bağlantısı, profil ve çıkış; ismi tekrar tekrar isteme yok.
- Hatırlatmalar: ses/titreşim/takip davranışı; Android izin durumunu teknik jargonla tekrar eden kartlara bölme.
- AI bağlantısı: gerekliyse API anahtarı güvenli alanı, kaydetme/bağlantı doğrulama ve bağlantıyı dene. Model adını kullanıcıya düzenletme yok. Anahtar yazma/kaydetme mevcut raporda sorunlu; test edilmesi gerekir.
- Verilerim: senkronizasyon, dışa aktarma, geri yükleme. Büyük “tüm planları sıfırla” düğmesi kaldırılmalı; geri yükleme uyarısı açık olmalı.
- Gizlilik: AI ile paylaşım ve hatırlanan tercihleri görme/düzenleme.
- Rotam hakkında: sürüm ve yenilikler; sorun bildir. Hata raporu kullanıcı onayı olmadan gönderilmez.
- UI tıklama sesi/animasyon ve sessiz saatler gibi gereksiz anahtarlar öne çıkarılmamalı. Sistem erişilebilirlik tercihleri gözetilmeli.

### 4.11 Arama, bildirim, widget ve ortak durumlar

- Arama: plan/rutin metni, tarih ve durum üzerinden hızlı arama; “Tümü / Planlar / Rutinler” gibi filtreler ikincil.
- Bildirim: tek bildirim kanalı tasarım dili, Başladım/Biraz sonra gibi aksiyon, e-posta yok.
- Ana ekran widget’ı: bugünün rotası, yakın planlar, saat olmayan iş ve plan ekleme/tamamlandı eylemi; marka diliyle tutarlı.
- Boş durum: ilk planı eklemeye yönlendir.
- AI/bağlantı hatası: yazılan metin kaybolmasın; yeniden dene veya elle devam et.
- Yükleniyor: “Plan hazırlanıyor…” gibi anlaşılır durum; kilitlenme hissi yok.
- Tarih/saat seçici, plan silme onayı, geri alma bildirimi ve tekrarlı plan kapsam pencereleri aynı görsel dilde olmalı.

## 5. Kullanıcının video ile bildirdiği mevcut sorunlar

Video dosyası önceki oturumda bu yerel konumda kayıtlıydı: `upload/Screen_Recording_20261103_081216_Rotam.mp4`. Video ve ses bu belge hazırlanırken yeniden incelenemedi; aşağıdaki maddeler kullanıcının yazılı aktarımına dayanır.

- İndirme sayfasında iki farklı paket (`device check`, `review build`) anlaşılmıyor; ZIP içinden çıkan dosyalar kafa karıştırıyor, APK net sunulmalı.
- Dışarıdan görünen uygulama/launcher logosu eski/yanlış.
- Bazen Ayarlar sekmesinden ana sayfaya geçiş tutarsız.
- Rotam AI’da mikrofon ve yanıtı sesli dinleme eski kullanımda vardı, yeni sürümde yok; AI “model kullanılamıyor” hatası veriyor.
- Sohbet çöp kutusu gereksiz bulunuyor.
- Plan eklerken klavye ekranı kaydırıyor; yazı ve üst bölüm görünmüyor.
- Kategori/saat alanları karışık ve hiyerarşi mantıksız; dokunsal geri bildirim/ses eksik.
- “Plan oluşturulamadı” hatası.
- Görsel genel olarak fazla siyah/gri/beyaz; kontrollü turuncu daha belirgin olmalı.
- Sürekli ad sorma/profil satırları gereksiz; isim onboarding ve hesapta ele alınmalı.
- Bildirim izni kontrolü çalışmıyor; Android izin kartları ve “kesin bildirim zamanı” gibi teknik metinler anlaşılmaz.
- Hatırlatmalar tek sabit ön süre olmamalı; bir plan farklı zamanlarda hatırlatılabilmeli, atlanan planın takip mantığı olmalı.
- Sessiz saatler, UI sesleri, titreşim ve animasyon için gereksiz anahtarlar.
- Gemini API key kaydetme sorunu şüphesi; kullanıcı kaydedince alan boşalıyor, saklandığı doğrulanamıyor. Videoda API anahtarı açıkça görünmüş; anahtar gizli kabul edilmeli, yeniden kullanılmamalı veya belgeye kopyalanmamalı.
- “Gemini 2.5 flash” model adı kullanıcıya gereksiz ve düzenlenebilir gösterilmemeli.
- “Tüm planları sıfırla” tehlikeli ve gereksiz.
- “Planı başlat/tamamla” davranışı anlaşılmıyor; plan tamamlama, odak başlatma ve rutin örneği durumu net ayrılmalı.

## 6. Tasarım çıktıları

Bütün görseller tasarım konseptidir, kod değildir. Tek tek dosyalar Library’de ayrıca kayıtlıdır.

| Görsel | İçerik | Library dosyası |
|---|---|---|
| Ana sayfa | Bugün, senin ritmin; arama ve düzenlenmiş ana sayfa | `Rotam ana ekranı: arama simgesi.png` — `libfile_b1e6a905015081919ffff3dbdeeaa524` |
| Önceki ana sayfa | İlk günlük plan/odak yönü | `Rotam günlük plan ve odak ekranı.png` — `libfile_abc2cd1e155c8191b7f2a8d2c2fe80fa` |
| Plan ekleme, önceki varyant | AI ile çoklu plan onaylama | `Rotam’da iki planı onayla.png` — `libfile_9853ba3289548191bfbdc8679dc00a5a` |
| Plan ekleme, sade varyant | İki taslağı sade onaylama, ayarların ikincil olması | `Sade plan onay ekranı.png` — `libfile_12196b61d0fc81919d38b28fba73fa08` |
| Rutinler | Okul/spor/okuma rutin listesi | `Rutinler ekranı: Gününü düzenle.png` — `libfile_b1a910e90614819196f9298597a3f555` |
| Ayarlar ana ekranı | Hesap, tercihler, veriler, uygulama bölümleri | `Rotam ayarlar ekranı.png` — `libfile_69c14b0e1df881919f565055c54648a0` |
| Takvim | Ay görünümü, seçili gün akışı, İlerlemem erişimi | `Kasım 2026 Takvim ve Planlar.png` — `libfile_f34f4edd697c8191bba1713e43588cce` |
| Plan detayı/düzenleme/erteleme | Üç aşamalı plan yönetimi panosu | `ROTAM: Plan, AI düzenleme ve erteleme.png` — `libfile_e8d52a7efe4881918be5749ed5575f35` |
| Odak/özet/ilerleme ilk varyant | Üç ekran tek panoda | `ROTAM: Odak, özet ve ilerleme.png` — `libfile_6c136b7c430c819187fe1857ddbcc9ed` |
| Odak/özet/ilerleme, marka estetiği güçlendirilmiş | Kavisli kart, elmas, büyük başlıklar | `Üç ekranlı odaklanma ve ilerleme panosu.png` — `libfile_54cac525d0488191ac96a1dd15bc41ae` |
| Google giriş/ilk kurulum | Karşılama, bilgiler, izin, ilk plan | `ROTAM: Dört ekranlı karşılama konsepti.png` — `libfile_620772facb3c8191a981ba5925d0f857` |
| Kalan ekranların toplu panosu | Hesap/hatırlatma/AI/veriler/gizlilik/yardım/arama/rutin/ses/bildirim-widget/boş-hata/onaylar | `ROTAM Android arayüzleri: 12 panelli tasarım.png` — `libfile_ec71d2cb323c8191980c050cac6a5d6f` |
| Marka işareti | Seçilen kıvrımlı R ve turuncu elmas | `Karar kesitiyle ROTAM monogramı.png` — `libfile_85c6cb65f8b48191a3debe8ce5894960` |

### Görsellerle ilgili değerlendirme

- Son 12 panelli görsel, kalan kapsamı tek yerde tanıtıyor; küçük panel formatı nedeniyle üretim detaylarını ve metin okunurluğunu tek tek ekran mockupları kadar ince değerlendiremez. Özellikle hassas ekranlarda ayrı mockup gerekebilir.
- İlerlemem/istatistik örnek rakamları temsili. Ürün verisi gibi kullanılmamalı.
- İlk kurulum mockup’ındaki adlar örnek profil değeridir.
- Tasarımda kavisli kart estetiği ağırlaştırılmamalı: her satırı büyük şekilli karta çevirmek yerine öne çıkan özet/aktif içerikte kullanılmalı.

## 7. Açık kararlar ve eksik detaylar

- Google giriş sağlayıcısı ve bulut verisi/senkronizasyon altyapısı henüz teknik olarak seçilmedi. Google kimlik doğrulaması tek başına veri yedeklemesi sağlamaz.
- API anahtarı kullanımı, güvenli saklama, sağlayıcı ve bağlantı başarısızlığında deneyim ürün/teknik karara bağlanmalı.
- Otomatik hatırlatmanın kesin takvimi, tekrar deneme sınırı, bağlama göre algoritma ve kullanıcıdan öğrenme biçimi belirlenmeli.
- İlk girişte hangi profil verisinin gerçekten gerektiği (özellikle yaş) ürün gizliliği açısından tekrar gözden geçirilmeli.
- Bildirim ses/titreşim varsayılanları Android sürümleri ve izinleriyle uyumlu teknik tasarıma çevrilmeli.
- Widget kapsamı, desteklenen boyutlar ve hızlı eylemler kesinleştirilmeli.
- Arama durumu filtreleri ve tamamlanmış/geçmiş planların görünümü netleştirilmeli.
- Hata kaydı/raporunda kullanıcı verisinin ne zaman gönderileceği, önizleme ve izin akışı belirlenmeli.
- APK dağıtımı tek ve anlaşılır artefakt olarak düzenlenmeli; release/review ve device-check çıktılarının kullanıcıya nasıl sunulacağı karara bağlanmalı.
- Takvim mockup’ındaki örnek hafta/tarihler konsept verisidir; gerçek zaman ve yerelleştirme ile test edilmeli.
- Eski ekranlar ve güncel kararlar arasında tasarımda farklar olabilir; uygulamaya başlamadan önce bu dosya normal sohbette tamamlanıp sürümlenmelidir.

## 8. Sonraki çalışma planı

1. Bu dosyayı normal ChatGPT sohbetine aktar.
2. Erişilemeyen güncelleme notlarını, kullanıcıdan gelecek düzeltmeleri ve eksik ürün kararlarını burada tamamla.
3. Görselleri tek tek referans alarak nihai ekran ve durum listesini dondur.
4. Bu belgeyi gereksinim kaynağı olarak güncelle; hangi maddelerin uygulanacağı ve test kabul ölçütleri açık olsun.
5. Ancak kullanıcı Codex uygulama aşamasına geçirdiğinde repo üzerinde çalış: mevcut Android/CI düzenini incele, önce bildirilen kritik bug’ları doğrula, sonra tasarım ve özellikleri uygula.
6. Her ana akışı gerçek emülatör ve kullanıcının cihazında doğrula; review build ile tek APK, net sürüm/kurulum yönergesi sağla.

## 9. Önceki teknik proje notları (tarihsel anlık görüntü)

Önceki notlara göre depo `Nihad64/Rota`, dal `feature/rotam-3-redesign`, draft PR #2 idi. Uygulama Kotlin Compose/Room ve `com.rota.app` paket adıyla, eski Java sürümünden yükseltme uyumluluğunu hedefliyordu. Eski imzalama materyalinin repo geçmişinde bulunması gibi güvenlik konusu tespit edilmiş ancak çözümü/anahtar rotasyonu karara bağlanmamıştı. Bu bilgiler yalnızca eski bağlam özetidir; bu çalışma sırasında GitHub açılmadı ve güncel durum teyit edilmedi. Gizli anahtar, parola veya API anahtarı bu belgeye alınmamıştır.

---

**Belge sonu — karar, tasarım ve uygulama durumu birbirine karıştırılmamalı. Bu dosyadaki tasarımlar uygulanmış özellik anlamına gelmez.**

---

# EK A — 9 KASIM 2026 NORMAL SOHBETİNDE KESİNLEŞTİRİLEN YENİ KARARLAR

**Bu ek, yukarıdaki önceki proje belgesinin ayrılmaz parçasıdır.** Çelişki halinde bu ekteki açıkça onaylanmış yeni ürün kararları önceki taslakları geçersiz kılar. Ancak teknik çözüm önerileri kullanıcı tarafından onaylanmış kesin teknoloji seçimi sayılmaz. Ürün gereksinimleri ile uygulanmış kod birbirine karıştırılmamalıdır. 9 Kasım oturumunda **kod, GitHub, PR veya CI değiştirilmedi**.

## A.1 Çalışma ilkeleri ve kapsam

- İlk hedef: Kullanıcının **kendi kişisel kullanımına** yönelik sorunsuz Rotam; gelecekte başkalarına sunulduğunda anlaşılır, yalın ilk kurulum ve sürdürülebilir mimari.
- Android sürümü korunacak ve **iOS desteği** de hedefe eklenecek. Mac üzerinde Android Emulator ile Android testi yapılabilir; iOS sürümünü test etmek amacıyla Android uygulamasını iOS'a dönüştürmek zorunlu değildir. iOS testinde Xcode/iOS Simulator kullanılabilir. Hedef iki platformdur, tek platformu diğeri için bozmamak gerekir.
- Mevcut önceki teknoloji: Kotlin, Jetpack Compose ve Room. **Kotlin Multiplatform + Compose Multiplatform** geçişi bir **inceleme önerisi**, kesin ve sorgusuz geçiş talimatı değildir. Codex önce repo/bağımlılık/cihaz uyumluluğunu, veri geçişini ve riskleri incelemeli, uygun bir aşamalı plan sunmalıdır; mevcut Android işlevselliği ve kullanıcı verileri korunmalıdır.
- **Öncelik:** gerçek kullanıcıyı etkileyen iki kritik hata (launcher ikonu, AI erişimi/plan oluşturamama), ardından sağlam temel akış, veri/senkronizasyon, onaylı ekran tasarımları ve iki platform testi.
- Tasarım görselleri **referans/mockup**, kaynak kod değil. Bazıları eski/alternatif varyant. Tek bir görselden işlev uydurma; yazılı son karar öncelikli. Özellikle dosyada geçen bazı eski AI anahtarı/AI yerleşimi davranışları yeni kararlarla değiştirilmiştir.

## A.2 Google hesabı, kalıcı veriler ve senkronizasyon — ONAYLANDI

1. Google hesabıyla giriş; temel profil (ad/soyad, tercih edilen hitap adı, kullanıcı isteğiyle yaş gibi gerekli bilgiler) tutulur. Gereksiz kişisel veri zorunlu alınmaz.
2. Uygulamayı silip yeniden kurduğunda, telefon değiştirdiğinde veya Android'den iOS'a geçtiğinde **aynı hesapla planlar, rutinler, geçmiş, tercihler, ilerleme ve AI'nin onaylı kişisel hafızası geri gelir**.
3. İki cihaz aynı hesabı kullanırsa değişiklikler senkronize edilir. Çevrimdışı kullanımda yerel veriler erişilebilir kalır; internete dönünce değişiklikler güvenilir biçimde eşitlenir. Çakışma, silme, tekrar planların çoğaltılmaması gibi durumlar teknik tasarımda ele alınmalıdır.
4. Kullanıcıya tekrar tekrar ad sorulmaz; onboarding her girişte çıkmaz. Google oturumu tek başına depolama değildir. Bulut veritabanı ve güvenli senkronizasyon gerekir.
5. **Firebase Authentication + Firestore** konuşulan makul teknik öneridir; nihai sağlayıcı/şema kod analizi ve gizlilik değerlendirmesi sonrası kesinleştirilecektir. Maliyet ve erişim kuralları gözden geçirilmeli.

## A.3 Rotam AI — uygulama geneline entegre, hafızalı, ücretsiz kullanım hedefi — ONAYLANDI

1. Rotam AI yalnızca bağımsız bir sohbet kutusu değil, **uygulamanın plan, rutin, takvim, geçmiş, istatistik ve kullanıcı tercihlerini izinli şekilde sorgulayıp yorumlayabilen bir yardımcıdır**. Gerekli bağlamı uygulama servislerinden çekebilmeli; her şeyi tek prompt'a körlemesine doldurmamalı.
2. Kullanıcı istediği anda **yazarak veya sesli konuşarak** plan oluşturabilir/düzenleyebilir, takvim/alışkanlık soruları sorabilir. Eski AI deneyimindeki mikrofon ve **AI yanıtını isteğe bağlı sesli dinleme** özelliği korunmalı/geri getirilmeli. Ancak aşağıdaki kaydırmalı geçmiş-plan kartlarına ekstra mikrofon/ses düğmesi konmayacak.
3. Kullanıcı örneğin “Geçen ay kaç kere spor yaptım?”, “Bu hafta hangi günler çalışıyorum?” veya “Perşembeki sporu işimle çakışmayacak şekilde cumaya taşı” dediğinde uygulama kayıtlarına dayanarak doğru yanıt/işlem taslağı üretmeli. Plan/rutinleri **kullanıcı onayı olmadan kalıcı değiştirmemeli veya silmemeli**.
4. Kullanıcının tekrarlayan saatleri, ulaşım süreleri, tercihleri ve açıkça hatırlatılmasını istediği bilgileri kişisel hafızasında saklayabilir. **Bir günlük istisnayı kalıcı rutin sanmamalı**. Gerekirse “Bunu sonraki seferler için de hatırlayayım mı?” diye sormalı. Hafıza kullanıcıya görünür/düzenlenebilir/silinebilir olmalıdır.
5. Eksik bilgi varsa **yalnız gerçekten gerekli olanı sor**. Saat vermeden plan kaydetmek mümkünse kullanıcıyı gereksiz soruyla yorma. Bildiği bilgileri **öneri/taslak** olarak göstermeli ve onay almalı; asla tahminini kesin gerçek diye kaydetmemeli.
6. Başlangıç için **Gemini 2.5 Flash ile devam etme kararı** alındı; daha güçlü/masraflı model şart değil. Modelin gerçekten sınırsız olduğu **iddia edilmemeli**. Ücretsiz günlük/dakikalık API kotaları uygulanabilir; kota ve sağlayıcı politikaları zamanla değişebilir, Codex geliştirmede güncel durumu ve gerçek kota hesabını kontrol etmelidir.
7. **Kullanıcıdan API anahtarı istenmeyecek**: ne ilk girişte ne de sıradan ayarlar bölümünde. Rotam AI mümkün olduğunca hazır çalışacak. Kendi anahtarını girme ekranı ilk sürümün ürün gereksinimi değil; mimari ileride sağlayıcı değişimine izin verebilir. Eski belgede yazan `AI bağlantısı / API key alanı` yeni karara göre **gizlenmeli/kaldırılmalı veya yalnız geliştirici düzeyinde tutulmalı**; normal kullanıcıya gösterilmemeli.
8. **Güvenlik açısından API anahtarını APK/IPA kaynaklarına veya istemci yapılandırmasına gömmeyin.** Sunucu tarafında yönetim/korumalı hizmet, kullanıcı bazlı kimlik doğrulama, kötüye kullanım koruması ve kota kontrolü tasarlayın. Tamamen ücretsiz ve sınırsız kullanım garantisi verilmesin. İlk aşama kişisel kullanım; herkese açmadan önce maliyet/gizlilik/ölçek yeniden ele alınmalı.
9. AI basit tarih/isim eşlemesi veya kayıt işlemlerinde gereksiz pahalı model çağrıları yapmasın; uygun işlemler deterministik kodla halledilsin. Ancak kullanıcının doğal diliyle tam uygulama bağlamında konuşma deneyimi korunmalı.
10. Google hesabı / bulut / AI üçüncü taraflarına gönderilecek kişisel veriler için gerekli açıklama, onay, en az veri ve erişim denetimi sağlanmalı. AI erişimi bütün telefon dosyalarına sınırsız erişim **değildir**; ilgili uygulama verileriyle sınırlıdır.

## A.4 Akıllı hatırlatmalar — ONAYLANDI

- Plan saatinden önce, zamanı geldiğinde ve sonrasında **bağlama göre** hatırlatmalar olabilir. Sabit “her zaman 1 saat önce” kuralı yok.
- AI önceden bildiği rutinlerden öneri yapar. Örnek: kullanıcı genellikle **14:00–22:00** çalışıyor ve **13:00'te evden çıkıyorsa**, “Yarın işe gideceğim” ifadesinden bu saatleri **önerebilir**, ancak **onaylamadan kesinleştiremez**. Kullanıcı “Hayır, yarın 06:00–14:00 çalışıyorum” derse yalnızca **o güne** uygular, kalıcı çalışma düzenini sessizce değiştirmez.
- Kullanıcı bir saati söylememişse gerekmiyorsa zorunlu sorma. Başlangıç, bitiş bağımsız opsiyoneldir; sahte bitiş veya süre icat edilmez.
- Cevaplamadığı planın durumunu tekrar sorabilir. “Başladım / Tamamladım / Biraz sonra” gibi bildirim eylemleri. Plan başlatıldı/tamamlandı ise ilgili hatırlatmalar durmalı; ertelemede eski takipler uygun biçimde yenilenmeli.
- **Duolingo'dan esinlenen ısrarcı ama sınırlı** bildirim: makul ilk uyarı, sonra ikinci hatırlatma, giderek seyrekleşme; kullanıcı sessize aldığında veya reddettiğinde buna saygı. **E-posta yok.** Kesin zamanlama, sınırlar ve sistem izinleri Codex tarafından uygulanabilir varsayımlarla belgelenmeli ve test edilmeli.
- Gecikmiş durumlar widget ve uygulamanın giriş ekranında da gösterilecek.

## A.5 Geçmiş, yarım bırakılmış, yanıtlanmamış ve ertelenmiş planlar — ONAYLANDI

Plan durumları **farklı ve kalıcı** olarak tutulmalı:

- **Tamamlandı**: kullanıcı tamamladığını bildirdi.
- **Kısmen yapıldı**: miktar/süre/sayfa gibi mümkünse gerçek ilerleme kaydı (ör. 20 sayfadan 10 = %50; geçerli miktarı kullanıcı bildirir).
- **Yapılmadı**: kullanıcı açıkça yapmadığını belirtti.
- **Ertelendi**: yeni tarih/saat ve eski erteleme geçmişi korunur; çift sayım olmaz.
- **Yanıtsız / bilinmiyor**: planın zamanı geçti ancak kullanıcı yanıt vermedi. **Otomatik başarısız sayılmaz**.

Örnek: 14:00–15:00 arası 20 sayfa okuma planı, saatler sonra hiç rapor verilmediyse uygulama “20 sayfayı okuyabildin mi?” diye sorar. Kullanıcı “tamamladım / kısmen / yapamadım / ertele” ile yanıtlar. Kısmen yapıldıysa ne kadar yapıldığı **elle** girilir. Hesaplanan ilerleme AI istatistiklerinde kullanılabilir; cezalandırıcı veya uydurma başarı puanı yok.

**Geri bildirim ekranının nihai yeni UX kararı:**

- Birkaç yanıtsız eski plan varsa uygulamaya girince **üst üste duran, arkadakiler hafif sağa/sola eğilmiş, derinliği olan kart destesi** gösterilir. Arkada başka kartlar olduğu sezilir (PayPal benzeri yığılma + Tinder benzeri hareket).
- Öndeki kart: plan adı, hedef/saat ve kısa “Yaptın mı?” sorusu.
- **Sola kaydır = Yapmadım**, **sağa kaydır = Yaptım**; yönler sabit ve anlaşılır işaretlenir.
- **Kısmen yaptım** ve **Ertele** ayrıca görünür ikincil eylemler. Kısmi miktar elle girilir; erteleme yeni zaman seçimiyle yapılır.
- Kaydırınca kart kontrollü biçimde döner/ekran dışına kayar; sonraki kart öne büyüyerek gelir. Hafif eğim/animasyon gösterişli ama akıcı ve erişilebilir olmalı. Kaydırma yapmak istemeyenler için basılabilir eşdeğer eylem düğmeleri şarttır.
- Yanlışlıkla kaydırmada **geri al** imkânı. Kısmen ve ertele için gereken veriler alınmadan sessizce işlem tamamlanmamalı.
- **Bu küçük kartlara ekstra mikrofon veya ses butonu EKLENMEYECEK**; elle yazmak yeterlidir.
- Yanıtlanmamış kartları öncelikli ve belirgin şekilde sun, ama kullanıcının uygulamaya erişimini sert biçimde engelleme. Bildirimler ve widget'larda da birikmiş durumlar gösterilebilir.
- Kullanıcının yanıtları uzun vadeli kişiselleştirme/istatistikte kullanılabilir. Gelecek planlar başarısız sayılmaz; erteleme ve kısmi tamamlama doğru sayılır.

## A.6 Widget'lar — ÜÇ BOYUT ONAYLANDI

1. **Küçük**: sıradaki plan, saati ve sade ilerleme.
2. **Orta**: bugünkü birkaç plan, durumları, hızlı plan açma ve `+` ile AI destekli plan ekleme.
3. **Büyük**: günün özeti, tamamlanan/toplam sayısı, ilerleme, sıradaki saatli ve “Gün içinde” saatiz işleri, ekleme kısayolu.

Koyu Rotam tasarımı, turuncu vurgular, yeni logo. Cihaz/platforma göre desteklenen boyut/etkileşim farkları uyarlansın; iOS'un widget kısıtları göz önüne alınsın. Mümkün olan yerde hızlı tamamlama eylemi; aksi durumda doğru plan detayını açsın. Cihaz çevrimdışıyken yerel planları göstersin. Yanıtsız geçmiş işler küçük işaretle belirtilebilir.

## A.7 TEK birleşik akıllı arama — ONAYLANDI

- Kullanıcı **tek arama alanı** görür; “normal arama / AI arama” diye sekme ya da mod seçtirilmez.
- Samsung Galeri'nin tarih/doğal dil araması örneğine benzer: `Market` → ilgili planlar; `Mayıs 2024` → o tarihteki kayıtlar; `Geçen hafta yaptıklarım` → tamamlanan geçmiş planlar; `Spor` → plan/rutin; `Geçen ay kaç kere spor yaptım?` → kayıtlı verilerden gerçek sayı + sonuç.
- Basit anahtar sözcük, tarih ve durum filtreleri mümkün olduğunca **yerel/veritabanı tabanlı hızlı** çözülsün; karmaşık doğal dilde AI kullanılabilir. Kullanıcı bunu seçmesin ve uygulama sonuçları uydurmasın.
- Tamamlanmış, ertelenmiş, yanıtlanmamış ve gelecekteki kayıtlar izin/veri kapsamına göre bulunabilsin. Arama simgesi ana sayfa tasarımındaki gibi kalsın.

## A.8 Yeni launcher/app ikonu — EN YÜKSEK ÖNCELİKLİ HATA

Kullanıcı, uygulama **içindeki** logo değil, **Android telefonun ana ekranında görünen dış launcher ikonunun** çok sayıda güncellemeye rağmen hâlâ eski/hatalı olduğunu defalarca bildirdi. **Yeni bir logo tasarlanmayacak.** Önceden seçilen kıvrımlı **R** ve küçük **turuncu elmas** marka işareti kullanılacak.

Codex mutlaka şu kök-neden kontrollerini yapmalı:

1. `mipmap` / `drawable` kaynak setleri, tüm çözünürlükler ve flavour/build variant'lar.
2. `AndroidManifest.xml` içindeki `android:icon`, `android:roundIcon`, uygulama/applicationId, adaptive icon foreground/background XML kaynakları ve Android 13+ monochrome/themed icon.
3. Debug/release/CI APK'larının gerçekten hangi kaynakları paketlediği; APK içeriğini doğrula.
4. Eski launcher cache'i, kurulu paket kimliği, değişmeyen eski APK / yanlış indirme ve yeniden imzalama senaryoları.
5. Telefona veri kaybettirebilecek uygulama kaldırma/yeniden yüklemeyi **ilk çözüm olarak önerme**. Önce güvenli güncelleme ve emülatör testi.
6. Test sonucunda Android ana ekranda gerçek ikonu görsel olarak doğrula; mümkünse kullanıcı cihazından teyit iste.
7. iOS sürümü için aynı **gerçek seçili logo** ile iOS App Icon set'i oluştur ve simülatörde doğrula.

**KRİTİK DOSYA EKSİĞİ:** Önceki belgede seçilen logonun Library referansı `Karar kesitiyle ROTAM monogramı.png` olarak var; **bu oturumda diske eklenen 11 tasarım görseli arasında bu bağımsız logo dosyası bulunmuyor**. Codex gerçek özgün logo dosyasını erişilebilir Library/asset/repo içinden doğrulamalı; kesin varlığını görmeden temsili/AI ile başka logo uydurup kullanmamalı. Gerekirse kullanıcıdan özgün logo dosyasını istemeli.

## A.9 AI ve plan oluşturma kritik hata dosyası — EN YÜKSEK ÖNCELİK

Önceki kullanıcı bildirimleri:

- **“AI modeli kullanılamıyor”** veya bağlantı/model erişimi hatası.
- Önceki Gemini 2.5 Flash API anahtarını girme/kaydetme denemesi çalışmıyordu; kayıt sonrası alanın boş görünmesi saklama durumunu belirsizleştiriyordu.
- **“Plan oluşturulamadı”** hatası.
- Mikrofonla sesli giriş ve yanıtı dinleme eski sürümde bulunup kaybolmuştu.

**Yeni ürün kararı gereği kullanıcıdan API anahtarı girişi istemeyeceğiz.** Bu yüzden sadece eski metin kutusunu yamamak yeterli değil: Codex **mevcut AI bağlantısının, hata yollarının, model seçiminin, API anahtarı güvenlik modelinin, kayıt-onay ve veri yazma akışının** kök nedenlerini incelemeli ve yeni hazır AI deneyimine uygun güvenli, çalışan mimariyle düzeltmeli. Kullanıcıya boş ekran değil anlaşılır hata ve **elle plan ekleme** alternatifi sunmalı. Günlük/dakikalık kota hatası, ağ yok, yetki reddi, hatalı yanıt gibi durumlar test edilmeli. **Videoda önceki gerçek API anahtarı göründüğüne dair kayıt var; anahtar hiçbir belgeye/koda kopyalanmamalı, ifşa edilmiş sayılarak gerekirse iptal/yenileme yapılmalı.**

## A.10 Ek kritik/eski hata listesi — KORUNDU

Aşağıdakiler önceki dosyada da vardır; yeni pakette özellikle kaybolmamalı:

- Ayarlar → Bugün geçişinde zaman zaman navigasyon hatası.
- Plan eklemede klavye üst bölümü/gerçek giriş metnini gizliyor, sayfa zıplıyor.
- Karmaşık kategori/başlangıç-bitiş zaman arayüzü ve gereksiz tekrar eden kontroller.
- Kullanıcıya alakasız teknik bildirim izin kartları ve çalışmayan izin kontrolü.
- Ana sayfada anlamsız “N” hesabı ve tekrar tekrar isim sorulması.
- Eski chatbot çöp kutusu düğmesinin gereksizliği.
- Gereksiz UI tıklama sesi/animasyon/sessiz saatler ayarlarının ön planda tutulması.
- Tehlikeli/belirsiz “tüm planları sıfırla” vurgusunun kaldırılması; veriler için güvenli dışa aktar/geri yükle.
- Başlat/Tamamla/Odak işlemlerinin birbirinden ayrılması.
- Fazla siyah/gri görünümün marka turuncusuyla dengelenmesi.
- Birden çok `device check`/`review build` ZIP'i yerine **net tek kurulabilir APK**, doğru ikonu içeren doğru build, açık kurulum yönlendirmesi.

## A.11 Önceki ekran kararları hâlâ geçerli

Yukarıdaki ana dosyanın **4.1–4.11** bölümleri aşağıdaki yeni kararlarla çelişmediği ölçüde geçerlidir: Bugün ana sayfası; AI ile `Anlat → Birlikte netleştir → Onayla ve ekle`; manuel plan ekleme fallback; Rutinler; Takvim ve İlerlemem; plan detay/düzenle/ertele; Odaklan mola/bitir ve gerçek süre; hesap/profil; Ayarlar; yükleniyor/hata/boş durum; widget; bildirim; senkronizasyon. Özellikle:

- Plan başlangıç/bitiş bağımsız isteğe bağlı, tek başlangıç açık uçlu ve tek bitiş “bu saate kadar” olabilir. Kullanıcı saat vermediyse uydurma.
- Tekrarlı plan için “yalnız bu gün / bu ve sonraki tekrarlar”; geçmiş silinmez.
- Odak molası odak süresine eklenmez. Odak bitişi otomatik plan tamamlama değildir.
- İstatistikler Takvim üzerinden İlerlemem'e; gelecek iş başarısız değildir, ertelenmiş iş iki kez sayılmaz.
- Bildirim/kayıt/AI değişiklikleri tek yetkili gerçek kayıt sistemine dayanmalı; çift kayıt ve eşzamanlama sorunlarından kaçın.

## A.12 Tasarım referanslarının paket içeriği

Bu paketle birlikte `tasarimlar/` klasöründeki **11 özgün kullanıcı yüklemesi** gelir:

1. `Rotam ana ekranı: arama simgesi.png`
2. `Sade plan onay ekranı.png`
3. `Rutinler ekranı: Gününü düzenle.png`
4. `Rotam ayarlar ekranı.png`
5. `Kasım 2026 Takvim ve Planlar.png`
6. `ROTAM: Plan, AI düzenleme ve erteleme.png`
7. `ROTAM: Odak, özet ve ilerleme.png`
8. `Rotam üç ekranlı odak arayüzü.png` (ek varyant)
9. `Üç ekranlı odaklanma ve ilerleme panosu.png` (son marka estetiği güçlendirilmiş referans)
10. `ROTAM: Dört ekranlı karşılama konsepti.png`
11. `ROTAM Android arayüzleri: 12 panelli tasarım.png`

**Not:** Önceki belgedeki referanslardan bağımsız seçili logo görseli, eski günlük plan/odak görseli ve eski çoklu plan varyantı pakette fiziksel olarak mevcut değildir. Önceki belgedeki Library adları/referanslar korunmuştur. Tek bir çoklu panodan her ekranın üretim ölçüleri çıkarılmamalı; yazılı gereksinim ve son varyant esas alınmalıdır. Eski mockup üzerinde API anahtarı alanı varsa yeni ürün kararına göre normal kullanıcı akışında kullanılmamalı. Launcher ikonunun nihai kaynağı mutlaka **özgün seçilmiş logo** olmalı.

## A.13 Codex için önerilen uygulama sırası ve kabul ölçütleri

**Aşama 0 — İnceleme, kodsuz rapor:** Gerçek repo, dal, PR, mevcut dosyalar, GitHub Actions, SDK, anahtar güvenliği, logo asset'leri, mevcut veri modeli ve dağıtım adımları. Önceki tarihsel notlar güncel HEAD kanıtı değildir. Maddeleri `mevcut/eksik/bozuk/uygulanmadı/doğrulanamadı` olarak sınıflandır. Gerçek repo içeriği bu belgeyle çelişirse bildir.

**Aşama 1 — Sağlam temel ve kritik hatalar:** Launcher ikonunun build ve cihazda düzeltilmesi; AI erişimi (API key'i kullanıcıya yüklemeyen güvenli yol), mikrofon/yanıt sesi, plan oluşturma, navigasyon, klavye, bildirim izni. Bug'ın kök nedenini açıkla, düzelt, test et. Mevcut verileri silme.

**Aşama 2 — Veri/hesap/hafıza:** Google girişi, senkronizasyon, çevrimdışı yazma, kullanıcıya ait AI verisi, yetkilendirme, silme/dışa aktarma. Gerçekten çalışan senkronizasyonu farklı cihaz/oturumla test et.

**Aşama 3 — Planlama deneyimi ve UI:** Kullanıcı görselleri ve yeni tasarım kararlarıyla ana sayfa, Plan Ekle, Rutinler, Takvim, Odak, İlerlemem, Ayarlar, arama ve widget. İşlemler taslak/onay mekanizmasını atlamamalı.

**Aşama 4 — Akıllı takip:** Geciken/yanıtsız/kısmi/ertelenen kayıtlar, kart destesi ve geri alma, sakin ancak ısrarcı takip bildirimleri, widget göstergeleri, gerçek istatistikler.

**Aşama 5 — iOS:** Android'i bozmadan taşınabilir ortak iş katmanı değerlendirmesi ve güvenli aşamalı iki platform uygulaması; Xcode/iOS Simulator testi. Platforma özgü widget/izin/bildirim deneyimlerini doğrula.

**Aşama 6 — Çıktı ve doğrulama:** Unit/UI/emülatör/varsa gerçek cihaz testleri, kaynak görsel ve paket içeriğinin doğrulanması, kullanıcının kuracağı **tek, açık isimli APK** ve iOS için koşullara uygun test çıktısı, CI raporu; mevcut veriyle yükseltme senaryosu. Sadece test sonucu kanıtlanan başarıyı iddia et.

**Asgari uçtan uca kabul senaryoları:**

- Google ile giriş → profil → plan/rutin oluştur → cihazda kayıt → oturum/cihaz değiştir → geri gelme/senkronize olma.
- Yazıyla ve mikrofonla “Yarın 14–22 iş, öncesinde süt ve yumurta al” → iki doğru taslak → onay → tekil kayıt → takvimde görünme.
- “Yarın işe gideceğim” → bilinen saatler **önerilir** → farklı yarın saatine düzelt → yalnız yarını etkiler.
- Yalnız başlangıç, yalnız bitiş, hiç saat olmaması, tekrarlı rutinin yalnız tek gün düzenlenmesi.
- Gecikmiş 20 sayfa kitap planı: yanıtsız kaldığında başarısız sayılmaz → solda yapılmadı/sağda yapıldı; kısmi miktar; ertele; geri al; ekran okuyucu/düğmeler.
- İki cihaz aynı hesapta değişiklikler; internetsiz kullanım ve yeniden bağlantı; yetkisiz kullanıcı erişim kontrolü.
- AI anahtarı istemeden güvenli bağlantı ve yanıt; model/ağ/kota hatası durumunda kullanıcıya açıklama ve manuel plan ekleme; gizli anahtar uygulama paketinde yok.
- Android launcher ikonu **kurulu APK ana ekranında** gerçek seçili logo; iOS app icon aynı seçili işaret.
- Saatli/saatiz plan takvimi, odak mola süresinin hesaplanmaması, bildirim tekrarlarının sınırlı olması, widget durumlarının güncellenmesi.

## A.14 Henüz teknik doğrulama isteyen açık işler — TAHMİNLE TAMAMLAMA

1. Kendi kullanımında “anahtarsız, ücretsiz, büyük ölçüde sınırsız hissettiren” AI hedefini hangi **güvenli, gerçek maliyeti kontrol edilen sağlayıcı erişimiyle** sağlamak mümkün? Ücretsiz kota ve kişisel veri şartları kontrol edilmeli. Bu konu henüz teknik olarak çözülmüş değildir.
2. iOS + Android ortak kod geçiş maliyeti ve Room/veritabanı/hatırlatmalar/widget/Google giriş eşdeğerleri; tasarım kararı evet ama uygulanma yolu incelenmeli.
3. Tam seçilen **launcher R + elmas** varlığının dosyası kullanıcı yüklemeleri arasında yok. Kodlaştıktan önce özgün kaynak doğrulanmalı.
4. Kesin bildirim frekansı, sessizleştirme ve geçmiş plan kartının görünme eşikleri ilk teknik prototipte kullanıcı açısından rahat mı? Her ekranı açmayı zorla bloke etme.
5. Kullanıcının önceki video dosyası bu pakete dahil değildir. Eski belgede listelediği hatalar kullanıldı; videonun görüntüsü yeniden doğrulanmış gibi gösterilmemeli.
6. Önceki belgede bahsedilen CI geçmişi, PR, dal ve test sayıları **tarihsel bilgi**; mevcut kodda yeniden teyit edilmeli.
7. Plan/AI/hafıza ile ilgili veri güvenliği, kullanıcı izni, dışa aktarma/silme, hesap silme, eski yerel verilerin yeni hesaba taşınması teknik gereksinim ve testlere dönüştürülmeli.
8. iOS dağıtımı ve App Store/Apple Developer gereksinimleri Android APK dağıtımından farklıdır; ücretsiz iOS simülatör testi ile genel dağıtım aynı şey değildir.

**Son durum (9 Kasım 2026):** Ürün kararları Codex'e aktarılacak düzeyde derlenmiştir; tüm ayrıntıların tarihsel eksiksizliği garanti edilmez. Yeni kod uygulanmamıştır. Codex önce inceleme/plan, sonra kontrollü uygulama ve test yapmalıdır. Kullanıcının amacı uzun bir soru-cevap oturumu değil, öncelikleri uygulayıp somut test çıktısı almak.
