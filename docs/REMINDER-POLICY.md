# Rotam hatırlatma sözleşmesi

Yeni planlarda otomatik takip; eski planlarda mevcut açık/kapalı ve özel saat kararı korunur. Planın başlangıç/bitiş saatleri bildirim için uydurulmaz. Otomatik takvimin saatleri yalnız bildirim zamanıdır.

- Başlangıç varsa: randevu 30, iş 20, spor 15, diğerleri 10 dakika önce; başlangıçta; bitişten 30 dakika sonra (bitiş yoksa başlangıçtan 60 dakika sonra); sonraki gün 18:00'de son takip. Toplam en fazla dört aşama.
- Yalnız son teslim varsa: 30 dakika önce, son teslimde, iki saat sonra, sonraki gün 18:00. Geceyi aşan bitiş tarihi kullanılır.
- Saatsiz: o gün 19:00'da durum sorusu, ertesi gün 18:00'de son takip. Plan saatsiz kalır.
- Sessiz saatlere denk gelen aşama sessiz saatlerin sonuna taşınır; aynı saate yığılan aşamalar tekilleşir. Takip soruları bilinen başka bir saat aralığının içindeyse (ör. iş), o aralığın sonundan 15 dakika sonraya alınır. En fazla bitişten iki gün sonraya kadar; ulaşım süresi tahmin edilmez.
- Tüm planlarda günde en fazla altı **otomatik** bildirim; aralarında en az 15 dakika. Eşzamanlı alarmlar aynı kilit altında bu bütçeyi paylaşır. Kişinin özel seçtiği tek alarm otomatik bütçeye katılmaz.
- `Başladım` (odak sayacı açmadan), `Tamamladım`, `Biraz sonra` bildirim eylemleridir. Başlangıç/bitirme/silme/duraklatma ilgili alarmları durdurur. Odak sayacını başlatmak da başlandı bilgisini kaydeder. Başlandı bilgisi plan UID+tarih/saat sözleşmesine bağlıdır; erteleme yeni sözleşmeyle takip başlatır.
- Biraz sonra yalnız bildirimi 30 dakika erteler, plan zamanını değiştirmez. En fazla üç kullanıcı ertelemesi, sonra o örneğin takibi durur. Bildirimi kaydırıp kapatmak o örneğin kalan takibini durdurur. Yapılmadı sayılmaz. Geri kalan kayıt ve geçmiş korunur.
- Bildirim UID'si, takvim imzası ve teslim belirteci doğrulanır. Yedek geri yüklemesiyle aynı sayısal ID başka plana verilirse eski eylem onu değiştiremez. Ertelenen planın eski bildirimi de işlem yapamaz. Gösterilen bildirim yeniden planlama sırasında sebepsiz kaldırılmaz.
- Kaçırılmış eski alarm topluca şimdi gönderilmez. Sonraki gelecekteki aşama kurulur. Altı saatten fazla gecikmiş bir alarm doğrudan gösterilmez. Son durum geçmişi bilinmeyen plan otomatik başarısız yapılmaz.
- İzin ve mevcut Android bildirim kanalı korunur; kapalı/sessiz kanal yeni kanalla aşılmaz. Kesin alarm izni yoksa izinli inexact alarm kullanılır. Android'in pil/Doze politikaları teslimi geciktirebilir; kesin teslim sözü verilmez. Yeniden başlatma, saat dilimi/saat değişimi ve periyodik bakım yeniden kurar. Kurulum hatası görünür durumda tutulur, kaydı silmez.

Kaynaklar: [Android alarm zamanlaması](https://developer.android.com/develop/background-work/services/alarms), [bildirim eylemleri/izinleri](https://developer.android.com/develop/ui/compose/notifications/create-notification). Android'in 15 dakika/Doze ve kesin alarm erişimi nedeniyle bu sınırlar hedef zamanlardır.
