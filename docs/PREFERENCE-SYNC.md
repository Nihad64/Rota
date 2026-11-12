# Tercihlerin cihazlar arasında korunması

Plan eşitlemesinden ayrı onay gerekir. `/users/{uid}/preferences/snapshot` yalnız hesabın sahibine açılır. Mevcut plan snapshot'ının sürümü/yolu ve kişisel kayıtları değiştirilmez.

Sekiz değer taşınır: uygulama hatırlatmaları, özel hatırlatma ön süresi, sessiz saat açık/kapalı ve başlangıç/bitiş dakikaları, onay sesi, titreşim, hareket. Sessiz saat sınırları 0–1439, ön süre 0–10080; diğerleri Boolean. Kimlik/oturum, API anahtarı, Google profil bilgisi, AI plan paylaşımı, AI hafıza kullanım izni, onboarding durumu ve Android sistem izinleri bu kapsama girmez. Gizlilik seçimleri başka cihaz tarafından açılamaz.

İlk cihaz yeni kişisel snapshot oluşturur. Yeni cihazda yerel varsayılanlarla bulut farklıysa her değişen tercih iki tarafın değeriyle gösterilir. Buluttakileri veya cihazdakileri seçmeden bu alanlar yazılmaz. Sonraki çevrimdışı değişiklikler alan bazında üç yönlü birleştirilir. Aynı alandaki farklı seçim incelenir; inceleme sırasında yeni yerel değişiklik olursa eski seçim uygulanmaz.

DataStore karşılaştırması ve uygulaması tek `edit` içindedir; yalnız beyaz listedeki sekiz alan yazılır. Plan/rutin Room işlemiyle karıştırılmaz. Plan eşitlemesini durdurmak veya hesabı değiştirmek tercih taşınmasını da durdurur. Eşitleme geçmişi hesabın karma kimliğiyle özel no_backup dosyasında tutulur. Onay/binding/hesap kontrolü ağ yazımından ve yerel uygulamadan önce tekrar yapılır. Periyodik ağ işi Android'in izin verdiği zamanlarda yeniden dener.

Dış Firestore biçimi mevcut schema=1/revision/writer/server timestamp/payload sözleşmesidir; tercih payload'ı 16 KiB ile sınırlıdır. İç sıkıştırılmış zarf yalnız PREFERENCE türleri varsa schema=3, plan/hafıza zarfı schema=2 kalır. Eski uygulama tercih yolunu kullanmaz. Plan okuyucusu yanlışlıkla karışmış PREFERENCE kaydını reddeder.

## Kuralların incelenmesi

Prototip kurallar: iki yol da yalnız path sahibi için read/create/update; create revision=1, update önceki+1; gerekli ve yalnız izinli beş alan, tip/boyut/timestamp sınırı. Başka hesap/anonim istek, users koleksiyonu listeleme, ekstra owner/role/admin alanı, geçersiz sürüm/saat, eksik alan, aşırı payload ve silme reddedilir. Üst user belgesinin varlığı gerekmez: Auth UID sahipliği yetkilendirir; herkese açık üst profil yoktur. Payload bytes olduğundan iç veri ilişkilerini kurallar açıp incelemez; istemci import öncesinde sıkı tür/anahtar doğrular. İzin alanları payload'dan yetki olarak yorumlanmaz.

CLI dry-run sözdizimi doğrulaması geçti. Yerelde iki ayrı DataStore ile yeni cihaz geri dönüşü, gizlilik izinlerinin korunması, bağımsız çevrimdışı değişiklikler, eskimiş inceleme ve iznin geri alınması testleri geçti. Demo Firebase emülatöründe sahiplik/tip/boyut testleri ile Android'de iki gerçek SDK istemcisinin tercih aktarım testi CI'da ayrıca çalışır. CI sonucu doğrulanmadan yeni kurallar canlıya dağıtılmaz. Kişisel telefon tercihleri otomatik yüklenmez.
