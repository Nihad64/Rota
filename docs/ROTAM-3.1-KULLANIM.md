# Rotam 3.1 — Güncelleme ve kullanım

## Telefonundaki sürümü gör

Rotam → Ayarlar → Rotam hakkında. Sürüm **3.1.0 (51)** olmalı. USB üzerinden bu sürüm kurulduysa aynı dosyayı tekrar kurman gerekmez.

## APK ile güncelleme

1. Telefonuna yalnız **Rotam-3.1.0.apk** dosyasını gönder/indir. ZIP veya kaynak kod dosyası açman gerekmez.
2. Telefonun Dosyalar uygulamasında APK'ya dokun. Telefon kurulum izni sorarsa bu dosyayı açtığın uygulama için izin ver.
3. **Güncelle** düğmesine bas; ardından **Aç**. Eski Rotam'ı kaldırma ve uygulama verilerini temizleme. Güncelleme mevcut kayıtların üzerine kurulur.

GitHub kullanman gerekirse PR #2'nin en son başarılı Android çalışmasında **Rotam-Android-APK** çıktısı bulunur. GitHub bu çıktıyı ZIP içinde verir; içindeki tek APK'yı çıkarıp aç. `main` dalındaki eski dosyalar bu paketin yerine geçmez.

## Değişen günlük akışlar

- **Rutin silme:** Rutinler → ilgili kartın üç noktalı menüsü → Rutini sil → onay. Gelecekteki tekrarlar durur ve yanıtsız örnekler kaldırılır. Tamamlanmış/kısmi/yapılmadı sonuçları geçmişte korunur. Ekrandaki Geri al işlemi, arada yeni düzenleme yapılmadıysa seriyi geri getirir.
- **Takvim:** Ay ve yıl işaretleri tek seferlik planları gösterir. Rutinler, ilgili güne girdiğinde günün listesinde görünür.
- **Arama:** Aynı rutinin bütün günleri tek kartta toplanır. Günleri göster ile açılır, daha fazlası isteğe bağlı yüklenir. Plan ve rutin sayıları ayrı yazılır. Bilinen eski sonuçlar açık aramayla bulunabilir.
- **Bugün:** Başlangıcı geçen yanıtsız plan durum kartına geçer. Yaptım / Kısmen yaptım / Yapmadım / Ertele seçenekleriyle yanıtlayabilirsin. Sırf saati geçti diye yapılmadı sayılmaz. Ana sayfada Başlat bulunmaz; sayaç yalnız Odaklan ekranından başlar.
- **Plan oluşturma:** Plan ekle ile yazabilir veya söyleyebilirsin. Kendin oluştur tek sayfalık formu açar. Başlık yeterlidir; saat, kategori, tekrar, hatırlatma ve not isteğe bağlıdır. Saat satırı başlangıç ve bitişi ayrı açar.
- **AI onayı:** Öneri kartlarını inceleyip onaylamadan plan kaydedilmez. Fazla bir önerinin menüsünden Taslağı çıkar seçilebilir. Okul/iş hazırlığı ayrıca istenmedikçe ikinci rutin yapılmaması için AI yönlendirmesi güncellendi; öneri yine incelenmelidir.
- **Ayarlar:** Hesap/eşitleme, bildirim, ses ve hareket, veriler ve gizlilik ayrı alanlardadır. Google profil adı kullanılır. Tekrarlanan isim formu ve ana sayfanın ikinci AI düğmesi kaldırıldı.

## Hesap, veri ve ses

Google girişiyle eşitleme izni ayrı işlemlerdir. Hesabım bölümünde bağlantıyı ve son eşitlemeyi görebilirsin. Yerel kayıtlar çevrimdışı kullanılabilir. AI internet, hesap ve sağlayıcının kullanılabilir kotasını gerektirir; senden Gemini API anahtarı istenmez. İzin verdiğin hafıza bilgilerini Gizlilik bölümünden inceleyebilir, düzenleyebilir ve unutturabilirsin.

Ses ve hareket bölümündeki kısa onay sesi isteğe bağlıdır; sessiz/titreşim modunu aşmaz. Hareket tercihi ve sistemin azaltılmış hareket ayarı dikkate alınır. Bildirim kanalı telefonun sistem ayarlarından kapalıysa uygulama bunu başka kanal açarak aşmaz.

## iOS durumu

Native uygulama, ortak veri sözleşmesi ve widget projesi depoda geliştirilir. iPhone'a dağıtım için Apple imzalama/App Attest yapılandırması gerekir. Android APK dosyası iPhone'a kurulamaz. Simülatör testinin sonucu teknik doğrulama kaydında ayrıca belirtilir; gerçek iPhone giriş/AI testiyle aynı değildir.
